package com.commercetools.sync.categories.helpers;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.helpers.AssetActionFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.AddAsset;
import io.sphere.sdk.categories.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.categories.commands.updateactions.RemoveAsset;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.Resource;

import javax.annotation.Nonnull;
import java.util.List;

import static com.commercetools.sync.categories.utils.CategoryAssetUpdateActionUtils.buildActions;

public final class CategoryAssetActionFactory<T,D extends CategoryDraft>
    extends AssetActionFactory<Category> {
    public CategoryAssetActionFactory(@Nonnull final CategorySyncOptions syncOptions) {
        this.syncOptions = syncOptions;
    }

    @Override
    public <D> List<UpdateAction<Category>> buildAssetActions(@Nonnull final Resource oldRessource,
                                                              @Nonnull final D newRessource,
                                                              @Nonnull final Asset oldAsset,
                                                              @Nonnull final AssetDraft newAssetDraft) {
        return buildActions( oldRessource, newRessource, oldAsset, newAssetDraft, (CategorySyncOptions) syncOptions);
    }



    @Override
    public UpdateAction<Category> buildRemoveAssetAction(@Nonnull final String assetKey) {
        return RemoveAsset.ofKey(assetKey);
    }

    @Override
    public UpdateAction<Category> buildChangeAssetOrderAction(@Nonnull final List<String> newAssetOrder) {
        return ChangeAssetOrder.of(newAssetOrder);
    }

    @Override
    public UpdateAction<Category> buildAddAssetAction(@Nonnull final AssetDraft assetDraft,
                                                      @Nonnull final Integer position) {
        return AddAsset.of(assetDraft, position);
    }
}
