package com.commercetools.sync.integration.sdk2.externalsource.states;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.StateITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
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
import com.commercetools.sync.sdk2.services.impl.StateServiceImpl;
import com.commercetools.sync.sdk2.states.StateSync;
import com.commercetools.sync.sdk2.states.StateSyncOptions;
import com.commercetools.sync.sdk2.states.StateSyncOptionsBuilder;
import com.commercetools.sync.sdk2.states.helpers.StateReferenceResolver;
import com.commercetools.sync.sdk2.states.helpers.StateSyncStatistics;
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private String key = "";

  @AfterAll
  static void tearDown() {
    deleteStatesFromTarget();
  }

  @BeforeEach
  void setup() {
    key = "state-" + current().nextInt();
    keyA = "state-A-" + current().nextInt();
    keyB = "state-B-" + current().nextInt();

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
  void sync_withOldStateWithRoleAndNewStateWithoutRole_shouldRemoveRole() {
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

    final State fetchedState = getStateByKey(CTP_TARGET_CLIENT, keyA).get();

    assertThat(fetchedState.getRoles()).isEmpty();
  }

  @Test
  void sync_withOldStateWithoutRoleAndNewStateWithoutRole_shouldDoNothing() {
    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(keyA)
            .type(StateTypeEnum.REVIEW_STATE)
            .roles(List.of())
            .initial(true)
            .build();

    final StateDraft stateDraftTarget =
        StateDraftBuilder.of()
            .key(keyA)
            .type(StateTypeEnum.REVIEW_STATE)
            .roles(List.of())
            .initial(true)
            .build();
    createStateInTarget(stateDraftTarget);

    final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final StateSync stateSync = new StateSync(stateSyncOptions);

    // test
    final StateSyncStatistics stateSyncStatistics =
        stateSync.sync(List.of(stateDraft)).toCompletableFuture().join();

    assertThat(stateSyncStatistics).hasValues(1, 0, 0, 0, 0);
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
    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(this.key)
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
  void sync_WithStateWithEmptyTransition_ShouldAddErrorMessage() {
    final StateDraft stateCDraft =
        StateDraftBuilder.of()
            .key(keyB)
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
        .isEqualTo(format("StateDraft with key: '%s' has invalid state transitions", keyB));
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_WithStateWithEmptyTransitionShouldBeResolved_ShouldAddErrorMessage() {
    final StateDraft stateCDraft =
        StateDraftBuilder.of()
            .key(keyB)
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
                          keyB));
              return null;
            })
        .toCompletableFuture()
        .join();
  }

  private void createStateInTarget(final StateDraft draft) {
    CTP_TARGET_CLIENT.states().create(draft).execute().thenApply(ApiHttpResponse::getBody).join();
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
}
