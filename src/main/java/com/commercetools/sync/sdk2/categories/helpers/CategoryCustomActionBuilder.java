package com.commercetools.sync.sdk2.categories.helpers;

import com.commercetools.api.models.category.CategorySetCustomFieldActionBuilder;
import com.commercetools.api.models.category.CategorySetCustomTypeActionBuilder;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.sync.sdk2.commons.helpers.GenericCustomActionBuilder;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CategoryCustomActionBuilder
    implements GenericCustomActionBuilder<CategoryUpdateAction> {
  @Nonnull
  @Override
  public CategoryUpdateAction buildRemoveCustomTypeAction(
      @Nullable final Long variantId, @Nullable final String objectId) {
    return CategorySetCustomTypeActionBuilder.of().build();
  }

  @Nonnull
  @Override
  public CategoryUpdateAction buildSetCustomTypeAction(
      @Nullable final Long variantId,
      @Nullable final String objectId,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, Object> customFieldsJsonMap) {
    return CategorySetCustomTypeActionBuilder.of()
        .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(customTypeId))
        .fields(fieldContainerBuilder -> fieldContainerBuilder.values(customFieldsJsonMap))
        .build();
  }

  @Nonnull
  @Override
  public CategoryUpdateAction buildSetCustomFieldAction(
      @Nullable final Long variantId,
      @Nullable final String objectId,
      @Nullable final String customFieldName,
      @Nullable final Object customFieldValue) {
    return CategorySetCustomFieldActionBuilder.of()
        .name(customFieldName)
        .value(customFieldValue)
        .build();
  }
}
