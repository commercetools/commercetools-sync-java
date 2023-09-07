package com.commercetools.sync.integration.externalsource.inventories;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.utils.CustomValueConverter.convertCustomValueObjDataToJsonNode;
import static com.commercetools.sync.integration.commons.utils.ChannelITUtils.*;
import static com.commercetools.sync.integration.commons.utils.InventoryITUtils.*;
import static com.commercetools.sync.integration.commons.utils.InventoryITUtils.CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ByProjectKeyChannelsGet;
import com.commercetools.api.models.channel.*;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.inventory.InventoryEntryDraftBuilder;
import com.commercetools.api.models.inventory.InventoryEntryUpdateAction;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.integration.commons.utils.ChannelITUtils;
import com.commercetools.sync.inventories.InventorySync;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import com.commercetools.sync.inventories.utils.InventoryTransformUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Contains integration tests of inventory sync. */
class InventorySyncIT {

  /**
   * Deletes inventories and supply channels from source and target CTP projects. Populates source
   * and target CTP projects with test data.
   */
  @BeforeEach
  void setup() {
    deleteInventoryEntriesFromTargetAndSource();
    deleteChannelsFromTargetAndSource();
    final Channel channel = ChannelITUtils.ensureChannelsInTargetProject();
    final List<Channel> channels = ChannelITUtils.ensureChannelsInSourceProject();

    ensureInventoriesCustomType(CTP_SOURCE_CLIENT);
    ensureInventoriesCustomType(CTP_TARGET_CLIENT);

    final ChannelResourceIdentifier supplyChannelReference =
        ChannelResourceIdentifierBuilder.of().id(channel.getId()).build();
    populateInventoriesInSourceProject(channels);
    populateInventoriesInTargetProject(supplyChannelReference);
  }

  /**
   * Deletes all the test data from the {@code CTP_SOURCE_CLIENT} and the {@code CTP_SOURCE_CLIENT}
   * projects that were set up in this test class.
   */
  @AfterAll
  static void tearDown() {
    deleteInventoryEntriesFromTargetAndSource();
    deleteChannelsFromTargetAndSource();
  }

  @Test
  void sync_WithUpdatedInventory_ShouldUpdateInventory() {
    // Ensure that old entry has correct values before sync.
    final Optional<InventoryEntry> oldInventoryBeforeSync =
        getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null, null);
    assertValues(
        oldInventoryBeforeSync.get(),
        QUANTITY_ON_STOCK_1,
        EXPECTED_DELIVERY_1,
        RESTOCKABLE_IN_DAYS_1);

    // Prepare sync data.
    final InventoryEntryDraft newInventoryDraft =
        InventoryEntryDraftBuilder.of()
            .sku(SKU_1)
            .quantityOnStock(QUANTITY_ON_STOCK_2)
            .expectedDelivery(EXPECTED_DELIVERY_2)
            .restockableInDays(RESTOCKABLE_IN_DAYS_2)
            .custom(getMockCustomFieldsDraft())
            .build();
    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

    // Sync and ensure that proper statistics were returned.
    final InventorySyncStatistics inventorySyncStatistics =
        inventorySync.sync(singletonList(newInventoryDraft)).toCompletableFuture().join();
    assertThat(inventorySyncStatistics).hasValues(1, 0, 1, 0);

