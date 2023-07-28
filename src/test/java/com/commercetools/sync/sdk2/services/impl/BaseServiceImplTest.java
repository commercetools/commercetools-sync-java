package com.commercetools.sync.sdk2.services.impl;

import static com.commercetools.sync.sdk2.commons.ExceptionUtils.createBadGatewayException;
import static com.commercetools.sync.sdk2.commons.utils.TestUtils.mockGraphQLResponse;
import static java.util.Collections.*;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.*;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectPagedQueryResponse;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionPagedQueryResponse;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.sdk2.customobjects.CustomObjectSyncOptionsBuilder;
import com.commercetools.sync.sdk2.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.services.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings("unchecked")
class BaseServiceImplTest {

  private final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
      warningCallback = mock(TriConsumer.class);

  private final ByProjectKeyProductProjectionsKeyByKeyGet
      byProjectKeyProductProjectionsKeyByKeyGet = mock();
  private final ByProjectKeyProductProjectionsGet byProjectKeyProductProjectionsGet = mock();
  private final ByProjectKeyGraphqlPost byProjectKeyGraphQlPost = mock();
  private ProductService service;

  @BeforeEach
  void setup() {
    final ProjectApiRoot client = mockProjectApiRoot();
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(client)
            .warningCallback(warningCallback)
            .batchSize(20)
            .cacheSize(2)
            .build();
    service = new ProductServiceImpl(syncOptions);
  }

  @AfterEach
  void cleanup() {
    reset(byProjectKeyProductProjectionsKeyByKeyGet, byProjectKeyGraphQlPost, warningCallback);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void fetchCachedResourceId_WithBlankKey_ShouldMakeNoRequestAndReturnEmptyOptional(
      final String key) {
    // test
    final Optional<String> result = service.getIdFromCacheOrFetch(key).toCompletableFuture().join();

    // assertions
    assertThat(result).isEmpty();
    verify(byProjectKeyProductProjectionsKeyByKeyGet, never()).execute();
  }

  @Test
  void fetchCachedResourceId_WithFetchResourceWithKey_ShouldReturnResourceId() {
    // preparation
    final ProductProjectionPagedQueryResponse pagedQueryResponse =
        mock(ProductProjectionPagedQueryResponse.class);
    final ProductProjection mockProductResult = mock(ProductProjection.class);
    final String key = "testKey";
    final String id = "testId";
    when(mockProductResult.getKey()).thenReturn(key);
    when(mockProductResult.getId()).thenReturn(id);
    when(pagedQueryResponse.getResults()).thenReturn(singletonList(mockProductResult));

    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(pagedQueryResponse);
    when(byProjectKeyProductProjectionsGet.execute()).thenReturn(completedFuture(apiHttpResponse));

    // test
    final Optional<String> result = service.getIdFromCacheOrFetch(key).toCompletableFuture().join();

    // assertions
    assertThat(result).contains(id);
  }

  @Test
  void fetchCachedResourceId_WithCachedResource_ShouldReturnResourceIdWithoutMakingRequest() {
    // preparation
    final ProductProjectionPagedQueryResponse pagedQueryResponse =
        mock(ProductProjectionPagedQueryResponse.class);
    final ProductProjection mockProductResult = mock(ProductProjection.class);
    final String key = "testKey";
    final String id = "testId";
    when(mockProductResult.getKey()).thenReturn(key);
    when(mockProductResult.getId()).thenReturn(id);
    when(pagedQueryResponse.getResults()).thenReturn(singletonList(mockProductResult));

    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(pagedQueryResponse);
    when(byProjectKeyProductProjectionsGet.execute()).thenReturn(completedFuture(apiHttpResponse));
    service.getIdFromCacheOrFetch(key).toCompletableFuture().join();

    // test
    final Optional<String> result = service.getIdFromCacheOrFetch(key).toCompletableFuture().join();

    // assertions
    assertThat(result).contains(id);
    // only 1 request of the first fetch, but no more since second time it gets it from cache.
    verify(byProjectKeyProductProjectionsGet, times(1)).execute();
  }

  @Test
  void fetchMatchingResources_WithEmptyKeySet_ShouldFetchAndCacheNothing() {
    // test
    final Set<ProductProjection> resources =
        service.fetchMatchingProductsByKeys(new HashSet<>()).toCompletableFuture().join();

    // assertions
    assertThat(resources).isEmpty();
    verify(byProjectKeyProductProjectionsGet, never()).execute();
  }

  @Test
  void fetchMatchingResources_WithSpecialCharactersInKeySet_ShouldExecuteQuery() {
    // preparation
    final String key1 = "special-\"charTest";

    final HashSet<String> resourceKeys = new HashSet<>();
    resourceKeys.add(key1);

    final ProductProjectionPagedQueryResponse result =
        mock(ProductProjectionPagedQueryResponse.class);
    when(result.getResults()).thenReturn(EMPTY_LIST);
    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(result);
    when(byProjectKeyProductProjectionsGet.execute()).thenReturn(completedFuture(apiHttpResponse));

    // test
    service.fetchMatchingProductsByKeys(resourceKeys).toCompletableFuture().join();

    // assertions
    verify(byProjectKeyProductProjectionsGet, times(1)).execute();
  }

  @SuppressWarnings("unchecked")
  @Test
  void fetchMatchingResources_WithKeySet_ShouldFetchResourcesAndCacheKeys() {
    // preparation
    final String key1 = RandomStringUtils.random(15);
    final String key2 = RandomStringUtils.random(15);

    final HashSet<String> resourceKeys = new HashSet<>();
    resourceKeys.add(key1);
    resourceKeys.add(key2);

    final ProductProjection mock1 = mock(ProductProjection.class);
    when(mock1.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock1.getKey()).thenReturn(key1);

    final ProductProjection mock2 = mock(ProductProjection.class);
    when(mock2.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock2.getKey()).thenReturn(key2);

    final ProductProjectionPagedQueryResponse result =
        mock(ProductProjectionPagedQueryResponse.class);
    when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));

    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(result);
    when(byProjectKeyProductProjectionsGet.execute()).thenReturn(completedFuture(apiHttpResponse));

