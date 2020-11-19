package com.commercetools.sync.states;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.impl.StateServiceImpl;
import com.commercetools.sync.states.helpers.StateSyncStatistics;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateRole;
import io.sphere.sdk.states.StateType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import javax.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.states.utils.StateReferenceResolutionUtils.mapToStateDrafts;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.ThreadLocalRandom.current;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class StateSyncTest {

    private List<String> errorMessages;
    private List<Throwable> exceptions;
    private List<String> warningCallBackMessages;

    private String keyA;
    private String keyB;
    private String keyC;

    @BeforeEach
    void setUp() {
        errorMessages = new ArrayList<>();
        exceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
        keyA = "state-A-" + current().nextInt();
        keyB = "state-B-" + current().nextInt();
        keyC = "state-C-" + current().nextInt();
    }

    @Test
    void sync_WithInvalidDrafts_ShouldCompleteWithoutAnyProcessing() {
        // preparation
        final SphereClient ctpClient = mock(SphereClient.class);
        final List<String> errors = new ArrayList<>();
        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(ctpClient)
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errors.add(exception.getMessage());
            })
            .build();

        final StateService stateService = mock(StateService.class);
        final StateSync stateSync = new StateSync(stateSyncOptions, stateService);

        final StateDraft stateDraftWithoutKey = StateDraftBuilder
            .of(null, StateType.LINE_ITEM_STATE)
            .name(LocalizedString.ofEnglish("state-name"))
            .build();

        // test
        final StateSyncStatistics statistics = stateSync
            .sync(asList(stateDraftWithoutKey, null))
            .toCompletableFuture()
            .join();

        // assertion
        verifyNoMoreInteractions(ctpClient);
        verifyNoMoreInteractions(stateService);
        assertThat(errors).hasSize(2);
        assertThat(errors).containsExactly(
            "StateDraft with name: LocalizedString(en -> state-name) doesn't have a key. "
                + "Please make sure all state drafts have keys.",
            "StateDraft is null.");

        assertThat(statistics).hasValues(2, 0, 0, 2, 0);
    }

    @Test
    void sync_WithErrorCachingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        final StateDraft stateDraft = StateDraftBuilder
            .of("state-1", StateType.LINE_ITEM_STATE)
            .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final StateSyncOptions syncOptions = StateSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception.getCause());
            })
            .build();

        final StateService stateService = spy(new StateServiceImpl(syncOptions));
        when(stateService.cacheKeysToIds(anySet()))
            .thenReturn(supplyAsync(() -> { throw new SphereException(); }));

        final StateSync stateSync = new StateSync(syncOptions, stateService);

        // test
        final StateSyncStatistics stateSyncStatistics = stateSync
            .sync(singletonList(stateDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .singleElement().satisfies(message ->
                assertThat(message).contains("Failed to build a cache of keys to ids.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .singleElement().satisfies(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
            });

        assertThat(stateSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        final StateDraft stateDraft = StateDraftBuilder
            .of("state-1", StateType.LINE_ITEM_STATE)
            .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final SphereClient mockClient = mock(SphereClient.class);
        final StateSyncOptions syncOptions = StateSyncOptionsBuilder
            .of(mockClient)
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception.getCause());
            })
            .build();

        final StateService stateService = mock(StateService.class);
        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(stateDraft.getKey(), UUID.randomUUID().toString());
        when(stateService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(stateService.fetchMatchingStatesByKeysWithTransitions(anySet()))
                .thenReturn(supplyAsync(() -> { throw new CompletionException(new SphereException()); }));
        final StateSync stateSync = new StateSync(syncOptions, stateService);

        // test
        final StateSyncStatistics stateSyncStatistics = stateSync
            .sync(singletonList(stateDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
                .hasSize(1)
                .singleElement().satisfies(message ->
                        assertThat(message).contains("Failed to fetch existing states")
            );

        assertThat(exceptions)
                .hasSize(1)
                .singleElement().satisfies(throwable -> {
                    assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                    assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
                });

        assertThat(stateSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
        // preparation
        final StateDraft stateDraft = StateDraftBuilder
            .of("state-1", StateType.LINE_ITEM_STATE)
            .build();

        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .build();

        final StateService stateService = mock(StateService.class);
        when(stateService.cacheKeysToIds(anySet())).thenReturn(completedFuture(emptyMap()));
        when(stateService.fetchMatchingStatesByKeysWithTransitions(anySet())).thenReturn(completedFuture(emptySet()));
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
        final StateDraft stateDraft = StateDraftBuilder
            .of("state-1", StateType.LINE_ITEM_STATE)
            .name(LocalizedString.ofEnglish("foo"))
            .transitions(null)
            .build();

        final State mockedExistingState = mock(State.class);
        when(mockedExistingState.getKey()).thenReturn(stateDraft.getKey());
        when(mockedExistingState.getName()).thenReturn(LocalizedString.ofEnglish("bar"));
        when(mockedExistingState.getTransitions()).thenReturn(null);

        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .build();

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

    @Test
    void syncDrafts_WithConcurrentModificationException_ShouldRetryToUpdateNewStateWithSuccess() {
        // Preparation
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<String> warningCallBackMessages = new ArrayList<>();

        final String key = "state-" + current().nextInt();
        final StateDraft stateDraft = StateDraftBuilder
                .of(key, StateType.REVIEW_STATE)
                .name(ofEnglish("state-name-updated"))
                .description(ofEnglish("state-desc-updated"))
                .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
                .initial(true)
                .build();

        final StateService stateService =
                buildMockStateServiceWithSuccessfulUpdateOnRetry(stateDraft);

        final StateSyncOptions syncOptions =
                StateSyncOptionsBuilder.of(mock(SphereClient.class))
                        .errorCallback((exception, oldResource, newResource, updateActions) -> {
                            errorMessages.add(exception.getMessage());
                            exceptions.add(exception.getCause());
                        })
                        .warningCallback((exception, oldResource, newResource)
                                -> warningCallBackMessages.add(exception.getMessage()))
                        .build();

        final StateSync stateSync = new StateSync(syncOptions, stateService);

        // Test
        final StateSyncStatistics statistics = stateSync.sync(singletonList(stateDraft))
                .toCompletableFuture()
                .join();

        // Assertion
        AssertionsForStatistics.assertThat(statistics).hasValues(1, 0, 1, 0);
        assertThat(exceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    void syncDrafts_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
        // Preparation
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<String> warningCallBackMessages = new ArrayList<>();

        final String key = "state-" + current().nextInt();
        final StateDraft stateDraft = StateDraftBuilder
                .of(key, StateType.REVIEW_STATE)
                .name(ofEnglish("state-name-updated"))
                .description(ofEnglish("state-desc-updated"))
                .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
                .initial(true)
                .build();

        final StateService stateService =
                buildMockStateServiceWithFailedFetchOnRetry(stateDraft);

        final StateSyncOptions syncOptions =
                StateSyncOptionsBuilder.of(mock(SphereClient.class))
                        .errorCallback((exception, oldResource, newResource, updateActions) -> {
                            errorMessages.add(exception.getMessage());
                            exceptions.add(exception.getCause());
                        })
                        .warningCallback((exception, oldResource, newResource)
                                -> warningCallBackMessages.add(exception.getMessage()))
                        .build();

        final StateSync stateSync = new StateSync(syncOptions, stateService);

        // Test
        final StateSyncStatistics statistics = stateSync.sync(singletonList(stateDraft))
                .toCompletableFuture()
                .join();

        // Assertion
        AssertionsForStatistics.assertThat(statistics).hasValues(1, 0, 0, 1);

        assertThat(errorMessages).hasSize(1);
        assertThat(exceptions).hasSize(1);
        assertThat(exceptions.get(0)).isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorMessages.get(0)).contains(
                format("Failed to update state with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                        + "after concurrency modification.", stateDraft.getKey()));
    }

    @Test
    void syncDrafts_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
        // Preparation
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<String> warningCallBackMessages = new ArrayList<>();

        final String key = "state-" + current().nextInt();
        final StateDraft stateDraft = StateDraftBuilder
                .of(key, StateType.REVIEW_STATE)
                .name(ofEnglish("state-name-updated"))
                .description(ofEnglish("state-desc-updated"))
                .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
                .initial(true)
                .build();

        final StateService stateService =
                buildMockStateServiceWithNotFoundFetchOnRetry(stateDraft);

        final StateSyncOptions syncOptions =
                StateSyncOptionsBuilder.of(mock(SphereClient.class))
                        .errorCallback((exception, oldResource, newResource, updateActions) -> {
                            errorMessages.add(exception.getMessage());
                            exceptions.add(exception.getCause());
                        })
                        .warningCallback((exception, oldResource, newResource)
                                -> warningCallBackMessages.add(exception.getMessage()))
                        .build();

        final StateSync stateSync = new StateSync(syncOptions, stateService);

        // Test
        final StateSyncStatistics statistics = stateSync.sync(singletonList(stateDraft))
                .toCompletableFuture()
                .join();

        // Assertion
        AssertionsForStatistics.assertThat(statistics).hasValues(1, 0, 0, 1);

        AssertionsForStatistics.assertThat(statistics).hasValues(1, 0, 0, 1);
        assertThat(errorMessages).hasSize(1);
        assertThat(exceptions).hasSize(1);
        assertThat(errorMessages.get(0)).contains(
                format("Failed to update state with key: '%s'. Reason: Not found when attempting to fetch while "
                        + "retrying after concurrency modification.", stateDraft.getKey()));
    }

    @Test
    void sync_WithExceptionOnResolvingTransition_ShouldUpdateTransitions() {
        final StateDraft stateCDraft = createStateDraft(keyC);
        final State stateC = mock(State.class);
        when(stateC.getKey()).thenReturn(stateCDraft.getKey());

        final StateDraft stateBDraft = createStateDraft(keyB, stateC);
        final State stateB = mock(State.class);
        when(stateB.getKey()).thenReturn(stateBDraft.getKey());

        final StateService mockStateService = mock(StateService.class);
        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(stateCDraft.getKey(), UUID.randomUUID().toString());
        keyToIds.put(stateBDraft.getKey(), UUID.randomUUID().toString());

        when(mockStateService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockStateService.fetchMatchingStatesByKeysWithTransitions(anySet()))
                .thenReturn(exceptionallyCompletedFuture(new BadGatewayException()));

        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .batchSize(3)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorMessages.add(exception.getMessage());
                    exceptions.add(exception.getCause());
                })
                .warningCallback((exception, newResource, oldResource) ->
                        warningCallBackMessages.add(exception.getMessage()))
                .build();

        final StateSync stateSync = new StateSync(stateSyncOptions, mockStateService);
        final List<StateDraft> stateDrafts = mapToStateDrafts(Arrays.asList(stateB, stateC));
        // test
        final StateSyncStatistics stateSyncStatistics = stateSync
                .sync(stateDrafts)
                .toCompletableFuture()
                .join();

        assertThat(stateSyncStatistics).hasValues(2, 0, 0, 2, 0);
        Assertions.assertThat(exceptions).isNotEmpty();
        Assertions.assertThat(errorMessages).isNotEmpty();
        Assertions.assertThat(errorMessages.get(0))
                .contains("Failed to fetch existing states with keys");
        Assertions.assertThat(warningCallBackMessages).isEmpty();
    }

    @Nonnull
    private StateService buildMockStateServiceWithSuccessfulUpdateOnRetry(
            @Nonnull final StateDraft stateDraft) {

        final StateService mockStateService = mock(StateService.class);

        final State mockState = mock(State.class);
        when(mockState.getKey()).thenReturn(stateDraft.getKey());

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(stateDraft.getKey(), UUID.randomUUID().toString());

        when(mockStateService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockStateService.fetchMatchingStatesByKeysWithTransitions(anySet()))
                .thenReturn(completedFuture(singleton(mockState)));
        when(mockStateService.fetchState(any())).thenReturn(completedFuture(Optional.of(mockState)));
        when(mockStateService.updateState(any(), anyList()))
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(mockState));
        return mockStateService;
    }

    @Nonnull
    private StateService buildMockStateServiceWithFailedFetchOnRetry(
            @Nonnull final StateDraft stateDraft) {

        final StateService mockStateService = mock(StateService.class);

        final State mockState = mock(State.class);
        when(mockState.getKey()).thenReturn(stateDraft.getKey());

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(stateDraft.getKey(), UUID.randomUUID().toString());

        when(mockStateService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockStateService.fetchMatchingStatesByKeysWithTransitions(anySet()))
                .thenReturn(completedFuture(singleton(mockState)));
        when(mockStateService.fetchState(any()))
                .thenReturn(exceptionallyCompletedFuture(new BadGatewayException()));
        when(mockStateService.updateState(any(), anyList()))
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(mockState));
        return mockStateService;
    }

    @Nonnull
    private StateService buildMockStateServiceWithNotFoundFetchOnRetry(
            @Nonnull final StateDraft stateDraft) {

        final StateService mockStateService = mock(StateService.class);

        final State mockState = mock(State.class);
        when(mockState.getKey()).thenReturn(stateDraft.getKey());

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(stateDraft.getKey(), UUID.randomUUID().toString());

        when(mockStateService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockStateService.fetchMatchingStatesByKeysWithTransitions(anySet()))
                .thenReturn(completedFuture(singleton(mockState)));
        when(mockStateService.fetchState(any())).thenReturn(completedFuture(Optional.empty()));
        when(mockStateService.updateState(any(), anyList()))
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(mockState));
        return mockStateService;
    }

    @Nonnull
    private List<Reference<State>> buildReferences(@Nonnull final State... transitionStates) {
        List<Reference<State>> references = new ArrayList<>();
        if (transitionStates.length > 0) {
            for (State transitionState : transitionStates) {
                references.add(Reference.of("state", transitionState.getId(), transitionState));
            }
        }
        return references;
    }

    @Nonnull
    private StateDraft createStateDraft(final String key, final State... transitionStates) {
        List<Reference<State>> references = buildReferences();
        return createStateDraftWithReference(key, references);
    }

    @Nonnull
    private StateDraft createStateDraftWithReference(final String key, final List<Reference<State>> references) {
        return StateDraftBuilder
                .of(key, StateType.REVIEW_STATE)
                .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
                .transitions(new HashSet<>(references))
                .initial(true)
                .build();
    }
}
