package com.commercetools.sync.inventories.utils;

import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.inventories.service.InventoryEntryTransformService;
import com.commercetools.sync.inventories.service.impl.InventoryEntryTransformServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public final class InventoryTransformUtils {

  /**
   * Transforms InventoryEntries by resolving the references and map them to InventoryEntryDrafts.
   *
   * <p>This method resolves(fetch key values for the reference id's) non null and unexpanded
   * references of the InventoryEntry{@link InventoryEntry} by using cache.
   *
   * <p>If the reference ids are already cached, key values are pulled from the cache, otherwise it
   * executes the query to fetch the key value for the reference id's and store the idToKey value
   * pair in the cache for reuse.
   *
   * <p>Then maps the InventoryEntry to InventoryEntryDraft by performing reference resolution
   * considering idToKey value from the cache.
   *
   * @param client commercetools client.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @param inventoryEntries the inventoryEntries to resolve the references.
   * @return a new list which contains InventoryEntryDrafts which have all their references
   *     resolved.
   *     <p>TODO: Move the implementation from service class to this util class.
   */
  @Nonnull
  public static CompletableFuture<List<InventoryEntryDraft>> toInventoryEntryDrafts(
      @Nonnull final SphereClient client,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache,
      @Nonnull final List<InventoryEntry> inventoryEntries) {

    final InventoryEntryTransformService inventoryEntryTransformService =
        new InventoryEntryTransformServiceImpl(client, referenceIdToKeyCache);
    return inventoryEntryTransformService.toInventoryEntryDrafts(inventoryEntries);
  }
}
