package com.commercetools.sync.services.impl;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.commons.FakeClient;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.services.CartDiscountService;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetDescription;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.InternalServerErrorException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.queries.PagedQueryResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartDiscountServiceImplTest {

    public static final String CART_DISCOUNT_KEY_1 = "key_1";

    @Test
    void fetchCartDiscount_WithEmptyKey_ShouldNotFetchAnyCartDiscount() {
        // preparation
        final SphereClient sphereClient = mock(SphereClient.class);
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(sphereClient)
                .build();
        final CartDiscountService cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);

        // test
        final CompletionStage<Optional<CartDiscount>> result = cartDiscountService.fetchCartDiscount("");


        // assertions
        assertThat(result).isCompletedWithValue(Optional.empty());
        verify(sphereClient, never()).execute(any());
    }

    @Test
    void fetchCartDiscount_WithNullKey_ShouldNotFetchAnyCartDiscount() {
        // preparation
        final SphereClient sphereClient = mock(SphereClient.class);
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(sphereClient)
                .build();
        final CartDiscountService cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);

        // test
        final CompletionStage<Optional<CartDiscount>> result = cartDiscountService.fetchCartDiscount(null);


        // assertions
        assertThat(result).isCompletedWithValue(Optional.empty());
        verify(sphereClient, never()).execute(any());
    }

    @Test
    void fetchCartDiscount_WithValidKey_ShouldReturnMockCartDiscount() {
        // preparation
        final CartDiscount mockCartDiscount = mock(CartDiscount.class);
        when(mockCartDiscount.getId()).thenReturn("testId");
        when(mockCartDiscount.getKey()).thenReturn("any_key");
        final PagedQueryResult<CartDiscount> pagedQueryResult = mock(PagedQueryResult.class);
        when(pagedQueryResult.head()).thenReturn(Optional.of(mockCartDiscount));
        final FakeClient<PagedQueryResult<CartDiscount>> fakeCartDiscountClient = new FakeClient<>(pagedQueryResult);
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(fakeCartDiscountClient)
                .build();
        final CartDiscountService cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);

        // test
        final CompletionStage<Optional<CartDiscount>> result =
                cartDiscountService.fetchCartDiscount("any_key");

        // assertions
        assertThat(result).isCompletedWithValue(Optional.of(mockCartDiscount));
        assertThat(fakeCartDiscountClient.isExecuted()).isTrue();
    }

    @Test
    void createCartDiscount_WithNullCartDiscountKey_ShouldNotCreateCartDiscount() {
        // preparation
        final CartDiscountDraft mockCartDiscountDraft = mock(CartDiscountDraft.class);
        final Map<String, Throwable> errors = new HashMap<>();
        when(mockCartDiscountDraft.getKey()).thenReturn(null);

        final FakeClient<CartDiscount> fakeCartDiscountClient = new FakeClient<>(mock(CartDiscount.class));
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(fakeCartDiscountClient)
                .errorCallback((exception, oldResource, newResource, actions) ->
                        errors.put(exception.getMessage(), exception))
                .build();
        final CartDiscountService cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);

        // test
        final CompletionStage<Optional<CartDiscount>> result = cartDiscountService
            .createCartDiscount(mockCartDiscountDraft);

        // assertions
        assertThat(result).isCompletedWithValue(Optional.empty());
        assertThat(errors.keySet())
            .containsExactly("Failed to create draft with key: 'null'. Reason: Draft key is blank!");
        assertThat(fakeCartDiscountClient.isExecuted()).isFalse();
    }

    @Test
    void createCartDiscount_WithEmptyCartDiscountKey_ShouldHaveEmptyOptionalAsAResult() {
        //preparation
        final FakeClient<CartDiscount> fakeCartDiscountClient = new FakeClient<>(mock(CartDiscount.class));
        final CartDiscountDraft mockCartDiscountDraft = mock(CartDiscountDraft.class);
        final Map<String, Throwable> errors = new HashMap<>();
        when(mockCartDiscountDraft.getKey()).thenReturn("");

        final CartDiscountSyncOptions options = CartDiscountSyncOptionsBuilder
            .of(fakeCartDiscountClient)
            .errorCallback((exception, oldResource, newResource, actions) ->
                    errors.put(exception.getMessage(), exception))
            .build();

        final CartDiscountServiceImpl cartDiscountService = new CartDiscountServiceImpl(options);

        // test
        final CompletionStage<Optional<CartDiscount>> result = cartDiscountService
            .createCartDiscount(mockCartDiscountDraft);

        // assertion
        assertThat(result).isCompletedWithValue(Optional.empty());
        assertThat(errors.keySet())
            .containsExactly("Failed to create draft with key: ''. Reason: Draft key is blank!");
        assertThat(fakeCartDiscountClient.isExecuted()).isFalse();
    }

    @Test
    void createCartDiscount_WithUnsuccessfulMockCtpResponse_ShouldNotCreateCartDiscount() {
        // preparation
        final CartDiscountDraft mockCartDiscountDraft = mock(CartDiscountDraft.class);
        final Map<String, Throwable> errors = new HashMap<>();
        when(mockCartDiscountDraft.getKey()).thenReturn("cartDiscountKey");

        final FakeClient<Throwable> fakeCartDiscountClient = new FakeClient<>(new InternalServerErrorException());

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(fakeCartDiscountClient)
                .errorCallback((exception, oldResource, newResource, actions) ->
                        errors.put(exception.getMessage(), exception))
                .build();

        final CartDiscountService cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);


        // test
        final CompletionStage<Optional<CartDiscount>> result =
                cartDiscountService.createCartDiscount(mockCartDiscountDraft);

        // assertions
        assertThat(result).isCompletedWithValue(Optional.empty());
        assertThat(errors.keySet())
                .hasSize(1)
                .singleElement().satisfies(message -> {
                    assertThat(message).contains("Failed to create draft with key: 'cartDiscountKey'.");
                });

        assertThat(errors.values())
                .hasSize(1)
                .singleElement().satisfies(exception -> {
                    assertThat(exception).isExactlyInstanceOf(SyncException.class);
                    assertThat(exception.getCause()).isExactlyInstanceOf(InternalServerErrorException.class);
                });
    }

    @Test
    void updateCartDiscount_WithMockSuccessfulCtpResponse_ShouldCallCartDiscountUpdateCommand() {
        // preparation
        final CartDiscount mockCartDiscount = mock(CartDiscount.class);
        final FakeClient<CartDiscount> fakeCartDiscountClient = new FakeClient<>(mockCartDiscount);
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(fakeCartDiscountClient)
                .build();

        final CartDiscountService cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);

        final List<UpdateAction<CartDiscount>> updateActions =
                singletonList(SetDescription.of(LocalizedString.ofEnglish("new_desc")));
        // test
        final CompletionStage<CartDiscount> result =
                cartDiscountService.updateCartDiscount(mockCartDiscount, updateActions);

        // assertions
        assertThat(result).isCompletedWithValue(mockCartDiscount);
        assertThat(fakeCartDiscountClient.isExecuted()).isTrue();
    }

    @Test
    void updateCartDiscount_WithMockUnsuccessfulCtpResponse_ShouldCompleteExceptionally() {
        // preparation
        final CartDiscount mockCartDiscount = mock(CartDiscount.class);


        final FakeClient<Throwable> fakeCartDiscountClient = new FakeClient<>(new InternalServerErrorException());
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(fakeCartDiscountClient)
                .build();

        final CartDiscountService cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);

        final List<UpdateAction<CartDiscount>> updateActions =
                singletonList(SetDescription.of(LocalizedString.ofEnglish("new_desc")));
        // test
        final CompletionStage<CartDiscount> result =
                cartDiscountService.updateCartDiscount(mockCartDiscount, updateActions);

        // assertions
        assertThat(result)
                .failsWithin(1, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(InternalServerErrorException.class);
    }

    @Test
    void fetchMatchingCartDiscountsByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
        // Mock sphere client to return BadGatewayException on any request.
        final List<String> errorCallBackMessages = new ArrayList<>();
        final List<Throwable> errorCallBackExceptions = new ArrayList<>();

        FakeClient<CartDiscount> fakeClient = new FakeClient<>(new BadGatewayException());

        final CartDiscountSyncOptions spyOptions =
                CartDiscountSyncOptionsBuilder.of(fakeClient)
                    .errorCallback((exception, oldResource, newResource, actions) -> {
                        errorCallBackMessages.add(exception.getMessage());
                        errorCallBackExceptions.add(exception);
                    })
                    .build();

        final CartDiscountService spyCartDiscountService = new CartDiscountServiceImpl(spyOptions);


        final Set<String> cartDiscountKeys = new HashSet<>();
        cartDiscountKeys.add(CART_DISCOUNT_KEY_1);

        // test and assert
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(spyCartDiscountService.fetchMatchingCartDiscountsByKeys(cartDiscountKeys))
                .failsWithin(1, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(BadGatewayException.class);
    }

}
