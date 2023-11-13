package com.commercetools.sync.categories.helpers;

import com.commercetools.api.models.category.CategorySetAssetCustomFieldActionBuilder;
import com.commercetools.api.models.category.CategorySetAssetCustomTypeActionBuilder;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AssetCustomActionBuilder implements GenericCustomActionBuilder<CategoryUpdateAction> {

  @Nonnull
  @Override
  public CategoryUpdateAction buildRemoveCustomTypeAction(
      @Nullable final Long variantId, @Nullable final String assetKey) {
    return CategorySetAssetCustomTypeActionBuilder.of().build();
  }

  @Nonnull
  @Override
  public CategoryUpdateAction buildSetCustomTypeAction(
      @Nullable final Long variantId,
      @Nullable final String assetKey,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, Object> customFieldsJsonMap) {
    return CategorySetAssetCustomTypeActionBuilder.of()
        .assetKey(assetKey)
        .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(customTypeId))
        .fields(fieldContainerBuilder -> fieldContainerBuilder.values(customFieldsJsonMap))
        .build();
  }

  @Nonnull
  @Override
  public CategoryUpdateAction buildSetCustomFieldAction(
      @Nullable final Long variantId,
      @Nullable final String assetKey,
      @Nullable final String customFieldName,
      @Nullable final Object customFieldValue) {
    return CategorySetAssetCustomFieldActionBuilder.of()
        .assetKey(assetKey)
        .name(customFieldName)
        .value(customFieldValue)
        .build();
  }
}
