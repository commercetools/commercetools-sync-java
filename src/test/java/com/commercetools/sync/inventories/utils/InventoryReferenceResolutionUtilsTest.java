package com.commercetools.sync.inventories.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.channel.ChannelReferenceBuilder;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class InventoryReferenceResolutionUtilsTest {

  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @AfterEach
  void clearCache() {
    referenceIdToKeyCache.clearCache();
  }

  @Test
  void
      mapToInventoryEntryDrafts_WithNonExpandedReferencesAndIdsCached_ShouldReturnResourceIdentifiersWithKeys() {
    // preparation
    final String customTypeId = UUID.randomUUID().toString();
    final String customTypeKey = "customTypeKey";

    final List<InventoryEntry> mockInventoryEntries = new ArrayList<>();

    final String channelId = UUID.randomUUID().toString();
    final String channelKey = "channelKey";

    referenceIdToKeyCache.add(customTypeId, customTypeKey);
    referenceIdToKeyCache.add(channelId, channelKey);

    for (int i = 0; i < 2; i++) {
      final InventoryEntry mockInventoryEntry = mock(InventoryEntry.class);
      when(mockInventoryEntry.getSku()).thenReturn("sku_" + i);
      when(mockInventoryEntry.getQuantityOnStock()).thenReturn(1L);
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final TypeReference typeReference = TypeReferenceBuilder.of().id(customTypeId).build();
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockInventoryEntry.getCustom()).thenReturn(mockCustomFields);

      final ChannelReference channelReference = ChannelReferenceBuilder.of().id(channelId).build();
      when(mockInventoryEntry.getSupplyChannel()).thenReturn(channelReference);

      mockInventoryEntries.add(mockInventoryEntry);
    }

    // test
    final List<InventoryEntryDraft> referenceReplacedDrafts =
        InventoryReferenceResolutionUtils.mapToInventoryEntryDrafts(
            mockInventoryEntries, referenceIdToKeyCache);

    // assertion
    for (InventoryEntryDraft referenceReplacedDraft : referenceReplacedDrafts) {
      assertThat(referenceReplacedDraft.getCustom().getType().getKey()).isEqualTo(customTypeKey);
      assertThat(referenceReplacedDraft.getSupplyChannel().getKey()).isEqualTo(channelKey);
    }
  }

  @Test
  void
      mapToInventoryEntryDrafts_WithNonExpandedReferencesAndIdsNotCached_ShouldReturnResourceIdentifiersWithoutKeys() {
    // preparation
    final String customTypeId = UUID.randomUUID().toString();
    final List<InventoryEntry> mockInventoryEntries = new ArrayList<>();
    final String channelId = UUID.randomUUID().toString();

    for (int i = 0; i < 2; i++) {
      final InventoryEntry mockInventoryEntry = mock(InventoryEntry.class);
      when(mockInventoryEntry.getSku()).thenReturn("sku_" + i);
      when(mockInventoryEntry.getQuantityOnStock()).thenReturn(1L);

      final CustomFields mockCustomFields = mock(CustomFields.class);
      final TypeReference typeReference = TypeReferenceBuilder.of().id(customTypeId).build();
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockInventoryEntry.getCustom()).thenReturn(mockCustomFields);

      final ChannelReference channelReference = ChannelReferenceBuilder.of().id(channelId).build();
      when(mockInventoryEntry.getSupplyChannel()).thenReturn(channelReference);

      mockInventoryEntries.add(mockInventoryEntry);
    }

    // test
    final List<InventoryEntryDraft> referenceReplacedDrafts =
        InventoryReferenceResolutionUtils.mapToInventoryEntryDrafts(
            mockInventoryEntries, referenceIdToKeyCache);

    // assertion
    for (InventoryEntryDraft referenceReplacedDraft : referenceReplacedDrafts) {
      assertThat(referenceReplacedDraft.getCustom().getType().getId()).isEqualTo(customTypeId);
      assertThat(referenceReplacedDraft.getSupplyChannel().getId()).isEqualTo(channelId);
    }
  }

  @Test
  void mapToInventoryEntryDrafts_WithNullReferences_ShouldNotReturnResourceIdentifiers() {
    // preparation
    final InventoryEntry mockInventoryEntry = mock(InventoryEntry.class);
    when(mockInventoryEntry.getSku()).thenReturn("sku");
    when(mockInventoryEntry.getQuantityOnStock()).thenReturn(1L);
    // test
    final List<InventoryEntryDraft> referenceReplacedDrafts =
        InventoryReferenceResolutionUtils.mapToInventoryEntryDrafts(
            Collections.singletonList(mockInventoryEntry), referenceIdToKeyCache);

    // assertion
    for (InventoryEntryDraft referenceReplacedDraft : referenceReplacedDrafts) {
      assertThat(referenceReplacedDraft.getCustom()).isNull();
      assertThat(referenceReplacedDraft.getSupplyChannel()).isNull();
    }
  }
}
