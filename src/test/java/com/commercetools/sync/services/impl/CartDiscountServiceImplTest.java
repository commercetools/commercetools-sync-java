package com.commercetools.sync.services.impl;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.services.CartDiscountService;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountUpdateCommand;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetDescription;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscountTarget;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.CartPredicate;
import io.sphere.sdk.cartdiscounts.LineItemsTarget;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.client.InternalServerErrorException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.only;

class CartDiscountServiceImplTest {

    private String testCartDiscountKey = "ABC123";

    private static final String PREDICATE_1 = "1 = 1";
    private static final CartPredicate CART_DISCOUNT_CART_PREDICATE_1 = CartPredicate.of(PREDICATE_1);
    private static final CartDiscountValue CART_DISCOUNT_VALUE_1 = CartDiscountValue.ofRelative(1000);
    private static final CartDiscountTarget CART_DISCOUNT_TARGET_1 = LineItemsTarget.ofAll();
    private static final String SORT_ORDER_2 = "0.2";

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
        final SphereClient sphereClient = mock(SphereClient.class);
        final CartDiscount mockCartDiscount = mock(CartDiscount.class);
        when(mockCartDiscount.getId()).thenReturn("testId");
        when(mockCartDiscount.getName()).thenReturn(LocalizedString.ofEnglish("eng"));
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(sphereClient)
                .build();
        final CartDiscountService cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);

        @SuppressWarnings("unchecked")
        final PagedQueryResult<CartDiscount> pagedQueryResult =  mock(PagedQueryResult.class);
        when(pagedQueryResult.head()).thenReturn(Optional.of(mockCartDiscount));
        when(cartDiscountSyncOptions.getCtpClient().execute(any(CartDiscountQuery.class)))
                .thenReturn(completedFuture(pagedQueryResult));

        // test
        final CompletionStage<Optional<CartDiscount>> result =
                cartDiscountService.fetchCartDiscount(testCartDiscountKey);


        // assertions
        assertThat(result).isCompletedWithValue(Optional.of(mockCartDiscount));
        verify(sphereClient, only()).execute(any());
    }

    @Test
    void createCartDiscount_WithEmptyCartDiscountKey_ShouldNotCreateCartDiscount() {
        // preparation
        final CartDiscountDraft mockCartDiscountDraft = mock(CartDiscountDraft.class);
        final Map<String, Throwable> errors = new HashMap<>();
        when(mockCartDiscountDraft.getName()).thenReturn(LocalizedString.ofEnglish(""));

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .errorCallback(errors::put)
                .build();
        final CartDiscountService cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);

        // test
        final CompletionStage<Optional<CartDiscount>> result =
                cartDiscountService.createCartDiscount(mockCartDiscountDraft);

        // assertions
        assertThat(result).isCompletedWithValue(Optional.empty());
        assertThat(errors).isNotEmpty();
        assertThat(errors.size()).isEqualTo(1);
        assertTrue(errors.keySet().stream().anyMatch(e -> e.contains("Draft key is blank!")));
        verify(cartDiscountSyncOptions.getCtpClient(), times(0)).execute(any());

    }

    @Test
    void createCartDiscount_WithUnsuccessfulMockCtpResponse_ShouldNotCreateCartDiscount() {
        // preparation
        final CartDiscountDraft mockCartDiscountDraft = mock(CartDiscountDraft.class);
        final Map<String, Throwable> errors = new HashMap<>();
        when(mockCartDiscountDraft.getName()).thenReturn(LocalizedString.ofEnglish("cartDiscountKey"));

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .errorCallback(errors::put)
                .build();

        final CartDiscountService cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);

        when(cartDiscountSyncOptions.getCtpClient().execute(any()))
                .thenReturn(CompletableFutureUtils.failed(new InternalServerErrorException()));

        // test
        final CompletionStage<Optional<CartDiscount>> result =
                cartDiscountService.createCartDiscount(mockCartDiscountDraft);

        // assertions
        assertThat(result).isCompletedWithValue(Optional.empty());
        assertThat(errors.keySet())
                .hasSize(1)
                .hasOnlyOneElementSatisfying(message -> {
                    assertThat(message).contains("Failed to create draft with key: 'cartDiscountKey'.");
                });

        assertThat(errors.values())
                .hasSize(1)
                .hasOnlyOneElementSatisfying(exception ->
                        assertThat(exception).isExactlyInstanceOf(InternalServerErrorException.class));


    }

    @Test
    void updateCartDiscount_WithMockSuccessfulCtpResponse_ShouldCallCartDiscountUpdateCommand() {
        // preparation
        final CartDiscount mockCartDiscount = mock(CartDiscount.class);
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .build();

        when(cartDiscountSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(mockCartDiscount));
        final CartDiscountService cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);

        final List<UpdateAction<CartDiscount>> updateActions =
                singletonList(SetDescription.of(LocalizedString.ofEnglish("new_desc")));
        // test
        final CompletionStage<CartDiscount> result =
                cartDiscountService.updateCartDiscount(mockCartDiscount, updateActions);

        // assertions
        assertThat(result).isCompletedWithValue(mockCartDiscount);
        verify(cartDiscountSyncOptions.getCtpClient())
                .execute(eq(CartDiscountUpdateCommand.of(mockCartDiscount, updateActions)));
    }

    @Test
    void updateCartDiscount_WithMockUnsuccessfulCtpResponse_ShouldCompleteExceptionally() {
        // preparation
        final CartDiscount mockCartDiscount = mock(CartDiscount.class);
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .build();

        when(cartDiscountSyncOptions.getCtpClient().execute(any()))
                .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new InternalServerErrorException()));

        final CartDiscountService cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);

        final List<UpdateAction<CartDiscount>> updateActions =
                singletonList(SetDescription.of(LocalizedString.ofEnglish("new_desc")));
        // test
        final CompletionStage<CartDiscount> result =
                cartDiscountService.updateCartDiscount(mockCartDiscount, updateActions);

        // assertions
        assertThat(result).hasFailedWithThrowableThat()
                .isExactlyInstanceOf(InternalServerErrorException.class);
    }


    @Test
    void createCartDiscount_WithInvalidCartDiscount_ShouldHaveEmptyOptionalAsAResult() {
        //preparation
        final SphereClient sphereClient = mock(SphereClient.class);
        final Map<String, Throwable> errors = new HashMap<>();
        final CartDiscountDraft newCartDiscountDraft =
                CartDiscountDraftBuilder.of(LocalizedString.of(Locale.ENGLISH, ""),
                        CART_DISCOUNT_CART_PREDICATE_1,
                        CART_DISCOUNT_VALUE_1,
                        CART_DISCOUNT_TARGET_1,
                        SORT_ORDER_2,
                        false)
                        .active(false)
                        .build();

        final CartDiscountSyncOptions options = CartDiscountSyncOptionsBuilder
                .of(sphereClient)
                .errorCallback(errors::put)
                .build();

        final CartDiscountServiceImpl cartDiscountService = new CartDiscountServiceImpl(options);

        // test
        final Optional<CartDiscount> result =
                cartDiscountService.createCartDiscount(newCartDiscountDraft)
                        .toCompletableFuture().join();

        // assertion
        assertThat(result).isEmpty();
        assertThat(errors.keySet())
                .containsExactly("Failed to create draft with key: ''. Reason: Draft key is blank!");

    }

}

