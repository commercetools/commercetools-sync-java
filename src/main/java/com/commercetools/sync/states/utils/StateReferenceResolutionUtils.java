package com.commercetools.sync.states.utils;

import static com.commercetools.sync.commons.utils.SyncUtils.getReferenceWithKeyReplaced;

import io.sphere.sdk.models.Reference;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.expansion.StateExpansionModel;
import io.sphere.sdk.states.queries.StateQuery;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class StateReferenceResolutionUtils {

  /**
   * Returns an {@link List}&lt;{@link StateDraft}&gt; consisting of the results of applying the
   * mapping from {@link State} to {@link StateDraft} with considering reference resolution.
   *
   * <table summary="Mapping of Reference fields for the reference resolution">
   *   <thead>
   *     <tr>
   *       <th>Reference field</th>
   *       <th>from</th>
   *       <th>to</th>
   *     </tr>
   *   </thead>
   *   <tbody>
   *     <tr>
   *        <td>transitions</td>
   *        <td>{@link Set}&lt;{@link Reference}&lt;{@link State}&gt;&gt;</td>
   *        <td>{@link Set}&lt;{@link Reference}&lt;{@link State}&gt;&gt; (with key replaced with id field)</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The transition references should be expanded with a key. Any reference that is
   * not expanded will have its id in place and not replaced by the key will be considered as
   * existing resources on the target commercetools project and the library will issues an
   * update/create API request without reference resolution.
   *
   * @param states the states with expanded references.
   * @return a {@link List} of {@link StateDraft} built from the supplied {@link List} of {@link
   *     State}.
   */
  @Nonnull
  public static List<StateDraft> mapToStateDrafts(@Nonnull final List<State> states) {
    return states.stream()
        .filter(Objects::nonNull)
        .map(
            state -> {
              final Set<Reference<State>> newTransitions = replaceTransitionIdsWithKeys(state);
              return StateDraftBuilder.of(state.getKey(), state.getType())
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
      transitions.forEach(
          transition -> {
            newTransitions.add(
                getReferenceWithKeyReplaced(
                    transition, () -> State.referenceOfId(transition.getObj().getKey())));
          });
    }
    return newTransitions;
  }

  /**
   * Builds a {@link StateQuery} for fetching states from a source CTP project with all the needed
   * references expanded for the sync:
   *
   * <ul>
   *   <li>Transition States
   * </ul>
   *
   * <p>Note: Please only use this util if you desire to sync all the aforementioned references from
   * a source commercetools project. Otherwise, it is more efficient to build the query without
   * expansions, if they are not needed, to avoid unnecessarily bigger payloads fetched from the
   * source project.
   *
   * @return the query for fetching states from the source CTP project with all the aforementioned
   *     references expanded.
   */
  public static StateQuery buildStateQuery() {
    return StateQuery.of().withExpansionPaths(StateExpansionModel::transitions);
  }

  private StateReferenceResolutionUtils() {}
}
