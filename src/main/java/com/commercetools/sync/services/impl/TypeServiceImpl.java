package com.commercetools.sync.services.impl;


import com.commercetools.sync.commons.helpers.CtpQueryUtils;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.queries.TypeQuery;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Implementation of TypeService interface.
 * TODO: USE graphQL to get only keys.
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
        final Consumer<List<Type>> typePageConsumer = typesPage ->
            typesPage.forEach(type -> keyToIdCache.put(type.getKey(), type.getId()));

        return CtpQueryUtils.queryAll(ctpClient, TypeQuery.of(), typePageConsumer)
                            .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Override
    public void invalidateCache() {
        invalidCache = true;
    }
}
