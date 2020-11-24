package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.FakeClient;
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
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShoppingListServiceImplTest {

    private ShoppingListServiceImpl service;
    private ShoppingListSyncOptions shoppingListSyncOptions;
    private List<String> errorMessages;
    private List<Throwable> errorExceptions;

    @BeforeEach
    void setUp() {
        errorMessages = new ArrayList<>();
        errorExceptions = new ArrayList<>();
    }

    @Test
    void fetchShoppingList_WithEmptyKey_ShouldNotFetchAnyShoppingList() {
        // test
        final FakeClient<ShoppingList> fakeShoppingListClient = new FakeClient<>(mock(ShoppingList.class));
        initMockService(fakeShoppingListClient);
        final Optional<ShoppingList> result = service.fetchShoppingList("").toCompletableFuture().join();

        // assertions
        assertThat(result).isEmpty();
        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(fakeShoppingListClient.isExecuted()).isFalse();
    }

    @Test
    void fetchShoppingList_WithNullKey_ShouldNotFetchAnyShoppingList() {
        // test
        final FakeClient<ShoppingList> fakeShoppingListClient = new FakeClient<>(mock(ShoppingList.class));
        initMockService(fakeShoppingListClient);
        final Optional<ShoppingList> result = service.fetchShoppingList(null).toCompletableFuture().join();

        // assertions
        assertThat(result).isEmpty();
        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(fakeShoppingListClient.isExecuted()).isFalse();
    }

    @Test
    void fetchShoppingList_WithValidKey_ShouldReturnMockShoppingList() {
        // preparation
        final ShoppingList mockShoppingList = mock(ShoppingList.class);
        when(mockShoppingList.getId()).thenReturn("testId");
        when(mockShoppingList.getKey()).thenReturn("any_key");

        @SuppressWarnings("unchecked")
        final PagedQueryResult<ShoppingList> pagedQueryResult =  mock(PagedQueryResult.class);
        when(pagedQueryResult.head()).thenReturn(Optional.of(mockShoppingList));
        final FakeClient<PagedQueryResult<ShoppingList>> fakeShoppingListClient = new FakeClient<>(pagedQueryResult);
        initMockService(fakeShoppingListClient);

        // test
        final Optional<ShoppingList> result =
                service.fetchShoppingList("any_key").toCompletableFuture().join();

        // assertions
        assertThat(result).containsSame(mockShoppingList);
        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(fakeShoppingListClient.isExecuted()).isTrue();
    }

    @Test
    void fetchMatchingShoppingListsByKeys_WithUnexpectedException_ShouldFail() {
        final FakeClient<ShoppingList> fakeShoppingListClient =
                new FakeClient<>(new BadGatewayException("bad gateway"));
        initMockService(fakeShoppingListClient);

        assertThat(service.fetchMatchingShoppingListsByKeys(singleton("key")))
                .failsWithin(1, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(BadGatewayException.class);

        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
    }

    @Test
    void fetchMatchingShoppingListsByKeys_WithEmptyKeys_ShouldReturnEmptyOptional() {
        final FakeClient<ShoppingList> fakeShoppingListClient =
                new FakeClient<>(mock(ShoppingList.class));
        initMockService(fakeShoppingListClient);
        Set<ShoppingList> customer = service.fetchMatchingShoppingListsByKeys(emptySet()).toCompletableFuture().join();

        assertThat(customer).isEmpty();
        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(fakeShoppingListClient.isExecuted()).isFalse();
    }


    @Test
    void createShoppingList_WithNullShoppingListKey_ShouldNotCreateShoppingList() {
        // preparation
        final ShoppingListDraft mockShoppingListDraft = mock(ShoppingListDraft.class);
        final Map<String, Throwable> errors = new HashMap<>();
        when(mockShoppingListDraft.getKey()).thenReturn(null);
        final FakeClient<ShoppingList> fakeShoppingListClient =
                new FakeClient<>(mock(ShoppingList.class));
        initMockService(fakeShoppingListClient);
        final ShoppingListSyncOptions shoppingListSyncOptions = ShoppingListSyncOptionsBuilder
                .of(fakeShoppingListClient)
                .errorCallback((exception, oldResource, newResource, actions) ->
                        errors.put(exception.getMessage(), exception))
                .build();
        final ShoppingListService shoppingListService = new ShoppingListServiceImpl(shoppingListSyncOptions);

        // test
        final CompletionStage<Optional<ShoppingList>> result = shoppingListService
                .createShoppingList(mockShoppingListDraft);

        // assertions
        assertThat(result).isCompletedWithValue(Optional.empty());
        assertThat(errors.keySet())
                .containsExactly("Failed to create draft with key: 'null'. Reason: Draft key is blank!");
        assertThat(fakeShoppingListClient.isExecuted()).isFalse();
    }

    @Test
    void createShoppingList_WithEmptyShoppingListKey_ShouldHaveEmptyOptionalAsAResult() {
        //preparation

        final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
        final Map<String, Throwable> errors = new HashMap<>();
        when(shoppingListDraft.getKey()).thenReturn("");

        final FakeClient<ShoppingList> fakeShoppingListClient =
                new FakeClient<>(mock(ShoppingList.class));
        initMockService(fakeShoppingListClient, errors);

        // test
        final CompletionStage<Optional<ShoppingList>> result = service
                .createShoppingList(shoppingListDraft);

        // assertion
        assertThat(result).isCompletedWithValue(Optional.empty());
        assertThat(errors.keySet())
                .containsExactly("Failed to create draft with key: ''. Reason: Draft key is blank!");
        assertThat(fakeShoppingListClient.isExecuted()).isFalse();
    }

    @Test
    void createShoppingList_WithUnsuccessfulMockCtpResponse_ShouldNotCreateShoppingList() {
        // preparation
        final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
        final Map<String, Throwable> errors = new HashMap<>();
        when(shoppingListDraft.getKey()).thenReturn("key");

        final FakeClient<ShoppingList> fakeShoppingListClient =
                new FakeClient<>(new InternalServerErrorException());
        initMockService(fakeShoppingListClient, errors);

        // test
        final CompletionStage<Optional<ShoppingList>> result =
                service.createShoppingList(shoppingListDraft);

        // assertions
        assertThat(result).isCompletedWithValue(Optional.empty());
        assertThat(errors.keySet())
                .hasSize(1)
                .singleElement().satisfies(message -> {
            assertThat(message).contains("Failed to create draft with key: 'key'.");
        });

        assertThat(errors.values())
                .hasSize(1)
                .singleElement().satisfies(exception -> {
            assertThat(exception).isExactlyInstanceOf(SyncException.class);
            assertThat(exception.getCause()).isExactlyInstanceOf(InternalServerErrorException.class);
        });
    }

    @Test
    void updateShoppingList_WithMockSuccessfulCtpResponse_ShouldCallShoppingListUpdateCommand() {
        // preparation
        final ShoppingList shoppingList = mock(ShoppingList.class);

        final FakeClient<ShoppingList> fakeShoppingListClient = new FakeClient<>(shoppingList);
        initMockService(fakeShoppingListClient);

        final List<UpdateAction<ShoppingList>> updateActions =
                singletonList(ChangeName.of(LocalizedString.ofEnglish("new_name")));
        // test
        final CompletionStage<ShoppingList> result =
                service.updateShoppingList(shoppingList, updateActions);

        // assertions
        assertThat(result).isCompletedWithValue(shoppingList);
        assertThat(fakeShoppingListClient.isExecuted()).isTrue();
    }

    @Test
    void updateShoppingList_WithMockUnsuccessfulCtpResponse_ShouldCompleteExceptionally() {
        // preparation
        final ShoppingList shoppingList = mock(ShoppingList.class);
        final FakeClient<ShoppingList> fakeShoppingListClient = new FakeClient<>(new InternalServerErrorException());
        initMockService(fakeShoppingListClient);

        final List<UpdateAction<ShoppingList>> updateActions =
                singletonList(ChangeName.of(LocalizedString.ofEnglish("new_name")));
        // test
        final CompletionStage<ShoppingList> result =
                service.updateShoppingList(shoppingList, updateActions);

        // assertions
        assertThat(result).failsWithin(1, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(InternalServerErrorException.class);
    }


    private void initMockService(@Nonnull final SphereClient sphereClient,
                                 @Nonnull final Map<String, Throwable> errors) {
        shoppingListSyncOptions = ShoppingListSyncOptionsBuilder
                .of(sphereClient)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errors.put(exception.getMessage(), exception);
                })
                .build();
        service = new ShoppingListServiceImpl(shoppingListSyncOptions);
    }

    private void initMockService(@Nonnull final SphereClient sphereClient) {
        shoppingListSyncOptions = ShoppingListSyncOptionsBuilder
                .of(sphereClient)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorMessages.add(exception.getMessage());
                    errorExceptions.add(exception.getCause());
                })
                .build();
        service = new ShoppingListServiceImpl(shoppingListSyncOptions);
    }
}
