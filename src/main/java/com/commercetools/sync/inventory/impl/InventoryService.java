package com.commercetools.sync.inventory.impl;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

interface InventoryService {

    List<InventoryEntry> fetchInventoryEntriesBySkus(@Nonnull final Set<String> skus);

    List<Channel> fetchAllSupplyChannels();

    Channel createSupplyChannel(String key);

    InventoryEntry createInventoryEntry(@Nonnull final InventoryEntryDraft inventoryEntryDraft);

    InventoryEntry updateInventoryEntry(@Nonnull final InventoryEntry inventoryEntry,
                                        @Nonnull final List<UpdateAction<InventoryEntry>> updateActions);
}
