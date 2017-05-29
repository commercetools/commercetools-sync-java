package com.commercetools.sync.inventories;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraftBuilder;
import io.sphere.sdk.types.Type;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.Collections.singleton;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InventorySyncMockUtils {

    /**
     * Returns mock {@link Channel} instance. Returned instance represents channel of passed {@code id}, {@code key}
     * and of role {@link ChannelRole#INVENTORY_SUPPLY}.
     *
     * @param id result of calling {@link Channel#getId()}
     * @param key result of calling {@link Channel#getKey()}
     * @return mock instance of {@link Channel}
     */
    public static Channel getMockSupplyChannel(final String id, final String key) {
        final Channel channel = mock(Channel.class);
        when(channel.getId()).thenReturn(id);
        when(channel.getKey()).thenReturn(key);
        when(channel.getRoles()).thenReturn(singleton(ChannelRole.INVENTORY_SUPPLY));
        return channel;
    }

    /**
     * Returns mock {@link InventoryEntry} instance. Executing getters on returned instance will return values passed
     * in parameters.
     *
     * @param sku result of calling {@link InventoryEntry#getSku()}
     * @param quantityOnStock result of calling {@link InventoryEntry#getQuantityOnStock()}
     * @param restockableInDays result of calling {@link InventoryEntry#getRestockableInDays()}
     * @param expectedDelivery result of calling {@link InventoryEntry#getExpectedDelivery()}
     * @param supplyChannel result of calling {@link InventoryEntry#getSupplyChannel()}
     * @param customFields result of calling {@link InventoryEntry#getCustom()}
     * @return mock instance of {@link InventoryEntry}
     */
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

    /**
     * Returns mock {@link CustomFields} instance. Executing {@link CustomFields#getType()} on returned instance will
     * return {@link Reference} of given {@code typeId} with mock {@link Type} instance of {@code typeId} and {@code
     * typeKey} (getters of key and id would return given values). Executing {@link CustomFields#getFieldsJsonMap()} on
     * returned instance will return {@link Map} populated with given {@code fieldName} and {@code fieldValue}
     *
     * @param typeId custom type id
     * @param fieldName custom field name
     * @param fieldValue custom field value
     * @return mock instance of {@link CustomFields}
     */
    public static CustomFields getMockCustomFields(final String typeId, final String fieldName,
                                                   final Object fieldValue) {
        final CustomFields customFields = mock(CustomFields.class);
        final Type type = mock(Type.class);
        when(type.getId()).thenReturn(typeId);
        when(customFields.getFieldsJsonMap()).thenReturn(mockFields(fieldName, fieldValue));
        when(customFields.getType()).thenReturn(Type.referenceOfId(typeId).filled(type));
        return customFields;
    }

    private static Map<String, JsonNode> mockFields(final String name, final Object obj) {
        return CustomFieldsDraftBuilder.ofTypeKey("123")
                .addObject(name, obj)
                .build()
                .getFields();
    }

    /**
     * Returns mock instance of {@link InventoryService}. Executing any method with any parameter on this instance
     * returns values passed in parameters, wrapped in {@link CompletionStage}.
     *
     * @param supplyChannels result of calling {@link InventoryService#fetchAllSupplyChannels()}
     * @param inventoryEntries result of calling {@link InventoryService#fetchInventoryEntriesBySkus(Set)}
     * @param createdSupplyChannel result of calling {@link InventoryService#createSupplyChannel(String)}
     * @param createdInventoryEntry result of calling {@link InventoryService#createInventoryEntry(InventoryEntryDraft)}
     * @param updatedInventoryEntry result of calling
     *      {@link InventoryService#updateInventoryEntry(InventoryEntry, List)}
     * @return mock instance of {@link InventoryService}
     */
    public static InventoryService getMockInventoryService(final List<Channel> supplyChannels,
                                                           final List<InventoryEntry> inventoryEntries,
                                                           final Channel createdSupplyChannel,
                                                           final InventoryEntry createdInventoryEntry,
                                                           final InventoryEntry updatedInventoryEntry) {
        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.fetchAllSupplyChannels()).thenReturn(completedFuture(supplyChannels));
        when(inventoryService.fetchInventoryEntriesBySkus(any())).thenReturn(completedFuture(inventoryEntries));
        when(inventoryService.createSupplyChannel(any())).thenReturn(completedFuture(createdSupplyChannel));
        when(inventoryService.createInventoryEntry(any())).thenReturn(completedFuture(createdInventoryEntry));
        when(inventoryService.updateInventoryEntry(any(), any())).thenReturn(completedFuture(updatedInventoryEntry));
        return inventoryService;
    }

    /**
     * Returns {@link CompletionStage} completed exceptionally.
     *
     * @param <T> type of result that is supposed to be inside {@link CompletionStage}
     * @return {@link CompletionStage} instance that is completed exceptionally with {@link RuntimeException}
     */
    public static <T> CompletionStage<T> getCompletionStageWithException() {
        final CompletableFuture<T> exceptionalStage = new CompletableFuture<>();
        exceptionalStage.completeExceptionally(new RuntimeException());
        return exceptionalStage;
    }
}
