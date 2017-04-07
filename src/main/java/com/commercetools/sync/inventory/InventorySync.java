package com.commercetools.sync.inventory;

import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import java.util.List;

public interface InventorySync {

    void syncInventoryDrafts(@Nonnull final List<InventoryEntryDraft> inventories);
}
