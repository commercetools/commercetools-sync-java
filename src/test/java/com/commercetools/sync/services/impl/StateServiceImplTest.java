package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.FakeClient;
import com.commercetools.sync.internals.helpers.CustomHeaderSphereClientDecorator;
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
import io.sphere.sdk.states.commands.updateactions.ChangeInitial;
import io.sphere.sdk.states.queries.StateQuery;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
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
        stateId = RandomStringUtils.random(15);
        stateKey = RandomStringUtils.random(15);

        errorMessages = new ArrayList<>();
        errorExceptions = new ArrayList<>();
        StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(client)
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
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

    private interface StatePagedQueryResult extends PagedQueryResult<State> {
    }

    @Test
    void fetchCachedStateId_WithKey_ShouldFetchState() {
        final String key = RandomStringUtils.random(15);
        final String id = RandomStringUtils.random(15);

        State mock = mock(State.class);
        when(mock.getId()).thenReturn(id);
        when(mock.getKey()).thenReturn(key);

        StatePagedQueryResult result = mock(StatePagedQueryResult.class);
        when(result.getResults()).thenReturn(Collections.singletonList(mock));

        final FakeClient<StatePagedQueryResult> fakeStateClient = new FakeClient(result);
        initMockService(fakeStateClient);

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

        final FakeClient<StatePagedQueryResult> fakeStateClient = new FakeClient<>(result);
        initMockService(fakeStateClient);

        Set<State> states = service.fetchMatchingStatesByKeys(stateKeys).toCompletableFuture().join();

        assertAll(
            () -> assertThat(states).isNotEmpty(),
            () -> assertThat(states).contains(mock1, mock2),
            () -> assertThat(service.keyToIdCache).containsKeys(key1, key2)
        );

        assertThat(fakeStateClient.isExecuted()).isTrue();
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

        final FakeClient<StatePagedQueryResult> fakeStateClient = new FakeClient<>(result);
        initMockService(fakeStateClient);

        Set<State> states = service.fetchMatchingStatesByKeysWithTransitions(stateKeys).toCompletableFuture().join();

        assertAll(
            () -> assertThat(states).isNotEmpty(),
            () -> assertThat(states).contains(mock1, mock2),
            () -> assertThat(service.keyToIdCache).containsKeys(key1, key2)
        );

        assertThat(fakeStateClient.isExecuted()).isTrue();
    }

    @Test
    void fetchState_WithKey_ShouldFetchState() {
        State mock = mock(State.class);
        when(mock.getId()).thenReturn(stateId);
        when(mock.getKey()).thenReturn(stateKey);
        StatePagedQueryResult result = mock(StatePagedQueryResult.class);
        when(result.head()).thenReturn(Optional.of(mock));

        final FakeClient<StatePagedQueryResult> fakeStateClient = new FakeClient<>(result);
        initMockService(fakeStateClient);

        Optional<State> stateOptional = service.fetchState(stateKey).toCompletableFuture().join();

        assertAll(
            () -> assertThat(stateOptional).containsSame(mock),
            () -> assertThat(service.keyToIdCache.get(stateKey)).isEqualTo(stateId)
        );
        assertThat(fakeStateClient.isExecuted()).isTrue();
    }

    @Test
    void createState_WithDraft_ShouldCreateState() {
        State mock = mock(State.class);
        when(mock.getId()).thenReturn(stateId);
        when(mock.getKey()).thenReturn(stateKey);

        final FakeClient<State> fakeStateClient = new FakeClient<>(mock);
        initMockService(fakeStateClient);

        StateDraft draft = StateDraft.of(stateKey, StateType.LINE_ITEM_STATE);
        Optional<State> stateOptional = service.createState(draft).toCompletableFuture().join();

        assertThat(stateOptional).containsSame(mock);
        assertThat(fakeStateClient.isExecuted()).isTrue();
    }

    @Test
    void createState_WithRequestException_ShouldNotCreateState() {
        State mock = mock(State.class);
        when(mock.getId()).thenReturn(stateId);

        final FakeClient<State> fakeStateClient = new FakeClient<>(new BadRequestException("bad request"));
        initMockService(fakeStateClient);

        StateDraft draft = mock(StateDraft.class);
        when(draft.getKey()).thenReturn(stateKey);

        Optional<State> stateOptional = service.createState(draft).toCompletableFuture().join();

        assertAll(
            () -> assertThat(stateOptional).isEmpty(),
            () -> assertThat(errorMessages).hasSize(1),
            () -> assertThat(errorMessages).singleElement().satisfies(message -> {
                assertThat(message).contains("Failed to create draft with key: '" + stateKey + "'.");
                assertThat(message).contains("BadRequestException");
            }),
            () -> assertThat(errorExceptions).hasSize(1),
            () -> assertThat(errorExceptions).singleElement().satisfies(exception ->
                assertThat(exception).isExactlyInstanceOf(BadRequestException.class))
        );
    }

    @Test
    void createState_WithDraftHasNoKey_ShouldNotCreateState() {
        StateDraft draft = mock(StateDraft.class);

        Optional<State> stateOptional = service.createState(draft).toCompletableFuture().join();

        assertAll(
            () -> assertThat(stateOptional).isEmpty(),
            () -> assertThat(errorMessages).hasSize(1),
            () -> assertThat(errorExceptions).hasSize(1),
            () -> assertThat(errorMessages.get(0))
                .isEqualTo("Failed to create draft with key: 'null'. Reason: Draft key is blank!")
        );
    }

    @Test
    void updateState_WithNoError_ShouldUpdateState() {
        State mock = mock(State.class);
        final FakeClient<State> fakeStateClient = new FakeClient<>(mock);
        initMockService(fakeStateClient);

        List<UpdateAction<State>> updateActions = Collections.singletonList(ChangeInitial.of(false));

        State state = service.updateState(mock, updateActions).toCompletableFuture().join();

        assertThat(state).isSameAs(mock);
        assertThat(fakeStateClient.isExecuted()).isTrue();
    }

    private void initMockService(@Nonnull final FakeClient fakeStateClient) {
        errorMessages = new ArrayList<>();
        errorExceptions = new ArrayList<>();
        StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(fakeStateClient)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorMessages.add(exception.getMessage());
                    errorExceptions.add(exception.getCause());
                })
                .build();
        service = new StateServiceImpl(stateSyncOptions);

    }

}
