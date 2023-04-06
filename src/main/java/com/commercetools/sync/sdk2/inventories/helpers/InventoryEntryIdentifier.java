package com.commercetools.sync.sdk2.inventories.helpers;

import static java.lang.String.format;

import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.channel.ChannelResourceIdentifier;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class provides as a container of the unique identifier of an {@link
 * io.sphere.sdk.inventory.InventoryEntry} for the sync which is a combination of the SKU and the
 * supply channel id of this inventory entry.
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
   * Builds an {@link com.commercetools.sync.sdk2.inventories.helpers.InventoryEntryIdentifier}
   * instance given an {@link io.sphere.sdk.inventory.InventoryEntryDraft} using its sku and supply
   * channel id.
   *
   * @param inventoryEntryDraft the draft to take the sku and supply channel id value from.
   * @return an instance of {@link
   *     com.commercetools.sync.sdk2.inventories.helpers.InventoryEntryIdentifier} for the given
   *     draft.
   */
  public static InventoryEntryIdentifier of(
      @Nonnull final InventoryEntryDraft inventoryEntryDraft) {

    final ChannelResourceIdentifier supplyChannelIdentifier =
        inventoryEntryDraft.getSupplyChannel();
    return new InventoryEntryIdentifier(
        inventoryEntryDraft.getSku(),
        supplyChannelIdentifier != null ? supplyChannelIdentifier.getId() : null);
  }

  /**
   * Builds an {@link com.commercetools.sync.sdk2.inventories.helpers.InventoryEntryIdentifier}
   * instance given an {@link io.sphere.sdk.inventory.InventoryEntry} using it's sku and supply
   * channel id.
   *
   * @param inventoryEntry the entry to take the sku and channel id value from.
   * @return an instance of {@link
   *     com.commercetools.sync.sdk2.inventories.helpers.InventoryEntryIdentifier} for the given
   *     entry.
   */
  public static InventoryEntryIdentifier of(@Nonnull final InventoryEntry inventoryEntry) {

    final ChannelReference supplyChannel = inventoryEntry.getSupplyChannel();
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
