package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.helpers.AssetCustomActionBuilder;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.commands.updateactions.AddAsset;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetName;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.products.commands.updateactions.RemoveAsset;
import io.sphere.sdk.products.commands.updateactions.SetAssetDescription;
import io.sphere.sdk.products.commands.updateactions.SetAssetSources;
import io.sphere.sdk.products.commands.updateactions.SetAssetTags;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.products.utils.ProductAssetUpdateActionUtils.buildActions;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

//TODO: CONSIDER CHANGE NAME.
// TODO: Add tests.
public final class ProductVariantAssetUpdateActionUtils {


    @Nonnull
    static List<UpdateAction<Product>> buildProductVariantAssetsUpdateActions(
        @Nonnull final ProductVariant oldProductVariant,
        @Nonnull final List<AssetDraft> newProductVariantAssetDrafts,
        @Nonnull final ProductSyncOptions syncOptions) {

        final Integer oldProductVariantId = oldProductVariant.getId();
        final List<Asset> oldProductVariantAssets = oldProductVariant.getAssets();

        // Asset list that represents the state of the old variant assets after each applied update action.
        final List<Asset> intermediateOldAssets = new ArrayList<>(oldProductVariantAssets);

        final Map<String, Asset> oldAssetsKeyMap = oldProductVariantAssets
            .stream().collect(toMap(Asset::getKey, asset -> asset));

        final Map<String, AssetDraft> newAssetDraftsKeyMap = newProductVariantAssetDrafts
            .stream().collect(toMap(AssetDraft::getKey, assetDraft -> assetDraft));

        // Action order prio: removeAsset → changeAssetOrder → addAsset

        //1. Remove or compare if matching.
        final List<UpdateAction<Product>> updateActions = new ArrayList<>(
            buildRemoveAssetOrAssetUpdateActions(oldProductVariant, intermediateOldAssets, oldAssetsKeyMap,
                newAssetDraftsKeyMap, syncOptions));

        //2. Compare ordering of assets and add a ChangeAssetOrder action if needed.
        buildChangeAssetOrderUpdateAction(oldProductVariantId, intermediateOldAssets, newProductVariantAssetDrafts)
            .ifPresent(updateActions::add);

        // For every new asset draft, If it doesn't exist in the old assets, then add an AddAsset action to the list
        // of update actions.
        updateActions.addAll(
            buildAddAssetUpdateActions(oldProductVariantId, newProductVariantAssetDrafts, oldAssetsKeyMap));

        return updateActions;
    }

    @Nonnull
    private static List<UpdateAction<Product>> buildRemoveAssetOrAssetUpdateActions(
        @Nonnull final ProductVariant oldProductVariant,
        @Nonnull final List<Asset> intermediateOldAssets,
        @Nonnull final Map<String, Asset> oldAssetsKeyMap,
        @Nonnull final Map<String, AssetDraft> newAssetDraftsKeyMap,
        @Nonnull final ProductSyncOptions syncOptions) {
        // For every old asset, If it doesn't exist anymore in the new asset drafts,
        // then add a RemoveAsset action to the list of update actions. If the asset still exists in the new draft,
        // then compare the asset fields (name, desc, etc..), and add the computed actions to the list of update
        // actions.
        final Integer oldProductVariantId = oldProductVariant.getId();
        return oldProductVariant
            .getAssets()
            .stream()
            .map(oldAsset -> {
                final String oldAssetKey = oldAsset.getKey();
                final AssetDraft matchingNewAssetDraft = newAssetDraftsKeyMap.get(oldAssetKey);

                return ofNullable(matchingNewAssetDraft)
                    .map(assetDraft -> // If asset exists, compare the two assets.
                        buildActions(oldProductVariantId, oldAsset, matchingNewAssetDraft, syncOptions))
                    .orElseGet(() -> { // If asset doesn't exists, remove asset.
                        intermediateOldAssets.remove(oldAssetsKeyMap.get(oldAssetKey)); //TODO: EXPENSIVE
                        return singletonList(
                            RemoveAsset.ofVariantIdWithKey(oldProductVariantId, oldAssetKey, true));
                    });
            })
            .flatMap(Collection::stream)
            .collect(toList());
    }

