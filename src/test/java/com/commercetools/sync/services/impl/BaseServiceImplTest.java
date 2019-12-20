package com.commercetools.sync.services.impl;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseServiceImplTest {

    @SuppressWarnings("unchecked")
    private Consumer<String> warningCallback = mock(Consumer.class);
    private SphereClient client = mock(SphereClient.class);
    private ProductService service;

    @BeforeEach
    void setup() {
        final ProductSyncOptions syncOptions =
            ProductSyncOptionsBuilder
                .of(client)
                .warningCallback(warningCallback)
                .batchSize(20)
                .build();
        service = new ProductServiceImpl(syncOptions);
    }

    @AfterEach
    void cleanup() {
        reset(client, warningCallback);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void fetchCachedResourceId_WithBlankKey_ShouldMakeNoRequestAndReturnEmptyOptional(final String key) {
        //test
        final Optional<String> result = service.getIdFromCacheOrFetch(key).toCompletableFuture().join();

        //assertions
        assertThat(result).isEmpty();
        verify(client, never()).execute(any());
    }

    @Test
    void fetchCachedResourceId_WithFetchResourceWithKey_ShouldReturnResourceId() {
        //preparation
        final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
        final Product mockProductResult = mock(Product.class);
        final String key = "testKey";
        final String id = "testId";
        when(mockProductResult.getKey()).thenReturn(key);
        when(mockProductResult.getId()).thenReturn(id);
        when(pagedQueryResult.getResults()).thenReturn(singletonList(mockProductResult));

        when(client.execute(any())).thenReturn(completedFuture(pagedQueryResult));

        //test
        final Optional<String> result = service.getIdFromCacheOrFetch(key).toCompletableFuture().join();

        //assertions
        assertThat(result).contains(id);
    }

    @Test
    void fetchCachedResourceId_WithCachedKey_ShouldReturnResourceIdWithoutMakingRequest() {
        //preparation
        final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
        final Product mockProductResult = mock(Product.class);
        final String key = "testKey";
        final String id = "testId";
        when(mockProductResult.getKey()).thenReturn(key);
        when(mockProductResult.getId()).thenReturn(id);
        when(pagedQueryResult.getResults()).thenReturn(singletonList(mockProductResult));
        when(client.execute(any())).thenReturn(completedFuture(pagedQueryResult));
        service.getIdFromCacheOrFetch(key).toCompletableFuture().join();

        //test
        final Optional<String> result = service.getIdFromCacheOrFetch(key).toCompletableFuture().join();

        //assertions
        assertThat(result).contains(id);
        // only 1 request of the first fetch, but no more since second time it gets it from cache.
        verify(client, times(1)).execute(any(ProductQuery.class));
    }

    @Test
    void fetchMatchingResources_WithEmptyKeySet_ShouldFetchAndCacheNothing() {
        //test
        final Set<Product> resources = service
            .fetchMatchingProductsByKeys(new HashSet<>())
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(resources).isEmpty();
        verify(client, never()).execute(any(ProductQuery.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void fetchMatchingResources_WithKeySet_ShouldFetchResourcesAndCacheKeys() {
        //preparation
        final String key1 = RandomStringUtils.random(15);
        final String key2 = RandomStringUtils.random(15);

        final HashSet<String> resourceKeys = new HashSet<>();
        resourceKeys.add(key1);
        resourceKeys.add(key2);

        final Product mock1 = mock(Product.class);
        when(mock1.getId()).thenReturn(RandomStringUtils.random(15));
        when(mock1.getKey()).thenReturn(key1);

        final Product mock2 = mock(Product.class);
        when(mock2.getId()).thenReturn(RandomStringUtils.random(15));
        when(mock2.getKey()).thenReturn(key2);

        final PagedQueryResult result = mock(PagedQueryResult.class);
        when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));

        when(client.execute(any(ProductQuery.class))).thenReturn(completedFuture(result));

        //test fetch
        final Set<Product> resources = service
            .fetchMatchingProductsByKeys(resourceKeys)
            .toCompletableFuture().join();

        //assertions
        assertThat(resources).containsExactlyInAnyOrder(mock1, mock2);
        verify(client, times(1)).execute(any(ProductQuery.class));

        //test caching
        final Optional<String> cachedKey1 = service
            .getIdFromCacheOrFetch(mock1.getKey())
            .toCompletableFuture().join();

        final Optional<String> cachedKey2 = service
            .getIdFromCacheOrFetch(mock2.getKey())
            .toCompletableFuture().join();

        //assertions
        assertThat(cachedKey1).contains(mock1.getId());
        assertThat(cachedKey2).contains(mock2.getId());
        // still 1 request from the first #fetchMatchingProductsByKeys call
        verify(client, times(1)).execute(any(ProductQuery.class));
    }

    @Test
    void fetchMatchingResources_WithBadGateWayException_ShouldCompleteExceptionally() {
        //preparation
        final String key1 = RandomStringUtils.random(15);
        final String key2 = RandomStringUtils.random(15);

        final HashSet<String> resourceKeys = new HashSet<>();
        resourceKeys.add(key1);
        resourceKeys.add(key2);

        when(client.execute(any(ProductQuery.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));

        //test
        final CompletionStage<Set<Product>> result = service.fetchMatchingProductsByKeys(resourceKeys);

        //assertions
        assertThat(result).hasFailedWithThrowableThat().isExactlyInstanceOf(BadGatewayException.class);
        verify(client).execute(any(ProductQuery.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void fetchResource_WithBlankKey_ShouldMakeNoRequestAndReturnEmptyOptional(final String key) {
        //test
        final Optional<Product> optional = service.fetchProduct(key).toCompletableFuture().join();

        //assertions
        assertThat(optional).isEmpty();
        verify(client, never()).execute(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void fetchResource_WithExistingProductKey_ShouldFetchResource() {
        //preparation
        final String resourceId = RandomStringUtils.random(15);
        final String resourceKey = RandomStringUtils.random(15);

        final Product mockProductResult = mock(Product.class);
        when(mockProductResult.getKey()).thenReturn(resourceKey);
        when(mockProductResult.getId()).thenReturn(resourceId);

        final PagedQueryResult<Product> result = mock(PagedQueryResult.class);
        when(result.head()).thenReturn(Optional.of(mockProductResult));

        when(client.execute(any())).thenReturn(completedFuture(result));

        //test
        final Optional<Product> resourceOptional = service.fetchProduct(resourceKey).toCompletableFuture().join();

        //assertions
        assertThat(resourceOptional).containsSame(mockProductResult);
        verify(client).execute(any(ProductQuery.class));
    }

    @Test
    void fetchResource_WithBadGateWayException_ShouldCompleteExceptionally() {
        //preparation
        when(client.execute(any(ProductQuery.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));

        //test
        final CompletionStage<Optional<Product>> result = service.fetchProduct("foo");

        //assertions
        assertThat(result).hasFailedWithThrowableThat().isExactlyInstanceOf(BadGatewayException.class);
        verify(client, times(1)).execute(any(ProductQuery.class));
    }

    @Test
    void cacheKeysToIds_WithEmptySetOfKeys_ShouldMakeNoRequestAndReturnEmptyOptional() {
        //test
        final Map<String, String> optional = service.cacheKeysToIds(emptySet()).toCompletableFuture().join();

        //assertions
        assertThat(optional).isEmpty();
        verify(client, never()).execute(any());
    }

    @Test
    void cacheKeysToIds_WithAllCachedKeys_ShouldMakeNoRequestAndReturnCachedEntry() {
        //preparation
        final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
        final Product mockProductResult = mock(Product.class);
        final String key = "testKey";
        final String id = "testId";
        when(mockProductResult.getKey()).thenReturn(key);
        when(mockProductResult.getId()).thenReturn(id);
        when(pagedQueryResult.getResults()).thenReturn(singletonList(mockProductResult));
        when(client.execute(any())).thenReturn(completedFuture(pagedQueryResult));
        service.getIdFromCacheOrFetch(key).toCompletableFuture().join();

        //test
        final Map<String, String> optional = service.cacheKeysToIds(singleton("testKey")).toCompletableFuture().join();

        //assertions
        assertThat(optional).containsExactly(MapEntry.entry(key, id));
        verify(client, times(1)).execute(any(ProductQuery.class));
    }

    @Test
    void cacheKeysToIds_WithNoCachedKeys_ShouldMakeRequestAndReturnCachedEntry() {
        //preparation
        final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
        final Product mockProductResult = mock(Product.class);
        final String key = "testKey";
        final String id = "testId";
        when(mockProductResult.getKey()).thenReturn(key);
        when(mockProductResult.getId()).thenReturn(id);
        when(pagedQueryResult.getResults()).thenReturn(singletonList(mockProductResult));
        when(client.execute(any())).thenReturn(completedFuture(pagedQueryResult));

        //test
        final Map<String, String> optional = service.cacheKeysToIds(singleton("testKey")).toCompletableFuture().join();

        //assertions
        assertThat(optional).containsExactly(MapEntry.entry(key, id));
        verify(client, times(1)).execute(any(ProductQuery.class));
    }

    @Test
    void cacheKeysToIds_WithBadGateWayException_ShouldCompleteExceptionally() {
        //preparation
        final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
        final Product mockProductResult = mock(Product.class);
        final String key = "testKey";
        final String id = "testId";
        when(mockProductResult.getKey()).thenReturn(key);
        when(mockProductResult.getId()).thenReturn(id);
        when(pagedQueryResult.getResults()).thenReturn(singletonList(mockProductResult));
        when(client.execute(any(ProductQuery.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));

        //test
        final CompletionStage<Map<String, String>> result = service.cacheKeysToIds(singleton("testKey"));

        //assertions
        assertThat(result).hasFailedWithThrowableThat().isExactlyInstanceOf(BadGatewayException.class);
        verify(client, times(1)).execute(any(ProductQuery.class));
    }
}
