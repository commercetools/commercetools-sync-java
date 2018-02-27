package com.commercetools.sync.commons.utils;

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

    @Nonnull
    public static <T> List<UpdateAction<T>> buildAssetsUpdateActions(
        @Nonnull final List<Asset> oldAssets,
        @Nullable final List<AssetDraft> newAssetDrafts,
        @Nonnull final BiFunction<Asset, AssetDraft, List<UpdateAction<T>>> assetActionsBuilder,
        @Nonnull final Function<String, UpdateAction<T>> removeAssetActionBuilder,
        @Nonnull final Function<List<String>, UpdateAction<T>> changeAssetOrderActionBuilder,
        @Nonnull final BiFunction<AssetDraft, Integer, UpdateAction<T>> addAssetActionBuilder) {

        return ofNullable(newAssetDrafts)
            .map(assetDrafts ->
                buildAssetsUpdateActionsWithNewAssetDrafts(
                    oldAssets,
                    assetDrafts,
                    assetActionsBuilder,
                    removeAssetActionBuilder,
                    changeAssetOrderActionBuilder,
                    addAssetActionBuilder))
            .orElseGet(() -> oldAssets.stream()
                                      .map(oldAsset -> removeAssetActionBuilder.apply(oldAsset.getKey()))
                                      .collect(Collectors.toList()));
    }


    /**
     *
     * @param oldAssets
     * @param newAssetDrafts
     * @param assetActionsBuilder
     * @param removeAssetActionBuilder
     * @param changeAssetOrderActionBuilder
     * @param addAssetActionBuilder
     * @param <T>
     * @return
     */
    @Nonnull
    private static <T> List<UpdateAction<T>> buildAssetsUpdateActionsWithNewAssetDrafts(
        @Nonnull final List<Asset> oldAssets,
        @Nonnull final List<AssetDraft> newAssetDrafts,
        @Nonnull final BiFunction<Asset, AssetDraft, List<UpdateAction<T>>> assetActionsBuilder,
        @Nonnull final Function<String, UpdateAction<T>> removeAssetActionBuilder,
        @Nonnull final Function<List<String>, UpdateAction<T>> changeAssetOrderActionBuilder,
        @Nonnull final BiFunction<AssetDraft, Integer, UpdateAction<T>> addAssetActionBuilder) {

        // Asset list that represents the state of the old variant assets after each applied update action.
        final List<Asset> intermediateOldAssets = new ArrayList<>(oldAssets);

        final Map<String, Asset> oldAssetsKeyMap = oldAssets.stream().collect(toMap(Asset::getKey, asset -> asset));
        final Map<String, AssetDraft> newAssetDraftsKeyMap = newAssetDrafts
            .stream().collect(toMap(AssetDraft::getKey, assetDraft -> assetDraft));

        // Action order prio: removeAsset → changeAssetOrder → addAsset

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


    /**
     * TODO: Document that is shouldn't be used alone!
     * @param oldAssets
     * @param intermediateOldAssets
     * @param oldAssetsKeyMap
     * @param newAssetDraftsKeyMap
     * @param buildAssetActionsBuilder
     * @param removeAssetActionBuilder
     * @param <T>
     * @return
     */
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


    /**
     * * TODO: Document that is shouldn't be used alone!
     * @param intermediateOldAssets
     * @param newAssetDrafts
     * @param changeAssetOrderActionBuilder
     * @param <T>
     * @return
     */
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


    /**
     * TODO: Document that is shouldn't be used alone!
     * @param newProductVariantAssetDrafts
     * @param oldAssetsKeyMap
     * @param addAssetActionBuilder
     * @param <T>
     * @return
     */
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
                                    return oldAssetsKeyMap.get(assetDraftKey) == null ?
                                        addAssetActionBuilder.apply(assetDraft, assetDraftIndex) : null;
                                }))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(toList());
    }


    private AssetsUpdateActionUtils() {
    }
}
