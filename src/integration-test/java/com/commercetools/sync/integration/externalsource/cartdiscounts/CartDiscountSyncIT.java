package com.commercetools.sync.integration.externalsource.cartdiscounts;

import com.commercetools.sync.cartdiscounts.CartDiscountSync;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.models.LocalizedString;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_CART_PREDICATE_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_CART_PREDICATE_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_DESC_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_DESC_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_DRAFT_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_KEY_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_KEY_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_TARGET_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_TARGET_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_VALUE_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_VALUE_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.JANUARY_FROM;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.JANUARY_UNTIL;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.PREDICATE_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.SORT_ORDER_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.deleteCartDiscountsFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.getCartDiscountByKey;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.populateSourceProject;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.populateTargetProject;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class CartDiscountSyncIT {

    /**
     * Deletes types from the target CTP projects.
     * Populates the target CTP project with test data.
     */
    @Before
    public void setup() {
        deleteTypesFromTargetAndSource();
        deleteCartDiscountsFromTargetAndSource();
        populateSourceProject();
        populateTargetProject();
    }

    @Test
    public void sync_WithUpdatedCartDiscount_WithNewCartPredicate_ShouldUpdateCartDiscountWithNewCartPredicate() {
        // preparation
        final Optional<CartDiscount> oldCartDiscountBefore =
            getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_1);
        assertThat(oldCartDiscountBefore).isNotEmpty();

        final CartDiscountDraft newCartDiscountDraftWithExistingKey =
            CartDiscountDraftBuilder.of(LocalizedString.of(Locale.ENGLISH, CART_DISCOUNT_KEY_1),
                CART_DISCOUNT_CART_PREDICATE_2,
                CART_DISCOUNT_VALUE_1,
                CART_DISCOUNT_TARGET_1,
                SORT_ORDER_1,
                false)
                                    .active(false)
                                    .description(CART_DISCOUNT_DESC_2)
                                    .validFrom(JANUARY_FROM)
                                    .validUntil(JANUARY_UNTIL)
                                    .build();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);

        final Optional<CartDiscount> oldCartDiscountAfter =
            getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_1);

        assertThat(oldCartDiscountAfter).isNotEmpty();
        assertThat(oldCartDiscountAfter).hasValueSatisfying(cartDiscount ->
            assertThat(cartDiscount.getCartPredicate()).isEqualTo(PREDICATE_2));
    }

    @Test
    public void sync_WithUpdatedCartDiscount_WithNewValue_ShouldUpdateCartDiscountWithNewValue() {
        // preparation
        final Optional<CartDiscount> oldCartDiscountBefore =
            getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_1);
        assertThat(oldCartDiscountBefore).isNotEmpty();

        final CartDiscountDraft newCartDiscountDraftWithExistingKey =
            CartDiscountDraftBuilder.of(LocalizedString.of(Locale.ENGLISH, CART_DISCOUNT_KEY_1),
                CART_DISCOUNT_CART_PREDICATE_1,
                CART_DISCOUNT_VALUE_2,
                CART_DISCOUNT_TARGET_1,
                SORT_ORDER_1,
                false)
                                    .active(false)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .validFrom(JANUARY_FROM)
                                    .validUntil(JANUARY_UNTIL)
                                    .build();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);

        final Optional<CartDiscount> oldCartDiscountAfter =
            getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_1);

        assertThat(oldCartDiscountAfter).isNotEmpty();
        assertThat(oldCartDiscountAfter).hasValueSatisfying(cartDiscount ->
            assertThat(cartDiscount.getValue()).isEqualTo(CART_DISCOUNT_VALUE_2));
    }

    @Test
    public void sync_WithUpdatedCartDiscount_WithNewTarget_ShouldUpdateCartDiscountWithNewTarget() {
        // preparation
        final Optional<CartDiscount> oldCartDiscountBefore =
            getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_1);
        assertThat(oldCartDiscountBefore).isNotEmpty();

        final CartDiscountDraft newCartDiscountDraftWithExistingKey =
            CartDiscountDraftBuilder.of(LocalizedString.of(Locale.ENGLISH, CART_DISCOUNT_KEY_1),
                CART_DISCOUNT_CART_PREDICATE_1,
                CART_DISCOUNT_VALUE_1,
                CART_DISCOUNT_TARGET_2,
                "0.1",
                false)
                                    .active(false)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .validFrom(JANUARY_FROM)
                                    .validUntil(JANUARY_UNTIL)
                                    .build();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);

        final Optional<CartDiscount> oldCartDiscountAfter =
            getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_1);

        assertThat(oldCartDiscountAfter).isNotEmpty();
        assertThat(oldCartDiscountAfter).hasValueSatisfying(cartDiscount ->
            assertThat(cartDiscount.getTarget()).isEqualTo(CART_DISCOUNT_TARGET_2));
    }

    @Test
    public void sync_WithNewCartDiscount_ShouldCreateNewDiscount() {
        //preparation
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(singletonList(CART_DISCOUNT_DRAFT_2))
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(cartDiscountSyncStatistics).hasValues(1, 1, 0, 0);

        final Optional<CartDiscount> cartDiscountAfterCreation =
            getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_2);

        assertThat(cartDiscountAfterCreation).isNotEmpty();
        assertThat(cartDiscountAfterCreation).hasValueSatisfying(cartDiscount -> {
            assertThat(cartDiscount.getName()).isEqualTo(LocalizedString.of(Locale.ENGLISH, CART_DISCOUNT_KEY_2));
            assertThat(cartDiscount.getDescription()).isEqualTo(CART_DISCOUNT_DESC_2);
            assertThat(cartDiscount.getCartPredicate()).isEqualTo(PREDICATE_2);
            assertThat(cartDiscount.getValue()).isEqualTo(CART_DISCOUNT_VALUE_2);
        });
    }


}
