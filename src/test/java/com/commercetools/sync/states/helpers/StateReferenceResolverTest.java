package com.commercetools.sync.states.helpers;

import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_ID_VALUE_ON_REFERENCE;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.states.StateSyncOptions;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class StateReferenceResolverTest {

  @Test
  void resolveReferences_WithStateKeys_ShouldResolveReferences() {
    // preparation
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    final int nStates = 10;
    final List<State> states =
        IntStream.range(0, nStates)
            .mapToObj(i -> i + "")
            .map(
                key -> {
                  final State state = mock(State.class);
                  when(state.getKey()).thenReturn(key);
                  when(state.getId()).thenReturn(key);
                  when(state.toReference()).thenReturn(State.referenceOfId(key));
                  return state;
                })
            .collect(Collectors.toList());

    final StateService mockStateService = mock(StateService.class);
    when(mockStateService.fetchMatchingStatesByKeysWithTransitions(any()))
        .thenReturn(CompletableFuture.completedFuture(new HashSet<>(states)));

    final Set<Reference<State>> stateReferences =
        states.stream().map(State::toReference).collect(Collectors.toSet());

    final StateDraft stateDraft =
        StateDraftBuilder.of("state-key", StateType.LINE_ITEM_STATE)
            .transitions(stateReferences)
            .build();

    final StateReferenceResolver stateReferenceResolver =
        new StateReferenceResolver(stateSyncOptions, mockStateService);

    // test
    final StateDraft resolvedDraft =
        stateReferenceResolver.resolveReferences(stateDraft).toCompletableFuture().join();

    // assertion
    assertThat(resolvedDraft.getTransitions()).isNotNull();
    assertThat(resolvedDraft.getTransitions()).hasSize(nStates);
    assertThat(resolvedDraft.getTransitions()).hasSameElementsAs(stateReferences);
  }

  @Test
  void resolveReferences_WithNullStateReferences_ShouldNotResolveReferences() {
    // preparation
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    final StateService mockStateService = mock(StateService.class);
    when(mockStateService.fetchMatchingStatesByKeysWithTransitions(any()))
        .thenReturn(CompletableFuture.completedFuture(new HashSet<>()));

    final StateDraft stateDraft =
        StateDraftBuilder.of("state-key", StateType.LINE_ITEM_STATE).build();

    final StateReferenceResolver stateReferenceResolver =
        new StateReferenceResolver(stateSyncOptions, mockStateService);

    // test and assertion
    assertThat(stateReferenceResolver.resolveReferences(stateDraft).toCompletableFuture())
        .hasNotFailed()
        .isCompletedWithValueMatching(resolvedDraft -> resolvedDraft.getTransitions() == null);
  }

  @Test
  void resolveReferences_WithNullIdOnStateReference_ShouldNotResolveReference() {
    // preparation
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    final StateService mockStateService = mock(StateService.class);
    when(mockStateService.fetchMatchingStatesByKeysWithTransitions(any()))
        .thenReturn(CompletableFuture.completedFuture(new HashSet<>()));

    final StateDraft stateDraft =
        StateDraftBuilder.of("state-key", StateType.LINE_ITEM_STATE)
            .transitions(singleton(State.referenceOfId(null)))
            .build();

    final StateReferenceResolver stateReferenceResolver =
        new StateReferenceResolver(stateSyncOptions, mockStateService);

    // test and assertion
    assertThat(stateReferenceResolver.resolveReferences(stateDraft).toCompletableFuture())
        .hasFailed()
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(
            format(
                "Failed to resolve 'transition' reference on StateDraft with "
                    + "key:'%s'. Reason: %s",
                stateDraft.getKey(), BLANK_ID_VALUE_ON_REFERENCE));
  }

  @Test
  void resolveReferences_WithEmptyIdOnStateReference_ShouldNotResolveReference() {
    // preparation
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    final StateService mockStateService = mock(StateService.class);
    when(mockStateService.fetchMatchingStatesByKeysWithTransitions(any()))
        .thenReturn(CompletableFuture.completedFuture(new HashSet<>()));

    final StateDraft stateDraft =
        StateDraftBuilder.of("state-key", StateType.LINE_ITEM_STATE)
            .transitions(singleton(State.referenceOfId("")))
            .build();

    final StateReferenceResolver stateReferenceResolver =
        new StateReferenceResolver(stateSyncOptions, mockStateService);

    // test and assertion
    assertThat(stateReferenceResolver.resolveReferences(stateDraft).toCompletableFuture())
        .hasFailed()
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(
            format(
                "Failed to resolve 'transition' reference on StateDraft with "
                    + "key:'%s'. Reason: %s",
                stateDraft.getKey(), BLANK_ID_VALUE_ON_REFERENCE));
  }

  @Test
  void resolveReferences_WithExceptionStateFetch_ShouldNotResolveReference() {
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    final StateService mockStateService = mock(StateService.class);

    final State state = mock(State.class);
    when(state.getKey()).thenReturn("state-key");
    when(state.getId()).thenReturn("state-id");
    when(state.toReference()).thenReturn(State.referenceOfId("state-id"));

    when(mockStateService.fetchMatchingStatesByKeysWithTransitions(any()))
        .thenReturn(CompletableFuture.completedFuture(singleton(state)));

    final StateDraft stateDraft =
        StateDraftBuilder.of("state-key", StateType.LINE_ITEM_STATE)
            .transitions(singleton(state.toReference()))
            .build();

    final CompletableFuture<Set<State>> futureThrowingSphereException = new CompletableFuture<>();
    futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
    when(mockStateService.fetchMatchingStatesByKeysWithTransitions(anySet()))
        .thenReturn(futureThrowingSphereException);

    final StateReferenceResolver stateReferenceResolver =
        new StateReferenceResolver(stateSyncOptions, mockStateService);

    // test and assertion
    assertThat(stateReferenceResolver.resolveReferences(stateDraft).toCompletableFuture())
        .hasFailed()
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(SphereException.class)
        .hasMessageContaining("CTP error on fetch");
  }

  @Test
  void resolveReferences_WithNullTransitionOnTransitionsList_ShouldNotFail() {
    // preparation
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    final StateService mockStateService = mock(StateService.class);
    when(mockStateService.fetchMatchingStatesByKeysWithTransitions(any()))
        .thenReturn(CompletableFuture.completedFuture(new HashSet<>()));

    final StateDraft stateDraft =
        StateDraftBuilder.of("state-key", StateType.LINE_ITEM_STATE)
            .transitions(singleton(null))
            .build();

    final StateReferenceResolver stateReferenceResolver =
        new StateReferenceResolver(stateSyncOptions, mockStateService);

    assertThat(stateReferenceResolver.resolveReferences(stateDraft).toCompletableFuture())
        .hasNotFailed()
        .isCompleted();
  }
}
