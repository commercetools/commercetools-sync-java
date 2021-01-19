package com.commercetools.sync.products.utils;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;

import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.helpers.AssetCustomActionBuilder;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetName;
import io.sphere.sdk.products.commands.updateactions.SetAssetDescription;
import io.sphere.sdk.products.commands.updateactions.SetAssetSources;
import io.sphere.sdk.products.commands.updateactions.SetAssetTags;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

public final class ProductVariantAssetUpdateActionUtils {

  /**
   * Compares all the fields of an {@link Asset} and an {@link AssetDraft} and returns a list of
   * {@link UpdateAction}&lt;{@link Product}&gt; as a result. If both the {@link Asset} and the
   * {@link AssetDraft} have identical fields, then no update action is needed and hence an empty
   * {@link List} is returned.
   *
   * @param <D> Type of the mainresource draft
   * @param oldResource mainresource, whose asset should be updated.
   * @param newResource new mainresource draft, which contains the asset to update.
   * @param variantId the variantId needed for building the update action.
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new fields.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return A list with the update actions or an empty list if the asset fields are identical.
   */
  @Nonnull
  public static <D> List<UpdateAction<Product>> buildActions(
      @Nonnull final Resource oldResource,
      @Nonnull final D newResource,
      @Nonnull final Integer variantId,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAsset,
      @Nonnull final ProductSyncOptions syncOptions) {

    final List<UpdateAction<Product>> updateActions =
        filterEmptyOptionals(
            buildChangeAssetNameUpdateAction(variantId, oldAsset, newAsset),
            buildSetAssetDescriptionUpdateAction(variantId, oldAsset, newAsset),
            buildSetAssetTagsUpdateAction(variantId, oldAsset, newAsset),
            buildSetAssetSourcesUpdateAction(variantId, oldAsset, newAsset));

    updateActions.addAll(
        buildCustomUpdateActions(
            oldResource, newResource, variantId, oldAsset, newAsset, syncOptions));
    return updateActions;
  }

  /**
   * Compares the {@link LocalizedString} names of an {@link Asset} and an {@link AssetDraft} and
   * returns an {@link UpdateAction}&lt;{@link Product}&gt; as a result in an {@link Optional}. If
   * both the {@link Asset} and the {@link AssetDraft} have the same name, then no update action is
   * needed and hence an empty {@link Optional} is returned.
   *
   * @param variantId the variantId needed for building the update action.
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new name.
   * @return A filled optional with the update action or an empty optional if the names are
   *     identical.
   */
  @Nonnull
  public static Optional<UpdateAction<Product>> buildChangeAssetNameUpdateAction(
      @Nonnull final Integer variantId,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAsset) {
    return buildUpdateAction(
        oldAsset.getName(),
        newAsset.getName(),
        () ->
            ChangeAssetName.ofAssetKeyAndVariantId(
                variantId, oldAsset.getKey(), newAsset.getName(), true));
  }

  /**
   * Compares the {@link LocalizedString} descriptions of an {@link Asset} and an {@link AssetDraft}
   * and returns an {@link UpdateAction}&lt;{@link Product}&gt; as a result in an {@link Optional}.
   * If both the {@link Asset} and the {@link AssetDraft} have the same description, then no update
   * action is needed and hence an empty {@link Optional} is returned.
   *
   * @param variantId the variantId needed for building the update action.
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new description.
   * @return A filled optional with the update action or an empty optional if the descriptions are
   *     identical.
   */
  @Nonnull
  public static Optional<UpdateAction<Product>> buildSetAssetDescriptionUpdateAction(
      @Nonnull final Integer variantId,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAsset) {
    return buildUpdateAction(
        oldAsset.getDescription(),
        newAsset.getDescription(),
        () ->
            SetAssetDescription.ofVariantIdAndAssetKey(
                variantId, oldAsset.getKey(), newAsset.getDescription(), true));
  }

  /**
   * Compares the tags of an {@link Asset} and an {@link AssetDraft} and returns an {@link
   * UpdateAction}&lt;{@link Product}&gt; as a result in an {@link Optional}. If both the {@link
   * Asset} and the {@link AssetDraft} have the same tags, then no update action is needed and hence
   * an empty {@link Optional} is returned.
   *
   * @param variantId the variantId needed for building the update action.
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new tags.
   * @return A filled optional with the update action or an empty optional if the tags are
   *     identical.
   */
  @Nonnull
  public static Optional<UpdateAction<Product>> buildSetAssetTagsUpdateAction(
      @Nonnull final Integer variantId,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAsset) {
    return buildUpdateAction(
        oldAsset.getTags(),
        newAsset.getTags(),
        () ->
            SetAssetTags.ofVariantIdAndAssetKey(
                variantId, oldAsset.getKey(), newAsset.getTags(), true));
  }

  /**
   * Compares the sources of an {@link Asset} and an {@link AssetDraft} and returns an {@link
   * UpdateAction}&lt;{@link Product}&gt; as a result in an {@link Optional}. If both the {@link
   * Asset} and the {@link AssetDraft} have the same sources, then no update action is needed and
   * hence an empty {@link Optional} is returned.
   *
   * @param variantId the variantId needed for building the update action.
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new sources.
   * @return A filled optional with the update action or an empty optional if the sources are
   *     identical.
   */
  @Nonnull
  public static Optional<UpdateAction<Product>> buildSetAssetSourcesUpdateAction(
      @Nonnull final Integer variantId,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAsset) {
    return buildUpdateAction(
        oldAsset.getSources(),
        newAsset.getSources(),
        () ->
            SetAssetSources.ofVariantIdAndAssetKey(
                variantId, oldAsset.getKey(), newAsset.getSources(), true));
  }

  /**
   * Compares the custom fields and custom types of an {@link Asset} and an {@link AssetDraft} and
   * returns a list of {@link UpdateAction}&lt;{@link Product}&gt; as a result. If both the {@link
   * Asset} and the {@link AssetDraft} have identical custom fields and types, then no update action
   * is needed and hence an empty {@link List} is returned.
   *
   * @param <D> Type of the mainresource draft
   * @param oldResource mainresource, whose asset should be updated.
   * @param newResource new mainresource draft, which contains the asset to update.
   * @param variantId the variantId needed for building the update action.
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new custom fields and types.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return A list with the custom field/type update actions or an empty list if the custom
   *     fields/types are identical.
   */
  @Nonnull
  public static <D> List<UpdateAction<Product>> buildCustomUpdateActions(
      @Nonnull final Resource oldResource,
      @Nonnull final D newResource,
      @Nonnull final Integer variantId,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAsset,
      @Nonnull final ProductSyncOptions syncOptions) {

    return CustomUpdateActionUtils.buildCustomUpdateActions(
        oldResource,
        newResource,
        oldAsset,
        newAsset,
        new AssetCustomActionBuilder(),
        variantId,
        Asset::getId,
        asset -> Asset.resourceTypeId(),
        Asset::getKey,
        syncOptions);
  }

  private ProductVariantAssetUpdateActionUtils() {}
}
