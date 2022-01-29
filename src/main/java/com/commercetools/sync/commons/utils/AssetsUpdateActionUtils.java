package com.commercetools.sync.commons.utils;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.helpers.AssetActionFactory;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class AssetsUpdateActionUtils {

  public static final String ASSET_KEY_NOT_SET =
      "Asset with %s has no defined key. Keys are required for " + "asset matching.";

  /**
   * Compares a list of {@link Asset}s with a list of {@link AssetDraft}s. The method serves as a
   * generic implementation for assets syncing. The method takes in functions for building the
   * required update actions ( AddAsset, RemoveAsset, ChangeAssetOrder and 1-1 update actions on
   * assets (e.g. changeAssetName, setAssetDescription, etc..) for the required resource.
   *
   * <p>If the list of new {@link AssetDraft}s is {@code null}, then remove actions are built for
   * every existing asset in the {@code oldAssets} list.
   *
   * @param <T> the type of the resource the asset update actions are built for.
   * @param <D> the type of the draft, which contains the changes the asset update actions are built
   *     for.
   * @param newResource resource draft from a source project, which contains the asset to update.
   * @param oldAssets the old list of assets.
   * @param newAssetDrafts the new list of asset drafts.
   * @param assetActionFactory factory responsible for building asset update actions.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the warn callback within the utility
   * @return a list of asset update actions on the resource of type T if the list of assets is not
   *     identical. Otherwise, if the assets are identical, an empty list is returned.
   * @throws BuildUpdateActionException in case there are asset drafts with duplicate keys.
   */
  @Nonnull
  public static <T extends Resource, D> List<UpdateAction<T>> buildAssetsUpdateActions(
      @Nonnull final D newResource,
      @Nonnull final List<Asset> oldAssets,
      @Nullable final List<AssetDraft> newAssetDrafts,
      @Nonnull final AssetActionFactory<T, D> assetActionFactory,
      @Nonnull final BaseSyncOptions syncOptions)
      throws BuildUpdateActionException {

    if (newAssetDrafts != null) {
      return buildAssetsUpdateActionsWithNewAssetDrafts(
          newResource, oldAssets, newAssetDrafts, assetActionFactory, syncOptions);
    } else {
      return oldAssets.stream()
          .map(Asset::getKey)
          .map(assetActionFactory::buildRemoveAssetAction)
          .collect(toCollection(ArrayList::new));
    }
  }

  /**
   * Compares a list of {@link Asset}s with a list of {@link AssetDraft}s. The method serves as a
   * generic implementation for assets syncing. The method takes in functions for building the
   * required update actions ( AddAsset, RemoveAsset, ChangeAssetOrder and 1-1 update actions on
   * assets (e.g. changeAssetName, setAssetDescription, etc..) for the required resource.
   *
   * @param oldAssets the old list of assets.
   * @param newAssetDrafts the new list of asset drafts.
   * @param assetActionFactory factory responsible for building asset update actions.
   * @param <T> the type of the resource the asset update actions are built for.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the warn callback within the utility
   * @return a list of asset update actions on the resource of type T if the list of assets is not
   *     identical. Otherwise, if the assets are identical, an empty list is returned.
   * @throws BuildUpdateActionException in case there are asset drafts with duplicate keys.
   */
  @SuppressWarnings("unchecked")
  @Nonnull
  private static <T extends Resource, D>
      List<UpdateAction<T>> buildAssetsUpdateActionsWithNewAssetDrafts(
          @Nonnull final D newResource,
          @Nonnull final List<Asset> oldAssets,
          @Nonnull final List<AssetDraft> newAssetDrafts,
          @Nonnull final AssetActionFactory<T, D> assetActionFactory,
          @Nonnull final BaseSyncOptions syncOptions)
          throws BuildUpdateActionException {

    // Asset set that has only the keys of the assets which should be removed, this is used in the
    // method
    // #buildChangeAssetOrderUpdateAction in order to compare the state of the asset lists after the
    // remove actions
    // have already been applied.
    final HashSet<String> removedAssetKeys = new HashSet<>();

    final Map<String, Asset> oldAssetsKeyMap = new HashMap<>();

    oldAssets.forEach(
        asset -> {
          String assetKey = asset.getKey();
          if (isNotBlank(assetKey)) {
            oldAssetsKeyMap.put(assetKey, asset);
          } else {
            syncOptions.applyWarningCallback(
                new SyncException(format(ASSET_KEY_NOT_SET, "id: " + asset.getId())), asset, null);
          }
        });
    final Map<String, AssetDraft> newAssetDraftsKeyMap = new HashMap<>();
    try {
      newAssetDrafts.forEach(
          newAsset -> {
            String assetKey = newAsset.getKey();
            if (isNotBlank(assetKey)) {
              newAssetDraftsKeyMap.merge(
                  assetKey,
                  newAsset,
                  (assetDraftA, assetDraftB) -> {
                    throw new DuplicateKeyException(
                        "Supplied asset drafts have duplicate keys. Asset keys are"
                            + " expected to be unique inside their container (a product variant or a category).");
                  });

            } else {
              syncOptions.applyWarningCallback(
                  new SyncException(format(ASSET_KEY_NOT_SET, "name: " + newAsset.getName())),
                  null,
                  newAsset);
            }
          });

    } catch (final DuplicateKeyException exception) {
      throw new BuildUpdateActionException(exception);
    }

    // It is important to have a changeAssetOrder action before an addAsset action, since
    // changeAssetOrder requires
    // asset ids for sorting them, and new assets don't have ids yet since they are generated
    // by CTP after an asset is created. Therefore, the order of update actions must be:

    // 1. Remove or compare if matching.
    final List<UpdateAction<T>> updateActions =
        buildRemoveAssetOrAssetUpdateActions(
            newResource, oldAssets, removedAssetKeys, newAssetDraftsKeyMap, assetActionFactory);

    // 2. Compare ordering of assets and add a ChangeAssetOrder action if needed.
    buildChangeAssetOrderUpdateAction(
            oldAssets, newAssetDrafts, removedAssetKeys, assetActionFactory)
        .ifPresent(updateActions::add);

    // For every new asset draft, If it doesn't exist in the old assets, then add an AddAsset action
    // to the list
    // of update actions.
    updateActions.addAll(
        buildAddAssetUpdateActions(newAssetDrafts, oldAssetsKeyMap, assetActionFactory));

    return updateActions;
  }

  /**
   * Checks if there are any asset which are not existing in the {@code newAssetDraftsKeyMap}. If
   * there are, then "remove" asset update actions are built using the instance of {@link
   * AssetActionFactory} supplied to remove these assets. Otherwise, if there are no assets that
   * should be removed, an empty list is returned.
   *
   * @param oldAssets the list of old {@link Asset}s
   * @param removedAssetKeys a set containing keys of removed assets.
   * @param newAssetDraftsKeyMap a map of keys to asset drafts of the new list of asset drafts
   * @param assetActionFactory factory responsible for building asset update actions.
   * @param <T> the type of the resource the asset update action is built for.
   * @return a list of asset update actions on the resource of type T if there are new assets that
   *     should be added. Otherwise, if the assets order is identical, an empty optional is
   *     returned.
   */
  @Nonnull
  private static <T extends Resource, D> List<UpdateAction<T>> buildRemoveAssetOrAssetUpdateActions(
      @Nonnull final D newResource,
      @Nonnull final List<Asset> oldAssets,
      @Nonnull final Set<String> removedAssetKeys,
      @Nonnull final Map<String, AssetDraft> newAssetDraftsKeyMap,
      @Nonnull final AssetActionFactory<T, D> assetActionFactory) {
    // For every old asset, If it doesn't exist anymore in the new asset drafts,
    // then add a RemoveAsset action to the list of update actions. If the asset still exists in the
    // new draft,
    // then compare the asset fields (name, desc, etc..), and add the computed actions to the list
    // of update
    // actions.
    return oldAssets.stream()
        .filter(asset -> isNotBlank(asset.getKey()))
        .map(
            oldAsset -> {
              final String oldAssetKey = oldAsset.getKey();
              final AssetDraft matchingNewAssetDraft = newAssetDraftsKeyMap.get(oldAssetKey);
              return ofNullable(matchingNewAssetDraft)
                  .map(
                      assetDraft -> // If asset exists, compare the two assets.
                      assetActionFactory.buildAssetActions(newResource, oldAsset, assetDraft))
                  .orElseGet(
                      () -> { // If asset doesn't exist, remove asset.
                        removedAssetKeys.add(oldAssetKey);
                        return singletonList(
                            assetActionFactory.buildRemoveAssetAction(oldAssetKey));
                      });
            })
        .flatMap(Collection::stream)
        .collect(toCollection(ArrayList::new));
  }

  /**
   * Compares the order of a list of old {@link Asset}s and a list of new {@link AssetDraft}s. If
   * there is a change in order, then a change asset order (with the new order) is built. The method
   * filters out the removed assets from the old asset list using the keys in the {@code
   * removedAssetKeys} {@link Set}. If there are no changes in order an empty optional is returned.
   *
   * @param oldAssets the list of old {@link Asset}s
   * @param newAssetDrafts the list of new {@link AssetDraft}s
   * @param removedAssetKeys a set containing keys of removed assets.
   * @param assetActionFactory factory responsible for building asset update actions.
   * @param <T> the type of the resource the asset update action is built for.
   * @return a list of asset update actions on the resource of type T if the list of the order of
   *     assets is not identical. Otherwise, if the assets order is identical, an empty optional is
   *     returned.
   */
  @Nonnull
  private static <T extends Resource, D>
      Optional<UpdateAction<T>> buildChangeAssetOrderUpdateAction(
          @Nonnull final List<Asset> oldAssets,
          @Nonnull final List<AssetDraft> newAssetDrafts,
          @Nonnull final Set<String> removedAssetKeys,
          @Nonnull final AssetActionFactory<T, D> assetActionFactory) {

    final Map<String, String> oldAssetKeyToIdMap =
        oldAssets.stream()
            .filter(asset -> isNotBlank(asset.getKey()))
            .collect(toMap(Asset::getKey, Asset::getId));

    final List<String> newOrder =
        newAssetDrafts.stream()
            .filter(asset -> isNotBlank(asset.getKey()))
            .map(AssetDraft::getKey)
            .map(oldAssetKeyToIdMap::get)
            .filter(Objects::nonNull)
            .collect(toList());

    final List<String> oldOrder =
        oldAssets.stream()
            .filter(asset -> isNotBlank(asset.getKey()))
            .filter(asset -> !removedAssetKeys.contains(asset.getKey()))
            .map(Asset::getId)
            .collect(toList());

    return buildUpdateAction(
        oldOrder, newOrder, () -> assetActionFactory.buildChangeAssetOrderAction(newOrder));
  }

  /**
   * Checks if there are any new asset drafts which are not existing in the {@code oldAssetsKeyMap}.
   * If there are, then "add" asset update actions are built using the instance of {@link
   * AssetActionFactory} supplied to add the missing assets. Otherwise, if there are no new assets,
   * then an empty list is returned.
   *
   * @param newAssetDrafts the list of new {@link AssetDraft}s
   * @param oldAssetsKeyMap a map of keys to assets of the old list of assets
   * @param assetActionFactory factory responsible for building asset update actions.
   * @param <T> the type of the resource the asset update action is built for.
   * @return a list of asset update actions on the resource of type T if there are new assets that
   *     should be added. Otherwise, if the assets order is identical, an empty optional is
   *     returned.
   */
  @Nonnull
  private static <T extends Resource, D> List<UpdateAction<T>> buildAddAssetUpdateActions(
      @Nonnull final List<AssetDraft> newAssetDrafts,
      @Nonnull final Map<String, Asset> oldAssetsKeyMap,
      @Nonnull final AssetActionFactory<T, D> assetActionFactory) {

    final ArrayList<Optional<UpdateAction<T>>> optionalActions =
        IntStream.range(0, newAssetDrafts.size())
            .mapToObj(
                assetDraftIndex ->
                    ofNullable(newAssetDrafts.get(assetDraftIndex))
                        .filter(
                            assetDraft ->
                                isNotBlank(assetDraft.getKey())
                                    && !oldAssetsKeyMap.containsKey(assetDraft.getKey()))
                        .map(
                            assetDraft ->
                                assetActionFactory.buildAddAssetAction(
                                    assetDraft, assetDraftIndex)))
            .collect(toCollection(ArrayList::new));
    return filterEmptyOptionals(optionalActions);
  }

  private AssetsUpdateActionUtils() {}
}
