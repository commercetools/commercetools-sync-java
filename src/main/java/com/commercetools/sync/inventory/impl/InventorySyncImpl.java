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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.String.format;

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

    private ExecutorService executorService = null;
    private Map<String, String> supplyChannelKeyToId;

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
        if (options.getParallelProcessing() > 1) {
            executorService = Executors.newFixedThreadPool(options.getParallelProcessing());
        }
    }

    @Override
    public void syncInventoryDrafts(@Nonnull List<InventoryEntryDraft> inventories) {
        LOGGER.info(format("About to sync %d inventories into CTP project with key '%s'.",
                inventories.size(), options.getCtpProjectKey()));

        buildChannelMap();
        List<InventoryEntryDraft> accumulator = new LinkedList<>();
        for (InventoryEntryDraft entry : inventories) {
            if (entry != null) {
                if (entry.getSku() != null) {
                    accumulator.add(entry);
                    if (accumulator.size() == BATCH_SIZE) {
                        runDraftsProcessing(accumulator);
                        accumulator = new LinkedList<>();
                    }
                } else {
                    emptySkuCounter.incrementAndGet();
                }
            }
        }
        if (!accumulator.isEmpty()) {
            runDraftsProcessing(accumulator);
        }
        awaitDraftsProcessingFinished();
        LOGGER.info(format("Inventories sync for CTP project with key '%s' ended successfully!",
                options.getCtpProjectKey()));
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

    private void buildChannelMap() {
        if (supplyChannelKeyToId == null) {
            supplyChannelKeyToId = new ConcurrentHashMap<>();
            inventoryService.fetchAllSupplyChannels()
                    .forEach(c -> supplyChannelKeyToId.put(c.getKey(), c.getId()));
        }
    }

    private void runDraftsProcessing(final List<InventoryEntryDraft> drafts) {
        if (executorService != null) {
            executorService.submit(() -> processBatch(drafts));
        } else {
            processBatch(drafts);
        }
    }

    private void awaitDraftsProcessingFinished() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(14, TimeUnit.DAYS);
            } catch(InterruptedException ex) {
                LOGGER.error("Parallel inventories sync process was interrupted.", ex);
            }
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
        final Optional<InventoryEntryDraft> fixedDraft = replaceChannelReference(draft);
        if (fixedDraft.isPresent()) {
            final List<UpdateAction<InventoryEntry>> updateActions =
                    InventorySyncUtils.buildActions(entry, fixedDraft.get(), typeService);

            if (!updateActions.isEmpty()) {
                try {
                    inventoryService.updateInventoryEntry(entry, updateActions);
                    updatedCounter.incrementAndGet();
                } catch (Exception e) {
                    failedCounter.incrementAndGet();
                }
            }
        } else {
            failedCounter.incrementAndGet();
        }
    }

    private void attemptCreate(final InventoryEntryDraft draft) {
        final Optional<InventoryEntryDraft> fixedDraft = replaceChannelReference(draft);
        if (fixedDraft.isPresent()) {
            try {
                inventoryService.createInventoryEntry(fixedDraft.get());
                createdCounter.incrementAndGet();
            } catch (Exception e) {
                failedCounter.incrementAndGet();
            }
        } else {
            failedCounter.incrementAndGet();
        }
    }

    /**
     * Returns {@link Optional} that may contain {@link InventoryEntryDraft}. The payload of optional would be:
     * <ul>
     *     <li>Same {@code draft} instance if it has no reference to supply channel</li>
     *     <li>New {@link InventoryEntryDraft} instance if {@code draft} contains reference to supply channel.
     *     New instance would have same values as {@code draft} except for supply channel reference. Reference
     *     will be replaced with reference that points to ID of existing channel for key given in draft.</li>
     *     <li>Empty if supply channel for key wasn't found or operation of creating new supply channel fails.
     *     (depending on sync options)</li>
     * </ul>
     *
     * @param draft inventory entry draft from processed list
     * @return {@link Optional} with draft that is prepared to being created or compared with existing InventoryEntry,
     * or empty optional if errors occured.
     */
    private Optional<InventoryEntryDraft> replaceChannelReference(final InventoryEntryDraft draft) {
        final String supplyChannelKey = SkuKeyTuple.of(draft).getKey();
        if (supplyChannelKey != null) {
            if (supplyChannelKeyToId.containsKey(supplyChannelKey)) {
                return Optional.of(
                        ofEntryDraftPlusRefToSupplyChannel(draft, supplyChannelKeyToId.get(supplyChannelKey)));
            } else {
                if (options.isEnsureChannels()) {
                    return createMissingSupplyChannel(supplyChannelKey)
                            .map(id -> ofEntryDraftPlusRefToSupplyChannel(draft, id));
                } else {
                    LOGGER.error(format("Supply channel of key '%s' wasn't found in target system", supplyChannelKey));
                    return Optional.empty();
                }
            }
        }
        return Optional.of(draft);
    }

    /**
     * Returns new {@link InventoryEntryDraft} containing same data as {@code draft} except for
     * supply channel reference that is replaced by reference pointing to {@code supplyChannelId}
     * @param draft draft where reference should be replaced
     * @param supplyChannelId ID of supply channel existing in target system
     * @return {@link InventoryEntryDraft} with supply channel reference pointing to {@code supplyChannelId}
     * and other data same as in {@code draft}
     */
    private InventoryEntryDraft ofEntryDraftPlusRefToSupplyChannel(@Nonnull final InventoryEntryDraft draft,
                                                                   @Nonnull final String supplyChannelId) {
        final Reference<Channel> supplyChannelRef = Channel.referenceOfId(supplyChannelId);
        return InventoryEntryDraft.of(draft.getSku(), draft.getQuantityOnStock(), draft.getExpectedDelivery(),
                draft.getRestockableInDays(), supplyChannelRef).withCustom(draft.getCustom());
    }

    /**
     * If there is no entry for given {@code key} in map of existing supply channels key-id,
     * method tries to create supply channel of given {@code key}.
     *
     * @param key key of supply channel that seems to not exists in a system.
     * @return {@link Optional} containing ID of created/found supply channel for given key,
     * or empty {@link Optional} when operation failed.
     */
    private synchronized Optional<String> createMissingSupplyChannel(@Nonnull final String key) {
        if (supplyChannelKeyToId.containsKey(key)) {
            return Optional.of(supplyChannelKeyToId.get(key));
        } else {
            try {
                final Channel newChannel = inventoryService.createSupplyChannel(key);
                if (newChannel != null) {
                    supplyChannelKeyToId.put(newChannel.getKey(), newChannel.getId());
                    return Optional.of(newChannel.getId());
                }
            } catch (Exception ex) {
                LOGGER.error(format("Failed to create new supply channel of key '%s'", key), ex);
            }
            return Optional.empty();
        }
    }
}
