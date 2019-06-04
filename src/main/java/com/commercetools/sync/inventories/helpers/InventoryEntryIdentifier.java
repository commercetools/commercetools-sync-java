package com.commercetools.sync.inventories.helpers;


import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * This class provides as a container of the unique identifier of an {@link InventoryEntry} for the sync which is a
 * combination of both the SKU of the inventory entry and the supply channel if of this inventory entry.
 */
public final class InventoryEntryIdentifier {
    private String inventoryEntrySku;
    private String inventoryEntryChannelId;

    private InventoryEntryIdentifier(@Nonnull final String inventoryEntrySku,
                                     @Nullable final String inventoryEntryChannelId) {
        this.inventoryEntrySku = inventoryEntrySku;
        this.inventoryEntryChannelId = inventoryEntryChannelId;
    }

    /**
     * Builds an {@link InventoryEntryIdentifier} instance given an {@link InventoryEntryDraft} using it's sku and
     * supply channel id.
     * @param inventoryEntryDraft the draft to take the sku and channel id value from.
     * @return an instance of {@link InventoryEntryIdentifier} for the given draft.
     */
    public static InventoryEntryIdentifier of(@Nonnull final InventoryEntryDraft inventoryEntryDraft) {
        final ResourceIdentifier<Channel> supplyChannelIdentifer= inventoryEntryDraft.getSupplyChannel();
        return new InventoryEntryIdentifier(inventoryEntryDraft.getSku(),
            supplyChannel != null ? supplyChannel.getId() : null);
    }

    /**
     * Builds an {@link InventoryEntryIdentifier} instance given an {@link InventoryEntry} using it's sku and
     * supply channel id.
     * @param inventoryEntry the entry to take the sku and channel id value from.
     * @return an instance of {@link InventoryEntryIdentifier} for the given entry.
     */
    public static InventoryEntryIdentifier of(@Nonnull final InventoryEntry inventoryEntry) {
        final Reference<Channel> supplyChannel = inventoryEntry.getSupplyChannel();
        return new InventoryEntryIdentifier(inventoryEntry.getSku(),
            supplyChannel != null ? supplyChannel.getId() : null);
    }

    /**
     * Builds an {@link InventoryEntryIdentifier} instance given an sku and supply channel id.
     *
     * @param inventoryEntrySku the SKU of the inventory entry.
     * @param inventoryEntryChannelId the channel id of the inventory entry.
     * @return an instance of {@link InventoryEntryIdentifier} for the given entry.
     */
    public static InventoryEntryIdentifier of(@Nonnull final String inventoryEntrySku,
                                              @Nullable final String inventoryEntryChannelId) {
        return new InventoryEntryIdentifier(inventoryEntrySku, inventoryEntryChannelId);
    }

    public String getInventoryEntryChannelId() {
        return inventoryEntryChannelId;
    }

    public String getInventoryEntrySku() {
        return inventoryEntrySku;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof  InventoryEntryIdentifier) {
            final InventoryEntryIdentifier that = (InventoryEntryIdentifier) obj;
            return Objects.equals(that.getInventoryEntryChannelId(), inventoryEntryChannelId)
                && Objects.equals(that.getInventoryEntrySku(), inventoryEntrySku);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inventoryEntrySku, inventoryEntryChannelId);
    }
}