    @Nonnull
    private static Optional<UpdateAction<Product>> buildChangeAssetOrderUpdateAction(
        final int variantId,
        @Nonnull final List<Asset> intermediateOldAssets,
        @Nonnull final List<AssetDraft> newAssetDrafts) {

        final Map<String, String> oldAssetKeyToIdMap = intermediateOldAssets.stream()
                                                                            .collect(
                                                                                toMap(Asset::getKey, Asset::getId));

        final List<String> newOrder = newAssetDrafts.stream()
                                                    .map(AssetDraft::getKey)
                                                    .map(oldAssetKeyToIdMap::get)
                                                    .filter(Objects::nonNull)
                                                    .collect(toList());

        final List<String> oldOrder = intermediateOldAssets.stream()
                                                           .map(Asset::getId)
                                                           .collect(toList());

        return buildUpdateAction(oldOrder, newOrder, () ->
            ChangeAssetOrder.ofVariantId(variantId, newOrder, true));
    }

    @Nonnull
    private static List<UpdateAction<Product>> buildAddAssetUpdateActions(
        @Nonnull final Integer oldProductVariantId,
        @Nonnull final List<AssetDraft> newProductVariantAssetDrafts,
        @Nonnull final Map<String, Asset> oldAssetsKeyMap) {

        return IntStream.range(0, newProductVariantAssetDrafts.size())
                        .mapToObj(assetDraftIndex ->
                            ofNullable(newProductVariantAssetDrafts.get(assetDraftIndex))
                                .map(assetDraft ->
                                    oldAssetsKeyMap.get(assetDraft.getKey()) == null ?
                                        AddAsset.ofVariantId(oldProductVariantId, assetDraft)
                                                .withPosition(assetDraftIndex) : null))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(toList());
    }

    @Nonnull
    public static Optional<UpdateAction<Product>> buildChangeAssetNameUpdateAction(
        final int variantId,
        @Nonnull final Asset oldProductVariantAsset,
        @Nonnull final AssetDraft newProductVariantAsset) {

        return buildUpdateAction(oldProductVariantAsset.getName(), newProductVariantAsset.getName(),
            () -> ChangeAssetName.ofAssetKeyAndVariantId(variantId, oldProductVariantAsset.getKey(),
                newProductVariantAsset.getName(), true));
    }

    @Nonnull
    public static Optional<UpdateAction<Product>> buildSetAssetDescriptionUpdateAction(
        final int variantId,
        @Nonnull final Asset oldProductVariantAsset,
        @Nonnull final AssetDraft newProductVariantAsset) {

        return buildUpdateAction(oldProductVariantAsset.getDescription(), newProductVariantAsset.getDescription(),
            () -> SetAssetDescription.ofVariantIdAndAssetKey(variantId, oldProductVariantAsset.getKey(),
                newProductVariantAsset.getDescription(), true));
    }

    @Nonnull
    public static Optional<UpdateAction<Product>> buildSetAssetTagsUpdateAction(
        final int variantId,
        @Nonnull final Asset oldProductVariantAsset,
        @Nonnull final AssetDraft newProductVariantAsset) {

        return buildUpdateAction(oldProductVariantAsset.getTags(), newProductVariantAsset.getTags(),
            () -> SetAssetTags.ofVariantIdAndAssetKey(variantId, oldProductVariantAsset.getKey(),
                newProductVariantAsset.getTags(), true));
    }

    @Nonnull
    public static Optional<UpdateAction<Product>> buildSetAssetSourcesUpdateAction(
        final int variantId,
        @Nonnull final Asset oldProductVariantAsset,
        @Nonnull final AssetDraft newProductVariantAsset) {

        return buildUpdateAction(oldProductVariantAsset.getSources(), newProductVariantAsset.getSources(),
            () -> SetAssetSources.ofVariantIdAndAssetKey(variantId, oldProductVariantAsset.getKey(),
                newProductVariantAsset.getSources(), true));
    }

    @Nonnull
    public static List<UpdateAction<Product>> buildCustomUpdateActions(
        final int variantId,
        @Nonnull final Asset oldProductVariantAsset,
        @Nonnull final AssetDraft newProductVariantAsset,
        @Nonnull final ProductSyncOptions syncOptions) {

        return CustomUpdateActionUtils.buildCustomUpdateActions(oldProductVariantAsset, newProductVariantAsset,
            new AssetCustomActionBuilder(), variantId, Asset::getId, asset -> Asset.resourceTypeId(), Asset::getKey,
            syncOptions);
    }

    private ProductVariantAssetUpdateActionUtils() {
    }
}
