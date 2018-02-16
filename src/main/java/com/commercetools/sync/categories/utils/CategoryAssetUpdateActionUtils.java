package com.commercetools.sync.categories.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.ChangeAssetName;
import io.sphere.sdk.categories.commands.updateactions.SetAssetDescription;
import io.sphere.sdk.categories.commands.updateactions.SetAssetSources;
import io.sphere.sdk.categories.commands.updateactions.SetAssetTags;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;

//TODO: CONSIDER CHANGE NAME.
// TODO: Add tests.
public final class CategoryAssetUpdateActionUtils {

    @Nonnull
    public static Optional<UpdateAction<Category>> buildChangeAssetNameUpdateAction(
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getName(), newAsset.getName(),
            () -> ChangeAssetName.ofKey(oldAsset.getKey(), newAsset.getName()));
    }

    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetAssetDescriptionUpdateAction(
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getDescription(), newAsset.getDescription(),
            () -> SetAssetDescription.ofKey(oldAsset.getKey(), newAsset.getDescription()));
    }

    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetAssetTagsUpdateAction(
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getTags(), newAsset.getTags(),
            () -> SetAssetTags.ofKey(oldAsset.getKey(), newAsset.getTags()));
    }

    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetAssetSourcesUpdateAction(
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getSources(), newAsset.getSources(),
            () -> SetAssetSources.ofKey(oldAsset.getKey(), newAsset.getSources()));
    }

    @Nonnull
    public static List<UpdateAction<Category>> buildCustomUpdateActions(
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset,
        @Nonnull final CategorySyncOptions syncOptions) {
        return CustomUpdateActionUtils.buildCustomUpdateActions(oldAsset, newAsset,
            Category.class, -1, Asset::getId, asset -> Asset.resourceTypeId(), Asset::getKey, syncOptions);
    }
}
