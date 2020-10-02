package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.services.StoreService;
import io.sphere.sdk.stores.Store;
import io.sphere.sdk.stores.StoreDraft;
import io.sphere.sdk.stores.expansion.StoreExpansionModel;
import io.sphere.sdk.stores.queries.StoreQuery;
import io.sphere.sdk.stores.queries.StoreQueryBuilder;
import io.sphere.sdk.stores.queries.StoreQueryModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public final class StoreServiceImpl
    extends BaseService<StoreDraft, Store, BaseSyncOptions, StoreQuery,
    StoreQueryModel, StoreExpansionModel<Store>> implements StoreService {

    public StoreServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> storeKeys) {

        return cacheKeysToIds(storeKeys, Store::getKey,
            keysNotCached -> StoreQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().isIn(keysNotCached))
                .build());
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedStoreId(@Nullable final String key) {

        return fetchCachedResourceId(key, Store::getKey, () -> StoreQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().is(key))
                .build());
    }
}
