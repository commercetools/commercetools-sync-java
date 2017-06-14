package com.commercetools.sync.inventories;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.inventories.helpers.InventoryEntryIdentifier;
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
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.inventories.InventorySyncMessages.CTP_INVENTORY_ENTRY_CREATE_FAILED;
import static com.commercetools.sync.inventories.InventorySyncMessages.CTP_INVENTORY_ENTRY_UPDATE_FAILED;
import static com.commercetools.sync.inventories.InventorySyncMessages.CTP_INVENTORY_FETCH_FAILED;
import static com.commercetools.sync.inventories.InventorySyncMessages.FAILED_TO_RESOLVE_CUSTOM_TYPE;
import static com.commercetools.sync.inventories.InventorySyncMessages.FAILED_TO_RESOLVE_SUPPLY_CHANNEL;
import static com.commercetools.sync.inventories.InventorySyncMessages.INVENTORY_DRAFT_HAS_NO_SKU;
import static com.commercetools.sync.inventories.InventorySyncMessages.INVENTORY_DRAFT_IS_NULL;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Default implementation of inventories sync process.
 */
public final class InventorySync extends BaseSync<InventoryEntryDraft, InventorySyncStatistics, InventorySyncOptions> {

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
     * is then processed by {@link InventorySync#processBatch(List)}.
     *
     * <p><strong>Inherited doc:</strong>
     * {@inheritDoc}
     *
     * @param inventories {@link List} of {@link InventoryEntryDraft} resources that would be synced into CTP project.
     * @return {@link CompletionStage} with {@link InventorySyncStatistics} holding statistics of all sync
     *                                           processes performed by this sync instance
     */
    @Override
    protected CompletionStage<InventorySyncStatistics> process(@Nonnull final List<InventoryEntryDraft>
                                                                       inventories) {
        final List<InventoryEntryDraft> validInventories = inventories.stream()
            .filter(this::validateDraft)
            .collect(toList());
        final List<CompletableFuture<Void>> completableFutures = IntStream
            .range(0, calculateAmountOfBatches(validInventories.size()))
            .mapToObj(batchIndex -> getBatch(batchIndex, validInventories))
            .map(this::processBatch)
            .map(CompletionStage::toCompletableFuture)
            .collect(toList());
        return allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]))
            .thenApply(v -> {
                statistics.incrementProcessed(inventories.size());
                return statistics;
            });
    }

    /**
     * Checks if a draft is valid for further processing. If so, then returns {@code true}. Otherwise handles an error
     * and returns {@code false}. A valid draft is a {@link InventoryEntryDraft} object that is not {@code null} and its
     * SKU is not empty.
     *
     * @param draft nullable draft
     * @return boolean that indicate if given {@code draft} is valid for sync
     */
    private boolean validateDraft(@Nullable final InventoryEntryDraft draft) {
        if (draft == null) {
            handleError(INVENTORY_DRAFT_IS_NULL, null, 1);
        } else if (isBlank(draft.getSku())) {
            handleError(INVENTORY_DRAFT_HAS_NO_SKU, null, 1);
        } else {
            return true;
        }
        return false;
    }

    /**
     * Calculates amount of batches, available in a list of a given size. The batch size is specified by a sync options.
     *
     * @param listSize size of a list
     * @return amount of batches, available in a list of a given size
     */
    private int calculateAmountOfBatches(int listSize) {
        return (listSize + syncOptions.getBatchSize() - 1) / syncOptions.getBatchSize();
    }

    /**
     * Returns a batch of drafts, extracted from a given list. The batch size is specified by a sync options.
     *
     * @param batchIndex zero-based index of a batch
     * @param drafts list of drafts
     * @return an n-th batch of drafts, where n is specified by {@code batchIndex}
     */
    @Nonnull
    private List<InventoryEntryDraft> getBatch(final int batchIndex, @Nonnull final List<InventoryEntryDraft> drafts) {
        final int startIndex = batchIndex * syncOptions.getBatchSize();
        final int endIndex = min((batchIndex + 1) * syncOptions.getBatchSize(), drafts.size());
        return drafts.subList(startIndex, endIndex);
    }

    /**
     * Fetches existing {@link InventoryEntry} objects from CTP project that correspond to passed {@code batchOfDrafts}.
     * Having existing inventory entries fetched, {@code batchOfDrafts} is compared and synced with fetched objects by
     * {@link InventorySync#syncBatch(List, List)} function. When fetching existing inventory entries results in
     * an empty optional then {@code batchOfDrafts} isn't processed.
     *
     * @param batchOfDrafts batch of drafts that need to be synced
     * @return {@link CompletionStage} of {@link Void} that indicates method progress.
     */
    private CompletionStage<Void> processBatch(@Nonnull final List<InventoryEntryDraft> batchOfDrafts) {
        return fetchExistingInventories(batchOfDrafts)
            .thenCompose(oldInventoriesOptional -> oldInventoriesOptional
                .map(oldInventories -> syncBatch(oldInventories, batchOfDrafts))
                .orElseGet(() -> completedFuture(null)));
    }

    /**
     * Given a list of inventory entry drafts, this method extracts a list of all distinct SKUs from the drafts, then
     * it tries to fetch a list of Inventory entries with those SKUs. If operation succeed then {@link CompletionStage}
     * containing {@link Optional} with fetched {@link List} is returned, otherwise an error is handled and
     * {@link CompletionStage} with empty {@link Optional} is returned.
     *
     * @param drafts {@link List} of inventory entry drafts
     * @return a future which contains an {@link Optional} that may contain list of inventory entries
     */
    private CompletionStage<Optional<List<InventoryEntry>>> fetchExistingInventories(@Nonnull final
                                                                                     List<InventoryEntryDraft> drafts) {
        final Set<String> skus = extractSkus(drafts);
        return inventoryService.fetchInventoryEntriesBySkus(skus)
            .thenApply(Optional::of)
            .exceptionally(exception -> {
                final String errorMessage = format(CTP_INVENTORY_FETCH_FAILED, extractSkus(drafts));
                handleError(errorMessage, exception, drafts.size());
                return Optional.empty();
            });
    }

    /**
     * Given a list of inventory entry {@code drafts}, this method resolves the references of each entry and attempts to
     * sync it to the CTP project depending whether the references resolution was successful. In addition the given
     * {@code oldInventories} list is converted to a {@link Map} of an identifier to an inventory entry, for a resources
     * comparison reason.
     *
     * @param oldInventories inventory entries from CTP
     * @param inventoryEntryDrafts drafts that need to be synced
     * @return a future which contains an empty result after execution of the update
     */
    private CompletionStage<Void> syncBatch(@Nonnull final List<InventoryEntry> oldInventories,
                                            @Nonnull final List<InventoryEntryDraft> inventoryEntryDrafts) {
        final Map<InventoryEntryIdentifier , InventoryEntry> identifierToOldInventoryEntry = oldInventories
            .stream().collect(toMap(InventoryEntryIdentifier::of, identity()));
        final List<CompletableFuture<Void>> futures = new ArrayList<>(inventoryEntryDrafts.size());
        inventoryEntryDrafts.forEach(inventoryEntryDraft ->
            futures.add(resolveReferences(inventoryEntryDraft)
                .thenCompose(resolvedDraftOptional -> resolvedDraftOptional
                    .map(resolvedDraft -> syncDraft(identifierToOldInventoryEntry, resolvedDraft))
                    .orElseGet(() -> completedFuture(null)))));
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
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
     * Given an inventory entry {@code draft}, this method resolves all references on the draft. If the the references
     * resolution was successful this method returns a future which contains an {@link Optional} containing resolved
     * inventory entry draft. Otherwise an error is handled and a future containing an empty optional is returned.
     *
     * @param inventoryEntryDraft an inventory entry draft which references has to be resolved
     * @return a future which may contain the resolved inventory entry draft
     */
    private CompletableFuture<Optional<InventoryEntryDraft>> resolveReferences(@Nonnull final InventoryEntryDraft
                                                                                   inventoryEntryDraft) {
        return referenceResolver.resolveCustomTypeReference(inventoryEntryDraft)
            .thenCompose(draftWithResolvedCustomTypeReference -> referenceResolver
                .resolveSupplyChannelReference(draftWithResolvedCustomTypeReference)
                .thenApply(Optional::of)
                .exceptionally(exception -> {
                    final String errorMessage = format(FAILED_TO_RESOLVE_SUPPLY_CHANNEL,
                        inventoryEntryDraft.getSku(), exception.getMessage());
                    handleError(errorMessage, exception, 1);
                    return Optional.empty();
                }))
            .exceptionally(exception -> {
                final String errorMessage = format(FAILED_TO_RESOLVE_CUSTOM_TYPE,
                    inventoryEntryDraft.getSku(), exception.getMessage());
                handleError(errorMessage, exception, 1);
                return Optional.empty();
            })
            .toCompletableFuture();
    }

    /**
     * Checks if the {@code resolvedDraft} matches with an old existing inventory entry. If it does, it tries to update
     * it. If it doesn't, it creates it.
     *
     * @param oldInventories map of {@link InventoryEntryIdentifier} to old {@link InventoryEntry} instances
     * @param resolvedDraft inventory entry draft which has its references resolved
     * @return a future which contains an empty result after execution of the update
     */
    private CompletableFuture<Void> syncDraft(@Nonnull final Map<InventoryEntryIdentifier , InventoryEntry>
                                                  oldInventories,
                                              @Nonnull final InventoryEntryDraft resolvedDraft) {
        final InventoryEntry oldInventory = oldInventories.get(InventoryEntryIdentifier.of(resolvedDraft));
        return oldInventory != null
            ? buildUpdateActionsAndUpdate(oldInventory, resolvedDraft).toCompletableFuture()
            : create(resolvedDraft).toCompletableFuture();
    }

    /**
     * Given an existing {@link InventoryEntry} and a new {@link InventoryEntryDraft}, the method calculates all the
     * update actions required to synchronize the existing entry to be the same as the new one. If there are update
     * actions found, a request is made to CTP to update the existing entry, otherwise it doesn't issue a request.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP, the error handling method
     * is called.
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
                .thenAccept(updatedInventory -> statistics.incrementUpdated())
                .exceptionally(exception -> {
                    final Reference<Channel> supplyChannel = draft.getSupplyChannel();
                    final String errorMessage = format(CTP_INVENTORY_ENTRY_UPDATE_FAILED, draft.getSku(),
                        supplyChannel != null ? supplyChannel.getId() : null);
                    handleError(errorMessage, exception, 1);
                    return null;
                });
        }
        return completedFuture(null);
    }

    /**
     * Given an inventory entry {@code draft}, issues a request to the CTP project to create a corresponding Inventory
     * Entry.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP, the error handling method
     * is called.
     *
     * @param draft the inventory entry draft to create the inventory entry from.
     * @return a future which contains an empty result after execution of the create.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<Void> create(@Nonnull final InventoryEntryDraft draft) {
        return inventoryService.createInventoryEntry(draft)
            .thenAccept(createdInventory -> statistics.incrementCreated())
            .exceptionally(exception -> {
                final Reference<Channel> supplyChannel = draft.getSupplyChannel();
                final String errorMessage = format(CTP_INVENTORY_ENTRY_CREATE_FAILED, draft.getSku(),
                    supplyChannel != null ? supplyChannel.getId() : null);
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
