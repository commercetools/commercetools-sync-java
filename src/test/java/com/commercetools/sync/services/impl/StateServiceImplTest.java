package com.commercetools.sync.services.impl;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.sync.states.StateSyncOptions;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.StateCreateCommand;
import io.sphere.sdk.states.commands.StateUpdateCommand;
import io.sphere.sdk.states.commands.updateactions.ChangeInitial;
import io.sphere.sdk.states.queries.StateQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StateServiceImplTest {

  private SphereClient client = mock(SphereClient.class);
  private StateServiceImpl service;
  private List<String> errorMessages;
  private List<Throwable> errorExceptions;

  private String stateId;
  private String stateKey;

  @BeforeEach
  void setup() {
    stateId = RandomStringUtils.random(15);
    stateKey = RandomStringUtils.random(15);

    errorMessages = new ArrayList<>();
    errorExceptions = new ArrayList<>();
    StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(client)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  errorExceptions.add(exception.getCause());
                })
            .build();
    service = new StateServiceImpl(stateSyncOptions);
  }

  @AfterEach
  void cleanup() {
    reset(client);
  }

  private interface StatePagedQueryResult extends PagedQueryResult<State> {}

  @Test
  void fetchCachedStateId_WithKey_ShouldFetchState() {
    final String key = RandomStringUtils.random(15);
    final String id = RandomStringUtils.random(15);

    State mock = mock(State.class);
    when(mock.getId()).thenReturn(id);
    when(mock.getKey()).thenReturn(key);

    StatePagedQueryResult result = mock(StatePagedQueryResult.class);
    when(result.getResults()).thenReturn(Collections.singletonList(mock));

    when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));

    Optional<String> fetchedId = service.fetchCachedStateId(key).toCompletableFuture().join();

    assertThat(fetchedId).contains(id);
  }

  @Test
  void buildStateQuery_WithType_ShouldBuildQuery() {
    final QueryPredicate<State> stateQueryPredicate =
        QueryPredicate.of(format("type= \"%s\"", StateType.LINE_ITEM_STATE.toSphereName()));
    final StateQuery stateQuery = StateQuery.of().withPredicates(stateQueryPredicate);

    assertThat(stateQuery).isNotNull();
    assertThat(stateQuery.toString()).contains(StateType.LINE_ITEM_STATE.toSphereName());
  }

  @Test
  void fetchMatchingStatesByKeys_WithKeySet_ShouldFetchStates() {
    String key1 = RandomStringUtils.random(15);
    String key2 = RandomStringUtils.random(15);

    HashSet<String> stateKeys = new HashSet<>();
    stateKeys.add(key1);
    stateKeys.add(key2);

    State mock1 = mock(State.class);
    when(mock1.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock1.getKey()).thenReturn(key1);

    State mock2 = mock(State.class);
    when(mock2.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock2.getKey()).thenReturn(key2);

    StatePagedQueryResult result = mock(StatePagedQueryResult.class);
    when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));

    when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));

    Set<State> states = service.fetchMatchingStatesByKeys(stateKeys).toCompletableFuture().join();

    assertAll(
        () -> assertThat(states).isNotEmpty(),
        () -> assertThat(states).contains(mock1, mock2),
        () -> assertThat(service.keyToIdCache.asMap()).containsKeys(key1, key2));
    ArgumentCaptor<StateQuery> captor = ArgumentCaptor.forClass(StateQuery.class);
    verify(client).execute(captor.capture());
    assertThat(captor.getValue().expansionPaths()).isEmpty();
  }

  @Test
  void shouldFetchStatesByKeysWithExpandedTransitions() {
    String key1 = RandomStringUtils.random(15);
    String key2 = RandomStringUtils.random(15);

    HashSet<String> stateKeys = new HashSet<>();
    stateKeys.add(key1);
    stateKeys.add(key2);

    State mock1 = mock(State.class);
    when(mock1.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock1.getKey()).thenReturn(key1);

    State mock2 = mock(State.class);
    when(mock2.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock2.getKey()).thenReturn(key2);

    StatePagedQueryResult result = mock(StatePagedQueryResult.class);
    when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));

    when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));

    Set<State> states =
        service.fetchMatchingStatesByKeysWithTransitions(stateKeys).toCompletableFuture().join();

    assertAll(
        () -> assertThat(states).isNotEmpty(),
        () -> assertThat(states).contains(mock1, mock2),
        () -> assertThat(service.keyToIdCache.asMap()).containsKeys(key1, key2));

    ArgumentCaptor<StateQuery> captor = ArgumentCaptor.forClass(StateQuery.class);
    verify(client).execute(captor.capture());
    assertAll(
        () -> assertThat(captor.getValue().expansionPaths()).hasSize(1),
        () ->
            assertThat(captor.getValue().expansionPaths().get(0).toSphereExpand())
                .contains("transitions[*]"));
  }

  @Test
  void fetchState_WithKey_ShouldFetchState() {
    State mock = mock(State.class);
    when(mock.getId()).thenReturn(stateId);
    when(mock.getKey()).thenReturn(stateKey);
    StatePagedQueryResult result = mock(StatePagedQueryResult.class);
    when(result.head()).thenReturn(Optional.of(mock));

    when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));

    Optional<State> stateOptional = service.fetchState(stateKey).toCompletableFuture().join();

    assertAll(
        () -> assertThat(stateOptional).containsSame(mock),
        () -> assertThat(service.keyToIdCache.getIfPresent(stateKey)).isEqualTo(stateId));
    verify(client).execute(any(StateQuery.class));
  }

  @Test
  void createState_WithDraft_ShouldCreateState() {
    State mock = mock(State.class);
    when(mock.getId()).thenReturn(stateId);
    when(mock.getKey()).thenReturn(stateKey);

    when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(mock));

    StateDraft draft = StateDraft.of(stateKey, StateType.LINE_ITEM_STATE);
    Optional<State> stateOptional = service.createState(draft).toCompletableFuture().join();

    assertThat(stateOptional).containsSame(mock);
    verify(client).execute(eq(StateCreateCommand.of(draft)));
  }

  @Test
  void createState_WithRequestException_ShouldNotCreateState() {
    State mock = mock(State.class);
    when(mock.getId()).thenReturn(stateId);

    when(client.execute(any()))
        .thenReturn(CompletableFutureUtils.failed(new BadRequestException("bad request")));

    StateDraft draft = mock(StateDraft.class);
    when(draft.getKey()).thenReturn(stateKey);

    Optional<State> stateOptional = service.createState(draft).toCompletableFuture().join();

    assertAll(
        () -> assertThat(stateOptional).isEmpty(),
        () -> assertThat(errorMessages).hasSize(1),
        () ->
            assertThat(errorMessages)
                .singleElement(as(STRING))
                .contains("Failed to create draft with key: '" + stateKey + "'.")
                .contains("BadRequestException"),
        () -> assertThat(errorExceptions).hasSize(1),
        () ->
            assertThat(errorExceptions)
                .singleElement(as(THROWABLE))
                .isExactlyInstanceOf(BadRequestException.class));
  }

  @Test
  void createState_WithDraftHasNoKey_ShouldNotCreateState() {
    StateDraft draft = mock(StateDraft.class);

    Optional<State> stateOptional = service.createState(draft).toCompletableFuture().join();

    assertAll(
        () -> assertThat(stateOptional).isEmpty(),
        () -> assertThat(errorMessages).hasSize(1),
        () -> assertThat(errorExceptions).hasSize(1),
        () ->
            assertThat(errorMessages.get(0))
                .isEqualTo("Failed to create draft with key: 'null'. Reason: Draft key is blank!"));
  }

  @Test
  void updateState_WithNoError_ShouldUpdateState() {
    State mock = mock(State.class);
    when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(mock));
    List<UpdateAction<State>> updateActions = Collections.singletonList(ChangeInitial.of(false));

    State state = service.updateState(mock, updateActions).toCompletableFuture().join();

    assertThat(state).isSameAs(mock);
    verify(client).execute(eq(StateUpdateCommand.of(mock, updateActions)));
  }
}
