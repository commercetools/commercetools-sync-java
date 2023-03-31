package com.commercetools.sync.integration.sdk2.services.impl;

import static com.commercetools.sync.integration.sdk2.commons.utils.CategoryITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static com.spotify.futures.CompletableFutures.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyProductProjectionsGet;
import com.commercetools.api.client.ByProjectKeyProductProjectionsKeyByKeyGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.common.Image;
import com.commercetools.api.models.common.ImageBuilder;
import com.commercetools.api.models.common.ImageDimensionsBuilder;
import com.commercetools.api.models.common.LocalizedStringBuilder;
import com.commercetools.api.models.error.DuplicateFieldError;
import com.commercetools.api.models.product.CategoryOrderHints;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductAddExternalImageActionBuilder;
import com.commercetools.api.models.product.ProductChangeNameAction;
import com.commercetools.api.models.product.ProductChangeNameActionBuilder;
import com.commercetools.api.models.product.ProductChangeSlugAction;
import com.commercetools.api.models.product.ProductChangeSlugActionBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductMixin;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionPagedQueryResponse;
import com.commercetools.api.models.product.ProductProjectionType;
import com.commercetools.api.models.product.ProductSetKeyActionBuilder;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifier;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.services.ProductService;
import com.commercetools.sync.sdk2.services.impl.ProductServiceImpl;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
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
  private static List<CategoryReference> categoryReferencesWithIds;
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
            .products()
            .create(productDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(p -> ProductMixin.toProjection(p, ProductProjectionType.STAGED))
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
        .updateProduct(
            product, Collections.singletonList(ProductSetKeyActionBuilder.of().key(newKey).build()))
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
    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);

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
        createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType.toResourceIdentifier())
            .categories(emptyList())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categoryOrderHints((CategoryOrderHints) null)
            .build();
    final Product product2 =
        CTP_TARGET_CLIENT
            .products()
            .create(productDraft1)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

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
    verify(spyClient, times(1)).graphql();
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
    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);
    when(spyClient.productProjections()).thenReturn(mock());
    final ByProjectKeyProductProjectionsGet getMock = mock(ByProjectKeyProductProjectionsGet.class);
    when(spyClient.productProjections().get()).thenReturn(getMock);
    when(getMock.withWhere(any(String.class))).thenReturn(getMock);
    when(getMock.withPredicateVar(any(String.class), any())).thenReturn(getMock);
    when(getMock.withLimit(any(Integer.class))).thenReturn(getMock);
    when(getMock.withWithTotal(any(Boolean.class))).thenReturn(getMock);
    when(getMock.execute())
        .thenReturn(exceptionallyCompletedFuture(new BadGatewayException(500, "", null, "", null)))
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
        .updateProduct(
            product, Collections.singletonList(ProductSetKeyActionBuilder.of().key(newKey).build()))
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
        createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType.toResourceIdentifier())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categories(emptyList())
            .categoryOrderHints((CategoryOrderHints) null)
            .build();

    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);
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
    final ProductProjection created =
        spyProductService.createProduct(productDraft1).toCompletableFuture().join().get();

    // assertion
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();

    // assert CTP state
    final ProductProjection queried =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(productDraft1.getKey())
            .get()
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(queried.getKey()).isEqualTo(created.getKey());
    assertThat(queried.getName()).isEqualTo(created.getName());
    assertThat(queried.getSlug()).isEqualTo(created.getSlug());

    // Assert that the created product is cached
    final Optional<String> productId =
        spyProductService
            .getIdFromCacheOrFetch(productDraft1.getKey())
            .toCompletableFuture()
            .join();
    assertThat(productId).isPresent();
    verify(spyClient, times(0)).productTypes();
  }
  //
  @Test
  void createProduct_WithBlankKey_ShouldNotCreateProduct() {
    // preparation
    final String newKey = "";
    final ProductDraft productDraft1 =
        createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType.toResourceIdentifier())
            .key(newKey)
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categories(emptyList())
            .categoryOrderHints((CategoryOrderHints) null)
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
        createProductDraftBuilder(PRODUCT_KEY_1_RESOURCE_PATH, productType.toResourceIdentifier())
            .key(newKey)
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categories(emptyList())
            .categoryOrderHints((CategoryOrderHints) null)
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
              assertThat(exception).isExactlyInstanceOf(CompletionException.class);
              final CompletionException errorResponse = ((CompletionException) exception);

              final BadRequestException badRequestException =
                  (BadRequestException) errorResponse.getCause();

              final List<DuplicateFieldError> fieldErrors =
                  badRequestException.getErrorResponse().getErrors().stream()
                      .map(
                          error -> {
                            assertThat(error.getCode())
                                .isEqualTo(DuplicateFieldError.DUPLICATE_FIELD);
                            return (DuplicateFieldError) error;
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
    final ApiHttpResponse<ProductProjectionPagedQueryResponse> response =
        CTP_TARGET_CLIENT
            .productProjections()
            .get()
            .withWhere("key=:key")
            .withPredicateVar("key", newKey)
            .execute()
            .join();
    assertThat(response.getBody().getTotal()).isEqualTo(0);
  }

  @Test
  void updateProduct_WithValidChanges_ShouldUpdateProductCorrectly() {
    final String newProductName = "This is my new name!";

    final ProductChangeNameAction productChangeNameAction =
        ProductChangeNameActionBuilder.of()
            .name(
                LocalizedStringBuilder.of()
                    .addValue(Locale.GERMAN.toLanguageTag(), newProductName)
                    .build())
            .build();

    final ProductProjection updatedProduct =
        productService
            .updateProduct(product, Collections.singletonList(productChangeNameAction))
            .toCompletableFuture()
            .join();
    assertThat(updatedProduct).isNotNull();

    // assert CTP product
    final ProductProjection fetchedProductOptional =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(product.getKey())
            .get()
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(fetchedProductOptional).isNotNull();
    final ProductProjection fetchedProduct = fetchedProductOptional.get();
    assertThat(fetchedProduct.getName()).isEqualTo(updatedProduct.getName());
    assertThat(fetchedProduct.getSlug()).isEqualTo(updatedProduct.getSlug());
    assertThat(fetchedProduct.getKey()).isEqualTo(updatedProduct.getKey());
  }

  @Test
  void updateProduct_WithInvalidChanges_ShouldNotUpdateProduct() {
    final ProductDraft productDraft1 =
        createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType.toResourceIdentifier())
            .categories(emptyList())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categoryOrderHints((CategoryOrderHints) null)
            .build();

    CTP_TARGET_CLIENT.products().create(productDraft1).execute().join();

    final ProductChangeSlugAction changeSlugUpdateAction =
        ProductChangeSlugActionBuilder.of().slug(productDraft1.getSlug()).build();

    productService
        .updateProduct(product, Collections.singletonList(changeSlugUpdateAction))
        .exceptionally(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(CompletionException.class);
              final CompletionException errorResponse = ((CompletionException) exception);

              final BadRequestException badRequestException =
                  (BadRequestException) errorResponse.getCause();

              final List<DuplicateFieldError> fieldErrors =
                  badRequestException.getErrorResponse().getErrors().stream()
                      .map(
                          error -> {
                            assertThat(error.getCode())
                                .isEqualTo(DuplicateFieldError.DUPLICATE_FIELD);
                            return (DuplicateFieldError) error;
                          })
                      .collect(toList());
              assertThat(fieldErrors).hasSize(1);
              assertThat(fieldErrors)
                  .allSatisfy(
                      error -> {
                        assertThat(error.getField()).isEqualTo("slug.en");
                        assertThat(error.getDuplicateValue())
                            .isEqualTo(productDraft1.getSlug().get("en"));
                      });
              return null;
            })
        .toCompletableFuture()
        .join();

    // assert CTP state
    final ProductProjection fetchedProduct =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(product.getKey())
            .get()
            .execute()
            .join()
            .getBody();

    assertThat(fetchedProduct).isNotNull();
    assertThat(fetchedProduct.getSlug()).isNotEqualTo(productDraft1.getSlug());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  void updateProduct_WithMoreThan500Actions_ShouldNotFail() {
    // Update the product 501 times with a different name every time.
    final int numberOfUpdateActions = 501;
    final List<ProductUpdateAction> updateActions =
        IntStream.range(1, numberOfUpdateActions + 1)
            .mapToObj(
                i ->
                    ProductChangeNameActionBuilder.of()
                        .name(
                            LocalizedStringBuilder.of()
                                .addValue(Locale.GERMAN.toLanguageTag(), format("name:%s", i))
                                .build())
                        .build())
            .collect(Collectors.toList());

    final ProductProjection updatedProduct =
        productService.updateProduct(product, updateActions).toCompletableFuture().join();
    assertThat(updatedProduct).isNotNull();

    // assert CTP state
    final ProductProjection fetchedProduct =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(product.getKey())
            .get()
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(fetchedProduct).isNotNull();
    // Test that the fetched product has the name of the last update action that was applied.
    assertThat(fetchedProduct.getName())
        .isEqualTo(
            LocalizedStringBuilder.of()
                .addValue(Locale.GERMAN.toLanguageTag(), format("name:%s", numberOfUpdateActions))
                .build());
  }

  @Test
  void updateProduct_WithMoreThan500ImageAdditions_ShouldHaveAllNewImages() {
    final Long productMasterVariantId = product.getMasterVariant().getId();

    // Update the product by adding 600 images in separate update actions
    final int numberOfImages = 600;
    final List<Image> addedImages = new ArrayList<>();
    final List<ProductUpdateAction> updateActions =
        IntStream.range(1, numberOfImages + 1)
            .mapToObj(
                i -> {
                  final Image newExternalImage =
                      ImageBuilder.of()
                          .url(format("image#%s", i))
                          .dimensions(ImageDimensionsBuilder.of().w(10).h(10).build())
                          .build();
                  addedImages.add(newExternalImage); // keep track of added images.
                  return ProductAddExternalImageActionBuilder.of()
                      .image(newExternalImage)
                      .variantId(productMasterVariantId)
                      .build();
                })
            .collect(Collectors.toList());

    final ProductProjection updatedProduct =
        productService.updateProduct(product, updateActions).toCompletableFuture().join();
    assertThat(updatedProduct).isNotNull();

    // assert CTP state
    final ProductProjection fetchedProduct =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(product.getKey())
            .get()
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(fetchedProduct).isNotNull();

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
    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);
    when(spyClient.productProjections()).thenReturn(mock());
    when(spyClient.productProjections().withKey(anyString())).thenReturn(mock());
    final ByProjectKeyProductProjectionsKeyByKeyGet getMock =
        mock(ByProjectKeyProductProjectionsKeyByKeyGet.class);
    when(spyClient.productProjections().withKey(anyString()).get()).thenReturn(getMock);
    when(getMock.execute())
        .thenReturn(exceptionallyCompletedFuture(new BadGatewayException(500, "", null, "", null)))
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
