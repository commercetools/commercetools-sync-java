package com.commercetools.sync.inventory.impl;

import com.commercetools.sync.inventory.InventorySync;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InventorySyncImpl implements InventorySync {

    private static final int BATCH_SIZE = 30;

    @Override
    public void syncInventoryDrafts(@Nonnull List<InventoryEntryDraft> inventories) {
        final List<InventoryEntryDraft> accumulator = new LinkedList<>();

        for (InventoryEntryDraft entry : inventories) {
            if (entry != null) {
                accumulator.add(entry);
                if (accumulator.size() == BATCH_SIZE) {
                    processBatch(accumulator);
                    accumulator.clear();
                }
            }
        }
        if (!accumulator.isEmpty()) {
            processBatch(accumulator);
        }
    }

    private void processBatch(List<InventoryEntryDraft> inventories) {
        final Set<String> skus = extractSkus(inventories);
        //TODO fetch exising from service
        //TODO call for InventoryEntries of skus
        //TODO put result to Map<String, Map<String, InventoryEntry>> for easy access of responding values (sku, key)?
        //TODO foreach inventoryEntry find responding, build updateActions, do update
    }

    private Set<String> extractSkus(List<InventoryEntryDraft> inventories) {
        return inventories.stream()
                .map(InventoryEntryDraft::getSku)
                .filter(entry -> entry != null)
                .collect(Collectors.toSet());
    }
}
