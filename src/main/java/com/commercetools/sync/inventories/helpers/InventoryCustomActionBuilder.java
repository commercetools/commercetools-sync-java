package com.commercetools.sync.inventories.helpers;

import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.commands.updateactions.SetCustomField;
import io.sphere.sdk.inventory.commands.updateactions.SetCustomType;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class InventoryCustomActionBuilder implements GenericCustomActionBuilder<InventoryEntry> {

  @Nonnull
  @Override
  public UpdateAction<InventoryEntry> buildRemoveCustomTypeAction(
      @Nullable final Integer variantId, @Nullable final String objectId) {
    return SetCustomType.ofRemoveType();
  }

  @Nonnull
  @Override
  public UpdateAction<InventoryEntry> buildSetCustomTypeAction(
      @Nullable final Integer variantId,
      @Nullable final String objectId,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, JsonNode> customFieldsJsonMap) {
    return SetCustomType.ofTypeIdAndJson(customTypeId, customFieldsJsonMap);
  }

  @Nonnull
  @Override
  public UpdateAction<InventoryEntry> buildSetCustomFieldAction(
      @Nullable final Integer variantId,
      @Nullable final String objectId,
      @Nullable final String customFieldName,
      @Nullable final JsonNode customFieldValue) {
    return SetCustomField.ofJson(customFieldName, customFieldValue);
  }
}
