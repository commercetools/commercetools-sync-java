package com.commercetools.sync.inventories.service.impl;

import static com.commercetools.sync.inventories.utils.InventoryReferenceResolutionUtils.mapToInventoryEntryDrafts;
import static java.util.stream.Collectors.toSet;

import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.inventories.service.InventoryEntryTransformService;
import com.commercetools.sync.services.impl.BaseTransformServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class InventoryEntryTransformServiceImpl extends BaseTransformServiceImpl
    implements InventoryEntryTransformService {

  public InventoryEntryTransformServiceImpl(
      @Nonnull final SphereClient ctpClient,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    super(ctpClient, referenceIdToKeyCache);
  }

  @Nonnull
  @Override
  public CompletableFuture<List<InventoryEntryDraft>> toInventoryEntryDrafts(
      @Nonnull final List<InventoryEntry> inventoryEntries) {

    final List<CompletableFuture<Void>> transformReferencesToRunParallel = new ArrayList<>();
    transformReferencesToRunParallel.add(this.transformCustomTypeReference(inventoryEntries));
    transformReferencesToRunParallel.add(this.transformChannelReference(inventoryEntries));

    return CompletableFuture.allOf(
            transformReferencesToRunParallel.stream().toArray(CompletableFuture[]::new))
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
            .map(Reference::getId)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(setOfTypeIds, GraphQlQueryResources.TYPES);
  }

  @Nonnull
  private CompletableFuture<Void> transformChannelReference(
      @Nonnull final List<InventoryEntry> inventoryEntries) {

    final Set<String> setOfChannelIds =
        inventoryEntries.stream()
            .map(InventoryEntry::getSupplyChannel)
            .filter(Objects::nonNull)
            .map(Reference::getId)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(setOfChannelIds, GraphQlQueryResources.CHANNELS);
  }
}
