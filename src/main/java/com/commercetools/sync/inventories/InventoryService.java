package com.commercetools.sync.inventories;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * Interface contains methods to perform sphere queries/commands related to Inventory topic.
 * Listed operations are used in {@link InventorySync}.
 */
interface InventoryService {

    /**
     * Fetches all {@link InventoryEntry} objects whose sku is included in {@code skus}.
     * For some {@code skus} there could be no matching entries in CT platform, so for such skus no entries would be
     * returned. Because of that result can contain inventory entries of skus, that are only subset of passed
     * {@code skus}, or even in empty list when no matching entry is found.
     * Returned entries have an expanded reference to the {@code supplyChannel}.
     *
     * @param skus {@link Set} of sku values, used in search predicate
     * @return {@link List} of matching entries or empty list when there was no entry of sku matching to {@code skus}.
     */
    @Nonnull
    List<InventoryEntry> fetchInventoryEntriesBySkus(@Nonnull final Set<String> skus);

    /**
     * Fetches all {@link Channel} that contain role {@code "InventorySupply"}.
     *
     * @return {@link List} of matching channels or empty list when no supply channel was found.
     */
    @Nonnull
    List<Channel> fetchAllSupplyChannels();

    /**
     * Creates new supply channel of role {@code "InventorySupply"} and {@code key}.
     *
     * @param key key of supply channel
     * @return created {@link Channel}
     */
    @Nullable
    Channel createSupplyChannel(@Nonnull final String key);

    /**
     * Creates new inventory entry from {@code inventoryEntryDraft}.
     *
     * @param inventoryEntryDraft draft with data for new inventory entry
     * @return {@link CompletionStage} with created {@link InventoryEntry} or an exception
     */
    CompletionStage<InventoryEntry> createInventoryEntry(@Nonnull final InventoryEntryDraft inventoryEntryDraft);

    /**
     * Updates existing inventory entry with {@code updateActions}.
     *
     * @param inventoryEntry entry that should be updated
     * @param updateActions {@link List} of actions that should be applied to {@code inventoryEntry}
     * @return {@link CompletionStage} with updated {@link InventoryEntry} or an exception
     */
    CompletionStage<InventoryEntry> updateInventoryEntry(@Nonnull final InventoryEntry inventoryEntry,
                                                         @Nonnull final List<UpdateAction<InventoryEntry>> updateActions);
}
