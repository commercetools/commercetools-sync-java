package com.commercetools.sync.integration.sdk2.services.impl;

import static com.commercetools.sync.integration.sdk2.commons.utils.ChannelITUtils.deleteChannelsFromTargetAndSource;
import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.sdk2.commons.utils.InventoryITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelDraft;
import com.commercetools.api.models.channel.ChannelDraftBuilder;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.inventory.*;
import com.commercetools.sync.sdk2.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.sdk2.inventories.helpers.InventoryEntryIdentifier;
import com.commercetools.sync.sdk2.services.InventoryService;
import com.commercetools.sync.sdk2.services.impl.InventoryServiceImpl;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryServiceImplIT {

  private InventoryService inventoryService;

  /**
   * Deletes inventories and supply channels from source and target CTP projects. Populates target
   * CTP project with test data.
   */
  @BeforeEach
  void setup() {
    deleteInventoryEntriesFromTargetAndSource();
    deleteTypesFromTargetAndSource();
    deleteChannelsFromTargetAndSource();
    populateTargetProject();
    inventoryService =
        new InventoryServiceImpl(InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build());
  }

  /** Cleans up the target and source test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    deleteInventoryEntriesFromTargetAndSource();
    deleteTypesFromTargetAndSource();
    deleteChannelsFromTargetAndSource();
  }

  @Test
  void fetchInventoryEntriesBySku_ShouldReturnCorrectInventoryEntriesWithoutReferenceExpansion() {
    // prepare draft and create
    final ChannelDraft channelDraft1 = ChannelDraftBuilder.of().key("CHANNEL_KEY").build();
    final Channel channel =
        CTP_TARGET_CLIENT
            .channels()
            .post(channelDraft1)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final InventoryEntryDraft inventoryEntryDraft1 =
        InventoryEntryDraftBuilder.of()
            .sku("SKU_WITH_CHANNEL")
            .quantityOnStock(1L)
            .supplyChannel(ChannelResourceIdentifierBuilder.of().id(channel.getId()).build())
            .build();

    final InventoryEntryDraft inventoryEntryDraft2 =
        InventoryEntryDraftBuilder.of()
            .sku("SKU_WITHOUT_CHANNEL")
            .quantityOnStock(1L)
            .supplyChannel(ChannelResourceIdentifierBuilder.of().id(channel.getId()).build())
            .build();

    CompletableFuture.allOf(
            inventoryService.createInventoryEntry(inventoryEntryDraft1).toCompletableFuture(),
            inventoryService.createInventoryEntry(inventoryEntryDraft2).toCompletableFuture())
        .join();

    // test
    final Set<InventoryEntryIdentifier> identifiers = new HashSet<>();
    identifiers.add(InventoryEntryIdentifier.of(inventoryEntryDraft1));
    identifiers.add(InventoryEntryIdentifier.of(inventoryEntryDraft2));

    final Set<InventoryEntry> result =
        inventoryService
            .fetchInventoryEntriesByIdentifiers(identifiers)
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    // assert references are not expanded
    result.stream()
        .filter(inventoryEntry -> inventoryEntry.getSupplyChannel() != null)
        .map(InventoryEntry::getSupplyChannel)
        .forEach(supplyChannel -> assertThat(supplyChannel.getObj()).isNull());
  }

  @Test
  void createInventoryEntry_ShouldCreateCorrectInventoryEntry() {
    // prepare draft and create
    final InventoryEntryDraft inventoryEntryDraft =
        InventoryEntryDraftBuilder.of()
            .sku(SKU_2)
            .quantityOnStock(QUANTITY_ON_STOCK_2)
            .expectedDelivery(EXPECTED_DELIVERY_2)
            .restockableInDays(RESTOCKABLE_IN_DAYS_2)
            .build();

    final Optional<InventoryEntry> result =
        inventoryService.createInventoryEntry(inventoryEntryDraft).toCompletableFuture().join();

    // assertions
    assertThat(result)
        .hasValueSatisfying(
            inventoryEntry -> {
              assertThat(inventoryEntry.getQuantityOnStock()).isEqualTo(QUANTITY_ON_STOCK_2);
              assertThat(inventoryEntry.getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS_2);
              assertThat(inventoryEntry.getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY_2);
            });

    // assert CTP state
    final Optional<InventoryEntry> updatedInventoryEntry =
        getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_2, null, null);
    assertThat(updatedInventoryEntry).isEqualTo(result.get());
  }

  @Test
  void updateInventoryEntry_ShouldUpdateInventoryEntry() {
    // fetch existing inventory entry and assert its state
    final Optional<InventoryEntry> existingEntryOptional =
        getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null, null);
    assertThat(existingEntryOptional).isNotEmpty();

    final InventoryEntry existingEntry = existingEntryOptional.get();
    assertThat(existingEntry.getQuantityOnStock()).isEqualTo(QUANTITY_ON_STOCK_1);
    assertThat(existingEntry.getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS_1);
    assertThat(existingEntry.getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY_1);

    // build update actions and do update
    final List<InventoryEntryUpdateAction> updateActions =
        Stream.of(
                InventoryEntryChangeQuantityActionBuilder.of()
                    .quantity(QUANTITY_ON_STOCK_2)
                    .build(),
                InventoryEntrySetExpectedDeliveryActionBuilder.of()
                    .expectedDelivery(EXPECTED_DELIVERY_2)
                    .build(),
                InventoryEntrySetRestockableInDaysActionBuilder.of()
                    .restockableInDays(RESTOCKABLE_IN_DAYS_2)
                    .build())
            .collect(toList());

    final InventoryEntry result =
        inventoryService
            .updateInventoryEntry(existingEntry, updateActions)
            .toCompletableFuture()
            .join();
    assertThat(result).isNotNull();
    assertThat(result.getQuantityOnStock()).isEqualTo(QUANTITY_ON_STOCK_2);
    assertThat(result.getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS_2);
    assertThat(result.getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY_2);

    // assert CTP state
    final Optional<InventoryEntry> updatedInventoryEntry =
        getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null, null);
    assertThat(updatedInventoryEntry).isNotEmpty();
    assertThat(updatedInventoryEntry.get()).isEqualTo(result);
  }
}
