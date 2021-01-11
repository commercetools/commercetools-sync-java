package com.commercetools.sync.products.helpers;

import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildActions;

import com.commercetools.sync.commons.helpers.AssetActionFactory;
import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.AddAsset;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.products.commands.updateactions.RemoveAsset;
import java.util.List;
import javax.annotation.Nonnull;

public final class ProductAssetActionFactory extends AssetActionFactory<Product, ProductDraft> {
  private Integer variantId;

  public ProductAssetActionFactory(
      @Nonnull final Integer variantId, @Nonnull final ProductSyncOptions syncOptions) {
    this.variantId = variantId;
    this.syncOptions = syncOptions;
  }

  @Override
  public List<UpdateAction<Product>> buildAssetActions(
      @Nonnull final Product oldResource,
      @Nonnull final ProductDraft newResource,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAssetDraft) {
    return buildActions(
        oldResource,
        newResource,
        variantId,
        oldAsset,
        newAssetDraft,
        (ProductSyncOptions) syncOptions);
  }

  @Override
  public UpdateAction<Product> buildRemoveAssetAction(@Nonnull final String assetKey) {
    return RemoveAsset.ofVariantIdWithKey(variantId, assetKey, true);
  }

  @Override
  public UpdateAction<Product> buildChangeAssetOrderAction(
      @Nonnull final List<String> newAssetOrder) {
    return ChangeAssetOrder.ofVariantId(variantId, newAssetOrder, true);
  }

  @Override
  public UpdateAction<Product> buildAddAssetAction(
      @Nonnull final AssetDraft assetDraft, @Nonnull final Integer position) {
    return AddAsset.ofVariantId(variantId, assetDraft).withPosition(position).withStaged(true);
  }
}
