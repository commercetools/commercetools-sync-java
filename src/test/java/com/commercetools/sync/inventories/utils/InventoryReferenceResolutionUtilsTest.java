package com.commercetools.sync.inventories.utils;

import static com.commercetools.sync.inventories.utils.InventoryReferenceResolutionUtils.mapToInventoryEntryDrafts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
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
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final Reference<Type> typeReference =
          Reference.ofResourceTypeIdAndId(UUID.randomUUID().toString(), customTypeId);
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockInventoryEntry.getCustom()).thenReturn(mockCustomFields);

      final Reference<Channel> channelReference =
          Reference.ofResourceTypeIdAndId(Channel.referenceTypeId(), channelId);
      when(mockInventoryEntry.getSupplyChannel()).thenReturn(channelReference);

      mockInventoryEntries.add(mockInventoryEntry);
    }

    // test
    final List<InventoryEntryDraft> referenceReplacedDrafts =
        mapToInventoryEntryDrafts(mockInventoryEntries, referenceIdToKeyCache);

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
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final Reference<Type> typeReference = Type.referenceOfId(customTypeId);
      when(mockCustomFields.getType()).thenReturn(typeReference);

      final InventoryEntry mockInventoryEntry = mock(InventoryEntry.class);
      when(mockInventoryEntry.getCustom()).thenReturn(mockCustomFields);

      final Reference<Channel> channelReference = Channel.referenceOfId(channelId);
      when(mockInventoryEntry.getSupplyChannel()).thenReturn(channelReference);

      mockInventoryEntries.add(mockInventoryEntry);
    }

    // test
    final List<InventoryEntryDraft> referenceReplacedDrafts =
        mapToInventoryEntryDrafts(mockInventoryEntries, referenceIdToKeyCache);

    // assertion
    for (InventoryEntryDraft referenceReplacedDraft : referenceReplacedDrafts) {
      assertThat(referenceReplacedDraft.getCustom().getType().getId()).isEqualTo(customTypeId);
      assertThat(referenceReplacedDraft.getSupplyChannel().getId()).isEqualTo(channelId);
    }
  }

  @Test
  void mapToInventoryEntryDrafts_WithNullReferences_ShouldNotReturnResourceIdentifiers() {
    // test
    final List<InventoryEntryDraft> referenceReplacedDrafts =
        mapToInventoryEntryDrafts(
            Collections.singletonList(mock(InventoryEntry.class)), referenceIdToKeyCache);

    // assertion
    for (InventoryEntryDraft referenceReplacedDraft : referenceReplacedDrafts) {
      assertThat(referenceReplacedDraft.getCustom()).isNull();
      assertThat(referenceReplacedDraft.getSupplyChannel()).isNull();
    }
  }
}
