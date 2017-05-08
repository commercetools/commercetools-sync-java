package com.commercetools.sync.inventory;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;

import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InventorySyncMockUtils {

    public static Channel getMockChannel(String id, String key) {
        Channel channel = mock(Channel.class);
        when(channel.getId()).thenReturn(id);
        when(channel.getKey()).thenReturn(key);
        return channel;
    }

    public static InventoryService getMockInventoryService(final List<Channel> supplyChannels,
                                                           final List<InventoryEntry> inventoryEntries,
                                                           final Channel createdSupplyChannel,
                                                           final InventoryEntry createdInventoryEntry,
                                                           final InventoryEntry updatedInventoryEntry) {
        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.fetchAllSupplyChannels()).thenReturn(supplyChannels);
        when(inventoryService.fetchInventoryEntriesBySkus(any())).thenReturn(inventoryEntries);
        when(inventoryService.createSupplyChannel(any())).thenReturn(createdSupplyChannel);
        when(inventoryService.createInventoryEntry(any())).thenReturn(createdInventoryEntry);
        when(inventoryService.updateInventoryEntry(any(), any())).thenReturn(updatedInventoryEntry);
        return inventoryService;
    }

    /**
     *
     * @return mock of {@link InventoryService} that throws {@link RuntimeException} on creating and updating calls
     */
    public static InventoryService getMockThrowingInventoryService(final List<Channel> supplyChannels,
                                                             final List<InventoryEntry> inventoryEntries) {
        final InventoryService inventoryService = getMockInventoryService(supplyChannels, inventoryEntries,
                null, null, null);
        when(inventoryService.createSupplyChannel(any())).thenThrow(new RuntimeException());
        when(inventoryService.createInventoryEntry(any())).thenThrow(new RuntimeException());
        when(inventoryService.updateInventoryEntry(any(), any())).thenThrow(new RuntimeException());
        return inventoryService;
    }
}
