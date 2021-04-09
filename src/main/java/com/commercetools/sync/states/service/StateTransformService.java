package com.commercetools.sync.states.service;

import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public interface StateTransformService {

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
   * @param states the states to resolve the references.
   * @return a new list which contains StateDrafts which have all their references resolved.
   */
  @Nonnull
  CompletableFuture<List<StateDraft>> toStateDrafts(@Nonnull List<State> states);
}
