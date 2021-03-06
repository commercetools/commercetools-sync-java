package com.commercetools.sync.inventories.utils;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKey;
import static java.util.stream.Collectors.toList;

import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.Type;
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
   *       <td>{@link Reference}&lt;{@link Channel}&gt;</td>
   *       <td>{@link ResourceIdentifier}&lt;{@link Channel}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>custom.type</td>
   *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
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
    return InventoryEntryDraftBuilder.of(inventoryEntry)
        .custom(mapToCustomFieldsDraft(inventoryEntry, referenceIdToKeyCache))
        .supplyChannel(
            getResourceIdentifierWithKey(inventoryEntry.getSupplyChannel(), referenceIdToKeyCache))
        .build();
  }

  private InventoryReferenceResolutionUtils() {}
}
