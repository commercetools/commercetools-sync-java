package com.commercetools.sync.inventories;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraftBuilder;
import io.sphere.sdk.types.Type;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.singleton;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InventorySyncMockUtils {

    public static Channel getMockSupplyChannel(String id, String key) {
        final Channel channel = mock(Channel.class);
        when(channel.getId()).thenReturn(id);
        when(channel.getKey()).thenReturn(key);
        when(channel.getRoles()).thenReturn(singleton(ChannelRole.INVENTORY_SUPPLY));
        return channel;
    }

    public static InventoryEntry getMockInventoryEntry(final String sku,
                                                       final Long quantityOnStock,
                                                       final Integer restockableInDays,
                                                       final ZonedDateTime expectedDelivery,
                                                       final Reference<Channel> supplyChannel,
                                                       final CustomFields customFields) {
        final InventoryEntry inventoryEntry = mock(InventoryEntry.class);
        when(inventoryEntry.getSku()).thenReturn(sku);
        when(inventoryEntry.getQuantityOnStock()).thenReturn(quantityOnStock);
        when(inventoryEntry.getRestockableInDays()).thenReturn(restockableInDays);
        when(inventoryEntry.getExpectedDelivery()).thenReturn(expectedDelivery);
        when(inventoryEntry.getSupplyChannel()).thenReturn(supplyChannel);
        when(inventoryEntry.getCustom()).thenReturn(customFields);
        return inventoryEntry;
    }

    public static CustomFields getMockCustomFields(final String typeId, final String typeKey, final String fieldName,
                                                   final Object fieldValue) {
        final CustomFields customFields = mock(CustomFields.class);
        final Type type = mock(Type.class);
        when(type.getKey()).thenReturn(typeKey);
        when(type.getId()).thenReturn(typeId);
        when(customFields.getFieldsJsonMap()).thenReturn(mockFields(fieldName, fieldValue));
        when(customFields.getType()).thenReturn(Type.referenceOfId(typeId).filled(type));
        return customFields;
    }

    private static Map<String, JsonNode> mockFields(String name, Object obj) {
        return CustomFieldsDraftBuilder.ofTypeKey("123")
                .addObject(name, obj)
                .build()
                .getFields();
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
     * @return mock of {@link InventoryService} that throws {@link RuntimeException} on blocking creating and updating
     * calls, and returns future monad with {@link RuntimeException} on non blocking creating and updating calls.
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
