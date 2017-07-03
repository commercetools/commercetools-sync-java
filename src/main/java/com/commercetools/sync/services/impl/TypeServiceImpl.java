package com.commercetools.sync.services.impl;


import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.types.queries.TypeQuery;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of TypeService interface.
 * TODO: USE graphQL to get only keys OR MAKE PR/ISSUE TO FIX QueryExecutionUtils.queryAll
 * TODO: INTEGRATION TEST GITHUB ISSUE#7
 */
public final class TypeServiceImpl implements TypeService {
    private final SphereClient ctpClient;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();
    private boolean invalidCache = false;

    public TypeServiceImpl(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedTypeId(@Nonnull final String key) {
        if (keyToIdCache.isEmpty() || invalidCache) {
            return cacheAndFetch(key);
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key) {
        return QueryExecutionUtils.queryAll(ctpClient, TypeQuery.of())
                                  .thenApply(types -> {
                                      types.forEach(type -> keyToIdCache.put(type.getKey(), type.getId()));
                                      return Optional.ofNullable(keyToIdCache.get(key));
                                  });
    }

    @Override
    public void invalidateCache() {
        invalidCache = true;
    }
}
