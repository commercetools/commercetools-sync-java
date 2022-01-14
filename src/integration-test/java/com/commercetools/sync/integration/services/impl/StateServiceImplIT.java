package com.commercetools.sync.integration.services.impl;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.STATE_DESCRIPTION_1;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.STATE_KEY_1;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.STATE_NAME_1;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.clearTransitions;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.createState;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.deleteStates;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.impl.StateServiceImpl;
import com.commercetools.sync.states.StateSyncOptions;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.errors.DuplicateFieldError;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.updateactions.ChangeKey;
import io.sphere.sdk.states.commands.updateactions.ChangeType;
import io.sphere.sdk.states.commands.updateactions.SetName;
import io.sphere.sdk.states.queries.StateQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateServiceImplIT {
  private static final StateType STATE_TYPE = StateType.PRODUCT_STATE;
  private static final StateType TRANSITION_STATE_TYPE = StateType.REVIEW_STATE;
  private static final String OLD_STATE_KEY = "old_state_key";

  private List<String> errorCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  private State oldState;
  private StateService stateService;
  private ArrayList<String> warnings;

  /** Deletes states from the target CTP projects, then it populates the project with test data. */
  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();

    deleteStates(CTP_TARGET_CLIENT, Optional.of(STATE_TYPE));
    deleteStates(CTP_TARGET_CLIENT, Optional.of(TRANSITION_STATE_TYPE));
    warnings = new ArrayList<>();
    oldState = createState(CTP_TARGET_CLIENT, STATE_TYPE);

    final StateSyncOptions StateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .warningCallback(
                (exception, oldResource, newResource) -> warnings.add(exception.getMessage()))
            .build();
    stateService = new StateServiceImpl(StateSyncOptions);
  }

  /** Cleans up the target test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    deleteStates(CTP_TARGET_CLIENT, Optional.of(STATE_TYPE));
    deleteStates(CTP_TARGET_CLIENT, Optional.of(TRANSITION_STATE_TYPE));
  }

  @Test
  void fetchCachedStateId_WithNonExistingState_ShouldNotFetchAState() {
    final Optional<String> stateId =
        stateService.fetchCachedStateId("non-existing-state-key").toCompletableFuture().join();
    assertThat(stateId).isEmpty();
    assertThat(warnings).isEmpty();
  }

  @Test
  void fetchCachedStateId_WithExistingState_ShouldFetchStateAndCache() {
    final Optional<String> stateId =
        stateService.fetchCachedStateId(oldState.getKey()).toCompletableFuture().join();
    assertThat(stateId).isNotEmpty();
    assertThat(warnings).isEmpty();
  }

  @Test
  void fetchCachedStateId_WithNullKey_ShouldReturnFutureWithEmptyOptional() {
    final Optional<String> stateId =
        stateService.fetchCachedStateId(null).toCompletableFuture().join();

    assertThat(stateId).isEmpty();
    assertThat(warnings).isEmpty();
  }

  @Test
  void fetchMatchingStatesByKeys_WithEmptySetOfKeys_ShouldReturnEmptySet() {
    final Set<String> stateKeys = new HashSet<>();
    final Set<State> matchingStates =
        stateService.fetchMatchingStatesByKeys(stateKeys).toCompletableFuture().join();

    assertThat(matchingStates).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingStatesByKeys_WithNonExistingKeys_ShouldReturnEmptySet() {
    final Set<String> stateKeys = new HashSet<>();
    stateKeys.add("state_key_1");
    stateKeys.add("state_key_2");

    final Set<State> matchingStates =
        stateService.fetchMatchingStatesByKeys(stateKeys).toCompletableFuture().join();

    assertThat(matchingStates).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingStatesByKeys_WithAnyExistingKeys_ShouldReturnASetOfStates() {
    final String key = "state_key_X";
    final State transitionState =
        createState(CTP_TARGET_CLIENT, "state_transition_key_1", TRANSITION_STATE_TYPE, null);
    final State fetchState =
        createState(CTP_TARGET_CLIENT, key, TRANSITION_STATE_TYPE, transitionState);

    final Set<String> stateKeys = new HashSet<>();
    stateKeys.add(key);

    final Set<State> matchingStates =
        stateService.fetchMatchingStatesByKeys(stateKeys).toCompletableFuture().join();

    assertThat(matchingStates).hasSize(1);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();

    State matchedState = matchingStates.iterator().next();
    assertThat(matchedState.getId()).isEqualTo(fetchState.getId());
    assertThat(matchedState.getTransitions()).hasSize(1);
    Reference<State> transition = matchedState.getTransitions().iterator().next();
    assertThat(transition.getId()).isEqualTo(transitionState.getId());
    assertThat(transition.getObj()).isNull();

    clearTransitions(CTP_TARGET_CLIENT, fetchState);
    deleteStates(CTP_TARGET_CLIENT, Optional.of(TRANSITION_STATE_TYPE));
  }

  @Test
  void fetchMatchingStatesByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
    // Mock sphere client to return BadGatewayException on any request.
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    when(spyClient.execute(any(StateQuery.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
        .thenCallRealMethod();

    final StateSyncOptions spyOptions =
        StateSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final StateService spyStateService = new StateServiceImpl(spyOptions);

    final Set<String> keys = new HashSet<>();
    keys.add(OLD_STATE_KEY);

    // test and assert
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(spyStateService.fetchMatchingStatesByKeys(keys))
        .failsWithin(10, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  void fetchMatchingStatesByKeys_WithAllExistingSetOfKeys_ShouldCacheFetchedStateIds() {
    final Set<State> fetchedStates =
        stateService
            .fetchMatchingStatesByKeys(singleton(OLD_STATE_KEY))
            .toCompletableFuture()
            .join();
    assertThat(fetchedStates).hasSize(1);

    final Optional<State> stateOptional =
        CTP_TARGET_CLIENT
            .execute(
                StateQuery.of().withPredicates(queryModel -> queryModel.key().is(OLD_STATE_KEY)))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(stateOptional).isNotNull();

    // Change state old_state_key on ctp
    final String newKey = "new_state_key";
    stateService
        .updateState(stateOptional.get(), Collections.singletonList(ChangeKey.of(newKey)))
        .toCompletableFuture()
        .join();

    // Fetch cached id by old key
    final Optional<String> cachedStateId =
        stateService.fetchCachedStateId(OLD_STATE_KEY).toCompletableFuture().join();

    assertThat(cachedStateId).isNotEmpty();
    assertThat(cachedStateId).contains(stateOptional.get().getId());
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingStatesByKeysWithTransitions_WithAnyExistingKeys_ShouldReturnASetOfStates() {
    final String key = "state_key_Y";
    final State transitionState =
        createState(CTP_TARGET_CLIENT, "state_transition_key_2", TRANSITION_STATE_TYPE, null);
    final State fetchState =
        createState(CTP_TARGET_CLIENT, key, TRANSITION_STATE_TYPE, transitionState);

    final Set<String> stateKeys = new HashSet<>();
    stateKeys.add(key);

    final Set<State> matchingStates =
        stateService
            .fetchMatchingStatesByKeysWithTransitions(stateKeys)
            .toCompletableFuture()
            .join();

    assertThat(matchingStates).hasSize(1);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();

    State matchedState = matchingStates.iterator().next();
    assertThat(matchedState.getId()).isEqualTo(fetchState.getId());
    assertThat(matchedState.getTransitions()).hasSize(1);
    Reference<State> transition = matchedState.getTransitions().iterator().next();
    assertThat(transition.getId()).isEqualTo(transitionState.getId());
    assertThat(transition.getObj()).isNotNull();
    assertThat(transition.getObj()).isEqualTo(transitionState);

    clearTransitions(CTP_TARGET_CLIENT, fetchState);
    deleteStates(CTP_TARGET_CLIENT, Optional.of(TRANSITION_STATE_TYPE));
  }

  @Test
  void createState_WithValidState_ShouldCreateStateAndCacheId() {
    final StateDraft newStateDraft =
        StateDraftBuilder.of(STATE_KEY_1, STATE_TYPE)
            .name(STATE_NAME_1)
            .description(STATE_DESCRIPTION_1)
            .build();

    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    final StateSyncOptions spyOptions =
        StateSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final StateService spyStateService = new StateServiceImpl(spyOptions);

    // test
    final Optional<State> createdState =
        spyStateService.createState(newStateDraft).toCompletableFuture().join();

    final Optional<State> queriedOptional =
        CTP_TARGET_CLIENT
            .execute(
                StateQuery.of()
                    .withPredicates(stateQueryModel -> stateQueryModel.key().is(STATE_KEY_1)))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(queriedOptional)
        .hasValueSatisfying(
            queried ->
                assertThat(createdState)
                    .hasValueSatisfying(
                        created -> {
                          assertThat(created.getKey()).isEqualTo(queried.getKey());
                          assertThat(created.getDescription()).isEqualTo(queried.getDescription());
                          assertThat(created.getName()).isEqualTo(queried.getName());
                        }));

    // Assert that the created state is cached
    final Optional<String> stateId =
        spyStateService.fetchCachedStateId(STATE_KEY_1).toCompletableFuture().join();
    assertThat(stateId).isPresent();
    verify(spyClient, times(0)).execute(any(StateQuery.class));
  }

  @Test
  void createState_WithInvalidState_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final StateDraft newStateDraft =
        StateDraftBuilder.of("", STATE_TYPE)
            .name(STATE_NAME_1)
            .description(STATE_DESCRIPTION_1)
            .build();

    final StateSyncOptions options =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final StateService stateService = new StateServiceImpl(options);

    // test
    final Optional<State> result =
        stateService.createState(newStateDraft).toCompletableFuture().join();

    // assertion
    assertThat(result).isEmpty();
    assertThat(errorCallBackMessages)
        .containsExactly("Failed to create draft with key: ''. Reason: Draft key is blank!");
  }

  @Test
  void createState_WithDuplicateKey_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final StateDraft newStateDraft =
        StateDraftBuilder.of(OLD_STATE_KEY, STATE_TYPE)
            .name(STATE_NAME_1)
            .description(STATE_DESCRIPTION_1)
            .build();

    final StateSyncOptions options =
        StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final StateService stateService = new StateServiceImpl(options);

    // test
    final Optional<State> result =
        stateService.createState(newStateDraft).toCompletableFuture().join();

    // assertion
    assertThat(result).isEmpty();
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("A duplicate value");

    assertThat(errorCallBackExceptions)
        .hasSize(1)
        .singleElement()
        .matches(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(ErrorResponseException.class);
              final ErrorResponseException errorResponseException =
                  (ErrorResponseException) exception;

              final List<DuplicateFieldError> fieldErrors =
                  errorResponseException.getErrors().stream()
                      .map(
                          sphereError -> {
                            assertThat(sphereError.getCode()).isEqualTo(DuplicateFieldError.CODE);
                            return sphereError.as(DuplicateFieldError.class);
                          })
                      .collect(toList());
              return fieldErrors.size() == 1;
            });
  }

  @Test
  void updateState_WithValidChanges_ShouldUpdateStateCorrectly() {
    final Optional<State> stateOptional =
        CTP_TARGET_CLIENT
            .execute(
                StateQuery.of()
                    .withPredicates(stateQueryModel -> stateQueryModel.key().is(OLD_STATE_KEY)))
            .toCompletableFuture()
            .join()
            .head();
    assertThat(stateOptional).isNotNull();

    final SetName setNameUpdateAction = SetName.of(ofEnglish("new_state_name"));

    final State updatedState =
        stateService
            .updateState(stateOptional.get(), singletonList(setNameUpdateAction))
            .toCompletableFuture()
            .join();
    assertThat(updatedState).isNotNull();

    final Optional<State> updatedStateOptional =
        CTP_TARGET_CLIENT
            .execute(
                StateQuery.of()
                    .withPredicates(stateQueryModel -> stateQueryModel.key().is(OLD_STATE_KEY)))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(stateOptional).isNotEmpty();
    final State fetchedState = updatedStateOptional.get();
    assertThat(fetchedState.getKey()).isEqualTo(updatedState.getKey());
    assertThat(fetchedState.getDescription()).isEqualTo(updatedState.getDescription());
    assertThat(fetchedState.getName()).isEqualTo(updatedState.getName());
  }

  @Test
  void updateState_WithInvalidChanges_ShouldCompleteExceptionally() {
    final Optional<State> stateOptional =
        CTP_TARGET_CLIENT
            .execute(
                StateQuery.of()
                    .withPredicates(stateQueryModel -> stateQueryModel.key().is(OLD_STATE_KEY)))
            .toCompletableFuture()
            .join()
            .head();
    assertThat(stateOptional).isNotNull();

    final ChangeType changeTypeUpdateAction = ChangeType.of(null);
    stateService
        .updateState(stateOptional.get(), singletonList(changeTypeUpdateAction))
        .exceptionally(
            exception -> {
              assertThat(exception).isNotNull();
              assertThat(exception.getMessage())
                  .contains("Request body does not contain valid JSON.");
              return null;
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void fetchState_WithExistingStateKey_ShouldFetchState() {
    final Optional<State> stateOptional =
        CTP_TARGET_CLIENT
            .execute(
                StateQuery.of()
                    .withPredicates(stateQueryModel -> stateQueryModel.key().is(OLD_STATE_KEY)))
            .toCompletableFuture()
            .join()
            .head();
    assertThat(stateOptional).isNotNull();

    final Optional<State> fetchedStateOptional =
        executeBlocking(stateService.fetchState(OLD_STATE_KEY));
    assertThat(fetchedStateOptional).isEqualTo(stateOptional);
  }

  @Test
  void fetchState_WithBlankKey_ShouldNotFetchState() {
    final Optional<State> fetchedStateOptional =
        executeBlocking(stateService.fetchState(StringUtils.EMPTY));
    assertThat(fetchedStateOptional).isEmpty();
  }

  @Test
  void fetchState_WithNullKey_ShouldNotFetchState() {
    final Optional<State> fetchedStateOptional = executeBlocking(stateService.fetchState(null));
    assertThat(fetchedStateOptional).isEmpty();
  }
}
