package com.commercetools.sync.services.impl;

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
import io.sphere.sdk.inventory.queries.InventoryEntryQueryBuilder;
import io.sphere.sdk.inventory.queries.InventoryEntryQueryModel;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public final class InventoryServiceImpl
    extends BaseService<
        InventoryEntryDraft,
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
  public CompletionStage<Set<InventoryEntry>> fetchInventoryEntriesBySkus(
      @Nonnull final Set<String> skus) {

    return fetchMatchingResources(
        skus,
        draft -> String.valueOf(InventoryEntryIdentifier.of(draft)),
        () ->
            InventoryEntryQueryBuilder.of()
                .plusPredicates(queryModel -> queryModel.sku().isIn(skus))
                .build());
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
