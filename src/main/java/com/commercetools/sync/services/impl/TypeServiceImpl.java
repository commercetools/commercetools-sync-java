package com.commercetools.sync.services.impl;


import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.types.TypeSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.commands.TypeUpdateCommand;
import io.sphere.sdk.types.queries.TypeQuery;
import io.sphere.sdk.types.queries.TypeQueryBuilder;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    public TypeServiceImpl(@Nonnull final TypeSyncOptions syncOptions) {
        this.syncOptions = syncOptions;
    }

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
    @Override
    public CompletionStage<List<Type>> fetchMatchingTypesByKeys(@Nonnull final Set<String> keys) {
        final TypeQuery query = TypeQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().isIn(keys))
                .build();

        return QueryExecutionUtils.queryAll(syncOptions.getCtpClient(), query);
    }

    @Nonnull
    @Override
    public CompletionStage<Type> createType(@Nonnull TypeDraft typeDraft) {
        return syncOptions.getCtpClient().execute(TypeCreateCommand.of(typeDraft));
    }

    @Nonnull
    @Override
    public CompletionStage<Type> updateType(@Nonnull Type type, @Nonnull List<UpdateAction<Type>> updateActions) {
        return syncOptions.getCtpClient().execute(TypeUpdateCommand.of(type, updateActions));
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
