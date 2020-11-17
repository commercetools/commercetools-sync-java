package com.commercetools.sync.cartdiscounts;


import io.sphere.sdk.cartdiscounts.CartPredicate;
import io.sphere.sdk.cartdiscounts.CartDiscountTarget;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.LineItemsTarget;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.MoneyImpl;

import java.time.ZonedDateTime;
import java.util.Locale;

import static com.commercetools.sync.commons.MockUtils.createCustomFieldsJsonMap;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;


public class CartDiscountSyncMockUtils {

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
}
