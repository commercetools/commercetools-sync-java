package com.commercetools.sync.integration.inventories;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ChannelITUtils.deleteChannelsFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.EXPECTED_DELIVERY_1;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.EXPECTED_DELIVERY_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.QUANTITY_ON_STOCK_1;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.QUANTITY_ON_STOCK_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.REFERENCE_ID_TO_KEY_CACHE;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.RESTOCKABLE_IN_DAYS_1;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.RESTOCKABLE_IN_DAYS_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.SKU_1;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.SKU_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.SUPPLY_CHANNEL_KEY_1;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.SUPPLY_CHANNEL_KEY_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.deleteInventoryEntriesFromTargetAndSource;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.getChannelByKey;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.getInventoryEntryBySkuAndSupplyChannel;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.populateSourceProject;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.populateTargetProject;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.inventories.InventorySync;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import com.commercetools.sync.inventories.utils.InventoryTransformUtils;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.expansion.InventoryEntryExpansionModel;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
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
import org.junit.jupiter.api.Disabled;
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
    deleteTypesFromTargetAndSource();
    deleteChannelsFromTargetAndSource();
    populateSourceProject();
    populateTargetProject();
  }

  /**
   * Deletes all the test data from the {@code CTP_SOURCE_CLIENT} and the {@code CTP_SOURCE_CLIENT}
   * projects that were set up in this test class.
   */
  @AfterAll
  static void tearDown() {
    deleteInventoryEntriesFromTargetAndSource();
    deleteTypesFromTargetAndSource();
    deleteChannelsFromTargetAndSource();
  }

  @Test
  void sync_WithUpdatedInventory_ShouldUpdateInventory() {
    // Ensure that old entry has correct values before sync.
    final Optional<InventoryEntry> oldInventoryBeforeSync =
        getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null);
    assertThat(oldInventoryBeforeSync).isNotEmpty();
    assertValues(
        oldInventoryBeforeSync.get(),
        QUANTITY_ON_STOCK_1,
        EXPECTED_DELIVERY_1,
        RESTOCKABLE_IN_DAYS_1);

    // Prepare sync data.
    final InventoryEntryDraft newInventoryDraft =
        InventoryEntryDraftBuilder.of(
                SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null)
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
        getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null);
    assertThat(oldInventoryAfterSync).isNotEmpty();
    assertValues(
        oldInventoryAfterSync.get(),
        QUANTITY_ON_STOCK_2,
        EXPECTED_DELIVERY_2,
        RESTOCKABLE_IN_DAYS_2);
  }

  @Test
  void sync_WithNewInventory_ShouldCreateInventory() {
    // Ensure that old entry has correct values before sync.
    final Optional<InventoryEntry> oldInventoryBeforeSync =
        getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_2, null);
    assertThat(oldInventoryBeforeSync).isEmpty();

    // Prepare sync data.
    final InventoryEntryDraft newInventoryDraft =
        InventoryEntryDraftBuilder.of(
                SKU_2, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null)
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
        getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_2, null);
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
    final ResourceIdentifier<Channel> supplyChannelReference =
        ResourceIdentifier.ofKey(SUPPLY_CHANNEL_KEY_1);

    final InventoryEntryDraft newInventoryDraft =
        InventoryEntryDraftBuilder.of(
                SKU_1,
                QUANTITY_ON_STOCK_2,
                EXPECTED_DELIVERY_2,
                RESTOCKABLE_IN_DAYS_2,
                supplyChannelReference)
            .build();

    // Ensure old entry values before sync.
    final Optional<InventoryEntry> oldInventoryBeforeSync =
        getInventoryEntryBySkuAndSupplyChannel(
            CTP_TARGET_CLIENT, SKU_1, supplyChannel.get().toReference());
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
            CTP_TARGET_CLIENT, SKU_1, supplyChannel.get().toReference());
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
    final ResourceIdentifier<Channel> newSupplyChannelReference =
        ResourceIdentifier.ofKey(SUPPLY_CHANNEL_KEY_2);
    final InventoryEntryDraft newInventoryDraft =
        InventoryEntryDraftBuilder.of(
                SKU_1,
                QUANTITY_ON_STOCK_2,
                EXPECTED_DELIVERY_2,
                RESTOCKABLE_IN_DAYS_2,
                newSupplyChannelReference)
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
    final ChannelQuery targetChannelsQuery =
        ChannelQuery.of()
            .withPredicates(
                channelQueryModel ->
                    channelQueryModel
                        .roles()
                        .containsAny(singletonList(ChannelRole.INVENTORY_SUPPLY)));
    final List<Channel> targetChannelsBeforeSync =
        CTP_TARGET_CLIENT.execute(targetChannelsQuery).toCompletableFuture().join().getResults();
    assertThat(targetChannelsBeforeSync).hasSize(1);
    assertThat(targetChannelsBeforeSync.get(0).getKey()).isEqualTo(SUPPLY_CHANNEL_KEY_1);

    // Prepare InventoryEntryDraft of sku SKU_1 and reference to above supply channel key.
    final InventoryEntryDraft newInventoryDraft =
        InventoryEntryDraftBuilder.of(
                SKU_1,
                QUANTITY_ON_STOCK_2,
                EXPECTED_DELIVERY_2,
                RESTOCKABLE_IN_DAYS_2,
                ResourceIdentifier.ofKey(SUPPLY_CHANNEL_KEY_1))
            .build();

    // Fetch existing Channel of key SUPPLY_CHANNEL_KEY_1 from target project.
    final Optional<Channel> targetSupplyChannel =
        getChannelByKey(CTP_TARGET_CLIENT, SUPPLY_CHANNEL_KEY_1);
    assertThat(targetSupplyChannel).isNotEmpty();
    final Reference<Channel> targetChannelReference = targetSupplyChannel.get().toReference();

    // Ensure old entry values before sync.
    final Optional<InventoryEntry> oldInventoryBeforeSync =
        getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, targetChannelReference);
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
        getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, targetChannelReference);
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
        CTP_TARGET_CLIENT.execute(targetChannelsQuery).toCompletableFuture().join().getResults();
    assertThat(targetChannelsAfterSync).isNotEmpty();
    assertThat(targetChannelsAfterSync).hasSize(1);
    assertThat(targetChannelsAfterSync.get(0)).isEqualTo(targetChannelsBeforeSync.get(0));
  }

  @Test
  void sync_FromSourceToTargetProjectWithChannelsEnsured_ShouldReturnProperStatistics() {
    // Fetch new inventories from source project. Convert them to drafts.
    final List<InventoryEntry> inventoryEntries =
        CTP_SOURCE_CLIENT
            .execute(
                InventoryEntryQuery.of()
                    .withExpansionPaths(InventoryEntryExpansionModel::supplyChannel)
                    .plusExpansionPaths(ExpansionPath.of("custom.type")))
            .toCompletableFuture()
            .join()
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
  }

  @Test
  void sync_FromSourceToTargetWithoutChannelsEnsured_ShouldReturnProperStatistics() {
    // Fetch new inventories from source project. Convert them to drafts.
    final List<InventoryEntry> inventoryEntries =
        CTP_SOURCE_CLIENT
            .execute(
                InventoryEntryQuery.of()
                    .withExpansionPaths(InventoryEntryExpansionModel::supplyChannel)
                    .plusExpansionPaths(ExpansionPath.of("custom.type")))
            .toCompletableFuture()
            .join()
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
    assertThat(inventorySyncStatistics).hasValues(3, 0, 1, 1);
  }

  @Disabled
  @Test
  void sync_WithBatchProcessing_ShouldCreateAllGivenInventories() {
    // Ensure inventory entries amount in target project before sync.
    final List<InventoryEntry> oldEntriesBeforeSync =
        CTP_TARGET_CLIENT
            .execute(InventoryEntryQuery.of())
            .toCompletableFuture()
            .join()
            .getResults();
    assertThat(oldEntriesBeforeSync).hasSize(2);

    // Create 10 drafts of new inventories.
    final List<InventoryEntryDraft> newInventories =
        LongStream.range(0, 10)
            .mapToObj(l -> InventoryEntryDraftBuilder.of(String.valueOf(l), l).build())
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
            .execute(InventoryEntryQuery.of())
            .toCompletableFuture()
            .join()
            .getResults();
    assertThat(oldEntriesAfterSync).hasSize(12);
  }

  @Test
  void sync_ShouldReturnProperStatisticsObject() {
    // Fetch new inventories from source project. Convert them to drafts.
    final List<InventoryEntry> inventoryEntries =
        CTP_SOURCE_CLIENT
            .execute(
                InventoryEntryQuery.of()
                    .withExpansionPaths(InventoryEntryExpansionModel::supplyChannel)
                    .plusExpansionPaths(ExpansionPath.of("custom.type")))
            .toCompletableFuture()
            .join()
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
            .execute(
                InventoryEntryQuery.of()
                    .withExpansionPaths(InventoryEntryExpansionModel::supplyChannel)
                    .plusExpansionPaths(ExpansionPath.of("custom.type")))
            .toCompletableFuture()
            .join()
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
            List<UpdateAction<InventoryEntry>>>
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
                i -> InventoryEntryDraftBuilder.of(String.valueOf(i), QUANTITY_ON_STOCK_1).build())
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
      @Nullable final Integer restockableInDays) {
    assertThat(inventoryEntry.getQuantityOnStock()).isEqualTo(quantityOnStock);
    assertThat(inventoryEntry.getExpectedDelivery()).isEqualTo(expectedDelivery);
    assertThat(inventoryEntry.getRestockableInDays()).isEqualTo(restockableInDays);
  }
}
