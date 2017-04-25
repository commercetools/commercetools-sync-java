package com.commercetools.sync.inventory.impl;

import com.commercetools.sync.inventory.InventorySync;
import com.commercetools.sync.inventory.helpers.InventorySyncOptions;
import com.commercetools.sync.inventory.helpers.InventorySyncStatistics;
import com.commercetools.sync.inventory.utils.InventorySyncUtils;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.models.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.commercetools.sync.inventory.utils.InventoryDraftTransformer.transformToDrafts;
import static java.lang.String.format;

//TODO test
/**
 * Default implementation of inventories sync process.
 */
public final class InventorySyncImpl implements InventorySync {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventorySyncImpl.class);

    /*
     * Holds threads for executing sync process in parallel. Should be instantiated when parallel processing is
     * enabled via {@link InventorySyncOptions}.
     */
    private ExecutorService executorService = null;

    //Cache that maps supply channel key to supply channel Id for supply channels existing in database.
    private Map<String, String> supplyChannelKeyToId;

    private InventoryService inventoryService;
    //TODO assert that counters from BaseStatistics became thread safe
    private InventorySyncStatistics statistics;
    private InventorySyncOptions options;

    public InventorySyncImpl(@Nonnull final InventorySyncOptions options) {
        this(options, new InventoryServiceImpl(options.getCtpClient()));
    }

    InventorySyncImpl(final InventorySyncOptions options, final InventoryService inventoryService) {
        this.options = options;
        this.inventoryService = inventoryService;
        this.statistics = new InventorySyncStatistics();
        if (options.getParallelProcessing() > 1) {
            executorService = Executors.newFixedThreadPool(options.getParallelProcessing());
        }
    }

    /**
     * <p>Performs full process of synchronisation between inventory entries present in a system
     * and passed {@code inventories}. This is accomplished by:
     * <ul>
     *     <li>Comparing entries and drafts by {@code sku} and {@code supplyChannel} key</li>
     *     <li>Calculating of necessary updates and creation commands</li>
     *     <li>Actually <strong>performing</strong> changes in a target CTP project by sphere calls</li>
     * </ul>
     * The process is customized according to {@link InventorySyncOptions} passed to constructor of this object.
     * After the process finishes you can obtain its summary by calling {@link InventorySyncImpl#getStatistics()}.</p>
     * <p><strong>This method is meant to be called once. For new sync process please create new {@link InventorySyncImpl}
     * instance!</strong></p>
     *
     * @param inventories {@link List} of {@link InventoryEntryDraft} containing data that would be synced into
     *                                CTP project.
     */
    @Override
    public void syncInventoryDrafts(@Nonnull List<InventoryEntryDraft> inventories) {
        LOGGER.info(format("About to sync %d inventories into CTP project with key '%s'.",
                inventories.size(), options.getClientConfig().getProjectKey()));
        //TODO assert if time management in base statistics doesn't require to start timer here

        buildChannelMap();
        List<InventoryEntryDraft> accumulator = new LinkedList<>();
        for (InventoryEntryDraft entry : inventories) {
            if (entry != null) {
                if (entry.getSku() != null) {
                    accumulator.add(entry);
                    if (accumulator.size() == options.getBatchSize()) {
                        runDraftsProcessing(accumulator);
                        accumulator = new LinkedList<>();
                    }
                } else {
                    statistics.incrementUnprocessedDueToEmptySku();
                }
            }
        }
        if (!accumulator.isEmpty()) {
            runDraftsProcessing(accumulator);
        }
        awaitDraftsProcessingFinished();
        statistics.calculateProcessingTime();
        LOGGER.info(format("Inventories sync for CTP project with key '%s' ended successfully!",
                options.getClientConfig().getProjectKey()));
    }

    /**
     * Converts {@code inventories} to {@link InventoryEntryDraft} objects and perform full synchronisation process
     * as described in {@link InventorySyncImpl#syncInventoryDrafts(List)}.
     *
     * @param inventories  {@link List} of {@link InventoryEntry} that you would like to sync into your CTP project.
     * @see InventorySyncImpl#syncInventoryDrafts(List)
     */
    @Override
    public void syncInventory(@Nonnull List<InventoryEntry> inventories) {
        LOGGER.info(format("Converting inventory entries to InventoryEntryDraft objects."));
        final List<InventoryEntryDraft> drafts = transformToDrafts(inventories);
        syncInventoryDrafts(drafts);
    }

    @Override
    @Nonnull
    public InventorySyncStatistics getStatistics() {
        return statistics;
    }

    /**
     * Instantiate and fill {@code supplyChannelKeyToId} with values fetched from API.
     */
    private void buildChannelMap() {
        if (options.getParallelProcessing() > 1) {
            supplyChannelKeyToId = new ConcurrentHashMap<>();
        } else {
            supplyChannelKeyToId = new HashMap<>();
        }
        inventoryService.fetchAllSupplyChannels()
                .forEach(c -> supplyChannelKeyToId.put(c.getKey(), c.getId()));
    }

    /**
     * Process batch in current thread in case of sequential processing or submit batch
     * processing to {@code executorService} in case of parallel execution.
     *
     * @param drafts batch of {@link InventoryEntryDraft}
     */
    private void runDraftsProcessing(final List<InventoryEntryDraft> drafts) {
        if (executorService != null) {
            executorService.submit(() -> processBatch(drafts));
        } else {
            processBatch(drafts);
        }
    }

    /**
     * When processed in parallel it awaits for all tasks from {@code executorService} to end.
     */
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

    /**
     * Method process batch of {@link InventoryEntryDraft}. The processing means:
     * <ol>
     *     <li>Fetching existing {@link InventoryEntry} that correspond to passed {@code drafts}</li>
     *     <li>Deciding if each draft should be used for creating new entry or updating existing one</li>
     *     <li>Attempting to create new entries or update existing ones</li>
     *     <li>Updates the {@code statistics} object backed for this instance</li>
     * </ol>
     * @param drafts batch of drafts that need to be synced
     */
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
            statistics.incrementProcessed();
        });
    }

    /**
     * Returns mapping of {@link SkuKeyTuple} to {@link InventoryEntry} of instances existing in a database.
     * Instances are fetched from API by skus, that corresponds to skus present in {@code drafts}.
     *
     * @param drafts {@link List} of drafts
     * @return {@link SkuKeyTuple} to {@link InventoryEntry} of existing entries that correspond to passed
     * {@code drafts} by sku comparision.
     */
    private Map<SkuKeyTuple, InventoryEntry> fetchExistingInventories(final List<InventoryEntryDraft> drafts) {
        final Set<String> skus = extractSkus(drafts);
        final List<InventoryEntry> existingEntries = inventoryService.fetchInventoryEntriesBySkus(skus);
        return mapSkuAndKeyToInventoryEntry(existingEntries);
    }

    /**
     *
     * @param inventories {@link List} of {@link InventoryEntryDraft} where each draft contains its sku
     * @return {@link Set} of distinct skus found in {@code inventories}
     */
    private Set<String> extractSkus(final List<InventoryEntryDraft> inventories) {
        return inventories.stream()
                .map(InventoryEntryDraft::getSku)
                .collect(Collectors.toSet());
    }

    /**
     * Creates map of {@link SkuKeyTuple} to {@link InventoryEntry}, so that all distinct tuples of sku and supply
     * channel key from {@code inventories} are mapped to entry instance. For use in comparision with
     * {@link InventoryEntryDraft} i.e. create mapping from existing entries and then decide if specific draft
     * should be used for creation of new entry or for building update actions for existing entry (by checking if
     * sku-key tuple of draft is present in returned mapping)
     *
     * @param inventories list of {@link InventoryEntry}
     * @return {@link Map} of {@link SkuKeyTuple} to {@link InventoryEntry}
     */
    private Map<SkuKeyTuple, InventoryEntry> mapSkuAndKeyToInventoryEntry(final List<InventoryEntry> inventories) {
        final Map<SkuKeyTuple, InventoryEntry> skuKeyToInventory = new HashMap<>();
        inventories.forEach(entry -> skuKeyToInventory.put(SkuKeyTuple.of(entry), entry));
        return skuKeyToInventory;
    }

    /**
     * Tries to update {@code entry} in database with data from {@code draft}.
     * It calculates list of {@link UpdateAction} and calls API only when there is a need.
     * Before calculate differences, the channel reference from {@code draft} is replaced, so it points to
     * proper channel ID in target system.
     * It either updates inventory entry and increments "updated" counter in statistics, or increments "failed" counter
     * and log error in case of any exception.
     *
     * @param entry entry from existing system that could be updated.
     * @param draft draft containing data that could differ from data in {@code entry}.
     *              <strong>Sku isn't compared</strong>
     */
    private void attemptUpdate(final InventoryEntry entry, final InventoryEntryDraft draft) {
        final Optional<InventoryEntryDraft> fixedDraft = replaceChannelReference(draft);
        if (fixedDraft.isPresent()) {
            final List<UpdateAction<InventoryEntry>> updateActions =
                    InventorySyncUtils.buildActions(entry, fixedDraft.get(), options);

            if (!updateActions.isEmpty()) {
                try {
                    inventoryService.updateInventoryEntry(entry, updateActions);
                    statistics.incrementUpdated();
                } catch (Exception e) {
                    LOGGER.error(format("Failed to update inventory entry ['%s'] with data from draft ['%s'].",
                            entry.toString(), fixedDraft.get().toString()), e);
                    statistics.incrementFailed();
                }
            }
        } else {
            statistics.incrementFailed();
        }
    }

    /**
     * Tries to create Inventory Entry in database, using {@code draft}.
     * Before calls API, the channel reference from {@code draft} is replaced, so it points to proper channel ID
     * in target system.
     * It either creates inventory entry and increments "created" counter in statistics, or increments "failed" counter
     * and log error in case of any exception.
     *
     * @param draft draft of new inventory entry.
     */
    private void attemptCreate(final InventoryEntryDraft draft) {
        final Optional<InventoryEntryDraft> fixedDraft = replaceChannelReference(draft);
        if (fixedDraft.isPresent()) {
            try {
                inventoryService.createInventoryEntry(fixedDraft.get());
                statistics.incrementCreated();
            } catch (Exception e) {
                LOGGER.error(format("Failed to create inventory entry from draft ['%s'].",
                        fixedDraft.get().toString()), e);
                statistics.incrementFailed();
            }
        } else {
            statistics.incrementFailed();
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
        return InventoryEntryDraftBuilder.of(draft)
                .supplyChannel(supplyChannelRef)
                .build();
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
