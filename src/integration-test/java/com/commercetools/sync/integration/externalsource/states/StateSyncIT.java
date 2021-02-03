package com.commercetools.sync.integration.externalsource.states;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.deleteStates;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.deleteStatesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.getStateByKey;
import static com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_TRANSITION_CONTAINER_KEY;
import static com.commercetools.sync.states.utils.StateReferenceResolutionUtils.mapToStateDrafts;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.states.State.referenceOfId;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.ThreadLocalRandom.current;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.WaitingToBeResolvedTransitions;
import com.commercetools.sync.services.UnresolvedReferencesService;
import com.commercetools.sync.services.impl.StateServiceImpl;
import com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl;
import com.commercetools.sync.states.StateSync;
import com.commercetools.sync.states.StateSyncOptions;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import com.commercetools.sync.states.helpers.StateReferenceResolver;
import com.commercetools.sync.states.helpers.StateSyncStatistics;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateSyncIT {

  String keyA;
  String keyB;
  String keyC;

  List<String> errorCallBackMessages;
  List<String> warningCallBackMessages;
  List<Throwable> errorCallBackExceptions;
  String key = "";

  @AfterAll
  static void tearDown() {
    deleteStatesFromTargetAndSource();
  }

  @BeforeEach
  void setup() {
    key = "state-" + current().nextInt();
    keyA = "state-A-" + current().nextInt();
    keyB = "state-B-" + current().nextInt();
    keyC = "state-C-" + current().nextInt();

    final StateDraft stateDraft =
        StateDraftBuilder.of(key, StateType.LINE_ITEM_STATE)
            .name(LocalizedString.ofEnglish("state-name"))
            .description(LocalizedString.ofEnglish("state-desc"))
            .roles(Collections.singleton(StateRole.RETURN))
            .initial(false)
            .build();

    executeBlocking(CTP_TARGET_CLIENT.execute(StateCreateCommand.of(stateDraft)));
    errorCallBackMessages = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
  }

  @Test
  void sync_withNewState_shouldCreateState() {
    final StateDraft stateDraft =
        StateDraftBuilder.of("new-state", StateType.REVIEW_STATE)
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .build();

    final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(singletonList(stateDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 1, 0, 0, 0);
  }

  @Test
  void sync_withCreateStateException_shouldPrintMessage() {
    final StateDraft stateDraft =
        StateDraftBuilder.of(keyA, StateType.REVIEW_STATE)
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .build();

    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    final StateCreateCommand command = any(StateCreateCommand.class);
    when(spyClient.execute(command))
        .thenReturn(exceptionallyCompletedFuture(new BadRequestException("test error message")));

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(singletonList(stateDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 1, 0);
    Assertions.assertThat(errorCallBackExceptions).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages.get(0))
        .contains(format("Failed to create draft with key: '%s'", keyA));
  }

  @Test
  void sync_withNewStateWithoutRole_shouldRemoveRole() {
    final StateDraft stateDraft =
        StateDraftBuilder.of(keyA, StateType.REVIEW_STATE).roles(Collections.emptySet()).build();

    final StateDraft stateDraftTarget =
        StateDraftBuilder.of(keyA, StateType.REVIEW_STATE)
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .build();
    createStateInTarget(stateDraftTarget);

    final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(singletonList(stateDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 1, 0, 0);
    QueryExecutionUtils.queryAll(
            CTP_TARGET_CLIENT, StateQueryBuilder.of().plusPredicates(q -> q.key().is(keyA)).build())
        .thenAccept(
            resultStates -> {
              Assertions.assertThat(resultStates.size()).isEqualTo(1);
              Assertions.assertThat(resultStates.get(0).getRoles().isEmpty()).isTrue();
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void sync_withNewStateWithoutRole_shouldDoNothing() {
    final StateDraft stateDraft =
        StateDraftBuilder.of(keyA, StateType.REVIEW_STATE).roles(Collections.emptySet()).build();

    final StateDraft stateDraftTarget =
        StateDraftBuilder.of(keyA, StateType.REVIEW_STATE).roles(Collections.emptySet()).build();
    createStateInTarget(stateDraftTarget);

    final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(singletonList(stateDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 1, 0, 0);
    QueryExecutionUtils.queryAll(
            CTP_TARGET_CLIENT, StateQueryBuilder.of().plusPredicates(q -> q.key().is(keyA)).build())
        .thenAccept(
            resultStates -> {
              Assertions.assertThat(resultStates.size()).isEqualTo(1);
              Assertions.assertThat(resultStates.get(0).getRoles().isEmpty()).isTrue();
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void sync_withNewStateWithNewRole_shouldAddRole() {
    final StateDraft stateDraft =
        StateDraftBuilder.of(keyA, StateType.REVIEW_STATE)
            .roles(asSet(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .build();

    final StateDraft stateDraftTarget =
        StateDraftBuilder.of(keyA, StateType.REVIEW_STATE).roles(Collections.emptySet()).build();
    createStateInTarget(stateDraftTarget);

    final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(singletonList(stateDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 1, 0, 0);
    QueryExecutionUtils.queryAll(
            CTP_TARGET_CLIENT, StateQueryBuilder.of().plusPredicates(q -> q.key().is(keyA)).build())
        .thenAccept(
            resultStates -> {
              Assertions.assertThat(resultStates.size()).isEqualTo(1);
              Assertions.assertThat(resultStates.get(0).getRoles().size()).isEqualTo(1);
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void sync_withNewStateWihCreationException_shouldPrintErrorMessage() {
    final StateDraft stateDraft =
        StateDraftBuilder.of(keyA, StateType.REVIEW_STATE)
            .roles(asSet(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .build();

    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    final StateCreateCommand command = any(StateCreateCommand.class);
    when(spyClient.execute(command)).thenReturn(completedFuture(any(State.class)));
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(singletonList(stateDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 1, 0);
    Assertions.assertThat(errorCallBackExceptions).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages.get(0))
        .contains(format("Failed to process the StateDraft with key: '%s'", keyA));
    Assertions.assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_WithUpdatedState_ShouldUpdateState() {
    // preparation
    String key = this.key;
    final StateDraft stateDraft =
        StateDraftBuilder.of(key, StateType.REVIEW_STATE)
            .name(ofEnglish("state-name-updated"))
            .description(ofEnglish("state-desc-updated"))
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .initial(true)
            .build();

    final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(singletonList(stateDraft)).toCompletableFuture().join();

    // assertion
    assertThat(stateSyncStatistics).hasValues(1, 0, 1, 0, 0);

    final Optional<State> oldStateAfter = getStateByKey(CTP_TARGET_CLIENT, key);

    Assertions.assertThat(oldStateAfter)
        .hasValueSatisfying(
            state -> {
              Assertions.assertThat(state.getType()).isEqualTo(StateType.REVIEW_STATE);
              Assertions.assertThat(state.getName()).isEqualTo(ofEnglish("state-name-updated"));
              Assertions.assertThat(state.getDescription())
                  .isEqualTo(ofEnglish("state-desc-updated"));
              Assertions.assertThat(state.getRoles())
                  .isEqualTo(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS));
              Assertions.assertThat(state.isInitial()).isEqualTo(true);
            });
    deleteStates(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_withEqualState_shouldNotUpdateState() {
    StateDraft stateDraft =
        StateDraftBuilder.of(key, StateType.LINE_ITEM_STATE)
            .name(ofEnglish("state-name"))
            .description(ofEnglish("state-desc"))
            .roles(Collections.singleton(StateRole.RETURN))
            .initial(false)
            .build();

    final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(singletonList(stateDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 0, 0);
  }

  @Test
  void sync_withChangedStateButConcurrentModificationException_shouldRetryAndUpdateState() {
    // preparation
    final SphereClient spyClient = buildClientWithConcurrentModificationUpdate();

    List<String> errorCallBackMessages = new ArrayList<>();
    List<String> warningCallBackMessages = new ArrayList<>();
    List<Throwable> errorCallBackExceptions = new ArrayList<>();
    final StateSyncOptions spyOptions = StateSyncOptionsBuilder.of(spyClient).build();

    final StateSync stateSync = new StateSync(spyOptions);

    final StateDraft stateDraft =
        StateDraftBuilder.of(key, StateType.REVIEW_STATE)
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
        .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()))
        .thenCallRealMethod();

    return spyClient;
  }

  @Test
  void sync_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
    // preparation
    final SphereClient spyClient =
        buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry();

    final StateSyncOptions spyOptions =
        StateSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, newResource, oldResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();

    final StateSync stateSync = new StateSync(spyOptions);

    final StateDraft stateDraft =
        StateDraftBuilder.of(key, StateType.REVIEW_STATE)
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

    Assertions.assertThat(errorCallBackExceptions.get(0).getCause())
        .isExactlyInstanceOf(BadGatewayException.class);
    Assertions.assertThat(errorCallBackMessages.get(0))
        .contains(
            format(
                "Failed to update state with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                    + "after concurrency modification.",
                stateDraft.getKey()));
  }

  @Nonnull
  private SphereClient buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry() {
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

    final StateUpdateCommand updateCommand = any(StateUpdateCommand.class);
    when(spyClient.execute(updateCommand))
        .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()))
        .thenCallRealMethod();

    final StateQuery stateQuery = any(StateQuery.class);
    when(spyClient.execute(stateQuery))
        .thenCallRealMethod() // Call real fetch on fetching matching states
        .thenReturn(exceptionallyCompletedFuture(new BadGatewayException()));

    return spyClient;
  }

  @Test
  void sync_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
    // preparation
    final SphereClient spyClient =
        buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry();

    List<String> errorCallBackMessages = new ArrayList<>();
    List<String> warningCallBackMessages = new ArrayList<>();
    List<Throwable> errorCallBackExceptions = new ArrayList<>();
    final StateSyncOptions spyOptions =
        StateSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, newResource, oldResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();

    final StateSync stateSync = new StateSync(spyOptions);

    final StateDraft stateDraft =
        StateDraftBuilder.of(key, StateType.REVIEW_STATE)
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

    Assertions.assertThat(errorCallBackMessages.get(0))
        .contains(
            format(
                "Failed to update state with key: '%s'. Reason: Not found when attempting to fetch while"
                    + " retrying after concurrency modification.",
                stateDraft.getKey()));
  }

  @Nonnull
  private SphereClient buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry() {
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

    final StateUpdateCommand stateUpdateCommand = any(StateUpdateCommand.class);
    when(spyClient.execute(stateUpdateCommand))
        .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()))
        .thenCallRealMethod();

    final StateQuery stateQuery = any(StateQuery.class);

    when(spyClient.execute(stateQuery))
        .thenCallRealMethod() // Call real fetch on fetching matching states
        .thenReturn(completedFuture(PagedQueryResult.empty()));

    return spyClient;
  }

  @Test
  void sync_WithSeveralBatches_ShouldReturnProperStatistics() {
    // 2 batches
    final List<StateDraft> stateDrafts =
        IntStream.range(0, 10)
            .mapToObj(
                i ->
                    StateDraft.of("key" + i, StateType.REVIEW_STATE)
                        .withName(ofEnglish("name" + i)))
            .collect(Collectors.toList());

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).batchSize(5).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(10, 10, 0, 0, 0);
  }

  @Test
  void sync_WithNotExistentStates_ShouldResolveStateLater() {
    final StateDraft stateCDraft = createStateDraft(keyC);
    final State stateC = createStateInSource(stateCDraft);

    final StateDraft stateBDraft = createStateDraft(keyB, stateC);
    final State stateB = createStateInSource(stateBDraft);

    StateDraft[] draftsWithReplacesKeys =
        mapToStateDrafts(asList(stateB, stateC)).toArray(new StateDraft[2]);
    final StateDraft stateADraft =
        createStateDraftReferencingStateDrafts(keyA, draftsWithReplacesKeys);
    final List<StateDraft> stateDrafts = asList(stateADraft);

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).batchSize(1).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 0, 1);

    final UnresolvedReferencesService<WaitingToBeResolvedTransitions> unresolvedTransitionsService =
        new UnresolvedReferencesServiceImpl<>(stateSyncOptions);

    Set<WaitingToBeResolvedTransitions> result =
        unresolvedTransitionsService
            .fetch(
                Collections.singleton(keyA),
                CUSTOM_OBJECT_TRANSITION_CONTAINER_KEY,
                WaitingToBeResolvedTransitions.class)
            .toCompletableFuture()
            .join();

    Assertions.assertThat(result.size()).isEqualTo(1);
    WaitingToBeResolvedTransitions waitingToBeResolvedTransitions = result.iterator().next();
    Assertions.assertThat(
            waitingToBeResolvedTransitions
                .getMissingTransitionStateKeys()
                .containsAll(asList(keyB, keyC)))
        .isTrue();
    Assertions.assertThat(waitingToBeResolvedTransitions.getStateDraft().getKey()).isEqualTo(keyA);
  }

  @Test
  void sync_WithAllExistentStates_ShouldResolveAllStates() {

    final StateDraft stateCDraft = createStateDraft(keyC);
    final State stateC = createStateInSource(stateCDraft);

    final StateDraft stateBDraft = createStateDraft(keyB, stateC);
    final State stateB = createStateInSource(stateBDraft);

    final StateDraft stateADraft = createStateDraft(keyA, stateB, stateC);
    final State stateA = createStateInSource(stateADraft);

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).batchSize(3).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);
    final List<StateDraft> stateDrafts = mapToStateDrafts(Arrays.asList(stateA, stateB, stateC));
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(3, 3, 0, 0, 0);
    Assertions.assertThat(stateSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 3 state(s) were processed in total "
                + "(3 created, 0 updated, 0 failed to sync and 0 state(s) with missing transition(s)).");
    final UnresolvedReferencesService<WaitingToBeResolvedTransitions> unresolvedTransitionsService =
        new UnresolvedReferencesServiceImpl<>(stateSyncOptions);

    Set<WaitingToBeResolvedTransitions> result =
        unresolvedTransitionsService
            .fetch(
                Collections.singleton(keyA),
                CUSTOM_OBJECT_TRANSITION_CONTAINER_KEY,
                WaitingToBeResolvedTransitions.class)
            .toCompletableFuture()
            .join();

    Assertions.assertThat(result.size()).isEqualTo(0);
  }

  @Test
  void sync_WithExceptionWhenFetchingUnresolvedTransition_ShouldPrintErrorMessage() {

    final StateDraft stateCDraft = createStateDraft(keyC);
    final State stateC = createStateInSource(stateCDraft);

    final StateDraft stateBDraft = createStateDraft(keyB, stateC);
    final State stateB = createStateInSource(stateBDraft);

    final StateDraft stateADraft = createStateDraft(keyA, stateB, stateC);
    final State stateA = createStateInSource(stateADraft);

    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

    when(spyClient.execute(any(CustomObjectQuery.class)))
        .thenReturn(exceptionallyCompletedFuture(new BadRequestException("a test exception")))
        .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()))
        .thenCallRealMethod();

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(spyClient)
            .batchSize(3)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, newResource, oldResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();

    final StateSync stateSync = new StateSync(stateSyncOptions);
    final List<StateDraft> stateDrafts = mapToStateDrafts(Arrays.asList(stateA, stateB, stateC));
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(3, 1, 0, 2, 1);
    Assertions.assertThat(errorCallBackExceptions).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages.get(0))
        .contains(format("Failed to fetch StateDrafts waiting to be resolved with keys"));
  }

  @Test
  void sync_WithListOfNullElements_ShouldPrintErrorMessage() {

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .batchSize(3)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, newResource, oldResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();

    final StateSync stateSync = new StateSync(stateSyncOptions);
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(Collections.singletonList(null)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 1, 0);
    Assertions.assertThat(errorCallBackExceptions).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages.get(0)).contains("StateDraft is null.");
  }

  @Test
  void sync_WithUpdatedTransition_ShouldUpdateTransitions() {

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
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).batchSize(3).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);
    final List<StateDraft> stateDrafts = mapToStateDrafts(Arrays.asList(stateA, stateB, stateC));
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(3, 0, 1, 0, 0);

    QueryExecutionUtils.queryAll(
            CTP_TARGET_CLIENT, StateQueryBuilder.of().plusPredicates(q -> q.key().is(keyA)).build())
        .thenAccept(
            resultStates -> {
              Assertions.assertThat(resultStates.size()).isEqualTo(1);
              Assertions.assertThat(resultStates.get(0).getTransitions().size()).isEqualTo(2);
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void sync_WithExceptionOnResolvingTransition_ShouldUpdateTransitions() {

    final StateDraft stateCDraft = createStateDraft(keyC);
    final State stateC = createStateInSource(stateCDraft);
    final StateDraft stateBDraft = createStateDraft(keyB, stateC);
    final State stateB = createStateInSource(stateBDraft);
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    when(spyClient.execute(any()))
        .thenCallRealMethod()
        .thenReturn(exceptionallyCompletedFuture(new BadGatewayException()));

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(spyClient)
            .batchSize(3)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, newResource, oldResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();

    final StateSync stateSync = new StateSync(stateSyncOptions);
    final List<StateDraft> stateDrafts = mapToStateDrafts(Arrays.asList(stateB, stateC));
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(2, 0, 0, 2, 0);
    Assertions.assertThat(errorCallBackExceptions).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages.get(0))
        .contains("Failed to fetch existing states with keys");
    Assertions.assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_WithDeletedTransition_ShouldRemoveTransitions() {

    final StateDraft stateCDraft = createStateDraft(keyC);
    final State stateC = createStateInSource(stateCDraft);
    final StateDraft tagetStateCDraft = createStateDraft(keyC);
    final State targetStateC = createStateInTarget(tagetStateCDraft);

    final StateDraft stateBDraft = createStateDraft(keyB, stateC);
    final State stateB = createStateInSource(stateBDraft);
    final StateDraft tagetStateBDraft = createStateDraft(keyB, targetStateC);
    final State targetStateB = createStateInTarget(tagetStateBDraft);

    final StateDraft stateADraft = createStateDraft(keyA, stateB);
    final State stateA = createStateInSource(stateADraft);
    final StateDraft tagetStateADraft = createStateDraft(keyA, targetStateB, targetStateC);
    final State targetStateA = createStateInTarget(tagetStateADraft);
    Assertions.assertThat(targetStateB.getTransitions().size()).isEqualTo(1);
    Assertions.assertThat(targetStateA.getTransitions().size()).isEqualTo(2);
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).batchSize(3).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);
    final List<StateDraft> stateDrafts = mapToStateDrafts(Arrays.asList(stateA, stateB, stateC));
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(3, 0, 1, 0, 0);

    QueryExecutionUtils.queryAll(
            CTP_TARGET_CLIENT, StateQueryBuilder.of().plusPredicates(q -> q.key().is(keyA)).build())
        .thenAccept(
            resultStates -> {
              Assertions.assertThat(resultStates.size()).isEqualTo(1);
              Assertions.assertThat(resultStates.get(0).getTransitions().size()).isEqualTo(1);
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void sync_WithEmptyNewTransition_ShouldRemoveTransitions() {

    final StateDraft stateCDraft = createStateDraft(keyC);
    final State stateC = createStateInSource(stateCDraft);
    final StateDraft tagetStateCDraft = createStateDraft(keyC);
    final State targetStateC = createStateInTarget(tagetStateCDraft);

    final StateDraft stateBDraft = createStateDraft(keyB, stateC);
    final State stateB = createStateInSource(stateBDraft);
    final StateDraft tagetStateBDraft = createStateDraft(keyB, targetStateC);
    final State targetStateB = createStateInTarget(tagetStateBDraft);

    final StateDraft stateADraft = createStateDraft(keyA);
    final State stateA = createStateInSource(stateADraft);
    final StateDraft tagetStateADraft = createStateDraft(keyA, targetStateB, targetStateC);
    final State targetStateA = createStateInTarget(tagetStateADraft);
    Assertions.assertThat(targetStateB.getTransitions().size()).isEqualTo(1);
    Assertions.assertThat(targetStateA.getTransitions().size()).isEqualTo(2);
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).batchSize(3).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);
    final List<StateDraft> stateDrafts = mapToStateDrafts(Arrays.asList(stateA, stateB, stateC));
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(3, 0, 1, 0, 0);

    QueryExecutionUtils.queryAll(
            CTP_TARGET_CLIENT, StateQueryBuilder.of().plusPredicates(q -> q.key().is(keyA)).build())
        .thenAccept(
            resultStates -> {
              Assertions.assertThat(resultStates.size()).isEqualTo(1);
              Assertions.assertThat(resultStates.get(0).getTransitions()).isNull();
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void sync_WithStateWithoutKey_ShouldAddErrorMessage() {
    String nameA = "state-A";
    final StateDraft stateADraft =
        StateDraftBuilder.of(null, StateType.REVIEW_STATE)
            .name(LocalizedString.ofEnglish(nameA))
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .initial(true)
            .build();

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .batchSize(3)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, newResource, oldResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();
    final StateSync stateSync = new StateSync(stateSyncOptions);
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(Arrays.asList(stateADraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 1, 0);
    Assertions.assertThat(errorCallBackExceptions).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages.get(0))
        .contains("StateDraft with name:" + " LocalizedString(en -> state-A) doesn't have a key.");
    Assertions.assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_WithUpdatedTransitionAndClientThrowsError_ShouldAddErrorMessage() {
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

    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

    final StateUpdateCommand updateCommand = any(StateUpdateCommand.class);
    when(spyClient.execute(updateCommand))
        .thenReturn(exceptionallyCompletedFuture(new BadRequestException("a test exception")))
        .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()))
        .thenCallRealMethod();

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(spyClient)
            .batchSize(3)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, newResource, oldResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();
    final StateSync stateSync = new StateSync(stateSyncOptions);
    final List<StateDraft> stateDrafts = mapToStateDrafts(Arrays.asList(stateA, stateB, stateC));
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(3, 0, 0, 1, 0);
    Assertions.assertThat(errorCallBackExceptions).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages.get(0))
        .contains(" detailMessage: a test exception");
    Assertions.assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_WithoutAnyNewStateDraft_ShouldProcessNothing() {

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).batchSize(3).build();
    final StateSync stateSync = new StateSync(stateSyncOptions);
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(Collections.emptyList()).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(0, 0, 0, 0, 0);
  }

  @Test
  void sync_WithFailureInKeysToIdCreation_ShouldAddErrorMessage() {
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

    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    when(spyClient.execute(any(ResourceKeyIdGraphQlRequest.class)))
        .thenReturn(exceptionallyCompletedFuture(new BadRequestException("a test exception")))
        .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()))
        .thenCallRealMethod();

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(spyClient)
            .batchSize(3)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, newResource, oldResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();

    final List<StateDraft> stateDrafts = mapToStateDrafts(Arrays.asList(stateA, stateB, stateC));
    // test
    final StateSyncStatistics stateSyncStatistics =
        new StateSync(stateSyncOptions).sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(3, 0, 0, 3, 0);
    Assertions.assertThat(errorCallBackExceptions).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages.get(0))
        .isEqualTo("Failed to build a cache of keys to ids.");
    Assertions.assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_WithStateWithEmptyTransition_ShouldAddErrorMessage() {
    final StateDraft stateCDraft =
        StateDraftBuilder.of(keyC, StateType.REVIEW_STATE)
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .transitions(new HashSet<>(Arrays.asList(Reference.of(State.referenceTypeId(), ""))))
            .initial(true)
            .build();
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .batchSize(3)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, newResource, oldResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();
    // test
    final StateSyncStatistics stateSyncStatistics =
        new StateSync(stateSyncOptions)
            .sync(Arrays.asList(stateCDraft))
            .toCompletableFuture()
            .join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 1, 0);
    Assertions.assertThat(errorCallBackExceptions).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages).isNotEmpty();
    Assertions.assertThat(errorCallBackMessages.get(0))
        .isEqualTo(format("StateDraft with key: '%s' has invalid state transitions", keyC));
    Assertions.assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_WithStateWithEmptyTransitionShouldBeResolved_ShouldAddErrorMessage() {
    final StateDraft stateCDraft =
        StateDraftBuilder.of(keyC, StateType.REVIEW_STATE)
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .transitions(new HashSet<>(Arrays.asList(Reference.of(State.referenceTypeId(), ""))))
            .initial(true)
            .build();
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .batchSize(3)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, newResource, oldResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();
    // test
    StateServiceImpl stateService = new StateServiceImpl(stateSyncOptions);
    final StateReferenceResolver stateReferenceResolver =
        new StateReferenceResolver(stateSyncOptions, stateService);

    CompletionStage<StateDraft> result = stateReferenceResolver.resolveReferences(stateCDraft);
    result
        .exceptionally(
            exception -> {
              Assertions.assertThat(exception.getMessage())
                  .contains(
                      format(
                          "Failed to resolve 'transition' reference on StateDraft with key:'%s",
                          keyC));
              return null;
            })
        .toCompletableFuture()
        .join();
  }

  private State createStateInSource(final StateDraft draft) {
    return executeBlocking(
        CTP_SOURCE_CLIENT.execute(
            StateCreateCommand.of(draft).withExpansionPaths(StateExpansionModel::transitions)));
  }

  private State createStateInTarget(final StateDraft draft) {
    return executeBlocking(
        CTP_TARGET_CLIENT.execute(
            StateCreateCommand.of(draft).withExpansionPaths(StateExpansionModel::transitions)));
  }

  private StateDraft createStateDraftReferencingStateDrafts(
      final String key, final StateDraft... transitionStatesDraft) {
    List<Reference<State>> references = new ArrayList<>();
    if (transitionStatesDraft.length > 0) {
      for (StateDraft transitionState : transitionStatesDraft) {
        references.add(referenceOfId(transitionState.getKey()));
      }
    }
    return createStateDraftWithReference(key, references);
  }

  private StateDraft createStateDraft(final String key, final State... transitionStates) {
    List<Reference<State>> references = new ArrayList<>();
    if (transitionStates.length > 0) {
      for (State transitionState : transitionStates) {
        references.add(referenceOfId(transitionState.getId()));
      }
    }
    return createStateDraftWithReference(key, references);
  }

  private StateDraft createStateDraftWithReference(
      final String key, final List<Reference<State>> references) {
    return StateDraftBuilder.of(key, StateType.REVIEW_STATE)
        .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
        .transitions(new HashSet<>(references))
        .initial(true)
        .build();
  }
}
