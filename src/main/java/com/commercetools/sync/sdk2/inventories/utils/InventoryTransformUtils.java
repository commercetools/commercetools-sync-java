package com.commercetools.sync.sdk2.inventories.utils;

import static com.commercetools.sync.sdk2.inventories.utils.InventoryReferenceResolutionUtils.mapToInventoryEntryDrafts;
import static java.util.stream.Collectors.toSet;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.services.impl.BaseTransformServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
   */
  @Nonnull
  public static CompletableFuture<List<InventoryEntryDraft>> toInventoryEntryDrafts(
      @Nonnull final ProjectApiRoot client,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache,
      @Nonnull final List<InventoryEntry> inventoryEntries) {

    final InventoryEntryTransformServiceImpl inventoryEntryTransformService =
        new InventoryEntryTransformServiceImpl(client, referenceIdToKeyCache);
    return inventoryEntryTransformService.toInventoryEntryDrafts(inventoryEntries);
  }

  private static class InventoryEntryTransformServiceImpl extends BaseTransformServiceImpl {

    public InventoryEntryTransformServiceImpl(
        @Nonnull final ProjectApiRoot ctpClient,
        @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
      super(ctpClient, referenceIdToKeyCache);
    }

    @Nonnull
    public CompletableFuture<List<InventoryEntryDraft>> toInventoryEntryDrafts(
        @Nonnull final List<InventoryEntry> inventoryEntries) {

      final List<CompletableFuture<Void>> transformReferencesToRunParallel = new ArrayList<>();
      transformReferencesToRunParallel.add(this.transformCustomTypeReference(inventoryEntries));
      transformReferencesToRunParallel.add(this.transformChannelReference(inventoryEntries));

      return CompletableFuture.allOf(
              transformReferencesToRunParallel.toArray(CompletableFuture[]::new))
          .thenApply(ignore -> mapToInventoryEntryDrafts(inventoryEntries, referenceIdToKeyCache));
    }

    @Nonnull
    private CompletableFuture<Void> transformCustomTypeReference(
        @Nonnull final List<InventoryEntry> inventoryEntries) {

      final Set<String> setOfTypeIds =
          inventoryEntries.stream()
              .map(InventoryEntry::getCustom)
              .filter(Objects::nonNull)
              .map(CustomFields::getType)
              .map(TypeReference::getId)
              .collect(toSet());

      return fetchAndFillReferenceIdToKeyCache(setOfTypeIds, GraphQlQueryResource.TYPES);
    }

    @Nonnull
    private CompletableFuture<Void> transformChannelReference(
        @Nonnull final List<InventoryEntry> inventoryEntries) {

      final Set<String> setOfChannelIds =
          inventoryEntries.stream()
              .map(InventoryEntry::getSupplyChannel)
              .filter(Objects::nonNull)
              .map(ChannelReference::getId)
              .collect(toSet());

      return fetchAndFillReferenceIdToKeyCache(setOfChannelIds, GraphQlQueryResource.CHANNELS);
    }
  }
}
