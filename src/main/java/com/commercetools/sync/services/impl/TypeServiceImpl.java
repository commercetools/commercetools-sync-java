package com.commercetools.sync.services.impl;


import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.services.TypeService;
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
 * TODO: USE graphQL to get only keys. GITHUB ISSUE#84
 */
public final class TypeServiceImpl implements TypeService {
    private final BaseSyncOptions syncOptions;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();
    private boolean isCached = false;

    public TypeServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
        this.syncOptions = syncOptions;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedTypeId(@Nonnull final String key) {
        if (!isCached) {
            return fetchAndCache(key);
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    private CompletionStage<Optional<String>> fetchAndCache(@Nonnull final String key) {
        final Consumer<List<Type>> typePageConsumer = typesPage ->
            typesPage.forEach(type -> keyToIdCache.put(type.getKey(), type.getId()));

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), TypeQuery.of(), typePageConsumer)
                            .thenAccept(result -> isCached = true)
                            .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }
}
