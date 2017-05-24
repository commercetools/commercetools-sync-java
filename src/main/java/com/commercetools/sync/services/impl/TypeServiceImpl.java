package com.commercetools.sync.services.impl;


import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.types.queries.TypeQuery;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Implementation of TypeService interface.
 * TODO: USE graphQL to get only keys
 * TODO: UNIT TEST
 * TODO: JAVA DOC
 */
public class TypeServiceImpl implements TypeService {
    private final SphereClient ctpClient;
    private final Map<String, String> cache = new HashMap<>();

    public TypeServiceImpl(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Nullable
    @Override
    public String getCachedTypeKeyById(@Nullable final String id) {
        if (cache.isEmpty()) {
            fetchAllTypesKeysIntoCache().toCompletableFuture().join();
        }
        return cache.get(id);
    }

    @Nonnull
    private CompletionStage<Void> fetchAllTypesKeysIntoCache() {
        return QueryExecutionUtils.queryAll(ctpClient, TypeQuery.of())
          .thenAccept(types -> types.forEach(type -> cache.put(type.getId(), type.getKey())));
    }
}
