package com.commercetools.sync.inventories.helpers;

import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockInventoryEntry;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InventoryEntryIdentifierTest {

  private static final String SKU = "123";
  private static final String SKU_2 = "321";
  private static final String CHANNEL_ID = "channel-id";
  private static final String CHANNEL_ID_2 = "channel-id-2";

  @Test
  void of_WithDraftWithoutSupplyChannel_ShouldBuildInventoryEntryIdentifier() {
    final InventoryEntryDraft draft = InventoryEntryDraft.of(SKU, 1L);
    final InventoryEntryIdentifier inventoryEntryIdentifier = InventoryEntryIdentifier.of(draft);
    assertThat(inventoryEntryIdentifier).isNotNull();
    assertThat(inventoryEntryIdentifier.getInventoryEntrySku()).isEqualTo(SKU);
    assertThat(inventoryEntryIdentifier.getInventoryEntryChannelKey()).isNull();
  }

  @Test
  void of_WithDraftWithSupplyChannel_ShouldBuildInventoryEntryIdentifier() {
    final InventoryEntryDraft draft =
        InventoryEntryDraft.of(SKU, 1L, null, null, Channel.referenceOfId(CHANNEL_ID));
    final InventoryEntryIdentifier inventoryEntryIdentifier = InventoryEntryIdentifier.of(draft);
    assertThat(inventoryEntryIdentifier).isNotNull();
    assertThat(inventoryEntryIdentifier.getInventoryEntrySku()).isEqualTo(SKU);
    assertThat(inventoryEntryIdentifier.getInventoryEntryChannelKey()).isEqualTo(CHANNEL_ID);
  }

  @Test
  void of_WithEntryWithoutSupplyChannel_ShouldBuildInventoryEntryIdentifier() {
    final InventoryEntry inventoryEntry = getMockInventoryEntry(SKU, null, null, null, null, null);
    final InventoryEntryIdentifier inventoryEntryIdentifier =
        InventoryEntryIdentifier.of(inventoryEntry);
    assertThat(inventoryEntryIdentifier).isNotNull();
    assertThat(inventoryEntryIdentifier.getInventoryEntrySku()).isEqualTo(SKU);
    assertThat(inventoryEntryIdentifier.getInventoryEntryChannelKey()).isNull();
  }

  @Test
  void of_WithEntryWithSupplyChannel_ShouldBuildInventoryEntryIdentifier() {
    final InventoryEntry inventoryEntry =
        getMockInventoryEntry(SKU, null, null, null, Channel.referenceOfId(CHANNEL_ID), null);
    final InventoryEntryIdentifier inventoryEntryIdentifier =
        InventoryEntryIdentifier.of(inventoryEntry);
    assertThat(inventoryEntryIdentifier).isNotNull();
    assertThat(inventoryEntryIdentifier.getInventoryEntrySku()).isEqualTo(SKU);
    assertThat(inventoryEntryIdentifier.getInventoryEntryChannelKey()).isEqualTo(CHANNEL_ID);
  }

  @Test
  void of_WithSkuAndNoSupplyChannel_ShouldBuildInventoryEntryIdentifier() {
    final InventoryEntryIdentifier inventoryEntryIdentifier =
        InventoryEntryIdentifier.of(SKU, null);
    assertThat(inventoryEntryIdentifier).isNotNull();
    assertThat(inventoryEntryIdentifier.getInventoryEntrySku()).isEqualTo(SKU);
    assertThat(inventoryEntryIdentifier.getInventoryEntryChannelKey()).isNull();
  }

  @Test
  void of_WithSkuAndSupplyChannel_ShouldBuildInventoryEntryIdentifier() {
    final InventoryEntryIdentifier inventoryEntryIdentifier =
        InventoryEntryIdentifier.of(SKU, CHANNEL_ID);
    assertThat(inventoryEntryIdentifier).isNotNull();
    assertThat(inventoryEntryIdentifier.getInventoryEntrySku()).isEqualTo(SKU);
    assertThat(inventoryEntryIdentifier.getInventoryEntryChannelKey()).isEqualTo(CHANNEL_ID);
  }

  @Test
  void equals_WithSameIdentifier_ShouldBeTrue() {
    // preparation
    final InventoryEntry inventoryEntry =
        getMockInventoryEntry(SKU, null, null, null, Channel.referenceOfId(CHANNEL_ID), null);
    final InventoryEntryIdentifier entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);

    // test
    assertThat(entryIdentifier).isEqualTo(entryIdentifier);
  }

  @Test
  void equals_WithEqualSkuAndChannelKey_ShouldBeTrue() {
    // preparation
    final InventoryEntry inventoryEntry =
        getMockInventoryEntry(SKU, null, null, null, Channel.referenceOfId(CHANNEL_ID), null);
    final InventoryEntryIdentifier entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);
    final InventoryEntryIdentifier draftIdentifier =
        InventoryEntryIdentifier.of(
            InventoryEntryDraft.of(SKU, 1L, null, null, Channel.referenceOfId(CHANNEL_ID)));

    // test
    assertThat(entryIdentifier).isEqualTo(draftIdentifier);
  }

  @Test
  void equals_WithDifferentEntryAndDraft_ShouldBeFalse() {
    // Different SKUs, same channels
    InventoryEntry inventoryEntry =
        getMockInventoryEntry(SKU, null, null, null, Channel.referenceOfId(CHANNEL_ID), null);
    InventoryEntryIdentifier entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);
    InventoryEntryIdentifier draftIdentifier =
        InventoryEntryIdentifier.of(
            InventoryEntryDraft.of(SKU_2, 1L, null, null, Channel.referenceOfId(CHANNEL_ID)));

    assertThat(entryIdentifier).isNotEqualTo(draftIdentifier);

    // Same SKUs, different channels
    inventoryEntry =
        getMockInventoryEntry(SKU, null, null, null, Channel.referenceOfId(CHANNEL_ID), null);
    entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);
    draftIdentifier =
        InventoryEntryIdentifier.of(
            InventoryEntryDraft.of(SKU, 1L, null, null, Channel.referenceOfId(CHANNEL_ID_2)));

    assertThat(entryIdentifier).isNotEqualTo(draftIdentifier);

    // different SKUs, different channels
    inventoryEntry =
        getMockInventoryEntry(SKU, null, null, null, Channel.referenceOfId(CHANNEL_ID), null);
    entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);
    draftIdentifier =
        InventoryEntryIdentifier.of(
            InventoryEntryDraft.of(SKU_2, 1L, null, null, Channel.referenceOfId(CHANNEL_ID_2)));

    assertThat(entryIdentifier).isNotEqualTo(draftIdentifier);
  }

  @Test
  void equals_WithNoIdentifier_ShouldBeFalse() {
    final InventoryEntry inventoryEntry =
        getMockInventoryEntry(SKU, null, null, null, Channel.referenceOfId(CHANNEL_ID), null);
    final InventoryEntryIdentifier entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);

    assertThat(entryIdentifier).isNotEqualTo(inventoryEntry);
  }

  @Test
  void inventoryEntryIdentifiersCreatedFromSimilarDraftAndEntry_ShouldHaveSameHashCodes() {
    InventoryEntry inventoryEntry =
        getMockInventoryEntry(SKU, null, null, null, Channel.referenceOfId(CHANNEL_ID), null);
    InventoryEntryIdentifier entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);
    InventoryEntryIdentifier draftIdentifier =
        InventoryEntryIdentifier.of(
            InventoryEntryDraft.of(SKU, 1L, null, null, Channel.referenceOfId(CHANNEL_ID)));

    assertThat(entryIdentifier.hashCode()).isEqualTo(draftIdentifier.hashCode());

    // No supply channel
    inventoryEntry = getMockInventoryEntry(SKU, null, null, null, null, null);
    entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);
    draftIdentifier =
        InventoryEntryIdentifier.of(InventoryEntryDraft.of(SKU, 1L, null, null, null));

    assertThat(entryIdentifier.hashCode()).isEqualTo(draftIdentifier.hashCode());
  }

  @Test
  void inventoryEntryIdentifier_ShouldWorkAsHashMapKey() {
    final InventoryEntry inventoryEntry =
        getMockInventoryEntry(SKU, null, null, null, Channel.referenceOfId(CHANNEL_ID), null);
    final InventoryEntryIdentifier inventoryEntryIdentifier =
        InventoryEntryIdentifier.of(inventoryEntry);
    final Map<InventoryEntryIdentifier, InventoryEntry> map = new HashMap<>();
    map.put(inventoryEntryIdentifier, inventoryEntry);

    assertThat(map.containsKey(inventoryEntryIdentifier)).isTrue();
    assertThat(map.get(inventoryEntryIdentifier)).isEqualTo(inventoryEntry);
  }

  @Test
  void toString_WithBothSkuAndChannelKey_ShouldReturnCorrectString() {
    // preparation
    final InventoryEntry inventoryEntry =
        getMockInventoryEntry(SKU, null, null, null, Channel.referenceOfId(CHANNEL_ID), null);
    final InventoryEntryIdentifier inventoryEntryIdentifier =
        InventoryEntryIdentifier.of(inventoryEntry);

    // test
    final String result = inventoryEntryIdentifier.toString();

    // assertion
    assertThat(result).isEqualTo(format("{sku='%s', channelKey='%s'}", SKU, CHANNEL_ID));
  }
}
