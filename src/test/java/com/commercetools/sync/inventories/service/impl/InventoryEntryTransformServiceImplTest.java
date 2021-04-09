package com.commercetools.sync.inventories.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.models.ResourceIdsGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.inventories.service.InventoryEntryTransformService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class InventoryEntryTransformServiceImplTest {

  @Test
  void
      transform_InventoryReferences_ShouldResolveReferencesUsingCacheAndMapToInventoryEntryDraft() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
    final InventoryEntryTransformService inventoryEntryTransformService =
        new InventoryEntryTransformServiceImpl(sourceClient, referenceIdToKeyCache);

    final String customTypeId = UUID.randomUUID().toString();
    final String customTypeKey = "customTypeKey";
    final String inventoryEntrySku = "inventoryEntrySkuValue";

    final List<InventoryEntry> mockInventoryEntriesPage = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final InventoryEntry mockInventoryEntry = mock(InventoryEntry.class);
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final Reference<Type> typeReference =
          Reference.ofResourceTypeIdAndId("resourceTypeId", customTypeId);
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockInventoryEntry.getCustom()).thenReturn(mockCustomFields);
      when(mockInventoryEntry.getSku()).thenReturn(inventoryEntrySku);
      mockInventoryEntriesPage.add(mockInventoryEntry);
    }

    final String jsonStringCustomTypes =
        "{\"results\":[{\"id\":\"" + customTypeId + "\"," + "\"key\":\"" + customTypeKey + "\"}]}";
    final ResourceKeyIdGraphQlResult customTypesResult =
        SphereJsonUtils.readObject(jsonStringCustomTypes, ResourceKeyIdGraphQlResult.class);

    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(customTypesResult));

    // test
    final List<InventoryEntryDraft> inventoryEntryDraftsResolved =
        inventoryEntryTransformService
            .toInventoryEntryDrafts(mockInventoryEntriesPage)
            .toCompletableFuture()
            .join();

    // assertions
    final Optional<InventoryEntryDraft> inventoryEntryDraft1 =
        inventoryEntryDraftsResolved.stream()
            .filter(inventoryEntryDraft -> inventoryEntrySku.equals(inventoryEntryDraft.getSku()))
            .findFirst();

    assertThat(inventoryEntryDraft1)
        .hasValueSatisfying(
            inventoryEntryDraft ->
                assertThat(inventoryEntryDraft.getCustom().getType().getKey())
                    .isEqualTo(customTypeKey));
  }
}
