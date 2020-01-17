package com.commercetools.sync.services.impl;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.StateService;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.expansion.StateExpansionModel;
import io.sphere.sdk.states.queries.StateQuery;
import io.sphere.sdk.states.queries.StateQueryBuilder;
import io.sphere.sdk.states.queries.StateQueryModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static java.util.Collections.singleton;

public final class StateServiceImpl
    extends BaseServiceWithKey<StateDraft, State, ProductSyncOptions, StateQuery, StateQueryModel,
        StateExpansionModel<State>> implements StateService {

    public StateServiceImpl(@Nonnull final ProductSyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedStateId(@Nullable final String key) {
        return fetchCachedResourceId(key,
            () -> StateQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().isIn(singleton(key)))
                .build());
    }
}
