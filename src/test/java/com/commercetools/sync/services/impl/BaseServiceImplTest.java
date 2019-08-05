package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.expansion.ExpansionPathContainer;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.models.WithKey;
import io.sphere.sdk.queries.MetaModelQueryDsl;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QuerySort;
import io.sphere.sdk.queries.ResourceQueryModel;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseServiceImplTest {

    private TestQuery query = mock(TestQuery.class);
    private TestResource resource = mock(TestResource.class);
    @SuppressWarnings("unchecked")
    private Consumer<String> warningCallback = mock(Consumer.class);
    private SphereClient client = mock(SphereClient.class);
    private TestServiceImpl service;

    class TestServiceImpl extends BaseService<TestDraft, TestResource, TestSyncOptions, TestQuery, TestQueryModel,
        TestExpansionModel<TestResource>> {

        TestServiceImpl(@Nonnull final TestSyncOptions syncOptions) {
            super(syncOptions);
        }

        @Override
        CompletionStage<Optional<String>> fetchAndCache(@Nonnull final String key) {
            return fetchAndCache(key, () -> query, TestResource::getKey, "TestResource");
        }

        void cache(final String key, final String id) {
            keyToIdCache.put(key, id);
        }

        void clearCached() {
            keyToIdCache.clear();
        }

    }

    interface TestResource extends Resource<TestResource>, WithKey {

    }

    interface TestDraft {

    }

    static class TestSyncOptions extends BaseSyncOptions<TestResource, TestDraft> {

        TestSyncOptions(@Nonnull final SphereClient ctpClient,
                        @Nullable final BiConsumer<String, Throwable> errorCallBack,
                        @Nullable final Consumer<String> warningCallBack,
                        int batchSize,
                        @Nullable final TriFunction<List<UpdateAction<TestResource>>, TestDraft, TestResource,
                            List<UpdateAction<TestResource>>> beforeUpdateCallback,
                        @Nullable final Function<TestDraft, TestDraft> beforeCreateCallback) {
            super(ctpClient, errorCallBack, warningCallBack, batchSize, beforeUpdateCallback, beforeCreateCallback);
        }
    }

    interface TestQuery extends MetaModelQueryDsl<TestResource, TestQuery, TestQueryModel,
        TestExpansionModel<TestResource>> {

    }

    interface TestQueryModel extends ResourceQueryModel<TestResource> {

    }

    interface TestExpansionModel<T> extends ExpansionPathContainer<T> {

    }

    private interface TestPagedQueryResult extends PagedQueryResult<TestResource> {
    }

    @BeforeEach
    void setup() {
        final TestSyncOptions syncOptions = new TestSyncOptions(client, null, warningCallback, 20, null, null);
        service = new TestServiceImpl(syncOptions);
    }

    @AfterEach
    void cleanup() {
        reset(client, query, resource, warningCallback);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void fetchCachedResourceId_WithInvalidKey_ShouldReturnEmpty(final String key) {
        //test
        final Optional<String> result = service.fetchCachedResourceId(key).toCompletableFuture().join();

        //assertions
        assertThat(result).isEmpty();
        verify(client, never()).execute(any(TestQuery.class));
    }

    @Test
    void fetchCachedResourceId_WithFetchReturnResourceWithoutKey_ShouldReturnEmptyAndTriggerWarningCallback() {
        //preparation
        when(query.sort()).thenReturn(Collections.singletonList(QuerySort.of("id asc")));
        when(query.withLimit(any())).thenReturn(query);

        final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
        when(pagedQueryResult.getResults()).thenReturn(Collections.singletonList(resource));

        when(resource.getKey()).thenReturn(null);

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(pagedQueryResult));

        //test
        final Optional<String> result = service.fetchCachedResourceId("testKey").toCompletableFuture().join();

        //assertions
        assertThat(result).isEmpty();
        verify(warningCallback).accept(any());
    }

    @Test
    void fetchCachedResourceId_WithFetchReturnResource_ShouldReturnResourceId() {
        //preparation
        when(query.sort()).thenReturn(Collections.singletonList(QuerySort.of("id asc")));
        when(query.withLimit(any())).thenReturn(query);

        final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
        when(pagedQueryResult.getResults()).thenReturn(Collections.singletonList(resource));

        final String key = "testKey";
        final String id = "testId";
        when(resource.getKey()).thenReturn(key);
        when(resource.getId()).thenReturn(id);

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(pagedQueryResult));

        //test
        final String result = service.fetchCachedResourceId(key).toCompletableFuture().join().orElse(null);

        //assertions
        assertThat(result).isEqualTo(id);

        //additional cleanup
        service.clearCached();
    }

    @Test
    void fetchCachedResourceId_WithCachedResource_ShouldReturnResourceIdWithoutMakingRequest() {
        //preparation
        final String key = "testKey";
        final String id = "testId";
        service.cache(key, id);

        //test
        final String result = service.fetchCachedResourceId(key).toCompletableFuture().join().orElse(null);

        //assertions
        assertThat(result).isEqualTo(id);
        verify(client, never()).execute(any(TestQuery.class));

        //additional cleanup
        service.clearCached();
    }

    @Test
    void fetchMatchingResources_WithEmptyKeySet_ShouldFetchAndCacheNothing() {
        //test
        final Set<TestResource> resources = service.fetchMatchingResources(new HashSet<>(),
            () -> null, TestResource::getKey).toCompletableFuture().join();

        //assertions
        assertAll(
            () -> assertThat(resources).isEmpty(),
            () -> assertThat(service.keyToIdCache).isEmpty()
        );
        verify(client, never()).execute(any(TestQuery.class));
    }

    @Test
    void fetchMatchingResources_WithKeySet_ShouldFetchResourcesAndCacheKeys() {
        //preparation
        final String key1 = RandomStringUtils.random(15);
        final String key2 = RandomStringUtils.random(15);

        final HashSet<String> resourceKeys = new HashSet<>();
        resourceKeys.add(key1);
        resourceKeys.add(key2);

        final TestResource mock1 = mock(TestResource.class);
        when(mock1.getId()).thenReturn(RandomStringUtils.random(15));
        when(mock1.getKey()).thenReturn(key1);

        final TestResource mock2 = mock(TestResource.class);
        when(mock2.getId()).thenReturn(RandomStringUtils.random(15));
        when(mock2.getKey()).thenReturn(key2);

        final TestPagedQueryResult result = mock(TestPagedQueryResult.class);
        when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));

        when(client.execute(any(TestQuery.class))).thenReturn(completedFuture(result));

        final TestQuery testQuery = mock(TestQuery.class);
        when(testQuery.sort()).thenReturn(Collections.singletonList(QuerySort.of("id asc")));
        when(testQuery.withLimit(anyLong())).thenReturn(testQuery);

        //test
        final Set<TestResource> resources = service.fetchMatchingResources(resourceKeys,
            () -> testQuery, TestResource::getKey).toCompletableFuture().join();

        //assertions
        assertAll(
            () -> assertThat(resources).contains(mock1, mock2),
            () -> assertThat(service.keyToIdCache).containsKeys(key1, key2)
        );
        verify(client).execute(any(TestQuery.class));
    }

    @Test
    void fetchMatchingResources_WithBadGateWayException_ShouldCompleteExceptionally() {
        //preparation
        final String key1 = RandomStringUtils.random(15);
        final String key2 = RandomStringUtils.random(15);

        final HashSet<String> resourceKeys = new HashSet<>();
        resourceKeys.add(key1);
        resourceKeys.add(key2);

        when(client.execute(any(TestQuery.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));


        final TestQuery testQuery = mock(TestQuery.class);
        when(testQuery.sort()).thenReturn(Collections.singletonList(QuerySort.of("id asc")));
        when(testQuery.withLimit(anyLong())).thenReturn(testQuery);

        //test
        final CompletionStage<Set<TestResource>> result = service.fetchMatchingResources(resourceKeys,
            () -> testQuery, TestResource::getKey);

        //assertions
        assertThat(result).hasFailedWithThrowableThat().isExactlyInstanceOf(BadGatewayException.class);
        verify(client).execute(any(TestQuery.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void fetchResource_WithInvalidKey_ShouldFetchNothing(final String key) {
        //test
        final Optional<TestResource> optional = service.fetchResource(key,
            () -> null).toCompletableFuture().join();

        //assertions
        assertThat(optional).isEmpty();
        verify(client, never()).execute(any(TestQuery.class));
    }

    @Test
    void fetchResource_WithKey_ShouldFetchResource() {
        //preparation
        final String resourceId = RandomStringUtils.random(15);
        final String resourceKey = RandomStringUtils.random(15);

        final TestResource mock = mock(TestResource.class);
        when(mock.getId()).thenReturn(resourceId);
        when(mock.getKey()).thenReturn(resourceKey);
        final TestPagedQueryResult result = mock(TestPagedQueryResult.class);
        when(result.head()).thenReturn(Optional.of(mock));

        when(client.execute(any())).thenReturn(completedFuture(result));

        //test
        final Optional<TestResource> resourceOptional = service.fetchResource(resourceKey,
            () -> mock(TestQuery.class)).toCompletableFuture().join();

        //assertions
        assertAll(
            () -> assertThat(resourceOptional).containsSame(mock),
            () -> assertThat(service.keyToIdCache.get(resourceKey)).isEqualTo(resourceId)
        );
        verify(client).execute(any(TestQuery.class));
    }

    @Test
    void fetchResource_WithBadGateWayException_ShouldCompleteExceptionally() {
        //preparation
        when(client.execute(any(TestQuery.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));

        //test
        final CompletionStage<Optional<TestResource>> result = service.fetchResource("foo",
            () -> mock(TestQuery.class));

        //assertions
        assertThat(result).hasFailedWithThrowableThat().isExactlyInstanceOf(BadGatewayException.class);
        verify(client).execute(any(TestQuery.class));
    }

}
