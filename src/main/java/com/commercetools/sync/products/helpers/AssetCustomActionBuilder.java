package com.commercetools.sync.products.helpers;

import static io.sphere.sdk.types.CustomFieldsDraft.ofTypeIdAndJson;

import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomField;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomType;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AssetCustomActionBuilder implements GenericCustomActionBuilder<Product> {

  @Override
  @Nonnull
  public UpdateAction<Product> buildRemoveCustomTypeAction(
      @Nullable final Integer variantId, @Nullable final String assetKey) {
    return SetAssetCustomType.ofVariantIdAndAssetKey(variantId, assetKey, null, true);
  }

  @Override
  @Nonnull
  public UpdateAction<Product> buildSetCustomTypeAction(
      @Nullable final Integer variantId,
      @Nullable final String assetKey,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, JsonNode> customFieldsJsonMap) {
    return SetAssetCustomType.ofVariantIdAndAssetKey(
        variantId, assetKey, ofTypeIdAndJson(customTypeId, customFieldsJsonMap), true);
  }

  @Override
  @Nonnull
  public UpdateAction<Product> buildSetCustomFieldAction(
      @Nullable final Integer variantId,
      @Nullable final String assetKey,
      @Nullable final String customFieldName,
      @Nullable final JsonNode customFieldValue) {
    return SetAssetCustomField.ofVariantIdUsingJsonAndAssetKey(
        variantId, assetKey, customFieldName, customFieldValue, true);
  }
}
