package com.commercetools.sync.integration.sdk2.commons.utils;

import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;

import com.commercetools.api.client.ByProjectKeyStatesGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StatePagedQueryResponse;
import com.commercetools.api.models.state.StateTypeEnum;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class StateITUtils {

  public static final String STATE_KEY_1 = "key_1";
  public static final LocalizedString STATE_NAME_1 = LocalizedString.ofEnglish("name_1");
  public static final LocalizedString STATE_DESCRIPTION_1 =
      LocalizedString.ofEnglish("description_1");
  private static final String STATE_KEY = "old_state_key";

  private StateITUtils() {}

  /** Deletes all states from CTP projects defined by the {@code CTP_TARGET_CLIENT}. */
  public static void deleteStatesFromTarget() {
    deleteStates(CTP_TARGET_CLIENT, null);
  }

  /**
   * Deletes all states from the CTP project defined by the {@code ctpClient}.
   *
   * @param ctpClient defines the CTP project to delete the states from.
   * @param stateType optional type of states with should be deleted
   */
  public static <T extends State> void deleteStates(
      @Nonnull final ProjectApiRoot ctpClient, @Nullable final StateTypeEnum stateType) {
    ByProjectKeyStatesGet stateQuery = ctpClient.states().get().withWhere("builtIn = false");

    if (stateType != null) {
      stateQuery =
          stateQuery
              .addWhere("type=:stateType")
              .withPredicateVar("stateType", stateType.getJsonName());
    }

    QueryUtils.queryAll(
            stateQuery,
            states -> {
              final List<State> statesToRemove = new LinkedList<>();
              for (State state : states) {
                if (state.getTransitions() != null && !state.getTransitions().isEmpty()) {
                  state = clearTransitions(ctpClient, state);
                }
                statesToRemove.add(state);
              }

              CompletableFuture.allOf(
                      statesToRemove.stream()
                          .map(state -> deleteState(ctpClient, state))
                          .map(CompletionStage::toCompletableFuture)
                          .toArray(CompletableFuture[]::new))
                  .join();
            })
        .toCompletableFuture()
        .join();
  }

  private static CompletionStage<State> deleteState(ProjectApiRoot ctpClient, State state) {
    return ctpClient.states().delete(state).execute().thenApply(ApiHttpResponse::getBody);
  }

  /**
   * Creates a {@link State} with the {@link StateTypeEnum} supplied in the CTP project defined by
   * the {@code ctpClient} in a blocking fashion. The created state will have a key with the value
   * {@value STATE_KEY}.
   *
   * @param ctpClient defines the CTP project to create the state in.
   * @param stateType defines the state type to create the state with.
   * @return the created state.
   */
  public static State ensureState(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final StateTypeEnum stateType) {
    State state =
        ctpClient
            .states()
            .get()
            .withWhere("key=:key")
            .withPredicateVar("key", STATE_KEY)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(StatePagedQueryResponse::getResults)
            .thenApply(states -> states.isEmpty() ? null : states.get(0))
            .join();

    if (state == null) {
      state = createState(ctpClient, STATE_KEY, stateType, null);
    }

    return state;
  }

  /**
   * Creates a {@link State} with the {@code stateKey} and the {@link StateTypeEnum} supplied in the
   * CTP project defined by the {@code ctpClient} in a blocking fashion. Optionally transition state
   * can be provided to create the state with.
   *
   * @param ctpClient defines the CTP project to create the state in.
   * @param stateKey defines the key to create the state with.
   * @param stateType defines the state type to create the state with.
   * @param transitionState defines the transition state to create the state with.
   * @return the created state.
   */
  public static State createState(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nonnull final String stateKey,
      @Nonnull final StateTypeEnum stateType,
      @Nullable final State transitionState) {
    StateDraftBuilder stateDraftBuilder = StateDraftBuilder.of().key(stateKey).type(stateType);
    if (transitionState != null) {
      stateDraftBuilder = stateDraftBuilder.transitions(transitionState.toResourceIdentifier());
    }

    final StateDraft stateDraft = stateDraftBuilder.build();

    return ctpClient.states().create(stateDraft).executeBlocking().getBody();
  }

  /**
   * Deletes all transitions defined in the given {@code state} from the CTP project defined by the
   * {@code ctpClient}.
   *
   * @param ctpClient defines the CTP project to delete the states from.
   * @param state defines the state to remove transitions from.
   */
  public static State clearTransitions(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final State state) {
    return ctpClient
        .states()
        .update(state)
        .with(
            stateUpdateActionStateUpdateActionBuilderUpdateActionBuilder ->
                stateUpdateActionStateUpdateActionBuilderUpdateActionBuilder.plus(
                    stateUpdateActionBuilder ->
                        stateUpdateActionBuilder
                            .setTransitionsBuilder()
                            .transitions(Collections.emptyList())))
        .executeBlocking()
        .getBody();
  }

  public static Optional<State> getStateByKey(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final String key) {
    return Optional.ofNullable(ctpClient.states().withKey(key).get().executeBlocking().getBody());
  }
}
