package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.helpers.PriceCustomActionBuilder;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.ChangePrice;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;

public final class ProductVariantPriceUpdateActionUtils {

    /**
     * TODO.
     * @param variantId
     * @param oldPrice
     * @param newPrice
     * @param syncOptions
     * @return
     */
    public static List<UpdateAction<Product>> buildActions(
        @Nonnull final Integer variantId,
        @Nonnull final Price oldPrice,
        @Nonnull final PriceDraft newPrice,
        @Nonnull final ProductSyncOptions syncOptions) {

        final List<UpdateAction<Product>> updateActions = new ArrayList<>();

        buildChangePriceUpdateAction(oldPrice, newPrice).ifPresent(updateActions::add);
        updateActions.addAll(buildCustomUpdateActions(variantId, oldPrice, newPrice, syncOptions));

        return updateActions;
    }


    /**
     * TODO
     * @param oldPrice
     * @param newPrice
     * @return
     */
    public static Optional<ChangePrice> buildChangePriceUpdateAction(
        @Nonnull final Price oldPrice,
        @Nonnull final PriceDraft newPrice) {

        final Optional<ChangePrice> actionAfterValuesDiff = buildUpdateAction(oldPrice.getValue(), newPrice.getValue(),
            () -> ChangePrice.of(oldPrice, newPrice, true));

        return actionAfterValuesDiff.map(Optional::of)
                                    .orElseGet(() ->
                                        // If values are not different, compare tiers.
                                        buildUpdateAction(oldPrice.getTiers(), newPrice.getTiers(),
                                            () -> ChangePrice.of(oldPrice, newPrice, true)));
    }

    /**
     * Compares the custom fields and custom types of a {@link Price} and a {@link PriceDraft} and returns a list of
     * {@link UpdateAction}&lt;{@link Product}&gt; as a result. If both the {@link Price} and the {@link PriceDraft}
     * have identical custom fields and types, then no update action is needed and hence an empty {@link List} is
     * returned.
     *
     * @param variantId   the variantId needed for building the update action.
     * @param oldPrice    the price which should be updated.
     * @param newPrice    the price draft where we get the new custom fields and types.
     * @param syncOptions responsible for supplying the sync options to the sync utility method. It is used for
     *                    triggering the error callback within the utility, in case of errors.
     * @return A list with the custom field/type update actions or an empty list if the custom fields/types are
     *         identical.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildCustomUpdateActions(
        @Nonnull final Integer variantId,
        @Nonnull final Price oldPrice,
        @Nonnull final PriceDraft newPrice,
        @Nonnull final ProductSyncOptions syncOptions) {

        return CustomUpdateActionUtils.buildCustomUpdateActions(
            oldPrice,
            newPrice,
            new PriceCustomActionBuilder(),
            variantId,
            Price::getId,
            price -> Price.resourceTypeId(),
            Price::getId,
            syncOptions);
    }

    private ProductVariantPriceUpdateActionUtils() {
    }
}
