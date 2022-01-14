package com.commercetools.sync.integration.services.impl;

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
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import io.sphere.sdk.models.errors.DuplicateFieldError;
import io.sphere.sdk.products.Image;
import io.sphere.sdk.products.ImageDimensions;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductProjectionType;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.AddExternalImage;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import io.sphere.sdk.products.commands.updateactions.SetKey;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductServiceImplIT {
  private ProductService productService;
  private static ProductType productType;
  private static List<Reference<Category>> categoryReferencesWithIds;
  private ProductProjection product;

  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  /**
   * Delete all product related test data from target project. Then creates custom types for target
   * CTP project categories.
   */
  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    createCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY,
        Locale.ENGLISH,
        OLD_CATEGORY_CUSTOM_TYPE_NAME,
        CTP_TARGET_CLIENT);
    final List<Category> categories =
        createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
    categoryReferencesWithIds = getReferencesWithIds(categories);
    productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
  }

  /**
   * Deletes Products and Types from target CTP projects, then it populates target CTP project with
   * product test data.
   */
  @BeforeEach
  void setupTest() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
    deleteAllProducts(CTP_TARGET_CLIENT);

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();

    // Create a mock new product in the target project.
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            productType.toReference(),
            null,
            null,
            categoryReferencesWithIds,
            createRandomCategoryOrderHints(categoryReferencesWithIds));
    product =
        CTP_TARGET_CLIENT
            .execute(ProductCreateCommand.of(productDraft))
            .thenApply(p -> p.toProjection(ProductProjectionType.STAGED))
            .toCompletableFuture()
            .join();

    productService = new ProductServiceImpl(productSyncOptions);
  }

  /** Cleans up the target test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
  }

  @Test
  void getIdFromCacheOrFetch_WithNotCachedExistingProduct_ShouldFetchProduct() {
    final Optional<String> productId =
        productService.getIdFromCacheOrFetch(product.getKey()).toCompletableFuture().join();
    assertThat(productId).isNotEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void getIdFromCacheOrFetch_WithNullProductKey_ShouldReturnEmptyOptional() {
    final Optional<String> productId =
        productService.getIdFromCacheOrFetch(null).toCompletableFuture().join();
    assertThat(productId).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void getIdFromCacheOrFetch_WithCachedExistingProduct_ShouldFetchFromCache() {
    final String oldKey = product.getKey();
    final Optional<String> oldProductId =
        productService.getIdFromCacheOrFetch(oldKey).toCompletableFuture().join();

    // Change product key on ctp
    final String newKey = "newKey";
    productService
        .updateProduct(product, Collections.singletonList(SetKey.of(newKey)))
        .toCompletableFuture()
        .join();

    // Fetch product from cache
    final Optional<String> cachedProductId =
        productService.getIdFromCacheOrFetch(oldKey).toCompletableFuture().join();

    assertThat(cachedProductId).isNotEmpty();
    assertThat(cachedProductId).isEqualTo(oldProductId);

    // Fetch product from ctp (because of new key not cached)
    final Optional<String> productId =
        productService.getIdFromCacheOrFetch(newKey).toCompletableFuture().join();

    assertThat(productId).isNotEmpty();
    // Both keys point to the same id.
    assertThat(productId).isEqualTo(cachedProductId);

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void cacheKeysToIds_WithEmptyKeys_ShouldReturnCurrentCache() {
    Map<String, String> cache =
        productService.cacheKeysToIds(emptySet()).toCompletableFuture().join();
    assertThat(cache).hasSize(0); // Since cache is empty

    cache = productService.cacheKeysToIds(singleton(product.getKey())).toCompletableFuture().join();
    assertThat(cache).hasSize(1);

    cache = productService.cacheKeysToIds(emptySet()).toCompletableFuture().join();
    assertThat(cache).hasSize(1); // Since cache has been fed with a product key

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void cacheKeysToIds_WithAlreadyCachedKeys_ShouldNotMakeRequestsAndReturnCurrentCache() {
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();
    final ProductService spyProductService = new ProductServiceImpl(productSyncOptions);

    final ProductDraft productDraft1 =
        createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType.toReference())
            .categories(emptyList())
            .taxCategory(null)
            .state(null)
            .categoryOrderHints(null)
            .build();
    Product product2 =
        CTP_TARGET_CLIENT
            .execute(ProductCreateCommand.of(productDraft1))
            .toCompletableFuture()
            .join();

    Set<String> keys =
        Arrays.asList(product.getKey(), product2.getKey()).stream().collect(Collectors.toSet());
    Map<String, String> cache = spyProductService.cacheKeysToIds(keys).toCompletableFuture().join();
    assertThat(cache).hasSize(2);

    // Attempt to cache same (already cached) key.
    cache =
        spyProductService.cacheKeysToIds(singleton(product.getKey())).toCompletableFuture().join();
    assertThat(cache).hasSize(2);
    assertThat(cache).containsKeys(product.getKey(), product2.getKey());

    // verify only 1 request was made to fetch id the first time, but not second time since it's
    // already in cache.
    verify(spyClient, times(1)).execute(any());
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void cacheKeysToIds_WithSomeEmptyKeys_ShouldReturnCorrectCache() {
    final Set<String> productKeys = new HashSet<>();
    productKeys.add(product.getKey());
    productKeys.add(null);
    productKeys.add("");
    Map<String, String> cache =
        productService.cacheKeysToIds(productKeys).toCompletableFuture().join();
    assertThat(cache).hasSize(1);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingProductsByKeys_WithEmptySetOfKeys_ShouldReturnEmptySet() {
    final Set<ProductProjection> fetchedProducts =
        productService
            .fetchMatchingProductsByKeys(Collections.emptySet())
            .toCompletableFuture()
            .join();
    assertThat(fetchedProducts).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingProductsByKeys_WithAllExistingSetOfKeys_ShouldReturnSetOfProducts() {
    final Set<ProductProjection> fetchedProducts =
        productService
            .fetchMatchingProductsByKeys(singleton(product.getKey()))
            .toCompletableFuture()
            .join();
    assertThat(fetchedProducts).hasSize(1);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingProductsByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
    // preparation
    // Mock sphere client to return BadGatewayException on any request.
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    when(spyClient.execute(any(ProductProjectionQuery.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
        .thenCallRealMethod();
    final ProductSyncOptions spyOptions =
        ProductSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();
    final ProductService spyProductService = new ProductServiceImpl(spyOptions);

    final Set<String> keys = new HashSet<>();
    keys.add(product.getKey());

    // test and assert
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(spyProductService.fetchMatchingProductsByKeys(keys))
        .failsWithin(10, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  void fetchMatchingProductsByKeys_WithSomeExistingSetOfKeys_ShouldReturnSetOfProducts() {
    final Set<String> keys = new HashSet<>();
    keys.add(product.getKey());
    keys.add("new-key");
    final Set<ProductProjection> fetchedProducts =
        productService.fetchMatchingProductsByKeys(keys).toCompletableFuture().join();
    assertThat(fetchedProducts).hasSize(1);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingProductsByKeys_WithAllExistingSetOfKeys_ShouldCacheFetchedProductsIds() {
    final String oldKey = product.getKey();
    final Set<ProductProjection> fetchedProducts =
        productService.fetchMatchingProductsByKeys(singleton(oldKey)).toCompletableFuture().join();
    assertThat(fetchedProducts).hasSize(1);

    // Change product oldKey on ctp
    final String newKey = "newKey";
    productService
        .updateProduct(product, Collections.singletonList(SetKey.of(newKey)))
        .toCompletableFuture()
        .join();

    // Fetch cached id by old key
    final Optional<String> cachedProductId =
        productService.getIdFromCacheOrFetch(oldKey).toCompletableFuture().join();

    assertThat(cachedProductId).isNotEmpty();
    assertThat(cachedProductId).contains(product.getId());
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void createProduct_WithValidProduct_ShouldCreateProductAndCacheId() {
    // preparation
    final ProductDraft productDraft1 =
        createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType.toReference())
            .taxCategory(null)
            .state(null)
            .categories(emptyList())
            .categoryOrderHints(null)
            .build();

    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    final ProductSyncOptions spyOptions =
        ProductSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final ProductService spyProductService = new ProductServiceImpl(spyOptions);

    // test
    final Optional<ProductProjection> createdProductOptional =
        spyProductService.createProduct(productDraft1).toCompletableFuture().join();

    // assertion
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();

    // assert CTP state
    final Optional<ProductProjection> queriedOptional =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionQuery.ofCurrent()
                    .withPredicates(
                        QueryPredicate.of(format("key = \"%s\"", productDraft1.getKey()))))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(queriedOptional)
        .hasValueSatisfying(
            queried ->
                assertThat(createdProductOptional)
                    .hasValueSatisfying(
                        created -> {
                          assertThat(queried.getKey()).isEqualTo(created.getKey());
                          assertThat(queried.getName()).isEqualTo(created.getName());
                          assertThat(queried.getSlug()).isEqualTo(created.getSlug());
                        }));

    // Assert that the created product is cached
    final Optional<String> productId =
        spyProductService
            .getIdFromCacheOrFetch(productDraft1.getKey())
            .toCompletableFuture()
            .join();
    assertThat(productId).isPresent();
    verify(spyClient, times(0)).execute(any(ProductTypeQuery.class));
  }

  @Test
  void createProduct_WithBlankKey_ShouldNotCreateProduct() {
    // preparation
    final String newKey = "";
    final ProductDraft productDraft1 =
        createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType.toReference())
            .key(newKey)
            .taxCategory(null)
            .state(null)
            .categories(emptyList())
            .categoryOrderHints(null)
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

    // test
    final Optional<ProductProjection> createdProductOptional =
        productService.createProduct(productDraft1).toCompletableFuture().join();

    // assertion
    assertThat(createdProductOptional).isEmpty();
    assertThat(errorCallBackMessages)
        .containsExactly("Failed to create draft with key: ''. Reason: Draft key is blank!");
  }

  @Test
  void createProduct_WithDuplicateSlug_ShouldNotCreateProduct() {
    // Create product with same slug as existing product
    final String newKey = "newKey";
    final ProductDraft productDraft1 =
        createProductDraftBuilder(PRODUCT_KEY_1_RESOURCE_PATH, productType.toReference())
            .key(newKey)
            .taxCategory(null)
            .state(null)
            .categories(emptyList())
            .categoryOrderHints(null)
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

    final Optional<ProductProjection> createdProductOptional =
        productService.createProduct(productDraft1).toCompletableFuture().join();

    assertThat(createdProductOptional).isEmpty();
    final String duplicatedSlug = "english-slug";
    assertThat(errorCallBackExceptions)
        .hasSize(1)
        .allSatisfy(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(ErrorResponseException.class);
              final ErrorResponseException errorResponse = ((ErrorResponseException) exception);

              final List<DuplicateFieldError> fieldErrors =
                  errorResponse.getErrors().stream()
                      .map(
                          sphereError -> {
                            assertThat(sphereError.getCode()).isEqualTo(DuplicateFieldError.CODE);
                            return sphereError.as(DuplicateFieldError.class);
                          })
                      .collect(toList());
              assertThat(fieldErrors).hasSize(1);
              assertThat(fieldErrors)
                  .allSatisfy(
                      error -> {
                        assertThat(error.getField()).isEqualTo("slug.en");
                        assertThat(error.getDuplicateValue()).isEqualTo(duplicatedSlug);
                      });
            });

    assertThat(errorCallBackMessages)
        .hasSize(1)
        .allSatisfy(
            errorMessage -> {
              assertThat(errorMessage).contains("\"code\" : \"DuplicateField\"");
              assertThat(errorMessage).contains("\"field\" : \"slug.en\"");
              assertThat(errorMessage).contains("\"duplicateValue\" : \"" + duplicatedSlug + "\"");
            });

    // assert CTP state
    final Optional<ProductProjection> productOptional =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionQuery.ofStaged()
                    .withPredicates(QueryPredicate.of(format("key = \"%s\"", newKey))))
            .toCompletableFuture()
            .join()
            .head();
    assertThat(productOptional).isEmpty();
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  void updateProduct_WithValidChanges_ShouldUpdateProductCorrectly() {
    final String newProductName = "This is my new name!";
    final ChangeName changeNameUpdateAction =
        ChangeName.of(LocalizedString.of(Locale.GERMAN, newProductName));

    final ProductProjection updatedProduct =
        productService
            .updateProduct(product, Collections.singletonList(changeNameUpdateAction))
            .toCompletableFuture()
            .join();
    assertThat(updatedProduct).isNotNull();

    // assert CTP state
    final Optional<ProductProjection> fetchedProductOptional =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionQuery.ofStaged()
                    .withPredicates(QueryPredicate.of(format("key = \"%s\"", product.getKey()))))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(fetchedProductOptional).isNotEmpty();
    final ProductProjection fetchedProduct = fetchedProductOptional.get();
    assertThat(fetchedProduct.getName()).isEqualTo(updatedProduct.getName());
    assertThat(fetchedProduct.getSlug()).isEqualTo(updatedProduct.getSlug());
    assertThat(fetchedProduct.getKey()).isEqualTo(updatedProduct.getKey());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  void updateProduct_WithInvalidChanges_ShouldNotUpdateProduct() {
    final ProductDraft productDraft1 =
        createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType.toReference())
            .categories(emptyList())
            .taxCategory(null)
            .state(null)
            .categoryOrderHints(null)
            .build();
    CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft1)).toCompletableFuture().join();

    final ChangeSlug changeSlugUpdateAction = ChangeSlug.of(productDraft1.getSlug());

    productService
        .updateProduct(product, Collections.singletonList(changeSlugUpdateAction))
        .exceptionally(
            exception -> {
              assertThat(exception).isNotNull();

              assertThat(exception).isExactlyInstanceOf(CompletionException.class);
              assertThat(exception.getCause()).isExactlyInstanceOf(ErrorResponseException.class);
              final ErrorResponseException errorResponse =
                  ((ErrorResponseException) exception.getCause());

              final List<DuplicateFieldError> fieldErrors =
                  errorResponse.getErrors().stream()
                      .map(
                          sphereError -> {
                            assertThat(sphereError.getCode()).isEqualTo(DuplicateFieldError.CODE);
                            return sphereError.as(DuplicateFieldError.class);
                          })
                      .collect(toList());
              assertThat(fieldErrors).hasSize(1);
              assertThat(fieldErrors)
                  .allSatisfy(
                      error -> {
                        assertThat(error.getField()).isEqualTo("slug.en");
                        assertThat(error.getDuplicateValue())
                            .isEqualTo(productDraft1.getSlug().get(Locale.ENGLISH));
                      });
              return null;
            })
        .toCompletableFuture()
        .join();

    // assert CTP state
    final Optional<ProductProjection> fetchedProductOptional =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionQuery.ofCurrent()
                    .withPredicates(QueryPredicate.of(format("key = \"%s\"", product.getKey()))))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(fetchedProductOptional).isNotEmpty();
    final ProductProjection fetchedProduct = fetchedProductOptional.get();
    assertThat(fetchedProduct.getSlug()).isNotEqualTo(productDraft1.getSlug());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  void updateProduct_WithMoreThan500Actions_ShouldNotFail() {
    // Update the product 501 times with a different name every time.
    final int numberOfUpdateActions = 501;
    final List<UpdateAction<Product>> updateActions =
        IntStream.range(1, numberOfUpdateActions + 1)
            .mapToObj(i -> ChangeName.of(LocalizedString.of(Locale.GERMAN, format("name:%s", i))))
            .collect(Collectors.toList());

    final ProductProjection updatedProduct =
        productService.updateProduct(product, updateActions).toCompletableFuture().join();
    assertThat(updatedProduct).isNotNull();

    // assert CTP state
    final Optional<ProductProjection> fetchedProductOptional =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionQuery.ofStaged()
                    .withPredicates(QueryPredicate.of(format("key = \"%s\"", product.getKey()))))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(fetchedProductOptional).isNotEmpty();
    final ProductProjection fetchedProduct = fetchedProductOptional.get();

    // Test that the fetched product has the name of the last update action that was applied.
    assertThat(fetchedProduct.getName())
        .isEqualTo(LocalizedString.of(Locale.GERMAN, format("name:%s", numberOfUpdateActions)));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  void updateProduct_WithMoreThan500ImageAdditions_ShouldHaveAllNewImages() {
    final Integer productMasterVariantId = product.getMasterVariant().getId();

    // Update the product by adding 600 images in separate update actions
    final int numberOfImages = 600;
    final List<Image> addedImages = new ArrayList<>();
    final List<UpdateAction<Product>> updateActions =
        IntStream.range(1, numberOfImages + 1)
            .mapToObj(
                i -> {
                  final Image newExternalImage =
                      Image.of(format("image#%s", i), ImageDimensions.of(10, 10));
                  addedImages.add(newExternalImage); // keep track of added images.
                  return AddExternalImage.of(newExternalImage, productMasterVariantId);
                })
            .collect(Collectors.toList());

    final ProductProjection updatedProduct =
        productService.updateProduct(product, updateActions).toCompletableFuture().join();
    assertThat(updatedProduct).isNotNull();

    // assert CTP state
    final Optional<ProductProjection> fetchedProductOptional =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionQuery.ofStaged()
                    .withPredicates(QueryPredicate.of(format("key = \"%s\"", product.getKey()))))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(fetchedProductOptional).isNotEmpty();

    final ProductProjection fetchedProduct = fetchedProductOptional.get();
    // Test that the fetched product has exactly the 600 images added before.
    final List<Image> currentMasterVariantImages = fetchedProduct.getMasterVariant().getImages();
    assertThat(currentMasterVariantImages).containsAll(addedImages);
  }

  @Test
  void fetchProduct_WithExistingKey_ShouldReturnProduct() {
    final Optional<ProductProjection> fetchedProductOptional =
        productService.fetchProduct(product.getKey()).toCompletableFuture().join();
    assertThat(fetchedProductOptional).isNotEmpty();
    assertThat(fetchedProductOptional.get().getId()).contains(product.getId());
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchProduct_WithNonExistingKey_ShouldNotReturnProduct() {
    final Optional<ProductProjection> fetchedProductOptional =
        productService.fetchProduct("someNonExistingKey").toCompletableFuture().join();
    assertThat(fetchedProductOptional).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchProduct_WithNullKey_ShouldNotReturnProduct() {
    final Optional<ProductProjection> fetchedProductOptional =
        productService.fetchProduct(null).toCompletableFuture().join();
    assertThat(fetchedProductOptional).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchProduct_WithBlankKey_ShouldNotReturnProduct() {
    final Optional<ProductProjection> fetchedProductOptional =
        productService.fetchProduct(StringUtils.EMPTY).toCompletableFuture().join();
    assertThat(fetchedProductOptional).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchProduct_WithBadGatewayException_ShouldFail() {
    // preparation
    // Mock sphere client to return BadGatewayException on any request.
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    when(spyClient.execute(any(ProductProjectionQuery.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
        .thenCallRealMethod();
    final ProductSyncOptions spyOptions =
        ProductSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();
    final ProductService spyProductService = new ProductServiceImpl(spyOptions);

    final String productKey = product.getKey();

    // test and assertion
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(spyProductService.fetchProduct(productKey))
        .failsWithin(10, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }
}
