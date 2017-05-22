package com.commercetools.sync.inventories;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * Provides CRUD operations upon {@link InventoryEntry}.
 */
interface InventoryService {

    /**
     * Queries existing {@link InventoryEntry}'s against set of skus.
     * Returned entries have an expanded reference to the {@code supplyChannel}.
     *
     * @param skus {@link Set} of sku values, used in search predicate
     * @return {@link List} of matching entries or empty list when there was no entry of sku matching to {@code skus}.
     */
    @Nonnull
    CompletionStage<List<InventoryEntry>> fetchInventoryEntriesBySkus(@Nonnull final Set<String> skus);

    /**
     * Fetches all {@link Channel} that contain role {@code "InventorySupply"}.
     *
     * @return {@link List} of matching channels or empty list when no supply channel was found.
     */
    @Nonnull
    CompletionStage<List<Channel>> fetchAllSupplyChannels();

    /**
     * Creates new supply channel of role {@code "InventorySupply"} and {@code key}.
     *
     * @param key key of supply channel
     * @return created {@link Channel}
     */
    @Nonnull
    CompletionStage<Channel> createSupplyChannel(@Nonnull final String key);

    /**
     * Creates new inventory entry from {@code inventoryEntryDraft}.
     *
     * @param inventoryEntryDraft draft with data for new inventory entry
     * @return {@link CompletionStage} with created {@link InventoryEntry} or an exception
     */
    @Nonnull
    CompletionStage<InventoryEntry> createInventoryEntry(@Nonnull final InventoryEntryDraft inventoryEntryDraft);

    /**
     * Updates existing inventory entry with {@code updateActions}.
     *
     * @param inventoryEntry entry that should be updated
     * @param updateActions {@link List} of actions that should be applied to {@code inventoryEntry}
     * @return {@link CompletionStage} with updated {@link InventoryEntry} or an exception
     */
    @Nonnull
    CompletionStage<InventoryEntry> updateInventoryEntry(@Nonnull final InventoryEntry inventoryEntry,
                                                         @Nonnull final List<UpdateAction<InventoryEntry>>
                                                             updateActions);
}
