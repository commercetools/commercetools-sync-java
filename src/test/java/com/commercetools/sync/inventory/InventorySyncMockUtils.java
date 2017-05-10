package com.commercetools.sync.inventory;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;
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
        when(inventoryService.createInventoryEntry(any())).thenReturn(completedFuture(createdInventoryEntry));
        when(inventoryService.updateInventoryEntry(any(), any())).thenReturn(completedFuture(updatedInventoryEntry));
        return inventoryService;
    }

    /**
     *
     * @return mock of {@link InventoryService} that throws {@link RuntimeException} on creating and updating calls
     */
    public static InventoryService getMockThrowingInventoryService(final List<Channel> supplyChannels,
                                                             final List<InventoryEntry> inventoryEntries) {
        final CompletableFuture<InventoryEntry> exceptionallyStage = new CompletableFuture<>();
        exceptionallyStage.completeExceptionally(new RuntimeException());
        final InventoryService inventoryService = getMockInventoryService(supplyChannels, inventoryEntries,
                null, null, null);
        when(inventoryService.createSupplyChannel(any())).thenThrow(new RuntimeException());
        when(inventoryService.createInventoryEntry(any())).thenReturn(exceptionallyStage);
        when(inventoryService.updateInventoryEntry(any(), any())).thenReturn(exceptionallyStage);
        return inventoryService;
    }
}
