package com.commercetools.sync.inventories.helpers;

import com.commercetools.api.models.inventory.InventoryEntrySetCustomFieldActionBuilder;
import com.commercetools.api.models.inventory.InventoryEntrySetCustomTypeActionBuilder;
import com.commercetools.api.models.inventory.InventoryEntryUpdateAction;
import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class InventoryCustomActionBuilder
    implements GenericCustomActionBuilder<InventoryEntryUpdateAction> {

  @Nonnull
  @Override
  public InventoryEntryUpdateAction buildRemoveCustomTypeAction(
      @Nullable final Long variantId, @Nullable final String objectId) {
    return InventoryEntrySetCustomTypeActionBuilder.of().build();
  }

  @Nonnull
  @Override
  public InventoryEntryUpdateAction buildSetCustomTypeAction(
      @Nullable final Long variantId,
      @Nullable final String objectId,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, Object> customFieldsJsonMap) {
    return InventoryEntrySetCustomTypeActionBuilder.of()
        .type(typeResourceIdBuilder -> typeResourceIdBuilder.id(customTypeId))
        .fields(fieldContainerBuilder -> fieldContainerBuilder.values(customFieldsJsonMap))
        .build();
  }

  @Nonnull
  @Override
  public InventoryEntryUpdateAction buildSetCustomFieldAction(
      @Nullable final Long variantId,
      @Nullable final String objectId,
      @Nullable final String customFieldName,
      @Nullable final Object customFieldValue) {
    return InventoryEntrySetCustomFieldActionBuilder.of()
        .name(customFieldName)
        .value(customFieldValue)
        .build();
  }
}
