package com.commercetools.sync.services.impl;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.only;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.services.ShoppingListService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.InternalServerErrorException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.commands.ShoppingListUpdateCommand;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

  @BeforeEach
  void setUp() {
    errorMessages = new ArrayList<>();
    errorExceptions = new ArrayList<>();
    shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  errorExceptions.add(exception.getCause());
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
    verify(shoppingListSyncOptions.getCtpClient(), never()).execute(any());
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
    verify(shoppingListSyncOptions.getCtpClient(), never()).execute(any());
  }

  @Test
  void fetchShoppingList_WithValidKey_ShouldReturnMockShoppingList() {
    // preparation
    final ShoppingList mockShoppingList = mock(ShoppingList.class);
    when(mockShoppingList.getId()).thenReturn("testId");
    when(mockShoppingList.getKey()).thenReturn("any_key");

    @SuppressWarnings("unchecked")
    final PagedQueryResult<ShoppingList> pagedQueryResult = mock(PagedQueryResult.class);
    when(pagedQueryResult.head()).thenReturn(Optional.of(mockShoppingList));
    when(shoppingListSyncOptions.getCtpClient().execute(any(ShoppingListQuery.class)))
        .thenReturn(completedFuture(pagedQueryResult));

    // test
    final Optional<ShoppingList> result =
        service.fetchShoppingList("any_key").toCompletableFuture().join();

    // assertions
    assertThat(result).containsSame(mockShoppingList);
    assertThat(errorExceptions).isEmpty();
    assertThat(errorMessages).isEmpty();
    verify(shoppingListSyncOptions.getCtpClient(), only()).execute(any());
  }

  @Test
  void fetchMatchingShoppingListsByKeys_WithUnexpectedException_ShouldFail() {
    when(shoppingListSyncOptions.getCtpClient().execute(any()))
        .thenReturn(CompletableFutureUtils.failed(new BadGatewayException("bad gateway")));

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
        ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class))
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
    verify(shoppingListSyncOptions.getCtpClient(), times(0)).execute(any());
  }

  @Test
  void createShoppingList_WithEmptyShoppingListKey_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final SphereClient sphereClient = mock(SphereClient.class);
    final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
    final Map<String, Throwable> errors = new HashMap<>();
    when(shoppingListDraft.getKey()).thenReturn("");

    final ShoppingListSyncOptions options =
        ShoppingListSyncOptionsBuilder.of(sphereClient)
            .errorCallback(
                (exception, oldResource, newResource, actions) ->
                    errors.put(exception.getMessage(), exception))
            .build();

    final ShoppingListService shoppingListService = new ShoppingListServiceImpl(options);

    // test
    final CompletionStage<Optional<ShoppingList>> result =
        shoppingListService.createShoppingList(shoppingListDraft);

    // assertion
    assertThat(result).isCompletedWithValue(Optional.empty());
    assertThat(errors.keySet())
        .containsExactly("Failed to create draft with key: ''. Reason: Draft key is blank!");
    verify(options.getCtpClient(), times(0)).execute(any());
  }

  @Test
  void createShoppingList_WithUnsuccessfulMockCtpResponse_ShouldNotCreateShoppingList() {
    // preparation
    final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
    final Map<String, Throwable> errors = new HashMap<>();
    when(shoppingListDraft.getKey()).thenReturn("key");

    final ShoppingListSyncOptions options =
        ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback(
                (exception, oldResource, newResource, actions) ->
                    errors.put(exception.getMessage(), exception))
            .build();

    final ShoppingListService shoppingListService = new ShoppingListServiceImpl(options);

    when(options.getCtpClient().execute(any()))
        .thenReturn(CompletableFutureUtils.failed(new InternalServerErrorException()));

    // test
    final CompletionStage<Optional<ShoppingList>> result =
        shoppingListService.createShoppingList(shoppingListDraft);

    // assertions
    assertThat(result).isCompletedWithValue(Optional.empty());
    assertThat(errors.keySet())
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to create draft with key: 'key'.");

    assertThat(errors.values())
        .hasSize(1)
        .singleElement()
        .satisfies(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(SyncException.class);
              assertThat(exception.getCause())
                  .isExactlyInstanceOf(InternalServerErrorException.class);
            });
  }

  @Test
  void updateShoppingList_WithMockSuccessfulCtpResponse_ShouldCallShoppingListUpdateCommand() {
    // preparation
    final ShoppingList shoppingList = mock(ShoppingList.class);
    final ShoppingListSyncOptions options =
        ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    when(options.getCtpClient().execute(any())).thenReturn(completedFuture(shoppingList));
    final ShoppingListService shoppingListService = new ShoppingListServiceImpl(options);

    final List<UpdateAction<ShoppingList>> updateActions =
        singletonList(ChangeName.of(LocalizedString.ofEnglish("new_name")));
    // test
    final CompletionStage<ShoppingList> result =
        shoppingListService.updateShoppingList(shoppingList, updateActions);

    // assertions
    assertThat(result).isCompletedWithValue(shoppingList);
    verify(options.getCtpClient())
        .execute(eq(ShoppingListUpdateCommand.of(shoppingList, updateActions)));
  }

  @Test
  void updateShoppingList_WithMockUnsuccessfulCtpResponse_ShouldCompleteExceptionally() {
    // preparation
    final ShoppingList shoppingList = mock(ShoppingList.class);
    final ShoppingListSyncOptions shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    when(shoppingListSyncOptions.getCtpClient().execute(any()))
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new InternalServerErrorException()));

    final ShoppingListService shoppingListService =
        new ShoppingListServiceImpl(shoppingListSyncOptions);

    final List<UpdateAction<ShoppingList>> updateActions =
        singletonList(ChangeName.of(LocalizedString.ofEnglish("new_name")));
    // test
    final CompletionStage<ShoppingList> result =
        shoppingListService.updateShoppingList(shoppingList, updateActions);

    // assertions
    assertThat(result)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(InternalServerErrorException.class);
  }
}
