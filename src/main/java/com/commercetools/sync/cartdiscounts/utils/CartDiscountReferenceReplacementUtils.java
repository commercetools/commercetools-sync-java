package com.commercetools.sync.cartdiscounts.utils;

import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceReplacementUtils.replaceCustomTypeIdWithKeys;

public final class CartDiscountReferenceReplacementUtils {

    /**
     * Takes a list of Categories that are supposed to have their custom type, parent category and asset custom type
     * references expanded in order to be able to fetch the keys and replace the resource identifier ids with the
     * corresponding keys and then return a new list of category drafts with their resource identifiers containing keys
     * instead of the ids. Note that if the references are not expanded for a category, the resource identifier ids will
     * not be replaced with keys and will still have their ids in place.
     *
     * @param cartDiscounts the categories to replace their resource identifier ids with keys
     * @return a list of category drafts with keys set on the resource identifiers.
     */
    @Nonnull
    public static List<CartDiscountDraft> replaceCartDiscountsReferenceIdsWithKeys(
        @Nonnull final List<CartDiscount> cartDiscounts) {
        return cartDiscounts
            .stream()
            .map(cartDiscount -> {
                final CustomFieldsDraft customTypeWithKeysInReference = replaceCustomTypeIdWithKeys(cartDiscount);

                return CartDiscountDraftBuilder.of(
                    cartDiscount.getCartPredicate(),
                    cartDiscount.getName(),
                    cartDiscount.isRequiringDiscountCode(),
                    cartDiscount.getSortOrder(),
                    cartDiscount.getTarget(),
                    cartDiscount.getValue())
                                               .key(cartDiscount.getKey())
                                               .description(cartDiscount.getDescription())
                                               .active(cartDiscount.isActive())
                                               .validFrom(cartDiscount.getValidFrom())
                                               .validUntil(cartDiscount.getValidUntil())
                                               .stackingMode(cartDiscount.getStackingMode())
                                               .custom(customTypeWithKeysInReference)
                                               .build();
            })
            .collect(Collectors.toList());
    }
    

    private CartDiscountReferenceReplacementUtils() {
    }
}
