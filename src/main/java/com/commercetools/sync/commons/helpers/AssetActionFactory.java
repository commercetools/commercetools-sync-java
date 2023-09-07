package com.commercetools.sync.commons.helpers;

import com.commercetools.api.models.ResourceUpdateAction;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.sync.commons.BaseSyncOptions;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Helper class for building update actions for assets that are contained in the resource of type
 * {@code T}.
 *
 * @param <T> the type of the resource the asset update actions are built for.
 * @param <D> the type of the draft, which contains the changes the asset update actions are built
 *     for.
 */
public abstract class AssetActionFactory<T extends ResourceUpdateAction<T>, D> {

  public BaseSyncOptions syncOptions = null;

  /**
   * Takes a matching old asset and a new asset and computes the update actions needed to sync them.
   *
   * @param newResource new mainresource draft, which contains the asset to update.
   * @param oldAsset the old asset to compare.
   * @param newAssetDraft the matching new asset draft.
   * @return update actions needed to sync the two assets.
   */
  public abstract List<T> buildAssetActions(
      @Nonnull final D newResource, @Nonnull Asset oldAsset, @Nonnull AssetDraft newAssetDraft);

  /**
   * Takes an asset key to build a RemoveAsset action of the type T.
   *
   * @param assetKey the key of the asset used un building the update action.
   * @return the built remove asset update action.
   */
  public abstract T buildRemoveAssetAction(@Nonnull String assetKey);

  /**
   * Takes a list of asset ids to build a ChangeAssetOrder action of the type T.
   *
   * @param newAssetOrder the new asset order needed to build the action.
   * @return the built update action.
   */
  public abstract T buildChangeAssetOrderAction(@Nonnull List<String> newAssetOrder);

  /**
   * Takes an asset draft and an asset position to build an AddAsset action of the type T.
   * buildRemoveAssetAction
   *
   * @param newAssetDraft the new asset draft to create an Add asset action for.
   * @param position the position to add the new asset to.
   * @return the built update action.
   */
  public abstract T buildAddAssetAction(
      @Nonnull AssetDraft newAssetDraft, @Nonnull Integer position);
}
