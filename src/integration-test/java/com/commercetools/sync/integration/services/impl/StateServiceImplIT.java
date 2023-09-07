package com.commercetools.sync.integration.services.impl;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.*;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyStatesGet;
import com.commercetools.api.client.ByProjectKeyStatesRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.error.DuplicateFieldError;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateChangeKeyActionBuilder;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.state.StateSetNameAction;
import com.commercetools.api.models.state.StateSetNameActionBuilder;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.impl.StateServiceImpl;
import com.commercetools.sync.states.StateSyncOptions;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateServiceImplIT {

  private static final StateTypeEnum STATE_TYPE = StateTypeEnum.PRODUCT_STATE;

  private static final StateTypeEnum TRANSITION_STATE_TYPE = StateTypeEnum.REVIEW_STATE;
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

    deleteStates(CTP_TARGET_CLIENT, STATE_TYPE);
    deleteStates(CTP_TARGET_CLIENT, TRANSITION_STATE_TYPE);
    warnings = new ArrayList<>();
    oldState = ensureState(CTP_TARGET_CLIENT, STATE_TYPE);

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
    deleteStates(CTP_TARGET_CLIENT, STATE_TYPE);
    deleteStates(CTP_TARGET_CLIENT, TRANSITION_STATE_TYPE);
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
    StateReference transition = matchedState.getTransitions().iterator().next();
    assertThat(transition.getId()).isEqualTo(transitionState.getId());
    assertThat(transition.getObj()).isNull();

    clearTransitions(CTP_TARGET_CLIENT, fetchState);
    deleteStates(CTP_TARGET_CLIENT, TRANSITION_STATE_TYPE);
  }

  @Test
  void fetchMatchingStatesByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
    // Mock sphere client to return BadGatewayException on any request.

    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);
    when(spyClient.states()).thenReturn(mock(ByProjectKeyStatesRequestBuilder.class));
    final ByProjectKeyStatesGet getMock = mock(ByProjectKeyStatesGet.class);
    when(spyClient.states().get()).thenReturn(getMock);
    when(getMock.withWhere(any(String.class))).thenReturn(getMock);
    when(getMock.withPredicateVar(any(String.class), any())).thenReturn(getMock);
    when(getMock.withLimit(any(Integer.class))).thenReturn(getMock);
    when(getMock.withWithTotal(any(Boolean.class))).thenReturn(getMock);
    when(getMock.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new BadGatewayException(500, "", null, "", null)))
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

    final State state =
        CTP_TARGET_CLIENT.states().withKey(OLD_STATE_KEY).get().executeBlocking().getBody();

    assertThat(state).isNotNull();

    // Change state old_state_key on ctp
    final String newKey = "new_state_key";
    stateService
        .updateState(state, singletonList(StateChangeKeyActionBuilder.of().key(newKey).build()))
        .toCompletableFuture()
        .join();

    // Fetch cached id by old key
    final Optional<String> cachedStateId =
        stateService.fetchCachedStateId(OLD_STATE_KEY).toCompletableFuture().join();

    assertThat(cachedStateId).isNotEmpty();
    assertThat(cachedStateId).contains(state.getId());
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

    final State matchedState = matchingStates.iterator().next();
    assertThat(matchedState.getId()).isEqualTo(fetchState.getId());
    assertThat(matchedState.getTransitions()).hasSize(1);
    final StateReference transition = matchedState.getTransitions().iterator().next();
    assertThat(transition.getId()).isEqualTo(transitionState.getId());
    assertThat(transition.getObj()).isNotNull();
    assertThat(transition.getObj()).isEqualTo(transitionState);

    clearTransitions(CTP_TARGET_CLIENT, fetchState);
    deleteStates(CTP_TARGET_CLIENT, TRANSITION_STATE_TYPE);
  }

  @Test
  void createState_WithValidState_ShouldCreateStateAndCacheId() {
    // preparation
    final StateDraft newStateDraft =
        StateDraftBuilder.of()
            .key(STATE_KEY_1)
            .name(STATE_NAME_1)
            .description(STATE_DESCRIPTION_1)
            .type(STATE_TYPE)
            .build();

    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);

    final StateSyncOptions spyOptions =
        StateSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final StateService stateService = new StateServiceImpl(spyOptions);

    // test
    final Optional<State> createdOptional =
        stateService.createState(newStateDraft).toCompletableFuture().join();
    // assertion
    final State fetchedState =
        CTP_TARGET_CLIENT
            .states()
            .withKey(STATE_KEY_1)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    assertThat(fetchedState)
        .satisfies(
            queried ->
                assertThat(createdOptional)
                    .hasValueSatisfying(
                        created -> {
                          assertThat(created.getKey()).isEqualTo(queried.getKey());

                          assertThat(created.getDescription()).isEqualTo(queried.getDescription());
                          assertThat(created.getName()).isEqualTo(queried.getName());
                          assertThat(created.getType()).isEqualTo(queried.getType());
                        }));

    final ByProjectKeyStatesRequestBuilder mock1 = mock(ByProjectKeyStatesRequestBuilder.class);
    when(spyClient.states()).thenReturn(mock1);
    final ByProjectKeyStatesGet mock2 = mock(ByProjectKeyStatesGet.class);
    when(mock1.get()).thenReturn(mock2);
    when(mock2.withWhere(any(String.class))).thenReturn(mock2);
    when(mock2.withPredicateVar(any(String.class), any())).thenReturn(mock2);
    final CompletableFuture<ApiHttpResponse<State>> spy = mock(CompletableFuture.class);

    // Assert that the created state is cached
    final Optional<String> stateId =
        stateService.fetchCachedStateId(STATE_KEY_1).toCompletableFuture().join();
    assertThat(stateId).isPresent();
    verify(spy, times(0)).handle(any());
  }

  @Test
  void createState_WithInvalidState_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final StateDraft newStateDraft =
        StateDraftBuilder.of()
            .key("")
            .type(STATE_TYPE)
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
        StateDraftBuilder.of()
            .key(OLD_STATE_KEY)
            .type(STATE_TYPE)
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
              BadRequestException badRequestException = (BadRequestException) exception.getCause();

              final List<DuplicateFieldError> fieldErrors =
                  badRequestException.getErrorResponse().getErrors().stream()
                      .map(
                          ctpError -> {
                            assertThat(ctpError.getCode())
                                .isEqualTo(DuplicateFieldError.DUPLICATE_FIELD);
                            return (DuplicateFieldError) ctpError;
                          })
                      .collect(toList());
              return fieldErrors.size() == 1;
            });
  }

  @Test
  void updateState_WithValidChanges_ShouldUpdateStateCorrectly() {
    final State state =
        CTP_TARGET_CLIENT.states().withKey(OLD_STATE_KEY).get().executeBlocking().getBody();
    assertThat(state).isNotNull();

    final StateSetNameAction setNameAction =
        StateSetNameActionBuilder.of().name(ofEnglish("new_state_name")).build();

    final State updatedState =
        stateService.updateState(state, singletonList(setNameAction)).toCompletableFuture().join();
    assertThat(updatedState).isNotNull();

    final State fetchedState =
        CTP_TARGET_CLIENT.states().withKey(OLD_STATE_KEY).get().executeBlocking().getBody();

    assertThat(fetchedState.getKey()).isEqualTo(updatedState.getKey());
    assertThat(fetchedState.getDescription()).isEqualTo(updatedState.getDescription());
    assertThat(fetchedState.getName()).isEqualTo(updatedState.getName());
  }

  @Test
  void fetchState_WithExistingStateKey_ShouldFetchState() {
    final State state =
        CTP_TARGET_CLIENT.states().withKey(OLD_STATE_KEY).get().executeBlocking().getBody();
    assertThat(state).isNotNull();

    final Optional<State> fetchedStateOptional =
        stateService.fetchState(OLD_STATE_KEY).toCompletableFuture().join();
    assertThat(fetchedStateOptional.get()).isEqualTo(state);
  }

  @Test
  void fetchState_WithBlankKey_ShouldNotFetchState() {
    final Optional<State> fetchedStateOptional =
        stateService.fetchState(StringUtils.EMPTY).toCompletableFuture().join();
    assertThat(fetchedStateOptional).isEmpty();
  }

  @Test
  void fetchState_WithNullKey_ShouldNotFetchState() {
    final Optional<State> fetchedStateOptional =
        stateService.fetchState(null).toCompletableFuture().join();
    assertThat(fetchedStateOptional).isEmpty();
  }
}
