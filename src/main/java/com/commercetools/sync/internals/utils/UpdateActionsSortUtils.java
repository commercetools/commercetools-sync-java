package com.commercetools.sync.internals.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.AddPrice;
import io.sphere.sdk.products.commands.updateactions.ChangePrice;
import io.sphere.sdk.products.commands.updateactions.RemovePrice;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomField;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is only meant for the internal use of the commercetools-sync-java library.
 */
public final class UpdateActionsSortUtils {
    /**
     * Given a list of update actions, this method returns a copy of the supplied list but sorted with the following
     * precedence:
     * <ol>
     * <li>{@link RemovePrice}</li>
     * <li>{@link ChangePrice} or {@link SetProductPriceCustomType} or {@link SetProductPriceCustomField}</li>
     * <li>{@link AddPrice}</li>
     * </ol>
     *
     * <p>This is to ensure that there are no conflicts when adding a new price that might have a duplicate value for
     * a unique field, which could already be changed or removed.
     *
     * @param updateActions list of update actions to sort.
     * @return a new sorted list of update actions (remove, change, add).
     */
    @Nonnull
    public static List<UpdateAction<Product>> sortPriceActions(
        @Nonnull final List<UpdateAction<Product>> updateActions) {

        final List<UpdateAction<Product>> actionsCopy = new ArrayList<>(updateActions);
        actionsCopy.sort((action1, action2) -> {
            if (action1 instanceof RemovePrice && !(action2 instanceof RemovePrice)) {
                return -1;
            }

            if (!(action1 instanceof RemovePrice) && action2 instanceof RemovePrice) {
                return 1;
            }

            if (!(action1 instanceof AddPrice) && action2 instanceof AddPrice) {
                return -1;
            }

            if (action1 instanceof AddPrice && !(action2 instanceof AddPrice)) {
                return 1;
            }

            return 0;
        });
        return actionsCopy;
    }

    @Nonnull
    public static List<UpdateAction<Product>> sortProductVariantAssetActions(
        @Nonnull final List<UpdateAction<Product>> updateActions) {

        final List<UpdateAction<Product>> actionsCopy = new ArrayList<>(updateActions);
        actionsCopy.sort((action1, action2) -> {
            if (action1 instanceof RemoveAsset && !(action2 instanceof RemoveAsset)) {
                return -1;
            }

            if (!(action1 instanceof RemoveAsset) && action2 instanceof RemoveAsset) {
                return 1;
            }

            if (!(action1 instanceof AddAsset) && action2 instanceof AddAsset) {
                return -1;
            }

            if (action1 instanceof AddAsset && !(action2 instanceof AddAsset)) {
                return 1;
            }

            if (!(action1 instanceof ChangeAssetOrder) && action2 instanceof ChangeAssetOrder) {
                return -1;
            }

            if (action1 instanceof ChangeAssetOrder && !(action2 instanceof ChangeAssetOrder)) {
                return 1;
            }

            return 0;
        });
        return actionsCopy;
    }

    @Nonnull
    public static List<UpdateAction<Category>> sortCategoryAssetActions(
        @Nonnull final List<UpdateAction<Category>> updateActions) {

        final List<UpdateAction<Category>> actionsCopy = new ArrayList<>(updateActions);
        actionsCopy.sort((action1, action2) -> {
            if (action1 instanceof io.sphere.sdk.categories.commands.updateactions.RemoveAsset
                && !(action2 instanceof io.sphere.sdk.categories.commands.updateactions.RemoveAsset)) {
                return -1;
            }

            if (!(action1 instanceof io.sphere.sdk.categories.commands.updateactions.RemoveAsset)
                && action2 instanceof io.sphere.sdk.categories.commands.updateactions.RemoveAsset) {
                return 1;
            }

            if (!(action1 instanceof io.sphere.sdk.categories.commands.updateactions.AddAsset)
                && action2 instanceof io.sphere.sdk.categories.commands.updateactions.AddAsset) {
                return -1;
            }

            if (action1 instanceof io.sphere.sdk.categories.commands.updateactions.AddAsset
                && !(action2 instanceof io.sphere.sdk.categories.commands.updateactions.AddAsset)) {
                return 1;
            }

            if (!(action1 instanceof io.sphere.sdk.categories.commands.updateactions.ChangeAssetOrder)
                && action2 instanceof io.sphere.sdk.categories.commands.updateactions.ChangeAssetOrder) {
                return -1;
            }

            if (action1 instanceof io.sphere.sdk.categories.commands.updateactions.ChangeAssetOrder
                && !(action2 instanceof io.sphere.sdk.categories.commands.updateactions.ChangeAssetOrder)) {
                return 1;
            }

            return 0;
        });
        return actionsCopy;
    }

    private UpdateActionsSortUtils() {
    }
}
