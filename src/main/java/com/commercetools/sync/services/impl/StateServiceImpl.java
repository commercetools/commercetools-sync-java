package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.states.StateSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.StateCreateCommand;
import io.sphere.sdk.states.commands.StateUpdateCommand;
import io.sphere.sdk.states.expansion.StateExpansionModel;
import io.sphere.sdk.states.queries.StateQuery;
import io.sphere.sdk.states.queries.StateQueryModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;
import static java.util.Collections.singleton;

public final class StateServiceImpl extends BaseService<StateDraft, State, BaseSyncOptions, StateQuery,
    StateQueryModel, StateExpansionModel<State>> implements StateService {

    public StateServiceImpl(@Nonnull final StateSyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedStateId(@Nullable final String key) {
        return fetchCachedResourceId(key);
    }

    @Nonnull
    @Override
    CompletionStage<Optional<String>> fetchAndCache(@Nonnull final String key) {
        return fetchAndCache(key, StateQuery::of, State::getKey, "State");
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
                                                            boolean withTransitions) {
        return fetchMatchingResources(
            stateKeys,
            () -> {
                StateQuery stateQuery = StateQuery.of().withPredicates(buildResourceKeysQueryPredicate(stateKeys));
                if (withTransitions) {
                    stateQuery = stateQuery.withExpansionPaths(StateExpansionModel::transitions);
                }
                return stateQuery;
            },
            State::getKey);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<State>> fetchState(@Nullable final String key) {
        return fetchResource(key, () -> StateQuery.of().withPredicates(buildResourceKeysQueryPredicate(singleton(key))),
            State::getKey);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<State>> createState(@Nonnull final StateDraft stateDraft) {
        return createResource(stateDraft, StateDraft::getKey, StateCreateCommand::of);
    }

    @Nonnull
    @Override
    public CompletionStage<State> updateState(@Nonnull final State state,
                                              @Nonnull final List<UpdateAction<State>> updateActions) {
        return updateResource(state, StateUpdateCommand::of, updateActions);
    }

}
