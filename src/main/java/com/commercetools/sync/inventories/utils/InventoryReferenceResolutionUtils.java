package com.commercetools.sync.inventories.utils;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKey;
import static java.util.stream.Collectors.toList;

import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.channel.ChannelResourceIdentifier;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.inventory.InventoryEntryDraftBuilder;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeResourceIdentifier;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class InventoryReferenceResolutionUtils {

  /**
   * Returns an {@link List}&lt;{@link InventoryEntryDraft}&gt; consisting of the results of
   * applying the mapping from {@link InventoryEntry} to {@link InventoryEntryDraft} with
   * considering reference resolution.
   *
   * <table>
   *   <caption>Mapping of Reference fields for the reference resolution</caption>
   *   <thead>
   *     <tr>
   *       <th>Reference field</th>
   *       <th>from</th>
   *       <th>to</th>
   *     </tr>
   *   </thead>
   *   <tbody>
   *     <tr>
   *       <td>supplyChannel</td>
   *       <td>{@link ChannelReference}</td>
   *       <td>{@link ChannelResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>custom.type</td>
   *        <td>{@link TypeReference}</td>
   *        <td>{@link TypeResourceIdentifier}</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The {@link Channel} and {@link Type} reference should contain Id in the
   * map(cache) with a key value. Any reference, which have its id in place and not replaced by the
   * key, it would not be found in the map. In this case, this reference will be considered as
   * existing resources on the target commercetools project and the library will issues an
   * update/create API request without reference resolution.
   *
   * @param inventoryEntries the inventory entries without expansion of references.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link List} of {@link InventoryEntryDraft} built from the supplied {@link List} of
   *     {@link InventoryEntry}.
   */
  @Nonnull
  public static List<InventoryEntryDraft> mapToInventoryEntryDrafts(
      @Nonnull final List<InventoryEntry> inventoryEntries,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    return inventoryEntries.stream()
        .map(inventoryEntry -> mapToInventoryEntryDraft(inventoryEntry, referenceIdToKeyCache))
        .collect(toList());
  }

  @Nonnull
  private static InventoryEntryDraft mapToInventoryEntryDraft(
      @Nonnull final InventoryEntry inventoryEntry,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    final ChannelResourceIdentifier channelResourceIdentifier =
        getResourceIdentifierWithKey(
            inventoryEntry.getSupplyChannel(),
            referenceIdToKeyCache,
            (id, key) -> ChannelResourceIdentifierBuilder.of().key(key).id(id).build());
    return InventoryEntryDraftBuilder.of()
        .sku(inventoryEntry.getSku())
        .quantityOnStock(inventoryEntry.getQuantityOnStock())
        .expectedDelivery(inventoryEntry.getExpectedDelivery())
        .restockableInDays(inventoryEntry.getRestockableInDays())
        .key(inventoryEntry.getKey())
        .custom(mapToCustomFieldsDraft(inventoryEntry, referenceIdToKeyCache))
        .supplyChannel(channelResourceIdentifier)
        .build();
  }

  private InventoryReferenceResolutionUtils() {}
}