    // test fetch
    final Set<ProductProjection> resources =
        service.fetchMatchingProductsByKeys(resourceKeys).toCompletableFuture().join();

    // assertions
    assertThat(resources).containsExactlyInAnyOrder(mock1, mock2);
    verify(byProjectKeyProductProjectionsGet, times(1)).execute();

    // test caching
    final Optional<String> cachedKey1 =
        service.getIdFromCacheOrFetch(mock1.getKey()).toCompletableFuture().join();

    final Optional<String> cachedKey2 =
        service.getIdFromCacheOrFetch(mock2.getKey()).toCompletableFuture().join();

    // assertions
    assertThat(cachedKey1).contains(mock1.getId());
    assertThat(cachedKey2).contains(mock2.getId());
    // still 1 request from the first #fetchMatchingProductsByKeys call
    verify(byProjectKeyProductProjectionsGet, times(1)).execute();
  }

  @Test
  void fetchMatchingResources_WithKeySetOf500_ShouldChunkAndFetchResourcesAndCacheKeys() {
    // preparation
    List<String> randomKeys = new ArrayList<>();
    IntStream.range(0, 500).forEach(ignore -> randomKeys.add(RandomStringUtils.random(15)));

    final HashSet<String> resourceKeys = new HashSet<>();
    resourceKeys.addAll(randomKeys);

    final ProductProjection mock1 = mock(ProductProjection.class);
    when(mock1.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock1.getKey()).thenReturn(randomKeys.get(0));

    final ProductProjection mock2 = mock(ProductProjection.class);
    when(mock2.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock2.getKey()).thenReturn(randomKeys.get(251));

    final ProductProjectionPagedQueryResponse result =
        mock(ProductProjectionPagedQueryResponse.class);
    when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));
    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(result);
    when(byProjectKeyProductProjectionsGet.execute()).thenReturn(completedFuture(apiHttpResponse));

    // test fetch
    final Set<ProductProjection> resources =
        service.fetchMatchingProductsByKeys(resourceKeys).toCompletableFuture().join();

    // assertions
    assertThat(resources).containsExactlyInAnyOrder(mock1, mock2);
    verify(byProjectKeyProductProjectionsGet, times(2)).execute();

    // test caching
    final Optional<String> cachedKey1 =
        service.getIdFromCacheOrFetch(mock1.getKey()).toCompletableFuture().join();

    final Optional<String> cachedKey2 =
        service.getIdFromCacheOrFetch(mock2.getKey()).toCompletableFuture().join();

    // assertions
    assertThat(cachedKey1).contains(mock1.getId());
    assertThat(cachedKey2).contains(mock2.getId());
    verify(byProjectKeyProductProjectionsGet, times(2)).execute();
  }

  @Test
  void fetchMatchingResources_WithBadGateWayException_ShouldCompleteExceptionally() {
    // preparation
    final String key1 = RandomStringUtils.random(15);
    final String key2 = RandomStringUtils.random(15);

    final HashSet<String> resourceKeys = new HashSet<>();
    resourceKeys.add(key1);
    resourceKeys.add(key2);

    when(byProjectKeyProductProjectionsGet.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(createBadGatewayException()));

    // test
    final CompletionStage<Set<ProductProjection>> result =
        service.fetchMatchingProductsByKeys(resourceKeys);

    // assertions
    assertThat(result)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
    verify(byProjectKeyProductProjectionsGet).execute();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void fetchResource_WithBlankKey_ShouldMakeNoRequestAndReturnEmptyOptional(final String key) {
    // test
    final Optional<ProductProjection> optional =
        service.fetchProduct(key).toCompletableFuture().join();

    // assertions
    assertThat(optional).isEmpty();
    verify(byProjectKeyProductProjectionsKeyByKeyGet, never()).execute();
  }

  @SuppressWarnings("unchecked")
  @Test
  void fetchResource_WithKey_ShouldFetchResource() {
    // preparation
    final String resourceId = RandomStringUtils.random(15);
    final String resourceKey = RandomStringUtils.random(15);

    final ProductProjection mockProductResult = mock(ProductProjection.class);
    when(mockProductResult.getKey()).thenReturn(resourceKey);
    when(mockProductResult.getId()).thenReturn(resourceId);

    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(mockProductResult);
    when(byProjectKeyProductProjectionsKeyByKeyGet.execute())
        .thenReturn(completedFuture(apiHttpResponse));

    // test
    final Optional<ProductProjection> resourceOptional =
        service.fetchProduct(resourceKey).toCompletableFuture().join();

    // assertions
    assertThat(resourceOptional).containsSame(mockProductResult);
    verify(byProjectKeyProductProjectionsKeyByKeyGet).execute();
  }

  @Test
  void fetchResource_WithBadGateWayException_ShouldCompleteExceptionally() {
    // preparation
    when(byProjectKeyProductProjectionsKeyByKeyGet.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(createBadGatewayException()));

    // test
    final CompletionStage<Optional<ProductProjection>> result = service.fetchProduct("foo");

    // assertions
    assertThat(result)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
    verify(byProjectKeyProductProjectionsKeyByKeyGet, times(1)).execute();
  }

  @Test
  void cacheKeysToIdsUsingGraphQl_WithEmptySetOfKeys_ShouldMakeNoRequestAndReturnEmptyOptional() {
    // test
    final Map<String, String> optional =
        service.cacheKeysToIds(emptySet()).toCompletableFuture().join();

    // assertions
    assertThat(optional).isEmpty();
    verify(byProjectKeyProductProjectionsKeyByKeyGet, never()).execute();
  }

  @Test
  void cacheKeysToIdsUsingGraphQl_WithAllCachedKeys_ShouldMakeNoRequestAndReturnCachedEntry() {
    // preparation
    final ProductProjectionPagedQueryResponse pagedQueryResponse =
        mock(ProductProjectionPagedQueryResponse.class);
    final ProductProjection mockProductResult = mock(ProductProjection.class);
    final String key = "testKey";
    final String id = "testId";
    when(mockProductResult.getKey()).thenReturn(key);
    when(mockProductResult.getId()).thenReturn(id);
    when(pagedQueryResponse.getResults()).thenReturn(singletonList(mockProductResult));
    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(pagedQueryResponse);
    when(byProjectKeyProductProjectionsGet.execute()).thenReturn(completedFuture(apiHttpResponse));
    service.getIdFromCacheOrFetch(key).toCompletableFuture().join();

    // test
    final Map<String, String> optional =
        service.cacheKeysToIds(singleton("testKey")).toCompletableFuture().join();

    // assertions
    assertThat(optional).containsExactly(MapEntry.entry(key, id));
    verify(byProjectKeyProductProjectionsGet, times(1)).execute();
  }

  @Test
  void cacheKeysToIds_WithCachedKeysExceedingCacheSize_ShouldNotReturnLeastUsedKeys()
      throws JsonProcessingException {
    // preparation
    final ProductProjectionPagedQueryResponse pagedQueryResponse =
        mock(ProductProjectionPagedQueryResponse.class);
    final ProductProjection product1 = mock(ProductProjection.class);
    when(product1.getKey()).thenReturn("key-1");
    when(product1.getId()).thenReturn("id-1");
    final ProductProjection product2 = mock(ProductProjection.class);
    when(product2.getKey()).thenReturn("key-2");
    when(product2.getId()).thenReturn("id-2");
    when(pagedQueryResponse.getResults()).thenReturn(Arrays.asList(product1, product2));
    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(pagedQueryResponse);
    final String jsonStringProducts =
        "{ \"products\": {\"results\":[{\"id\":\"testId\",\"key\":\"testKey\"}]}}";
    final ApiHttpResponse<GraphQLResponse> productsResponse =
        mockGraphQLResponse(jsonStringProducts);
    when(byProjectKeyProductProjectionsGet.execute()).thenReturn(completedFuture(apiHttpResponse));
    when(byProjectKeyGraphQlPost.execute())
        .thenReturn(CompletableFuture.completedFuture(productsResponse));
    service.fetchMatchingProductsByKeys(
        Arrays.asList("key-1", "key-2").stream().collect(Collectors.toSet()));
    service.getIdFromCacheOrFetch("key-1"); // access the first added cache entry

    // test
    final Map<String, String> optional =
        service.cacheKeysToIds(singleton("testKey")).toCompletableFuture().join();

    // assertions
    assertThat(optional)
        .containsExactly(MapEntry.entry("key-1", "id-1"), MapEntry.entry("testKey", "testId"));
    verify(byProjectKeyProductProjectionsGet, times(1)).execute();
    verify(byProjectKeyGraphQlPost, times(1)).execute();
  }

  @Test
  void cacheKeysToIdsUsingGraphQl_WithNoCachedKeys_ShouldMakeRequestAndReturnCachedEntry()
      throws JsonProcessingException {
    // preparation
    final String key = "testKey";
    final String id = "testId";
    final String jsonStringProducts =
        "{ \"products\": {\"results\":[{\"id\":\"" + id + "\",\"key\":\"" + key + "\"}]}}";
    final ApiHttpResponse<GraphQLResponse> productsResponse =
        mockGraphQLResponse(jsonStringProducts);
    when(byProjectKeyGraphQlPost.execute()).thenReturn(completedFuture(productsResponse));

    // test
    final Map<String, String> optional =
        service.cacheKeysToIds(singleton("testKey")).toCompletableFuture().join();

    // assertions
    assertThat(optional).containsExactly(MapEntry.entry(key, id));
    verify(byProjectKeyGraphQlPost, times(1)).execute();
  }

  @Test
  void cacheKeysToIdsUsingGraphQl_With500Keys_ShouldChunkAndMakeRequestAndReturnCachedEntry()
      throws JsonProcessingException {
    // preparation
    Set<String> randomKeys = new HashSet<>();
    IntStream.range(0, 500).forEach(ignore -> randomKeys.add(RandomStringUtils.random(15)));
    final String key = randomKeys.stream().findFirst().get();
    final String id = "testId";
    final String jsonStringProducts =
        "{ \"products\": {\"results\":[{\"id\":\"" + id + "\",\"key\":\"" + key + "\"}]}}";
    final ApiHttpResponse<GraphQLResponse> productsResponse =
        mockGraphQLResponse(jsonStringProducts);
    when(byProjectKeyGraphQlPost.execute()).thenReturn(completedFuture(productsResponse));

    // test
    final Map<String, String> optional =
        service.cacheKeysToIds(randomKeys).toCompletableFuture().join();

    // assertions
    assertThat(optional).containsExactly(MapEntry.entry(key, id));
    verify(byProjectKeyGraphQlPost, times(2)).execute();
  }

  @Test
  void cacheKeysToIdsUsingGraphQl_WithBadGateWayException_ShouldCompleteExceptionally() {
    // preparation
    when(byProjectKeyGraphQlPost.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(createBadGatewayException()));

    // test
    final CompletionStage<Map<String, String>> result =
        service.cacheKeysToIds(singleton("testKey"));

    // assertions
    assertThat(result)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
    verify(byProjectKeyGraphQlPost, times(1)).execute();
  }

  @Test
  void cacheKeysToIds_WithEmptySetOfKeys_ShouldNotMakeRequestAndReturnEmpty() {
    // preparation
    final ProjectApiRoot client = mockProjectApiRoot();
    CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(client).build();
    CustomObjectServiceImpl serviceImpl = new CustomObjectServiceImpl(customObjectSyncOptions);

    // test
    final Map<String, String> optional =
        serviceImpl.cacheKeysToIds(emptySet()).toCompletableFuture().join();

    // assertions
    assertThat(optional).isEmpty();
    verify(byProjectKeyGraphQlPost, never()).execute();
  }

  @Test
  void cacheKeysToIds_WithEmptyCache_ShouldMakeRequestAndReturnCacheEntries() {
    // preparation
    final ProjectApiRoot client = mock(ProjectApiRoot.class);
    final ByProjectKeyCustomObjectsRequestBuilder byProjectKeyCustomObjectsRequestBuilder =
        mock(ByProjectKeyCustomObjectsRequestBuilder.class);
    final ByProjectKeyCustomObjectsGet byProjectKeyCustomObjectsGet =
        mock(ByProjectKeyCustomObjectsGet.class);
    when(byProjectKeyCustomObjectsGet.withWhere(anyString()))
        .thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withLimit(anyInt())).thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsRequestBuilder.get()).thenReturn(byProjectKeyCustomObjectsGet);
    when(client.customObjects()).thenReturn(byProjectKeyCustomObjectsRequestBuilder);

    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(client).build();
    final CustomObjectServiceImpl serviceImpl =
        new CustomObjectServiceImpl(customObjectSyncOptions);
    final String customObjectId = "customObjectId";
    final String customObjectContainer = "customObjectContainer";
    final String customObjectKey = "customObjectKey";
    final CustomObject customObject = mock(CustomObject.class);
    when(customObject.getId()).thenReturn(customObjectId);
    when(customObject.getContainer()).thenReturn(customObjectContainer);
    when(customObject.getKey()).thenReturn(customObjectKey);

    final CustomObjectPagedQueryResponse pagedQueryResponse =
        mock(CustomObjectPagedQueryResponse.class);
    when(pagedQueryResponse.getResults()).thenReturn(singletonList(customObject));
    final ApiHttpResponse<CustomObjectPagedQueryResponse> apiHttpResponse =
        mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(pagedQueryResponse);
    when(byProjectKeyCustomObjectsGet.execute()).thenReturn(completedFuture(apiHttpResponse));

    final Map<String, String> result =
        serviceImpl
            .cacheKeysToIds(
                singleton(
                    CustomObjectCompositeIdentifier.of(customObjectKey, customObjectContainer)))
            .toCompletableFuture()
            .join();

    assertAll(
        () -> assertThat(result).hasSize(1),
        () ->
            assertThat(
                    result.get(
                        CustomObjectCompositeIdentifier.of(customObjectKey, customObjectContainer)
                            .toString()))
                .isEqualTo(customObjectId));
    verify(byProjectKeyCustomObjectsGet).execute();
  }

  @Test
  void
      cacheKeysToIds_With500CustomObjectIdentifiers_ShouldChunkAndMakeRequestAndReturnCacheEntries() {
    // preparation
    final ProjectApiRoot client = mock(ProjectApiRoot.class);
    Set<CustomObjectCompositeIdentifier> randomIdentifiers = new HashSet<>();
    IntStream.range(0, 500)
        .forEach(
            i ->
                randomIdentifiers.add(
                    CustomObjectCompositeIdentifier.of(
                        "customObjectId" + i, "customObjectContainer" + i)));

    final String customObjectId = randomIdentifiers.stream().findFirst().get().getKey();
    final String customObjectContainer =
        randomIdentifiers.stream().findFirst().get().getContainer();
    final String customObjectKey = "customObjectKey";
    final ByProjectKeyCustomObjectsRequestBuilder byProjectKeyCustomObjectsRequestBuilder =
        mock(ByProjectKeyCustomObjectsRequestBuilder.class);
    final ByProjectKeyCustomObjectsGet byProjectKeyCustomObjectsGet =
        mock(ByProjectKeyCustomObjectsGet.class);
    when(byProjectKeyCustomObjectsGet.withWhere(anyString()))
        .thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withLimit(anyInt())).thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsRequestBuilder.get()).thenReturn(byProjectKeyCustomObjectsGet);
    when(client.customObjects()).thenReturn(byProjectKeyCustomObjectsRequestBuilder);

    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(client).build();
    final CustomObjectServiceImpl serviceImpl =
        new CustomObjectServiceImpl(customObjectSyncOptions);
    final CustomObject customObject = mock(CustomObject.class);
    when(customObject.getId()).thenReturn(customObjectId);
    when(customObject.getContainer()).thenReturn(customObjectContainer);
    when(customObject.getKey()).thenReturn(customObjectKey);

    final CustomObjectPagedQueryResponse pagedQueryResponse =
        mock(CustomObjectPagedQueryResponse.class);
    when(pagedQueryResponse.getResults()).thenReturn(singletonList(customObject));
    final ApiHttpResponse<CustomObjectPagedQueryResponse> apiHttpResponse =
        mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(pagedQueryResponse);
    when(byProjectKeyCustomObjectsGet.execute()).thenReturn(completedFuture(apiHttpResponse));

    // test
    serviceImpl.cacheKeysToIds(randomIdentifiers).toCompletableFuture().join();

    // assertion
    verify(byProjectKeyCustomObjectsGet, times(2)).execute();
  }

  private ProjectApiRoot mockProjectApiRoot() {
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);
    final ByProjectKeyProductProjectionsRequestBuilder
        byProjectKeyProductProjectionsRequestBuilder = mock();
    when(ctpClient.productProjections()).thenReturn(byProjectKeyProductProjectionsRequestBuilder);
    final ByProjectKeyProductProjectionsKeyByKeyRequestBuilder
        byProjectKeyProductProjectionsKeyByKeyRequestBuilder = mock();
    when(byProjectKeyProductProjectionsRequestBuilder.get())
        .thenReturn(byProjectKeyProductProjectionsGet);
    when(byProjectKeyProductProjectionsGet.withStaged(anyBoolean()))
        .thenReturn(byProjectKeyProductProjectionsGet);
    when(byProjectKeyProductProjectionsGet.withWhere(anyString()))
        .thenReturn(byProjectKeyProductProjectionsGet);
    when(byProjectKeyProductProjectionsGet.withPredicateVar(anyString(), anyString()))
        .thenReturn(byProjectKeyProductProjectionsGet);
    when(byProjectKeyProductProjectionsGet.withPredicateVar(anyString(), anyCollection()))
        .thenReturn(byProjectKeyProductProjectionsGet);
    when(byProjectKeyProductProjectionsGet.withLimit(anyInt()))
        .thenReturn(byProjectKeyProductProjectionsGet);
    when(byProjectKeyProductProjectionsGet.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyProductProjectionsGet);
    when(byProjectKeyProductProjectionsGet.getQueryParam(anyString()))
        .thenReturn(singletonList("foo"));
    when(byProjectKeyProductProjectionsRequestBuilder.withKey(any()))
        .thenReturn(byProjectKeyProductProjectionsKeyByKeyRequestBuilder);
    when(byProjectKeyProductProjectionsKeyByKeyRequestBuilder.get())
        .thenReturn(byProjectKeyProductProjectionsKeyByKeyGet);
    final ByProjectKeyGraphqlRequestBuilder byProjectKeyGraphqlRequestBuilder = mock();
    when(ctpClient.graphql()).thenReturn(byProjectKeyGraphqlRequestBuilder);
    when(ctpClient.graphql().post(any(GraphQLRequest.class))).thenReturn(byProjectKeyGraphQlPost);
    final CompletableFuture<ApiHttpResponse<ProductProjection>>
        apiHttpResponseCompletableFutureProduct = mock();
    final CompletableFuture<ApiHttpResponse<ProductProjectionPagedQueryResponse>>
        apiHttpResponseCompletableFuturePagedQueryResponse = mock();
    final CompletableFuture<ApiHttpResponse<GraphQLResponse>>
        apihttpResponseCompletableFutureGraphQl = mock();
    when(byProjectKeyProductProjectionsKeyByKeyGet.execute())
        .thenReturn(apiHttpResponseCompletableFutureProduct);
    when(byProjectKeyProductProjectionsGet.execute())
        .thenReturn(apiHttpResponseCompletableFuturePagedQueryResponse);
    when(byProjectKeyGraphQlPost.execute()).thenReturn(apihttpResponseCompletableFutureGraphQl);
    return ctpClient;
  }
}
