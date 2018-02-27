package com.commercetools.sync.categories.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.AddAsset;
import io.sphere.sdk.categories.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.categories.commands.updateactions.RemoveAsset;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.commercetools.sync.categories.utils.CategoryAssetUpdateActionUtils.buildActions;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

//TODO: CONSIDER CHANGE NAME.
// TODO: Add tests.
public final class CategoryAssetsUpdateActionUtils {
    @Nonnull
    static List<UpdateAction<Category>> buildAssetsUpdateActions(
        @Nonnull final List<Asset> oldCategoryAssets,
        @Nonnull final List<AssetDraft> newCategoryAssetDrafts,
        @Nonnull final CategorySyncOptions syncOptions) {

        // Asset list that represents the state of the old variant assets after each applied update action.
        final List<Asset> intermediateOldAssets = new ArrayList<>(oldCategoryAssets);

        final Map<String, Asset> oldAssetsKeyMap = oldCategoryAssets
            .stream().collect(toMap(Asset::getKey, asset -> asset));

        final Map<String, AssetDraft> newAssetDraftsKeyMap = newCategoryAssetDrafts
            .stream().collect(toMap(AssetDraft::getKey, assetDraft -> assetDraft));

        // Action order prio: removeAsset → changeAssetOrder → addAsset

        //1. Remove or compare if matching.
        final List<UpdateAction<Category>> updateActions = new ArrayList<>(
            buildRemoveAssetOrAssetUpdateActions(oldCategoryAssets, intermediateOldAssets, oldAssetsKeyMap,
                newAssetDraftsKeyMap, syncOptions));

        //2. Compare ordering of assets and add a ChangeAssetOrder action if needed.
        buildChangeAssetOrderUpdateAction(intermediateOldAssets, newCategoryAssetDrafts).ifPresent(updateActions::add);

        // For every new asset draft, If it doesn't exist in the old assets, then add an AddAsset action to the list
        // of update actions.
        updateActions.addAll(buildAddAssetUpdateActions(newCategoryAssetDrafts, oldAssetsKeyMap));

        return updateActions;
    }

    @Nonnull
    private static List<UpdateAction<Category>> buildRemoveAssetOrAssetUpdateActions(
        @Nonnull final List<Asset> oldCategoryAssets,
        @Nonnull final List<Asset> intermediateOldAssets,
        @Nonnull final Map<String, Asset> oldAssetsKeyMap,
        @Nonnull final Map<String, AssetDraft> newAssetDraftsKeyMap,
        @Nonnull final CategorySyncOptions syncOptions) {
        // For every old asset, If it doesn't exist anymore in the new asset drafts,
        // then add a RemoveAsset action to the list of update actions. If the asset still exists in the new draft,
        // then compare the asset fields (name, desc, etc..), and add the computed actions to the list of update
        // actions.
        return oldCategoryAssets
            .stream()
            .map(oldAsset -> {
                final String oldAssetKey = oldAsset.getKey();
                final AssetDraft matchingNewAssetDraft = newAssetDraftsKeyMap.get(oldAssetKey);

                return ofNullable(matchingNewAssetDraft)
                    .map(assetDraft -> // If asset exists, compare the two assets.
                        buildActions(oldAsset, matchingNewAssetDraft, syncOptions))
                    .orElseGet(() -> { // If asset doesn't exists, remove asset.
                        intermediateOldAssets.remove(oldAssetsKeyMap.get(oldAssetKey)); //TODO: EXPENSIVE
                        return singletonList(RemoveAsset.ofKey(oldAssetKey));
                    });
            })
            .flatMap(Collection::stream)
            .collect(toList());
    }

    @Nonnull
    private static Optional<UpdateAction<Category>> buildChangeAssetOrderUpdateAction(
        @Nonnull final List<Asset> intermediateOldAssets,
        @Nonnull final List<AssetDraft> newAssetDrafts) {

        final Map<String, String> oldAssetKeyToIdMap =
            intermediateOldAssets.stream()
                                 .collect(toMap(Asset::getKey, Asset::getId));

        final List<String> newOrder = newAssetDrafts.stream()
                                                    .map(AssetDraft::getKey)
                                                    .map(oldAssetKeyToIdMap::get)
                                                    .filter(Objects::nonNull)
                                                    .collect(toList());

        final List<String> oldOrder = intermediateOldAssets.stream()
                                                           .map(Asset::getId)
                                                           .collect(toList());

        return buildUpdateAction(oldOrder, newOrder, () -> ChangeAssetOrder.of(newOrder));
    }

    @Nonnull
    private static List<UpdateAction<Category>> buildAddAssetUpdateActions(
        @Nonnull final List<AssetDraft> newCategoryAssetDrafts,
        @Nonnull final Map<String, Asset> oldAssetsKeyMap) {

        return IntStream.range(0, newCategoryAssetDrafts.size())
                        .mapToObj(assetDraftIndex ->
                            ofNullable(newCategoryAssetDrafts.get(assetDraftIndex))
                                .map(assetDraft ->
                                    oldAssetsKeyMap.get(assetDraft.getKey()) == null ?
                                        AddAsset.of(assetDraft, assetDraftIndex) : null))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(toList());
    }

    private CategoryAssetsUpdateActionUtils() {
    }
}