    // Ensure that old entry has correct values after sync.
    final Optional<InventoryEntry> oldInventoryAfterSync =
        getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null, "custom.type");
    assertThat(oldInventoryAfterSync).isNotEmpty();
    assertValues(
        oldInventoryAfterSync.get(),
        QUANTITY_ON_STOCK_2,
        EXPECTED_DELIVERY_2,
        RESTOCKABLE_IN_DAYS_2);
    final JsonNode syncedCustomFieldDataAsJson =
        convertCustomValueObjDataToJsonNode(
            oldInventoryAfterSync.get().getCustom().getFields().values().get(CUSTOM_FIELD_NAME));
    assertThat(syncedCustomFieldDataAsJson).isEqualTo(CUSTOM_FIELD_VALUE);
  }

  @Test
  void sync_WithNewInventory_ShouldCreateInventory() {
    // Ensure that old entry has correct values before sync.
    final Optional<InventoryEntry> oldInventoryBeforeSync =
        getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_2, null, null);
    assertThat(oldInventoryBeforeSync).isEmpty();

    // Prepare sync data.
    final InventoryEntryDraft newInventoryDraft =
        InventoryEntryDraftBuilder.of()
            .sku(SKU_2)
            .quantityOnStock(QUANTITY_ON_STOCK_2)
            .expectedDelivery(EXPECTED_DELIVERY_2)
            .restockableInDays(RESTOCKABLE_IN_DAYS_2)
            .build();
    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

    // Sync and ensure that proper statistics were returned.
    final InventorySyncStatistics inventorySyncStatistics =
        inventorySync.sync(singletonList(newInventoryDraft)).toCompletableFuture().join();
    assertThat(inventorySyncStatistics).hasValues(1, 1, 0, 0);

    // Ensure that old entry has correct values after sync.
    final Optional<InventoryEntry> oldInventoryAfterSync =
        getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_2, null, null);
    assertThat(oldInventoryAfterSync).isNotEmpty();
    assertValues(
        oldInventoryAfterSync.get(),
        QUANTITY_ON_STOCK_2,
        EXPECTED_DELIVERY_2,
        RESTOCKABLE_IN_DAYS_2);
  }

  @Test
  void sync_WithKeyToExistingSupplyChannelInPlaceOfId_ShouldUpdateInventory() {
    /*
     * Fetch existing Channel of key SUPPLY_CHANNEL_KEY_1 from target project.
     * This is done only for test assertion reasons, not necessary for sync.
     */
    final Optional<Channel> supplyChannel =
        getChannelByKey(CTP_TARGET_CLIENT, SUPPLY_CHANNEL_KEY_1);
    assertThat(supplyChannel).isNotEmpty();

    /*
     * Prepare InventoryEntryDraft of sku SKU_1 and reference to supply channel of key SUPPLY_CHANNEL_KEY_1.
     */
    final ChannelResourceIdentifier supplyChannelReference =
        ChannelResourceIdentifierBuilder.of().key(SUPPLY_CHANNEL_KEY_1).build();

    final InventoryEntryDraft newInventoryDraft =
        InventoryEntryDraftBuilder.of()
            .sku(SKU_1)
            .quantityOnStock(QUANTITY_ON_STOCK_2)
            .expectedDelivery(EXPECTED_DELIVERY_2)
            .restockableInDays(RESTOCKABLE_IN_DAYS_2)
            .supplyChannel(supplyChannelReference)
            .build();

    // Ensure old entry values before sync.
    final Optional<InventoryEntry> oldInventoryBeforeSync =
        getInventoryEntryBySkuAndSupplyChannel(
            CTP_TARGET_CLIENT, SKU_1, supplyChannel.get().toReference(), null);
    assertThat(oldInventoryBeforeSync).isPresent();
    assertValues(
        oldInventoryBeforeSync.get(),
        QUANTITY_ON_STOCK_1,
        EXPECTED_DELIVERY_1,
        RESTOCKABLE_IN_DAYS_1);

    // Prepare sync options and perform sync of draft to target project.
    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
    final InventorySyncStatistics inventorySyncStatistics =
        inventorySync.sync(singletonList(newInventoryDraft)).toCompletableFuture().join();
    assertThat(inventorySyncStatistics).hasValues(1, 0, 1, 0);

    // Ensure old entry values after sync.
    final Optional<InventoryEntry> oldInventoryAfterSync =
        getInventoryEntryBySkuAndSupplyChannel(
            CTP_TARGET_CLIENT, SKU_1, supplyChannel.get().toReference(), null);
    assertThat(oldInventoryAfterSync).isPresent();
    assertValues(
        oldInventoryAfterSync.get(),
        QUANTITY_ON_STOCK_2,
        EXPECTED_DELIVERY_2,
        RESTOCKABLE_IN_DAYS_2);
  }

  @Test
  void sync_WithNewSupplyChannelAndChannelsEnsured_ShouldCreateNewSupplyChannel() {
    // Ensure that supply channel doesn't exist before sync.
    final Optional<Channel> oldSupplyChannelBeforeSync =
        getChannelByKey(CTP_TARGET_CLIENT, SUPPLY_CHANNEL_KEY_2);
    assertThat(oldSupplyChannelBeforeSync).isEmpty();

    // Prepare sync data.
    final ChannelResourceIdentifier newSupplyChannelReference =
        ChannelResourceIdentifierBuilder.of().key(SUPPLY_CHANNEL_KEY_2).build();
    final InventoryEntryDraft newInventoryDraft =
        InventoryEntryDraftBuilder.of()
            .sku(SKU_1)
            .quantityOnStock(QUANTITY_ON_STOCK_2)
            .expectedDelivery(EXPECTED_DELIVERY_2)
            .restockableInDays(RESTOCKABLE_IN_DAYS_2)
            .supplyChannel(newSupplyChannelReference)
            .build();
    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).ensureChannels(true).build();
    final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

    // Sync and ensure that proper statistics were returned.
    final InventorySyncStatistics inventorySyncStatistics =
        inventorySync.sync(singletonList(newInventoryDraft)).toCompletableFuture().join();
    assertThat(inventorySyncStatistics).hasValues(1, 1, 0, 0);

    // Ensure that supply channel exists before sync.
    final Optional<Channel> oldSupplyChannelAfterSync =
        getChannelByKey(CTP_TARGET_CLIENT, SUPPLY_CHANNEL_KEY_2);
    assertThat(oldSupplyChannelAfterSync).isNotEmpty();
    assertThat(oldSupplyChannelAfterSync.get().getKey()).isEqualTo(SUPPLY_CHANNEL_KEY_2);
  }

  @Test
  void sync_WithReferencesToSourceChannels_ShouldUpdateInventoriesWithoutChannelCreation() {
    // Ensure channels in target project before sync
    final ByProjectKeyChannelsGet ensureChannelTargetQuery =
        CTP_TARGET_CLIENT
            .channels()
            .get()
            .withWhere("roles contains any (\"" + ChannelRoleEnum.INVENTORY_SUPPLY + "\")");
    final List<Channel> targetChannelsBeforeSync =
        ensureChannelTargetQuery.execute().toCompletableFuture().join().getBody().getResults();
    assertThat(targetChannelsBeforeSync).hasSize(1);
    assertThat(targetChannelsBeforeSync.get(0).getKey()).isEqualTo(SUPPLY_CHANNEL_KEY_1);

    // Prepare InventoryEntryDraft of sku SKU_1 and reference to above supply channel key.
    final InventoryEntryDraft newInventoryDraft =
        InventoryEntryDraftBuilder.of()
            .sku(SKU_1)
            .quantityOnStock(QUANTITY_ON_STOCK_2)
            .expectedDelivery(EXPECTED_DELIVERY_2)
            .restockableInDays(RESTOCKABLE_IN_DAYS_2)
            .supplyChannel(
                channelResourceIdentifierBuilder ->
                    channelResourceIdentifierBuilder.key(SUPPLY_CHANNEL_KEY_1))
            .build();

    // Fetch existing Channel of key SUPPLY_CHANNEL_KEY_1 from target project.
    final Optional<Channel> targetSupplyChannel =
        getChannelByKey(CTP_TARGET_CLIENT, SUPPLY_CHANNEL_KEY_1);
    assertThat(targetSupplyChannel).isNotEmpty();
    final ChannelReference targetChannelReference = targetSupplyChannel.get().toReference();

    // Ensure old entry values before sync.
    final Optional<InventoryEntry> oldInventoryBeforeSync =
        getInventoryEntryBySkuAndSupplyChannel(
            CTP_TARGET_CLIENT, SKU_1, targetChannelReference, null);
    assertThat(oldInventoryBeforeSync).isPresent();
    assertValues(
        oldInventoryBeforeSync.get(),
        QUANTITY_ON_STOCK_1,
        EXPECTED_DELIVERY_1,
        RESTOCKABLE_IN_DAYS_1);
    assertThat(oldInventoryBeforeSync.get().getSupplyChannel().getId())
        .isEqualTo(targetChannelReference.getId());

    // Prepare sync options and perform sync of draft to target project.
    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
    final InventorySyncStatistics inventorySyncStatistics =
        inventorySync.sync(singletonList(newInventoryDraft)).toCompletableFuture().join();
    assertThat(inventorySyncStatistics).hasValues(1, 0, 1, 0);

    // Ensure old entry values after sync.
    final Optional<InventoryEntry> oldInventoryAfterSync =
        getInventoryEntryBySkuAndSupplyChannel(
            CTP_TARGET_CLIENT, SKU_1, targetChannelReference, null);
    assertThat(oldInventoryAfterSync).isPresent();
    assertValues(
        oldInventoryAfterSync.get(),
        QUANTITY_ON_STOCK_2,
        EXPECTED_DELIVERY_2,
        RESTOCKABLE_IN_DAYS_2);
    assertThat(oldInventoryAfterSync.get().getSupplyChannel().getId())
        .isEqualTo(targetChannelReference.getId());

    // Ensure channels in target project after sync.
    final List<Channel> targetChannelsAfterSync =
        ensureChannelTargetQuery.execute().toCompletableFuture().join().getBody().getResults();
    assertThat(targetChannelsAfterSync).isNotEmpty();
    assertThat(targetChannelsAfterSync).hasSize(1);
    assertThat(targetChannelsAfterSync.get(0)).isEqualTo(targetChannelsBeforeSync.get(0));
  }

  @Test
  void sync_FromSourceToTargetProjectWithChannelsEnsured_ShouldReturnProperStatistics() {
    // Fetch 3 inventories from source project. Convert them to drafts.
    final List<InventoryEntry> inventoryEntries =
        CTP_SOURCE_CLIENT
            .inventory()
            .get()
            .withExpand("custom.type")
            .addExpand("supplyChannel")
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    final List<InventoryEntryDraft> newInventories =
        InventoryTransformUtils.toInventoryEntryDrafts(
                CTP_SOURCE_CLIENT, REFERENCE_ID_TO_KEY_CACHE, inventoryEntries)
            .join();

    // Prepare sync options and perform sync of draft to target project.
    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).ensureChannels(true).build();
    final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
    final InventorySyncStatistics inventorySyncStatistics =
        inventorySync.sync(newInventories).toCompletableFuture().join();
    // In target project existed 2 inventories before update. One resource equals an inventory in
    // source project.
    assertThat(inventorySyncStatistics).hasValues(3, 1, 1, 0);
  }

  @Test
  void sync_FromSourceToTargetWithoutChannelsEnsured_ShouldReturnProperStatistics() {
    // Fetch 3 inventories from source project. Convert them to drafts.
    final List<InventoryEntry> inventoryEntries =
        CTP_SOURCE_CLIENT
            .inventory()
            .get()
            .withExpand("custom.type")
            .addExpand("supplyChannel")
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    final List<InventoryEntryDraft> newInventories =
        InventoryTransformUtils.toInventoryEntryDrafts(
                CTP_SOURCE_CLIENT, REFERENCE_ID_TO_KEY_CACHE, inventoryEntries)
            .join();

    // Prepare sync options and perform sync of draft to target project.
    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).ensureChannels(false).build();
    final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
    final InventorySyncStatistics inventorySyncStatistics =
        inventorySync.sync(newInventories).toCompletableFuture().join();
    // In target project existed 2 inventories before update. One resource equals an inventory in
    // source project.
    assertThat(inventorySyncStatistics).hasValues(3, 0, 1, 1);
  }

  @Test
  void sync_WithBatchProcessing_ShouldCreateAllGivenInventories() {
    // Ensure inventory entries amount in target project before sync.
    final List<InventoryEntry> oldEntriesBeforeSync =
        CTP_TARGET_CLIENT
            .inventory()
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();
    assertThat(oldEntriesBeforeSync).hasSize(2);

    // Create 10 drafts of new inventories.
    final List<InventoryEntryDraft> newInventories =
        LongStream.range(0, 10)
            .mapToObj(
                l ->
                    InventoryEntryDraftBuilder.of()
                        .sku(String.valueOf(l))
                        .quantityOnStock(l)
                        .build())
            .collect(toList());

    // Set batch size of number less that new inventories size.
    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).batchSize(3).build();
    final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

    // Perform sync and ensure its results.
    final InventorySyncStatistics inventorySyncStatistics =
        inventorySync.sync(newInventories).toCompletableFuture().join();
    assertThat(inventorySyncStatistics).hasValues(10, 10, 0, 0);

    // Ensure inventory entries amount in target project after sync.
    final List<InventoryEntry> oldEntriesAfterSync =
        CTP_TARGET_CLIENT
            .inventory()
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();
    assertThat(oldEntriesAfterSync).hasSize(12);
  }

  @Test
  void sync_ShouldReturnProperStatisticsObject() {
    // Fetch new inventories from source project. Convert them to drafts.
    final List<InventoryEntry> inventoryEntries =
        CTP_SOURCE_CLIENT
            .inventory()
            .get()
            .withExpand("custom.type")
            .addExpand("supplyChannel")
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    final List<InventoryEntryDraft> newInventories =
        InventoryTransformUtils.toInventoryEntryDrafts(
                CTP_SOURCE_CLIENT, REFERENCE_ID_TO_KEY_CACHE, inventoryEntries)
            .join();

    // Prepare sync options and perform sync of draft to target project.
    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).ensureChannels(true).build();
    final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
    final InventorySyncStatistics inventorySyncStatistics =
        inventorySync.sync(newInventories).toCompletableFuture().join();
    assertThat(inventorySyncStatistics).hasValues(3, 1, 1, 0);
    assertThat(inventorySyncStatistics.getLatestBatchProcessingTimeInMillis()).isGreaterThan(0L);
  }

  @Test
  void sync_WithCustomErrorCallback_ShouldExecuteCallbackOnError() {
    // Fetch new inventories from source project. Convert them to drafts.
    final List<InventoryEntry> inventoryEntries =
        CTP_SOURCE_CLIENT
            .inventory()
            .get()
            .withExpand("custom.type")
            .addExpand("supplyChannel")
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    final List<InventoryEntryDraft> newInventories =
        InventoryTransformUtils.toInventoryEntryDrafts(
                CTP_SOURCE_CLIENT, REFERENCE_ID_TO_KEY_CACHE, inventoryEntries)
            .join();

    // Prepare sync options and perform sync of draft to target project.
    final AtomicInteger invocationCounter = new AtomicInteger(0);
    QuadConsumer<
            SyncException,
            Optional<InventoryEntryDraft>,
            Optional<InventoryEntry>,
            List<InventoryEntryUpdateAction>>
        countingErrorCallback =
            (exception, newResource, oldResource, updateActions) ->
                invocationCounter.incrementAndGet();
    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(countingErrorCallback)
            .ensureChannels(false)
            .build();
    final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
    final InventorySyncStatistics inventorySyncStatistics =
        inventorySync.sync(newInventories).toCompletableFuture().join();
    assertThat(inventorySyncStatistics).hasValues(3, 0, 1, 1);
    assertThat(invocationCounter.get()).isEqualTo(1);
  }

  @Test
  void sync_WithSyncDifferentBatchesConcurrently_ShouldReturnProperStatistics() {
    // Prepare new inventories.
    final List<InventoryEntryDraft> newDrafts =
        IntStream.range(100, 160)
            .mapToObj(
                i ->
                    InventoryEntryDraftBuilder.of()
                        .sku(String.valueOf(i))
                        .quantityOnStock(QUANTITY_ON_STOCK_1)
                        .build())
            .collect(toList());

    // Split them to batches.
    final List<InventoryEntryDraft> firstBatch = newDrafts.subList(0, 20);
    final List<InventoryEntryDraft> secondBatch = newDrafts.subList(20, 40);
    final List<InventoryEntryDraft> thirdBatch = newDrafts.subList(40, 60);

    // Initialize sync.
    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

    // Run batch syncing concurrently.
    final CompletableFuture<InventorySyncStatistics> firstResult =
        inventorySync.sync(firstBatch).toCompletableFuture();
    final CompletableFuture<InventorySyncStatistics> secondResult =
        inventorySync.sync(secondBatch).toCompletableFuture();
    final CompletableFuture<InventorySyncStatistics> thirdResult =
        inventorySync.sync(thirdBatch).toCompletableFuture();

    CompletableFuture.allOf(firstResult, secondResult, thirdResult).join();

    // Ensure instance's statistics.
    assertThat(inventorySync.getStatistics()).hasValues(60, 60, 0, 0);
  }

  private void assertValues(
      @Nonnull final InventoryEntry inventoryEntry,
      @Nonnull final Long quantityOnStock,
      @Nullable final ZonedDateTime expectedDelivery,
      @Nullable final Long restockableInDays) {
    assertThat(inventoryEntry.getQuantityOnStock()).isEqualTo(quantityOnStock);
    assertThat(inventoryEntry.getExpectedDelivery()).isEqualTo(expectedDelivery);
    assertThat(inventoryEntry.getRestockableInDays()).isEqualTo(restockableInDays);
  }
}
