package com.commercetools.sync.inventories;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.inventories.helpers.InventoryReferenceResolver;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import com.commercetools.sync.inventories.utils.InventorySyncUtils;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.InventoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.InventoryServiceImpl;
import com.commercetools.sync.services.impl.ChannelServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Default implementation of inventories sync process.
 */
public final class InventorySync extends BaseSync<InventoryEntryDraft, InventorySyncStatistics, InventorySyncOptions> {
    private static final String CTP_INVENTORY_FETCH_FAILED = "Failed to fetch existing inventory entries of SKUs %s.";
    private static final String CTP_INVENTORY_ENTRY_UPDATE_FAILED = "Failed to update inventory entry of sku '%s' and "
        + "supply channel key '%s'.";
    private static final String INVENTORY_DRAFT_HAS_NO_SKU = "Failed to process inventory entry without sku.";
    private static final String INVENTORY_DRAFT_IS_NULL = "Failed to process null inventory draft.";
    private static final String CTP_INVENTORY_ENTRY_CREATE_FAILED = "Failed to create inventory entry of sku '%s' "
        + "and supply channel key '%s'.";
    private static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type reference on "
        + "InventoryEntryDraft with sku:'%s'. Reason: %s";
    private static final String FAILED_TO_RESOLVE_SUPPLY_CHANNEL = "Failed to resolve supply channel reference on "
        + "InventoryEntryDraft with sku:'%s'. Reason: %s";


    private final InventoryService inventoryService;

    private final InventoryReferenceResolver referenceResolver;

    /**
     * Takes a {@link InventorySyncOptions} instance to instantiate a new {@link InventorySync} instance that could be
     * used to sync inventory drafts with the given inventory entries in the CTP project specified in the injected
     * {@link InventorySyncOptions} instance.
     *
     * @param syncOptions the container of all the options of the sync process including the CTP project client and/or
     *                    configuration and other sync-specific options.
     */
    public InventorySync(@Nonnull final InventorySyncOptions syncOptions) {
        this(syncOptions, new InventoryServiceImpl(syncOptions.getCtpClient()),
            new ChannelServiceImpl(syncOptions.getCtpClient(), Collections.singleton(ChannelRole.INVENTORY_SUPPLY)),
            new TypeServiceImpl(syncOptions.getCtpClient()));
    }

    InventorySync(@Nonnull final InventorySyncOptions syncOptions, @Nonnull final InventoryService inventoryService,
                  @Nonnull final ChannelService channelService, @Nonnull final TypeService typeService) {
        super(new InventorySyncStatistics(), syncOptions);
        this.inventoryService = inventoryService;
        this.referenceResolver = new InventoryReferenceResolver(syncOptions, typeService, channelService);
    }

