package com.commercetools.sync.integration.services;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.impl.ProductServiceImpl;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
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
import java.util.stream.Collectors;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_2_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ProductServiceIT {
    private ProductService productService;
    private static ProductType productType;
    private static Set<ResourceIdentifier<Category>> categoryResourceIdentifiers;
    private static Set<ResourceIdentifier<Category>> categoryResourcesWithIds;
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
        categoryResourceIdentifiers = createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2))
            .stream()
            .map(category -> ResourceIdentifier.<Category>ofIdOrKey(category.getId(), category.getKey(),
                Category.referenceTypeId()))
            .collect(Collectors.toSet());
        categoryResourcesWithIds =
            categoryResourceIdentifiers.stream()
                            .map(categoryResourceIdentifier ->
                                ResourceIdentifier.<Category>ofId(categoryResourceIdentifier.getId(),
                                    Category.referenceTypeId()))
                            .collect(toSet());
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
                                                                               .setErrorCallBack(
                                                                                   (errorMessage, exception) -> {
                                                                                       errorCallBackMessages
                                                                                           .add(errorMessage);
                                                                                       errorCallBackExceptions
                                                                                           .add(exception);
                                                                                   })
                                                                               .setWarningCallBack(warningMessage ->
                                                                                   warningCallBackMessages
                                                                                       .add(warningMessage))
                                                                               .build();

        // Create a mock new product in the target project.
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH,
            productType.toReference(), categoryResourcesWithIds,
            createRandomCategoryOrderHints(categoryResourceIdentifiers));
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
    public void cacheKeysToIds_ShouldCacheProductKeysOnlyFirstCall() {
        Map<String, String> cache = productService.cacheKeysToIds().toCompletableFuture().join();
        assertThat(cache).hasSize(1);

        // Create new product without caching
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_2_RESOURCE_PATH, productType.toReference(),
            categoryResourcesWithIds, createRandomCategoryOrderHints(categoryResourceIdentifiers));

        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();

        cache = productService.cacheKeysToIds().toCompletableFuture().join();
        assertThat(cache).hasSize(1);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }


    @Test
    public void cacheKeysToIds_WithTargetProductsWithBlankKeys_ShouldGiveAWarningAboutKeyNotSetAndNotCacheKey() {
        // Create new product without key
        final ProductDraft productDraftWithNullKey = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            productType.toReference())
            .key(null)
            .build();

        final ProductDraft productDraftWithEmptyKey = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            productType.toReference())
            .key(StringUtils.EMPTY)
            .slug(LocalizedString.of(Locale.ENGLISH, "newSlug"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final Product productWithNullKey = CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraftWithNullKey))
                                                          .toCompletableFuture().join();
        final Product productWithEmptyKey = CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraftWithEmptyKey))
                                                            .toCompletableFuture().join();

        final Map<String, String> cache = productService.cacheKeysToIds().toCompletableFuture().join();
        assertThat(cache).hasSize(1);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).hasSize(2);
        // Since the order of fetch is not ensured, so we assert in whole list of warning messages (as string):
        assertThat(warningCallBackMessages.toString()).contains(format("Product with id: '%s' has no key set. Keys are"
            + " required for product matching.", productWithNullKey.getId()));
        assertThat(warningCallBackMessages.toString()).contains(format("Product with id: '%s' has no key set. Keys are"
            + " required for product matching.", productWithEmptyKey.getId()));
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
        final Set<String> keys =  new HashSet<>();
        keys.add(product.getKey());
        final Set<Product> fetchedProducts = productService.fetchMatchingProductsByKeys(keys)
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
                                                                         .setErrorCallBack(
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
            .categories(Collections.emptyList())
            .categoryOrderHints(null)
            .slug(LocalizedString.of(Locale.ENGLISH, "newSlug"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft productDraft2 = createProductDraft(PRODUCT_KEY_2_RESOURCE_PATH, productType.toReference(),
            categoryResourcesWithIds, createRandomCategoryOrderHints(categoryResourceIdentifiers));

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
            .categories(Collections.emptyList())
            .categoryOrderHints(null)
            .slug(LocalizedString.of(Locale.ENGLISH, "newSlug"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft productDraft2 = createProductDraft(PRODUCT_KEY_2_RESOURCE_PATH, productType.toReference(),
            categoryResourcesWithIds, createRandomCategoryOrderHints(categoryResourceIdentifiers));

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
            .categories(Collections.emptyList())
            .categoryOrderHints(null)
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft productDraft2 = createProductDraftBuilder(PRODUCT_KEY_1_RESOURCE_PATH,
            productType.toReference())
            .key("newKey1")
            .categories(Collections.emptyList())
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
        assertThat(errorCallBackMessages.get(0)).contains(" A duplicate value '\"rehruecken-o-kn\"' exists for field "
            + "'slug.en'");
        assertThat(errorCallBackMessages.get(1)).contains(" A duplicate value '\"rehruecken-o-kn\"' exists for field "
            + "'slug.en'");
        assertThat(createdProducts).isEmpty();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void createProduct_WithValidProduct_ShouldCreateProduct() {
        // create a draft based of the same existing product but with different key, slug and master variant SKU since
        // these values should be unique on CTP for the product to be created.
        final String newKey = "newKey";
        final ProductDraft productDraft1 = createProductDraftBuilder(PRODUCT_KEY_1_RESOURCE_PATH,
            productType.toReference())
            .key(newKey)
            .categories(Collections.emptyList())
            .categoryOrderHints(null)
            .slug(LocalizedString.of(Locale.ENGLISH, "newSlug"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
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
                                  .withPredicates(QueryPredicate.of(format("key = \"%s\"", newKey))))
            .toCompletableFuture().join().head();

        assertThat(productOptional).isNotEmpty();
        final Product fetchedProduct = productOptional.get();
        assertThat(fetchedProduct.getMasterData().getCurrent().getName())
            .isEqualTo(createdProduct.getMasterData().getCurrent().getName());
        assertThat(fetchedProduct.getMasterData().getCurrent().getSlug())
            .isEqualTo(createdProduct.getMasterData().getCurrent().getSlug());
        assertThat(fetchedProduct.getKey()).isEqualTo(newKey);
    }

    @Test
    public void createProduct_WithInvalidProduct_ShouldNotCreateProduct() {
        // Create product with same slug as existing product
        final String newKey = "newKey";
        final ProductDraft productDraft1 = createProductDraftBuilder(PRODUCT_KEY_1_RESOURCE_PATH,
            productType.toReference())
            .key(newKey)
            .categories(Collections.emptyList())
            .categoryOrderHints(null)
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final Optional<Product> createdProductOptional = productService.createProduct(productDraft1)
                                                                          .toCompletableFuture().join();
        assertThat(createdProductOptional).isEmpty();
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).contains("A duplicate value '\\\"rehruecken-o-kn\\\"' exists for field"
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
        final Optional<Product> productOptional = CTP_TARGET_CLIENT
            .execute(ProductQuery.of()
                                 .withPredicates(QueryPredicate.of(format("key = \"%s\"", product.getKey()))))
            .toCompletableFuture().join().head();

        final String newProductName = "This is my new name!";
        final ChangeName changeNameUpdateAction = ChangeName
            .of(LocalizedString.of(Locale.GERMAN, newProductName));

        final Product updatedProduct = productService
            .updateProduct(productOptional.get(), Collections.singletonList(changeNameUpdateAction))
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
            .categories(Collections.emptyList())
            .categoryOrderHints(null)
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
    @SuppressWarnings("ConstantConditions")
    public void updateProduct_WithInvalidChanges_ShouldNotUpdateProduct() {
        final ProductDraft productDraft1 = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            productType.toReference())
            .categories(Collections.emptyList())
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
    public void fetchProduct_WithExistingKey_ShouldReturnProduct() {
        final Optional<Product> fetchedProductOptional = productService.fetchProduct(product.getKey())
                                                                       .toCompletableFuture()
                                                                       .join();
        assertThat(fetchedProductOptional).isNotEmpty();
        final Product fetchedProduct = fetchedProductOptional.get();
        assertThat(fetchedProduct.getId()).isEqualTo(product.getId());
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
                                                                       .setErrorCallBack(
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
