package com.commercetools.sync.states.service.impl;

import static com.commercetools.sync.states.utils.StateReferenceResolutionUtils.mapToStateDrafts;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.services.impl.BaseTransformServiceImpl;
import com.commercetools.sync.states.service.StateReferenceTransformService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class StateReferenceTransformServiceImpl extends BaseTransformServiceImpl
    implements StateReferenceTransformService {

  public StateReferenceTransformServiceImpl(
      @Nonnull final SphereClient ctpClient,
      @Nonnull final Map<String, String> referenceIdToKeyCache) {
    super(ctpClient, referenceIdToKeyCache);
  }

  @Nonnull
  @Override
  public CompletableFuture<List<StateDraft>> transformStateReferences(
      @Nonnull final List<State> states) {

    return transformTransitionReference(states)
        .thenApply(ignore -> mapToStateDrafts(states, referenceIdToKeyCache));
  }

  @Nonnull
  private CompletableFuture<Void> transformTransitionReference(@Nonnull final List<State> states) {

    final Set<String> setOfTransitionStateIds =
        states.stream()
            .map(State::getTransitions)
            .filter(Objects::nonNull)
            .map(
                transitions ->
                    transitions.stream()
                        .filter(Objects::nonNull)
                        .map(Reference::getId)
                        .collect(toList()))
            .flatMap(Collection::stream)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(setOfTransitionStateIds, GraphQlQueryResources.STATES);
  }
}
