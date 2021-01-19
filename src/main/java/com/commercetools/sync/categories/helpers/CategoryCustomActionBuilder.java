package com.commercetools.sync.categories.helpers;

import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.SetCustomField;
import io.sphere.sdk.categories.commands.updateactions.SetCustomType;
import io.sphere.sdk.commands.UpdateAction;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CategoryCustomActionBuilder implements GenericCustomActionBuilder<Category> {
  @Nonnull
  @Override
  public UpdateAction<Category> buildRemoveCustomTypeAction(
      @Nullable final Integer variantId, @Nullable final String objectId) {
    return SetCustomType.ofRemoveType();
  }

  @Nonnull
  @Override
  public UpdateAction<Category> buildSetCustomTypeAction(
      @Nullable final Integer variantId,
      @Nullable final String objectId,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, JsonNode> customFieldsJsonMap) {
    return SetCustomType.ofTypeIdAndJson(customTypeId, customFieldsJsonMap);
  }

  @Nonnull
  @Override
  public UpdateAction<Category> buildSetCustomFieldAction(
      @Nullable final Integer variantId,
      @Nullable final String objectId,
      @Nullable final String customFieldName,
      @Nullable final JsonNode customFieldValue) {
    return SetCustomField.ofJson(customFieldName, customFieldValue);
  }
}
