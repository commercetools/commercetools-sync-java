package com.commercetools.sync.integration.sdk2.externalsource.states;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.StateITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.sdk2.services.impl.UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_TRANSITION_CONTAINER_KEY;
import static java.lang.String.format;
import static java.util.concurrent.ThreadLocalRandom.current;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.state.StateResourceIdentifierBuilder;
import com.commercetools.api.models.state.StateRoleEnum;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.sync.sdk2.commons.models.WaitingToBeResolvedTransitions;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.services.UnresolvedReferencesService;
import com.commercetools.sync.sdk2.services.impl.StateServiceImpl;
import com.commercetools.sync.sdk2.services.impl.UnresolvedReferencesServiceImpl;
import com.commercetools.sync.sdk2.states.StateSync;
import com.commercetools.sync.sdk2.states.StateSyncOptions;
import com.commercetools.sync.sdk2.states.StateSyncOptionsBuilder;
import com.commercetools.sync.sdk2.states.helpers.StateReferenceResolver;
import com.commercetools.sync.sdk2.states.helpers.StateSyncStatistics;
import com.commercetools.sync.sdk2.states.utils.StateTransformUtils;
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateSyncIT {

  private String keyA;
  private String keyB;
  private String keyC;

  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private String key = "";
  private ReferenceIdToKeyCache referenceIdToKeyCache;

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
        StateDraftBuilder.of()
            .key(key)
            .type(StateTypeEnum.LINE_ITEM_STATE)
            .name(ofEnglish("state-name"))
            .description(ofEnglish("state-desc"))
            .roles(List.of(StateRoleEnum.RETURN))
            .initial(false)
            .build();

    CTP_TARGET_CLIENT.states().create(stateDraft).executeBlocking();
    errorCallBackMessages = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
  }

  @Test
  void sync_withNewState_shouldCreateState() {
    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key("new-state")
            .type(StateTypeEnum.REVIEW_STATE)
            .roles(List.of(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS))
            .build();

    final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(List.of(stateDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 1, 0, 0, 0);
  }

  @Test
  void sync_withCreateStateException_shouldPrintMessage() {
    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(keyA)
            .type(StateTypeEnum.REVIEW_STATE)
            .roles(List.of(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS))
            .build();

    final ProjectApiRoot client = buildClientWithBadGatewayExceptionOnPost();

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(client)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(List.of(stateDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 1, 0);
    assertThat(errorCallBackExceptions).isNotEmpty();
    assertThat(errorCallBackMessages).isNotEmpty();
    assertThat(errorCallBackMessages.get(0))
        .contains(format("Failed to create draft with key: '%s'", keyA));
  }

  @Test
  void sync_withNewStateWithoutRole_shouldRemoveRole() {
    final StateDraft stateDraft =
        StateDraftBuilder.of().key(keyA).type(StateTypeEnum.REVIEW_STATE).roles(List.of()).build();

    final StateDraft stateDraftTarget =
        StateDraftBuilder.of()
            .key(keyA)
            .type(StateTypeEnum.REVIEW_STATE)
            .roles(List.of(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS))
            .build();
    createStateInTarget(stateDraftTarget);

    final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(List.of(stateDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 1, 0, 0);

    final State fetchedState =
        CTP_TARGET_CLIENT
            .states()
            .withKey(keyA)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    assertThat(fetchedState.getRoles()).isEmpty();
  }

  @Test
  void sync_withNewStateWithoutRole_shouldDoNothing() {
    final StateDraft stateDraft =
        StateDraftBuilder.of().key(keyA).type(StateTypeEnum.REVIEW_STATE).roles(List.of()).build();

    final StateDraft stateDraftTarget =
        StateDraftBuilder.of().key(keyA).type(StateTypeEnum.REVIEW_STATE).roles(List.of()).build();
    createStateInTarget(stateDraftTarget);

    final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(List.of(stateDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 1, 0, 0);

    final State fetchedState = getStateByKey(CTP_TARGET_CLIENT, keyA).get();
    assertThat(fetchedState.getRoles()).isEmpty();
  }

  @Test
  void sync_withNewStateWithNewRole_shouldAddRole() {
    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(keyA)
            .type(StateTypeEnum.REVIEW_STATE)
            .roles(List.of(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS))
            .build();

    final StateDraft stateDraftTarget =
        StateDraftBuilder.of().key(keyA).type(StateTypeEnum.REVIEW_STATE).roles(List.of()).build();
    createStateInTarget(stateDraftTarget);

    final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(List.of(stateDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 1, 0, 0);

    final State fetchedState = getStateByKey(CTP_TARGET_CLIENT, keyA).get();

    assertThat(fetchedState.getRoles()).hasSize(1);
  }

  @Test
  void sync_withNewStateWihCreationException_shouldPrintErrorMessage() {
    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(keyA)
            .type(StateTypeEnum.REVIEW_STATE)
            .roles(List.of(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS))
            .build();

    final ProjectApiRoot spyClient = buildClientWithInvalidResponseReturned();
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
        stateSync.sync(List.of(stateDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 1, 0);
    assertThat(errorCallBackExceptions).isNotEmpty();
    assertThat(errorCallBackMessages).isNotEmpty();
    assertThat(errorCallBackMessages.get(0))
        .contains(format("Failed to process the StateDraft with key: '%s'", keyA));
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_WithUpdatedState_ShouldUpdateState() {
    // preparation
    String key = this.key;
    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(key)
            .type(StateTypeEnum.REVIEW_STATE)
            .name(ofEnglish("state-name-updated"))
            .description(ofEnglish("state-desc-updated"))
            .roles(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS)
            .initial(true)
            .build();

    final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(List.of(stateDraft)).toCompletableFuture().join();

    // assertion
    assertThat(stateSyncStatistics).hasValues(1, 0, 1, 0, 0);

    final Optional<State> oldStateAfter = getStateByKey(CTP_TARGET_CLIENT, key);

    assertThat(oldStateAfter)
        .hasValueSatisfying(
            state -> {
              assertThat(state.getType()).isEqualTo(StateTypeEnum.REVIEW_STATE);
              assertThat(state.getName()).isEqualTo(ofEnglish("state-name-updated"));
              assertThat(state.getDescription()).isEqualTo(ofEnglish("state-desc-updated"));
              assertThat(state.getRoles())
                  .isEqualTo(List.of(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS));
              assertThat(state.getInitial()).isEqualTo(true);
            });
    deleteStates(CTP_TARGET_CLIENT, null);
  }

  @Test
  void sync_withEqualState_shouldNotUpdateState() {
    StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(key)
            .type(StateTypeEnum.LINE_ITEM_STATE)
            .name(ofEnglish("state-name"))
            .description(ofEnglish("state-desc"))
            .roles(StateRoleEnum.RETURN)
            .initial(false)
            .build();

    final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(List.of(stateDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 0, 0);
  }

  @Test
  void sync_withChangedStateButConcurrentModificationException_shouldRetryAndUpdateState() {
    // preparation
    final ProjectApiRoot spyClient = buildClientWithConcurrentModificationUpdate();

    List<String> errorCallBackMessages = new ArrayList<>();
    List<String> warningCallBackMessages = new ArrayList<>();
    List<Throwable> errorCallBackExceptions = new ArrayList<>();
    final StateSyncOptions spyOptions = StateSyncOptionsBuilder.of(spyClient).build();

    final StateSync stateSync = new StateSync(spyOptions);

    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(key)
            .type(StateTypeEnum.REVIEW_STATE)
            .name(ofEnglish("state-name-updated"))
            .description(ofEnglish("state-desc-updated"))
            .roles(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS)
            .initial(true)
            .build();

    final StateSyncStatistics syncStatistics =
        stateSync.sync(List.of(stateDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
    // preparation
    final ProjectApiRoot spyClient =
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
        StateDraftBuilder.of()
            .key(key)
            .type(StateTypeEnum.REVIEW_STATE)
            .name(ofEnglish("state-name-updated"))
            .description(ofEnglish("state-desc-updated"))
            .roles(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS)
            .initial(true)
            .build();

    final StateSyncStatistics syncStatistics =
        stateSync.sync(List.of(stateDraft)).toCompletableFuture().join();

    // Test and assertion
    assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackExceptions).hasSize(1);

    assertThat(errorCallBackExceptions.get(0).getCause())
        .isExactlyInstanceOf(BadGatewayException.class);
    assertThat(errorCallBackMessages.get(0))
        .contains(
            format(
                "Failed to update state with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                    + "after concurrency modification.",
                stateDraft.getKey()));
  }

  @Test
  void sync_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
    // preparation
    final ProjectApiRoot spyClient =
        buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry();

    List<String> errorCallBackMessages = new ArrayList<>();
    List<Throwable> errorCallBackExceptions = new ArrayList<>();
    final StateSyncOptions spyOptions =
        StateSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final StateSync stateSync = new StateSync(spyOptions);

    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(key)
            .type(StateTypeEnum.REVIEW_STATE)
            .name(ofEnglish("state-name-updated"))
            .description(ofEnglish("state-desc-updated"))
            .roles(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS)
            .initial(true)
            .build();

    final StateSyncStatistics syncStatistics =
        stateSync.sync(List.of(stateDraft)).toCompletableFuture().join();

    // Test and assertion
    assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackExceptions).hasSize(1);

    assertThat(errorCallBackMessages.get(0))
        .contains(
            format(
                "Failed to update state with key: '%s'. Reason: Not found when attempting to fetch while"
                    + " retrying after concurrency modification.",
                stateDraft.getKey()));
  }

  @Test
  void sync_WithSeveralBatches_ShouldReturnProperStatistics() {
    // 2 batches
    final List<StateDraft> stateDrafts =
        IntStream.range(0, 10)
            .mapToObj(
                i ->
                    StateDraftBuilder.of()
                        .key("key" + i)
                        .type(StateTypeEnum.REVIEW_STATE)
                        .name(ofEnglish("name" + i))
                        .build())
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

    final StateDraft[] draftsWithReplacesKeys =
        StateTransformUtils.toStateDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, List.of(stateB, stateC))
            .join()
            .toArray(new StateDraft[2]);
    final StateDraft stateADraft =
        createStateDraftReferencingStateDrafts(keyA, draftsWithReplacesKeys);
    final List<StateDraft> stateDrafts = List.of(stateADraft);

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).batchSize(1).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 0, 1);

    final UnresolvedReferencesService<WaitingToBeResolvedTransitions> unresolvedTransitionsService =
        new UnresolvedReferencesServiceImpl<>(stateSyncOptions);

    final Set<WaitingToBeResolvedTransitions> result =
        unresolvedTransitionsService
            .fetch(
                Set.of(keyA),
                CUSTOM_OBJECT_TRANSITION_CONTAINER_KEY,
                WaitingToBeResolvedTransitions.class)
            .toCompletableFuture()
            .join();

    assertThat(result.size()).isEqualTo(1);
    WaitingToBeResolvedTransitions waitingToBeResolvedTransitions = result.iterator().next();
    assertThat(
            waitingToBeResolvedTransitions
                .getMissingTransitionStateKeys()
                .containsAll(List.of(keyB, keyC)))
        .isTrue();
    assertThat(waitingToBeResolvedTransitions.getStateDraft().getKey()).isEqualTo(keyA);
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
    final List<StateDraft> stateDrafts =
        StateTransformUtils.toStateDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, List.of(stateA, stateB, stateC))
            .join();
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(3, 3, 0, 0, 0);
    assertThat(stateSyncStatistics.getReportMessage())
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

    assertThat(result.size()).isEqualTo(0);
  }

  @Test
  void sync_WithExceptionWhenFetchingUnresolvedTransition_ShouldPrintErrorMessage() {

    final StateDraft stateCDraft = createStateDraft(keyC);
    final State stateC = createStateInSource(stateCDraft);

    final StateDraft stateBDraft = createStateDraft(keyB, stateC);
    final State stateB = createStateInSource(stateBDraft);

    final StateDraft stateADraft = createStateDraft(keyA, stateB, stateC);
    final State stateA = createStateInSource(stateADraft);

    final ProjectApiRoot client = buildClientWith2ExceptionOnCustomObjects();

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(client)
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
    final List<StateDraft> stateDrafts =
        StateTransformUtils.toStateDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, List.of(stateA, stateB, stateC))
            .join();
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(3, 1, 0, 2, 1);
    assertThat(errorCallBackExceptions).isNotEmpty();
    assertThat(errorCallBackMessages).isNotEmpty();
    assertThat(errorCallBackMessages.get(0))
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
    assertThat(errorCallBackExceptions).isNotEmpty();
    assertThat(errorCallBackMessages).isNotEmpty();
    assertThat(errorCallBackMessages.get(0)).contains("StateDraft is null.");
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

    assertThat(targetStateB.getTransitions().size()).isEqualTo(1);
    assertThat(targetStateA.getTransitions().size()).isEqualTo(1);
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).batchSize(3).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);
    final List<StateDraft> stateDrafts =
        StateTransformUtils.toStateDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, List.of(stateA, stateB, stateC))
            .join();
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(3, 0, 1, 0, 0);

    final State fetchedState = getStateByKey(CTP_TARGET_CLIENT, keyA).get();
    assertThat(fetchedState.getTransitions().size()).isEqualTo(2);
  }

  @Test
  void sync_WithExceptionOnResolvingTransition_ShouldUpdateTransitions() {

    final StateDraft stateCDraft = createStateDraft(keyC);
    final State stateC = createStateInSource(stateCDraft);
    final StateDraft stateBDraft = createStateDraft(keyB, stateC);
    final State stateB = createStateInSource(stateBDraft);
    final ProjectApiRoot spyClient = buildClientWithBadGatewayException();

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
    final List<StateDraft> stateDrafts =
        StateTransformUtils.toStateDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, List.of(stateB, stateC))
            .join();
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(2, 0, 0, 2, 0);
    assertThat(errorCallBackExceptions).isNotEmpty();
    assertThat(errorCallBackMessages).isNotEmpty();
    assertThat(errorCallBackMessages.get(0)).contains("Failed to fetch existing states with keys");
    assertThat(warningCallBackMessages).isEmpty();
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

    assertThat(targetStateB.getTransitions().size()).isEqualTo(1);
    assertThat(targetStateA.getTransitions().size()).isEqualTo(2);
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).batchSize(3).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);
    final List<StateDraft> stateDrafts =
        StateTransformUtils.toStateDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, List.of(stateA, stateB, stateC))
            .join();
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(3, 0, 1, 0, 0);

    final State fetchedState = getStateByKey(CTP_TARGET_CLIENT, keyA).get();
    assertThat(fetchedState.getTransitions().size()).isEqualTo(1);
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

    assertThat(targetStateB.getTransitions().size()).isEqualTo(1);
    assertThat(targetStateA.getTransitions().size()).isEqualTo(2);
    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).batchSize(3).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);
    final List<StateDraft> stateDrafts =
        StateTransformUtils.toStateDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, List.of(stateA, stateB, stateC))
            .join();
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(3, 0, 1, 0, 0);

    final State fetchedState = getStateByKey(CTP_TARGET_CLIENT, keyA).get();

    assertThat(fetchedState.getTransitions()).isNull();
  }

  @Test
  void sync_WithNullTransitions_TargetStateTransitionsShouldBeNull() {

    final StateDraft stateDraft = createStateDraft(keyA);
    final State state = createStateInSource(stateDraft);

    final StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).batchSize(3).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);
    final List<StateDraft> stateDrafts =
        StateTransformUtils.toStateDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, List.of(state))
            .join();
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 1, 0, 0, 0);

    final State fetchedState = getStateByKey(CTP_TARGET_CLIENT, keyA).get();
    assertThat(fetchedState.getTransitions()).isNull();
  }

  @Test
  void sync_WithStateWithoutKey_ShouldAddErrorMessage() {
    final String nameA = "state-A";
    final StateDraft stateADraft =
        StateDraftBuilder.of()
            .key("")
            .type(StateTypeEnum.REVIEW_STATE)
            .name(ofEnglish(nameA))
            .roles(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS)
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
        stateSync.sync(List.of(stateADraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 1, 0);
    assertThat(errorCallBackExceptions).isNotEmpty();
    assertThat(errorCallBackMessages).isNotEmpty();
    assertThat(errorCallBackMessages.get(0))
        .contains("StateDraft with name:" + " LocalizedString(en -> state-A) doesn't have a key.");
    assertThat(warningCallBackMessages).isEmpty();
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

    assertThat(targetStateB.getTransitions().size()).isEqualTo(1);
    assertThat(targetStateA.getTransitions().size()).isEqualTo(1);

    final ProjectApiRoot spyClient = buildClientWithBadGatewayAndConcurrentModificationUpdate();

    //    final StateUpdateCommand updateCommand = any(StateUpdateCommand.class);
    //    when(spyClient.execute(updateCommand))
    //        .thenReturn(exceptionallyCompletedFuture(new BadRequestException("a test exception")))
    //        .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()))
    //        .thenCallRealMethod();

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
    final List<StateDraft> stateDrafts =
        StateTransformUtils.toStateDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, List.of(stateA, stateB, stateC))
            .join();
    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(3, 0, 0, 1, 0);
    assertThat(errorCallBackExceptions).isNotEmpty();
    assertThat(errorCallBackMessages).isNotEmpty();
    assertThat(errorCallBackMessages.get(0)).contains("\"message\" : \"test\"");
    assertThat(warningCallBackMessages).isEmpty();
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
    assertThat(targetStateB.getTransitions().size()).isEqualTo(1);
    assertThat(targetStateA.getTransitions().size()).isEqualTo(1);

    final ProjectApiRoot spyClient = buildClientWithGraphQlBadGatewayAndConcurrentModification();

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

    final List<StateDraft> stateDrafts =
        StateTransformUtils.toStateDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, List.of(stateA, stateB, stateC))
            .join();
    // test
    final StateSyncStatistics stateSyncStatistics =
        new StateSync(stateSyncOptions).sync(stateDrafts).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(3, 0, 0, 3, 0);
    assertThat(errorCallBackExceptions).isNotEmpty();
    assertThat(errorCallBackMessages).isNotEmpty();
    assertThat(errorCallBackMessages.get(0)).isEqualTo("Failed to build a cache of keys to ids.");
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_WithStateWithEmptyTransition_ShouldAddErrorMessage() {
    final StateDraft stateCDraft =
        StateDraftBuilder.of()
            .key(keyC)
            .type(StateTypeEnum.REVIEW_STATE)
            .roles(List.of(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS))
            .transitions(StateResourceIdentifierBuilder.of().id("").build())
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
        new StateSync(stateSyncOptions).sync(List.of(stateCDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 1, 0);
    assertThat(errorCallBackExceptions).isNotEmpty();
    assertThat(errorCallBackMessages).isNotEmpty();
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(format("StateDraft with key: '%s' has invalid state transitions", keyC));
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_WithStateWithEmptyTransitionShouldBeResolved_ShouldAddErrorMessage() {
    final StateDraft stateCDraft =
        StateDraftBuilder.of()
            .key(keyC)
            .type(StateTypeEnum.REVIEW_STATE)
            .roles(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS)
            .transitions(StateResourceIdentifierBuilder.of().id("").build())
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
    final StateServiceImpl stateService = new StateServiceImpl(stateSyncOptions);
    final StateReferenceResolver stateReferenceResolver =
        new StateReferenceResolver(stateSyncOptions, stateService);

    final CompletionStage<StateDraft> result =
        stateReferenceResolver.resolveReferences(stateCDraft);
    result
        .exceptionally(
            exception -> {
              assertThat(exception.getMessage())
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
    return CTP_SOURCE_CLIENT
        .states()
        .create(draft)
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .join();
  }

  private State createStateInTarget(final StateDraft draft) {
    return CTP_TARGET_CLIENT
        .states()
        .create(draft)
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .join();
  }

  private StateDraft createStateDraftReferencingStateDrafts(
      final String key, final StateDraft... transitionStatesDraft) {
    final List<StateResourceIdentifier> references = new ArrayList<>();
    if (transitionStatesDraft.length > 0) {
      for (StateDraft transitionState : transitionStatesDraft) {
        references.add(StateResourceIdentifierBuilder.of().key(transitionState.getKey()).build());
      }
    }
    return createStateDraftWithReference(key, references);
  }

  private StateDraft createStateDraft(final String key, final State... transitionStates) {
    List<StateResourceIdentifier> references = null;
    if (transitionStates.length > 0) {
      references = new ArrayList<>();
      for (State transitionState : transitionStates) {
        references.add(StateResourceIdentifierBuilder.of().key(transitionState.getKey()).build());
      }
    }
    return createStateDraftWithReference(key, references);
  }

  private StateDraft createStateDraftWithReference(
      final String key, final List<StateResourceIdentifier> references) {
    return StateDraftBuilder.of()
        .key(key)
        .type(StateTypeEnum.REVIEW_STATE)
        .roles(List.of(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS))
        .transitions(references)
        .initial(true)
        .build();
  }

  private ProjectApiRoot buildClientWithBadGatewayExceptionOnPost() {
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("states") && method == ApiHttpMethod.POST) {
                    return CompletableFutureUtils.exceptionallyCompletedFuture(
                        createBadGatewayException());
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }

  private ProjectApiRoot buildClientWithBadGatewayException() {
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("states")) {
                    return CompletableFutureUtils.exceptionallyCompletedFuture(
                        createBadGatewayException());
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }

  private ProjectApiRoot buildClientWithBadGatewayAndConcurrentModificationUpdate() {
    final AtomicInteger requestCount = new AtomicInteger();
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("states/") && method == ApiHttpMethod.POST) {
                    if (requestCount.getAndIncrement() == 0) {
                      return CompletableFutureUtils.exceptionallyCompletedFuture(
                          createBadGatewayException());
                    } else if (requestCount.getAndIncrement() == 0) {
                      return CompletableFuture.failedFuture(
                          createConcurrentModificationException());
                    }
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }

  private ProjectApiRoot buildClientWithConcurrentModificationUpdate() {
    final AtomicInteger requestCount = new AtomicInteger();
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("states/")
                      && method == ApiHttpMethod.POST
                      && requestCount.getAndIncrement() == 0) {
                    return CompletableFutureUtils.exceptionallyCompletedFuture(
                        createConcurrentModificationException());
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }

  private ProjectApiRoot buildClientWithInvalidResponseReturned() {
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("states") && method == ApiHttpMethod.POST) {
                    return CompletableFuture.completedFuture(
                        new ApiHttpResponse<>(202, null, "{}".getBytes(StandardCharsets.UTF_8)));
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }

  private ProjectApiRoot buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry() {
    final AtomicInteger updateRequestCount = new AtomicInteger();
    final AtomicInteger getRequestCount = new AtomicInteger();
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("states/")
                      && method == ApiHttpMethod.POST
                      && updateRequestCount.getAndIncrement() == 0) {
                    return CompletableFutureUtils.exceptionallyCompletedFuture(
                        createConcurrentModificationException());
                  } else if (uri.contains("states")
                      && method == ApiHttpMethod.GET
                      && getRequestCount.getAndIncrement() == 1) {
                    return CompletableFutureUtils.exceptionallyCompletedFuture(
                        createBadGatewayException());
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }

  private ProjectApiRoot buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry() {
    final AtomicInteger updateRequestCount = new AtomicInteger();
    final AtomicInteger getRequestCount = new AtomicInteger();
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("states/")
                      && method == ApiHttpMethod.POST
                      && updateRequestCount.getAndIncrement() == 0) {
                    return CompletableFutureUtils.exceptionallyCompletedFuture(
                        createConcurrentModificationException());
                  } else if (uri.contains("states")
                      && method == ApiHttpMethod.GET
                      && getRequestCount.getAndIncrement() == 1) {
                    return CompletableFuture.failedFuture(createNotFoundException());
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }

  private ProjectApiRoot buildClientWith2ExceptionOnCustomObjects() {
    final AtomicInteger updateRequestCount = new AtomicInteger();
    final AtomicInteger getRequestCount = new AtomicInteger();
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  if (uri.contains("custom-objects/")
                      && updateRequestCount.getAndIncrement() == 0) {
                    if (getRequestCount.getAndIncrement() == 0) {
                      return CompletableFuture.failedFuture(createBadGatewayException());
                    } else if (getRequestCount.getAndIncrement() == 1) {
                      return CompletableFuture.failedFuture(
                          createConcurrentModificationException());
                    }
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }

  private ProjectApiRoot buildClientWithGraphQlBadGatewayAndConcurrentModification() {
    final AtomicInteger requestCount = new AtomicInteger();
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  if (uri.contains("graphql")) {
                    if (requestCount.getAndIncrement() == 0) {
                      return CompletableFuture.failedFuture(createBadGatewayException());
                    } else if (requestCount.getAndIncrement() == 1) {
                      return CompletableFuture.failedFuture(
                          createConcurrentModificationException());
                    }
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }
}
