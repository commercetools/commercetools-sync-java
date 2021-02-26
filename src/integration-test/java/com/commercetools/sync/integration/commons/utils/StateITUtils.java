package com.commercetools.sync.integration.commons.utils;

import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.states.commands.updateactions.SetTransitions.of;
import static io.sphere.sdk.utils.CompletableFutureUtils.listOfFuturesToFutureOfList;
import static java.lang.String.format;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.StateCreateCommand;
import io.sphere.sdk.states.commands.StateDeleteCommand;
import io.sphere.sdk.states.commands.StateUpdateCommand;
import io.sphere.sdk.states.commands.updateactions.SetTransitions;
import io.sphere.sdk.states.queries.StateQuery;
import io.sphere.sdk.states.queries.StateQueryBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class StateITUtils {

  public static final String STATE_KEY_1 = "key_1";
  public static final LocalizedString STATE_NAME_1 = LocalizedString.ofEnglish("name_1");
  public static final LocalizedString STATE_DESCRIPTION_1 =
      LocalizedString.ofEnglish("description_1");
  private static final String STATE_KEY = "old_state_key";

  private StateITUtils() {}

  /**
   * Deletes all states from CTP projects defined by the {@code CTP_SOURCE_CLIENT} and {@code
   * CTP_TARGET_CLIENT}.
   */
  public static void deleteStatesFromTargetAndSource() {
    deleteStates(CTP_TARGET_CLIENT);
    deleteStates(CTP_SOURCE_CLIENT);
  }

  /**
   * Deletes all states from the CTP project defined by the {@code ctpClient}.
   *
   * @param ctpClient defines the CTP project to delete the states from.
   */
  public static <T extends State> void deleteStates(@Nonnull final SphereClient ctpClient) {
    // delete transitions
    CtpQueryUtils.queryAll(
            ctpClient,
            StateQueryBuilder.of().plusPredicates(QueryPredicate.of("builtIn = false")).build(),
            Function.identity())
        .thenApply(
            fetchedCategories ->
                fetchedCategories.stream().flatMap(List::stream).collect(Collectors.toList()))
        .thenCompose(
            result -> {
              final List<CompletionStage<State>> clearStates = new ArrayList<>();
              result.stream()
                  .forEach(
                      state -> {
                        if (state.getTransitions() != null && !state.getTransitions().isEmpty()) {
                          List<? extends UpdateAction<State>> emptyUpdateActions =
                              Collections.emptyList();
                          clearStates.add(
                              ctpClient.execute(StateUpdateCommand.of(state, emptyUpdateActions)));
                        } else {
                          clearStates.add(CompletableFuture.completedFuture(state));
                        }
                      });
              return listOfFuturesToFutureOfList(clearStates);
            })
        .thenAccept(
            clearedStates ->
                clearedStates.forEach(
                    stateToRemove -> ctpClient.execute(StateDeleteCommand.of(stateToRemove))))
        .toCompletableFuture()
        .join();
  }

  /**
   * Deletes all states with {@code stateType} from the CTP project defined by the {@code
   * ctpClient}.
   *
   * @param ctpClient defines the CTP project to delete the states from.
   */
  public static void deleteStates(
      @Nonnull final SphereClient ctpClient, @Nonnull final StateType stateType) {
    final QueryPredicate<State> stateQueryPredicate =
        QueryPredicate.of(format("type= \"%s\"", stateType.toSphereName()));
    final StateQuery stateQuery = StateQuery.of().withPredicates(stateQueryPredicate);
    queryAndExecute(ctpClient, stateQuery, StateDeleteCommand::of);
  }

  /**
   * Creates a {@link State} with the {@link StateType} supplied in the CTP project defined by the
   * {@code ctpClient} in a blocking fashion. The created state will have a key with the value
   * {@value STATE_KEY}.
   *
   * @param ctpClient defines the CTP project to create the state in.
   * @param stateType defines the state type to create the state with.
   * @return the created state.
   */
  public static State createState(
      @Nonnull final SphereClient ctpClient, @Nonnull final StateType stateType) {
    return createState(ctpClient, STATE_KEY, stateType, null);
  }

  /**
   * Creates a {@link State} with the {@code stateKey} and the {@link StateType} supplied in the CTP
   * project defined by the {@code ctpClient} in a blocking fashion. Optionally transition state can
   * be provided to create the state with.
   *
   * @param ctpClient defines the CTP project to create the state in.
   * @param stateKey defines the key to create the state with.
   * @param stateType defines the state type to create the state with.
   * @param transitionState defines the transition state to create the state with.
   * @return the created state.
   */
  public static State createState(
      @Nonnull final SphereClient ctpClient,
      @Nonnull final String stateKey,
      @Nonnull final StateType stateType,
      @Nullable final State transitionState) {
    final StateDraft stateDraft =
        StateDraftBuilder.of(stateKey, stateType)
            .transitions(
                Optional.ofNullable(transitionState)
                    .map(
                        state -> {
                          final Set<Reference<State>> stateTransitions = new HashSet<>();
                          stateTransitions.add(State.referenceOfId(state.getId()));
                          return stateTransitions;
                        })
                    .orElse(null))
            .build();
    return executeBlocking(ctpClient.execute(StateCreateCommand.of(stateDraft)));
  }

  /**
   * Deletes all transitions defined in the given {@code state} from the CTP project defined by the
   * {@code ctpClient}.
   *
   * @param ctpClient defines the CTP project to delete the states from.
   * @param state defines the state to remove transitions from.
   */
  public static void clearTransitions(
      @Nonnull final SphereClient ctpClient, @Nonnull final State state) {
    final Set<Reference<State>> stateTransitions =
        Optional.ofNullable(state.getTransitions()).orElse(new HashSet<>());
    stateTransitions.clear();

    SetTransitions setTransitions = of(stateTransitions);
    executeBlocking(ctpClient.execute(StateUpdateCommand.of(state, setTransitions)));
  }

  public static Optional<State> getStateByKey(
      @Nonnull final SphereClient sphereClient, @Nonnull final String key) {

    final StateQuery query =
        StateQueryBuilder.of().plusPredicates(queryModel -> queryModel.key().is(key)).build();

    return sphereClient.execute(query).toCompletableFuture().join().head();
  }
}
