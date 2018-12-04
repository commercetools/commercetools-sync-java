package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.StateService;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.queries.StateQuery;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public final class StateServiceImpl implements StateService {
    private final ProductSyncOptions syncOptions;
    private final StateType stateType;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();

    public StateServiceImpl(@Nonnull final ProductSyncOptions syncOptions,
                            @Nonnull final StateType stateType) {
        this.syncOptions = syncOptions;
        this.stateType = stateType;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedStateId(@Nullable final String key) {
        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        if (keyToIdCache.isEmpty()) {
            return fetchAndCache(key);
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    private CompletionStage<Optional<String>> fetchAndCache(@Nonnull final String key) {
        final Consumer<List<State>> statePageConsumer = statePage ->
            statePage.forEach(state -> {
                final String fetchedStateKey = state.getKey();
                final String id = state.getId();
                keyToIdCache.put(fetchedStateKey, id);
            });

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), buildStateQuery(stateType), statePageConsumer)
                            .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }

    /**
     * Builds a {@link StateQuery} based on the given {@link StateType}.
     *
     * @param stateType state type to build the {@link StateQuery} with.
     * @return a {@link StateQuery} based on the given {@link StateType}.
     */
    public static StateQuery buildStateQuery(@Nonnull final StateType stateType) {
        final QueryPredicate<State> stateQueryPredicate =
            QueryPredicate.of(format("type= \"%s\"", stateType.toSphereName()));
        return StateQuery.of().withPredicates(stateQueryPredicate);
    }
}
