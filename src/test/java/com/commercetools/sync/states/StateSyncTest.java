package com.commercetools.sync.states;

import com.commercetools.sync.services.StateService;
import com.commercetools.sync.states.helpers.StateSyncStatistics;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class StateSyncTest {

    private StateService stateService = mock(StateService.class);

    @AfterEach
    void cleanup() {
        reset(stateService);
    }

    @Test
    void sync_WithInvalidDrafts_ShouldApplyErrorCallbackAndIncrementFailed() {
        List<String> errors = new ArrayList<>();

        StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        StateSync sync = new StateSync(options, stateService);

        StateDraft withoutKeyDraft = StateDraftBuilder.of(null, StateType.LINE_ITEM_STATE).build();

        StateSyncStatistics result = sync.sync(asList(null, withoutKeyDraft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).as("All prepared drafts should be processed")
                .isEqualTo(2),
            () -> assertThat(result.getFailed().get()).as("All prepared drafts should be faulty")
                .isEqualTo(2),
            () -> assertThat(errors).as("Error callback should be called for each draft").hasSize(2),
            () -> assertThat(errors).as("Error messages should contain proper reason")
                .contains("Failed to process null state draft.", "Failed to process state draft without key.")
        );
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithErrorFetchingExistingKeys_ShouldApplyErrorCallbackAndIncrementFailed() {
        List<String> errors = new ArrayList<>();

        StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        StateSync sync = new StateSync(options, stateService);

        StateDraft draft = StateDraftBuilder.of("someKey", StateType.LINE_ITEM_STATE).build();

        when(stateService.fetchMatchingStatesByKeys(any())).thenReturn(supplyAsync(() -> {
            throw new SphereException();
        }));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).as("All prepared drafts should be processed")
                .isEqualTo(1),
            () -> assertThat(errors).as("Error callback should be called").hasSize(1),
            () -> assertThat(errors).as("Error messages should contain proper reason")
                .contains("Failed to fetch existing states with keys: '[someKey]'.")
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithErrorCreating_ShouldIncrementFailedButNotApplyErrorCallback() {
        List<String> errors = new ArrayList<>();

        StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        StateSync sync = new StateSync(options, stateService);

        StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE).build();

        when(stateService.fetchMatchingStatesByKeys(any())).thenReturn(completedFuture(emptySet()));
        when(stateService.createState(any())).thenReturn(completedFuture(empty()));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).as("All prepared drafts should be processed")
                .isEqualTo(1),
            () -> assertThat(result.getFailed().get()).as("Creation should fail")
                .isEqualTo(1),
            () -> assertThat(errors).as("Error callback should not be called").isEmpty()
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verify(stateService, times(1)).createState(any());
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithErrorFetchingTransitionsKeys_ShouldApplyBeforeCreateCallbackAndIncrementBothCreatedAndFailed() {
        AtomicBoolean callbackApplied = new AtomicBoolean(false);

        StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .beforeCreateCallback((draft) -> {
                callbackApplied.set(true);
                return draft;
            })
            .build();
        StateSync sync = new StateSync(options, stateService);

        StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE).build();
        State state = mock(State.class);

        when(stateService.fetchMatchingStatesByKeys(any())).thenReturn(completedFuture(emptySet()));
        when(stateService.createState(any())).thenReturn(completedFuture(Optional.of(state)));
        when(stateService.fetchMatchingStatesByKeysWithTransitions(any())).thenReturn(completedFuture(emptySet()));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).as("All prepared drafts should be processed")
                .isEqualTo(1),
            () -> assertThat(result.getCreated().get()).as("Creation should succeed")
                .isEqualTo(1),
            () -> assertThat(result.getFailed().get()).as("Transition sync should fail")
                .isEqualTo(1),
            () -> assertThat(callbackApplied.get()).as("Before create callback should be called").isTrue()
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verify(stateService, times(1)).createState(any());
        verify(stateService, times(1)).fetchMatchingStatesByKeysWithTransitions(any());
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithNoError_ShouldIncrementCreatedAndUpdated() {
        StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        StateSync sync = new StateSync(options, stateService);

        State ref = mock(State.class);
        when(ref.getId()).thenReturn("id");
        when(ref.getKey()).thenReturn("key");

        Set<Reference<State>> newTransitions = new HashSet<>(singletonList(State.referenceOfId("id").filled(ref)));

        StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE)
            .transitions(newTransitions)
            .build();

        State state = mock(State.class);

        when(stateService.fetchMatchingStatesByKeys(any())).thenReturn(completedFuture(emptySet()));
        when(stateService.createState(any())).thenReturn(completedFuture(Optional.of(state)));
        when(stateService.fetchMatchingStatesByKeysWithTransitions(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(ref))));
        when(stateService.updateState(any(), any())).thenReturn(completedFuture(ref));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).as("All prepared drafts should be processed")
                .isEqualTo(1),
            () -> assertThat(result.getCreated().get()).as("Creation should succeed")
                .isEqualTo(1),
            () -> assertThat(result.getFailed().get()).as("There should be no failure recorded")
                .isEqualTo(0),
            () -> assertThat(result.getUpdated().get()).as("Transitions should be updated")
                .isEqualTo(1)
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verify(stateService, times(1)).createState(any());
        verify(stateService, times(1)).fetchMatchingStatesByKeysWithTransitions(any());
        verify(stateService, times(1)).updateState(any(), any());
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithErrorFetchingTransitionsKeys_ShouldApplyErrorCallbackAndIncrementBothCreatedAndFailed() {
        List<String> errors = new ArrayList<>();

        StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        StateSync sync = new StateSync(options, stateService);

        State ref = mock(State.class);
        when(ref.getId()).thenReturn("id");
        when(ref.getKey()).thenReturn("key");

        Set<Reference<State>> newTransitions = new HashSet<>(singletonList(State.referenceOfId("id1")));
        Set<Reference<State>> oldTransitions = new HashSet<>(singletonList(State.referenceOfId("id2")));

        StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE)
            .transitions(newTransitions)
            .build();

        State state = mock(State.class);
        when(state.getTransitions()).thenReturn(oldTransitions);

        when(stateService.fetchMatchingStatesByKeys(any())).thenReturn(completedFuture(emptySet()));
        when(stateService.createState(any())).thenReturn(completedFuture(Optional.of(state)));
        when(stateService.fetchMatchingStatesByKeysWithTransitions(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(ref))));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).as("All prepared drafts should be processed")
                .isEqualTo(1),
            () -> assertThat(result.getCreated().get()).as("Creation should succeed")
                .isEqualTo(1),
            () -> assertThat(result.getFailed().get()).as("There should be failure recorded")
                .isEqualTo(1),
            () -> assertThat(result.getUpdated().get()).as("Transitions should not be updated")
                .isEqualTo(0),
            () -> assertThat(errors).as("Error callback should be called").hasSize(1)
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verify(stateService, times(1)).createState(any());
        verify(stateService, times(1)).fetchMatchingStatesByKeysWithTransitions(any());
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithErrorUpdating_ShouldApplyErrorCallbackAndIncrementFailed() {
        List<String> errors = new ArrayList<>();

        StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        StateSync sync = new StateSync(options, stateService);

        StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE).initial(true).build();

        State state = mock(State.class);
        when(state.getKey()).thenReturn("key");

        when(stateService.fetchMatchingStatesByKeys(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(state))));
        when(stateService.updateState(any(), any())).thenReturn(supplyAsync(() -> {
            throw new SphereException();
        }));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).as("All prepared drafts should be processed")
                .isEqualTo(1),
            () -> assertThat(result.getUpdated().get()).as("Update should fail")
                .isEqualTo(0),
            () -> assertThat(result.getFailed().get()).as("There should be failure recorded")
                .isEqualTo(1),
            () -> assertThat(errors).as("Error callback should be called").hasSize(1)
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verify(stateService, times(1)).updateState(any(), any());
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithErrorUpdatingAndTryingToRecoverWithFetchException_ShouldApplyErrorCallbackAndIncrementFailed() {
        List<String> errors = new ArrayList<>();

        StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        StateSync sync = new StateSync(options, stateService);

        StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE).initial(true).build();

        State state = mock(State.class);
        when(state.getKey()).thenReturn("key");

        when(stateService.fetchMatchingStatesByKeys(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(state))));
        when(stateService.updateState(any(), any())).thenReturn(supplyAsync(() -> {
            throw new ConcurrentModificationException();
        }));
        when(stateService.fetchState(any())).thenReturn(supplyAsync(() -> {
            throw new SphereException();
        }));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).as("All prepared drafts should be processed")
                .isEqualTo(1),
            () -> assertThat(result.getUpdated().get()).as("Update should fail")
                .isEqualTo(0),
            () -> assertThat(result.getFailed().get()).as("There should be failure recorded")
                .isEqualTo(1),
            () -> assertThat(errors).as("Error callback should be called").hasSize(1),
            () -> assertThat(errors).as("Error should contain reason of failure")
                .hasOnlyOneElementSatisfying(msg -> assertThat(msg)
                    .contains("Failed to fetch from CTP while retrying after concurrency modification."))
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verify(stateService, times(1)).updateState(any(), any());
        verify(stateService, times(1)).fetchState(any());
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithErrorUpdatingAndTryingToRecoverWithEmptyResponse_ShouldApplyErrorCallbackAndIncrementFailed() {
        List<String> errors = new ArrayList<>();

        StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        StateSync sync = new StateSync(options, stateService);

        StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE).initial(true).build();

        State state = mock(State.class);
        when(state.getKey()).thenReturn("key");

        when(stateService.fetchMatchingStatesByKeys(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(state))));
        when(stateService.updateState(any(), any())).thenReturn(supplyAsync(() -> {
            throw new ConcurrentModificationException();
        }));
        when(stateService.fetchState(any())).thenReturn(completedFuture(Optional.empty()));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).as("All prepared drafts should be processed")
                .isEqualTo(1),
            () -> assertThat(result.getUpdated().get()).as("Update should fail")
                .isEqualTo(0),
            () -> assertThat(result.getFailed().get()).as("There should be failure recorded")
                .isEqualTo(1),
            () -> assertThat(errors).as("Error callback should be called").hasSize(1),
            () -> assertThat(errors).as("Error should contain reason of failure")
                .hasOnlyOneElementSatisfying(msg -> assertThat(msg)
                    .contains("Not found when attempting to fetch while retrying after "
                        + "concurrency modification."))
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verify(stateService, times(1)).updateState(any(), any());
        verify(stateService, times(1)).fetchState(any());
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithNoError_ShouldApplyBeforeUpdateCallbackAndIncrementUpdated() {
        AtomicBoolean callbackApplied = new AtomicBoolean(false);

        StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .beforeUpdateCallback((actions, draft, old) -> {
                callbackApplied.set(true);
                return actions;
            })
            .build();
        StateSync sync = new StateSync(options, stateService);

        StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE).initial(true).build();

        State state = mock(State.class);
        when(state.getId()).thenReturn("id");
        when(state.getKey()).thenReturn("key");
        when(state.getTransitions()).thenReturn(null);

        when(stateService.fetchMatchingStatesByKeys(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(state))));
        when(stateService.updateState(any(), any())).thenReturn(completedFuture(state));
        when(stateService.fetchMatchingStatesByKeysWithTransitions(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(state))));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).as("All prepared drafts should be processed")
                .isEqualTo(1),
            () -> assertThat(result.getUpdated().get()).as("Update should succeed")
                .isEqualTo(1),
            () -> assertThat(result.getFailed().get()).as("There should be no failures recorded")
                .isEqualTo(0),
            () -> assertThat(callbackApplied.get()).as("Before update callback should be called").isTrue()
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verify(stateService, times(1)).updateState(any(), any());
        verify(stateService, times(1)).fetchMatchingStatesByKeysWithTransitions(any());
        verifyNoMoreInteractions(stateService);
    }

}
