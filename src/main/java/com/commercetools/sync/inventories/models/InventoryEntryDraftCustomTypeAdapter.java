package com.commercetools.sync.inventories.models;

import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.sync.commons.models.CustomDraft;
import javax.annotation.Nullable;

/**
 * Adapt InventoryEntryDraft with {@link CustomDraft} interface to be used on {@link
 * com.commercetools.sync.commons.utils.CustomUpdateActionUtils}
 */
public final class InventoryEntryDraftCustomTypeAdapter implements CustomDraft {

  private final InventoryEntryDraft inventoryEntryDraft;

  private InventoryEntryDraftCustomTypeAdapter(InventoryEntryDraft inventoryEntryDraft) {
    this.inventoryEntryDraft = inventoryEntryDraft;
  }

  /**
   * Get custom fields of the {@link InventoryEntryDraft}
   *
   * @return the {@link CustomFieldsDraft}
   */
  @Nullable
  @Override
  public CustomFieldsDraft getCustom() {
    return this.inventoryEntryDraft.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link
   * InventoryEntryDraft}
   *
   * @param inventoryEntryDraft the {@link InventoryEntryDraft}
   * @return the {@link InventoryEntryDraftCustomTypeAdapter}
   */
  public static InventoryEntryDraftCustomTypeAdapter of(InventoryEntryDraft inventoryEntryDraft) {
    return new InventoryEntryDraftCustomTypeAdapter(inventoryEntryDraft);
  }
}
