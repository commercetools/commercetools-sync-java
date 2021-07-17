package com.commercetools.sync.services;

import com.commercetools.sync.inventories.helpers.InventoryEntryIdentifier;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public interface InventoryService {

  /**
   * Queries existing {@link InventoryEntry}'s against set of sku and supply channels.
   *
   * @param inventoryEntryIdentifiers {@link Set} of unique inventory identifiers, used in search
   *     predicate
   * @return {@link List} of matching entries or empty list when there was no matching resources.
   */
  @Nonnull
  CompletionStage<Set<InventoryEntry>> fetchInventoryEntriesByIdentifiers(
      @Nonnull final Set<InventoryEntryIdentifier> inventoryEntryIdentifiers);

  /**
   * Creates new inventory entry from {@code inventoryEntryDraft}.
   *
   * @param inventoryEntryDraft draft with data for new inventory entry
   * @return {@link CompletionStage} with created {@link InventoryEntry} or an exception
   */
  @Nonnull
  CompletionStage<Optional<InventoryEntry>> createInventoryEntry(
      @Nonnull final InventoryEntryDraft inventoryEntryDraft);

  /**
   * Updates existing inventory entry with {@code updateActions}.
   *
   * @param inventoryEntry entry that should be updated
   * @param updateActions {@link List} of actions that should be applied to {@code inventoryEntry}
   * @return {@link CompletionStage} with updated {@link InventoryEntry} or an exception
   */
  @Nonnull
  CompletionStage<InventoryEntry> updateInventoryEntry(
      @Nonnull final InventoryEntry inventoryEntry,
      @Nonnull final List<UpdateAction<InventoryEntry>> updateActions);
}
