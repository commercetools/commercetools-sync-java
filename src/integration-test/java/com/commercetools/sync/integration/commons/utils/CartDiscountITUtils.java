package com.commercetools.sync.integration.commons.utils;

import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscountTarget;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.CartPredicate;
import io.sphere.sdk.cartdiscounts.LineItemsTarget;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountCreateCommand;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountDeleteCommand;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQueryBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.utils.MoneyImpl;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;

public class CartDiscountITUtils {

    public static final String CART_DISCOUNT_KEY_1 = "key_1";
    public static final String CART_DISCOUNT_KEY_2 = "key_2";

    public static final LocalizedString CART_DISCOUNT_DESC_1 =
        LocalizedString.of(Locale.ENGLISH, "discount- get 10 percent");
    public static final LocalizedString CART_DISCOUNT_DESC_2 =
        LocalizedString.of(Locale.ENGLISH, "discount- get 20 EUR for special items");

    public static final String PREDICATE_1 = "1 = 1";
    public static final String PREDICATE_2 = "lineItemExists(sku = \"0123456789\" or sku = \"0246891213\") = true";

    public static final CartPredicate CART_DISCOUNT_CART_PREDICATE_1 = CartPredicate.of(PREDICATE_1);
    public static final CartPredicate CART_DISCOUNT_CART_PREDICATE_2 = CartPredicate.of(PREDICATE_2);

    public static final CartDiscountValue CART_DISCOUNT_VALUE_1 = CartDiscountValue.ofRelative(1000);
    public static final CartDiscountValue CART_DISCOUNT_VALUE_2 = CartDiscountValue.ofAbsolute(MoneyImpl.of(20, EUR));

    public static final CartDiscountTarget CART_DISCOUNT_TARGET_1 = LineItemsTarget.ofAll();
    public static final CartDiscountTarget CART_DISCOUNT_TARGET_2 =
        LineItemsTarget.of("sku = \"0123456789\" or sku = \"0246891213\"");

    public static final ZonedDateTime JANUARY_FROM = ZonedDateTime.parse("2019-01-01T00:00:00.000Z");
    public static final ZonedDateTime JANUARY_UNTIL = ZonedDateTime.parse("2019-01-31T00:00:00.000Z");
    public static final ZonedDateTime FEBRUARY_FROM = ZonedDateTime.parse("2019-02-01T00:00:00.000Z");
    public static final ZonedDateTime FEBRUARY_UNTIL = ZonedDateTime.parse("2019-02-28T00:00:00.000Z");

    public static final String SORT_ORDER_1 = "0.1";
    public static final String SORT_ORDER_2 = "0.2";

    public static final CartDiscountDraft CART_DISCOUNT_DRAFT_1 =
        CartDiscountDraftBuilder.of(LocalizedString.of(Locale.ENGLISH, CART_DISCOUNT_KEY_1),
            CART_DISCOUNT_CART_PREDICATE_1,
            CART_DISCOUNT_VALUE_1,
            CART_DISCOUNT_TARGET_1,
            SORT_ORDER_1,
            false)
                                .active(false)
                                .description(CART_DISCOUNT_DESC_1)
                                .validFrom(JANUARY_FROM)
                                .validUntil(JANUARY_UNTIL)
                                .build();

    public static final CartDiscountDraft CART_DISCOUNT_DRAFT_2 =
        CartDiscountDraftBuilder.of(LocalizedString.of(Locale.ENGLISH, CART_DISCOUNT_KEY_2),
            CART_DISCOUNT_CART_PREDICATE_2,
            CART_DISCOUNT_VALUE_2,
            CART_DISCOUNT_TARGET_2,
            SORT_ORDER_2,
            false)
                                .active(false)
                                .description(CART_DISCOUNT_DESC_2)
                                .validFrom(FEBRUARY_FROM)
                                .validUntil(FEBRUARY_UNTIL)
                                .build();


    /**
     * Deletes all cart discounts from CTP project, represented by provided {@code ctpClient}.
     *
     * @param ctpClient represents the CTP project the cart discounts will be deleted from.
     */
    public static void deleteCartDiscounts(@Nonnull final SphereClient ctpClient) {
        queryAndExecute(ctpClient, CartDiscountQuery.of(), CartDiscountDeleteCommand::of);
    }

    /**
     * Deletes all cart discounts from CTP projects defined by {@code CTP_SOURCE_CLIENT} and {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteCartDiscountsFromTargetAndSource() {
        deleteCartDiscounts(CTP_SOURCE_CLIENT);
        deleteCartDiscounts(CTP_TARGET_CLIENT);
    }


    public static void populateSourceProject() {
        CTP_SOURCE_CLIENT.execute(CartDiscountCreateCommand.of(CART_DISCOUNT_DRAFT_1)).toCompletableFuture().join();
        CTP_SOURCE_CLIENT.execute(CartDiscountCreateCommand.of(CART_DISCOUNT_DRAFT_2)).toCompletableFuture().join();
    }

    public static void populateTargetProject() {
        CTP_TARGET_CLIENT.execute(CartDiscountCreateCommand.of(CART_DISCOUNT_DRAFT_1)).toCompletableFuture().join();
    }

    /**
     * Tries to fetch cart discount of {@code key} using {@code sphereClient}.
     *
     * @param sphereClient sphere client used to execute requests.
     * @param key          key of requested cart discount.
     * @return {@link Optional} which may contain type of {@code key}.
     */
    public static Optional<CartDiscount> getCartDiscountByKey(
        @Nonnull final SphereClient sphereClient,
        @Nonnull final String key) {

        final CartDiscountQuery query = CartDiscountQueryBuilder
            .of() //todo: SUPPORT-4443 need to be merged from name to key.
            .plusPredicates(queryModel -> queryModel.name().lang(Locale.ENGLISH).is(key))
            .build();

        return sphereClient.execute(query).toCompletableFuture().join().head();
    }

    public static List<String> getSortOrders(int capacity) {
        // get 100 sortOrder list with odd numbers ex: 0.01, 0.03..
        // The sortOrder must be a decimal value > 0 and < 1. It is not allowed to end with a zero.
        final List<String> sortOrders = new ArrayList<>(capacity);
        int counter = 0;
        int current = 1;

        while (counter != capacity) {
            current += 2;
            sortOrders.add("0.0" + current);
            counter ++;
        }

        return sortOrders;
    }

}
