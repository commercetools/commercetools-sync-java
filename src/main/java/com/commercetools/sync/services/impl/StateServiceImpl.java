package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.StateService;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.queries.StateQuery;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static java.lang.String.format;

public class StateServiceImpl implements StateService {
    private final ProductSyncOptions syncOptions;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();

    public StateServiceImpl(@Nonnull final ProductSyncOptions syncOptions) {
        this.syncOptions = syncOptions;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedStateId(@Nonnull final String key) {
        if (keyToIdCache.isEmpty()) {
            return cacheAndFetch(key);
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key) {
        final Consumer<List<State>> statePageConsumer = statePage ->
            statePage.forEach(state -> {
                final String fetchedStateKey = state.getKey();
                final String id = state.getId();
                if (StringUtils.isNotBlank(fetchedStateKey)) {
                    keyToIdCache.put(fetchedStateKey, id);
                } else {
                    syncOptions.applyWarningCallback(format("State with id: '%s' has no key set. Keys are required for"
                        + " state matching.", id));
                }
            });

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), StateQuery.of(), statePageConsumer)
                            .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }
}
