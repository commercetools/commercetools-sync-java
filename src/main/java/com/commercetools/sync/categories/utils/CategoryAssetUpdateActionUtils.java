package com.commercetools.sync.categories.utils;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;

import com.commercetools.api.models.category.CategoryChangeAssetNameActionBuilder;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategorySetAssetDescriptionActionBuilder;
import com.commercetools.api.models.category.CategorySetAssetSourcesActionBuilder;
import com.commercetools.api.models.category.CategorySetAssetTagsActionBuilder;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.helpers.AssetCustomActionBuilder;
import com.commercetools.sync.commons.models.AssetCustomTypeAdapter;
import com.commercetools.sync.commons.models.AssetDraftCustomTypeAdapter;
import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

public final class CategoryAssetUpdateActionUtils {

  /**
   * Compares all the fields of an {@link Asset} and an {@link AssetDraft} and returns a list of
   * {@link com.commercetools.api.models.category.CategoryUpdateAction} as a result. If both the
   * {@link Asset} and the {@link AssetDraft} have identical fields, then no update action is needed
   * and hence an empty {@link java.util.List} is returned.
   *
   * @param newResource new mainresource draft, which contains the asset to update.
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new fields.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return A list with the update actions or an empty list if the asset fields are identical.
   */
  @Nonnull
  public static List<CategoryUpdateAction> buildActions(
      @Nonnull final CategoryDraft newResource,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAsset,
      @Nonnull final CategorySyncOptions syncOptions) {

    final List<CategoryUpdateAction> updateActions =
        filterEmptyOptionals(
            buildChangeAssetNameUpdateAction(oldAsset, newAsset),
            buildSetAssetDescriptionUpdateAction(oldAsset, newAsset),
            buildSetAssetTagsUpdateAction(oldAsset, newAsset),
            buildSetAssetSourcesUpdateAction(oldAsset, newAsset));

    updateActions.addAll(buildCustomUpdateActions(newResource, oldAsset, newAsset, syncOptions));
    return updateActions;
  }

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} names of an {@link
   * Asset} and an {@link AssetDraft} and returns an {@link CategoryUpdateAction} as a result in an
   * {@link java.util.Optional}. If both the {@link Asset} and the {@link AssetDraft} have the same
   * name, then no update action is needed and hence an empty {@link java.util.Optional} is
   * returned.
   *
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new name.
   * @return A filled optional with the update action or an empty optional if the names are
   *     identical.
   */
  @Nonnull
  public static Optional<CategoryUpdateAction> buildChangeAssetNameUpdateAction(
      @Nonnull final Asset oldAsset, @Nonnull final AssetDraft newAsset) {
    return buildUpdateAction(
        oldAsset.getName(),
        newAsset.getName(),
        () ->
            CategoryChangeAssetNameActionBuilder.of()
                .assetKey(oldAsset.getKey())
                .name(newAsset.getName())
                .build());
  }

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} descriptions of an
   * {@link Asset} and an {@link AssetDraft} and returns an {@link CategoryUpdateAction} as a result
   * in an {@link java.util.Optional}. If both the {@link Asset} and the {@link AssetDraft} have the
   * same description, then no update action is needed and hence an empty {@link java.util.Optional}
   * is returned.
   *
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new description.
   * @return A filled optional with the update action or an empty optional if the descriptions are
   *     identical.
   */
  @Nonnull
  public static Optional<CategoryUpdateAction> buildSetAssetDescriptionUpdateAction(
      @Nonnull final Asset oldAsset, @Nonnull final AssetDraft newAsset) {
    return buildUpdateAction(
        oldAsset.getDescription(),
        newAsset.getDescription(),
        () ->
            CategorySetAssetDescriptionActionBuilder.of()
                .assetKey(oldAsset.getKey())
                .description(newAsset.getDescription())
                .build());
  }

  /**
   * Compares the tags of an {@link Asset} and an {@link AssetDraft} and returns an {@link
   * CategoryUpdateAction} as a result in an {@link java.util.Optional}. If both the {@link Asset}
   * and the {@link AssetDraft} have the same tags, then no update action is needed and hence an
   * empty {@link java.util.Optional} is returned.
   *
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new tags.
   * @return A filled optional with the update action or an empty optional if the tags are
   *     identical.
   */
  @Nonnull
  public static Optional<CategoryUpdateAction> buildSetAssetTagsUpdateAction(
      @Nonnull final Asset oldAsset, @Nonnull final AssetDraft newAsset) {
    return buildUpdateAction(
        oldAsset.getTags(),
        newAsset.getTags(),
        () ->
            CategorySetAssetTagsActionBuilder.of()
                .assetKey(oldAsset.getKey())
                .tags(newAsset.getTags())
                .build());
  }

  /**
   * Compares the sources of an {@link Asset} and an {@link AssetDraft} and returns an {@link
   * CategoryUpdateAction} as a result in an {@link java.util.Optional}. If both the {@link Asset}
   * and the {@link AssetDraft} have the same sources, then no update action is needed and hence an
   * empty {@link java.util.Optional} is returned.
   *
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new sources.
   * @return A filled optional with the update action or an empty optional if the sources are
   *     identical.
   */
  @Nonnull
  public static Optional<CategoryUpdateAction> buildSetAssetSourcesUpdateAction(
      @Nonnull final Asset oldAsset, @Nonnull final AssetDraft newAsset) {
    return buildUpdateAction(
        oldAsset.getSources(),
        newAsset.getSources(),
        () ->
            CategorySetAssetSourcesActionBuilder.of()
                .assetKey(oldAsset.getKey())
                .sources(newAsset.getSources())
                .build());
  }

  /**
   * Compares the custom fields and custom types of an {@link Asset} and an {@link AssetDraft} and
   * returns a list of {@link CategoryUpdateAction} as a result. If both the {@link Asset} and the
   * {@link AssetDraft} have identical custom fields and types, then no update action is needed and
   * hence an empty {@link java.util.List} is returned.
   *
   * @param newCategory category in a source project, which contains the updated asset.
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new custom fields and types.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return A list with the custom field/type update actions or an empty list if the custom
   *     fields/types are identical.
   */
  @Nonnull
  public static List<CategoryUpdateAction> buildCustomUpdateActions(
      @Nonnull final CategoryDraft newCategory,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAsset,
      @Nonnull final CategorySyncOptions syncOptions) {

    return CustomUpdateActionUtils.buildCustomUpdateActions(
        newCategory,
        AssetCustomTypeAdapter.of(oldAsset),
        AssetDraftCustomTypeAdapter.of(newAsset),
        new AssetCustomActionBuilder(),
        -1L,
        AssetCustomTypeAdapter::getId,
        asset -> ResourceTypeId.ASSET.getJsonName(),
        AssetCustomTypeAdapter::getKey,
        syncOptions);
  }

  private CategoryAssetUpdateActionUtils() {}
}
