package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.products.utils.AssetUtils.createAssetDraft;
import static com.commercetools.sync.sdk2.products.utils.PriceUtils.createPriceDraft;

import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;

public class VariantUtils {

  public static ProductVariantDraft createProductVariantDraft(ProductVariant productVariant) {
    return ProductVariantDraftBuilder.of()
        .sku(productVariant.getSku())
        .key(productVariant.getKey())
        .assets(createAssetDraft(productVariant.getAssets()))
        .images(productVariant.getImages())
        .attributes(productVariant.getAttributes())
        .prices(createPriceDraft(productVariant.getPrices()))
        .build();
  }
}
