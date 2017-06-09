package com.commercetools.sync.inventories;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import com.commercetools.sync.inventories.utils.InventorySyncUtils;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.ConcurrentModificationException;
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
import java.util.stream.IntStream;

import static com.commercetools.sync.inventories.ChannelKeyExtractor.extractChannelKey;
import static com.commercetools.sync.inventories.InventorySyncMessages.CHANNEL_KEY_MAPPING_DOESNT_EXIST;
import static com.commercetools.sync.inventories.InventorySyncMessages.CTP_CHANNEL_CREATE_FAILED;
import static com.commercetools.sync.inventories.InventorySyncMessages.CTP_CHANNEL_FETCH_FAILED;
import static com.commercetools.sync.inventories.InventorySyncMessages.CTP_INVENTORY_ENTRY_CREATE_FAILED;
import static com.commercetools.sync.inventories.InventorySyncMessages.CTP_INVENTORY_ENTRY_UPDATE_FAILED;
import static com.commercetools.sync.inventories.InventorySyncMessages.CTP_INVENTORY_FETCH_FAILED;
import static com.commercetools.sync.inventories.InventorySyncMessages.INVENTORY_DRAFT_HAS_NO_SKU;
import static com.commercetools.sync.inventories.InventorySyncMessages.INVENTORY_DRAFT_IS_NULL;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Default implementation of inventories sync process.
 */
public final class InventorySync extends BaseSync<InventoryEntryDraft, InventorySyncStatistics, InventorySyncOptions> {

    static final int ATTEMPTS_ON_409_LIMIT = 5;

    private final InventoryService inventoryService;

    private final TypeService typeService;

    public InventorySync(@Nonnull final InventorySyncOptions syncOptions) {
        this(syncOptions, new InventoryServiceImpl(syncOptions.getCtpClient()),
                new TypeServiceImpl(syncOptions.getCtpClient()));
    }

