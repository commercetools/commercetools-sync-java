package com.commercetools.sync.integration.services.impl;

import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.*;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyProductTypesGet;
import com.commercetools.api.client.ByProjectKeyProductTypesRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.error.DuplicateFieldError;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeChangeNameAction;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.sync.integration.commons.utils.ProductTypeITUtils;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.impl.ProductTypeServiceImpl;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductTypeServiceImplIT {
  private ProductTypeService productTypeService;
  private static final String OLD_PRODUCT_TYPE_KEY = "old_product_type_key";
  private static final String OLD_PRODUCT_TYPE_NAME = "old_product_type_name";
  private static final Locale OLD_PRODUCT_TYPE_LOCALE = Locale.ENGLISH;

  private List<String> errorCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  /**
   * Deletes product types from the target CTP project, then it populates the project with test
   * data.
   */
  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();

    deleteProductTypes(CTP_TARGET_CLIENT);
    ProductTypeITUtils.ensureProductType(
        OLD_PRODUCT_TYPE_KEY, OLD_PRODUCT_TYPE_LOCALE, OLD_PRODUCT_TYPE_NAME, CTP_TARGET_CLIENT);
    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    productTypeService = new ProductTypeServiceImpl(productTypeSyncOptions);
  }

  /** Cleans up the target test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    deleteProductTypes(CTP_TARGET_CLIENT);
  }

  @Test
  void fetchCachedProductTypeId_WithNonExistingProductType_ShouldNotFetchAProductType() {
    final Optional<String> productTypeId =
        productTypeService
            .fetchCachedProductTypeId("non-existing-type-key")
            .toCompletableFuture()
            .join();
    assertThat(productTypeId).isEmpty();
  }

  @Test
  void fetchCachedProductTypeId_WithExistingProductType_ShouldFetchProductTypeAndCache() {
    final Optional<String> productTypeId =
        productTypeService
            .fetchCachedProductTypeId(OLD_PRODUCT_TYPE_KEY)
            .toCompletableFuture()
            .join();
    assertThat(productTypeId).isNotEmpty();
  }

  @Test
  void fetchCachedProductAttributeMetaDataMap_WithMetadataCache_ShouldReturnAnyAttributeMetadata() {
    final Optional<String> productTypeId =
        productTypeService
            .fetchCachedProductTypeId(OLD_PRODUCT_TYPE_KEY)
            .toCompletableFuture()
            .join();

    assertThat(productTypeId).isNotEmpty();

    Optional<Map<String, AttributeMetaData>> fetchCachedProductAttributeMetaDataMap =
        productTypeService
            .fetchCachedProductAttributeMetaDataMap(productTypeId.get())
            .toCompletableFuture()
            .join();

    assertThat(fetchCachedProductAttributeMetaDataMap).isNotEmpty();
  }

  @Test
  void
      fetchCachedProductAttributeMetaDataMap_WithoutMetadataCache_ShouldReturnAnyAttributeMetadata() {
    final ProductType productType =
        CTP_TARGET_CLIENT
            .productTypes()
            .withKey(OLD_PRODUCT_TYPE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    assertThat(productType).isNotNull();

    Optional<Map<String, AttributeMetaData>> fetchCachedProductAttributeMetaDataMap =
        productTypeService
            .fetchCachedProductAttributeMetaDataMap(productType.getId())
            .toCompletableFuture()
            .join();

    assertThat(fetchCachedProductAttributeMetaDataMap).isNotEmpty();
  }

  @Test
  void fetchMatchingProductTypesByKeys_WithEmptySetOfKeys_ShouldReturnEmptySet() {
    final Set<String> typeKeys = new HashSet<>();
    final Set<ProductType> matchingProductTypes =
        productTypeService.fetchMatchingProductTypesByKeys(typeKeys).toCompletableFuture().join();

    assertThat(matchingProductTypes).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingProductTypesByKeys_WithNonExistingKeys_ShouldReturnEmptySet() {
    final Set<String> typeKeys = new HashSet<>();
    typeKeys.add("type_key_1");
    typeKeys.add("type_key_2");

    final Set<ProductType> matchingProductTypes =
        productTypeService.fetchMatchingProductTypesByKeys(typeKeys).toCompletableFuture().join();

    assertThat(matchingProductTypes).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingProductTypesByKeys_WithAnyExistingKeys_ShouldReturnASetOfProductTypes() {
    final Set<String> typeKeys = new HashSet<>();
    typeKeys.add(OLD_PRODUCT_TYPE_KEY);

    final Set<ProductType> matchingProductTypes =
        productTypeService.fetchMatchingProductTypesByKeys(typeKeys).toCompletableFuture().join();

    assertThat(matchingProductTypes).isNotEmpty();
    assertThat(matchingProductTypes).hasSize(1);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingProductTypesByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
    // Mock client to return BadGatewayException on any request.

    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);
    when(spyClient.productTypes()).thenReturn(mock(ByProjectKeyProductTypesRequestBuilder.class));
    final ByProjectKeyProductTypesGet getMock = mock(ByProjectKeyProductTypesGet.class);
    when(spyClient.productTypes().get()).thenReturn(getMock);
    when(getMock.withWhere(any(String.class))).thenReturn(getMock);
    when(getMock.withPredicateVar(any(String.class), any())).thenReturn(getMock);
    when(getMock.withLimit(any(Integer.class))).thenReturn(getMock);
    when(getMock.withWithTotal(any(Boolean.class))).thenReturn(getMock);
    when(getMock.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new BadGatewayException(500, "", null, "", null)))
        .thenCallRealMethod();
    final ProductTypeSyncOptions spyOptions =
        ProductTypeSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final ProductTypeService spyProductTypeService = new ProductTypeServiceImpl(spyOptions);

    final Set<String> keys = new HashSet<>();
    keys.add(OLD_PRODUCT_TYPE_KEY);

    // test and assert
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(spyProductTypeService.fetchMatchingProductTypesByKeys(keys))
        .failsWithin(10, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  void fetchMatchingProductTypesByKeys_WithAllExistingSetOfKeys_ShouldCacheFetchedProductTypeIds() {
    final Set<ProductType> fetchedProductTypes =
        productTypeService
            .fetchMatchingProductTypesByKeys(singleton(OLD_PRODUCT_TYPE_KEY))
            .toCompletableFuture()
            .join();
    assertThat(fetchedProductTypes).hasSize(1);

    ProductType productType =
        CTP_TARGET_CLIENT
            .productTypes()
            .withKey(OLD_PRODUCT_TYPE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();
    assertThat(productType).isNotNull();

    // Change product oldKey on ctp
    final String newKey = "newKey";
    productTypeService
        .updateProductType(
            productType,
            Collections.singletonList(ProductTypeUpdateAction.setKeyBuilder().key(newKey).build()))
        .toCompletableFuture()
        .join();

    // Fetch cached id by old key
    final Optional<String> cachedProductTypeId =
        productTypeService
            .fetchCachedProductTypeId(OLD_PRODUCT_TYPE_KEY)
            .toCompletableFuture()
            .join();

    assertThat(cachedProductTypeId).isNotEmpty();
    assertThat(cachedProductTypeId).contains(productType.getId());
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void createProductType_WithValidProductType_ShouldCreateProductTypeAndCacheId() {
    // preparation
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_1)
            .name(PRODUCT_TYPE_NAME_1)
            .description(PRODUCT_TYPE_DESCRIPTION_1)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_1)
            .build();

    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);

    final ProductTypeSyncOptions spyOptions =
        ProductTypeSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final ProductTypeService productTypeService = new ProductTypeServiceImpl(spyOptions);

    // test
    final Optional<ProductType> createdOptional =
        productTypeService.createProductType(newProductTypeDraft).toCompletableFuture().join();
    // assertion
    final ProductType fetchedProductType =
        CTP_TARGET_CLIENT
            .productTypes()
            .withKey(PRODUCT_TYPE_KEY_1)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    assertThat(fetchedProductType)
        .satisfies(
            queried ->
                assertThat(createdOptional)
                    .hasValueSatisfying(
                        created -> {
                          assertThat(created.getKey()).isEqualTo(queried.getKey());

                          assertThat(created.getDescription()).isEqualTo(queried.getDescription());
                          assertThat(created.getName()).isEqualTo(queried.getName());

                          assertThat(created.getAttributes()).isEqualTo(queried.getAttributes());
                        }));

    final ByProjectKeyProductTypesRequestBuilder mock1 =
        mock(ByProjectKeyProductTypesRequestBuilder.class);
    when(spyClient.productTypes()).thenReturn(mock1);
    final ByProjectKeyProductTypesGet mock2 = mock(ByProjectKeyProductTypesGet.class);
    when(mock1.get()).thenReturn(mock2);
    when(mock2.withWhere(any(String.class))).thenReturn(mock2);
    when(mock2.withPredicateVar(any(String.class), any())).thenReturn(mock2);
    final CompletableFuture<ApiHttpResponse<ProductType>> spy = mock(CompletableFuture.class);

    // Assert that the created product type is cached
    final Optional<String> productTypeId =
        productTypeService
            .fetchCachedProductTypeId(PRODUCT_TYPE_KEY_1)
            .toCompletableFuture()
            .join();
    assertThat(productTypeId).isPresent();
    verify(spy, times(0)).handle(any());
  }

  @Test
  void createProductType_WithInvalidProductType_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("")
            .name(PRODUCT_TYPE_NAME_1)
            .description(PRODUCT_TYPE_DESCRIPTION_1)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_1)
            .build();

    final ProductTypeSyncOptions options =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final ProductTypeService service = new ProductTypeServiceImpl(options);

    // test
    final Optional<ProductType> result =
        service.createProductType(newProductTypeDraft).toCompletableFuture().join();

    // assertion
    assertThat(result).isEmpty();
    assertThat(errorCallBackMessages)
        .containsExactly("Failed to create draft with key: ''. Reason: Draft key is blank!");
  }

  @Test
  void createProductType_WithDuplicateKey_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(OLD_PRODUCT_TYPE_KEY)
            .name(PRODUCT_TYPE_NAME_1)
            .description(PRODUCT_TYPE_DESCRIPTION_1)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_1)
            .build();

    final ProductTypeSyncOptions options =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final ProductTypeService service = new ProductTypeServiceImpl(options);

    // test
    final Optional<ProductType> result =
        service.createProductType(newProductTypeDraft).toCompletableFuture().join();

    // assertion
    assertThat(result).isEmpty();
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("A duplicate value");
    assertThat(errorCallBackExceptions)
        .hasSize(1)
        .singleElement()
        .matches(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(CompletionException.class);
              final CompletionException completionException = (CompletionException) exception;

              final BadRequestException badRequestException =
                  (BadRequestException) completionException.getCause();

              final List<DuplicateFieldError> fieldErrors =
                  badRequestException.getErrorResponse().getErrors().stream()
                      .map(
                          ctpError -> {
                            assertThat(ctpError.getCode())
                                .isEqualTo(DuplicateFieldError.DUPLICATE_FIELD);
                            return (DuplicateFieldError) ctpError;
                          })
                      .collect(toList());
              return fieldErrors.size() == 1;
            });
  }

  @Test
  void updateProductType_WithValidChanges_ShouldUpdateProductTypeCorrectly() {
    final ProductType productType =
        CTP_TARGET_CLIENT
            .productTypes()
            .withKey(OLD_PRODUCT_TYPE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();
    final ProductTypeChangeNameAction changeNameAction =
        ProductTypeUpdateAction.changeNameBuilder().name("new_product_type_name").build();
    final ProductType updatedProductType =
        productTypeService
            .updateProductType(productType, singletonList(changeNameAction))
            .toCompletableFuture()
            .join();
    final ProductType fetchedProductType =
        CTP_TARGET_CLIENT
            .productTypes()
            .withKey(OLD_PRODUCT_TYPE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    assertThat(fetchedProductType.getKey()).isEqualTo(updatedProductType.getKey());
    assertThat(fetchedProductType.getDescription()).isEqualTo(updatedProductType.getDescription());
    assertThat(fetchedProductType.getName()).isEqualTo(updatedProductType.getName());
    assertThat(fetchedProductType.getAttributes()).isEqualTo(updatedProductType.getAttributes());
  }

  /*
  * This test verifies the cache stampede fix by making concurrent calls
    and ensuring the cache is populated correctly without race conditions.

    What this test verifies:
    1. All concurrent calls complete successfully (no race conditions)
    2. All calls return the same cached data (cache consistency)
    3. No exceptions occur during concurrent access

    NOTE: The tests were execute with logs and it can be seen that only one query is executed.
  */
  @Test
  void
      fetchCachedProductAttributeMetaDataMap_WithConcurrentCalls_ShouldHandleCacheStampedeCorrectly()
          throws Exception {

    // preparation - create a product type with attributes
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("cache-stampede-test-type")
            .name("Cache Stampede Test Type")
            .description("Test product type for cache stampede fix")
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_1)
            .build();

    final ProductType createdProductType =
        CTP_TARGET_CLIENT.productTypes().post(productTypeDraft).execute().join().getBody();

    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    final ProductTypeService productTypeService =
        new ProductTypeServiceImpl(productTypeSyncOptions);

    // test - make 10 concurrent calls to fetchCachedProductAttributeMetaDataMap
    // Used a CountDownLatch to ensure all threads start at approximately the same time
    final int numberOfConcurrentCalls = 10;
    final java.util.concurrent.CountDownLatch startLatch =
        new java.util.concurrent.CountDownLatch(1);
    final java.util.concurrent.CountDownLatch readyLatch =
        new java.util.concurrent.CountDownLatch(numberOfConcurrentCalls);
    final java.util.concurrent.ExecutorService executorService =
        java.util.concurrent.Executors.newFixedThreadPool(numberOfConcurrentCalls);
    final java.util.List<
            java.util.concurrent.CompletableFuture<Optional<Map<String, AttributeMetaData>>>>
        futures = new java.util.ArrayList<>();

    for (int i = 0; i < numberOfConcurrentCalls; i++) {
      final java.util.concurrent.CompletableFuture<Optional<Map<String, AttributeMetaData>>>
          future =
              java.util.concurrent.CompletableFuture.supplyAsync(
                  () -> {
                    try {
                      readyLatch.countDown();
                      startLatch.await(); // Wait for all threads to be ready
                      return productTypeService
                          .fetchCachedProductAttributeMetaDataMap(createdProductType.getId())
                          .toCompletableFuture()
                          .join();
                    } catch (InterruptedException e) {
                      Thread.currentThread().interrupt();
                      throw new RuntimeException(e);
                    }
                  },
                  executorService);
      futures.add(future);
    }

    readyLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);

    // Start all threads at once
    startLatch.countDown();

    // Wait for all futures to complete
    java.util.concurrent.CompletableFuture.allOf(
            futures.toArray(new java.util.concurrent.CompletableFuture[0]))
        .join();

    executorService.shutdown();
    executorService.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

    // assertions - all calls should return the same result
    final Optional<Map<String, AttributeMetaData>> firstResult = futures.get(0).join();
    assertThat(firstResult).isPresent();

    for (java.util.concurrent.CompletableFuture<Optional<Map<String, AttributeMetaData>>> future :
        futures) {
      assertThat(future).isCompleted();
      final Optional<Map<String, AttributeMetaData>> result = future.join();
      assertThat(result).isPresent();
      assertThat(result.get()).containsKey(ATTRIBUTE_DEFINITION_DRAFT_1.getName());
      // Verify all results are identical (same cached instance)
      assertThat(result.get()).isEqualTo(firstResult.get());
    }

    // cleanup
    CTP_TARGET_CLIENT
        .productTypes()
        .withId(createdProductType.getId())
        .delete()
        .withVersion(createdProductType.getVersion())
        .execute()
        .join();
  }

  @Test
  void fetchCachedProductAttributeMetaDataMap_WithPopulatedCache_ShouldReturnCachedData() {
    // This test verifies that after the first call, subsequent calls use the cache

    // preparation - create a product type
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("cache-reuse-test-type")
            .name("Cache Reuse Test Type")
            .description("Test product type for cache reuse")
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2)
            .build();

    final ProductType createdProductType =
        CTP_TARGET_CLIENT.productTypes().post(productTypeDraft).execute().join().getBody();

    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    final ProductTypeService productTypeService =
        new ProductTypeServiceImpl(productTypeSyncOptions);

    // test - first call to populate cache
    final Optional<Map<String, AttributeMetaData>> firstResult =
        productTypeService
            .fetchCachedProductAttributeMetaDataMap(createdProductType.getId())
            .toCompletableFuture()
            .join();

    // test - second call should use cache
    final Optional<Map<String, AttributeMetaData>> secondResult =
        productTypeService
            .fetchCachedProductAttributeMetaDataMap(createdProductType.getId())
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(firstResult).isPresent();
    assertThat(secondResult).isPresent();
    assertThat(firstResult.get()).isEqualTo(secondResult.get());
    assertThat(firstResult.get()).hasSize(2);
    assertThat(firstResult.get())
        .containsKeys(
            ATTRIBUTE_DEFINITION_DRAFT_1.getName(), ATTRIBUTE_DEFINITION_DRAFT_2.getName());

    // cleanup
    CTP_TARGET_CLIENT
        .productTypes()
        .withId(createdProductType.getId())
        .delete()
        .withVersion(createdProductType.getVersion())
        .execute()
        .join();
  }

  @Test
  void
      fetchCachedProductAttributeMetaDataMap_WithNonExistentProductType_ShouldReturnEmptyOptional() {
    // preparation
    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    final ProductTypeService productTypeService =
        new ProductTypeServiceImpl(productTypeSyncOptions);

    // test - query for non-existent product type ID
    final Optional<Map<String, AttributeMetaData>> result =
        productTypeService
            .fetchCachedProductAttributeMetaDataMap("non-existent-id-12345")
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(result).isEmpty();
  }
}
