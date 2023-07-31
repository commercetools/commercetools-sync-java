package com.commercetools.sync.sdk2.products.helpers;

import com.commercetools.api.models.product.ProductSetAssetCustomFieldAction;
import com.commercetools.api.models.product.ProductSetAssetCustomTypeAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.type.FieldContainer;
import com.commercetools.api.models.type.TypeResourceIdentifier;
import com.commercetools.sync.sdk2.commons.helpers.GenericCustomActionBuilder;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AssetCustomActionBuilder implements GenericCustomActionBuilder<ProductUpdateAction> {

  @Override
  @Nonnull
  public ProductUpdateAction buildRemoveCustomTypeAction(
      @Nullable final Long variantId, @Nullable final String assetKey) {
    return ProductSetAssetCustomTypeAction.builder().build();
  }

  @Override
  @Nonnull
  public ProductUpdateAction buildSetCustomTypeAction(
      @Nullable final Long variantId,
      @Nullable final String assetKey,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, Object> customFieldsJsonMap) {

    return ProductSetAssetCustomTypeAction.builder()
        .variantId(variantId)
        .assetKey(assetKey)
        .type(TypeResourceIdentifier.builder().id(customTypeId).build())
        .fields(FieldContainer.builder().values(customFieldsJsonMap).build())
        .staged(true)
        .build();
  }

  @Override
  @Nonnull
  public ProductUpdateAction buildSetCustomFieldAction(
      @Nullable final Long variantId,
      @Nullable final String assetKey,
      @Nullable final String customFieldName,
      @Nullable final Object customFieldValue) {
    return ProductSetAssetCustomFieldAction.builder()
        .variantId(variantId)
        .assetKey(assetKey)
        .name(customFieldName)
        .value(customFieldValue)
        .staged(true)
        .build();
  }
}
