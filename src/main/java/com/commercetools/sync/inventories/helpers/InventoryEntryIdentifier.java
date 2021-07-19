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
 * sync which is a combination of the SKU and the supply channel id of this inventory entry.
 */
public final class InventoryEntryIdentifier {
  private final String sku;
  private final String supplyChannelId;

  private InventoryEntryIdentifier(
      @Nonnull final String sku, @Nullable final String supplyChannelId) {
    this.sku = sku;
    this.supplyChannelId = supplyChannelId;
  }

  /**
   * Builds an {@link InventoryEntryIdentifier} instance given an {@link InventoryEntryDraft} using
   * its sku and supply channel id.
   *
   * @param inventoryEntryDraft the draft to take the sku and supply channel id value from.
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

  public String getSku() {
    return sku;
  }

  public String getSupplyChannelId() {
    return supplyChannelId;
  }

  @Override
  public String toString() {
    return format("{sku='%s', supplyChannelId='%s'}", sku, supplyChannelId);
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

    return getSku().equals(that.getSku())
        && Objects.equals(getSupplyChannelId(), that.getSupplyChannelId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getSku(), getSupplyChannelId());
  }
}
