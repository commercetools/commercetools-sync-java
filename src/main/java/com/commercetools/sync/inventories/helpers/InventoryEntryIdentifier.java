package com.commercetools.sync.inventories.helpers;

import static java.lang.String.format;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class provides as a container of the unique identifier of an {@link InventoryEntry} for the
 * sync which is a combination of both the SKU of the inventory entry and the supply channel key of
 * this inventory entry.
 */
public final class InventoryEntryIdentifier {
  private String inventoryEntrySku;
  private String inventoryEntryChannelKey;

  private InventoryEntryIdentifier(
      @Nonnull final String inventoryEntrySku, @Nullable final String inventoryEntryChannelKey) {
    this.inventoryEntrySku = inventoryEntrySku;
    this.inventoryEntryChannelKey = inventoryEntryChannelKey;
  }

  /**
   * Builds an {@link InventoryEntryIdentifier} instance given an {@link InventoryEntryDraft} using
   * its sku and supply channel key.
   *
   * @param inventoryEntryDraft the draft to take the sku and channel key value from.
   * @return an instance of {@link InventoryEntryIdentifier} for the given draft.
   */
  public static InventoryEntryIdentifier of(
      @Nonnull final InventoryEntryDraft inventoryEntryDraft) {

    final ResourceIdentifier<Channel> supplyChannelIdentifier =
        inventoryEntryDraft.getSupplyChannel();
    return new InventoryEntryIdentifier(
        inventoryEntryDraft.getSku(),
        supplyChannelIdentifier != null ? supplyChannelIdentifier.getId() : null);
  }

  /**
   * Builds an {@link InventoryEntryIdentifier} instance given an {@link InventoryEntry} using it's
   * sku and supply channel id.
   *
   * @param inventoryEntry the entry to take the sku and channel id value from.
   * @return an instance of {@link InventoryEntryIdentifier} for the given entry.
   */
  public static InventoryEntryIdentifier of(@Nonnull final InventoryEntry inventoryEntry) {

    final Reference<Channel> supplyChannel = inventoryEntry.getSupplyChannel();
    return new InventoryEntryIdentifier(
        inventoryEntry.getSku(), supplyChannel != null ? supplyChannel.getId() : null);
  }

  /**
   * Builds an {@link InventoryEntryIdentifier} instance given an sku and supply channel id.
   *
   * @param inventoryEntrySku the SKU of the inventory entry.
   * @param inventoryEntryChannelKey the channel key of the inventory entry.
   * @return an instance of {@link InventoryEntryIdentifier} for the given entry.
   */
  public static InventoryEntryIdentifier of(
      @Nonnull final String inventoryEntrySku, @Nullable final String inventoryEntryChannelKey) {

    return new InventoryEntryIdentifier(inventoryEntrySku, inventoryEntryChannelKey);
  }

  public String getInventoryEntrySku() {
    return inventoryEntrySku;
  }

  public String getInventoryEntryChannelKey() {
    return inventoryEntryChannelKey;
  }

  @Override
  public String toString() {
    return format("{sku='%s', channelKey='%s'}", inventoryEntrySku, inventoryEntryChannelKey);
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof InventoryEntryIdentifier)) {
      return false;
    }

    final InventoryEntryIdentifier that = (InventoryEntryIdentifier) other;

    return getInventoryEntrySku().equals(that.getInventoryEntrySku())
        && Objects.equals(getInventoryEntryChannelKey(), that.getInventoryEntryChannelKey());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getInventoryEntrySku(), getInventoryEntryChannelKey());
  }
}
