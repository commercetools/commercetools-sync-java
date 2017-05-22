package com.commercetools.sync.inventories;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import com.commercetools.sync.inventories.utils.InventorySyncUtils;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.models.Reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.commercetools.sync.inventories.utils.InventoryDraftTransformerUtils.transformToDrafts;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Default implementation of inventories sync process.
 */
public final class InventorySync extends BaseSync<InventoryEntryDraft, InventoryEntry, InventorySyncStatistics,
        InventorySyncOptions> {
    private static final String CTP_INVENTORY_FETCH_FAILED = "Failed to fetch existing inventory entries of SKUs %s.";
    private static final String CTP_CHANNEL_FETCH_FAILED = "Failed to fetch supply channels.";
    private static final String CTP_INVENTORY_ENTRY_UPDATE_FAILED = "Failed to update inventory entry of sku '%s' and "
        + "supply channel key '%s'.";
    private static final String INVENTORY_DRAFT_HAS_NO_SKU = "Failed to process inventory entry without sku.";
    private static final String INVENTORY_DRAFT_IS_NULL = "Failed to process null inventory draft.";
    private static final String CTP_CHANNEL_CREATE_FAILED = "Failed to create new supply channel of key '%s'.";
    private static final String CTP_INVENTORY_ENTRY_CREATE_FAILED = "Failed to create inventory entry of sku '%s' "
        + "and supply channel key '%s'.";

    //Cache that maps supply channel key to supply channel Id for supply channels existing in CTP project.
    private Map<String, String> supplyChannelKeyToId;

    @Nonnull
    private final InventoryService inventoryService;

    @Nonnull
    private final TypeService typeService;

    public InventorySync(@Nonnull final InventorySyncOptions syncOptions) {
        this(syncOptions, new InventoryServiceImpl(syncOptions.getCtpClient().getClient()),
                new TypeServiceImpl(syncOptions.getCtpClient().getClient()));
    }

    InventorySync(final InventorySyncOptions syncOptions, final InventoryService inventoryService,
                  final TypeService typeService) {
        super(new InventorySyncStatistics(), syncOptions);
        this.inventoryService = inventoryService;
        this.typeService = typeService;
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
    public CompletionStage<InventorySyncStatistics> syncDrafts(@Nonnull final List<InventoryEntryDraft>
                                                                       inventoryDrafts) {
        return super.syncDrafts(inventoryDrafts);
    }

    /**
     * Performs full process of synchronisation between inventory entries present in a system
     * and passed {@code inventories}. This is accomplished by:
     * <ul>
     *     <li>Comparing entries and drafts by {@code sku} and {@code supplyChannel} key</li>
     *     <li>Calculating of necessary updates and creation commands</li>
     *     <li>Actually <strong>performing</strong> changes in a target CTP project</li>
     * </ul>
     * The process is customized according to {@link InventorySyncOptions} passed to constructor of this object.
     *
     * <p><strong>Inherited doc:</strong>
     * {@inheritDoc}
     *
     * @param inventories {@link List} of {@link InventoryEntryDraft} resources that would be synced into CTP project.
     * @return {@link CompletionStage} with {@link InventorySyncStatistics} holding statistics of all sync
     *                                           processes performed by this sync instance
     */
    @Override
    protected CompletionStage<InventorySyncStatistics> processDrafts(@Nonnull final List<InventoryEntryDraft>
                                                                             inventories) {
        return populateSupplyChannels(inventories)
                .thenCompose(v -> splitToBatchesAndProcess(inventories))
                .exceptionally(ex -> {
                    handleFailure(CTP_CHANNEL_FETCH_FAILED, ex);
                    return statistics;
                });
    }

    /**
     * Converts {@code inventories} to {@link InventoryEntryDraft} objects and perform full synchronisation process
     * as described in {@link InventorySync#syncDrafts(List)}.
     *
     * @param inventories  {@link List} of {@link InventoryEntry} that you would like to sync into your CTP project.
     * @see InventorySync#syncDrafts(List)
     */
    @Override
    protected CompletionStage<InventorySyncStatistics> process(@Nonnull final List<InventoryEntry> inventories) {
        final List<InventoryEntryDraft> drafts = transformToDrafts(inventories);
        return processDrafts(drafts);
    }

    /**
     * Iterates through the whole {@code inventories} list and accumulates its valid drafts to batches. Every batch
     * is then processed by {@link InventorySync#processBatch(List)}. For invalid drafts from {@code inventories}
     * "processed" and "failed" counters from statistics are incremented and error callback is executed. Valid draft
     * is a {@link InventoryEntryDraft} object that is not {@code null} and its SKU is not empty.
     *
     * @param inventories {@link List} of {@link InventoryEntryDraft} resources that would be synced into CTP project.
     * @return {@link CompletionStage} with {@link InventorySyncStatistics} holding statistics of all sync
     *                                           processes performed by this sync instance
     */
    @Nonnull
    private CompletionStage<InventorySyncStatistics> splitToBatchesAndProcess(@Nonnull final List<InventoryEntryDraft>
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
                    statistics.incrementProcessed();
                    statistics.incrementFailed();
                    handleFailure(INVENTORY_DRAFT_HAS_NO_SKU, null);
                }
            } else {
                statistics.incrementProcessed();
                statistics.incrementFailed();
                handleFailure(INVENTORY_DRAFT_IS_NULL, null);
            }
        }
        if (!accumulator.isEmpty()) {
            completableFutures.add(processBatch(accumulator).toCompletableFuture());
        }
        return CompletableFuture
            .allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]))
            .thenApply(v -> statistics);
    }

    @Override
    @Nonnull
    public InventorySyncStatistics getStatistics() {
        return statistics;
    }

    /**
     * Methods tries to
     * <ul>
     *     <li>Fetch existing supply channels from CTP project</li>
     *     <li>Instantiate {@code supplyChannelKeyToId} map</li>
     *     <li>Create missing supply channels if needed</li>
     * </ul>
     * Method returns {@link CompletionStage} of {@link Void} that indicates method progress. It may contain exception
     * occurred during fetching supply channels.
     *
     * @param drafts {@link List} containing {@link InventoryEntryDraft} objects where missing supply channels can occur
     * @return {@link CompletionStage} of {@link Void} that indicates method progress. It may contain exception
     *     occurred during fetching supply channels
     */
    private CompletionStage<Void> populateSupplyChannels(@Nonnull final List<InventoryEntryDraft> drafts) {
        return inventoryService.fetchAllSupplyChannels()
                .thenAccept(existingSupplyChannels ->
                    supplyChannelKeyToId = existingSupplyChannels.stream()
                            .collect(toMap(Channel::getKey, Channel::getId))
                )
                .thenCompose(v -> createMissingSupplyChannels(drafts));
    }

    /**
     * When {@code ensureChannel} from {@link InventorySyncOptions} is set to {@code true} then attempts to create
     * missing supply channels. Missing supply channel is a supply channel of key that can not be found in CTP project,
     * but occurs in {@code drafts} list. Method returns {@link CompletionStage} of {@link Void} that indicates all
     * possible creation attempts progress.
     *
     * @param drafts {@link List} containing {@link InventoryEntryDraft} objects where missing supply channels can occur
     * @return {@link CompletionStage} of {@link Void} that indicates all possible creation attempts progress
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<Void> createMissingSupplyChannels(@Nonnull final List<InventoryEntryDraft> drafts) {
        if (syncOptions.shouldEnsureChannels()) {
            final List<String> missingChannelsKeys = drafts.stream()
                .map(SkuChannelKeyTuple::of)
                .map(SkuChannelKeyTuple::getKey)
                .distinct()
                .filter(Objects::nonNull)
                .filter(key -> !supplyChannelKeyToId.containsKey(key))
                .collect(toList());
            final List<CompletableFuture<Void>> creationStages = missingChannelsKeys.stream()
                .map(this::createMissingSupplyChannel)
                .map(CompletionStage::toCompletableFuture)
                .collect(toList());
            return CompletableFuture.allOf(creationStages.toArray(new CompletableFuture[creationStages.size()]));
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Fetches existing {@link InventoryEntry} objects from CTP project that correspond to passed {@code batchOfDrafts}.
     * Having existing inventory entries fetched, {@code batchOfDrafts} is compared and synced with fetched objects by
     * {@link InventorySync#compareAndSync(Map, List)} function. When fetching existing inventory entries results in
     * exception then error callback is executed and {@code batchOfDrafts} isn't processed.

     * @param batchOfDrafts batch of drafts that need to be synced
     * @return {@link CompletionStage} of {@link Void} that indicates method progress.
     */
    private CompletionStage<Void> processBatch(final List<InventoryEntryDraft> batchOfDrafts) {
        return fetchExistingInventories(batchOfDrafts)
                .thenCompose(existingInventories -> compareAndSync(existingInventories, batchOfDrafts))
                .exceptionally(ex -> {
                    handleFailure(format(CTP_INVENTORY_FETCH_FAILED, extractSkus(batchOfDrafts)), ex);
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
     * @param drafts drafts that need to be synced
     * @return {@link CompletionStage} of {@link Void} that indicates all possible creation/update attempts progress.
     */
    private CompletionStage<Void> compareAndSync(final Map<SkuChannelKeyTuple, InventoryEntry> existingInventories,
                                                 final List<InventoryEntryDraft> drafts) {
        final List<CompletableFuture<Void>> futures = new ArrayList<>(drafts.size());
        drafts.forEach(draft -> {
            final SkuChannelKeyTuple skuKeyOfDraft = SkuChannelKeyTuple.of(draft);
            if (existingInventories.containsKey(skuKeyOfDraft)) {
                final InventoryEntry existingEntry = existingInventories.get(skuKeyOfDraft);
                futures.add(attemptUpdate(existingEntry, draft).toCompletableFuture());
            } else {
                futures.add(attemptCreate(draft).toCompletableFuture());
            }
            statistics.incrementProcessed();
        });
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
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
        final Optional<InventoryEntryDraft> fixedDraft = replaceChannelReference(draft);
        if (fixedDraft.isPresent()) {
            final List<UpdateAction<InventoryEntry>> updateActions =
                InventorySyncUtils.buildActions(entry, fixedDraft.get(), syncOptions, typeService);
            if (!updateActions.isEmpty()) {
                return inventoryService.updateInventoryEntry(entry, updateActions)
                    .thenAccept(updatedEntry -> statistics.incrementUpdated())
                    .exceptionally(ex -> {
                        statistics.incrementFailed();
                        handleFailure(format(CTP_INVENTORY_ENTRY_UPDATE_FAILED, draft.getSku(),
                            SkuChannelKeyTuple.of(draft).getKey()), ex);
                        return null;
                    });
            }
        } else {
            statistics.incrementFailed();
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
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<Void> attemptCreate(final InventoryEntryDraft draft) {
        final Optional<InventoryEntryDraft> fixedDraft = replaceChannelReference(draft);
        if (fixedDraft.isPresent()) {
            return inventoryService.createInventoryEntry(fixedDraft.get())
                .thenAccept(createdEntry -> statistics.incrementCreated())
                .exceptionally(ex -> {
                    statistics.incrementFailed();
                    handleFailure(format(CTP_INVENTORY_ENTRY_CREATE_FAILED, draft.getSku(),
                        SkuChannelKeyTuple.of(draft).getKey()), ex);
                    return null;
                });
        } else {
            statistics.incrementFailed();
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns {@link Optional} that may contain {@link InventoryEntryDraft}. The payload of optional would be:
     * <ul>
     *     <li>Same {@code draft} instance if it has no reference to supply channel</li>
     *     <li>New {@link InventoryEntryDraft} instance if {@code draft} contains reference to supply channel.
     *     New instance would have same values as {@code draft} except for supply channel reference. Reference
     *     will be replaced with reference that points to ID of existing channel for key given in draft.</li>
     *     <li>Empty if supply channel for key wasn't found in {@code supplyChannelKeyToId}</li>
     * </ul>
     *
     * @param draft inventory entry draft from processed list
     * @return {@link Optional} with draft that is prepared to being created or compared with existing
     * {@link InventoryEntry}, or empty optional if method fails to find supply channel ID that should be referenced
     */
    private Optional<InventoryEntryDraft> replaceChannelReference(final InventoryEntryDraft draft) {
        final String supplyChannelKey = SkuChannelKeyTuple.of(draft).getKey();
        if (supplyChannelKey != null) {
            if (supplyChannelKeyToId.containsKey(supplyChannelKey)) {
                return Optional.of(
                    withSupplyChannel(draft, supplyChannelKeyToId.get(supplyChannelKey)));
            } else {
                handleFailure(format("Failed to find supply channel of key '%s'", supplyChannelKey), null);
                return Optional.empty();
            }
        }
        return Optional.of(draft);
    }

    /**
     * Returns new {@link InventoryEntryDraft} containing same data as {@code draft} except for
     * supply channel reference that is replaced by reference pointing to {@code supplyChannelId}.
     *
     * @param draft           draft where reference should be replaced
     * @param supplyChannelId ID of supply channel existing in target project
     * @return {@link InventoryEntryDraft} with supply channel reference pointing to {@code supplyChannelId}
     *      and other data same as in {@code draft}
     */
    private InventoryEntryDraft withSupplyChannel(@Nonnull final InventoryEntryDraft draft,
                                                  @Nonnull final String supplyChannelId) {
        final Reference<Channel> supplyChannelRef = Channel.referenceOfId(supplyChannelId);
        return InventoryEntryDraftBuilder.of(draft)
            .supplyChannel(supplyChannelRef)
            .build();
    }

    /**
     * Method tries to create supply channel of given {@code supplyChannelKey} in CTP project.
     * If operation succeed then {@code supplyChannelKeyToId} map is updated, otherwise
     * error callback function is executed.
     *
     * @param supplyChannelKey key of supply channel that seems to not exists in a system
     * @return {@link CompletionStage} instance that indicates method progress
     */
    private CompletionStage<Void> createMissingSupplyChannel(@Nonnull final String supplyChannelKey) {
        return inventoryService.createSupplyChannel(supplyChannelKey)
            .thenAccept(channel -> supplyChannelKeyToId.put(channel.getKey(), channel.getId()))
            .exceptionally(ex -> {
                handleFailure(format(CTP_CHANNEL_CREATE_FAILED, supplyChannelKey), ex);
                return null;
            });
    }

    /**
     * Given a reason message as {@link String} and {@link Throwable} exception, this method calls the optional error
     * callback specified in the {@code syncOptions}.
     *
     * @param message the reason of failure
     * @param exception the exception that occurred, if any
     */
    private void handleFailure(@Nonnull final String message, @Nullable final Throwable exception) {
        syncOptions.applyErrorCallback(message, exception);
    }
}
