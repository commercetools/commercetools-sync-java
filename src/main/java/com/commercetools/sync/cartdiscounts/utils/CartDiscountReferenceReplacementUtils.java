package com.commercetools.sync.cartdiscounts.utils;

import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceReplacementUtils.replaceCustomTypeIdWithKeys;

/**
 * Util class which provides utilities that can be used when syncing resources from a source commercetools project
 * to a target one.
 */
public final class CartDiscountReferenceReplacementUtils {

    /**
     * Takes a list of CartDiscounts that are supposed to have their custom type references expanded in order to be able
     * to fetch the keys and replace the resource identifier ids with the corresponding keys and then return a new list
     * of cartDiscountDrafts with their resource identifiers containing keys instead of the ids.
     * Note that if the references are not expanded for a cart discount, the resource identifier ids will
     * not be replaced with keys and will still have their ids in place.
     *
     * @param cartDiscounts the cartDiscounts to replace their resource identifier ids with keys
     * @return a list of cartDiscountsDrafts with keys set on the resource identifiers.
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

    /**
     * Builds a {@link CartDiscountQuery} for fetching cart discounts from a source CTP project with all the needed
     * references expanded for the sync:
     * <ul>
     *     <li>Custom Type</li>
     * </ul>
     *
     * <p>Note: Please only use this util if you desire to sync all the aforementioned references from
     * a source commercetools project. Otherwise, it is more efficient to build the query without expansions, if they
     * are not needed, to avoid unnecessarily bigger payloads fetched from the source project.
     *
     * @return the query for fetching cart discounts from the source CTP project with all the aforementioned references
     *         expanded.
     */
    public static CartDiscountQuery buildCartDiscountQuery() {
        return CartDiscountQuery.of().withExpansionPaths(ExpansionPath.of("custom.type"));
    }



    private CartDiscountReferenceReplacementUtils() {
    }
}
