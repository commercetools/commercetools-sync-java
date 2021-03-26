package com.commercetools.sync.inventories.utils;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKey;
import static java.util.stream.Collectors.toList;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.types.Type;
import java.util.List;
import java.util.Map;
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
   * <table summary="Mapping of Reference fields for the reference resolution">
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
   * @param referenceIdToKeyMap the map containing the cached id to key values.
   * @return a {@link List} of {@link InventoryEntryDraft} built from the supplied {@link List} of
   *     {@link InventoryEntry}.
   */
  @Nonnull
  public static List<InventoryEntryDraft> mapToInventoryEntryDrafts(
      @Nonnull final List<InventoryEntry> inventoryEntries,
      @Nonnull final Map<String, String> referenceIdToKeyMap) {

    return inventoryEntries.stream()
        .map(inventoryEntry -> mapToInventoryEntryDraft(inventoryEntry, referenceIdToKeyMap))
        .collect(toList());
  }

  @Nonnull
  private static InventoryEntryDraft mapToInventoryEntryDraft(
      @Nonnull final InventoryEntry inventoryEntry,
      @Nonnull final Map<String, String> referenceIdToKeyMap) {
    return InventoryEntryDraftBuilder.of(inventoryEntry)
        .custom(mapToCustomFieldsDraft(inventoryEntry, referenceIdToKeyMap))
        .supplyChannel(
            getResourceIdentifierWithKey(inventoryEntry.getSupplyChannel(), referenceIdToKeyMap))
        .build();
  }

  /**
   * Builds a {@link InventoryEntryQuery} for fetching inventories from a source CTP project.
   *
   * @return the query for fetching inventories from the source CTP project without any references
   *     expanded.
   */
  public static InventoryEntryQuery buildInventoryQuery() {
    return InventoryEntryQuery.of().withLimit(QueryExecutionUtils.DEFAULT_PAGE_SIZE);
  }

  private InventoryReferenceResolutionUtils() {}
}
