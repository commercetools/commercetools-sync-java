package com.commercetools.sync.services.impl;

import com.commercetools.sync.services.StateService;
import com.commercetools.sync.states.StateSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.commands.StateCreateCommand;
import io.sphere.sdk.states.commands.StateUpdateCommand;
import io.sphere.sdk.states.expansion.StateExpansionModel;
import io.sphere.sdk.states.queries.StateQuery;
import io.sphere.sdk.states.queries.StateQueryBuilder;
import io.sphere.sdk.states.queries.StateQueryModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Collections.singleton;

public final class StateServiceImpl
        extends BaseServiceWithKey<StateDraft, State, StateSyncOptions, StateQuery, StateQueryModel,
        StateExpansionModel<State>> implements StateService {

    private static final String STATE_KEY_NOT_SET = "State with id: '%s' has no key set. Keys are required for "
        + "state matching.";

    public StateServiceImpl(@Nonnull final StateSyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> keys) {

        return cacheKeysToIds(
            keys,
            keysNotCached -> StateQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().isIn(keysNotCached))
                .build());
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

    @Nonnull
    @Override
    public CompletionStage<Set<State>> fetchMatchingStatesByKeys(@Nonnull final Set<String> stateKeys) {
        return fetchMatchingStates(stateKeys, false);
    }

    @Nonnull
    @Override
    public CompletionStage<Set<State>> fetchMatchingStatesByKeysWithTransitions(@Nonnull final Set<String> stateKeys) {
        return fetchMatchingStates(stateKeys, true);
    }

    private CompletionStage<Set<State>> fetchMatchingStates(@Nonnull final Set<String> stateKeys,
                                                            final boolean withTransitions) {
        return fetchMatchingResources(stateKeys,
            () -> {
                StateQuery stateQuery = StateQuery.of()
                    .plusPredicates(stateQueryModel -> stateQueryModel.key().isIn(stateKeys));
                if (withTransitions) {
                    stateQuery = stateQuery.withExpansionPaths(StateExpansionModel::transitions);
                }
                return stateQuery;
            });
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<State>> fetchState(@Nullable final String key) {
        return fetchResource(key,
            () -> StateQuery.of().plusPredicates(stateQueryModel -> stateQueryModel.key().is(key)));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<State>> createState(@Nonnull final StateDraft stateDraft) {
        return createResource(stateDraft, StateCreateCommand::of);
    }

    @Nonnull
    @Override
    public CompletionStage<State> updateState(
        @Nonnull final State state,
        @Nonnull final List<UpdateAction<State>> updateActions) {
        return updateResource(state, StateUpdateCommand::of, updateActions);
    }
}
