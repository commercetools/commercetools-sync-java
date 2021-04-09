package com.commercetools.sync.states.utils;

import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.states.service.StateTransformService;
import com.commercetools.sync.states.service.impl.StateTransformServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import java.util.List;
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
   *     <p>TODO: Move the implementation from service class to this util class.
   */
  @Nonnull
  public static CompletableFuture<List<StateDraft>> toStateDrafts(
      @Nonnull final SphereClient client,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache,
      @Nonnull final List<State> states) {

    final StateTransformService stateTransformService =
        new StateTransformServiceImpl(client, referenceIdToKeyCache);
    return stateTransformService.toStateDrafts(states);
  }
}
