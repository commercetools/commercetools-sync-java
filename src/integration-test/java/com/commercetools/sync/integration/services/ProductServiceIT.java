package com.commercetools.sync.integration.services;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.impl.ProductServiceImpl;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Image;
import io.sphere.sdk.products.ImageDimensions;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.AddExternalImage;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import io.sphere.sdk.products.commands.updateactions.SetKey;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getReferencesWithIds;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_2_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ProductServiceIT {
    private ProductService productService;
    private static ProductType productType;
    private static List<Reference<Category>> categoryReferencesWithIds;
    private Product product;


    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;


    /**
     * Delete all product related test data from target project. Then creates custom types for target CTP project
     * categories.
     */
    @BeforeClass
    public static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_TARGET_CLIENT);
        final List<Category> categories = createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
        categoryReferencesWithIds = getReferencesWithIds(categories);
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    }

    /**
     * Deletes Products and Types from target CTP projects, then it populates target CTP project with product test
     * data.
     */
    @Before
    public void setupTest() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
        deleteAllProducts(CTP_TARGET_CLIENT);

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                               .errorCallback(
                                                                                   (errorMessage, exception) -> {
                                                                                       errorCallBackMessages
                                                                                           .add(errorMessage);
                                                                                       errorCallBackExceptions
                                                                                           .add(exception);
                                                                                   })
                                                                               .warningCallback(warningMessage ->
                                                                                   warningCallBackMessages
                                                                                       .add(warningMessage))
                                                                               .build();

        // Create a mock new product in the target project.
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH,
            productType.toReference(), null, null, categoryReferencesWithIds,
            createRandomCategoryOrderHints(categoryReferencesWithIds));
        product = CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft))
                                   .toCompletableFuture().join();

        productService = new ProductServiceImpl(productSyncOptions);
    }

    /**
     * Cleans up the target test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
    }

    @Test
    public void getIdFromCacheOrFetch_WithNotCachedExistingProduct_ShouldFetchProductAndCache() {
        final Optional<String> productId = productService.getIdFromCacheOrFetch(product.getKey())
                                                         .toCompletableFuture()
                                                         .join();
        assertThat(productId).isNotEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void getIdFromCacheOrFetch_WithNullProductKey_ShouldReturnEmptyOptional() {
        final Optional<String> productId = productService.getIdFromCacheOrFetch(null)
                                                         .toCompletableFuture()
                                                         .join();
        assertThat(productId).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void getIdFromCacheOrFetch_WithCachedExistingProduct_ShouldFetchFromCache() {
        final String oldKey = product.getKey();
        final Optional<String> oldProductId = productService.getIdFromCacheOrFetch(oldKey)
                                                            .toCompletableFuture()
                                                            .join();

        // Change product key on ctp
        final String newKey = "newKey";
        productService.updateProduct(product, Collections.singletonList(SetKey.of(newKey)))
                      .toCompletableFuture()
                      .join();

        // Fetch product from cache
        final Optional<String> cachedProductId = productService.getIdFromCacheOrFetch(oldKey)
                                                               .toCompletableFuture().join();

        assertThat(cachedProductId).isNotEmpty();
        assertThat(cachedProductId).isEqualTo(oldProductId);

        // Fetch product from ctp (because of new key not cached)
        final Optional<String> productId = productService.getIdFromCacheOrFetch(newKey)
                                                         .toCompletableFuture().join();

        assertThat(productId).isNotEmpty();
        // Both keys point to the same id.
        assertThat(productId).isEqualTo(cachedProductId);

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void cacheKeysToIds_WithEmptyKeys_ShouldReturnCurrentCache() {
        Map<String, String> cache = productService.cacheKeysToIds(emptySet()).toCompletableFuture().join();
        assertThat(cache).hasSize(0); // Since cache is empty

        cache = productService.cacheKeysToIds(singleton(product.getKey())).toCompletableFuture().join();
        assertThat(cache).hasSize(1);

        cache = productService.cacheKeysToIds(emptySet()).toCompletableFuture().join();
        assertThat(cache).hasSize(1); // Since cache has been fed with a product key

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void cacheKeysToIds_WithSomeEmptyKeys_ShouldReturnCorrectCache() {
        final Set<String> productKeys = new HashSet<>();
        productKeys.add(product.getKey());
        productKeys.add(null);
        productKeys.add("");
        Map<String, String> cache = productService.cacheKeysToIds(productKeys)
                                                  .toCompletableFuture().join();
        assertThat(cache).hasSize(1);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchMatchingProductsByKeys_WithEmptySetOfKeys_ShouldReturnEmptySet() {
        final Set<Product> fetchedProducts = productService.fetchMatchingProductsByKeys(Collections.emptySet())
                                                           .toCompletableFuture().join();
        assertThat(fetchedProducts).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchMatchingProductsByKeys_WithAllExistingSetOfKeys_ShouldReturnSetOfProducts() {
        final Set<Product> fetchedProducts = productService.fetchMatchingProductsByKeys(singleton(product.getKey()))
                                                           .toCompletableFuture().join();
        assertThat(fetchedProducts).hasSize(1);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchMatchingProductsByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
        // Mock sphere client to return BadeGatewayException on any request.
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(ProductQuery.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
            .thenCallRealMethod();
        final ProductSyncOptions spyOptions = ProductSyncOptionsBuilder.of(spyClient)
                                                                         .errorCallback(
                                                                             (errorMessage, exception) -> {
                                                                                 errorCallBackMessages
                                                                                     .add(errorMessage);
                                                                                 errorCallBackExceptions
                                                                                     .add(exception);
                                                                             })
                                                                         .build();
        final ProductService spyProductService = new ProductServiceImpl(spyOptions);


        final Set<String> keys =  new HashSet<>();
        keys.add(product.getKey());
        final Set<Product> fetchedProducts = spyProductService.fetchMatchingProductsByKeys(keys)
                                                                  .toCompletableFuture().join();
        assertThat(fetchedProducts).hasSize(0);
        assertThat(errorCallBackExceptions).isNotEmpty();
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorCallBackMessages).isNotEmpty();
        assertThat(errorCallBackMessages.get(0))
            .isEqualToIgnoringCase(format("Failed to fetch products with keys: '%s'. Reason: %s",
                keys.toString(), errorCallBackExceptions.get(0)));
    }

    @Test
    public void fetchMatchingProductsByKeys_WithSomeExistingSetOfKeys_ShouldReturnSetOfProducts() {
        final Set<String> keys =  new HashSet<>();
        keys.add(product.getKey());
        keys.add("new-key");
        final Set<Product> fetchedProducts = productService.fetchMatchingProductsByKeys(keys)
                                                           .toCompletableFuture().join();
        assertThat(fetchedProducts).hasSize(1);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void createProducts_WithAllValidProducts_ShouldCreateProducts() {
        // create a draft based of the same existing product but with different key, slug and master variant SKU since
        // these values should be unique on CTP for the product to be created.
        final ProductDraft productDraft1 = createProductDraftBuilder(PRODUCT_KEY_1_RESOURCE_PATH,
            productType.toReference())
            .key("newKey")
            .taxCategory(null)
            .state(null)
            .categories(emptyList())
            .categoryOrderHints(null)
            .slug(LocalizedString.of(Locale.ENGLISH, "newSlug"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft productDraft2 = createProductDraft(PRODUCT_KEY_2_RESOURCE_PATH, productType.toReference(),
            null, null, categoryReferencesWithIds,
            createRandomCategoryOrderHints(categoryReferencesWithIds));

        final Set<ProductDraft> productDrafts = new HashSet<>();
        productDrafts.add(productDraft1);
        productDrafts.add(productDraft2);

        final Set<Product> createdProducts = productService.createProducts(productDrafts)
                                                           .toCompletableFuture().join();

        assertThat(createdProducts).hasSize(2);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    @Ignore("Ignoring test because of bug in CTP: https://jira.commercetools.com/browse/SUPPORT-1348")
    public void createProducts_WithSomeValidProducts_ShouldCreateProductsAndTriggerCallBack() {
        // create a draft based of the same existing product but with different key, slug and master variant SKU since
        // these values should be unique on CTP for the product to be created.
        final ProductDraft productDraft1 = createProductDraftBuilder(PRODUCT_KEY_1_RESOURCE_PATH,
            productType.toReference())
            .key("1")
            .categories(emptyList())
            .categoryOrderHints(null)
            .slug(LocalizedString.of(Locale.ENGLISH, "newSlug"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft productDraft2 = createProductDraft(PRODUCT_KEY_2_RESOURCE_PATH, productType.toReference(),
            null, null, categoryReferencesWithIds,
            createRandomCategoryOrderHints(categoryReferencesWithIds));

        final Set<ProductDraft> productDrafts = new HashSet<>();
        productDrafts.add(productDraft1);
        productDrafts.add(productDraft2);

        final Set<Product> createdProducts = productService.createProducts(productDrafts)
                                                               .toCompletableFuture().join();

        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).contains("Invalid key '1'. Keys may only contain "
            + "alphanumeric characters, underscores and hyphens and must have a maximum length of 256 characters.");
        assertThat(createdProducts).hasSize(1);
    }

    @Test
    public void createProducts_WithNoneValidProducts_ShouldTriggerCallBack() {
        // create a draft based of the same existing product but with different key, slug and master variant SKU since
        // these values should be unique on CTP for the product to be created.
        final ProductDraft productDraft1 = createProductDraftBuilder(PRODUCT_KEY_1_RESOURCE_PATH,
            productType.toReference())
            .key("newKey")
            .taxCategory(null)
            .state(null)
            .categories(emptyList())
            .categoryOrderHints(null)
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft productDraft2 = createProductDraftBuilder(PRODUCT_KEY_1_RESOURCE_PATH,
            productType.toReference())
            .key("newKey1")
            .taxCategory(null)
            .state(null)
            .categories(emptyList())
            .categoryOrderHints(null)
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();


        final Set<ProductDraft> productDrafts = new HashSet<>();
        productDrafts.add(productDraft1);
        productDrafts.add(productDraft2);

        final Set<Product> createdProducts = productService.createProducts(productDrafts)
                                                               .toCompletableFuture().join();

        assertThat(errorCallBackExceptions).hasSize(2);
        assertThat(errorCallBackMessages).hasSize(2);
        assertThat(errorCallBackMessages.get(0)).contains(" A duplicate value '\"english-slug\"' exists for field "
            + "'slug.en'");
        assertThat(errorCallBackMessages.get(1)).contains(" A duplicate value '\"english-slug\"' exists for field "
            + "'slug.en'");
        assertThat(createdProducts).isEmpty();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void createProduct_WithValidProduct_ShouldCreateProduct() {
        final ProductDraft productDraft1 = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            productType.toReference())
            .taxCategory(null)
            .state(null)
            .categories(emptyList())
            .categoryOrderHints(null)
            .build();

        final Optional<Product> createdProductOptional = productService.createProduct(productDraft1)
                                                                       .toCompletableFuture().join();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(createdProductOptional).isNotEmpty();
        final Product createdProduct = createdProductOptional.get();


        //assert CTP state
        final Optional<Product> productOptional = CTP_TARGET_CLIENT
            .execute(ProductQuery.of()
                                  .withPredicates(QueryPredicate.of(format("key = \"%s\"", productDraft1.getKey()))))
            .toCompletableFuture().join().head();

        assertThat(productOptional).isNotEmpty();
        final Product fetchedProduct = productOptional.get();
        assertThat(fetchedProduct.getMasterData().getCurrent().getName())
            .isEqualTo(createdProduct.getMasterData().getCurrent().getName());
        assertThat(fetchedProduct.getMasterData().getCurrent().getSlug())
            .isEqualTo(createdProduct.getMasterData().getCurrent().getSlug());
        assertThat(fetchedProduct.getKey()).isEqualTo(productDraft1.getKey());
    }

    @Test
    public void createProduct_WithBeforeCreateCallbackSet_ShouldCreateFilteredProduct() {
        final String keyPostfix = "_filteredKey";

        // callback function that post fixes the product draft key with "_filteredKey"
        final Function<ProductDraft, ProductDraft> draftFunction = productDraft ->
                ProductDraftBuilder.of(productDraft).key(format("%s%s", productDraft.getKey(), keyPostfix)).build();

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                .errorCallback((errorMessage, exception) -> {
                    errorCallBackMessages.add(errorMessage);
                    errorCallBackExceptions.add(exception);
                })
                .warningCallback(warningMessage -> warningCallBackMessages.add(warningMessage))
                .beforeCreateCallback(draftFunction)
                .build();
        final ProductService productService = new ProductServiceImpl(productSyncOptions);

        final ProductDraft productDraft1 = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
                productType.toReference())
                .taxCategory(null)
                .state(null)
                .categories(emptyList())
                .categoryOrderHints(null)
                .build();

        final Optional<Product> createdProductOptional = productService.createProduct(productDraft1)
                .toCompletableFuture().join();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(createdProductOptional).isNotEmpty();

        final Product createdProduct = createdProductOptional.get();

        //Query for a product with key post fixed with "_filteredKey" added by the callback
        final String keyWithCallbackPostFix = format("%s%s", productDraft1.getKey(), keyPostfix);
        final Optional<Product> productOptional = CTP_TARGET_CLIENT
                .execute(ProductQuery.of()
                        .withPredicates(QueryPredicate.of(format("key = \"%s\"", keyWithCallbackPostFix))))
                .toCompletableFuture().join().head();

        assertThat(productOptional).isNotEmpty();
        final Product fetchedProduct = productOptional.get();
        assertThat(fetchedProduct.getKey()).isEqualTo(keyWithCallbackPostFix);

        assertThat(fetchedProduct.getMasterData().getCurrent().getName())
                .isEqualTo(createdProduct.getMasterData().getCurrent().getName());
    }

    @Test
    public void createProduct_WithBeforeCreateCallbackToSkipSet_ShouldNotCreateProduct() {
        // callback function that skips product creation
        final Function<ProductDraft, ProductDraft> draftFunction = productDraft -> null;

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                .errorCallback((errorMessage, exception) -> {
                    errorCallBackMessages.add(errorMessage);
                    errorCallBackExceptions.add(exception);
                })
                .warningCallback(warningMessage -> warningCallBackMessages.add(warningMessage))
                .beforeCreateCallback(draftFunction)
                .build();
        final ProductService productService = new ProductServiceImpl(productSyncOptions);

        // create a draft based of the same existing product but with different key, slug and master variant SKU since
        // these values should be unique on CTP for the product to be created.
        final String newKey = "newKey";
        final ProductDraft productDraft1 = createProductDraftBuilder(PRODUCT_KEY_1_RESOURCE_PATH,
                productType.toReference())
                .key(newKey)
                .taxCategory(null)
                .state(null)
                .categories(emptyList())
                .categoryOrderHints(null)
                .slug(LocalizedString.of(Locale.ENGLISH, "newSlug"))
                .masterVariant(ProductVariantDraftBuilder.of().build())
                .build();

        final Optional<Product> createdProductOptional = productService.createProduct(productDraft1)
                .toCompletableFuture().join();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(createdProductOptional).isEmpty();

        //Assert that product with key doesn not exist in CTP.
        final Optional<Product> productOptional = CTP_TARGET_CLIENT
                .execute(ProductQuery.of()
                        .withPredicates(QueryPredicate.of(format("key = \"%s\"", newKey))))
                .toCompletableFuture().join().head();

        assertThat(productOptional).isEmpty();
    }

    @Test
    public void createProduct_WithInvalidProduct_ShouldNotCreateProduct() {
        // Create product with same slug as existing product
        final String newKey = "newKey";
        final ProductDraft productDraft1 = createProductDraftBuilder(PRODUCT_KEY_1_RESOURCE_PATH,
            productType.toReference())
            .key(newKey)
            .taxCategory(null)
            .state(null)
            .categories(emptyList())
            .categoryOrderHints(null)
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final Optional<Product> createdProductOptional = productService.createProduct(productDraft1)
                                                                          .toCompletableFuture().join();
        assertThat(createdProductOptional).isEmpty();
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).contains("A duplicate value '\\\"english-slug\\\"' exists for field"
            + " 'slug.en' on");

        //assert CTP state
        final Optional<Product> productOptional = CTP_TARGET_CLIENT
            .execute(ProductQuery.of()
                                 .withPredicates(QueryPredicate.of(format("key = \"%s\"", newKey))))
            .toCompletableFuture().join().head();
        assertThat(productOptional).isEmpty();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void updateProduct_WithValidChanges_ShouldUpdateProductCorrectly() {
        final String newProductName = "This is my new name!";
        final ChangeName changeNameUpdateAction = ChangeName
            .of(LocalizedString.of(Locale.GERMAN, newProductName));

        final Product updatedProduct = productService
            .updateProduct(product, Collections.singletonList(changeNameUpdateAction))
            .toCompletableFuture().join();
        assertThat(updatedProduct).isNotNull();

        //assert CTP state
        final Optional<Product> fetchedProductOptional = CTP_TARGET_CLIENT
            .execute(ProductQuery.of()
                                 .withPredicates(QueryPredicate.of(format("key = \"%s\"", product.getKey()))))
            .toCompletableFuture().join().head();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(fetchedProductOptional).isNotEmpty();
        final Product fetchedProduct = fetchedProductOptional.get();
        assertThat(fetchedProduct.getMasterData().getCurrent().getName())
            .isEqualTo(updatedProduct.getMasterData().getCurrent().getName());
        assertThat(fetchedProduct.getMasterData().getCurrent().getSlug())
            .isEqualTo(updatedProduct.getMasterData().getCurrent().getSlug());
        assertThat(fetchedProduct.getKey()).isEqualTo(updatedProduct.getKey());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void updateProduct_WithInvalidChanges_ShouldNotUpdateProduct() {
        final ProductDraft productDraft1 = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            productType.toReference())
            .categories(emptyList())
            .taxCategory(null)
            .state(null)
            .categoryOrderHints(null)
            .build();
        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft1)).toCompletableFuture().join();


        final ChangeSlug changeSlugUpdateAction = ChangeSlug.of(productDraft1.getSlug());

        productService.updateProduct(product, Collections.singletonList(changeSlugUpdateAction))
                      .exceptionally(exception -> {
                          assertThat(exception).isNotNull();
                          assertThat(exception.getMessage()).contains(format("A duplicate value '\"%s\"' exists for "
                              + "field 'slug.en'", productDraft1.getSlug().get(Locale.ENGLISH)));
                          return null;
                      })
                      .toCompletableFuture().join();


        //assert CTP state
        final Optional<Product> fetchedProductOptional = CTP_TARGET_CLIENT
            .execute(ProductQuery.of()
                                 .withPredicates(QueryPredicate.of(format("key = \"%s\"", product.getKey()))))
            .toCompletableFuture().join().head();

        assertThat(fetchedProductOptional).isNotEmpty();
        final Product fetchedProduct = fetchedProductOptional.get();
        assertThat(fetchedProduct.getMasterData().getCurrent().getSlug()).isNotEqualTo(productDraft1.getSlug());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void updateProduct_WithMoreThan500Actions_ShouldNotFail() {
        // Update the product 501 times with a different name every time.
        final int numberOfUpdateActions = 501;
        final List<UpdateAction<Product>> updateActions =
            IntStream.range(1, numberOfUpdateActions + 1)
                     .mapToObj(i -> ChangeName.of(LocalizedString.of(Locale.GERMAN, format("name:%s", i))))
                     .collect(Collectors.toList());


        final Product updatedProduct = productService.updateProduct(product, updateActions)
                                                     .toCompletableFuture().join();
        assertThat(updatedProduct).isNotNull();

        //assert CTP state
        final Optional<Product> fetchedProductOptional = CTP_TARGET_CLIENT
            .execute(ProductQuery.of()
                                 .withPredicates(QueryPredicate.of(format("key = \"%s\"", product.getKey()))))
            .toCompletableFuture().join().head();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(fetchedProductOptional).isNotEmpty();
        final Product fetchedProduct = fetchedProductOptional.get();

        // Test that the fetched product has the name of the last update action that was applied.
        assertThat(fetchedProduct.getMasterData().getStaged().getName())
            .isEqualTo(LocalizedString.of(Locale.GERMAN, format("name:%s", numberOfUpdateActions)));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void updateProduct_WithMoreThan500ImageAdditions_ShouldHaveAllNewImages() {
        final Integer productMasterVariantId = product.getMasterData().getStaged().getMasterVariant().getId();

        // Update the product by adding 600 images in separate update actions
        final int numberOfImages = 600;
        final List<Image> addedImages = new ArrayList<>();
        final List<UpdateAction<Product>> updateActions =
            IntStream.range(1, numberOfImages + 1)
                     .mapToObj(i -> {
                         final Image newExternalImage = Image.of(format("image#%s", i), ImageDimensions.of(10, 10));
                         addedImages.add(newExternalImage); // keep track of added images.
                         return AddExternalImage.of(newExternalImage, productMasterVariantId);
                     })
                     .collect(Collectors.toList());

        final Product updatedProduct = productService.updateProduct(product, updateActions)
                                                     .toCompletableFuture().join();
        assertThat(updatedProduct).isNotNull();

        //assert CTP state
        final Optional<Product> fetchedProductOptional = CTP_TARGET_CLIENT
            .execute(ProductQuery.of()
                                 .withPredicates(QueryPredicate.of(format("key = \"%s\"", product.getKey()))))
            .toCompletableFuture().join().head();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(fetchedProductOptional).isNotEmpty();

        final Product fetchedProduct = fetchedProductOptional.get();
        assertThat(fetchedProduct.getVersion()).isEqualTo(numberOfImages + 1);
        // Test that the fetched product has exactly the 600 images added before.
        final List<Image> currentMasterVariantImages = fetchedProduct.getMasterData().getStaged()
                                                                     .getMasterVariant().getImages();
        assertThat(currentMasterVariantImages).containsAll(addedImages);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void publishProduct_ThatIsAlreadyPublished_ShouldThrowException() {
        productService.publishProduct(product)
                      .exceptionally(exception -> {
                          assertThat(exception).isExactlyInstanceOf(ErrorResponseException.class);
                          assertThat(exception.getMessage()).containsIgnoringCase("Product is published and has no "
                              + "staged changes");
                          assertThat(errorCallBackExceptions).isEmpty();
                          assertThat(errorCallBackMessages).isEmpty();
                          return null;
                      }).toCompletableFuture().join();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void publishProduct_ThatHasStagedChanges_ShouldPublishStagedChanges() {
        final String newProductName = "This is my new name!";
        final ChangeName changeNameUpdateAction = ChangeName
            .of(LocalizedString.of(Locale.GERMAN, newProductName));

        final Product updatedProduct = productService
            .updateProduct(product, Collections.singletonList(changeNameUpdateAction)).toCompletableFuture().join();

        assertThat(updatedProduct.getMasterData().isPublished()).isTrue();
        assertThat(updatedProduct.getMasterData().hasStagedChanges()).isTrue();
        assertThat(updatedProduct.getMasterData().getCurrent().getName())
            .isNotEqualTo(updatedProduct.getMasterData().getStaged().getName());

        final Product publishedProduct = productService.publishProduct(updatedProduct).toCompletableFuture().join();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(publishedProduct).isNotNull();
        assertThat(publishedProduct.getMasterData().getCurrent().getName())
            .isEqualTo(publishedProduct.getMasterData().getStaged().getName());
        assertThat(publishedProduct.getMasterData().isPublished()).isTrue();
        assertThat(publishedProduct.getMasterData().hasStagedChanges()).isFalse();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void publishProduct_ThatIsUnPublished_ShouldPublishProductCorrectly() {
        final String newKey = "newKey";
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            productType.toReference())
            .key(newKey)
            .categories(emptyList())
            .categoryOrderHints(null)
            .taxCategory(null)
            .state(null)
            .slug(LocalizedString.of(Locale.ENGLISH, "newSlug"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .publish(false)
            .build();

        final Optional<Product> newProductOptional =
            productService.createProduct(productDraft).toCompletableFuture().join();

        assertThat(newProductOptional).isPresent();
        final Product newProduct = newProductOptional.get();
        assertThat(newProduct.getMasterData().isPublished()).isFalse();
        assertThat(newProduct.getMasterData().getCurrent()).isNull();

        final Product publishedProduct = productService.publishProduct(newProduct)
                                                       .toCompletableFuture().join();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(publishedProduct).isNotNull();
        assertThat(publishedProduct.getMasterData().getCurrent()).isNotNull();
        assertThat(publishedProduct.getMasterData().getCurrent().getName())
            .isEqualTo(productDraft.getName());
        assertThat(publishedProduct.getMasterData().getCurrent().getSlug())
            .isEqualTo(productDraft.getSlug());
        assertThat(publishedProduct.getKey()).isEqualTo(productDraft.getKey());
        assertThat(publishedProduct.getMasterData().isPublished()).isTrue();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void revertProduct_ThatHasNoStagedChanges_ShouldThrowException() {
        productService.revertProduct(product)
                      .exceptionally(exception -> {
                          assertThat(exception).isExactlyInstanceOf(ErrorResponseException.class);
                          assertThat(exception.getMessage()).containsIgnoringCase("No staged changes present in "
                              + "catalog master");
                          assertThat(errorCallBackExceptions).isEmpty();
                          assertThat(errorCallBackMessages).isEmpty();
                          return null;
                      }).toCompletableFuture().join();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void revertProduct_ThatHasStagedChanges_ShouldRevertStagedChanges() {
        final String newProductName = "This is my new name!";
        final ChangeName changeNameUpdateAction = ChangeName
            .of(LocalizedString.of(Locale.GERMAN, newProductName));

        final Product updatedProduct = productService
            .updateProduct(product, Collections.singletonList(changeNameUpdateAction)).toCompletableFuture().join();

        assertThat(updatedProduct.getMasterData().isPublished()).isTrue();
        assertThat(updatedProduct.getMasterData().hasStagedChanges()).isTrue();
        assertThat(updatedProduct.getMasterData().getCurrent().getName())
            .isNotEqualTo(updatedProduct.getMasterData().getStaged().getName());

        final Product revertedProduct = productService.revertProduct(updatedProduct).toCompletableFuture().join();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(revertedProduct).isNotNull();
        assertThat(revertedProduct.getMasterData().getCurrent().getName())
            .isEqualTo(revertedProduct.getMasterData().getStaged().getName());
        assertThat(revertedProduct.getMasterData().isPublished()).isTrue();
        assertThat(revertedProduct.getMasterData().hasStagedChanges()).isFalse();
    }

    @Test
    public void fetchProduct_WithExistingKey_ShouldReturnProduct() {
        final Optional<Product> fetchedProductOptional = productService.fetchProduct(product.getKey())
                                                                       .toCompletableFuture()
                                                                       .join();
        assertThat(fetchedProductOptional).isNotEmpty();
        assertThat(fetchedProductOptional).contains(product);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchProduct_WithNonExistingKey_ShouldNotReturnProduct() {
        final Optional<Product> fetchedProductOptional = productService.fetchProduct("someNonExistingKey")
                                                                       .toCompletableFuture()
                                                                       .join();
        assertThat(fetchedProductOptional).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchProduct_WithNullKey_ShouldNotReturnProduct() {
        final Optional<Product> fetchedProductOptional = productService.fetchProduct(null)
                                                                       .toCompletableFuture()
                                                                       .join();
        assertThat(fetchedProductOptional).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchProduct_WithBlankKey_ShouldNotReturnProduct() {
        final Optional<Product> fetchedProductOptional = productService.fetchProduct(StringUtils.EMPTY)
                                                                       .toCompletableFuture()
                                                                       .join();
        assertThat(fetchedProductOptional).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchProduct_WithBadGatewayException_ShouldFail() {
        // Mock sphere client to return BadeGatewayException on any request.
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(ProductQuery.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
            .thenCallRealMethod();
        final ProductSyncOptions spyOptions = ProductSyncOptionsBuilder.of(spyClient)
                                                                       .errorCallback(
                                                                           (errorMessage, exception) -> {
                                                                               errorCallBackMessages
                                                                                   .add(errorMessage);
                                                                               errorCallBackExceptions
                                                                                   .add(exception);
                                                                           })
                                                                       .build();
        final ProductService spyProductService = new ProductServiceImpl(spyOptions);


        final String productKey = product.getKey();
        final Optional<Product> fetchedProductOptional = spyProductService.fetchProduct(productKey)
                                                                          .toCompletableFuture()
                                                                          .join();
        assertThat(fetchedProductOptional).isNotNull();
        assertThat(fetchedProductOptional).isEmpty();
        assertThat(errorCallBackExceptions).isNotEmpty();
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorCallBackMessages).isNotEmpty();
        assertThat(errorCallBackMessages.get(0))
            .isEqualToIgnoringCase(format("Failed to fetch products with keys: '%s'. Reason: %s", productKey,
                errorCallBackExceptions.get(0)));
    }
}
