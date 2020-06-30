package com.commercetools.sync.states.utils;

import io.sphere.sdk.models.Reference;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.expansion.StateExpansionModel;
import io.sphere.sdk.states.queries.StateQuery;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.SyncUtils.getReferenceWithKeyReplaced;

/**
 * Util class which provides utilities that can be used when syncing resources from a source commercetools project
 * to a target one.
 */
public final class StateTransitionReferenceReplacementUtils {

    /**
     * Takes a list of States that are supposed to have their transitions expanded in order to be able to
     * fetch the keys and replace the transition ids with the corresponding
     * keys and then return a new list of state drafts with their transitions containing keys instead of the ids.
     *
     * <p><b>Note:</b>If the transitions are not expanded for a state, the transition ids will not be replaced with keys
     * and will still have their ids in place.
     *
     * @param states the states to replace their transition ids with keys
     * @return a list of state drafts with keys instead of ids for transitions.
     */
    @Nonnull
    public static List<StateDraft> replaceStateTransitionIdsWithKeys(@Nonnull final List<State> states) {
        return states
            .stream()
            .filter(Objects::nonNull)
            .map(state -> {
                final Set<Reference<State>> newTransitions = replaceTransitionIdsWithKeys(state);
                return StateDraftBuilder
                    .of(state.getKey(), state.getType())
                    .name(state.getName())
                    .description(state.getDescription())
                    .initial(state.isInitial())
                    .roles(state.getRoles())
                    .transitions(newTransitions)
                    .build();
            })
            .collect(Collectors.toList());
    }

    private static Set<Reference<State>> replaceTransitionIdsWithKeys(@Nonnull final State state) {
        final Set<Reference<State>> transitions = state.getTransitions();
        final Set<Reference<State>> newTransitions = new HashSet<>();
        if (transitions != null && !transitions.isEmpty()) {
            transitions.forEach(transition -> {
                newTransitions.add(getReferenceWithKeyReplaced(transition,
                    () -> State.referenceOfId(transition.getObj().getKey())));
            });
        }
        return newTransitions;
    }

    /**
     * Builds a {@link StateQuery} for fetching states from a source CTP project with all the needed transitions
     * expanded for the sync.
     *
     * @return the query for fetching states from the source CTP project with all the aforementioned transitions
     *         expanded.
     */
    @Nonnull
    public static StateQuery buildStateQuery() {
        return StateQuery.of()
                     .withLimit(QueryExecutionUtils.DEFAULT_PAGE_SIZE)
                     .withExpansionPaths(StateExpansionModel::transitions);
    }

    private StateTransitionReferenceReplacementUtils() {
    }
}
