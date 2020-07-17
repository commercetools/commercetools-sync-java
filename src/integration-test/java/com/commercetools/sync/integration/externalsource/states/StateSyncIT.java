package com.commercetools.sync.integration.externalsource.states;

import com.commercetools.sync.commons.models.WaitingToBeResolvedTransitions;
import com.commercetools.sync.services.impl.UnresolvedTransitionsServiceImpl;
import com.commercetools.sync.states.StateSync;
import com.commercetools.sync.states.StateSyncOptions;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import com.commercetools.sync.states.helpers.StateSyncStatistics;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateRole;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.StateCreateCommand;
import io.sphere.sdk.states.commands.StateUpdateCommand;
import io.sphere.sdk.states.expansion.StateExpansionModel;
import io.sphere.sdk.states.queries.StateQuery;
import io.sphere.sdk.states.queries.StateQueryBuilder;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.deleteStates;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.deleteStatesFromTargetAndSourceByType;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.getStateByKey;
import static com.commercetools.sync.states.utils.StateTransitionReferenceReplacementUtils.replaceStateTransitionIdsWithKeys;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.states.State.referenceOfId;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class StateSyncIT {


    String stateKey = "";

    @AfterAll
    static void tearDown() {
        deleteStates(CTP_TARGET_CLIENT);
        deleteStates(CTP_SOURCE_CLIENT);
    }

    @BeforeEach
    void setup() {
        stateKey = "state-" + ThreadLocalRandom.current().nextInt();
        deleteStatesFromTargetAndSourceByType();
        final StateDraft stateDraft = StateDraftBuilder
            .of(stateKey, StateType.LINE_ITEM_STATE)
            .name(LocalizedString.ofEnglish("state-name"))
            .description(LocalizedString.ofEnglish("state-desc"))
            .roles(Collections.singleton(StateRole.RETURN))
            .initial(false)
            .build();

        executeBlocking(CTP_TARGET_CLIENT.execute(StateCreateCommand.of(stateDraft)));
    }

    @Test
    void sync_withNewState_shouldCreateState() {
        final StateDraft stateDraft = StateDraftBuilder
            .of("new-state", StateType.REVIEW_STATE)
            .roles(Collections.singleton(
                StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .build();

        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final StateSync stateSync = new StateSync(stateSyncOptions);

        // test
        final StateSyncStatistics stateSyncStatistics = stateSync
            .sync(singletonList(stateDraft))
            .toCompletableFuture()
            .join();

        assertThat(stateSyncStatistics).hasValues(1, 1, 0, 0, 0);
    }

    @Test
    void sync_WithUpdatedState_ShouldUpdateState() {
        // preparation
        String key = stateKey;
        final StateDraft stateDraft = StateDraftBuilder
            .of(key, StateType.REVIEW_STATE)
            .name(ofEnglish("state-name-updated"))
            .description(ofEnglish("state-desc-updated"))
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .initial(true)
            .build();

        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final StateSync stateSync = new StateSync(stateSyncOptions);

        // test
        final StateSyncStatistics stateSyncStatistics = stateSync
            .sync(singletonList(stateDraft))
            .toCompletableFuture()
            .join();

        // assertion
        assertThat(stateSyncStatistics).hasValues(1, 0, 1, 0, 0);

        final Optional<State> oldStateAfter = getStateByKey(CTP_TARGET_CLIENT, key);

        Assertions.assertThat(oldStateAfter).hasValueSatisfying(state -> {
            Assertions.assertThat(state.getType()).isEqualTo(StateType.REVIEW_STATE);
            Assertions.assertThat(state.getName()).isEqualTo(ofEnglish("state-name-updated"));
            Assertions.assertThat(state.getDescription()).isEqualTo(ofEnglish("state-desc-updated"));
            Assertions.assertThat(state.getRoles())
                .isEqualTo(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS));
            Assertions.assertThat(state.isInitial()).isEqualTo(true);
        });
        deleteStates(CTP_TARGET_CLIENT);
    }

    @Test
    void sync_withEqualState_shouldNotUpdateState() {
        StateDraft stateDraft = StateDraftBuilder
            .of(stateKey, StateType.LINE_ITEM_STATE)
            .name(ofEnglish("state-name"))
            .description(ofEnglish("state-desc"))
            .roles(Collections.singleton(StateRole.RETURN))
            .initial(false)
            .build();

        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final StateSync stateSync = new StateSync(stateSyncOptions);

        // test
        final StateSyncStatistics stateSyncStatistics = stateSync
            .sync(singletonList(stateDraft))
            .toCompletableFuture()
            .join();

        assertThat(stateSyncStatistics).hasValues(1, 0, 0, 0, 0);
    }

    @Test
    void sync_withChangedStateButConcurrentModificationException_shouldRetryAndUpdateState() {
        // preparation
        final SphereClient spyClient = buildClientWithConcurrentModificationUpdate();

        List<String> errorCallBackMessages = new ArrayList<>();
        List<String> warningCallBackMessages = new ArrayList<>();
        List<Throwable> errorCallBackExceptions = new ArrayList<>();
        final StateSyncOptions spyOptions = StateSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((errorMessage, throwable) -> {
                errorCallBackMessages.add(errorMessage);
                errorCallBackExceptions.add(throwable);
            })
            .warningCallback(warningCallBackMessages::add)
            .build();

        final StateSync stateSync = new StateSync(spyOptions);

        final StateDraft stateDraft = StateDraftBuilder
            .of(stateKey, StateType.REVIEW_STATE)
            .name(ofEnglish("state-name-updated"))
            .description(ofEnglish("state-desc-updated"))
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .initial(true)
            .build();

        final StateSyncStatistics syncStatistics =
            executeBlocking(stateSync.sync(singletonList(stateDraft)));

        assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
        Assertions.assertThat(errorCallBackExceptions).isEmpty();
        Assertions.assertThat(errorCallBackMessages).isEmpty();
        Assertions.assertThat(warningCallBackMessages).isEmpty();
    }

    @Nonnull
    private SphereClient buildClientWithConcurrentModificationUpdate() {
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

        final StateUpdateCommand updateCommand = any(StateUpdateCommand.class);
        when(spyClient.execute(updateCommand))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new ConcurrentModificationException()))
            .thenCallRealMethod();

        return spyClient;
    }

    @Test
    void sync_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
        // preparation
        final SphereClient spyClient = buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry();

        List<String> errorCallBackMessages = new ArrayList<>();
        List<String> warningCallBackMessages = new ArrayList<>();
        List<Throwable> errorCallBackExceptions = new ArrayList<>();
        final StateSyncOptions spyOptions = StateSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((errorMessage, throwable) -> {
                errorCallBackMessages.add(errorMessage);
                errorCallBackExceptions.add(throwable);
            })
            .warningCallback(warningCallBackMessages::add)
            .build();

        final StateSync stateSync = new StateSync(spyOptions);

        final StateDraft stateDraft = StateDraftBuilder
            .of(stateKey, StateType.REVIEW_STATE)
            .name(ofEnglish("state-name-updated"))
            .description(ofEnglish("state-desc-updated"))
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .initial(true)
            .build();

        final StateSyncStatistics syncStatistics =
            executeBlocking(stateSync.sync(singletonList(stateDraft)));

        // Test and assertion
        assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
        Assertions.assertThat(errorCallBackMessages).hasSize(1);
        Assertions.assertThat(errorCallBackExceptions).hasSize(1);

        Assertions.assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);
        Assertions.assertThat(errorCallBackMessages.get(0)).contains(
            format("Failed to update state with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                + "after concurrency modification.", stateDraft.getKey()));
    }

    @Nonnull
    private SphereClient buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry() {
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

        final StateUpdateCommand updateCommand = any(StateUpdateCommand.class);
        when(spyClient.execute(updateCommand))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new ConcurrentModificationException()))
            .thenCallRealMethod();

        final StateQuery stateQuery = any(StateQuery.class);
        when(spyClient.execute(stateQuery))
            .thenCallRealMethod() // cache state keys
            .thenCallRealMethod() // Call real fetch on fetching matching states
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));

        return spyClient;
    }

    @Test
    void sync_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
        // preparation
        final SphereClient spyClient = buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry();

        List<String> errorCallBackMessages = new ArrayList<>();
        List<String> warningCallBackMessages = new ArrayList<>();
        List<Throwable> errorCallBackExceptions = new ArrayList<>();
        final StateSyncOptions spyOptions = StateSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((errorMessage, throwable) -> {
                errorCallBackMessages.add(errorMessage);
                errorCallBackExceptions.add(throwable);
            })
            .warningCallback(warningCallBackMessages::add)
            .build();

        final StateSync stateSync = new StateSync(spyOptions);

        final StateDraft stateDraft = StateDraftBuilder
            .of(stateKey, StateType.REVIEW_STATE)
            .name(ofEnglish("state-name-updated"))
            .description(ofEnglish("state-desc-updated"))
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .initial(true)
            .build();

        final StateSyncStatistics syncStatistics =
            executeBlocking(stateSync.sync(singletonList(stateDraft)));

        // Test and assertion
        assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
        Assertions.assertThat(errorCallBackMessages).hasSize(1);
        Assertions.assertThat(errorCallBackExceptions).hasSize(1);

        Assertions.assertThat(errorCallBackMessages.get(0)).contains(
            format("Failed to update state with key: '%s'. Reason: Not found when attempting to fetch while"
                + " retrying after concurrency modification.", stateDraft.getKey()));
    }

    @Nonnull
    private SphereClient buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry() {
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

        final StateUpdateCommand stateUpdateCommand = any(StateUpdateCommand.class);
        when(spyClient.execute(stateUpdateCommand))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new ConcurrentModificationException()))
            .thenCallRealMethod();

        final StateQuery stateQuery = any(StateQuery.class);

        when(spyClient.execute(stateQuery))
            .thenCallRealMethod() // cache state keys
            .thenCallRealMethod() // Call real fetch on fetching matching states
            .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

        return spyClient;
    }

    @Test
    void sync_WithSeveralBatches_ShouldReturnProperStatistics() {
        // 2 batches
        final List<StateDraft> stateDrafts = IntStream
            .range(0, 10)
            .mapToObj(i -> StateDraft
                .of("key" + i, StateType.REVIEW_STATE)
                .withName(ofEnglish("name" + i)))
            .collect(Collectors.toList());

        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .batchSize(5)
            .build();

        final StateSync stateSync = new StateSync(stateSyncOptions);

        // test
        final StateSyncStatistics stateSyncStatistics = stateSync
            .sync(stateDrafts)
            .toCompletableFuture()
            .join();

        assertThat(stateSyncStatistics).hasValues(10, 10, 0, 0, 0);
    }


    @Test
    void sync_WithNotExistentStates_ShouldResolveStateLater() {
        // prepare the target project
        String keyB = "state-B";
        String keyC = "state-C";
        String keyA = "state-A";

        final StateDraft stateCDraft = createStateDraft(keyC);
        final State stateC = createStateInSource(stateCDraft);

        final StateDraft stateBDraft = createStateDraft(keyB, stateC);
        final State stateB = createStateInSource(stateBDraft);

        StateDraft[] draftsWithReplacesKeys =
            replaceStateTransitionIdsWithKeys(asList(stateB, stateC)).toArray(new StateDraft[2]);
        final StateDraft stateADraft = createStateDraftReferencingStateDrafts(keyA, draftsWithReplacesKeys);
        final List<StateDraft> stateDrafts = asList(stateADraft);

        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .batchSize(1)
            .build();

        final StateSync stateSync = new StateSync(stateSyncOptions);

        // test
        final StateSyncStatistics stateSyncStatistics = stateSync
            .sync(stateDrafts)
            .toCompletableFuture()
            .join();

        assertThat(stateSyncStatistics).hasValues(1, 0, 0, 0, 1);
        UnresolvedTransitionsServiceImpl unresolvedTransitionsService =
            new UnresolvedTransitionsServiceImpl(stateSyncOptions);
        Set<WaitingToBeResolvedTransitions> result =
            unresolvedTransitionsService.fetch(new HashSet<>(asList(keyA))).toCompletableFuture().join();
        Assertions.assertThat(result.size()).isEqualTo(1);
        WaitingToBeResolvedTransitions waitingToBeResolvedTransitions = result.iterator().next();
        Assertions
            .assertThat(waitingToBeResolvedTransitions.getMissingTransitionStateKeys().containsAll(asList(keyB, keyC)))
            .isTrue();
        Assertions.assertThat(waitingToBeResolvedTransitions.getStateDraft().getKey()).isEqualTo(keyA);
    }

    @Test
    void sync_WithAllExistentStates_ShouldResolveAllStates() {
        String keyB = "state-B";
        String keyC = "state-C";
        String keyA = "state-A";

        final StateDraft stateCDraft = createStateDraft(keyC);
        final State stateC = createStateInSource(stateCDraft);


        final StateDraft stateBDraft = createStateDraft(keyB, stateC);
        final State stateB = createStateInSource(stateBDraft);

        final StateDraft stateADraft = createStateDraft(keyA, stateB, stateC);
        final State stateA = createStateInSource(stateADraft);

        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .batchSize(3)
            .build();

        final StateSync stateSync = new StateSync(stateSyncOptions);
        final List<StateDraft> stateDrafts = replaceStateTransitionIdsWithKeys(Arrays.asList(stateA, stateB, stateC));
        // test
        final StateSyncStatistics stateSyncStatistics = stateSync
            .sync(stateDrafts)
            .toCompletableFuture()
            .join();

        assertThat(stateSyncStatistics).hasValues(3, 3, 0, 0, 0);
        UnresolvedTransitionsServiceImpl unresolvedTransitionsService =
            new UnresolvedTransitionsServiceImpl(stateSyncOptions);
        Set<WaitingToBeResolvedTransitions> result =
            unresolvedTransitionsService.fetch(new HashSet<>(asList(keyA))).toCompletableFuture().join();
        Assertions.assertThat(result.size()).isEqualTo(0);
    }

    @Test
    void sync_WithUpdatedTransition_ShouldUpdateTransitions() {
        String keyA = "state-A";
        String keyB = "state-B";
        String keyC = "state-C";

        final StateDraft stateCDraft = createStateDraft(keyC);
        final State stateC = createStateInSource(stateCDraft);
        final StateDraft tagetStateCDraft = createStateDraft(keyC);
        final State targetStateC = createStateInTarget(tagetStateCDraft);

        final StateDraft stateBDraft = createStateDraft(keyB, stateC);
        final State stateB = createStateInSource(stateBDraft);
        final StateDraft tagetStateBDraft = createStateDraft(keyB, targetStateC);
        final State targetStateB = createStateInTarget(tagetStateBDraft);

        final StateDraft stateADraft = createStateDraft(keyA, stateB, stateC);
        final State stateA = createStateInSource(stateADraft);
        final StateDraft tagetStateADraft = createStateDraft(keyA, targetStateB);
        final State targetStateA = createStateInTarget(tagetStateADraft);
        Assertions.assertThat(targetStateB.getTransitions().size()).isEqualTo(1);
        Assertions.assertThat(targetStateA.getTransitions().size()).isEqualTo(1);
        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .batchSize(3)
            .build();

        final StateSync stateSync = new StateSync(stateSyncOptions);
        final List<StateDraft> stateDrafts = replaceStateTransitionIdsWithKeys(Arrays.asList(stateA, stateB, stateC));
        // test
        final StateSyncStatistics stateSyncStatistics = stateSync
            .sync(stateDrafts)
            .toCompletableFuture()
            .join();

        assertThat(stateSyncStatistics).hasValues(3, 0, 1, 0, 0);
        UnresolvedTransitionsServiceImpl unresolvedTransitionsService =
            new UnresolvedTransitionsServiceImpl(stateSyncOptions);

        QueryExecutionUtils.queryAll(CTP_TARGET_CLIENT, StateQueryBuilder
            .of()
            .plusPredicates(q -> q.key().is(keyA)).build()).
            thenAccept(resultStates -> {
                Assertions.assertThat(resultStates.size()).isEqualTo(1);
                Assertions.assertThat(resultStates.get(0).getTransitions().size()).isEqualTo(2);
            }).toCompletableFuture().join();
    }

    private State createStateInSource(StateDraft draft) {
        return executeBlocking(CTP_SOURCE_CLIENT.execute(StateCreateCommand.of(draft)
            .withExpansionPaths(StateExpansionModel::transitions)));
    }

    private State createStateInTarget(StateDraft draft) {
        return executeBlocking(CTP_TARGET_CLIENT.execute(StateCreateCommand.of(draft)
            .withExpansionPaths(StateExpansionModel::transitions)));
    }


    private StateDraft createStateDraftReferencingStateDrafts(String key, StateDraft... transitionStatesDraft) {
        List<Reference<State>> references = new ArrayList<>();
        if (transitionStatesDraft.length > 0) {
            for (StateDraft transitionState : transitionStatesDraft) {
                references.add(referenceOfId(transitionState.getKey()));
            }
        }
        return createStateDraftWithReference(key, references);
    }

    private StateDraft createStateDraft(String key, State... transitionStates) {
        List<Reference<State>> references = new ArrayList<>();
        if (transitionStates.length > 0) {
            for (State transitionState : transitionStates) {
                references.add(referenceOfId(transitionState.getId()));
            }
        }
        return createStateDraftWithReference(key, references);
    }

    private StateDraft createStateDraftWithReference(String key, List<Reference<State>> references) {
        return StateDraftBuilder
            .of(key, StateType.REVIEW_STATE)
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .transitions(new HashSet<>(references))
            .initial(true)
            .build();
    }
}

