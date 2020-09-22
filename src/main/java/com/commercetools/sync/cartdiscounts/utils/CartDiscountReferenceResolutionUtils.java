package com.commercetools.sync.cartdiscounts.utils;

import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;

/**
 * Util class which provides utilities that can be used when syncing resources from a source commercetools project
 * to a target one.
 */
public final class CartDiscountReferenceResolutionUtils {

    /**
     * Returns an {@link List}&lt;{@link CartDiscountDraft}&gt; consisting of the results of applying the
     * mapping from {@link CartDiscount} to {@link CartDiscountDraft} with considering reference resolution.
     *
     * <table summary="Mapping of Reference fields for the reference resolution">
     *   <thead>
     *     <tr>
     *       <th>Reference field</th>
     *       <th>from</th>
     *       <th>to</th>
     *     </tr>
     *   </thead>
     *   <tbody>
     *     <tr>
     *        <td>custom.type</td>
     *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
     *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
     *     </tr>
     *   </tbody>
     * </table>
     *
     * <p><b>Note:</b> The {@link Type} reference should has an expanded reference with a key.
     * Any reference that is not expanded will have its id in place and not replaced by the key will be
     * considered as existing resources on the target commercetools project and
     * the library will issues an update/create API request without reference resolution.
     *
     * @param cartDiscounts the cart discounts with expanded references.
     * @return a {@link List} of {@link CartDiscountDraft} built from the
     *         supplied {@link List} of {@link CartDiscount}.
     */
    @Nonnull
    public static List<CartDiscountDraft> mapToCartDiscountDrafts(
        @Nonnull final List<CartDiscount> cartDiscounts) {
        return cartDiscounts
            .stream()
            .map(CartDiscountReferenceResolutionUtils::mapToCartDiscountDraft)
            .collect(Collectors.toList());
    }

    @Nonnull
    private static CartDiscountDraft mapToCartDiscountDraft(@Nonnull final CartDiscount cartDiscount) {
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
                                       .custom(mapToCustomFieldsDraft(cartDiscount))
                                       .build();
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

    private CartDiscountReferenceResolutionUtils() {
    }
}
