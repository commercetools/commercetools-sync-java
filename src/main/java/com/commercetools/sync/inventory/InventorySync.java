package com.commercetools.sync.inventory;

import com.commercetools.sync.inventory.helpers.InventorySyncStatistics;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import java.util.List;

//TODO probably delete and use Sync from commons when released to "develop"
public interface InventorySync {

    void syncInventoryDrafts(@Nonnull final List<InventoryEntryDraft> inventories);

    void syncInventory(@Nonnull final List<InventoryEntry> inventories);

    InventorySyncStatistics getStatistics();
}
