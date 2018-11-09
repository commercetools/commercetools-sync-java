package com.commercetools.sync.products.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.AddAsset;
import io.sphere.sdk.products.commands.updateactions.AddExternalImage;
import io.sphere.sdk.products.commands.updateactions.AddPrice;
import io.sphere.sdk.products.commands.updateactions.AddVariant;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetName;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.products.commands.updateactions.ChangeMasterVariant;
import io.sphere.sdk.products.commands.updateactions.ChangePrice;
import io.sphere.sdk.products.commands.updateactions.MoveImageToPosition;
import io.sphere.sdk.products.commands.updateactions.RemoveAsset;
import io.sphere.sdk.products.commands.updateactions.RemoveImage;
import io.sphere.sdk.products.commands.updateactions.RemovePrice;
import io.sphere.sdk.products.commands.updateactions.RemoveVariant;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomField;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomType;
import io.sphere.sdk.products.commands.updateactions.SetAssetDescription;
import io.sphere.sdk.products.commands.updateactions.SetAssetSources;
import io.sphere.sdk.products.commands.updateactions.SetAssetTags;
import io.sphere.sdk.products.commands.updateactions.SetAttribute;
import io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomField;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class UpdateActionsSortUtils {
    /**
     * Given a list of update actions, this method returns a copy of the supplied list but sorted with the following
     * precedence:
     * <ol>
     * <li>{@link RemoveVariant} only if it's a non master variant</li>
     * <li>Variant Change actions. For example: sku, attributes, images, prices, assets update actions</li>
     * <li>{@link AddVariant}</li>
     * <li>{@link ChangeMasterVariant}</li>
     * <li>{@link RemoveVariant} only if it's a master variant</li>
     * </ol>
     *
     * <p>This is to ensure that there are no conflicts when updating variants.
     *
     * @param updateActions   list of update actions to sort.
     * @param masterVariantId the id of the master variant. This is needed to be able to distinguish between non-master
     *                        variant remove actions and master variant remove actions.
     * @return a new sorted list of update actions.
     */
    @Nonnull
    static List<UpdateAction<Product>> sortVariantActions(@Nonnull final List<UpdateAction<Product>> updateActions,
                                                          @Nonnull final Integer masterVariantId) {

        final List<UpdateAction<Product>> actionsCopy = new ArrayList<>(updateActions);
        actionsCopy.sort((action1, action2) -> {
            if (isNonMasterVariantRemoveAction(action1, masterVariantId)
                && !isNonMasterVariantRemoveAction(action2, masterVariantId)) {
                return -1;
            }

            if (!isNonMasterVariantRemoveAction(action1, masterVariantId)
                && isNonMasterVariantRemoveAction(action2, masterVariantId)) {
                return 1;
            }

            if (!(isMasterVariantRemoveAction(action1, masterVariantId))
                && isMasterVariantRemoveAction(action2, masterVariantId)) {
                return -1;
            }

            if (isMasterVariantRemoveAction(action1, masterVariantId)
                && !isMasterVariantRemoveAction(action2, masterVariantId)) {
                return 1;
            }

            if (!(action1 instanceof ChangeMasterVariant) && action2 instanceof ChangeMasterVariant) {
                return -1;
            }

            if (action1 instanceof ChangeMasterVariant && !(action2 instanceof ChangeMasterVariant)) {
                return 1;
            }

            if (!(action1 instanceof AddVariant) && action2 instanceof AddVariant) {
                return -1;
            }

            if (action1 instanceof AddVariant && !(action2 instanceof AddVariant)) {
                return 1;
            }

            return 0;
        });
        return actionsCopy;
    }

    private static boolean isNonMasterVariantRemoveAction(@Nonnull final UpdateAction<Product> action,
                                                          @Nonnull final Integer masterVariantId) {
        return action instanceof RemoveVariant
            && !Objects.equals(((RemoveVariant) action).getId(), masterVariantId);
    }

    private static boolean isMasterVariantRemoveAction(@Nonnull final UpdateAction<Product> action,
                                                       @Nonnull final Integer masterVariantId) {
        return action instanceof RemoveVariant
            && Objects.equals(((RemoveVariant) action).getId(), masterVariantId);
    }

    /**
     * Given a list of update actions, this method returns a copy of the supplied list but sorted with the following
     * precedence:
     * <ol>
     * <li>{@link SetAttribute} OR {@link SetAttributeInAllVariants} with null values (unsets the attribute)</li>
     * <li>{@link SetAttribute} or {@link SetAttributeInAllVariants} with non-null values (sets the attribute)</li>
     * </ol>
     *
     * <p>This is to ensure that there are no conflicts when adding a new attribute. So we first issue all the unset
     * actions, then we issue the set actions.
     *
     * @param updateActions list of update actions to sort.
     * @return a new sorted list of update actions.
     */
    @Nonnull
    static List<UpdateAction<Product>> sortAttributeActions(@Nonnull final List<UpdateAction<Product>> updateActions) {

        final List<UpdateAction<Product>> actionsCopy = new ArrayList<>(updateActions);
        actionsCopy.sort((action1, action2) -> {
            if (isUnSetAttribute(action1) && !isUnSetAttribute(action2)) {
                return -1;
            }

            if (!isUnSetAttribute(action1) && isUnSetAttribute(action2)) {
                return 1;
            }
            return 0;
        });
        return actionsCopy;
    }

    private static boolean isUnSetAttribute(@Nonnull final UpdateAction<Product> action) {
        return (action instanceof SetAttribute
            && Objects.isNull(((SetAttribute) action).getValue()))
            || (action instanceof SetAttributeInAllVariants
            && Objects.isNull(((SetAttributeInAllVariants) action).getValue()));
    }

    /**
     * Given a list of update actions, this method returns a copy of the supplied list but sorted with the following
     * precedence:
     * <ol>
     * <li>{@link RemoveImage}</li>
     * <li>{@link AddExternalImage}</li>
     * <li>{@link MoveImageToPosition}</li>
     * </ol>
     *
     * <p>This is to ensure that there are no conflicts when adding a new image that might have a duplicate value for
     * a unique field, which could already be changed or removed. We move the image after adding it, since there is
     * no way to add the image at a certain index and moving an image doesn't require that the image already exists on
     * CTP.
     *
     * @param updateActions list of update actions to sort.
     * @return a new sorted list of update actions.
     */
    @Nonnull
    static List<UpdateAction<Product>> sortImageActions(@Nonnull final List<UpdateAction<Product>> updateActions) {

        final List<UpdateAction<Product>> actionsCopy = new ArrayList<>(updateActions);
        actionsCopy.sort((action1, action2) -> {
            if (action1 instanceof RemoveImage && !(action2 instanceof RemoveImage)) {
                return -1;
            }

            if (!(action1 instanceof RemoveImage) && action2 instanceof RemoveImage) {
                return 1;
            }

            if (!(action1 instanceof MoveImageToPosition) && action2 instanceof MoveImageToPosition) {
                return -1;
            }

            if (action1 instanceof MoveImageToPosition && !(action2 instanceof MoveImageToPosition)) {
                return 1;
            }

            return 0;
        });
        return actionsCopy;
    }

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
    static List<UpdateAction<Product>> sortPriceActions(@Nonnull final List<UpdateAction<Product>> updateActions) {

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

    /**
     * Given a list of update actions, this method returns a copy of the supplied list but sorted with the following
     * precedence:
     * <ol>
     * <li>{@link RemoveAsset}</li>
     * <li>{@link ChangeAssetName} or {@link SetAssetDescription} OR {@link SetAssetTags} OR
     * {@link SetAssetSources} OR {@link SetAssetCustomField} OR {@link SetAssetCustomType}</li>
     * <li>{@link ChangeAssetOrder}</li>
     * <li>{@link AddAsset}</li>
     * </ol>
     *
     * <p>This is to ensure that there are no conflicts when adding a new asset that might have a duplicate value for
     * a unique field, which could already be changed or removed. It is important to have a changeAssetOrder action
     * before an addAsset action, since changeAssetOrder requires asset ids for sorting them, and new assets don't have
     * ids yet since they are generated by CTP after an asset is created. Therefore, first set the correct order, then
     * we add the asset at the correct index.
     *
     * @param updateActions list of update actions to sort.
     * @return a new sorted list of update actions.
     */
    @Nonnull
    static List<UpdateAction<Product>> sortAssetActions(@Nonnull final List<UpdateAction<Product>> updateActions) {

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

    private UpdateActionsSortUtils() {
    }
}
