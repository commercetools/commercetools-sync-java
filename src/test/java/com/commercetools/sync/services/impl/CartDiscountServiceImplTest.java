package com.commercetools.sync.services.impl;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.queries.PagedQueryResultDsl;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.only;

class CartDiscountServiceImplTest {

    private String testCartDiscountKey = "ABC123";

    @Test
    void fetchCartDiscount_WithEmptyKey_ShouldNotFetchAnyCartDiscount() {
        // preparation
        final SphereClient sphereClient = mock(SphereClient.class);
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(sphereClient)
                .build();
        final CartDiscountServiceImpl cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);

        // test
        CompletionStage<Optional<CartDiscount>> result = cartDiscountService.fetchCartDiscount("");


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
        CompletionStage<Optional<CartDiscount>> result = cartDiscountService.fetchCartDiscount(null);


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
        final PagedQueryResult pagedQueryResult = mock(PagedQueryResultDsl.class);
        when(pagedQueryResult.head()).thenReturn(Optional.of(mockCartDiscount));
        when(cartDiscountSyncOptions.getCtpClient().execute(any(CartDiscountQuery.class))).thenReturn(CompletableFuture
                .completedFuture(pagedQueryResult));

        // test
        CompletionStage<Optional<CartDiscount>> result = cartDiscountService.fetchCartDiscount(testCartDiscountKey);


        // assertions
        assertThat(result).isCompletedWithValue(Optional.of(mockCartDiscount));
        verify(sphereClient, only()).execute(any());
    }

    @Test
    void createCartDiscount_WithEmptyCartDiscountKey_ShouldNotCreateCartDiscount() {
        // preparation
        final SphereClient sphereClient = mock(SphereClient.class);
        final CartDiscountDraft mockCartDiscountDraft = mock(CartDiscountDraft.class);
        final Map<String, Throwable> errors = new HashMap<>();
        when(mockCartDiscountDraft.getName()).thenReturn(LocalizedString.ofEnglish(""));

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(sphereClient)
                .errorCallback(errors::put)
                .build();
        final CartDiscountService cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);

        // test
        CompletionStage<Optional<CartDiscount>> result = cartDiscountService.createCartDiscount(mockCartDiscountDraft);

        // assertions
        assertThat(result).isCompletedWithValue(Optional.empty());
        assertThat(errors).isNotEmpty();
        assertThat(errors.size()).isEqualTo(1);
        assertTrue(errors.keySet().stream().anyMatch(e -> e.contains("Draft key is blank!")));
        verify(sphereClient, times(0)).execute(any());

    }

}
