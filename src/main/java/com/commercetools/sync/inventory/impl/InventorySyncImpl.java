package com.commercetools.sync.inventory.impl;

import com.commercetools.sync.inventory.InventorySync;
import com.commercetools.sync.inventory.InventorySyncOptions;
import com.commercetools.sync.inventory.utils.InventorySyncUtils;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
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

import static java.util.stream.Collectors.toMap;

//TODO test
//TODO document
public final class InventorySyncImpl implements InventorySync {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventorySyncImpl.class);
    private static final int BATCH_SIZE = 30;

    private AtomicInteger processedCounter = new AtomicInteger(0);
    private AtomicInteger createdCounter = new AtomicInteger(0);
    private AtomicInteger updatedCounter = new AtomicInteger(0);
    private AtomicInteger failedCounter = new AtomicInteger(0);
    private AtomicInteger emptySkuCounter = new AtomicInteger(0);

    private Map<String, String> channelKeyToChannelId;

    private InventorySyncOptions options;
    private InventoryService inventoryService;
    private TypeService typeService;

    public InventorySyncImpl(@Nonnull final InventorySyncOptions options) {
        this(options, new InventoryServiceImpl(options.getCTPclient()), new TypeServiceImpl(options.getCTPclient()));
    }

    InventorySyncImpl(final InventorySyncOptions options, final InventoryService inventoryService,
                      final TypeService typeService) {
        this.options = options;
        this.inventoryService = inventoryService;
        this.typeService = typeService;
    }

    @Override
    public void syncInventoryDrafts(@Nonnull List<InventoryEntryDraft> inventories) {
        buildChannelMap();
        final List<InventoryEntryDraft> accumulator = new LinkedList<>();

        //TODO parallelise process (GITHUB ISSUE #16)
        for (InventoryEntryDraft entry : inventories) {
            if (entry != null) {
                if (entry.getSku() != null) {
                    accumulator.add(entry);
                    if (accumulator.size() == BATCH_SIZE) {
                        //TODO when processed in parallel then accumulator should be handled differently (do not clear after passing)
                        processBatch(accumulator);
                        accumulator.clear();
                    }
                } else {
                    emptySkuCounter.incrementAndGet();
                }
            }
        }
        if (!accumulator.isEmpty()) {
            processBatch(accumulator);
        }
    }

    @Override
    public void syncInventory(@Nonnull List<InventoryEntry> inventories) {
        //TODO implement (GITHUB ISSUE #17)
    }

    @Override
    public String getSummary() {
        //TODO implement (GITHUB ISSUE #17)
        return null;
    }

    private synchronized void buildChannelMap() {
        if (channelKeyToChannelId == null) {
            channelKeyToChannelId = inventoryService.fetchAllSupplyChannels()
                    .stream()
                    .collect(toMap(Channel::getKey, Channel::getId));
        }
    }

    private void processBatch(final List<InventoryEntryDraft> drafts) {
        final Map<SkuKeyTuple, InventoryEntry> existingInventories = fetchExistingInventories(drafts);

        drafts.forEach(draft -> {
            final SkuKeyTuple skuKeyOfDraft = SkuKeyTuple.of(draft);
            if (existingInventories.containsKey(skuKeyOfDraft)) {
                final InventoryEntry existingEntry = existingInventories.get(skuKeyOfDraft);
                attemptUpdate(existingEntry, draft);
            } else {
                attemptCreate(draft);
            }
            processedCounter.incrementAndGet();
        });
    }

    private Map<SkuKeyTuple, InventoryEntry> fetchExistingInventories(final List<InventoryEntryDraft> drafts) {
        final Set<String> skus = extractSkus(drafts);
        final List<InventoryEntry> existingEntries = inventoryService.fetchInventoryEntriesBySkus(skus);
        return mapSkuAndKeyToInventoryEntry(existingEntries);
    }

    private Set<String> extractSkus(final List<InventoryEntryDraft> inventories) {
        return inventories.stream()
                .map(InventoryEntryDraft::getSku)
                .collect(Collectors.toSet());
    }

    private Map<SkuKeyTuple, InventoryEntry> mapSkuAndKeyToInventoryEntry(final List<InventoryEntry> inventories) {
        final Map<SkuKeyTuple, InventoryEntry> skuKeyToInventory = new HashMap<>();
        inventories.forEach(entry -> skuKeyToInventory.put(SkuKeyTuple.of(entry), entry));
        return skuKeyToInventory;
    }

    private void attemptUpdate(final InventoryEntry entry, final InventoryEntryDraft draft) {
        final InventoryEntryDraft fixedDraft = replaceChannelReference(draft);
        final List<UpdateAction<InventoryEntry>> updateActions =
                InventorySyncUtils.buildActions(entry, fixedDraft, typeService);

        if (!updateActions.isEmpty()) {
            try {
                inventoryService.updateInventoryEntry(entry, updateActions);
                updatedCounter.incrementAndGet();
            } catch (Exception e) {
                failedCounter.incrementAndGet();
            }
        }
    }

    private void attemptCreate(final InventoryEntryDraft draft) {
        final InventoryEntryDraft fixedDraft = replaceChannelReference(draft);
        try {
            inventoryService.createInventoryEntry(fixedDraft);
            createdCounter.incrementAndGet();
        } catch(Exception e) {
            failedCounter.incrementAndGet();
        }
    }

    private InventoryEntryDraft replaceChannelReference(final InventoryEntryDraft inventoryEntryDraft) {
        final String supplyChannelKey = SkuKeyTuple.of(inventoryEntryDraft).getKey();
        if (supplyChannelKey != null) {
            if (channelKeyToChannelId.containsKey(supplyChannelKey)) {
                final Reference<Channel> supplyChannelRef = Channel
                        .referenceOfId(channelKeyToChannelId.get(supplyChannelKey));
                return InventoryEntryDraft.of(inventoryEntryDraft.getSku(), inventoryEntryDraft.getQuantityOnStock(),
                        inventoryEntryDraft.getExpectedDelivery(), inventoryEntryDraft.getRestockableInDays(),
                        supplyChannelRef).withCustom(inventoryEntryDraft.getCustom());
            } else {
                //TODO Handle (GITHUB ISSUE #18)
            }
        }
        return inventoryEntryDraft;
    }
}
