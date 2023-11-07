package com.commercetools.sync.services.impl;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;

import com.commercetools.api.client.ByProjectKeyStatesGet;
import com.commercetools.api.client.ByProjectKeyStatesKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyStatesPost;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StatePagedQueryResponse;
import com.commercetools.api.models.state.StateUpdateAction;
import com.commercetools.api.models.state.StateUpdateBuilder;
import com.commercetools.api.predicates.query.state.StateQueryBuilderDsl;
import com.commercetools.sync.commons.models.GraphQlQueryResource;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.states.StateSyncOptions;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class StateServiceImpl
    extends BaseServiceWithKey<
        StateSyncOptions,
        State,
        StateDraft,
        ByProjectKeyStatesGet,
        StatePagedQueryResponse,
        ByProjectKeyStatesKeyByKeyGet,
        State,
        StateQueryBuilderDsl,
        ByProjectKeyStatesPost>
    implements StateService {

  private static final String STATE_KEY_NOT_SET =
      "State with id: '%s' has no key set. Keys are required for " + "state matching.";

  public StateServiceImpl(@Nonnull final StateSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> categoryKeys) {
    return super.cacheKeysToIdsUsingGraphQl(categoryKeys, GraphQlQueryResource.STATES);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedStateId(@Nullable final String key) {
    if (key == null) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    final ByProjectKeyStatesGet query =
        syncOptions
            .getCtpClient()
            .states()
            .get()
            .withWhere("key in :keys")
            .withPredicateVar("keys", Collections.singletonList(key));

    return super.fetchCachedResourceId(key, query);
  }

  @Nonnull
  @Override
  public CompletionStage<Set<State>> fetchMatchingStatesByKeys(
      @Nonnull final Set<String> stateKeys) {
    return fetchMatchingStates(stateKeys, false);
  }

  @Nonnull
  @Override
  public CompletionStage<Set<State>> fetchMatchingStatesByKeysWithTransitions(
      @Nonnull final Set<String> stateKeys) {
    return fetchMatchingStates(stateKeys, true);
  }

  private CompletionStage<Set<State>> fetchMatchingStates(
      @Nonnull final Set<String> stateKeys, final boolean withTransitions) {
    return super.fetchMatchingResources(
        stateKeys,
        State::getKey,
        (keysNotCached) -> {
          ByProjectKeyStatesGet query =
              syncOptions
                  .getCtpClient()
                  .states()
                  .get()
                  .withWhere("key in :keys")
                  .withPredicateVar("keys", keysNotCached);
          if (withTransitions) {
            query = query.withExpand("transitions[*]");
          }

          return query;
        });
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<State>> fetchState(@Nullable final String key) {
    return super.fetchResource(key, syncOptions.getCtpClient().states().withKey(key).get());
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<State>> createState(@Nonnull final StateDraft stateDraft) {
    return super.createResource(
        stateDraft,
        StateDraft::getKey,
        State::getId,
        Function.identity(),
        () -> syncOptions.getCtpClient().states().post(stateDraft));
  }

  @Nonnull
  @Override
  public CompletionStage<State> updateState(
      @Nonnull final State state, @Nonnull final List<StateUpdateAction> updateActions) {
    final List<List<StateUpdateAction>> actionBatches =
        batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);

    CompletionStage<ApiHttpResponse<State>> resultStage =
        CompletableFuture.completedFuture(new ApiHttpResponse<>(200, null, state));

    for (final List<StateUpdateAction> batch : actionBatches) {
      resultStage =
          resultStage
              .thenApply(ApiHttpResponse::getBody)
              .thenCompose(
                  updatedState ->
                      syncOptions
                          .getCtpClient()
                          .states()
                          .withId(updatedState.getId())
                          .post(
                              StateUpdateBuilder.of()
                                  .actions(batch)
                                  .version(updatedState.getVersion())
                                  .build())
                          .execute());
    }

    return resultStage.thenApply(ApiHttpResponse::getBody);
  }
}
