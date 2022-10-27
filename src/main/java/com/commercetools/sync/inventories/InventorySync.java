package com.commercetools.sync.inventories;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static com.commercetools.sync.inventories.utils.InventorySyncUtils.buildActions;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.utils.CompletableFutureUtils;
import com.commercetools.sync.inventories.helpers.InventoryBatchValidator;
import com.commercetools.sync.inventories.helpers.InventoryEntryIdentifier;
import com.commercetools.sync.inventories.helpers.InventoryReferenceResolver;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.InventoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.ChannelServiceImpl;
import com.commercetools.sync.services.impl.InventoryServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.ResourceIdentifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.ImmutablePair;

/** Default implementation of inventories sync process. */
public final class InventorySync
    extends BaseSync<
        InventoryEntryDraft, InventoryEntry, InventorySyncStatistics, InventorySyncOptions> {

  private static final String CTP_INVENTORY_FETCH_FAILED =
      "Failed to fetch existing inventory entries of SKUs %s.";
  private static final String CTP_INVENTORY_ENTRY_UPDATE_FAILED =
      "Failed to update inventory entry of SKU '%s' and " + "supply channel id '%s'.";
  private static final String FAILED_TO_PROCESS =
      "Failed to process the InventoryEntryDraft with SKU:'%s'. " + "Reason: %s";

  private final InventoryService inventoryService;
  private final InventoryReferenceResolver referenceResolver;
  private final InventoryBatchValidator batchValidator;

  /**
   * Takes a {@link InventorySyncOptions} instance to instantiate a new {@link InventorySync}
   * instance that could be used to sync inventory drafts with the given inventory entries in the
   * CTP project specified in the injected {@link InventorySyncOptions} instance.
   *
   * @param syncOptions the container of all the options of the sync process including the CTP
   *     project client and/or configuration and other sync-specific options.
   */
  public InventorySync(@Nonnull final InventorySyncOptions syncOptions) {
    this(
        syncOptions,
        new InventoryServiceImpl(syncOptions),
        new ChannelServiceImpl(syncOptions, Collections.singleton(ChannelRole.INVENTORY_SUPPLY)),
        new TypeServiceImpl(syncOptions));
  }

  InventorySync(
      @Nonnull final InventorySyncOptions syncOptions,
      @Nonnull final InventoryService inventoryService,
      @Nonnull final ChannelService channelService,
      @Nonnull final TypeService typeService) {
    super(new InventorySyncStatistics(), syncOptions);
    this.inventoryService = inventoryService;
    this.referenceResolver =
        new InventoryReferenceResolver(getSyncOptions(), typeService, channelService);
    this.batchValidator = new InventoryBatchValidator(getSyncOptions(), getStatistics());
  }

  /**
   * Iterates through the whole {@code inventories} list and accumulates its valid drafts to
   * batches. Every batch is then processed by {@link InventorySync#processBatch(List)}.
   *
   * <p><strong>Inherited doc:</strong> {@inheritDoc}
   *
   * @param inventoryEntryDrafts {@link List} of {@link InventoryEntryDraft} resources that would be
   *     synced into CTP project.
   * @return {@link CompletionStage} with {@link InventorySyncStatistics} holding statistics of all
   *     sync processes performed by this sync instance
   */
  @Nonnull
  @Override
  protected CompletionStage<InventorySyncStatistics> process(
      @Nonnull final List<InventoryEntryDraft> inventoryEntryDrafts) {

    final List<List<InventoryEntryDraft>> batches =
        batchElements(inventoryEntryDrafts, syncOptions.getBatchSize());
    return syncBatches(batches, CompletableFuture.completedFuture(statistics));
  }

  /**
   * Fetches existing {@link InventoryEntry} objects from CTP project that correspond to passed
   * {@code batchOfDrafts}. Having existing inventory entries fetched, {@code batchOfDrafts} is
   * compared and synced with fetched objects by {@link InventorySync#syncBatch(Set, Set)} function.
   * When fetching existing inventory entries results in an empty optional then {@code
   * batchOfDrafts} isn't processed.
   *
   * @param batch batch of drafts that need to be synced
   * @return {@link CompletionStage} of {@link Void} that indicates method progress.
   */
  protected CompletionStage<InventorySyncStatistics> processBatch(
      @Nonnull final List<InventoryEntryDraft> batch) {

    final ImmutablePair<Set<InventoryEntryDraft>, InventoryBatchValidator.ReferencedKeys> result =
        batchValidator.validateAndCollectReferencedKeys(batch);

    final Set<InventoryEntryDraft> validDrafts = result.getLeft();
    if (validDrafts.isEmpty()) {
      statistics.incrementProcessed(batch.size());
      return CompletableFuture.completedFuture(statistics);
    }

    return referenceResolver
        .populateKeyToIdCachesForReferencedKeys(result.getRight())
        .handle(ImmutablePair::new)
        .thenCompose(
            cachingResponse -> {
              final Throwable cachingException = cachingResponse.getValue();
              if (cachingException != null) {
                handleError(
                    "Failed to build a cache of keys to ids.",
                    cachingException,
                    null,
                    null,
                    null,
                    validDrafts.size());
                return CompletableFuture.completedFuture(null);
              }

              return resolveReferences(validDrafts).thenCompose(this::syncResolvedDrafts);
            })
        .thenApply(
            ignored -> {
              statistics.incrementProcessed(batch.size());
              return statistics;
            });
  }

  private CompletableFuture<Set<InventoryEntryDraft>> resolveReferences(
      @Nonnull final Set<InventoryEntryDraft> inventoryEntryDrafts) {

    final List<CompletionStage<InventoryEntryDraft>> collect =
        inventoryEntryDrafts.stream().map(this::resolveReferences).collect(toList());

    return CompletableFutureUtils.collectionOfFuturesToFutureOfCollection(collect, toSet());
  }

  private CompletionStage<InventoryEntryDraft> resolveReferences(
      @Nonnull final InventoryEntryDraft newInventoryEntry) {
    return referenceResolver
        .resolveReferences(newInventoryEntry)
        .exceptionally(
            completionException -> {
              handleFailedToProcessError(newInventoryEntry, completionException);
              return null;
            });
  }

  private void handleFailedToProcessError(
      @Nonnull final InventoryEntryDraft newInventoryEntry,
      @Nonnull final Throwable completionException) {
    final String errorMessage =
        format(FAILED_TO_PROCESS, newInventoryEntry.getSku(), completionException.getMessage());
    handleError(errorMessage, completionException, null, null, null, 1);
  }

  private CompletionStage<Void> syncResolvedDrafts(
      @Nonnull final Set<InventoryEntryDraft> resolvedDrafts) {

    final Set<InventoryEntryIdentifier> identifiers =
        resolvedDrafts.stream().map(InventoryEntryIdentifier::of).collect(Collectors.toSet());

    return inventoryService
        .fetchInventoryEntriesByIdentifiers(identifiers)
        .handle(ImmutablePair::new)
        .thenCompose(
            fetchResponse -> processFetchedInventories(resolvedDrafts, identifiers, fetchResponse));
  }

  private CompletionStage<Void> processFetchedInventories(
      @Nonnull final Set<InventoryEntryDraft> resolvedDrafts,
      @Nonnull final Set<InventoryEntryIdentifier> identifiers,
      @Nonnull final ImmutablePair<Set<InventoryEntry>, Throwable> fetchResponse) {
    final Set<InventoryEntry> fetchedInventoryEntries = fetchResponse.getKey();
    final Throwable exception = fetchResponse.getValue();

    if (exception != null) {
      final String errorMessage = format(CTP_INVENTORY_FETCH_FAILED, identifiers);
      handleError(errorMessage, exception, null, null, null, identifiers.size());
      return CompletableFuture.completedFuture(null);
    } else {
      return syncBatch(fetchedInventoryEntries, resolvedDrafts);
    }
  }

  private CompletionStage<Void> syncBatch(
      @Nonnull final Set<InventoryEntry> oldInventories,
      @Nonnull final Set<InventoryEntryDraft> inventoryEntryDrafts) {

    final Map<InventoryEntryIdentifier, InventoryEntry> oldInventoryMap =
        oldInventories.stream().collect(toMap(InventoryEntryIdentifier::of, identity()));

    return CompletableFuture.allOf(
        inventoryEntryDrafts.stream()
            .map(newInventoryEntry -> syncDraft(oldInventoryMap, newInventoryEntry))
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
  }

  /**
   * Checks if the {@code resolvedDraft} matches with an old existing inventory entry. If it does,
   * it tries to update it. If it doesn't, it creates it.
   *
   * @param oldInventories map of {@link InventoryEntryIdentifier} to old {@link InventoryEntry}
   *     instances
   * @param resolvedDraft inventory entry draft which has its references resolved
   * @return a future which contains an empty result after execution of the update
   */
  private CompletionStage<Optional<InventoryEntry>> syncDraft(
      @Nonnull final Map<InventoryEntryIdentifier, InventoryEntry> oldInventories,
      @Nonnull final InventoryEntryDraft resolvedDraft) {

    final InventoryEntry oldInventory =
        oldInventories.get(InventoryEntryIdentifier.of(resolvedDraft));

    return ofNullable(oldInventory)
        .map(type -> buildActionsAndUpdate(oldInventory, resolvedDraft))
        .orElseGet(() -> applyCallbackAndCreate(resolvedDraft))
        .exceptionally(
            completionException -> {
              handleFailedToProcessError(resolvedDraft, completionException);
              return Optional.empty();
            });
  }

  /**
   * Given an existing {@link InventoryEntry} and a new {@link InventoryEntryDraft}, the method
   * calculates all the update actions required to synchronize the existing entry to be the same as
   * the new one. If there are update actions found, a request is made to CTP to update the existing
   * entry, otherwise it doesn't issue a request.
   *
   * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was
   * carried out successfully or not. If an exception was thrown on executing the request to CTP,
   * the error handling method is called.
   *
   * @param entry existing inventory entry that could be updated.
   * @param draft draft containing data that could differ from data in {@code entry}. <strong>Sku
   *     isn't compared</strong>
   * @return a future which contains an empty result after execution of the update.
   */
  private CompletionStage<Optional<InventoryEntry>> buildActionsAndUpdate(
      @Nonnull final InventoryEntry entry, @Nonnull final InventoryEntryDraft draft) {

    final List<UpdateAction<InventoryEntry>> updateActions =
        buildActions(entry, draft, syncOptions);

    final List<UpdateAction<InventoryEntry>> beforeUpdateCallBackApplied =
        syncOptions.applyBeforeUpdateCallback(updateActions, draft, entry);

    if (!beforeUpdateCallBackApplied.isEmpty()) {
      return inventoryService
          .updateInventoryEntry(entry, beforeUpdateCallBackApplied)
          .handle(ImmutablePair::new)
          .thenCompose(
              updateResponse -> {
                final InventoryEntry updatedInventoryEntry = updateResponse.getKey();
                final Throwable sphereException = updateResponse.getValue();
                if (sphereException != null) {
                  final ResourceIdentifier<Channel> supplyChannel = draft.getSupplyChannel();
                  final String errorMessage =
                      format(
                          CTP_INVENTORY_ENTRY_UPDATE_FAILED,
                          draft.getSku(),
                          supplyChannel != null ? supplyChannel.getId() : null);
                  handleError(errorMessage, sphereException, entry, draft, updateActions, 1);
                  return CompletableFuture.completedFuture(Optional.empty());
                } else {
                  statistics.incrementUpdated();
                  return CompletableFuture.completedFuture(Optional.of(updatedInventoryEntry));
                }
              });
    }
    return completedFuture(null);
  }

  /**
   * Given an inventory entry {@code draft}, issues a request to the CTP project to create a
   * corresponding Inventory Entry.
   *
   * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was
   * carried out successfully or not. If an exception was thrown on executing the request to CTP,
   * the error handling method is called.
   *
   * @param inventoryEntryDraft the inventory entry draft to create the inventory entry from.
   * @return a future which contains an empty result after execution of the create.
   */
  private CompletionStage<Optional<InventoryEntry>> applyCallbackAndCreate(
      @Nonnull final InventoryEntryDraft inventoryEntryDraft) {

    return syncOptions
        .applyBeforeCreateCallback(inventoryEntryDraft)
        .map(
            draft ->
                inventoryService
                    .createInventoryEntry(draft)
                    .thenApply(
                        inventoryEntryOptional -> {
                          if (inventoryEntryOptional.isPresent()) {
                            statistics.incrementCreated();
                          } else {
                            statistics.incrementFailed();
                          }
                          return inventoryEntryOptional;
                        }))
        .orElse(CompletableFuture.completedFuture(Optional.empty()));
  }
}
