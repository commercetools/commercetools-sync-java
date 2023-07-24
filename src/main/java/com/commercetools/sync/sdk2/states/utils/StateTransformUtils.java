package com.commercetools.sync.sdk2.states.utils;

import static com.commercetools.sync.sdk2.states.utils.StateReferenceResolutionUtils.mapToStateDrafts;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.services.impl.BaseTransformServiceImpl;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public final class StateTransformUtils {

  /**
   * Transforms States by resolving the references and map them to StateDrafts.
   *
   * <p>This method resolves(fetch key values for the reference id's) non null and unexpanded
   * references of the State{@link State} by using cache.
   *
   * <p>If the reference ids are already cached, key values are pulled from the cache, otherwise it
   * executes the query to fetch the key value for the reference id's and store the idToKey value
   * pair in the cache for reuse.
   *
   * <p>Then maps the State to StateDraft by performing reference resolution considering idToKey
   * value from the cache.
   *
   * @param client commercetools client.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @param states the states to resolve the references.
   * @return a new list which contains StateDrafts which have all their references resolved.
   */
  @Nonnull
  public static CompletableFuture<List<StateDraft>> toStateDrafts(
      @Nonnull final ProjectApiRoot client,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache,
      @Nonnull final List<State> states) {

    final StateTransformServiceImpl stateTransformService =
        new StateTransformServiceImpl(client, referenceIdToKeyCache);
    return stateTransformService.toStateDrafts(states);
  }

  private static class StateTransformServiceImpl extends BaseTransformServiceImpl {

    StateTransformServiceImpl(
        @Nonnull final ProjectApiRoot ctpClient,
        @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
      super(ctpClient, referenceIdToKeyCache);
    }

    @Nonnull
    public CompletableFuture<List<StateDraft>> toStateDrafts(@Nonnull final List<State> states) {

      return transformTransitionReference(states)
          .thenApply(ignore -> mapToStateDrafts(states, referenceIdToKeyCache));
    }

    @Nonnull
    private CompletableFuture<Void> transformTransitionReference(
        @Nonnull final List<State> states) {

      final Set<String> setOfTransitionStateIds =
          states.stream()
              .map(State::getTransitions)
              .filter(Objects::nonNull)
              .map(
                  transitions ->
                      transitions.stream()
                          .filter(Objects::nonNull)
                          .map(StateReference::getId)
                          .collect(toList()))
              .flatMap(Collection::stream)
              .collect(toSet());

      return fetchAndFillReferenceIdToKeyCache(
          setOfTransitionStateIds, GraphQlQueryResource.STATES);
    }
  }
}
