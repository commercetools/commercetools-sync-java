package com.commercetools.sync.sdk2.services.impl;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyStatesByIDPost;
import com.commercetools.api.client.ByProjectKeyStatesByIDRequestBuilder;
import com.commercetools.api.client.ByProjectKeyStatesGet;
import com.commercetools.api.client.ByProjectKeyStatesKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyStatesKeyByKeyRequestBuilder;
import com.commercetools.api.client.ByProjectKeyStatesPost;
import com.commercetools.api.client.ByProjectKeyStatesRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.error.ConcurrentModificationException;
import com.commercetools.api.models.error.ErrorResponse;
import com.commercetools.api.models.error.ErrorResponseBuilder;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateChangeInitialActionBuilder;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StatePagedQueryResponse;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.api.models.state.StateUpdate;
import com.commercetools.api.models.state.StateUpdateAction;
import com.commercetools.sync.sdk2.states.StateSyncOptions;
import com.commercetools.sync.sdk2.states.StateSyncOptionsBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateServiceImplTest {

  private ProjectApiRoot client = mock(ProjectApiRoot.class);
  private ByProjectKeyStatesGet byProjectKeyStatesGet;
  private StateServiceImpl service;
  private List<String> errorMessages;
  private List<Throwable> errorExceptions;

  private String stateId;
  private String stateKey;
  private ByProjectKeyStatesKeyByKeyGet byProjectKeyStatesKeyByKeyGet;

  private ByProjectKeyStatesPost byProjectKeyStatesPost;
  private ByProjectKeyStatesByIDPost byProjectKeyStatesByIDPost;

  @BeforeEach
  void setup() {
    final ByProjectKeyStatesRequestBuilder byProjectKeyStatesRequestBuilder =
        mock(ByProjectKeyStatesRequestBuilder.class);
    when(client.states()).thenReturn(byProjectKeyStatesRequestBuilder);
    byProjectKeyStatesGet = mock(ByProjectKeyStatesGet.class);
    final ByProjectKeyStatesByIDRequestBuilder byProjectKeyStatesByIDRequestBuilder =
        mock(ByProjectKeyStatesByIDRequestBuilder.class);
    when(byProjectKeyStatesRequestBuilder.withId(anyString()))
        .thenReturn(byProjectKeyStatesByIDRequestBuilder);
    byProjectKeyStatesByIDPost = mock(ByProjectKeyStatesByIDPost.class);
    when(byProjectKeyStatesByIDRequestBuilder.post(any(StateUpdate.class)))
        .thenReturn(byProjectKeyStatesByIDPost);
    final ByProjectKeyStatesKeyByKeyRequestBuilder byProjectKeyStatesKeyByKeyRequestBuilder =
        mock(ByProjectKeyStatesKeyByKeyRequestBuilder.class);
    when(byProjectKeyStatesRequestBuilder.withKey(anyString()))
        .thenReturn(byProjectKeyStatesKeyByKeyRequestBuilder);

    byProjectKeyStatesKeyByKeyGet = mock(ByProjectKeyStatesKeyByKeyGet.class);
    when(byProjectKeyStatesKeyByKeyRequestBuilder.get()).thenReturn(byProjectKeyStatesKeyByKeyGet);
    when(byProjectKeyStatesRequestBuilder.get()).thenReturn(byProjectKeyStatesGet);
    when(byProjectKeyStatesGet.withWhere(anyString())).thenReturn(byProjectKeyStatesGet);
    when(byProjectKeyStatesGet.withPredicateVar(anyString(), anyCollection()))
        .thenReturn(byProjectKeyStatesGet);
    when(byProjectKeyStatesGet.withLimit(anyInt())).thenReturn(byProjectKeyStatesGet);
    when(byProjectKeyStatesGet.withWithTotal(anyBoolean())).thenReturn(byProjectKeyStatesGet);
    when(byProjectKeyStatesGet.getQueryParam(anyString())).thenReturn(Collections.emptyList());
    when(byProjectKeyStatesGet.withSort(anyString())).thenReturn(byProjectKeyStatesGet);
    when(byProjectKeyStatesGet.withExpand(anyString())).thenReturn(byProjectKeyStatesGet);

    byProjectKeyStatesPost = mock(ByProjectKeyStatesPost.class);
    when(byProjectKeyStatesRequestBuilder.post(any(StateDraft.class)))
        .thenReturn(byProjectKeyStatesPost);

    stateId = RandomStringUtils.random(15);
    stateKey = RandomStringUtils.random(15);

    errorMessages = new ArrayList<>();
    errorExceptions = new ArrayList<>();
    StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(client)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  errorExceptions.add(exception.getCause());
                })
            .build();
    service = new StateServiceImpl(stateSyncOptions);
  }

  @AfterEach
  void cleanup() {
    reset(client);
  }

  @Test
  void fetchCachedStateId_WithKey_ShouldFetchState() {
    final String key = RandomStringUtils.random(15);
    final String id = RandomStringUtils.random(15);

    final State mock = mock(State.class);
    when(mock.getId()).thenReturn(id);
    when(mock.getKey()).thenReturn(key);

    final ApiHttpResponse<StatePagedQueryResponse> apiHttpResponse = mock(ApiHttpResponse.class);
    final StatePagedQueryResponse result = mock(StatePagedQueryResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(result);
    when(result.getResults()).thenReturn(Collections.singletonList(mock));

    when(byProjectKeyStatesGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final Optional<String> fetchedId = service.fetchCachedStateId(key).toCompletableFuture().join();

    assertThat(fetchedId).contains(id);
  }

  @Test
  void fetchMatchingStatesByKeys_WithKeySet_ShouldFetchStates() {
    final String key1 = RandomStringUtils.random(15);
    final String key2 = RandomStringUtils.random(15);

    final HashSet<String> stateKeys = new HashSet<>();
    stateKeys.add(key1);
    stateKeys.add(key2);

    final State mock1 = mock(State.class);
    when(mock1.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock1.getKey()).thenReturn(key1);

    final State mock2 = mock(State.class);
    when(mock2.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock2.getKey()).thenReturn(key2);

    final ApiHttpResponse<StatePagedQueryResponse> apiHttpResponse = mock(ApiHttpResponse.class);
    final StatePagedQueryResponse result = mock(StatePagedQueryResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(result);
    when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));

    when(byProjectKeyStatesGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final Set<State> states =
        service.fetchMatchingStatesByKeys(stateKeys).toCompletableFuture().join();

    assertAll(
        () -> assertThat(states).isNotEmpty(),
        () -> assertThat(states).contains(mock1, mock2),
        () -> assertThat(service.keyToIdCache.asMap()).containsKeys(key1, key2));
    verify(byProjectKeyStatesGet, times(0)).withExpand(anyString());
  }

  @Test
  void shouldFetchStatesByKeysWithExpandedTransitions() {
    final String key1 = RandomStringUtils.random(15);
    final String key2 = RandomStringUtils.random(15);

    final HashSet<String> stateKeys = new HashSet<>();
    stateKeys.add(key1);
    stateKeys.add(key2);

    final State mock1 = mock(State.class);
    when(mock1.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock1.getKey()).thenReturn(key1);

    final State mock2 = mock(State.class);
    when(mock2.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock2.getKey()).thenReturn(key2);

    final ApiHttpResponse<StatePagedQueryResponse> apiHttpResponse = mock(ApiHttpResponse.class);
    final StatePagedQueryResponse result = mock(StatePagedQueryResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(result);
    when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));
    when(byProjectKeyStatesGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final Set<State> states =
        service.fetchMatchingStatesByKeysWithTransitions(stateKeys).toCompletableFuture().join();

    assertAll(
        () -> assertThat(states).isNotEmpty(),
        () -> assertThat(states).contains(mock1, mock2),
        () -> assertThat(service.keyToIdCache.asMap()).containsKeys(key1, key2));
    verify(byProjectKeyStatesGet, times(1)).withExpand("transitions[*]");
  }

  @Test
  void fetchState_WithKey_ShouldFetchState() {
    final State mock = mock(State.class);
    when(mock.getId()).thenReturn(stateId);
    when(mock.getKey()).thenReturn(stateKey);

    final ApiHttpResponse<State> apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(mock);
    when(byProjectKeyStatesKeyByKeyGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final Optional<State> stateOptional = service.fetchState(stateKey).toCompletableFuture().join();

    assertAll(
        () -> assertThat(stateOptional).containsSame(mock),
        () -> assertThat(service.keyToIdCache.getIfPresent(stateKey)).isEqualTo(stateId));
    verify(byProjectKeyStatesKeyByKeyGet, times(1)).execute();
  }

  @Test
  void createState_WithDraft_ShouldCreateState() {
    State mock = mock(State.class);
    when(mock.getId()).thenReturn(stateId);
    when(mock.getKey()).thenReturn(stateKey);

    final ApiHttpResponse<State> apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(mock);
    when(byProjectKeyStatesPost.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final StateDraft draft =
        StateDraftBuilder.of().key(stateKey).type(StateTypeEnum.LINE_ITEM_STATE).build();
    final Optional<State> stateOptional = service.createState(draft).toCompletableFuture().join();

    assertThat(stateOptional).containsSame(mock);
    verify(byProjectKeyStatesPost, times(1)).execute();
  }

  @Test
  void createState_WithRequestException_ShouldNotCreateState() throws JsonProcessingException {
    State mock = mock(State.class);
    when(mock.getId()).thenReturn(stateId);

    final ApiHttpResponse<State> apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(mock);
    final ErrorResponse errorResponse =
        ErrorResponseBuilder.of()
            .statusCode(409)
            .errors(Collections.emptyList())
            .message("test")
            .build();

    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    final String json = ow.writeValueAsString(errorResponse);

    when(byProjectKeyStatesPost.execute())
        .thenReturn(
            CompletableFuture.failedFuture(
                new ConcurrentModificationException(
                    409, "", null, "", new ApiHttpResponse<>(409, null, json.getBytes()))));

    final StateDraft draft = mock(StateDraft.class);
    when(draft.getKey()).thenReturn(stateKey);

    final Optional<State> stateOptional = service.createState(draft).toCompletableFuture().join();

    assertAll(
        () -> assertThat(stateOptional).isEmpty(),
        () -> assertThat(errorMessages).hasSize(1),
        () ->
            assertThat(errorMessages)
                .singleElement(as(STRING))
                .contains("Failed to create draft with key: '" + stateKey + "'.")
                .contains("ApiHttpResponse"),
        () -> assertThat(errorExceptions).hasSize(1),
        () ->
            assertThat(errorExceptions)
                .singleElement(as(THROWABLE))
                .isExactlyInstanceOf(ConcurrentModificationException.class));
  }

  @Test
  void createState_WithDraftHasNoKey_ShouldNotCreateState() {
    final StateDraft draft = mock(StateDraft.class);

    final Optional<State> stateOptional = service.createState(draft).toCompletableFuture().join();

    assertAll(
        () -> assertThat(stateOptional).isEmpty(),
        () -> assertThat(errorMessages).hasSize(1),
        () -> assertThat(errorExceptions).hasSize(1),
        () ->
            assertThat(errorMessages.get(0))
                .isEqualTo("Failed to create draft with key: 'null'. Reason: Draft key is blank!"));
  }

  @Test
  void updateState_WithNoError_ShouldUpdateState() {
    final State mock = mock(State.class);
    when(mock.getId()).thenReturn(stateId);
    when(mock.getVersion()).thenReturn(1L);

    final ApiHttpResponse<State> apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(mock);
    when(byProjectKeyStatesByIDPost.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final List<StateUpdateAction> updateActions =
        Collections.singletonList(StateChangeInitialActionBuilder.of().initial(false).build());

    final State state = service.updateState(mock, updateActions).toCompletableFuture().join();

    assertThat(state).isSameAs(mock);
    verify(byProjectKeyStatesByIDPost, times(1)).execute();
  }
}
