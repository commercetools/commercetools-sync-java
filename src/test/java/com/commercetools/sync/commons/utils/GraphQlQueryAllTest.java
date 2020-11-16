package com.commercetools.sync.commons.utils;


import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQLRequest;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQLResult;
import com.commercetools.sync.commons.models.ResourceKeyId;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static io.sphere.sdk.queries.QueryExecutionUtils.DEFAULT_PAGE_SIZE;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GraphQlQueryAllTest {

    private static final ResourceKeyIdGraphQLResult pagedGraphQlQueryResult = mock(ResourceKeyIdGraphQLResult.class);
    private static final SphereClient sphereClient = mock(SphereClient.class);

    /**
     * Prepares mock data for unit testing the CTP GraphQlQueryAll utilities; specifically stubs a {@link SphereClient}
     * to always return a mocked {@link ResourceKeyIdGraphQLResult} on every call of {@link SphereClient#execute(SphereRequest)}.
     */
    @BeforeAll
    static void setup() {
        when(sphereClient.execute(any())).thenReturn(CompletableFuture.completedFuture(pagedGraphQlQueryResult));
    }

    @Test
    void run_WithConsumer_ShouldApplyConsumer() {
        //preparation
        ResourceKeyId resource1 = new ResourceKeyId("key1", "id1");
        ResourceKeyId resource2 = new ResourceKeyId("key2", "id2");
        ResourceKeyId resource3 = new ResourceKeyId("key3", "id3");
        ResourceKeyId resource4 = new ResourceKeyId("key4", "id4");

        Set<ResourceKeyId> results = new HashSet<>();
        results.add(resource1);
        results.add(resource2);
        results.add(resource3);
        results.add(resource4);
        when(pagedGraphQlQueryResult.getResults()).thenReturn(results);

        final GraphQlQueryAll query = GraphQlQueryAll.of(sphereClient, mock(ResourceKeyIdGraphQLRequest.class), DEFAULT_PAGE_SIZE);
        final List<String> resourceIds = new ArrayList<>();

        final Consumer<Set<ResourceKeyId>> resourceIdCollector = page ->
            page.forEach(resource -> resourceIds.add(resource.getId()));

        //test
        query.run(resourceIdCollector).toCompletableFuture().join();

        //assertions
        assertThat(resourceIds).hasSize(4);
        assertThat(resourceIds).containsExactlyInAnyOrder("id2", "id1", "id3", "id4");
    }

    @Test
    void run_WithEmptyResults_ShouldSkipConsumer() {
        //preparation
        when(pagedGraphQlQueryResult.getResults()).thenReturn(emptySet());

        final GraphQlQueryAll query = GraphQlQueryAll.of(sphereClient, mock(ResourceKeyIdGraphQLRequest.class), DEFAULT_PAGE_SIZE);
        final List<String> resourceIds = new ArrayList<>();

        final Consumer<Set<ResourceKeyId>> resourceIdCollector = page ->
            page.forEach(resource -> resourceIds.add(resource.getId()));

        //test
        query.run(resourceIdCollector).toCompletableFuture().join();

        //assertions
        assertThat(resourceIds).isEmpty();
    }
}
