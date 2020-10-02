package com.commercetools.sync.services.impl;

import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.stores.Store;
import io.sphere.sdk.stores.queries.StoreQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StoreServiceImplTest {

    private StoreServiceImpl service;
    private CustomerSyncOptions customerSyncOptions;
    private List<String> errorMessages;
    private List<Throwable> errorExceptions;

    @BeforeEach
    void setUp() {
        errorMessages = new ArrayList<>();
        errorExceptions = new ArrayList<>();
        customerSyncOptions = CustomerSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorMessages.add(exception.getMessage());
                errorExceptions.add(exception.getCause());
            })
            .build();
        service = new StoreServiceImpl(customerSyncOptions);
    }

    @Test
    void fetchCachedStoreId_WithBlankKey_ShouldNotFetchCustomerId() {
        Optional<String> customerId = service.fetchCachedStoreId("")
                                             .toCompletableFuture()
                                             .join();

        assertThat(customerId).isEmpty();
        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        verifyNoInteractions(customerSyncOptions.getCtpClient());
    }

    @Test
    void fetchCachedStoreId_WithCachedStore_ShouldFetchIdFromCache() {
        service.keyToIdCache.put("key", "id");
        Optional<String> customerId = service.fetchCachedStoreId("key")
                                             .toCompletableFuture()
                                             .join();

        assertThat(customerId).contains("id");
        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        verifyNoInteractions(customerSyncOptions.getCtpClient());
    }

    @Test
    void fetchCachedStoreId_WithUnexpectedException_ShouldFail() {
        when(customerSyncOptions.getCtpClient().execute(any())).thenReturn(
            CompletableFutureUtils.failed(new BadGatewayException("bad gateway")));

        assertThat(service.fetchCachedStoreId("key"))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
    }

    @Test
    void cacheKeysToIds_WithNonExistingKey_ShouldReturnEmptyMap() {
        when(customerSyncOptions.getCtpClient().execute(any(StoreQuery.class)))
            .thenReturn(completedFuture(PagedQueryResult.empty()));

        Map<String, String> cache = service.cacheKeysToIds(singleton("non-existing-key")).toCompletableFuture().join();
        assertThat(cache).hasSize(0);

        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
    }

    @Test
    void cacheKeysToIds_WithExistingKey_ShouldReturnWithMap() {
        @SuppressWarnings("unchecked")
        final PagedQueryResult<Store> pagedQueryResult = mock(PagedQueryResult.class);
        final Store mockStore = mock(Store.class);
        when(mockStore.getKey()).thenReturn("key");
        when(mockStore.getId()).thenReturn("id");
        when(pagedQueryResult.getResults()).thenReturn(singletonList(mockStore));

        when(customerSyncOptions.getCtpClient().execute(any(StoreQuery.class)))
            .thenReturn(completedFuture(pagedQueryResult));

        Map<String, String> cache = service.cacheKeysToIds(singleton("key")).toCompletableFuture().join();
        assertThat(cache).hasSize(1);
        assertThat(cache.get("key")).isEqualTo("id");

        cache = service.cacheKeysToIds(emptySet()).toCompletableFuture().join();
        assertThat(cache).hasSize(1);

        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
    }
}
