package com.commercetools.sync.commons.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public final class AssetsUpdateActionUtils {

    /**
     * Compares a list of {@link Asset}s with a list of {@link AssetDraft}s. The method serves as a generic
     * implementation for assets syncing. The method takes in functions for building the required update actions (
     * AddAsset, RemoveAsset, ChangeAssetOrder and 1-1 update actions on assets (e.g. changeAssetName,
     * setAssetDescription, etc..) for the required resource.
     *
     * <p>If the list of new {@link AssetDraft}s is {@code null}, then remove actions are built for every existing asset
     * in the {@code oldAssets} list.
     *
     * @param oldAssets                     the old list of assets.
     * @param newAssetDrafts                the new list of asset drafts.
     * @param assetActionsBuilder           function that takes a matching old asset and a new asset and computes the
     *                                      update actions needed to sync them.
     * @param removeAssetActionBuilder      function that takes an asset key to build a RemoveAsset action of the
     *                                      type T.
     * @param changeAssetOrderActionBuilder function that takes a list of asset ids to build a ChangeAssetOrder action
     *                                      of the type T.
     * @param addAssetActionBuilder         function that takes an asset draft and an asset position to build an
     *                                      AddAsset action of the type T.
     * @param <T>                           the type of the resource the asset update actions are built for.
     * @return a list of asset update actions on the resource of type T if the list of assets is not identical.
     *         Otherwise, if the assets are identical, an empty list is returned.
     * @throws BuildUpdateActionException in case there are asset drafts with duplicate keys.
     */
    @Nonnull
    public static <T> List<UpdateAction<T>> buildAssetsUpdateActions(
        @Nonnull final List<Asset> oldAssets,
        @Nullable final List<AssetDraft> newAssetDrafts,
        @Nonnull final BiFunction<Asset, AssetDraft, List<UpdateAction<T>>> assetActionsBuilder,
        @Nonnull final Function<String, UpdateAction<T>> removeAssetActionBuilder,
        @Nonnull final Function<List<String>, UpdateAction<T>> changeAssetOrderActionBuilder,
        @Nonnull final BiFunction<AssetDraft, Integer, UpdateAction<T>> addAssetActionBuilder)
        throws BuildUpdateActionException {

        if (newAssetDrafts != null) {
            return buildAssetsUpdateActionsWithNewAssetDrafts(
                oldAssets,
                newAssetDrafts,
                assetActionsBuilder,
                removeAssetActionBuilder,
                changeAssetOrderActionBuilder,
                addAssetActionBuilder);
        } else {
            return oldAssets.stream()
                            .map(oldAsset -> removeAssetActionBuilder.apply(oldAsset.getKey()))
                            .collect(Collectors.toList());
        }
    }

    @Nonnull
    private static <T> List<UpdateAction<T>> buildAssetsUpdateActionsWithNewAssetDrafts(
        @Nonnull final List<Asset> oldAssets,
        @Nonnull final List<AssetDraft> newAssetDrafts,
        @Nonnull final BiFunction<Asset, AssetDraft, List<UpdateAction<T>>> assetActionsBuilder,
        @Nonnull final Function<String, UpdateAction<T>> removeAssetActionBuilder,
        @Nonnull final Function<List<String>, UpdateAction<T>> changeAssetOrderActionBuilder,
        @Nonnull final BiFunction<AssetDraft, Integer, UpdateAction<T>> addAssetActionBuilder)
        throws BuildUpdateActionException {

        // Asset list that represents the state of the old variant assets after each applied update action.
        final List<Asset> intermediateOldAssets = new ArrayList<>(oldAssets);

        final Map<String, Asset> oldAssetsKeyMap = oldAssets.stream().collect(toMap(Asset::getKey, asset -> asset));

        final Map<String, AssetDraft> newAssetDraftsKeyMap;
        try {
            newAssetDraftsKeyMap = newAssetDrafts
                .stream().collect(toMap(AssetDraft::getKey, assetDraft -> assetDraft));
        } catch (final IllegalStateException exception) {
            throw new BuildUpdateActionException("Supplied asset drafts have duplicate keys. Asset keys are expected to"
                + " be unique inside their container (a product variant or a category).", exception);
        }


        // It is important to have a changeAssetOrder action before an addAsset action, since changeAssetOrder requires
        // asset ids for sorting them, and new assets don't have ids yet since they are generated
        // by CTP after an asset is created. Therefore, the order of update actions must be:
        // removeAsset → changeAssetOrder → addAsset

        //1. Remove or compare if matching.
        final List<UpdateAction<T>> updateActions = buildRemoveAssetOrAssetUpdateActions(
            oldAssets,
            intermediateOldAssets,
            oldAssetsKeyMap,
            newAssetDraftsKeyMap,
            assetActionsBuilder,
            removeAssetActionBuilder);

        //2. Compare ordering of assets and add a ChangeAssetOrder action if needed.
        buildChangeAssetOrderUpdateAction(intermediateOldAssets, newAssetDrafts, changeAssetOrderActionBuilder)
            .ifPresent(updateActions::add);

        // For every new asset draft, If it doesn't exist in the old assets, then add an AddAsset action to the list
        // of update actions.
        updateActions.addAll(buildAddAssetUpdateActions(newAssetDrafts, oldAssetsKeyMap, addAssetActionBuilder));

        return updateActions;
    }

    @Nonnull
    private static <T> List<UpdateAction<T>> buildRemoveAssetOrAssetUpdateActions(
        @Nonnull final List<Asset> oldAssets,
        @Nonnull final List<Asset> intermediateOldAssets,
        @Nonnull final Map<String, Asset> oldAssetsKeyMap,
        @Nonnull final Map<String, AssetDraft> newAssetDraftsKeyMap,
        @Nonnull final BiFunction<Asset, AssetDraft, List<UpdateAction<T>>> buildAssetActionsBuilder,
        @Nonnull final Function<String, UpdateAction<T>> removeAssetActionBuilder) {
        // For every old asset, If it doesn't exist anymore in the new asset drafts,
        // then add a RemoveAsset action to the list of update actions. If the asset still exists in the new draft,
        // then compare the asset fields (name, desc, etc..), and add the computed actions to the list of update
        // actions.
        return oldAssets
            .stream()
            .map(oldAsset -> {
                final String oldAssetKey = oldAsset.getKey();
                final AssetDraft matchingNewAssetDraft = newAssetDraftsKeyMap.get(oldAssetKey);
                return ofNullable(matchingNewAssetDraft)
                    .map(assetDraft -> // If asset exists, compare the two assets.
                        buildAssetActionsBuilder.apply(oldAsset, assetDraft))
                    .orElseGet(() -> { // If asset doesn't exists, remove asset.
                        intermediateOldAssets.remove(oldAssetsKeyMap.get(oldAssetKey)); //TODO: EXPENSIVE
                        return singletonList(removeAssetActionBuilder.apply(oldAssetKey));
                    });
            })
            .flatMap(Collection::stream)
            .collect(toList());
    }


    @Nonnull
    private static <T> Optional<UpdateAction<T>> buildChangeAssetOrderUpdateAction(
        @Nonnull final List<Asset> intermediateOldAssets,
        @Nonnull final List<AssetDraft> newAssetDrafts,
        @Nonnull final Function<List<String>, UpdateAction<T>> changeAssetOrderActionBuilder) {

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

        return buildUpdateAction(oldOrder, newOrder, () -> changeAssetOrderActionBuilder.apply(newOrder));
    }

    @Nonnull
    private static <T> List<UpdateAction<T>> buildAddAssetUpdateActions(
        @Nonnull final List<AssetDraft> newProductVariantAssetDrafts,
        @Nonnull final Map<String, Asset> oldAssetsKeyMap,
        @Nonnull final BiFunction<AssetDraft, Integer, UpdateAction<T>> addAssetActionBuilder) {


        return IntStream.range(0, newProductVariantAssetDrafts.size())
                        .mapToObj(assetDraftIndex ->
                            ofNullable(newProductVariantAssetDrafts.get(assetDraftIndex))
                                .map(assetDraft -> {
                                    final String assetDraftKey = assetDraft.getKey();
                                    return oldAssetsKeyMap.get(assetDraftKey) == null
                                        ? addAssetActionBuilder.apply(assetDraft, assetDraftIndex) : null;
                                }))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(toList());
    }


    private AssetsUpdateActionUtils() {
    }
}
