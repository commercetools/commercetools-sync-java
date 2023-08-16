package com.commercetools.sync.sdk2.services.impl;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.commons.ExceptionUtils.createBadGatewayException;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.only;

import com.commercetools.api.client.ByProjectKeyShoppingListsByIDPost;
import com.commercetools.api.client.ByProjectKeyShoppingListsByIDRequestBuilder;
import com.commercetools.api.client.ByProjectKeyShoppingListsGet;
import com.commercetools.api.client.ByProjectKeyShoppingListsKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyShoppingListsKeyByKeyRequestBuilder;
import com.commercetools.api.client.ByProjectKeyShoppingListsPost;
import com.commercetools.api.client.ByProjectKeyShoppingListsRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListChangeNameActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListUpdate;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateBuilder;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.services.ShoppingListService;
import com.commercetools.sync.sdk2.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.sdk2.shoppinglists.ShoppingListSyncOptionsBuilder;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShoppingListServiceImplTest {

  private ShoppingListServiceImpl service;
  private ShoppingListSyncOptions shoppingListSyncOptions;
  private List<String> errorMessages;
  private List<Throwable> errorExceptions;

  private ByProjectKeyShoppingListsKeyByKeyGet byProjectKeyShoppingListsKeyByKeyGet;

  private ByProjectKeyShoppingListsGet byProjectKeyShoppingListsGet;

  private ByProjectKeyShoppingListsPost byProjectKeyShoppingListsPost;

  private ByProjectKeyShoppingListsByIDPost byProjectKeyShoppingListsByIDPost;

  private ByProjectKeyShoppingListsByIDRequestBuilder byProjectKeyShoppingListsByIDRequestBuilder;

  @BeforeEach
  void setUp() {
    final ProjectApiRoot projectApiRoot = mock(ProjectApiRoot.class);
    final ByProjectKeyShoppingListsRequestBuilder byProjectKeyShoppingListsRequestBuilder = mock();
    when(projectApiRoot.shoppingLists()).thenReturn(byProjectKeyShoppingListsRequestBuilder);
    byProjectKeyShoppingListsGet = mock();
    when(byProjectKeyShoppingListsRequestBuilder.get()).thenReturn(byProjectKeyShoppingListsGet);
    byProjectKeyShoppingListsByIDRequestBuilder = mock();
    when(byProjectKeyShoppingListsRequestBuilder.withId(anyString()))
        .thenReturn(byProjectKeyShoppingListsByIDRequestBuilder);
    byProjectKeyShoppingListsByIDPost = mock();
    when(byProjectKeyShoppingListsByIDRequestBuilder.post(any(ShoppingListUpdate.class)))
        .thenReturn(byProjectKeyShoppingListsByIDPost);
    byProjectKeyShoppingListsPost = mock();
    when(byProjectKeyShoppingListsRequestBuilder.post(any(ShoppingListDraft.class)))
        .thenReturn(byProjectKeyShoppingListsPost);
    when(byProjectKeyShoppingListsGet.withWhere(anyString()))
        .thenReturn(byProjectKeyShoppingListsGet);
    when(byProjectKeyShoppingListsGet.withPredicateVar(anyString(), anyCollection()))
        .thenReturn(byProjectKeyShoppingListsGet);
    when(byProjectKeyShoppingListsGet.withLimit(anyInt())).thenReturn(byProjectKeyShoppingListsGet);
    when(byProjectKeyShoppingListsGet.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyShoppingListsGet);
    final ByProjectKeyShoppingListsKeyByKeyRequestBuilder
        byProjectKeyShoppingListsKeyByKeyRequestBuilder = mock();
    when(byProjectKeyShoppingListsRequestBuilder.withKey(any()))
        .thenReturn(byProjectKeyShoppingListsKeyByKeyRequestBuilder);
    byProjectKeyShoppingListsKeyByKeyGet = mock();
    when(byProjectKeyShoppingListsKeyByKeyRequestBuilder.get())
        .thenReturn(byProjectKeyShoppingListsKeyByKeyGet);

    errorMessages = new ArrayList<>();
    errorExceptions = new ArrayList<>();
    shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(projectApiRoot)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  errorExceptions.add(exception);
                })
            .build();
    service = new ShoppingListServiceImpl(shoppingListSyncOptions);
  }

  @Test
  void fetchShoppingList_WithEmptyKey_ShouldNotFetchAnyShoppingList() {
    // test
    final Optional<ShoppingList> result =
        service.fetchShoppingList("").toCompletableFuture().join();

    // assertions
    assertThat(result).isEmpty();
    assertThat(errorExceptions).isEmpty();
    assertThat(errorMessages).isEmpty();

    verify(byProjectKeyShoppingListsKeyByKeyGet, never()).execute();
  }

  @Test
  void fetchShoppingList_WithNullKey_ShouldNotFetchAnyShoppingList() {
    // test
    final Optional<ShoppingList> result =
        service.fetchShoppingList(null).toCompletableFuture().join();

    // assertions
    assertThat(result).isEmpty();
    assertThat(errorExceptions).isEmpty();
    assertThat(errorMessages).isEmpty();
    verify(byProjectKeyShoppingListsKeyByKeyGet, never()).execute();
  }

  @Test
  void fetchShoppingList_WithValidKey_ShouldReturnMockShoppingList() {
    // preparation
    final ShoppingList mockShoppingList = mock(ShoppingList.class);
    when(mockShoppingList.getId()).thenReturn("testId");
    when(mockShoppingList.getKey()).thenReturn("any_key");

    final ApiHttpResponse<ShoppingList> response = mock(ApiHttpResponse.class);
    when(response.getBody()).thenReturn(mockShoppingList);
    when(byProjectKeyShoppingListsKeyByKeyGet.execute())
        .thenReturn(CompletableFuture.completedFuture(response));

    // test
    final Optional<ShoppingList> result =
        service.fetchShoppingList("any_key").toCompletableFuture().join();

    // assertions
    assertThat(result).containsSame(mockShoppingList);
    assertThat(errorExceptions).isEmpty();
    assertThat(errorMessages).isEmpty();
    verify(byProjectKeyShoppingListsKeyByKeyGet, only()).execute();
  }

  @Test
  void fetchMatchingShoppingListsByKeys_WithUnexpectedException_ShouldFail() {
    final BadGatewayException badGatewayException =
        new BadGatewayException(500, "", null, "Failed request", null);
    when(byProjectKeyShoppingListsGet.execute())
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(badGatewayException));

    assertThat(service.fetchMatchingShoppingListsByKeys(singleton("key")))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
    assertThat(errorExceptions).isEmpty();
    assertThat(errorMessages).isEmpty();
  }

  @Test
  void fetchMatchingShoppingListsByKeys_WithEmptyKeys_ShouldReturnEmptyOptional() {
    Set<ShoppingList> customer =
        service.fetchMatchingShoppingListsByKeys(emptySet()).toCompletableFuture().join();

    assertThat(customer).isEmpty();
    assertThat(errorExceptions).isEmpty();
    assertThat(errorMessages).isEmpty();
    verifyNoInteractions(shoppingListSyncOptions.getCtpClient());
  }

  @Test
  void createShoppingList_WithNullShoppingListKey_ShouldNotCreateShoppingList() {
    // preparation
    final ShoppingListDraft mockShoppingListDraft = mock(ShoppingListDraft.class);
    final Map<String, Throwable> errors = new HashMap<>();
    when(mockShoppingListDraft.getKey()).thenReturn(null);

    final ShoppingListSyncOptions shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, actions) ->
                    errors.put(exception.getMessage(), exception))
            .build();
    final ShoppingListService shoppingListService =
        new ShoppingListServiceImpl(shoppingListSyncOptions);

    // test
    final CompletionStage<Optional<ShoppingList>> result =
        shoppingListService.createShoppingList(mockShoppingListDraft);

    // assertions
    assertThat(result).isCompletedWithValue(Optional.empty());
    assertThat(errors.keySet())
        .containsExactly("Failed to create draft with key: 'null'. Reason: Draft key is blank!");
    verify(byProjectKeyShoppingListsPost, times(0)).execute();
  }

  @Test
  void createShoppingList_WithEmptyShoppingListKey_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
    when(shoppingListDraft.getKey()).thenReturn("");

    final ShoppingListService shoppingListService =
        new ShoppingListServiceImpl(shoppingListSyncOptions);

    // test
    final CompletionStage<Optional<ShoppingList>> result =
        shoppingListService.createShoppingList(shoppingListDraft);

    // assertion
    assertThat(result).isCompletedWithValue(Optional.empty());
    assertThat(errorMessages.get(0))
        .contains("Failed to create draft with key: ''. Reason: Draft key is blank!");
    verify(byProjectKeyShoppingListsPost, times(0)).execute();
  }

  @Test
  void createShoppingList_WithUnsuccessfulMockCtpResponse_ShouldNotCreateShoppingList() {
    // preparation
    final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
    when(shoppingListDraft.getKey()).thenReturn("key");

    final ShoppingListService shoppingListService =
        new ShoppingListServiceImpl(shoppingListSyncOptions);

    when(byProjectKeyShoppingListsPost.execute())
        .thenReturn(CompletableFuture.failedFuture(createBadGatewayException()));

    // test
    final CompletionStage<Optional<ShoppingList>> result =
        shoppingListService.createShoppingList(shoppingListDraft);

    // assertions
    assertThat(result).isCompletedWithValue(Optional.empty());
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to create draft with key: 'key'.");

    assertThat(errorExceptions)
        .hasSize(1)
        .singleElement()
        .satisfies(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(SyncException.class);
              assertThat(exception.getCause()).isExactlyInstanceOf(BadGatewayException.class);
            });
  }

  @Test
  void updateShoppingList_WithMockSuccessfulCtpResponse_ShouldCallShoppingListUpdateCommand() {
    // preparation
    final ShoppingList shoppingList = mock(ShoppingList.class);
    when(shoppingList.getId()).thenReturn("testId");

    final ApiHttpResponse<ShoppingList> apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(shoppingList);

    when(byProjectKeyShoppingListsByIDPost.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    final ShoppingListService shoppingListService =
        new ShoppingListServiceImpl(shoppingListSyncOptions);

    final List<ShoppingListUpdateAction> updateActions =
        singletonList(ShoppingListChangeNameActionBuilder.of().name(ofEnglish("new_name")).build());
    // test
    final CompletionStage<ShoppingList> result =
        shoppingListService.updateShoppingList(shoppingList, updateActions);

    // assertions
    assertThat(result).isCompletedWithValue(shoppingList);
    verify(byProjectKeyShoppingListsByIDRequestBuilder)
        .post(
            eq(
                ShoppingListUpdateBuilder.of()
                    .actions(updateActions)
                    .version(shoppingList.getVersion())
                    .build()));
  }

  @Test
  void updateShoppingList_WithMockUnsuccessfulCtpResponse_ShouldCompleteExceptionally() {
    // preparation
    final ShoppingList shoppingList = mock(ShoppingList.class);
    when(shoppingList.getId()).thenReturn("testId");

    when(byProjectKeyShoppingListsByIDPost.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(createBadGatewayException()));

    final ShoppingListService shoppingListService =
        new ShoppingListServiceImpl(shoppingListSyncOptions);

    final List<ShoppingListUpdateAction> updateActions =
        singletonList(ShoppingListChangeNameActionBuilder.of().name(ofEnglish("new_name")).build());
    // test
    final CompletionStage<ShoppingList> result =
        shoppingListService.updateShoppingList(shoppingList, updateActions);

    // assertions
    assertThat(result)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }
}
