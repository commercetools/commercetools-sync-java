package com.commercetools.sync.sdk2.inventories.helpers;

import static com.commercetools.sync.sdk2.inventories.InventorySyncMockUtils.getMockInventoryEntry;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.channel.ChannelReferenceBuilder;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.inventory.InventoryEntryDraftBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryEntryIdentifierTest {

  private static final String SKU = "123";
  private static final String SKU_2 = "321";
  private static final String CHANNEL_ID = UUID.randomUUID().toString();
  private static final String CHANNEL_ID_2 = UUID.randomUUID().toString();

  @Test
  void of_WithDraftWithoutSupplyChannel_ShouldBuildInventoryEntryIdentifier() {
    final InventoryEntryDraft draft =
        InventoryEntryDraftBuilder.of().sku(SKU).quantityOnStock(1L).build();
    final InventoryEntryIdentifier inventoryEntryIdentifier = InventoryEntryIdentifier.of(draft);
    assertThat(inventoryEntryIdentifier).isNotNull();
    assertThat(inventoryEntryIdentifier.getSku()).isEqualTo(SKU);
    assertThat(inventoryEntryIdentifier.getSupplyChannelId()).isNull();
  }

  @Test
  void of_WithDraftWithSupplyChannel_ShouldBuildInventoryEntryIdentifier() {
    final InventoryEntryDraft draft =
        InventoryEntryDraftBuilder.of()
            .sku(SKU)
            .quantityOnStock(1L)
            .supplyChannel(ChannelResourceIdentifierBuilder.of().id(CHANNEL_ID).build())
            .build();
    final InventoryEntryIdentifier inventoryEntryIdentifier = InventoryEntryIdentifier.of(draft);
    assertThat(inventoryEntryIdentifier).isNotNull();
    assertThat(inventoryEntryIdentifier.getSku()).isEqualTo(SKU);
    assertThat(inventoryEntryIdentifier.getSupplyChannelId()).isEqualTo(CHANNEL_ID);
  }

  @Test
  void of_WithEntryWithoutSupplyChannel_ShouldBuildInventoryEntryIdentifier() {
    final InventoryEntry inventoryEntry = getMockInventoryEntry(SKU, null, null, null, null, null);
    final InventoryEntryIdentifier inventoryEntryIdentifier =
        InventoryEntryIdentifier.of(inventoryEntry);
    assertThat(inventoryEntryIdentifier).isNotNull();
    assertThat(inventoryEntryIdentifier.getSku()).isEqualTo(SKU);
    assertThat(inventoryEntryIdentifier.getSupplyChannelId()).isNull();
  }

  @Test
  void of_WithEntryWithSupplyChannel_ShouldBuildInventoryEntryIdentifier() {
    final InventoryEntry inventoryEntry =
        getMockInventoryEntry(
            SKU, null, null, null, ChannelReferenceBuilder.of().id(CHANNEL_ID).build(), null);
    final InventoryEntryIdentifier inventoryEntryIdentifier =
        InventoryEntryIdentifier.of(inventoryEntry);
    assertThat(inventoryEntryIdentifier).isNotNull();
    assertThat(inventoryEntryIdentifier.getSku()).isEqualTo(SKU);
    assertThat(inventoryEntryIdentifier.getSupplyChannelId()).isEqualTo(CHANNEL_ID);
  }

  @Test
  void equals_WithSameIdentifier_ShouldBeTrue() {
    // preparation
    final InventoryEntry inventoryEntry =
        getMockInventoryEntry(
            SKU, null, null, null, ChannelReferenceBuilder.of().id(CHANNEL_ID).build(), null);
    final InventoryEntryIdentifier entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);

    // test
    assertThat(entryIdentifier).isEqualTo(entryIdentifier);
  }

  @Test
  void equals_WithEqualSkuAndChannelKey_ShouldBeTrue() {
    // preparation
    final InventoryEntry inventoryEntry =
        getMockInventoryEntry(
            SKU, null, null, null, ChannelReferenceBuilder.of().id(CHANNEL_ID).build(), null);
    final InventoryEntryIdentifier entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);
    final InventoryEntryIdentifier draftIdentifier =
        InventoryEntryIdentifier.of(
            InventoryEntryDraftBuilder.of()
                .sku(SKU)
                .quantityOnStock(1L)
                .supplyChannel(ChannelResourceIdentifierBuilder.of().id(CHANNEL_ID).build())
                .build());

    // test
    assertThat(entryIdentifier).isEqualTo(draftIdentifier);
  }

  @Test
  void equals_WithDifferentEntryAndDraft_ShouldBeFalse() {
    // Different SKUs, same channels
    InventoryEntry inventoryEntry =
        getMockInventoryEntry(
            SKU, null, null, null, ChannelReferenceBuilder.of().id(CHANNEL_ID).build(), null);
    InventoryEntryIdentifier entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);
    InventoryEntryIdentifier draftIdentifier =
        InventoryEntryIdentifier.of(
            InventoryEntryDraftBuilder.of()
                .sku(SKU_2)
                .quantityOnStock(1L)
                .supplyChannel(ChannelResourceIdentifierBuilder.of().id(CHANNEL_ID).build())
                .build());

    assertThat(entryIdentifier).isNotEqualTo(draftIdentifier);

    // Same SKUs, different channels
    inventoryEntry =
        getMockInventoryEntry(
            SKU, null, null, null, ChannelReferenceBuilder.of().id(CHANNEL_ID).build(), null);
    entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);
    draftIdentifier =
        InventoryEntryIdentifier.of(
            InventoryEntryDraftBuilder.of()
                .sku(SKU)
                .quantityOnStock(1L)
                .supplyChannel(ChannelResourceIdentifierBuilder.of().id(CHANNEL_ID_2).build())
                .build());

    assertThat(entryIdentifier).isNotEqualTo(draftIdentifier);

    // different SKUs, different channels
    inventoryEntry =
        getMockInventoryEntry(
            SKU, null, null, null, ChannelReferenceBuilder.of().id(CHANNEL_ID).build(), null);
    entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);
    draftIdentifier =
        InventoryEntryIdentifier.of(
            InventoryEntryDraftBuilder.of()
                .sku(SKU_2)
                .quantityOnStock(1L)
                .supplyChannel(ChannelResourceIdentifierBuilder.of().id(CHANNEL_ID_2).build())
                .build());

    assertThat(entryIdentifier).isNotEqualTo(draftIdentifier);
  }

  @Test
  void equals_WithNoIdentifier_ShouldBeFalse() {
    final InventoryEntry inventoryEntry =
        getMockInventoryEntry(
            SKU, null, null, null, ChannelReferenceBuilder.of().id(CHANNEL_ID).build(), null);
    final InventoryEntryIdentifier entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);

    assertThat(entryIdentifier).isNotEqualTo(inventoryEntry);
  }

  @Test
  void inventoryEntryIdentifiersCreatedFromSimilarDraftAndEntry_ShouldHaveSameHashCodes() {
    InventoryEntry inventoryEntry =
        getMockInventoryEntry(
            SKU, null, null, null, ChannelReferenceBuilder.of().id(CHANNEL_ID).build(), null);
    InventoryEntryIdentifier entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);
    InventoryEntryIdentifier draftIdentifier =
        InventoryEntryIdentifier.of(
            InventoryEntryDraftBuilder.of()
                .sku(SKU)
                .quantityOnStock(1L)
                .supplyChannel(ChannelResourceIdentifierBuilder.of().id(CHANNEL_ID).build())
                .build());

    assertThat(entryIdentifier.hashCode()).isEqualTo(draftIdentifier.hashCode());

    // No supply channel
    inventoryEntry = getMockInventoryEntry(SKU, null, null, null, null, null);
    entryIdentifier = InventoryEntryIdentifier.of(inventoryEntry);
    draftIdentifier =
        InventoryEntryIdentifier.of(
            InventoryEntryDraftBuilder.of().sku(SKU).quantityOnStock(1L).build());

    assertThat(entryIdentifier.hashCode()).isEqualTo(draftIdentifier.hashCode());
  }

  @Test
  void inventoryEntryIdentifier_ShouldWorkAsHashMapKey() {
    final InventoryEntry inventoryEntry =
        getMockInventoryEntry(
            SKU, null, null, null, ChannelReferenceBuilder.of().id(CHANNEL_ID).build(), null);
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
        getMockInventoryEntry(
            SKU, null, null, null, ChannelReferenceBuilder.of().id(CHANNEL_ID).build(), null);
    final InventoryEntryIdentifier inventoryEntryIdentifier =
        InventoryEntryIdentifier.of(inventoryEntry);

    // test
    final String result = inventoryEntryIdentifier.toString();

    // assertion
    assertThat(result).isEqualTo(format("{sku='%s', supplyChannelId='%s'}", SKU, CHANNEL_ID));
  }
}
