package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetName;
import io.sphere.sdk.products.commands.updateactions.SetAssetDescription;
import io.sphere.sdk.products.commands.updateactions.SetAssetSources;
import io.sphere.sdk.products.commands.updateactions.SetAssetTags;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;

//TODO: CONSIDER CHANGE NAME.
// TODO: Add tests.
public final class ProductVariantAssetUpdateActionUtils {
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
            Product.class, variantId, Asset::getId, asset -> Asset.resourceTypeId(), Asset::getKey, syncOptions);
    }

    /*
    @Nonnull
    public static List<UpdateAction<Product>> buildPriceCustomUpdateActions(
        @Nonnull final Price oldPrice,
        @Nonnull final PriceDraft newPrice,
        @Nonnull final ProductSyncOptions syncOptions) {
        return CustomUpdateActionUtils.buildCustomUpdateActions(oldPrice, newPrice, Product.class, -1,
            Price::getId, price -> Price.resourceTypeId(), Price::getId, syncOptions);
    }*/
}
