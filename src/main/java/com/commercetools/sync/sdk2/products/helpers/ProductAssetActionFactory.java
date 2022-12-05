package com.commercetools.sync.sdk2.products.helpers;

import static com.commercetools.sync.sdk2.products.utils.ProductVariantAssetUpdateActionUtils.buildActions;

import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.product.ProductAddAssetAction;
import com.commercetools.api.models.product.ProductChangeAssetOrderAction;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductRemoveAssetAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.helpers.AssetActionFactory;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import java.util.List;
import javax.annotation.Nonnull;

public final class ProductAssetActionFactory
    extends AssetActionFactory<ProductUpdateAction, ProductDraft> {
  private Long variantId;

  private BaseSyncOptions syncOptions;

  public ProductAssetActionFactory(
      @Nonnull final Long variantId, @Nonnull final ProductSyncOptions syncOptions) {
    this.variantId = variantId;
    this.syncOptions = syncOptions;
  }

  @Override
  public List<ProductUpdateAction> buildAssetActions(
      @Nonnull final ProductDraft newResource,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAssetDraft) {
    return buildActions(
        newResource, variantId, oldAsset, newAssetDraft, (ProductSyncOptions) syncOptions);
  }

  @Override
  public ProductUpdateAction buildRemoveAssetAction(@Nonnull final String assetKey) {
    return ProductRemoveAssetAction.builder()
        .variantId(variantId)
        .assetKey(assetKey)
        .staged(true)
        .build();
  }

  @Override
  public ProductUpdateAction buildChangeAssetOrderAction(
      @Nonnull final List<String> newAssetOrder) {
    return ProductChangeAssetOrderAction.builder()
        .variantId(variantId)
        .assetOrder(newAssetOrder)
        .staged(true)
        .build();
  }

  @Override
  public ProductUpdateAction buildAddAssetAction(
      @Nonnull final AssetDraft assetDraft, @Nonnull final Integer position) {
    return ProductAddAssetAction.builder()
        .variantId(variantId)
        .asset(assetDraft)
        .position(position)
        .staged(true)
        .build();
  }
}
