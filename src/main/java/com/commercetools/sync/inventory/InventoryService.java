package com.commercetools.sync.inventory;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

/**
 * Interface contains methods to perform sphere queries/commands related to Inventory topic.
 * Listed operations are used in {@link InventorySync}.
 */
interface InventoryService {

    /**
     * Fetches all {@link InventoryEntry} objects whose sku is included in {@code skus}.
     * Returned entries have an expanded reference to the {@code supplyChannel}.
     *
     * @param skus {@link Set} of sku values, used in search predicate
     * @return {@link List} of matching entries
     */
    List<InventoryEntry> fetchInventoryEntriesBySkus(@Nonnull final Set<String> skus);

    /**
     * Fetches all {@link Channel} that contain role {@code "InventorySupply"}.
     *
     * @return {@link List} of matching channels
     */
    List<Channel> fetchAllSupplyChannels();

    /**
     * Creates new supply channel of role {@code "InventorySupply"} and {@code key}.
     *
     * @param key key of supply channel
     * @return created {@link Channel}
     */
    Channel createSupplyChannel(String key);

    /**
     * Creates new inventory entry from {@code inventoryEntryDraft}.
     *
     * @param inventoryEntryDraft draft with data for new inventory entry
     * @return created {@link InventoryEntry}
     */
    InventoryEntry createInventoryEntry(@Nonnull final InventoryEntryDraft inventoryEntryDraft);

    /**
     * Updates existing inventory entry with {@code updateActions}.
     *
     * @param inventoryEntry entry that should be updated
     * @param updateActions {@link List} of actions that should be applied to {@code inventoryEntry}
     * @return updated {@link InventoryEntry}
     */
    InventoryEntry updateInventoryEntry(@Nonnull final InventoryEntry inventoryEntry,
                                        @Nonnull final List<UpdateAction<InventoryEntry>> updateActions);
}
