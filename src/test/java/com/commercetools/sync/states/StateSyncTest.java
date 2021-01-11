package com.commercetools.sync.states;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.impl.StateServiceImpl;
import com.commercetools.sync.states.helpers.StateSyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.queries.StateQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

class StateSyncTest {

  @Test
  void sync_WithInvalidDrafts_ShouldCompleteWithoutAnyProcessing() {
    // preparation
    final SphereClient ctpClient = mock(SphereClient.class);
    final List<String> errors = new ArrayList<>();
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(ctpClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errors.add(exception.getMessage());
                })
            .build();

    final StateService stateService = mock(StateService.class);
    final StateSync stateSync = new StateSync(stateSyncOptions, stateService);

    final StateDraft stateDraftWithoutKey =
        StateDraftBuilder.of(null, StateType.LINE_ITEM_STATE)
            .name(LocalizedString.ofEnglish("state-name"))
            .build();

    // test
    final StateSyncStatistics statistics =
        stateSync.sync(asList(stateDraftWithoutKey, null)).toCompletableFuture().join();

    // assertion
    verifyNoMoreInteractions(ctpClient);
    verifyNoMoreInteractions(stateService);
    assertThat(errors).hasSize(2);
    assertThat(errors)
        .containsExactly(
            "StateDraft with name: LocalizedString(en -> state-name) doesn't have a key. "
                + "Please make sure all state drafts have keys.",
            "StateDraft is null.");

    assertThat(statistics).hasValues(2, 0, 0, 2, 0);
  }

  @Test
  void sync_WithErrorCachingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final StateDraft stateDraft =
        StateDraftBuilder.of("state-1", StateType.LINE_ITEM_STATE).build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final StateSyncOptions syncOptions =
        StateSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final StateService stateService = spy(new StateServiceImpl(syncOptions));
    when(stateService.cacheKeysToIds(anySet()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new SphereException();
                }));

    final StateSync stateSync = new StateSync(syncOptions, stateService);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(singletonList(stateDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            message -> assertThat(message).contains("Failed to build a cache of keys to ids."));

    assertThat(exceptions)
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            throwable -> {
              assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
              assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
            });

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 1);
  }

  @Test
  void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final StateDraft stateDraft =
        StateDraftBuilder.of("state-1", StateType.LINE_ITEM_STATE).build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final SphereClient mockClient = mock(SphereClient.class);
    when(mockClient.execute(any(StateQuery.class)))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new SphereException();
                }));

    final StateSyncOptions syncOptions =
        StateSyncOptionsBuilder.of(mockClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final StateService stateService = spy(new StateServiceImpl(syncOptions));
    final Map<String, String> keyToIds = new HashMap<>();
    keyToIds.put(stateDraft.getKey(), UUID.randomUUID().toString());
    when(stateService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));

    final StateSync stateSync = new StateSync(syncOptions, stateService);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(singletonList(stateDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            message -> assertThat(message).contains("Failed to fetch existing states"));

    assertThat(exceptions)
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            throwable -> {
              assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
              assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
            });

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 1);
  }

  @Test
  void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
    // preparation
    final StateDraft stateDraft =
        StateDraftBuilder.of("state-1", StateType.LINE_ITEM_STATE).build();

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    final StateService stateService = mock(StateService.class);
    when(stateService.cacheKeysToIds(anySet())).thenReturn(completedFuture(emptyMap()));
    when(stateService.fetchMatchingStatesByKeysWithTransitions(anySet()))
        .thenReturn(completedFuture(emptySet()));
    when(stateService.createState(any())).thenReturn(completedFuture(Optional.empty()));

    final StateSyncOptions spyStateSyncOptions = spy(stateSyncOptions);

    final StateSync stateSync = new StateSync(spyStateSyncOptions, stateService);

    // test
    stateSync.sync(singletonList(stateDraft)).toCompletableFuture().join();

    // assertion
    verify(spyStateSyncOptions).applyBeforeCreateCallback(any());
    verify(spyStateSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
  }

  @Test
  void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
    // preparation
    final StateDraft stateDraft =
        StateDraftBuilder.of("state-1", StateType.LINE_ITEM_STATE)
            .name(LocalizedString.ofEnglish("foo"))
            .transitions(null)
            .build();

    final State mockedExistingState = mock(State.class);
    when(mockedExistingState.getKey()).thenReturn(stateDraft.getKey());
    when(mockedExistingState.getName()).thenReturn(LocalizedString.ofEnglish("bar"));
    when(mockedExistingState.getTransitions()).thenReturn(null);

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    final StateService stateService = mock(StateService.class);
    final Map<String, String> keyToIds = new HashMap<>();
    keyToIds.put(stateDraft.getKey(), UUID.randomUUID().toString());
    when(stateService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
    when(stateService.fetchMatchingStatesByKeysWithTransitions(anySet()))
        .thenReturn(completedFuture(singleton(mockedExistingState)));
    when(stateService.updateState(any(), any())).thenReturn(completedFuture(mockedExistingState));

    final StateSyncOptions spyStateSyncOptions = spy(stateSyncOptions);

    final StateSync stateSync = new StateSync(spyStateSyncOptions, stateService);

    // test
    stateSync.sync(singletonList(stateDraft)).toCompletableFuture().join();

    // assertion
    verify(spyStateSyncOptions).applyBeforeUpdateCallback(any(), any(), any());
    verify(spyStateSyncOptions, never()).applyBeforeCreateCallback(any());
  }
}
