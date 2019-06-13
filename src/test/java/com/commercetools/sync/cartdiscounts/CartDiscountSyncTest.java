package com.commercetools.sync.cartdiscounts;

import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import com.commercetools.sync.services.CartDiscountService;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.CartPredicate;
import io.sphere.sdk.cartdiscounts.ShippingCostTarget;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.SphereException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CartDiscountSyncTest {

    private static CartDiscountDraft newCartDiscount;

    @BeforeClass
    public static void setup() {
        newCartDiscount = CartDiscountDraftBuilder
            .of(LocalizedString.of(Locale.GERMAN, "Neu Name", Locale.ENGLISH, "new name"),
                CartPredicate.of("totalPrice >= \"50 EUR\""),
                CartDiscountValue.ofRelative(1000),
                ShippingCostTarget.of(),
                "0.25",
                false)
            .active(false)
            .description(LocalizedString.of(Locale.GERMAN, "Beschreibung",
                Locale.ENGLISH, "description"))
            .validFrom(ZonedDateTime.parse("2019-05-05T00:00:00.000Z"))
            .validUntil(ZonedDateTime.parse("2019-05-15T00:00:00.000Z"))
            .build();
    }

    @Test
    public void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CartDiscountSyncOptions syncOptions = CartDiscountSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();


        final CartDiscountService mockCartDiscountService = mock(CartDiscountService.class);

        when(mockCartDiscountService
            .fetchMatchingCartDiscountsByKeys(singleton(newCartDiscount.getName().get(Locale.ENGLISH))))
            .thenReturn(supplyAsync(() -> {
                throw new SphereException();
            }));

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(syncOptions, mockCartDiscountService);

        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(singletonList(newCartDiscount))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).isEqualTo("Failed to fetch existing cart discounts with keys: '[new name]'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
            });

        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    public void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
        // preparation
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .build();

        final CartDiscountService cartDiscountService = mock(CartDiscountService.class);
        when(cartDiscountService.fetchMatchingCartDiscountsByKeys(anySet())).thenReturn(completedFuture(emptySet()));
        when(cartDiscountService.createCartDiscount(any())).thenReturn(completedFuture(Optional.empty()));

        final CartDiscountSyncOptions spyCartDiscountSyncOptions = spy(cartDiscountSyncOptions);

        // test
        new CartDiscountSync(spyCartDiscountSyncOptions, cartDiscountService)
            .sync(singletonList(newCartDiscount)).toCompletableFuture().join();

        // assertion
        verify(spyCartDiscountSyncOptions).applyBeforeCreateCallBack(newCartDiscount);
        verify(spyCartDiscountSyncOptions, never()).applyBeforeUpdateCallBack(any(), any(), any());
    }

    @Test
    public void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
        // preparation
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .build();

        final CartDiscount mockedExistingCartDiscount = mock(CartDiscount.class);
        when(mockedExistingCartDiscount.getName()).thenReturn(newCartDiscount.getName());

        final CartDiscountService cartDiscountService = mock(CartDiscountService.class);
        when(cartDiscountService.fetchMatchingCartDiscountsByKeys(anySet()))
            .thenReturn(completedFuture(singleton(mockedExistingCartDiscount)));

        when(cartDiscountService.updateCartDiscount(any(), any()))
            .thenReturn(completedFuture(mockedExistingCartDiscount));

        final CartDiscountSyncOptions spyCartDiscountSyncOptions = spy(cartDiscountSyncOptions);

        // test
        new CartDiscountSync(spyCartDiscountSyncOptions, cartDiscountService)
            .sync(singletonList(newCartDiscount)).toCompletableFuture().join();

        // assertion
        verify(spyCartDiscountSyncOptions).applyBeforeUpdateCallBack(any(), any(), any());
        verify(spyCartDiscountSyncOptions, never()).applyBeforeCreateCallBack(newCartDiscount);
    }

    @Test
    public void sync_WithNullDraft_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        //preparation
        final CartDiscountDraft newCartDiscountDraft = null;

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .errorCallback((errorMessage, exception) -> {
                    errorMessages.add(errorMessage);
                    exceptions.add(exception);
                })
                .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        //test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
                .sync(singletonList(newCartDiscountDraft))
                .toCompletableFuture()
                .join();

        //assertions
        assertThat(errorMessages)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(message ->
                        assertThat(message).isEqualTo("Failed to process null cart discount draft.")
                );

        assertThat(exceptions)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(throwable -> assertThat(throwable).isNull());

        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 0, 1);
    }

}
