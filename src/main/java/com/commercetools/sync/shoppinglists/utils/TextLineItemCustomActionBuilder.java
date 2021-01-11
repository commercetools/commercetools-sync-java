package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetTextLineItemCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetTextLineItemCustomType;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TextLineItemCustomActionBuilder
    implements GenericCustomActionBuilder<ShoppingList> {

  @Nonnull
  @Override
  public UpdateAction<ShoppingList> buildRemoveCustomTypeAction(
      @Nullable final Integer variantId, @Nullable final String textLineItemId) {

    return SetTextLineItemCustomType.ofRemoveType(textLineItemId);
  }

  @Nonnull
  @Override
  public UpdateAction<ShoppingList> buildSetCustomTypeAction(
      @Nullable final Integer variantId,
      @Nullable final String textLineItemId,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, JsonNode> customFieldsJsonMap) {

    return SetTextLineItemCustomType.ofTypeIdAndJson(
        customTypeId, customFieldsJsonMap, textLineItemId);
  }

  @Nonnull
  @Override
  public UpdateAction<ShoppingList> buildSetCustomFieldAction(
      @Nullable final Integer variantId,
      @Nullable final String textLineItemId,
      @Nullable final String customFieldName,
      @Nullable final JsonNode customFieldValue) {

    return SetTextLineItemCustomField.ofJson(customFieldName, customFieldValue, textLineItemId);
  }
}
