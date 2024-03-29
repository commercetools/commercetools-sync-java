package com.commercetools.sync.products.utils;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;

import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductChangeAssetNameAction;
import com.commercetools.api.models.product.ProductSetAssetDescriptionAction;
import com.commercetools.api.models.product.ProductSetAssetSourcesAction;
import com.commercetools.api.models.product.ProductSetAssetTagsAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.sync.commons.models.AssetCustomTypeAdapter;
import com.commercetools.sync.commons.models.AssetDraftCustomTypeAdapter;
import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.helpers.AssetCustomActionBuilder;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

public final class ProductVariantAssetUpdateActionUtils {

  /**
   * Compares all the fields of an {@link Asset} and an {@link AssetDraft} and returns a list of
   *
   * <p>{@link ProductUpdateAction}&lt;{@link Product}&gt; as a result. If both the {@link Asset}
   * and the {@link AssetDraft} have identical fields, then no update action is needed and hence an
   * empty {@link List} is returned.
   *
   * @param <D> Type of the mainresource draft
   * @param newResource new mainresource draft, which contains the asset to update.
   * @param variantId the variantId needed for building the update action.
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new fields.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return A list with the update actions or an empty list if the asset fields are identical.
   */
  @Nonnull
  public static <D> List<ProductUpdateAction> buildActions(
      @Nonnull final D newResource,
      @Nonnull final Long variantId,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAsset,
      @Nonnull final ProductSyncOptions syncOptions) {

    final List<ProductUpdateAction> updateActions =
        filterEmptyOptionals(
            buildChangeAssetNameUpdateAction(variantId, oldAsset, newAsset),
            buildSetAssetDescriptionUpdateAction(variantId, oldAsset, newAsset),
            buildSetAssetTagsUpdateAction(variantId, oldAsset, newAsset),
            buildSetAssetSourcesUpdateAction(variantId, oldAsset, newAsset));

    updateActions.addAll(
        buildCustomUpdateActions(newResource, variantId, oldAsset, newAsset, syncOptions));
    return updateActions;
  }

  /**
   * Compares the {@link LocalizedString} names of an {@link Asset} and an {@link AssetDraft} and
   * returns an {@link ProductUpdateAction}&lt;{@link Product}&gt; as a result in an {@link
   * Optional}. If both the {@link Asset} and the {@link AssetDraft} have the same name, then no
   * update action is needed and hence an empty {@link Optional} is returned.
   *
   * @param variantId the variantId needed for building the update action.
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new name.
   * @return A filled optional with the update action or an empty optional if the names are
   *     identical.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildChangeAssetNameUpdateAction(
      @Nonnull final Long variantId,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAsset) {
    return buildUpdateAction(
        oldAsset.getName(),
        newAsset.getName(),
        () ->
            ProductChangeAssetNameAction.builder()
                .assetKey(oldAsset.getKey())
                .variantId(variantId)
                .name(newAsset.getName())
                .staged(true)
                .build());
  }

  /**
   * Compares the {@link LocalizedString} descriptions of an {@link Asset} and an {@link AssetDraft}
   * and returns an {@link ProductUpdateAction}&lt;{@link Product}&gt; as a result in an {@link
   * Optional}. If both the {@link Asset} and the {@link AssetDraft} have the same description, then
   * no update action is needed and hence an empty {@link Optional} is returned.
   *
   * @param variantId the variantId needed for building the update action.
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new description.
   * @return A filled optional with the update action or an empty optional if the descriptions are
   *     identical.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildSetAssetDescriptionUpdateAction(
      @Nonnull final Long variantId,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAsset) {
    return buildUpdateAction(
        oldAsset.getDescription(),
        newAsset.getDescription(),
        () ->
            ProductSetAssetDescriptionAction.builder()
                .variantId(variantId)
                .assetKey(oldAsset.getKey())
                .description(newAsset.getDescription())
                .staged(true)
                .build());
  }

  /**
   * Compares the tags of an {@link Asset} and an {@link AssetDraft} and returns an {@link
   * ProductUpdateAction}&lt;{@link Product}&gt; as a result in an {@link Optional}. If both the
   * {@link Asset} and the {@link AssetDraft} have the same tags, then no update action is needed
   * and hence an empty {@link Optional} is returned.
   *
   * @param variantId the variantId needed for building the update action.
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new tags.
   * @return A filled optional with the update action or an empty optional if the tags are
   *     identical.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildSetAssetTagsUpdateAction(
      @Nonnull final Long variantId,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAsset) {
    return buildUpdateAction(
        oldAsset.getTags(),
        newAsset.getTags(),
        () ->
            ProductSetAssetTagsAction.builder()
                .variantId(variantId)
                .assetKey(oldAsset.getKey())
                .tags(newAsset.getTags())
                .staged(true)
                .build());
  }

  /**
   * Compares the sources of an {@link Asset} and an {@link AssetDraft} and returns an {@link
   * ProductUpdateAction}&lt;{@link Product}&gt; as a result in an {@link Optional}. If both the
   * {@link Asset} and the {@link AssetDraft} have the same sources, then no update action is needed
   * and hence an empty {@link Optional} is returned.
   *
   * @param variantId the variantId needed for building the update action.
   * @param oldAsset the asset which should be updated.
   * @param newAsset the asset draft where we get the new sources.
   * @return A filled optional with the update action or an empty optional if the sources are
   *     identical.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildSetAssetSourcesUpdateAction(
      @Nonnull final Long variantId,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAsset) {
    return buildUpdateAction(
        oldAsset.getSources(),
        newAsset.getSources(),
        () ->
            ProductSetAssetSourcesAction.builder()
                .variantId(variantId)
                .assetKey(oldAsset.getKey())
                .sources(newAsset.getSources())
                .staged(true)
                .build());
  }

  /**
   * Compares the custom fields and custom types of an {@link Asset} and an {@link AssetDraft} and
   *
   * <p>returns a list of {@link ProductUpdateAction}&lt;{@link Product}&gt; as a result. If both
   * the {@link Asset} and the {@link AssetDraft} have identical custom fields and types, then no
   * update action is needed and hence an empty {@link List} is returned.
   *
   * @param <D> Type of the mainresource draft
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
  public static <D> List<ProductUpdateAction> buildCustomUpdateActions(
      @Nonnull final D newResource,
      @Nonnull final Long variantId,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAsset,
      @Nonnull final ProductSyncOptions syncOptions) {

    AssetCustomTypeAdapter assetCustomTypeAdapter = AssetCustomTypeAdapter.of(oldAsset);
    AssetDraftCustomTypeAdapter assetDraftCustomTypeAdapter =
        AssetDraftCustomTypeAdapter.of(newAsset);
    return CustomUpdateActionUtils.buildCustomUpdateActions(
        newResource,
        assetCustomTypeAdapter,
        assetDraftCustomTypeAdapter,
        new AssetCustomActionBuilder(),
        variantId,
        AssetCustomTypeAdapter::getId,
        AssetCustomTypeAdapter::getTypeId,
        AssetCustomTypeAdapter::getKey,
        syncOptions);
  }

  private ProductVariantAssetUpdateActionUtils() {}
}
