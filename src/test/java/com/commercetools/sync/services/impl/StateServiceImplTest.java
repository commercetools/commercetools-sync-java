package com.commercetools.sync.services.impl;

import com.commercetools.sync.states.StateSyncOptions;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.StateCreateCommand;
import io.sphere.sdk.states.commands.StateUpdateCommand;
import io.sphere.sdk.states.commands.updateactions.ChangeInitial;
import io.sphere.sdk.states.queries.StateQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.services.impl.ServiceImplUtils.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StateServiceImplTest {

    private SphereClient client = mock(SphereClient.class);
    private StateServiceImpl service;
    private List<String> errorMessages;
    private List<Throwable> errorExceptions;

    private String stateId;
    private String stateKey;

    @BeforeEach
    void setup() {
        stateId = randomString();
        stateKey = randomString();

        errorMessages = new ArrayList<>();
        errorExceptions = new ArrayList<>();
        StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(client)
            .errorCallback((errorMessage, errorException) -> {
                errorMessages.add(errorMessage);
                errorExceptions.add(errorException);
            })
            .build();
        service = new StateServiceImpl(stateSyncOptions);
    }

    @AfterEach
    void cleanup() {
        reset(client);
    }

    private interface StatePagedQueryResult extends PagedQueryResult<State> {
    }

    @Test
    void fetchCachedStateId_WithKey_ShouldFetchState() {
        final String key = randomString();
        final String id = randomString();

        State mock = mock(State.class);
        when(mock.getId()).thenReturn(id);
        when(mock.getKey()).thenReturn(key);

        StatePagedQueryResult result = mock(StatePagedQueryResult.class);
        when(result.getResults()).thenReturn(Collections.singletonList(mock));

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));

        Optional<String> fetchedId = service.fetchCachedStateId(key).toCompletableFuture().join();

        assertThat(fetchedId).isNotEmpty();
        assertThat(fetchedId.get()).isEqualTo(id);
    }

    @Test
    void buildStateQuery_WithType_ShouldBuildQuery() {
        final StateQuery stateQuery = StateServiceImpl.buildStateQuery(StateType.LINE_ITEM_STATE);

        assertThat(stateQuery).isNotNull();
        assertThat(stateQuery.toString()).contains(StateType.LINE_ITEM_STATE.toSphereName());
    }

    @Test
    void fetchMatchingStatesByKeys_WithKeySet_ShouldFetchStates() {
        String key1 = randomString();
        String key2 = randomString();

        HashSet<String> stateKeys = new HashSet<>();
        stateKeys.add(key1);
        stateKeys.add(key2);

        State mock1 = mock(State.class);
        when(mock1.getId()).thenReturn(randomString());
        when(mock1.getKey()).thenReturn(key1);

        State mock2 = mock(State.class);
        when(mock2.getId()).thenReturn(randomString());
        when(mock2.getKey()).thenReturn(key2);

        StatePagedQueryResult result = mock(StatePagedQueryResult.class);
        when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));

        Set<State> states = service.fetchMatchingStatesByKeys(stateKeys).toCompletableFuture().join();

        assertAll(
            () -> assertThat(states).as("Should return resources").isNotEmpty(),
            () -> assertThat(states).as("Should return prepared resources").contains(mock1, mock2),
            () -> assertThat(service.keyToIdCache).as("Should cache resources ids")
                .containsKeys(key1, key2)
        );
        ArgumentCaptor<StateQuery> captor = ArgumentCaptor.forClass(StateQuery.class);
        verify(client).execute(captor.capture());
        assertThat(captor.getValue().expansionPaths()).as("There should be no expansions paths").isEmpty();
    }

    @Test
    void shouldFetchStatesByKeysWithExpandedTransitions() {
        String key1 = randomString();
        String key2 = randomString();

        HashSet<String> stateKeys = new HashSet<>();
        stateKeys.add(key1);
        stateKeys.add(key2);

        State mock1 = mock(State.class);
        when(mock1.getId()).thenReturn(randomString());
        when(mock1.getKey()).thenReturn(key1);

        State mock2 = mock(State.class);
        when(mock2.getId()).thenReturn(randomString());
        when(mock2.getKey()).thenReturn(key2);

        StatePagedQueryResult result = mock(StatePagedQueryResult.class);
        when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));

        Set<State> states = service.fetchMatchingStatesByKeysWithTransitions(stateKeys).toCompletableFuture().join();

        assertAll(
            () -> assertThat(states).as("Should return resources").isNotEmpty(),
            () -> assertThat(states).as("Should return prepared resources").contains(mock1, mock2),
            () -> assertThat(service.keyToIdCache).as("Should cache resources ids")
                .containsKeys(key1, key2)
        );

        ArgumentCaptor<StateQuery> captor = ArgumentCaptor.forClass(StateQuery.class);
        verify(client).execute(captor.capture());
        assertAll(
            () -> assertThat(captor.getValue().expansionPaths())
                .as("Expansion paths should not be empty").isNotEmpty(),
            () -> assertThat(captor.getValue().expansionPaths())
                .as("There should be 1 expansion path").hasSize(1),
            () -> assertThat(captor.getValue().expansionPaths().get(0).toSphereExpand())
                .as("Expansion path should be about transitions").contains("transitions[*]")
        );
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
            () -> assertThat(stateOptional).as("Should return resource").isNotEmpty(),
            () -> assertThat(stateOptional).as("Should return prepared resource").containsSame(mock),
            () -> assertThat(service.keyToIdCache.get(stateKey)).as("Resource's id should be cached")
                .isEqualTo(stateId)
        );
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

        assertAll(
            () -> assertThat(stateOptional).as("Resource should be created").isNotEmpty(),
            () -> assertThat(stateOptional).containsSame(mock)
        );
        verify(client).execute(eq(StateCreateCommand.of(draft)));
    }

    @Test
    void createState_WithRequestException_ShouldNotCreateState() {
        State mock = mock(State.class);
        when(mock.getId()).thenReturn(stateId);

        when(client.execute(any())).thenReturn(CompletableFutureUtils.failed(new BadRequestException("bad request")));

        StateDraft draft = mock(StateDraft.class);
        when(draft.getKey()).thenReturn(stateKey);

        Optional<State> stateOptional = service.createState(draft).toCompletableFuture().join();

        assertAll(
            () -> assertThat(stateOptional).as("Result should be empty").isEmpty(),
            () -> assertThat(errorMessages).as("There should be 1 error message").hasSize(1),
            () -> assertThat(errorMessages).hasOnlyOneElementSatisfying(message -> {
                assertThat(message).as("There should be proper error message")
                    .contains("Failed to create draft with key: '" + stateKey + "'.");
                assertThat(message).as("Exception message should contain type of exception")
                    .contains("BadRequestException");
            }),
            () -> assertThat(errorExceptions).as("There should be 1 error message").hasSize(1),
            () -> assertThat(errorExceptions).hasOnlyOneElementSatisfying(exception ->
                assertThat(exception).as("Exception should be instance of 'BadRequestException'")
                    .isExactlyInstanceOf(BadRequestException.class))
        );
    }

    @Test
    void createState_WithDraftHasNoKey_ShouldNotCreateState() {
        StateDraft draft = mock(StateDraft.class);

        Optional<State> stateOptional = service.createState(draft).toCompletableFuture().join();

        assertAll(
            () -> assertThat(stateOptional).as("Result should be empty").isEmpty(),
            () -> assertThat(errorMessages).as("There should be 1 error message").hasSize(1),
            () -> assertThat(errorExceptions).as("There should be 1 exception").hasSize(1),
            () -> assertThat(errorMessages.get(0))
                .as("Error message should contain proper description")
                .isEqualTo("Failed to create draft with key: 'null'. Reason: Draft key is blank!")
        );
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
