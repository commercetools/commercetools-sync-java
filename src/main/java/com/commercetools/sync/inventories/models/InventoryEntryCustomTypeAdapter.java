package com.commercetools.sync.inventories.models;

import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.sync.commons.models.Custom;
import javax.annotation.Nullable;

/**
 * Adapt InventoryEntry with {@link Custom} interface to be used on {@link
 * com.commercetools.sync.commons.utils.CustomUpdateActionUtils}
 */
public final class InventoryEntryCustomTypeAdapter implements Custom {

  private final InventoryEntry inventoryEntry;

  private InventoryEntryCustomTypeAdapter(InventoryEntry inventoryEntry) {
    this.inventoryEntry = inventoryEntry;
  }

  /**
   * Get Id of the {@link InventoryEntry}
   *
   * @return the {@link InventoryEntry#getId()}
   */
  @Override
  public String getId() {
    return this.inventoryEntry.getId();
  }

  /**
   * Get typeId of the {@link InventoryEntry} see:
   * https://docs.commercetools.com/api/types#referencetype
   *
   * @return the typeId "inventoryEntry"
   */
  @Override
  public String getTypeId() {
    return "inventoryEntry";
  }

  /**
   * Get custom fields of the {@link InventoryEntry}
   *
   * @return the {@link CustomFields}
   */
  @Nullable
  @Override
  public CustomFields getCustom() {
    return this.inventoryEntry.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link
   * InventoryEntry}
   *
   * @param inventoryEntry the {@link InventoryEntry}
   * @return the {@link InventoryEntryCustomTypeAdapter}
   */
  public static InventoryEntryCustomTypeAdapter of(InventoryEntry inventoryEntry) {
    return new InventoryEntryCustomTypeAdapter(inventoryEntry);
  }
}
