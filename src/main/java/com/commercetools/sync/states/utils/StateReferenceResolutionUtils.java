package com.commercetools.sync.states.utils;

import static com.commercetools.sync.commons.utils.SyncUtils.getReferenceWithKeyReplaced;

import io.sphere.sdk.models.Reference;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.queries.StateQuery;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
   * <p><b>Note:</b> The transition references should be cached(idToKey value fetched and stored in
   * a map). Any reference that is not cached will have its id in place and not replaced by the key
   * will be considered as existing resources on the target commercetools project and the library
   * will issues an update/create API request without reference resolution..
   *
   * @param states the states without expansion of references.
   * @return a {@link List} of {@link StateDraft} built from the supplied {@link List} of {@link
   *     State}.
   */
  @Nonnull
  public static List<StateDraft> mapToStateDrafts(
      @Nonnull final List<State> states, @Nonnull final Map<String, String> referenceIdToKeyMap) {
    return states.stream()
        .filter(Objects::nonNull)
        .map(
            state -> {
              final Set<Reference<State>> newTransitions =
                  replaceTransitionIdsWithKeys(state, referenceIdToKeyMap);
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

  private static Set<Reference<State>> replaceTransitionIdsWithKeys(
      @Nonnull final State state, @Nonnull final Map<String, String> referenceIdToKeyMap) {
    final Set<Reference<State>> transitions = state.getTransitions();
    final Set<Reference<State>> newTransitions = new HashSet<>();
    if (transitions != null && !transitions.isEmpty()) {
      transitions.forEach(
          transition -> {
            newTransitions.add(
                getReferenceWithKeyReplaced(
                    transition,
                    () -> State.referenceOfId(referenceIdToKeyMap.get(transition.getId())),
                    referenceIdToKeyMap));
          });
    }
    return newTransitions;
  }

  /**
   * Builds a {@link StateQuery} for fetching states from a source CTP project.
   *
   * @return the query for fetching states from the source CTP project without any references
   *     expanded.
   */
  public static StateQuery buildStateQuery() {
    return StateQuery.of();
  }

  private StateReferenceResolutionUtils() {}
}
