package com.commercetools.sync.states.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StateMixin;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.state.StateResourceIdentifierBuilder;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.sync.commons.ExceptionUtils;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.states.StateSyncOptions;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import io.vrap.rmf.base.client.error.BadGatewayException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class StateReferenceResolverTest {

  @Test
  void resolveReferences_WithStateKeys_ShouldResolveReferences() {
    // preparation
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    final int nStates = 10;
    final List<State> states =
        IntStream.range(0, nStates)
            .mapToObj(i -> i + "")
            .map(
                key -> {
                  final State state = mock(State.class);
                  when(state.getKey()).thenReturn(key);
                  when(state.getId()).thenReturn(key);
                  when(state.toResourceIdentifier())
                      .thenReturn(StateResourceIdentifierBuilder.of().key(key).build());
                  return state;
                })
            .collect(Collectors.toList());

    final StateService mockStateService = mock(StateService.class);
    when(mockStateService.fetchMatchingStatesByKeysWithTransitions(any()))
        .thenReturn(CompletableFuture.completedFuture(new HashSet<>(states)));

    final List<StateResourceIdentifier> stateReferences =
        states.stream().map(StateMixin::toResourceIdentifier).collect(Collectors.toList());

    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key("state-key")
            .type(StateTypeEnum.LINE_ITEM_STATE)
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
        StateSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    final StateService mockStateService = mock(StateService.class);
    when(mockStateService.fetchMatchingStatesByKeysWithTransitions(any()))
        .thenReturn(CompletableFuture.completedFuture(new HashSet<>()));

    final StateDraft stateDraft =
        StateDraftBuilder.of().key("state-key").type(StateTypeEnum.LINE_ITEM_STATE).build();

    final StateReferenceResolver stateReferenceResolver =
        new StateReferenceResolver(stateSyncOptions, mockStateService);

    // test and assertion
    assertThat(stateReferenceResolver.resolveReferences(stateDraft).toCompletableFuture())
        .isCompletedWithValueMatching(resolvedDraft -> resolvedDraft.getTransitions() == null);
  }

  @Test
  void resolveReferences_WithNullKeyOnStateResourceIdentifier_ShouldNotResolveReference() {
    // preparation
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final StateService mockStateService = mock(StateService.class);
    when(mockStateService.fetchMatchingStatesByKeysWithTransitions(any()))
        .thenReturn(CompletableFuture.completedFuture(new HashSet<>()));

    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key("state-key")
            .type(StateTypeEnum.LINE_ITEM_STATE)
            .transitions(List.of(StateResourceIdentifierBuilder.of().build()))
            .build();

    final StateReferenceResolver stateReferenceResolver =
        new StateReferenceResolver(stateSyncOptions, mockStateService);

    // test and assertion
    assertThat(stateReferenceResolver.resolveReferences(stateDraft).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                "Failed to resolve 'transition' reference on StateDraft with "
                    + "key:'%s'. Reason: %s",
                stateDraft.getKey(), BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveReferences_WithEmptyKeyOnStateResourceIdentifier_ShouldNotResolveReference() {
    // preparation
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final StateService mockStateService = mock(StateService.class);
    when(mockStateService.fetchMatchingStatesByKeysWithTransitions(any()))
        .thenReturn(CompletableFuture.completedFuture(new HashSet<>()));

    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key("state-key")
            .type(StateTypeEnum.LINE_ITEM_STATE)
            .transitions(List.of(StateResourceIdentifierBuilder.of().key("").build()))
            .build();

    final StateReferenceResolver stateReferenceResolver =
        new StateReferenceResolver(stateSyncOptions, mockStateService);

    // test and assertion
    assertThat(stateReferenceResolver.resolveReferences(stateDraft).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                "Failed to resolve 'transition' reference on StateDraft with "
                    + "key:'%s'. Reason: %s",
                stateDraft.getKey(), BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveReferences_WithExceptionStateFetch_ShouldNotResolveReference() {
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final StateService mockStateService = mock(StateService.class);

    final State state = mock(State.class);
    when(state.getKey()).thenReturn("state-key");
    when(state.getId()).thenReturn("state-id");
    when(state.toResourceIdentifier())
        .thenReturn(StateResourceIdentifierBuilder.of().key("state-id").build());

    when(mockStateService.fetchMatchingStatesByKeysWithTransitions(any()))
        .thenReturn(CompletableFuture.completedFuture(Set.of(state)));

    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key("state-key")
            .type(StateTypeEnum.LINE_ITEM_STATE)
            .transitions(List.of(state.toResourceIdentifier()))
            .build();

    final CompletableFuture<Set<State>> futureThrowingSphereException = new CompletableFuture<>();
    futureThrowingSphereException.completeExceptionally(ExceptionUtils.createBadGatewayException());
    when(mockStateService.fetchMatchingStatesByKeysWithTransitions(anySet()))
        .thenReturn(futureThrowingSphereException);

    final StateReferenceResolver stateReferenceResolver =
        new StateReferenceResolver(stateSyncOptions, mockStateService);

    // test and assertion
    assertThat(stateReferenceResolver.resolveReferences(stateDraft).toCompletableFuture())
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class)
        .withMessageContaining("test");
  }

  @Test
  void resolveReferences_WithNullTransitionOnTransitionsList_ShouldNotFail() {
    // preparation
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    final StateService mockStateService = mock(StateService.class);
    when(mockStateService.fetchMatchingStatesByKeysWithTransitions(any()))
        .thenReturn(CompletableFuture.completedFuture(new HashSet<>()));

    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key("state-key")
            .type(StateTypeEnum.LINE_ITEM_STATE)
            .transitions((StateResourceIdentifier) null)
            .build();

    final StateReferenceResolver stateReferenceResolver =
        new StateReferenceResolver(stateSyncOptions, mockStateService);

    assertThat(stateReferenceResolver.resolveReferences(stateDraft).toCompletableFuture())
        .isCompleted();
  }
}
