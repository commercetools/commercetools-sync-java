package com.commercetools.sync.services.impl;


import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.types.queries.TypeQuery;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Implementation of TypeService interface.
 * TODO: USE graphQL to get only keys OR MAKE PR/ISSUE TO FIX QueryExecutionUtils.queryAll
 * TODO: UNIT TEST
 * TODO: JAVA DOC
 */
public class TypeServiceImpl implements TypeService {
    private final SphereClient ctpClient;
    /**
     * Cache of Types' [key -> id].
     */
    private final Map<String, String> cache = new HashMap<>();

    public TypeServiceImpl(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedTypeId(@Nullable final String key) {
        if (cache.isEmpty()) {
            cacheAndFetch(key);
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(cache.get(key)));
    }

    @Nonnull
    private CompletionStage<Optional<String>> cacheAndFetch(@Nullable final String key) {
        return QueryExecutionUtils.queryAll(ctpClient, TypeQuery.of())
                                  .thenApply(types -> {
                                      types.forEach(type -> cache.put(type.getKey(), type.getId()));
                                      return Optional.ofNullable(cache.get(key));
                                  });
    }
}
