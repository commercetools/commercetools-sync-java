package com.commercetools.sync.states;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyStatesGet;
import com.commercetools.api.client.ByProjectKeyStatesRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.sync.commons.ExceptionUtils;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.impl.StateServiceImpl;
import com.commercetools.sync.states.helpers.StateSyncStatistics;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.error.BaseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StateSyncTest {

  @Test
  void sync_WithInvalidDrafts_ShouldCompleteWithoutAnyProcessing() {
    // preparation
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);
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
        StateDraftBuilder.of()
            .key("")
            .type(StateTypeEnum.LINE_ITEM_STATE)
            .name(ofEnglish("state-name"))
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
        StateDraftBuilder.of().key("state-1").type(StateTypeEnum.LINE_ITEM_STATE).build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final StateSyncOptions syncOptions =
        StateSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final StateService stateService = Mockito.spy(new StateServiceImpl(syncOptions));
    when(stateService.cacheKeysToIds(anySet()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new BaseException();
                }));

    final StateSync stateSync = new StateSync(syncOptions, stateService);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(singletonList(stateDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to build a cache of keys to ids.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(CompletionException.class)
        .hasCauseExactlyInstanceOf(BaseException.class);

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 1);
  }

  @Test
  void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final StateDraft stateDraft =
        StateDraftBuilder.of().key("state-1").type(StateTypeEnum.LINE_ITEM_STATE).build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProjectApiRoot mockClient = mock(ProjectApiRoot.class);
    final ByProjectKeyStatesRequestBuilder byProjectKeyStatesRequestBuilder = mock();
    when(mockClient.states()).thenReturn(byProjectKeyStatesRequestBuilder);
    final ByProjectKeyStatesGet byProjectKeyStatesGet = mock();
    when(byProjectKeyStatesRequestBuilder.get()).thenReturn(byProjectKeyStatesGet);
    when(byProjectKeyStatesGet.withWhere(anyString())).thenReturn(byProjectKeyStatesGet);
    when(byProjectKeyStatesGet.withPredicateVar(anyString(), any(Collection.class)))
        .thenReturn(byProjectKeyStatesGet);
    when(byProjectKeyStatesGet.withExpand(anyString())).thenReturn(byProjectKeyStatesGet);
    when(byProjectKeyStatesGet.withLimit(anyInt())).thenReturn(byProjectKeyStatesGet);
    when(byProjectKeyStatesGet.withWithTotal(anyBoolean())).thenReturn(byProjectKeyStatesGet);
    when(byProjectKeyStatesGet.execute())
        .thenReturn(CompletableFuture.failedFuture(ExceptionUtils.createBadGatewayException()));

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
        .singleElement(as(STRING))
        .contains("Failed to fetch existing states");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(CompletionException.class)
        .hasCauseExactlyInstanceOf(BadGatewayException.class);

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 1);
  }

  @Test
  void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
    // preparation
    final StateDraft stateDraft =
        StateDraftBuilder.of().key("state-1").type(StateTypeEnum.LINE_ITEM_STATE).build();

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

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
        StateDraftBuilder.of()
            .key("state-1")
            .type(StateTypeEnum.LINE_ITEM_STATE)
            .name(ofEnglish("foo"))
            .transitions((List<StateResourceIdentifier>) null)
            .build();

    final State mockedExistingState = mock(State.class);
    when(mockedExistingState.getKey()).thenReturn(stateDraft.getKey());
    when(mockedExistingState.getName()).thenReturn(ofEnglish("bar"));
    when(mockedExistingState.getTransitions()).thenReturn(null);

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

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
