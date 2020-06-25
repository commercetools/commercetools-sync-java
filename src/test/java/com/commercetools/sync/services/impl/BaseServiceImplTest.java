package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.expansion.ExpansionPathContainer;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.models.WithKey;
import io.sphere.sdk.queries.MetaModelQueryDsl;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.queries.QuerySort;
import io.sphere.sdk.queries.ResourceQueryModel;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        TestSyncOptions syncOptions = new TestSyncOptions(client, null, warningCallback, 20, null, null);
        service = new TestServiceImpl(syncOptions);
    }

    @AfterEach
    void cleanup() {
        reset(client, query, resource, warningCallback);
    }

    @Test
    void fetchCachedResourceId_WithEmptyKey_ShouldReturnEmpty() {
        Optional<String> result = service.fetchCachedResourceId("").toCompletableFuture().join();

        assertThat(result).isEmpty();
    }

    @Test
    void fetchCachedResourceId_WithFetchReturnResourceWithoutKey_ShouldReturnEmptyAndTriggerWarningCallback() {
        when(query.sort()).thenReturn(Collections.singletonList(QuerySort.of("id asc")));
        when(query.withLimit(any())).thenReturn(query);

        PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
        when(pagedQueryResult.getResults()).thenReturn(Collections.singletonList(resource));

        when(resource.getKey()).thenReturn(null);

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(pagedQueryResult));

        Optional<String> result = service.fetchCachedResourceId("testKey").toCompletableFuture().join();

        assertThat(result).isEmpty();
        verify(warningCallback).accept(any());
    }

    @Test
    void fetchCachedResourceId_WithFetchReturnResource_ShouldReturnResourceId() {
        when(query.sort()).thenReturn(Collections.singletonList(QuerySort.of("id asc")));
        when(query.withLimit(any())).thenReturn(query);

        PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
        when(pagedQueryResult.getResults()).thenReturn(Collections.singletonList(resource));

        String key = "testKey";
        String id = "testId";
        when(resource.getKey()).thenReturn(key);
        when(resource.getId()).thenReturn(id);

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(pagedQueryResult));

        String result = service.fetchCachedResourceId(key).toCompletableFuture().join().orElse(null);

        assertThat(result).isEqualTo(id);

        service.clearCached();
    }

    @Test
    void fetchCachedResourceId_WithCachedResource_ShouldReturnResourceId() {
        String key = "testKey";
        String id = "testId";
        service.cache(key, id);

        String result = service.fetchCachedResourceId(key).toCompletableFuture().join().orElse(null);

        assertThat(result).isEqualTo(id);

        service.clearCached();
    }

    @Test
    void fetchMatchingResources_WithEmptyKeySet_ShouldFetchNothing() {
        Set<TestResource> resources = service.fetchMatchingResources(new HashSet<>(),
            () -> null, TestResource::getKey).toCompletableFuture().join();
        assertAll(
            () -> assertThat(resources).isEmpty(),
            () -> assertThat(service.keyToIdCache).isEmpty()
        );
        verify(client, never()).execute(any(TestQuery.class));
    }

    @Test
    void fetchMatchingResources_WithKeySet_ShouldFetchResources() {
        String key1 = RandomStringUtils.random(15);
        String key2 = RandomStringUtils.random(15);

        HashSet<String> resourceKeys = new HashSet<>();
        resourceKeys.add(key1);
        resourceKeys.add(key2);

        TestResource mock1 = mock(TestResource.class);
        when(mock1.getId()).thenReturn(RandomStringUtils.random(15));
        when(mock1.getKey()).thenReturn(key1);

        TestResource mock2 = mock(TestResource.class);
        when(mock2.getId()).thenReturn(RandomStringUtils.random(15));
        when(mock2.getKey()).thenReturn(key2);

        TestPagedQueryResult result = mock(TestPagedQueryResult.class);
        when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));

        when(client.execute(any())).thenReturn(completedFuture(result));

        TestQuery testQuery = mock(TestQuery.class);
        when(testQuery.sort()).thenReturn(Collections.singletonList(QuerySort.of("id asc")));
        when(testQuery.withLimit(anyLong())).thenReturn(testQuery);

        Set<TestResource> resources = service.fetchMatchingResources(resourceKeys,
            () -> testQuery, TestResource::getKey).toCompletableFuture().join();

        assertAll(
            () -> assertThat(resources).isNotEmpty(),
            () -> assertThat(resources).contains(mock1, mock2),
            () -> assertThat(service.keyToIdCache).containsKeys(key1, key2)
        );
        verify(client).execute(any(TestQuery.class));
    }

    @Test
    void fetchResource_WithNullKey_ShouldFetchNothing() {
        Optional<TestResource> optional = service.fetchResource(null,
            () -> null, TestResource::getKey).toCompletableFuture().join();
        assertThat(optional).isEmpty();
    }

    @Test
    void fetchResource_WithKey_ShouldFetchResource() {
        String resourceId = RandomStringUtils.random(15);
        String resourceKey = RandomStringUtils.random(15);

        TestResource mock = mock(TestResource.class);
        when(mock.getId()).thenReturn(resourceId);
        when(mock.getKey()).thenReturn(resourceKey);
        TestPagedQueryResult result = mock(TestPagedQueryResult.class);
        when(result.head()).thenReturn(Optional.of(mock));

        when(client.execute(any())).thenReturn(completedFuture(result));

        Optional<TestResource> resourceOptional = service.fetchResource(resourceKey,
            () -> mock(TestQuery.class), TestResource::getKey).toCompletableFuture().join();

        assertAll(
            () -> assertThat(resourceOptional).isNotEmpty(),
            () -> assertThat(resourceOptional).containsSame(mock),
            () -> assertThat(service.keyToIdCache.get(resourceKey)).isEqualTo(resourceId)
        );
        verify(client).execute(any(TestQuery.class));
    }

    @Test
    void buildResourceKeysQueryPredicate_WithEmptyKeySet_ShouldBuildQueryPredicate() {
        QueryPredicate<TestResource> queryPredicate = service.buildResourceKeysQueryPredicate(new HashSet<>());

        assertThat(queryPredicate.toSphereQuery()).isEqualTo("key in ()");
    }

    @Test
    void buildResourceKeysQueryPredicate_WithKeySet_ShouldBuildQueryPredicate() {
        String key1 = RandomStringUtils.random(15);
        String key2 = RandomStringUtils.random(15);

        HashSet<String> stateKeys = new HashSet<>();
        stateKeys.add(key1);
        stateKeys.add(key2);

        QueryPredicate<TestResource> queryPredicate = service.buildResourceKeysQueryPredicate(stateKeys);

        assertThat(queryPredicate.toSphereQuery())
            .isIn("key in (\"" + key1 + "\", \"" + key2 + "\")", "key in (\"" + key2 + "\", \"" + key1 + "\")");
    }

    @Test
    void buildResourceKeysQueryPredicate_WithKeySetContainingInvalidKeys_ShouldBuildQueryPredicate() {
        String key1 = RandomStringUtils.random(15);
        String key2 = RandomStringUtils.random(15);

        HashSet<String> stateKeys = new HashSet<>();
        stateKeys.add(key1);
        stateKeys.add(key2);
        stateKeys.add("");
        stateKeys.add(null);

        QueryPredicate<TestResource> queryPredicate = service.buildResourceKeysQueryPredicate(stateKeys);

        assertThat(queryPredicate.toSphereQuery())
            .isIn("key in (\"" + key1 + "\", \"" + key2 + "\")", "key in (\"" + key2 + "\", \"" + key1 + "\")");
    }

}
