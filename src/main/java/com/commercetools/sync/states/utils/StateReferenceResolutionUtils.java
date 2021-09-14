package com.commercetools.sync.states.utils;

import static com.commercetools.sync.commons.utils.SyncUtils.getReferenceWithKeyReplaced;

import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
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
   * <table>
   *   <caption>Mapping of Reference fields for the reference resolution</caption>
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
   * a map). Any reference, which have its id in place and not replaced by the key, it would not be
   * found in the map. In this case, this reference will be considered as existing resources on the
   * target commercetools project and the library will issues an update/create API request without
   * reference resolution..
   *
   * @param states the states without expansion of references.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link List} of {@link StateDraft} built from the supplied {@link List} of {@link
   *     State}.
   */
  @Nonnull
  public static List<StateDraft> mapToStateDrafts(
      @Nonnull final List<State> states,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return states.stream()
        .filter(Objects::nonNull)
        .map(
            state -> {
              final Set<Reference<State>> newTransitions =
                  replaceTransitionIdsWithKeys(state, referenceIdToKeyCache);
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
      @Nonnull final State state, @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    final Set<Reference<State>> transitions = state.getTransitions();

    if (transitions == null) return null;

    final Set<Reference<State>> newTransitions = new HashSet<>();
    if (!transitions.isEmpty()) {
      transitions.forEach(
          transition -> {
            newTransitions.add(
                getReferenceWithKeyReplaced(
                    transition,
                    () -> State.referenceOfId(referenceIdToKeyCache.get(transition.getId())),
                    referenceIdToKeyCache));
          });
    }
    return newTransitions;
  }

  private StateReferenceResolutionUtils() {}
}
