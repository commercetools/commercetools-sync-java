package com.commercetools.sync.inventories;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.inventories.helpers.InventoryReferenceResolver;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import com.commercetools.sync.inventories.utils.InventorySyncUtils;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.InventoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.ChannelServiceImpl;
import com.commercetools.sync.services.impl.InventoryServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
    private static final String CTP_CHANNEL_FETCH_FAILED = "Failed to fetch supply channels.";
    private static final String CTP_INVENTORY_ENTRY_UPDATE_FAILED = "Failed to update inventory entry of sku '%s' and "
        + "supply channel key '%s'.";
    private static final String INVENTORY_DRAFT_HAS_NO_SKU = "Failed to process inventory entry without sku.";
    private static final String INVENTORY_DRAFT_IS_NULL = "Failed to process null inventory draft.";
    private static final String CTP_CHANNEL_CREATE_FAILED = "Failed to create new supply channel of key '%s'.";
    private static final String CTP_INVENTORY_ENTRY_CREATE_FAILED = "Failed to create inventory entry of sku '%s' "
        + "and supply channel key '%s'.";
    private static final String INVENTORY_DRAFT_REFERENCE_RESOLUTION_FAILED = "Failed to resolve reference on "
        + "InventoryEntryDraft with sku:'%s'. Reason: %s";


    private final InventoryService inventoryService;

    private final InventoryReferenceResolver referenceResolver;

    public InventorySync(@Nonnull final InventorySyncOptions syncOptions) {
        this(syncOptions, new InventoryServiceImpl(syncOptions.getCtpClient()),
            new ChannelServiceImpl(syncOptions.getCtpClient()), new TypeServiceImpl(syncOptions.getCtpClient()));
    }

    InventorySync(@Nonnull final InventorySyncOptions syncOptions, @Nonnull final InventoryService inventoryService,
                  @Nonnull final ChannelService channelService, @Nonnull final TypeService typeService) {
        super(new InventorySyncStatistics(), syncOptions);
        this.inventoryService = inventoryService;
        this.referenceResolver = new InventoryReferenceResolver(syncOptions, typeService, channelService);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     *     <strong>NOTE:</strong> {@code inventoryDrafts} are compared with existing inventory entries by {@code sku}
     *     and {@code supplyChannel} key. Every {@link InventoryEntryDraft} that contains {@code supplyChannel} should
     *     either:
     *     <ul>
     *         <li>have {@code supplyChannel} expanded, that means
     *         {@code inventoryEntryDraft.getSupplyChannel().getObj()} should return {@link Channel} object,
     *         which contains channel key</li>
     *         <li>or have {@code supplyChannel} not expanded and {@code supplyChannel} key should be provided in
     *         place of reference id, that means {@code inventoryEntryDraft.getSupplyChannel().getObj()} should
     *         return {@code null} and {@code inventoryEntryDraft.getSupplyChannel().getId()} should
     *         return {@code supplyChannel} key</li>
     *     </ul>
     *     This is important for proper resources comparision.
     * </p>
     */
    @Override
    public CompletionStage<InventorySyncStatistics> sync(@Nonnull final List<InventoryEntryDraft>
                                                                       inventoryDrafts) {
        return super.sync(inventoryDrafts);
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

    @Override
    @Nonnull
    public InventorySyncStatistics getStatistics() {
        return statistics;
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
     * For each draft from {@code drafts} it checks if there is corresponding entry in {@code existingInventories},
     * and then either attempts to update such entry with data from draft or attempts to create new entry from draft.
     * After comparision and performing action "processed" counter from statistics is incremented.
     * Method returns {@link CompletionStage} of {@link Void} that indicates all possible creation/update attempts
     * progress.
     *
     * @param existingInventories mapping of {@link SkuChannelKeyTuple} to {@link InventoryEntry} of instances existing
     *                            in a CTP project
     * @param inventoryEntryDrafts drafts that need to be synced
     * @return {@link CompletionStage} of {@link Void} that indicates all possible creation/update attempts progress.
     */
    private CompletionStage<Void> syncBatch(final Map<SkuChannelKeyTuple, InventoryEntry> existingInventories,
                                            final List<InventoryEntryDraft> inventoryEntryDrafts) {
        final List<CompletableFuture<Void>> futures = new ArrayList<>(inventoryEntryDrafts.size());
        inventoryEntryDrafts.forEach(inventoryEntryDraft ->
            futures.add(resolveReferencesAndSync(existingInventories, inventoryEntryDraft)));
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    }

    private CompletableFuture<Void> resolveReferencesAndSync(@Nonnull final Map<SkuChannelKeyTuple, InventoryEntry>
                                                                 existingInventories,
                                                             @Nonnull final InventoryEntryDraft inventoryEntryDraft) {
        final SkuChannelKeyTuple skuKeyOfDraft = SkuChannelKeyTuple.of(inventoryEntryDraft);
        return referenceResolver.resolveReferences(inventoryEntryDraft)
                                .thenCompose(resolvedDraft -> {
                                    if (existingInventories.containsKey(skuKeyOfDraft)) {
                                        final InventoryEntry existingEntry = existingInventories.get(skuKeyOfDraft);
                                        return attemptUpdate(existingEntry, resolvedDraft);
                                    } else {
                                        return attemptCreate(inventoryEntryDraft);
                                    }
                                })
                                .exceptionally(exception -> {
                                    if (exception instanceof CompletionException) {
                                        // Unwrap the exception from CompletionException
                                        exception = exception.getCause();
                                    }
                                    final String errorMessage = format(INVENTORY_DRAFT_REFERENCE_RESOLUTION_FAILED,
                                        inventoryEntryDraft.getSku(), exception.getMessage());
                                    handleError(errorMessage, exception, 1);
                                    return null;
                                })
                                .toCompletableFuture();
    }

    /**
     * Returns {@link CompletionStage} instance which may contain mapping of {@link SkuChannelKeyTuple} to
     * {@link InventoryEntry} of instances existing in a CTP project. Instances are fetched from API by skus, that
     * corresponds to skus present in {@code drafts}. If fetching existing instances results in exception then
     * returned {@link CompletionStage} contains such exception.
     *
     * @param drafts {@link List} of drafts
     * @return {@link CompletionStage} instance which contains either {@link Map} of {@link SkuChannelKeyTuple} to
     *      {@link InventoryEntry} of instances existing in a CTP project that correspond to passed {@code drafts} by
     *      sku comparision, or exception occurred during fetching existing inventory entries
     */
    private CompletionStage<Map<SkuChannelKeyTuple, InventoryEntry>> fetchExistingInventories(final
                                                                                              List<InventoryEntryDraft>
                                                                                                  drafts) {
        final Set<String> skus = extractSkus(drafts);
        return inventoryService.fetchInventoryEntriesBySkus(skus)
            .thenApply(existingEntries -> existingEntries.stream().collect(toMap(SkuChannelKeyTuple::of,
                entry -> entry)));
    }

    /**
     * Returns distinct SKUs present in {@code inventories}.
     *
     * @param inventories {@link List} of {@link InventoryEntryDraft} where each draft contains its sku
     * @return {@link Set} of distinct SKUs found in {@code inventories}.
     */
    private Set<String> extractSkus(final List<InventoryEntryDraft> inventories) {
        return inventories.stream()
                .map(InventoryEntryDraft::getSku)
                .collect(Collectors.toSet());
    }

    /**
     * Tries to update {@code entry} in CTP project with data from {@code draft}.
     * It calculates list of {@link UpdateAction} and calls API only when there is a need.
     * Before calculate differences, the channel reference from {@code draft} is replaced, so it points to
     * proper channel ID in target system.
     * It either updates inventory entry and increments "updated" counter in statistics, or increments "failed" counter
     * and executes error callback function in case of any exception.
     *
     * @param entry entry from existing system that could be updated.
     * @param draft draft containing data that could differ from data in {@code entry}.
     *              <strong>Sku isn't compared</strong>
     * @return {@link CompletionStage} of {@link Void} that indicates method progress
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<Void> attemptUpdate(final InventoryEntry entry, final InventoryEntryDraft draft) {
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
     * Tries to create Inventory Entry in CTP project, using {@code draft}.
     * Before calling API, the channel reference from {@code draft} is replaced, so it points to proper channel ID
     * in target system.
     * It either creates inventory entry and increments "created" counter in statistics, or increments "failed" counter
     * and executes error callback function in case of any exception.
     *
     * @param draft draft of new inventory entry
     * @return {@link CompletionStage} instance that indicates method progress
     */
    private CompletionStage<Void> attemptCreate(final InventoryEntryDraft draft) {
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