    InventorySync(@Nonnull final InventorySyncOptions syncOptions, @Nonnull final InventoryService inventoryService,
                  @Nonnull final TypeService typeService) {
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
    public CompletionStage<InventorySyncStatistics> sync(@Nonnull final List<InventoryEntryDraft>
                                                                       inventoryDrafts) {
        return super.sync(inventoryDrafts);
    }

    /**
     * Performs full process of synchronisation between inventory entries present in a system
     * and passed {@code inventoryDrafts}. This is accomplished by:
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
     * @param inventoryDrafts {@link List} of {@link InventoryEntryDraft} resources that would be synced into CTP
     *                                   project.
     * @return {@link CompletionStage} with {@link InventorySyncStatistics} holding statistics of all sync
     *                                           processes performed by this sync instance
     */
    @Override
    protected CompletionStage<InventorySyncStatistics> process(@Nonnull final List<InventoryEntryDraft>
                                                                       inventoryDrafts) {
        final List<InventoryEntryDraft> validDrafts = inventoryDrafts.stream()
            .filter(this::validateDraft)
            .collect(toList());
        return populateSupplyChannels(validDrafts)
            .thenCompose(supplyChannelKeyToIdOptional -> supplyChannelKeyToIdOptional
                .map(supplyChannelKeyToId -> processDrafts(validDrafts, supplyChannelKeyToId))
                .orElseGet(() -> completedFuture(statistics)));
    }

    /**
     * Process given {@code drafts} in batches and returns {@link CompletionStage} with statistics of that processing.
     *
     * @param drafts {@link List} of {@link InventoryEntryDraft} resources that would be synced into CTP project.
     * @param supplyChannelKeyToId mapping of supply channel key to supply channel Id for supply channels existing in
     *                             CTP project.
     * @return {@link CompletionStage} with {@link InventorySyncStatistics} holding statistics of all sync
     *                                           processes performed by this sync instance
     */
    @Nonnull
    private CompletionStage<InventorySyncStatistics> processDrafts(@Nonnull final List<InventoryEntryDraft> drafts,
                                                                   @Nonnull final Map<String, String>
                                                                       supplyChannelKeyToId) {
        final List<CompletableFuture<Void>> completableFutures = IntStream
            .range(0, calculateAmountOfBatches(drafts.size()))
            .mapToObj(batchIndex -> getBatch(batchIndex, drafts))
            .map(batch -> processBatch(batch, supplyChannelKeyToId))
            .map(CompletionStage::toCompletableFuture)
            .collect(toList());
        return allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]))
            .thenApply(v -> statistics);
    }

    int calculateAmountOfBatches(int listSize) {
        return (listSize + syncOptions.getBatchSize() - 1) / syncOptions.getBatchSize();
    }

    List<InventoryEntryDraft> getBatch(int batchIndex, final List<InventoryEntryDraft> drafts) {
        final int startIndex = batchIndex * syncOptions.getBatchSize();
        final int endIndex = min((batchIndex + 1) * syncOptions.getBatchSize(), drafts.size());
        return drafts.subList(startIndex, endIndex);
    }

    /**
     * Checks if a draft is valid for further processing. If so, then returns {@code true}.
     * Otherwise applies an {@code errorCallback}, increments "failed" and "proceessed" statistics and returns
     * {@code false}.
     *
     * @param draft nullable draft, requested for sync
     * @return boolean that indicate if given {@code draft} is valid for sync
     */
    private boolean validateDraft(@Nullable final InventoryEntryDraft draft) {
        if (draft == null) {
            syncOptions.applyErrorCallback(INVENTORY_DRAFT_IS_NULL, null);
        } else if (isBlank(draft.getSku())) {
            syncOptions.applyErrorCallback(INVENTORY_DRAFT_HAS_NO_SKU, null);
        } else {
            return true;
        }
        statistics.incrementProcessed();
        statistics.incrementFailed();
        return false;
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
     *     <li>Apply error callback if fetching supply channels fails</li>
     *     <li>Instantiate {@code supplyChannelKeyToId} map</li>
     *     <li>Create missing supply channels if needed</li>
     * </ul>
     * Method returns {@link CompletionStage} of {@link Optional} that may contain mapping of supply channels'
     * keys to ids. An empty optional indicates that fetching existing channels failed.
     *
     * @param drafts {@link List} containing {@link InventoryEntryDraft} objects where missing supply channels can occur
     * @return {@link CompletionStage} of {@link Optional} that may contain mapping of supply channels' keys to ids.
     */
    private CompletionStage<Optional<Map<String, String>>> populateSupplyChannels(@Nonnull final
                                                                                  List<InventoryEntryDraft> drafts) {
        return inventoryService.fetchAllSupplyChannels()
            .thenApply(existingSupplyChannels -> existingSupplyChannels.stream()
                .collect(toMap(Channel::getKey, Channel::getId)))
            .exceptionally(exception -> {
                syncOptions.applyErrorCallback(CTP_CHANNEL_FETCH_FAILED, exception);
                return null;
            })
            .thenCompose(supplyChannelKeyToId -> createMissingSupplyChannels(drafts, supplyChannelKeyToId))
            .thenApply(Optional::ofNullable);
    }

    /**
     * When {@code ensureChannel} from {@link InventorySyncOptions} is set to {@code true} then attempts to create
     * missing supply channels. Missing supply channel is a supply channel of key that can not be found in CTP project,
     * but occurs in {@code drafts} list. Method returns {@link CompletionStage} of {@link Map} that contains updated
     * {@code supplyChannelKeyToId}. Null {@code supplyChannelKeyToId} results in null value within completion stage.
     *
     * @param drafts {@link List} containing {@link InventoryEntryDraft} objects where missing supply channels can occur
     * @param supplyChannelKeyToId mapping of supply channel key to supply channel Id for supply channels existing in
     *                             CTP project
     * @return {@link CompletionStage} of {@link Map} that contains updated {@code supplyChannelKeyToId} or {@code null}
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<Map<String, String>> createMissingSupplyChannels(@Nonnull final List<InventoryEntryDraft>
                                                                                     drafts,
                                                                             @Nullable final Map<String, String>
                                                                                 supplyChannelKeyToId) {
        if (supplyChannelKeyToId == null) {
            return completedFuture(null);
        } else if (syncOptions.shouldEnsureChannels()) {
            final List<String> missingChannelsKeys = findMissingChannelsKeys(drafts, supplyChannelKeyToId);
            final List<CompletableFuture<Optional<Channel>>> newChannelsFutures =
                createSupplyChannelsOfKeys(missingChannelsKeys);
            return allOf(newChannelsFutures.toArray(new CompletableFuture[newChannelsFutures.size()]))
                .thenRun(() -> {
                    final Map<String, String> newChannelKeyToId = newChannelsFutures.stream()
                        .map(CompletableFuture::join)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(toMap(Channel::getKey, Channel::getId));
                    supplyChannelKeyToId.putAll(newChannelKeyToId);
                })
                .thenApply(v -> supplyChannelKeyToId);
        } else {
            return completedFuture(supplyChannelKeyToId);
        }
    }

    /**
     * Tries to find supply channels' keys, that occur in {@code drafts} but aren't mapped by
     * {@code supplyChannelKeyToId}.
     *
     * @param drafts drafts that need to be synced
     * @param supplyChannelKeyToId mapping of supply channel key to supply channel Id for supply channels existing in
     *                             CTP project.
     * @return list with missing channels' keys
     */
    @Nonnull
    private List<String> findMissingChannelsKeys(@Nonnull final List<InventoryEntryDraft> drafts,
                                                 @Nonnull final Map<String, String> supplyChannelKeyToId) {
        return drafts.stream()
            .map(ChannelKeyExtractor::extractChannelKey)
            .distinct()
            .filter(Objects::nonNull)
            .filter(key -> !supplyChannelKeyToId.containsKey(key))
            .collect(toList());
    }

    /**
     * Tries to create supply channels of keys given in a {@code missingChannelsKeys} list.
     *
     * @param missingChannelsKeys missing channels' keys
     * @return list fulfilled with completable futures that contain optionals which may contain created channels
     */
    @Nonnull
    private List<CompletableFuture<Optional<Channel>>> createSupplyChannelsOfKeys(@Nonnull final List<String>
                                                                                      missingChannelsKeys) {
        return missingChannelsKeys.stream()
            .map(this::createMissingSupplyChannel)
            .map(CompletionStage::toCompletableFuture)
            .collect(toList());
    }

    /**
     * Method tries to create supply channel of given {@code supplyChannelKey} in CTP project.
     * If operation succeed then {@link CompletionStage} containing {@link Optional} with created {@link Channel} is
     * returned, otherwise error callback function is executed and {@link CompletionStage} with empty {@link Optional}
     * is returned.
     *
     * @param supplyChannelKey key of supply channel that seems to not exists in a system
     * @return {@link CompletionStage} instance with {@link Optional} that may contain created {@link Channel}
     */
    private CompletionStage<Optional<Channel>> createMissingSupplyChannel(@Nonnull final String supplyChannelKey) {
        return inventoryService.createSupplyChannel(supplyChannelKey)
            .thenApply(Optional::of)
            .exceptionally(exception -> {
                syncOptions.applyErrorCallback(format(CTP_CHANNEL_CREATE_FAILED, supplyChannelKey), exception);
                return Optional.empty();
            });
    }

    /**
     * Fetches existing {@link InventoryEntry} objects from CTP project that correspond to passed {@code batchOfDrafts}.
     * Having existing inventory entries fetched, {@code batchOfDrafts} is compared and synced with fetched objects by
     * {@link InventorySync#compareAndSync(List, List, Map)} function. When fetching existing inventory entries results
     * in exception then error callback is executed and {@code batchOfDrafts} isn't processed.

     * @param batchOfDrafts batch of drafts that need to be synced
     * @param supplyChannelKeyToId mapping of supply channel key to supply channel Id for supply channels existing in
     *                             CTP project.
     * @return {@link CompletionStage} of {@link Void} that indicates method progress
     */
    private CompletionStage<Void> processBatch(@Nonnull final List<InventoryEntryDraft> batchOfDrafts,
                                               @Nonnull final Map<String, String> supplyChannelKeyToId) {
        return fetchExistingInventories(batchOfDrafts)
            .thenCompose(oldInventoriesOptional -> oldInventoriesOptional
                .map(oldInventories -> compareAndSync(oldInventories, batchOfDrafts, supplyChannelKeyToId))
                .orElseGet(() -> completedFuture(null)));
    }

    /**
     * For each draft from {@code drafts} it checks if there is corresponding entry in {@code oldInventories},
     * and then either attempts to update such entry with data from draft or attempts to create new entry from draft.
     * After comparision and performing action "processed" and other relevant counter from statistics are incremented.
     * Method returns {@link CompletionStage} of {@link Void} that indicates all possible creation/update attempts
     * progress.
     *
     * @param oldInventories mapping of {@link ChannelKeyExtractor} to {@link InventoryEntry} of instances existing
     *                            in a CTP project
     * @param drafts drafts that need to be synced
     * @param supplyChannelKeyToId mapping of supply channel key to supply channel Id for supply channels existing in
     *                             CTP project.
     * @return {@link CompletionStage} of {@link Void} that indicates all possible creation/update attempts progress.
     */
    private CompletionStage<Void> compareAndSync(@Nonnull final List<InventoryEntry> oldInventories,
                                                 @Nonnull final List<InventoryEntryDraft> drafts,
                                                 @Nonnull final Map<String, String> supplyChannelKeyToId) {
        final List<CompletableFuture<InventoryEntry>> futures = new ArrayList<>(drafts.size());
        drafts.forEach(draft -> {
            final Optional<InventoryEntryDraft> fixedDraft = replaceChannelReference(draft, supplyChannelKeyToId);
            if (fixedDraft.isPresent()) {
                futures.add(
                    findCorrespondingEntry(oldInventories, fixedDraft.get())
                        .map(oldInventory -> update(oldInventory, fixedDraft.get(), ATTEMPTS_ON_409_LIMIT))
                        .orElseGet(() -> create(fixedDraft.get()))
                        .toCompletableFuture());
            } else {
                statistics.incrementFailed();
            }
            statistics.incrementProcessed();
        });
        return allOf(futures.toArray(new CompletableFuture[futures.size()]));
    }

    /**
     * Attempts to find in an {@code oldInventories} list an {@link InventoryEntry} of the same {@code sku} and
     * {@code supplyChannel} as {@code draft} instance. Supply channels, if not null, are compared by id.
     *
     * @param oldInventories list of old {@link InventoryEntry} instances
     * @param draft new {@link InventoryEntryDraft} instance where supply channel reference, if present, points to
     *              channel in target CTP project
     * @return {@link Optional} that may contain {@link InventoryEntry} found in {@code oldInventories} that correspond
     *      to {@code draft} or empty {@link Optional} if no such entry was found
     */
    private Optional<InventoryEntry> findCorrespondingEntry(@Nonnull final List<InventoryEntry> oldInventories,
                                                            @Nonnull final InventoryEntryDraft draft) {
        return oldInventories.stream()
            .filter(oldInventory -> oldInventory.getSku().equals(draft.getSku()))
            .filter(oldInventory -> hasSameSupplyChannel(oldInventory, draft))
            .findFirst();
    }

    private boolean hasSameSupplyChannel(final InventoryEntry inventory, final InventoryEntryDraft draft) {
        return draft.getSupplyChannel() == null
            ? (inventory.getSupplyChannel() == null)
            : ((inventory.getSupplyChannel() != null)
            && inventory.getSupplyChannel().getId().equals(draft.getSupplyChannel().getId()));
    }

    /**
     * Tries to fetch from a CTP a list of inventory entries of SKUs present in {@code drafts}.
     * If operation succeed then {@link CompletionStage} containing {@link Optional} with fetched {@link List} is
     * returned, otherwise error callback function is executed and {@link CompletionStage} with empty {@link Optional}
     * is returned.
     *
     * @param drafts {@link List} of drafts
     * @return {@link CompletionStage} instance with {@link Optional} that may contain list of inventory entries
     */
    private CompletionStage<Optional<List<InventoryEntry>>> fetchExistingInventories(@Nonnull final
                                                                                     List<InventoryEntryDraft> drafts) {
        final Set<String> skus = extractSkus(drafts);
        return inventoryService.fetchInventoryEntriesBySkus(skus)
            .thenApply(Optional::of)
            .exceptionally(exception -> {
                syncOptions.applyErrorCallback(format(CTP_INVENTORY_FETCH_FAILED, extractSkus(drafts)), exception);
                return Optional.empty();
            });
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
     * It either updates inventory entry and increments "updated" counter in statistics, or increments "failed" counter
     * and executes error callback function in case of any exception.
     * It attempts to fetch latest version of an {@code entry}, recalculate update actions and retry update in case of
     * concurrent modification error occurrence.
     *
     * @param entry entry from existing system that could be updated.
     * @param draft draft with the resolved channel reference, containing new data
     * @param retryOn409AttemptsLeft counter which indicates if another retry attempt should be raised
     * @return {@link CompletionStage} that may contain updated {@link InventoryEntry}
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<InventoryEntry> update(final InventoryEntry entry,
                                                   final InventoryEntryDraft draft,
                                                   final int retryOn409AttemptsLeft) {
        final List<UpdateAction<InventoryEntry>> updateActions =
            InventorySyncUtils.buildActions(entry, draft, syncOptions, typeService);
        if (!updateActions.isEmpty()) {
            return inventoryService.updateInventoryEntry(entry, updateActions)
                .thenApply(updatedInventory -> {
                    statistics.incrementUpdated();
                    return completedFuture(updatedInventory);
                })
                .exceptionally(exception -> handleUpdateException(exception.getCause(), draft, retryOn409AttemptsLeft)
                    .toCompletableFuture())
                .thenCompose(completableFuture -> completableFuture);
        }
        return completedFuture(entry);
    }

    /**
     * Tries to fetch latest version of a corresponding entry, recalculate update actions and retry update if
     * {@code exception} indicates concurrent modification error. Retry is performed only then, when
     * {@code retryOn409AttemptsLeft} is greater than 0. Returns {@link CompletionStage} of update invocation, if
     * retry was attempted. Otherwise it increments "failed" counter, executes error callback function and returns
     * {@link CompletionStage} with a {@code null} value.
     *
     * @param exception exception returned from CTP
     * @param draft draft draft with the resolved channel reference, containing new data
     * @param retryOn409AttemptsLeft counter which indicates if another retry attempt should be raised
     * @return {@link CompletionStage} that may contain updated {@link InventoryEntry}
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    CompletionStage<InventoryEntry> handleUpdateException(final Throwable exception,
                                                          final InventoryEntryDraft draft,
                                                          final int retryOn409AttemptsLeft) {
        if ((retryOn409AttemptsLeft > 0) && (exception instanceof ConcurrentModificationException)) {
            return inventoryService.fetchInventoryEntry(draft.getSku(), draft.getSupplyChannel())
                .thenCompose(inventoryEntryOptional -> inventoryEntryOptional
                    .map(inventoryEntry -> update(inventoryEntry, draft, retryOn409AttemptsLeft - 1)
                        .toCompletableFuture())
                    .orElseGet(() -> {
                        reportFailureOfInventoryUpdate(draft, exception);
                        return completedFuture(null);
                    })
                )
                .exceptionally(exceptionAfterFetch -> {
                    reportFailureOfInventoryUpdate(draft, exceptionAfterFetch);
                    return null;
                });
        } else {
            reportFailureOfInventoryUpdate(draft, exception);
            return completedFuture(null);
        }
    }

    private void reportFailureOfInventoryUpdate(final InventoryEntryDraft draft, final Throwable exception) {
        statistics.incrementFailed();
        syncOptions.applyErrorCallback(format(CTP_INVENTORY_ENTRY_UPDATE_FAILED, draft.getSku(),
            extractChannelKey(draft)), exception);
    }

    /**
     * Tries to create Inventory Entry in CTP project, using {@code draft}.
     * It either creates inventory entry and increments "created" counter in statistics, or increments "failed" counter
     * and executes error callback function in case of any exception.
     *
     * @param draft draft of new inventory entry with the resolved channel reference
     * @return {@link CompletionStage} instance that indicates method progress
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<InventoryEntry> create(final InventoryEntryDraft draft) {
        return inventoryService.createInventoryEntry(draft)
            .thenApply(createdInventory -> {
                statistics.incrementCreated();
                return createdInventory;
            })
            .exceptionally(exception -> {
                statistics.incrementFailed();
                syncOptions.applyErrorCallback(format(CTP_INVENTORY_ENTRY_CREATE_FAILED, draft.getSku(),
                    extractChannelKey(draft)), exception);
                return null;
            });
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
     * @param supplyChannelKeyToId mapping of supply channel key to supply channel Id for supply channels existing in
     *                             CTP project.
     * @return {@link Optional} with draft that is prepared to being created or compared with existing
     * {@link InventoryEntry}, or empty optional if method fails to find supply channel ID that should be referenced
     */
    private Optional<InventoryEntryDraft> replaceChannelReference(final InventoryEntryDraft draft,
                                                                  final Map<String, String> supplyChannelKeyToId) {
        final String supplyChannelKey = extractChannelKey(draft);
        if (supplyChannelKey != null) {
            if (supplyChannelKeyToId.containsKey(supplyChannelKey)) {
                return Optional.of(
                    withSupplyChannel(draft, supplyChannelKeyToId.get(supplyChannelKey)));
            } else {
                syncOptions.applyErrorCallback(format(CHANNEL_KEY_MAPPING_DOESNT_EXIST, supplyChannelKey), null);
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
}
