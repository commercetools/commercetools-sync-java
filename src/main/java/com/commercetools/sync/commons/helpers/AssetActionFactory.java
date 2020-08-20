package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.commons.BaseSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.Resource;

import javax.annotation.Nonnull;
import java.util.List;


/**
 * Helper class for building update actions for assets that are contained in the resource of type {@code T}.
 *
 * @param <T> the type of the resource the asset update actions are built for.
 * @param <D> the type of the draft, which contains the changes the asset update actions are built for.
 */
public abstract class AssetActionFactory<T extends Resource, D> {
    public BaseSyncOptions syncOptions = null;

    /**
     * Takes a matching old asset and a new asset and computes the update actions needed to sync them.
     *
     * @param oldResource   mainresource, whose asset should be updated.
     * @param newResource   new mainresource draft, which contains the asset to update.
     * @param oldAsset      the old asset to compare.
     * @param newAssetDraft the matching new asset draft.
     * @return update actions needed to sync the two assets.
     */
    public abstract List<UpdateAction<T>> buildAssetActions(@Nonnull final T oldResource,
                                                            @Nonnull final D newResource,
                                                            @Nonnull Asset oldAsset,
                                                            @Nonnull AssetDraft newAssetDraft);

    /**
     * Takes an asset key to build a RemoveAsset action of the type T.
     *
     * @param assetKey the key of the asset used un building the update action.
     * @return the built remove asset update action.
     */
    public abstract UpdateAction<T> buildRemoveAssetAction(@Nonnull String assetKey);

    /**
     * Takes a list of asset ids to build a ChangeAssetOrder action of the type T.
     *
     * @param newAssetOrder the new asset order needed to build the action.
     * @return the built update action.
     */
    public abstract UpdateAction<T> buildChangeAssetOrderAction(@Nonnull List<String> newAssetOrder);

    /**
     * Takes an asset draft and an asset position to build an AddAsset action of the type T.
     *
     * @param newAssetDraft the new asset draft to create an Add asset action for.
     * @param position      the position to add the new asset to.
     * @return the built update action.
     */
    public abstract UpdateAction<T> buildAddAssetAction(@Nonnull AssetDraft newAssetDraft,
                                                        @Nonnull Integer position);
}
