package com.commercetools.sync.services;

import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateUpdateAction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface StateService {

  /**
   * Filters out the keys which are already cached and fetches only the not-cached state keys from
   * the CTP project defined in an injected {@link com.commercetools.api.client.ProjectApiRoot} and
   * stores a mapping for every state to id in the cached map of keys -&gt; ids and returns this
   * cached map.
   *
   * <p>Note: If all the supplied keys are already cached, the cached map is returned right away
   * with no request to CTP.
   *
   * @param keys the state keys to fetch and cache the ids for.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Map}&gt; in which the
   *     result of it's completion contains a map of all state keys -&gt; ids
   */
  @Nonnull
  CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull Set<String> keys);

  /**
   * Given a {@code key}, this method first checks if a cached map of state keys -&gt; ids is not
   * empty. If not, it returns a completed future that contains an optional that contains what this
   * key maps to in the cache. If the cache is empty, the method populates the cache with the
   * mapping of all state keys to ids in the CTP project, by querying the CTP project for all
   * states.
   *
   * <p>After that, the method returns a {@link java.util.concurrent.CompletionStage}&lt;{@link
   * java.util.Optional}&lt;{@link String}&gt;&gt; in which the result of it's completion could
   * contain an {@link java.util.Optional} with the id inside of it or an empty {@link
   * java.util.Optional} if no {@link State} was found in the CTP project with this key.
   *
   * @param key the key by which a {@link State} id should be fetched from the CTP project.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Optional}&lt;{@link
   *     String}&gt;&gt; in which the result of its completion could contain an {@link
   *     java.util.Optional} with the id inside of it or an empty {@link java.util.Optional} if no
   *     {@link State} was found in the CTP project with this key.
   */
  @Nonnull
  CompletionStage<Optional<String>> fetchCachedStateId(@Nullable final String key);

  /**
   * Given a {@link java.util.Set} of state keys, this method fetches a set of all the states,
   * matching given set of keys in the CTP project, defined in an injected {@link
   * com.commercetools.api.client.ProjectApiRoot}. A mapping of the key to the id of the fetched
   * states is persisted in an in-memory map.
   *
   * @param stateKeys set of state keys to fetch matching states by.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Map}&gt; in which the
   *     result of it's completion contains a {@link java.util.Set} of all matching states.
   */
  @Nonnull
  CompletionStage<Set<State>> fetchMatchingStatesByKeys(@Nonnull final Set<String> stateKeys);

  /**
   * Given a {@link java.util.Set} of state keys, this method fetches a set of all the states with
   * expanded transitions, matching given set of keys in the CTP project, defined in an injected
   * {@link com.commercetools.api.client.ProjectApiRoot}. A mapping of the key to the id of the
   * fetched states is persisted in an in-memory map.
   *
   * @param stateKeys set of state keys to fetch matching states by.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Map}&gt; in which the
   *     result of it's completion contains a {@link java.util.Set} of all matching states with
   *     expanded transitions.
   */
  @Nonnull
  CompletionStage<Set<State>> fetchMatchingStatesByKeysWithTransitions(
      @Nonnull final Set<String> stateKeys);

  /**
   * Given a state key, this method fetches a state that matches given key in the CTP project
   * defined in a potentially injected {@link com.commercetools.api.client.ProjectApiRoot}. If there
   * is no matching state an empty {@link java.util.Optional} will be returned in the returned
   * future. A mapping of the key to the id of the fetched state is persisted in an in -memory map.
   *
   * @param key the key of the state to fetch.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Optional}&gt; in which
   *     the result of it's completion contains an {@link java.util.Optional} that contains the
   *     matching {@link State} if exists, otherwise empty.
   */
  @Nonnull
  CompletionStage<Optional<State>> fetchState(@Nullable final String key);

  /**
   * Given a resource draft of type {@link StateDraft}, this method attempts to create a resource
   * {@link State} based on it in the CTP project defined by the sync options.
   *
   * <p>A completion stage containing an empty option and the error callback will be triggered in
   * those cases:
   *
   * <ul>
   *   <li>the draft has a blank key
   *   <li>the create request fails on CTP
   * </ul>
   *
   * <p>On the other hand, if the resource gets created successfully on CTP, then the created
   * resource's id and key are cached and the method returns a {@link
   * java.util.concurrent.CompletionStage} in which the result of it's completion contains an
   * instance {@link java.util.Optional} of the resource which was created.
   *
   * @param stateDraft the resource draft to create a resource based off of.
   * @return a {@link java.util.concurrent.CompletionStage} containing an optional with the created
   *     resource if successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<State>> createState(@Nonnull final StateDraft stateDraft);

  /**
   * Given a {@link State} and a {@link java.util.List}&lt;{@link
   * com.commercetools.api.models.state.StateUpdateAction}&gt;, this method issues an update request
   * with these update actions on this {@link State} in the CTP project defined in a potentially
   * injected {@link com.commercetools.api.client.ProjectApiRoot}. This method returns {@link
   * java.util.concurrent.CompletionStage}&lt;{@link State}&gt; in which the result of it's
   * completion contains an instance of the {@link State} which was updated in the CTP project.
   *
   * @param state the {@link State} to update.
   * @param updateActions the update actions to update the {@link State} with.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link State}&gt; containing as a
   *     result of it's completion an instance of the {@link State} which was updated in the CTP
   *     project or a {@link java.util.concurrent.CompletionException}.
   */
  @Nonnull
  CompletionStage<State> updateState(
      @Nonnull final State state, @Nonnull final List<StateUpdateAction> updateActions);
}
