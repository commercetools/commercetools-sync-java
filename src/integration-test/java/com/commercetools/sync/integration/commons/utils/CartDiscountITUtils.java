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
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.utils.MoneyImpl;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static com.commercetools.sync.integration.commons.utils.ITUtils.createCustomFieldsJsonMap;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createTypeIfNotAlreadyExisting;
import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public final class CartDiscountITUtils {

    public static final String CART_DISCOUNT_KEY_1 = "key_1";
    public static final String CART_DISCOUNT_KEY_2 = "key_2";

    public static final LocalizedString CART_DISCOUNT_NAME_1 = LocalizedString.of(Locale.ENGLISH, "name_1");
    public static final LocalizedString CART_DISCOUNT_NAME_2 = LocalizedString.of(Locale.ENGLISH, "name_2");

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

    public static final String OLD_CART_DISCOUNT_TYPE_KEY = "oldCartDiscountCustomTypeKey";
    public static final String OLD_CART_DISCOUNT_TYPE_NAME = "oldCartDiscountCustomTypeName";

    public static final CartDiscountDraft CART_DISCOUNT_DRAFT_1 =
        CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_1,
            CART_DISCOUNT_CART_PREDICATE_1,
            CART_DISCOUNT_VALUE_1,
            CART_DISCOUNT_TARGET_1,
            SORT_ORDER_1,
            false)
                                .key(CART_DISCOUNT_KEY_1)
                                .active(false)
                                .description(CART_DISCOUNT_DESC_1)
                                .validFrom(JANUARY_FROM)
                                .validUntil(JANUARY_UNTIL)
                                .custom(getCustomFieldsDraft())
                                .build();

    public static final CartDiscountDraft CART_DISCOUNT_DRAFT_2 =
        CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_2,
            CART_DISCOUNT_CART_PREDICATE_2,
            CART_DISCOUNT_VALUE_2,
            CART_DISCOUNT_TARGET_2,
            SORT_ORDER_2,
            false)
                                .key(CART_DISCOUNT_KEY_2)
                                .active(false)
                                .description(CART_DISCOUNT_DESC_2)
                                .validFrom(FEBRUARY_FROM)
                                .validUntil(FEBRUARY_UNTIL)
                                .custom(getCustomFieldsDraft())
                                .build();


    public static CustomFieldsDraft getCustomFieldsDraft() {
        return CustomFieldsDraft.ofTypeKeyAndJson(OLD_CART_DISCOUNT_TYPE_KEY, createCustomFieldsJsonMap());
    }

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
        createCartDiscountCustomType(OLD_CART_DISCOUNT_TYPE_KEY,
            Locale.ENGLISH,
            OLD_CART_DISCOUNT_TYPE_NAME,
            CTP_SOURCE_CLIENT);

        CompletableFuture.allOf(
                CTP_SOURCE_CLIENT.execute(CartDiscountCreateCommand.of(CART_DISCOUNT_DRAFT_1)).toCompletableFuture(),
                CTP_SOURCE_CLIENT.execute(CartDiscountCreateCommand.of(CART_DISCOUNT_DRAFT_2)).toCompletableFuture())
                .join();
    }

    private static Type createCartDiscountCustomType(@Nonnull final String typeKey,
                                                     @Nonnull final Locale locale,
                                                     @Nonnull final String name,
                                                     @Nonnull final SphereClient ctpClient) {

        return createTypeIfNotAlreadyExisting(
            typeKey,
            locale,
            name,
            ResourceTypeIdsSetBuilder.of().add("cart-discount"),
            ctpClient);
    }

    public static void populateTargetProject() {
        createCartDiscountCustomType(OLD_CART_DISCOUNT_TYPE_KEY,
            Locale.ENGLISH,
            OLD_CART_DISCOUNT_TYPE_NAME,
            CTP_TARGET_CLIENT);
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
            .of()
            .plusPredicates(queryModel -> queryModel.key().is(key))
            .build();

        return sphereClient.execute(query).toCompletableFuture().join().head();
    }

    /**
     * Builds a list of odd sortOrder strings that start with `0.01`. They are odd because
     * because sortOrder in CTP is not allowed to end with a zero and should be decimal value between 0 and 1.
     *
     * @param capacity the number of sort orders to build.
     */
    public static List<String> getSortOrders(final int capacity) {
        return IntStream.range(0, capacity * 2)
                .filter(index -> index % 2 != 0)
                .mapToObj(oddNumber -> format("0.0%s", oddNumber))
                .collect(toList());
    }

    private CartDiscountITUtils() {
    }
}
