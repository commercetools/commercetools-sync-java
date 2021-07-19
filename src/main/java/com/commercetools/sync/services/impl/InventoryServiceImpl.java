package com.commercetools.sync.services.impl;

import static com.commercetools.sync.inventories.helpers.InventoryEntryQueryBuilder.buildQueries;

import com.commercetools.sync.commons.utils.ChunkUtils;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.helpers.InventoryEntryIdentifier;
import com.commercetools.sync.services.InventoryService;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.inventory.commands.InventoryEntryUpdateCommand;
import io.sphere.sdk.inventory.expansion.InventoryEntryExpansionModel;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.inventory.queries.InventoryEntryQueryModel;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public final class InventoryServiceImpl
    extends BaseService<
        InventoryEntryDraft,
        InventoryEntry,
        InventoryEntry,
        InventorySyncOptions,
        InventoryEntryQuery,
        InventoryEntryQueryModel,
        InventoryEntryExpansionModel<InventoryEntry>>
    implements InventoryService {

  public InventoryServiceImpl(@Nonnull final InventorySyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Set<InventoryEntry>> fetchInventoryEntriesByIdentifiers(
      @Nonnull final Set<InventoryEntryIdentifier> identifiers) {

    return ChunkUtils.executeChunks(syncOptions.getCtpClient(), buildQueries(identifiers))
        .thenApply(ChunkUtils::flattenPagedQueryResults)
        .thenApply(this::cacheAndMapToSet);
  }

  private HashSet<InventoryEntry> cacheAndMapToSet(@Nonnull final List<InventoryEntry> results) {
    results.forEach(
        resource ->
            keyToIdCache.put(
                String.valueOf(InventoryEntryIdentifier.of(resource)), resource.getId()));
    return new HashSet<>(results);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<InventoryEntry>> createInventoryEntry(
      @Nonnull final InventoryEntryDraft inventoryEntryDraft) {

    return createResource(
        inventoryEntryDraft,
        draft -> String.valueOf(InventoryEntryIdentifier.of(draft)),
        InventoryEntryCreateCommand::of);
  }

  @Nonnull
  @Override
  public CompletionStage<InventoryEntry> updateInventoryEntry(
      @Nonnull final InventoryEntry inventoryEntry,
      @Nonnull final List<UpdateAction<InventoryEntry>> updateActions) {

    return updateResource(inventoryEntry, InventoryEntryUpdateCommand::of, updateActions);
  }
}
