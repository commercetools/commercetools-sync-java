package com.commercetools.sync.inventory;

import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import java.util.List;

//TODO document
public interface InventorySync {

    void syncInventoryDrafts(@Nonnull final List<InventoryEntryDraft> inventories);

    void syncInventory(@Nonnull final List<InventoryEntry> inventories);

    String getSummary();
}