    /**
     * Iterates through the whole {@code inventories} list and accumulates its valid drafts to batches. Every batch
     * is then processed by {@link InventorySync#processBatch(List)}. For invalid drafts from {@code inventories}
     * "processed" and "failed" counters from statistics are incremented and error callback is executed. A valid draft
     * is a {@link InventoryEntryDraft} object that is not {@code null} and its SKU is not empty.
     *
     * <p><strong>Inherited doc:</strong>
     * {@inheritDoc}
     *
     * @param inventories {@link List} of {@link InventoryEntryDraft} resources that would be synced into CTP project.
     * @return {@link CompletionStage} with {@link InventorySyncStatistics} holding statistics of all sync
     *                                           processes performed by this sync instance
     */
    @Nonnull
    protected CompletionStage<InventorySyncStatistics> process(@Nonnull final List<InventoryEntryDraft>
                                                                                      inventories) {
        List<InventoryEntryDraft> accumulator = new ArrayList<>(syncOptions.getBatchSize());
        final List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        for (InventoryEntryDraft entry : inventories) {
            if (entry != null) {
                if (isNotEmpty(entry.getSku())) {
                    accumulator.add(entry);
                    if (accumulator.size() == syncOptions.getBatchSize()) {
                        completableFutures.add(processBatch(accumulator).toCompletableFuture());
                        accumulator = new ArrayList<>(syncOptions.getBatchSize());
                    }
                } else {
                    handleError(INVENTORY_DRAFT_HAS_NO_SKU, null, 1);
                }
            } else {
                handleError(INVENTORY_DRAFT_IS_NULL, null, 1);
            }
        }
        if (!accumulator.isEmpty()) {
            completableFutures.add(processBatch(accumulator).toCompletableFuture());
        }
        return CompletableFuture
            .allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]))
            .thenApply(result -> {
                statistics.incrementProcessed(inventories.size());
                return statistics;
            });
    }

    /**
     * Fetches existing {@link InventoryEntry} objects from CTP project that correspond to passed {@code batchOfDrafts}.
     * Having existing inventory entries fetched, {@code batchOfDrafts} is compared and synced with fetched objects by
     * {@link InventorySync#syncBatch(Map, List)} function. When fetching existing inventory entries results in
     * exception then error callback is executed and {@code batchOfDrafts} isn't processed.

     * @param batchOfDrafts batch of drafts that need to be synced
     * @return {@link CompletionStage} of {@link Void} that indicates method progress.
     */
    private CompletionStage<Void> processBatch(final List<InventoryEntryDraft> batchOfDrafts) {
        return fetchExistingInventories(batchOfDrafts)
                .thenCompose(existingInventories -> syncBatch(existingInventories, batchOfDrafts))
                .exceptionally(exception -> {
                    final String errorMessage = format(CTP_INVENTORY_FETCH_FAILED, extractSkus(batchOfDrafts));
                    handleError(errorMessage, exception, batchOfDrafts.size());
                    return null;
                });
    }

    /**
     * Given a list of inventory entry drafts, this method extracts a list of all distinct SKUs from the drafts, then
     * it tries to fetch a list of Inventory entries with those SKUs, then it returns a future which contains a
     * mapping of {@link SkuChannelKeyTuple} to {@link InventoryEntry} of instances existing in a CTP project or an
     * exception.
     *
     * @param drafts {@link List} of inventory entry drafts to create mapping for.
     * @return {@link CompletionStage} instance which contains either {@link Map} of {@link SkuChannelKeyTuple} to
     *      {@link InventoryEntry} of instances existing in a CTP project that correspond to passed {@code drafts} by
     *      sku comparision, or exception occurred during fetching existing inventory entries
     */
    private CompletionStage<Map<SkuChannelKeyTuple, InventoryEntry>>
        fetchExistingInventories(@Nonnull final List<InventoryEntryDraft> drafts) {
        final Set<String> skus = extractSkus(drafts);
        return inventoryService.fetchInventoryEntriesBySkus(skus)
                               .thenApply(
                                   existingEntries -> existingEntries.stream().collect(toMap(SkuChannelKeyTuple::of,
                                       entry -> entry)));
    }

    /**
     * Given a list of inventory entry {@code drafts}, this method resolves the references of each entry and attempts to
     * sync it to the CTP project depending whether it exists or not in the {@code existingInventories}.
     *
     * @param existingInventories  mapping of {@link SkuChannelKeyTuple} to {@link InventoryEntry} of instances existing
     *                             in a CTP project
     * @param inventoryEntryDrafts drafts that need to be synced
     * @return a future which contains an empty result after execution of the update.
     */
    private CompletionStage<Void> syncBatch(final Map<SkuChannelKeyTuple, InventoryEntry> existingInventories,
                                            final List<InventoryEntryDraft> inventoryEntryDrafts) {
        final List<CompletableFuture<Void>> futures = new ArrayList<>(inventoryEntryDrafts.size());
        inventoryEntryDrafts.forEach(inventoryEntryDraft ->
            futures.add(resolveReferencesAndSync(existingInventories, inventoryEntryDraft)));
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    }

    /**
     * Given an inventory entry {@code draft}, this method first resolves all references on the draft, then checks if
     * there is corresponding entry in {@code existingInventories}. If there is, then it attempts to update such entry
     * with data from draft, otherwise it attempts to create new entry from draft.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP, the optional error callback
     * specified in the {@code syncOptions} is called.
     *
     * @param existingInventories mapping of {@link SkuChannelKeyTuple} to {@link InventoryEntry} of instances existing
     *                            in a CTP project
     * @param inventoryEntryDraft draft that need to be synced
     * @return a future which contains an empty result after execution of the update.
     */
    private CompletableFuture<Void> resolveReferencesAndSync(@Nonnull final Map<SkuChannelKeyTuple, InventoryEntry>
                                                                 existingInventories,
                                                             @Nonnull final InventoryEntryDraft inventoryEntryDraft) {
        return referenceResolver.resolveCustomTypeReference(inventoryEntryDraft)
                                .thenCompose(draftWithResolvedCustomTypeReference -> referenceResolver
                                        .resolveSupplyChannelReference(draftWithResolvedCustomTypeReference)
                                        .thenCompose(resolvedDraft -> {
                                            final SkuChannelKeyTuple skuKeyOfDraft =
                                                SkuChannelKeyTuple.of(resolvedDraft);
                                            if (existingInventories.containsKey(skuKeyOfDraft)) {
                                                final InventoryEntry existingEntry =
                                                    existingInventories.get(skuKeyOfDraft);
                                                return buildUpdateActionsAndUpdate(existingEntry, resolvedDraft);
                                            } else {
                                                return attemptCreate(inventoryEntryDraft);
                                            }
                                        })
                                        .exceptionally(exception -> {
                                            final String errorMessage = format(FAILED_TO_RESOLVE_SUPPLY_CHANNEL,
                                                inventoryEntryDraft.getSku(), exception.getMessage());
                                            handleError(errorMessage, exception, 1);
                                            return null;
                                        }))
                                .exceptionally(exception -> {
                                    final String errorMessage = format(FAILED_TO_RESOLVE_CUSTOM_TYPE,
                                        inventoryEntryDraft.getSku(), exception.getMessage());
                                    handleError(errorMessage, exception, 1);
                                    return null;
                                })
                                .toCompletableFuture();
    }

    /**
     * Returns a distinct set of SKUs from the supplied list of inventory entry drafts.
     *
     * @param inventories {@link List} of {@link InventoryEntryDraft} where each draft contains its sku
     * @return {@link Set} of distinct SKUs found in {@code inventories}.
     */
    private Set<String> extractSkus(@Nonnull final List<InventoryEntryDraft> inventories) {
        return inventories.stream()
                .map(InventoryEntryDraft::getSku)
                .collect(Collectors.toSet());
    }

    /**
     * Given an existing {@link InventoryEntry} and a new {@link InventoryEntryDraft}, the method calculates all the
     * update actions required to synchronize the existing entry to be the same as the new one. If there are update
     * actions found, a request is made to CTP to update the existing entry, otherwise it doesn't issue a request.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP, the optional error callback
     * specified in the {@code syncOptions} is called.
     *
     * @param entry existing inventory entry that could be updated.
     * @param draft draft containing data that could differ from data in {@code entry}.
     *              <strong>Sku isn't compared</strong>
     * @return a future which contains an empty result after execution of the update.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<Void> buildUpdateActionsAndUpdate(@Nonnull final InventoryEntry entry,
                                                              @Nonnull final InventoryEntryDraft draft) {
        final List<UpdateAction<InventoryEntry>> updateActions =
            InventorySyncUtils.buildActions(entry, draft, syncOptions);
        if (!updateActions.isEmpty()) {
            return inventoryService.updateInventoryEntry(entry, updateActions)
                .thenAccept(updatedEntry -> statistics.incrementUpdated())
                .exceptionally(exception -> {
                    final String errorMessage = format(CTP_INVENTORY_ENTRY_UPDATE_FAILED, draft.getSku(),
                        SkuChannelKeyTuple.of(draft).getKey());
                    handleError(errorMessage, exception, 1);
                    return null;
                });
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Given an inventory entry {@code draft}, issues a request to the CTP project to create a corresponding Inventory
     * Entry.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP, the optional error callback
     * specified in the {@code syncOptions} is called.
     *
     * @param draft the inventory entry draft to create the inventory entry from.
     * @return a future which contains an empty result after execution of the create.
     */
    private CompletionStage<Void> attemptCreate(@Nonnull final InventoryEntryDraft draft) {
        return inventoryService.createInventoryEntry(draft)
                               .thenAccept(createdEntry -> statistics.incrementCreated())
                               .exceptionally(exception -> {
                                   final String errorMessage = format(CTP_INVENTORY_ENTRY_CREATE_FAILED, draft.getSku(),
                                       SkuChannelKeyTuple.of(draft).getKey());
                                   handleError(errorMessage, exception, 1);
                                   return null;
                               });
    }

    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed categories to sync.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     * @param exception    The exception that called caused the failure, if any.
     */
    private void handleError(@Nonnull final String errorMessage, @Nullable final Throwable exception,
                             final int failedTimes) {
        syncOptions.applyErrorCallback(errorMessage, exception);
        statistics.incrementFailed(failedTimes);
    }
}
