package com.commercetools.sync.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.Map;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Helper builder class for testing purpose. Allow to build mocks of {@link InventoryEntry}.
 * Object are built using {@link org.mockito.Mockito#mock(Class)}. The getters methods of mocked object
 * return values passed to builder.
 */
public class InventoryEntryMock {

    private String sku = null;
    private Long quantityOnStock = 0l;
    private Integer restockableInDays = null;
    private ZonedDateTime expectedDelivery = null;
    private Reference<Channel> supplyChannel = null;
    private CustomFields customFields = null;

    private InventoryEntryMock(String sku, Long quantityOnStock, Integer restockableInDays,
                               ZonedDateTime expectedDelivery) {
        this.sku = sku;
        this.quantityOnStock = quantityOnStock;
        this.restockableInDays = restockableInDays;
        this.expectedDelivery = expectedDelivery;
    }

    public static InventoryEntryMock of(@Nonnull String sku) {
        return new InventoryEntryMock(sku, 0l, null, null);
    }

    public static InventoryEntryMock of(@Nonnull String sku, long quantityOnStock, Integer restockableInDays,
                                        ZonedDateTime expectedDelivery) {
        return new InventoryEntryMock(sku, quantityOnStock, restockableInDays, expectedDelivery);
    }

    public InventoryEntryMock withQuantity(long quantityOnStock) {
        this.quantityOnStock = quantityOnStock;
        return this;
    }

    public InventoryEntryMock withRestockabe(Integer restockableInDays) {
        this.restockableInDays = restockableInDays;
        return this;
    }

    public InventoryEntryMock withDelivery(ZonedDateTime expectedDelivery) {
        this.expectedDelivery = expectedDelivery;
        return this;
    }

    public InventoryEntryMock withChannelRef(String supplyChannelId) {
        this.supplyChannel = Channel.referenceOfId(supplyChannelId);
        return this;
    }

    /**
     * Adds supply channel reference of {@code supplyChannelId} and {@code supplyChannelKey}.
     * Reference is real object of type {@link Reference}, but it holds mocked {@link Channel} with stubbed methods:
     * {@link Channel#getId()}, {@link Channel#getKey()} and {@link Channel#getRoles()}.
     */
    public InventoryEntryMock withChannelRefExpanded(String supplyChannelId, String supplyChannelKey) {
        final Channel channel = mock(Channel.class);
        when(channel.getKey()).thenReturn(supplyChannelKey);
        when(channel.getId()).thenReturn(supplyChannelId);
        when(channel.getRoles()).thenReturn(singleton(ChannelRole.INVENTORY_SUPPLY));
        this.supplyChannel = Channel.referenceOfId(supplyChannelId).filled(channel);
        return this;
    }

    /**
     * Adds mock of {@link CustomFields}.
     * Mock has stubbed {@link CustomFields#getType()} to return {@link Reference} object to {@code typeId}.
     * It has also stubbed {@link CustomFields#getFieldsJsonMap()} to return {@link Map} with one entry
     * of {@code field} to mocked {@link JsonNode}.
     */
    public InventoryEntryMock withCustomField(String typeId, String field) {
        final CustomFields customFields = mock(CustomFields.class);
        final Map<String, JsonNode> fields = singletonMap(field, mock(JsonNode.class));
        when(customFields.getType()).thenReturn(Type.referenceOfId(typeId));
        when(customFields.getFieldsJsonMap()).thenReturn(fields);
        this.customFields = customFields;
        return this;
    }

    /**
     * Works same as {@link InventoryEntryMock#withCustomField(String, String)} except for the fact, that type's
     * {@link Reference} also returns mocked {@link Type} object with stubbed methods: {@link Type#getId()}
     * and {@link Type#getKey()}
     */
    public InventoryEntryMock withCustomFieldExpanded(String typeId, String typeKey, String field) {
        final CustomFields customFields = mock(CustomFields.class);
        final Type type = mock(Type.class);
        when(type.getKey()).thenReturn(typeKey);
        when(type.getId()).thenReturn(typeId);
        when(customFields.getFieldsJsonMap()).thenReturn(singletonMap(field, mock(JsonNode.class)));
        when(customFields.getType()).thenReturn(Type.referenceOfId(typeId).filled(type));
        this.customFields = customFields;
        return this;
    }

    public InventoryEntry build() {
        final InventoryEntry inventoryEntry = mock(InventoryEntry.class);
        when(inventoryEntry.getSupplyChannel()).thenReturn(supplyChannel);
        when(inventoryEntry.getExpectedDelivery()).thenReturn(expectedDelivery);
        when(inventoryEntry.getRestockableInDays()).thenReturn(restockableInDays);
        when(inventoryEntry.getQuantityOnStock()).thenReturn(quantityOnStock);
        when(inventoryEntry.getCustom()).thenReturn(customFields);
        when(inventoryEntry.getSku()).thenReturn(sku);
        return inventoryEntry;
    }
}
