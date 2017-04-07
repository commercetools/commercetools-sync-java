package com.commercetools.sync.services.impl;

import com.commercetools.sync.services.InventoryService;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class InventoryServiceImpl implements InventoryService {

    private final BlockingSphereClient ctpClient;

    public InventoryServiceImpl(BlockingSphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Override
    public List<InventoryEntry> fetchInventoryEntriesBySkus(Set<String> skus) {
        //TODO implement proper query. Be aware of pagination.
        return Collections.emptyList();
    }

    @Override
    public InventoryEntry createInventoryEntry(@Nonnull InventoryEntryDraft inventoryEntryDraft) {
        //TODO implement
        return null;
    }

    @Override
    public InventoryEntry updateInventoryEntry(@Nonnull InventoryEntry inventoryEntry, @Nonnull List<UpdateAction<InventoryEntry>> updateActions) {
        //TODO implement
        return null;
    }
}
