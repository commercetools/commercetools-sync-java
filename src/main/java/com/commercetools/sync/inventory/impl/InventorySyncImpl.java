package com.commercetools.sync.inventory.impl;

import com.commercetools.sync.inventory.InventorySync;
import com.commercetools.sync.inventory.InventorySyncOptions;
import com.commercetools.sync.inventory.utils.InventorySyncUtils;
import com.commercetools.sync.services.InventoryService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class InventorySyncImpl implements InventorySync {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventorySyncImpl.class);
    private static final int BATCH_SIZE = 30;

    private AtomicInteger processedCounter = new AtomicInteger(0);
    private AtomicInteger createdCounter = new AtomicInteger(0);
    private AtomicInteger updatedCounter = new AtomicInteger(0);
    private AtomicInteger failedCounter = new AtomicInteger(0);

    private Map<String, String> channelKeyToChannelId;

    private InventorySyncOptions inventorySyncOptions;
    private InventoryService inventoryService;
    private TypeService typeService;

    public InventorySyncImpl(@Nonnull final InventorySyncOptions inventorySyncOptions) {
        this.inventorySyncOptions = inventorySyncOptions;
        //TODO pass services or create on my own?
    }

    @Override
    public void syncInventoryDrafts(@Nonnull List<InventoryEntryDraft> inventories) {
        buildChannelMap();
        final List<InventoryEntryDraft> accumulator = new LinkedList<>();

        for (InventoryEntryDraft entry : inventories) {
            if (entry != null && entry.getSku() != null) {
                accumulator.add(entry);
                if (accumulator.size() == BATCH_SIZE) {
                    //TODO when processed in parallel then accumulator should be handled differently (do not clear after passing)
                    processBatch(accumulator);
                    accumulator.clear();
                }
            } //TODO increment error otherwise?
        }
        if (!accumulator.isEmpty()) {
            processBatch(accumulator);
        }
    }

    private void buildChannelMap() {
        if (channelKeyToChannelId == null) {
            channelKeyToChannelId = new HashMap<>();
            inventoryService.fetchAllSupplyChannels()
                    .forEach(channel -> channelKeyToChannelId.put(channel.getKey(), channel.getId()));
        }
    }

    private void processBatch(List<InventoryEntryDraft> inventories) {
        final List<InventoryEntry> existingInventories =
                inventoryService.fetchInventoryEntriesBySkus(extractSkus(inventories));
        final Map<SkuKeyTuple, InventoryEntry> existingInventoriesMap =
                createMapOfExistingInventories(existingInventories);

        inventories.forEach(processedDraft -> {
            final SkuKeyTuple processedSkuAndKey = SkuKeyTuple.of(processedDraft);
            if (existingInventoriesMap.containsKey(processedSkuAndKey)) {
                final InventoryEntry existingEntry = existingInventoriesMap.get(processedSkuAndKey);
                final List<UpdateAction<InventoryEntry>> updateActions =
                        InventorySyncUtils.buildActions(existingEntry, processedDraft, typeService);
                if (!updateActions.isEmpty()) {
                    inventoryService.updateInventoryEntry(existingEntry, updateActions);
                    updatedCounter.incrementAndGet();
                    //TODO handle error
                }
            } else {
                final InventoryEntryDraft toCreate = replaceChannelReference(processedDraft);
                inventoryService.createInventoryEntry(toCreate);
                createdCounter.incrementAndGet();
                //TODO handle error
            }
            processedCounter.incrementAndGet();
        });
    }

    private Set<String> extractSkus(List<InventoryEntryDraft> inventories) {
        return inventories.stream()
                .map(InventoryEntryDraft::getSku)
                .filter(entry -> entry != null)
                .collect(Collectors.toSet());
    }

    private Map<SkuKeyTuple, InventoryEntry> createMapOfExistingInventories(List<InventoryEntry> existingInventories) {
        final Map<SkuKeyTuple, InventoryEntry> skuKeyTupleToInventoryEntry = new HashMap<>();
        existingInventories.forEach(entry -> skuKeyTupleToInventoryEntry.put(SkuKeyTuple.of(entry), entry));
        return skuKeyTupleToInventoryEntry;
    }

    private InventoryEntryDraft replaceChannelReference(final InventoryEntryDraft inventoryEntryDraft) {
        final String supplyChannelKey = SkuKeyTuple.of(inventoryEntryDraft).getKey();
        if (supplyChannelKey != null) {
            if (channelKeyToChannelId.containsKey(supplyChannelKey)) {
                final Reference<Channel> supplyChannelRef = Channel
                        .referenceOfId(channelKeyToChannelId.get(supplyChannelKey));
                return InventoryEntryDraft.of(inventoryEntryDraft.getSku(), inventoryEntryDraft.getQuantityOnStock(),
                        inventoryEntryDraft.getExpectedDelivery(), inventoryEntryDraft.getRestockableInDays(),
                        supplyChannelRef);
            } else {
                //TODO Create channel? Log error and skip this draft?
            }
        }
        return inventoryEntryDraft;
    }
}
