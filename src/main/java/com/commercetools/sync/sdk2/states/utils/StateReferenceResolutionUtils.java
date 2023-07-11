package com.commercetools.sync.sdk2.states.utils;

import static com.commercetools.sync.sdk2.commons.utils.SyncUtils.getResourceIdentifierWithKey;

import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.state.StateResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class StateReferenceResolutionUtils {

  /**
   * Returns an {@link java.util.List}&lt;{@link StateDraft}&gt; consisting of the results of
   * applying the mapping from {@link State} to {@link StateDraft} with considering reference
   * resolution.
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
   *        <td>{@link java.util.List}&lt;{@link StateReference}&gt;</td>
   *        <td>{@link java.util.List}&lt;{@link StateReference}&gt; (with key replaced with id field)</td>
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
   * @return a {@link java.util.List} of {@link StateDraft} built from the supplied {@link
   *     java.util.List} of {@link State}.
   */
  @Nonnull
  public static List<StateDraft> mapToStateDrafts(
      @Nonnull final List<State> states,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return states.stream()
        .filter(Objects::nonNull)
        .map(
            state -> {
              final List<StateResourceIdentifier> newTransitions =
                  replaceTransitionIdsWithKeys(state, referenceIdToKeyCache);
              return StateDraftBuilder.of()
                  .key(state.getKey())
                  .type(state.getType())
                  .name(state.getName())
                  .description(state.getDescription())
                  .initial(state.getInitial())
                  .roles(state.getRoles())
                  .transitions(newTransitions)
                  .build();
            })
        .collect(Collectors.toList());
  }

  @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
  private static List<StateResourceIdentifier> replaceTransitionIdsWithKeys(
      @Nonnull final State state, @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    final List<StateReference> transitions = state.getTransitions();

    if (transitions == null) return null;

    final List<StateResourceIdentifier> newTransitions = new ArrayList<>();
    if (!transitions.isEmpty()) {
      transitions.forEach(
          transition -> {
            newTransitions.add(
                getResourceIdentifierWithKey(
                    transition,
                    referenceIdToKeyCache,
                    (id, key) ->
                        StateResourceIdentifierBuilder.of()
                            .id(id)
                            .key(referenceIdToKeyCache.get(transition.getId()))
                            .build()));
          });
    }
    return newTransitions;
  }

  private StateReferenceResolutionUtils() {}
}
