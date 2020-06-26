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

    private final StateService stateService = mock(StateService.class);

    @AfterEach
    void cleanup() {
        reset(stateService);
    }

    @Test
    void sync_WithInvalidDrafts_ShouldApplyErrorCallbackAndIncrementFailed() {
        final List<String> errors = new ArrayList<>();

        final StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        final StateSync sync = new StateSync(options, stateService);

        final StateDraft withoutKeyDraft = StateDraftBuilder.of(null, StateType.LINE_ITEM_STATE).build();

        StateSyncStatistics result = sync.sync(asList(null, withoutKeyDraft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(2),
            () -> assertThat(result.getFailed().get()).isEqualTo(2),
            () -> assertThat(errors).hasSize(2),
            () -> assertThat(errors)
                .contains("Failed to process null state draft.", "Failed to process state draft without key.")
        );
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithErrorFetchingExistingKeys_ShouldApplyErrorCallbackAndIncrementFailed() {
        final List<String> errors = new ArrayList<>();

        final StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        final StateSync sync = new StateSync(options, stateService);

        final StateDraft draft = StateDraftBuilder.of("someKey", StateType.LINE_ITEM_STATE).build();

        when(stateService.fetchMatchingStatesByKeys(any())).thenReturn(supplyAsync(() -> {
            throw new SphereException();
        }));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(errors).hasSize(1),
            () -> assertThat(errors).contains("Failed to fetch existing states with keys: '[someKey]'.")
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithErrorCreating_ShouldIncrementFailedButNotApplyErrorCallback() {
        final List<String> errors = new ArrayList<>();

        final StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        final StateSync sync = new StateSync(options, stateService);

        final StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE).build();

        when(stateService.fetchMatchingStatesByKeys(any())).thenReturn(completedFuture(emptySet()));
        when(stateService.createState(any())).thenReturn(completedFuture(empty()));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(result.getFailed().get()).isEqualTo(1),
            () -> assertThat(errors).isEmpty()
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verify(stateService, times(1)).createState(any());
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithErrorFetchingTransitionsKeys_ShouldApplyBeforeCreateCallbackAndIncrementBothCreatedAndFailed() {
        final AtomicBoolean callbackApplied = new AtomicBoolean(false);

        final StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .beforeCreateCallback((draft) -> {
                callbackApplied.set(true);
                return draft;
            })
            .build();
        final StateSync sync = new StateSync(options, stateService);

        final StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE).build();
        final State state = mock(State.class);

        when(stateService.fetchMatchingStatesByKeys(any())).thenReturn(completedFuture(emptySet()));
        when(stateService.createState(any())).thenReturn(completedFuture(Optional.of(state)));
        when(stateService.fetchMatchingStatesByKeysWithTransitions(any())).thenReturn(completedFuture(emptySet()));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(result.getCreated().get()).isEqualTo(1),
            () -> assertThat(result.getFailed().get()).isEqualTo(1),
            () -> assertThat(callbackApplied.get()).isTrue()
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verify(stateService, times(1)).createState(any());
        verify(stateService, times(1)).fetchMatchingStatesByKeysWithTransitions(any());
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithNoError_ShouldIncrementCreatedAndUpdated() {
        final StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        final StateSync sync = new StateSync(options, stateService);

        final State ref = mock(State.class);
        when(ref.getId()).thenReturn("id");
        when(ref.getKey()).thenReturn("key");

        final Set<Reference<State>> newTransitions = new HashSet<>(singletonList(
            State.referenceOfId("id").filled(ref)));

        final StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE)
            .transitions(newTransitions)
            .build();

        final State state = mock(State.class);

        when(stateService.fetchMatchingStatesByKeys(any())).thenReturn(completedFuture(emptySet()));
        when(stateService.createState(any())).thenReturn(completedFuture(Optional.of(state)));
        when(stateService.fetchMatchingStatesByKeysWithTransitions(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(ref))));
        when(stateService.updateState(any(), any())).thenReturn(completedFuture(ref));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(result.getCreated().get()).isEqualTo(1),
            () -> assertThat(result.getFailed().get()).isEqualTo(0),
            () -> assertThat(result.getUpdated().get()).isEqualTo(1)
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verify(stateService, times(1)).createState(any());
        verify(stateService, times(1)).fetchMatchingStatesByKeysWithTransitions(any());
        verify(stateService, times(1)).updateState(any(), any());
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithErrorFetchingTransitionsKeys_ShouldApplyErrorCallbackAndIncrementBothCreatedAndFailed() {
        final List<String> errors = new ArrayList<>();

        final StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        final StateSync sync = new StateSync(options, stateService);

        final State ref = mock(State.class);
        when(ref.getId()).thenReturn("id");
        when(ref.getKey()).thenReturn("key");

        final Set<Reference<State>> newTransitions = new HashSet<>(singletonList(State.referenceOfId("id1")));
        final Set<Reference<State>> oldTransitions = new HashSet<>(singletonList(State.referenceOfId("id2")));

        final StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE)
            .transitions(newTransitions)
            .build();

        final State state = mock(State.class);
        when(state.getTransitions()).thenReturn(oldTransitions);

        when(stateService.fetchMatchingStatesByKeys(any())).thenReturn(completedFuture(emptySet()));
        when(stateService.createState(any())).thenReturn(completedFuture(Optional.of(state)));
        when(stateService.fetchMatchingStatesByKeysWithTransitions(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(ref))));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(result.getCreated().get()).isEqualTo(1),
            () -> assertThat(result.getFailed().get()).isEqualTo(1),
            () -> assertThat(result.getUpdated().get()).isEqualTo(0),
            () -> assertThat(errors).hasSize(1)
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verify(stateService, times(1)).createState(any());
        verify(stateService, times(1)).fetchMatchingStatesByKeysWithTransitions(any());
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithErrorUpdating_ShouldApplyErrorCallbackAndIncrementFailed() {
        final List<String> errors = new ArrayList<>();

        final StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        final StateSync sync = new StateSync(options, stateService);

        final StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE).initial(true).build();

        final State state = mock(State.class);
        when(state.getKey()).thenReturn("key");

        when(stateService.fetchMatchingStatesByKeys(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(state))));
        when(stateService.updateState(any(), any())).thenReturn(supplyAsync(() -> {
            throw new SphereException();
        }));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(result.getUpdated().get()).isEqualTo(0),
            () -> assertThat(result.getFailed().get()).isEqualTo(1),
            () -> assertThat(errors).hasSize(1)
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verify(stateService, times(1)).updateState(any(), any());
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithErrorUpdatingAndTryingToRecoverWithFetchException_ShouldApplyErrorCallbackAndIncrementFailed() {
        final List<String> errors = new ArrayList<>();

        final StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        final StateSync sync = new StateSync(options, stateService);

        final StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE).initial(true).build();

        final State state = mock(State.class);
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
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(result.getUpdated().get()).isEqualTo(0),
            () -> assertThat(result.getFailed().get()).isEqualTo(1),
            () -> assertThat(errors).hasSize(1),
            () -> assertThat(errors).hasOnlyOneElementSatisfying(msg -> assertThat(msg)
                .contains("Failed to fetch from CTP while retrying after concurrency modification."))
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verify(stateService, times(1)).updateState(any(), any());
        verify(stateService, times(1)).fetchState(any());
        verifyNoMoreInteractions(stateService);
    }

    @Test
    void sync_WithErrorUpdatingAndTryingToRecoverWithEmptyResponse_ShouldApplyErrorCallbackAndIncrementFailed() {
        final List<String> errors = new ArrayList<>();

        final StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        final StateSync sync = new StateSync(options, stateService);

        final StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE).initial(true).build();

        final State state = mock(State.class);
        when(state.getKey()).thenReturn("key");

        when(stateService.fetchMatchingStatesByKeys(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(state))));
        when(stateService.updateState(any(), any())).thenReturn(supplyAsync(() -> {
            throw new ConcurrentModificationException();
        }));
        when(stateService.fetchState(any())).thenReturn(completedFuture(Optional.empty()));

        StateSyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(result.getUpdated().get()).isEqualTo(0),
            () -> assertThat(result.getFailed().get()).isEqualTo(1),
            () -> assertThat(errors).hasSize(1),
            () -> assertThat(errors).hasOnlyOneElementSatisfying(msg -> assertThat(msg)
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
        final AtomicBoolean callbackApplied = new AtomicBoolean(false);

        final StateSyncOptions options = StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .beforeUpdateCallback((actions, draft, old) -> {
                callbackApplied.set(true);
                return actions;
            })
            .build();
        final StateSync sync = new StateSync(options, stateService);

        final StateDraft draft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE).initial(true).build();

        final State state = mock(State.class);
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
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(result.getUpdated().get()).isEqualTo(1),
            () -> assertThat(result.getFailed().get()).isEqualTo(0),
            () -> assertThat(callbackApplied.get()).isTrue()
        );
        verify(stateService, times(1)).fetchMatchingStatesByKeys(any());
        verify(stateService, times(1)).updateState(any(), any());
        verify(stateService, times(1)).fetchMatchingStatesByKeysWithTransitions(any());
        verifyNoMoreInteractions(stateService);
    }

}
