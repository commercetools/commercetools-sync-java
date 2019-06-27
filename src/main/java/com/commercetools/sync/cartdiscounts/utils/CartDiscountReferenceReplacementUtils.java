package com.commercetools.sync.cartdiscounts.utils;

import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.GiftLineItemCartDiscountValue;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceReplacementUtils.replaceCustomTypeIdWithKeys;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKeyReplaced;

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


    @Nonnull
    private static CartDiscountValue replaceCartDiscountValueReferenceIdsWithKeys(
        @Nonnull final CartDiscountValue cartDiscountValue) {

        if (cartDiscountValue instanceof GiftLineItemCartDiscountValue) {

            final GiftLineItemCartDiscountValue giftLineItemCartDiscountValue =
                (GiftLineItemCartDiscountValue) cartDiscountValue;

            return replaceGiftLineItemCartDiscountValueReferenceIdsWithKeys(giftLineItemCartDiscountValue);
        }

        return cartDiscountValue;
    }

    @Nonnull
    private static GiftLineItemCartDiscountValue replaceGiftLineItemCartDiscountValueReferenceIdsWithKeys(
        @Nonnull final GiftLineItemCartDiscountValue giftLineItemCartDiscountValue) {

        final ResourceIdentifier<Product> productWithKeyInReference =
            replaceProductReferenceIdWithKey(giftLineItemCartDiscountValue);
        final ResourceIdentifier<Channel> distributionChannelWithKeyInReference =
            replaceDistributionChannelReferenceIdWithKey(giftLineItemCartDiscountValue);
        final ResourceIdentifier<Channel> supplyChannelWithKeyInReference =
            replaceSupplyChannelReferenceIdWithKey(giftLineItemCartDiscountValue);

        return CartDiscountValue.ofGiftLineItem(
            productWithKeyInReference,
            giftLineItemCartDiscountValue.getVariantId(),
            supplyChannelWithKeyInReference,
            distributionChannelWithKeyInReference);
    }

    @Nullable
    @SuppressWarnings("ConstantConditions") // NPE checked in replaceReferenceIdWithKey
    private static ResourceIdentifier<Channel> replaceDistributionChannelReferenceIdWithKey(
        @Nonnull final GiftLineItemCartDiscountValue giftLineItemCartDiscountValue) {

        final Reference<Channel> distributionChannel = (Reference)
            giftLineItemCartDiscountValue.getDistributionChannel();

        if (distributionChannel != null) {
            return getResourceIdentifierWithKeyReplaced(distributionChannel,
                () -> ResourceIdentifier.ofId(distributionChannel.getObj().getKey()));
        }

        return distributionChannel;
    }

    @Nullable
    @SuppressWarnings("ConstantConditions") // NPE checked in replaceReferenceIdWithKey
    private static ResourceIdentifier<Channel> replaceSupplyChannelReferenceIdWithKey(
        @Nonnull final GiftLineItemCartDiscountValue giftLineItemCartDiscountValue) {

        final Reference<Channel> supplyChannel = (Reference) giftLineItemCartDiscountValue.getSupplyChannel();

        if (supplyChannel != null) {
            return getResourceIdentifierWithKeyReplaced(supplyChannel,
                () -> ResourceIdentifier.ofId(supplyChannel.getObj().getKey()));
        }

        return supplyChannel;
    }

    @Nullable
    @SuppressWarnings("ConstantConditions") // NPE checked in replaceReferenceIdWithKey
    private static ResourceIdentifier<Product> replaceProductReferenceIdWithKey(
        @Nonnull final GiftLineItemCartDiscountValue giftLineItemCartDiscountValue) {
        final Reference<Product> product = (Reference) giftLineItemCartDiscountValue.getProduct();
        return getResourceIdentifierWithKeyReplaced(product, () -> ResourceIdentifier.ofId(product.getObj().getKey()));
    }

    /**
     * Builds a {@link CartDiscountQuery} for fetching cart discounts from a source CTP project with all the needed
     * references expanded for the sync:
     * <ul>
     *     <li>Custom Type</li>
     *     <li>Gift lineItem distribution channel</li>
     *     <li>Gift lineItem supply channel</li>
     *     <li>Gift lineItem product reference</li>
     * </ul>
     *
     * @return the query for fetching cart discounts from the source CTP project with all the aforementioned references
     *         expanded.
     */
    public static CartDiscountQuery buildCartDiscountQuery() {
        return CartDiscountQuery.of()
                                .withExpansionPaths(ExpansionPath.of("custom.type"))
                                .plusExpansionPaths(ExpansionPath.of("value.product"))
                                .plusExpansionPaths(ExpansionPath.of("value.supplyChannel"))
                                .plusExpansionPaths(ExpansionPath.of("value.distributionChannel"));
    }


    private CartDiscountReferenceReplacementUtils() {
    }
}
