package com.commercetools.sync.integration.ctpprojectsource.cartdiscounts;

import com.commercetools.sync.cartdiscounts.CartDiscountSync;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import io.sphere.sdk.cartdiscounts.AbsoluteCartDiscountValue;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.CartPredicate;
import io.sphere.sdk.cartdiscounts.ShippingCostTarget;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.deleteCartDiscountsFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.populateSourceProject;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.populateTargetProject;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static org.assertj.core.api.Assertions.assertThat;

class CartDiscountSyncIT {

    @BeforeEach
    void setup() {
        deleteTypesFromTargetAndSource();
        deleteCartDiscountsFromTargetAndSource();
        populateSourceProject();
        populateTargetProject();
    }

    @AfterAll
    static void tearDown() {
        deleteTypesFromTargetAndSource();
        deleteCartDiscountsFromTargetAndSource();
    }

    @Test
    void sync_WithoutUpdates_ShouldReturnProperStatistics() {
        // preparation
        final List<CartDiscount> cartDiscounts = CTP_SOURCE_CLIENT
            .execute(CartDiscountQuery.of())
            .toCompletableFuture().join().getResults();

        final List<CartDiscountDraft> cartDiscountDrafts = cartDiscounts
            .stream()
            .map(cartDiscount ->
                CartDiscountDraftBuilder.of(cartDiscount.getName(),
                    cartDiscount.getCartPredicate(),
                    cartDiscount.getValue(),
                    cartDiscount.getTarget(),
                    cartDiscount.getSortOrder(),
                    cartDiscount.isRequiringDiscountCode())
                                        .key(cartDiscount.getKey())
                                        .active(cartDiscount.isActive())
                                        .description(cartDiscount.getDescription())
                                        .validFrom(cartDiscount.getValidFrom())
                                        .validUntil(cartDiscount.getValidUntil())
                                        .build())
            .collect(Collectors.toList());

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((error, throwable) -> {
                errorMessages.add(error);
                exceptions.add(throwable);
            })
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(cartDiscountDrafts)
            .toCompletableFuture().join();

        // assertion
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(cartDiscountSyncStatistics).hasValues(2, 1, 0, 0);
        assertThat(cartDiscountSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 2 cart discounts were processed in total"
                + " (1 created, 0 updated and 0 failed to sync).");

    }

    @Test
    void sync_WithUpdates_ShouldReturnProperStatistics() {
        // preparation
        final List<CartDiscount> cartDiscounts = CTP_SOURCE_CLIENT
            .execute(CartDiscountQuery.of())
            .toCompletableFuture().join().getResults();

        final List<CartDiscountDraft> typeDrafts = cartDiscounts
            .stream()
            .map(cartDiscount ->
                CartDiscountDraftBuilder.of(cartDiscount.getName(),
                    CartPredicate.of("totalPrice >= \"100 EUR\""), //new cart predicate
                    AbsoluteCartDiscountValue.of(MoneyImpl.of(40, EUR)), //new value
                    ShippingCostTarget.of(), //new target
                    cartDiscount.getSortOrder(),
                    cartDiscount.isRequiringDiscountCode())
                                        .key(cartDiscount.getKey())
                                        .active(cartDiscount.isActive())
                                        .description(cartDiscount.getDescription())
                                        .validFrom(cartDiscount.getValidFrom())
                                        .validUntil(cartDiscount.getValidUntil())
                                        .build())
            .collect(Collectors.toList());


        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((error, throwable) -> {
                errorMessages.add(error);
                exceptions.add(throwable);
            })
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(typeDrafts)
            .toCompletableFuture().join();

        // assertion
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(cartDiscountSyncStatistics).hasValues(2, 1, 1, 0);
        assertThat(cartDiscountSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 2 cart discounts were processed in total"
                + " (1 created, 1 updated and 0 failed to sync).");
    }

}
