package com.commercetools.sync.integration.inventories;

import com.commercetools.sync.inventories.InventorySync;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.QueueSphereClientDecorator;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.expansion.InventoryEntryExpansionModel;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.Reference;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.EXPECTED_DELIVERY_1;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.EXPECTED_DELIVERY_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.QUANTITY_ON_STOCK_1;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.QUANTITY_ON_STOCK_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.RESTOCKABLE_IN_DAYS_1;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.RESTOCKABLE_IN_DAYS_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.SKU_1;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.SKU_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.SUPPLY_CHANNEL_KEY_1;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.SUPPLY_CHANNEL_KEY_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.deleteChannelsFromTargetAndSource;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.deleteInventoryEntriesFromTargetAndSource;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.getChannelByKey;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.getInventoryEntryBySkuAndSupplyChannel;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.populateSourceProject;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.populateTargetProject;
import static com.commercetools.sync.inventories.utils.InventoryReferenceReplacementUtils.replaceInventoriesReferenceIdsWithKeys;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contains integration tests of inventory sync.
 */
public class InventorySyncIT {

    /**
     * Deletes inventories and supply channels from source and target CTP projects.
     * Populates source and target CTP projects with test data.
     */
    @Before
    public void setup() {
        deleteInventoryEntriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
        deleteChannelsFromTargetAndSource();
        populateSourceProject();
        populateTargetProject();
    }

    /**
     * Deletes all the test data from the {@code CTP_SOURCE_CLIENT} and the {@code CTP_SOURCE_CLIENT} projects that
     * were set up in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteInventoryEntriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
        deleteChannelsFromTargetAndSource();
    }

    @Test
    public void sync_WithUpdatedInventory_ShouldUpdateInventory() {
        //Ensure that old entry has correct values before sync.
        final Optional<InventoryEntry> oldInventoryBeforeSync =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null);
        assertThat(oldInventoryBeforeSync).isNotEmpty();
        assertValues(oldInventoryBeforeSync.get(), QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1);

        //Prepare sync data.
        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null).build();
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

        //Sync and ensure that proper statistics were returned.
        final InventorySyncStatistics inventorySyncStatistics = inventorySync.sync(singletonList(newInventoryDraft))
            .toCompletableFuture().join();
        assertStatistics(inventorySyncStatistics, 1, 0, 1, 0);

        //Ensure that old entry has correct values after sync.
        final Optional<InventoryEntry> oldInventoryAfterSync =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null);
        assertThat(oldInventoryAfterSync).isNotEmpty();
        assertValues(oldInventoryAfterSync.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);
    }

    @Test
    public void sync_WithNewInventory_ShouldCreateInventory() {
        //Ensure that old entry has correct values before sync.
        final Optional<InventoryEntry> oldInventoryBeforeSync =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_2, null);
        assertThat(oldInventoryBeforeSync).isEmpty();

        //Prepare sync data.
        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraftBuilder
            .of(SKU_2, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null).build();
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

        //Sync and ensure that proper statistics were returned.
        final InventorySyncStatistics inventorySyncStatistics = inventorySync.sync(singletonList(newInventoryDraft))
            .toCompletableFuture().join();
        assertStatistics(inventorySyncStatistics, 1, 1, 0, 0);

        //Ensure that old entry has correct values after sync.
        final Optional<InventoryEntry> oldInventoryAfterSync =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_2, null);
        assertThat(oldInventoryAfterSync).isNotEmpty();
        assertValues(oldInventoryAfterSync.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);
    }

    @Test
    public void sync_WithExpandedReferenceToExistingSupplyChannel_ShouldUpdateInventory() {
        //Fetch existing Channel of key SUPPLY_CHANNEL_KEY_1 from target project.
        final Optional<Channel> supplyChannel = getChannelByKey(CTP_TARGET_CLIENT, SUPPLY_CHANNEL_KEY_1);
        assertThat(supplyChannel).isNotEmpty();

        //Make Reference from fetched Channel. Ensure that it is expanded.
        final Reference<Channel> supplyChannelReference = supplyChannel.get().toReference();
        assertThat(supplyChannelReference.getObj()).isNotNull();
        assertThat(supplyChannelReference.getObj().getKey()).isEqualTo(SUPPLY_CHANNEL_KEY_1);

        //Prepare InventoryEntryDraft of sku SKU_1 and reference to above supply channel key.
        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, supplyChannelReference).build();

        //Ensure old entry values before sync.
        final Optional<InventoryEntry> oldInventoryBeforeSync =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, supplyChannelReference);
        assertThat(oldInventoryBeforeSync).isPresent();
        assertValues(oldInventoryBeforeSync.get(), QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1);
        assertThat(oldInventoryBeforeSync.get().getSupplyChannel().getId()).isEqualTo(supplyChannelReference.getId());

        //Prepare sync options and perform sync of draft to target project.
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
        final InventorySyncStatistics inventorySyncStatistics = inventorySync.sync(singletonList(newInventoryDraft))
            .toCompletableFuture().join();
        assertStatistics(inventorySyncStatistics, 1, 0, 1, 0);

        //Ensure old entry values after sync.
        final Optional<InventoryEntry> oldInventoryAfterSync =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, supplyChannelReference);
        assertThat(oldInventoryAfterSync).isPresent();
        assertValues(oldInventoryAfterSync.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);
        assertThat(oldInventoryAfterSync.get().getSupplyChannel().getId()).isEqualTo(supplyChannelReference.getId());
    }

    @Test
    public void sync_WithKeyToExistingSupplyChannelInPlaceOfReferenceId_ShouldUpdateInventory() {
        /*
         * Fetch existing Channel of key SUPPLY_CHANNEL_KEY_1 from target project.
         * This is done only for test assertion reasons, not necessary for sync.
         */
        final Optional<Channel> supplyChannel = getChannelByKey(CTP_TARGET_CLIENT, SUPPLY_CHANNEL_KEY_1);
        assertThat(supplyChannel).isNotEmpty();

        /*
         * Prepare InventoryEntryDraft of sku SKU_1 and reference to supply channel of key SUPPLY_CHANNEL_KEY_1.
         * Please note that the key is provided in place of Referenced id.
         */
        final Reference<Channel> supplyChannelReference = Channel.referenceOfId(SUPPLY_CHANNEL_KEY_1);
        assertThat(supplyChannelReference.getObj()).isNull();
        assertThat(supplyChannelReference.getId()).isEqualTo(SUPPLY_CHANNEL_KEY_1);

        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, supplyChannelReference).build();

        //Ensure old entry values before sync.
        final Optional<InventoryEntry> oldInventoryBeforeSync =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, supplyChannel.get().toReference());
        assertThat(oldInventoryBeforeSync).isPresent();
        assertValues(oldInventoryBeforeSync.get(), QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1);

        //Prepare sync options and perform sync of draft to target project.
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
        final InventorySyncStatistics inventorySyncStatistics = inventorySync.sync(singletonList(newInventoryDraft))
            .toCompletableFuture().join();
        assertStatistics(inventorySyncStatistics, 1, 0, 1, 0);

        //Ensure old entry values after sync.
        final Optional<InventoryEntry> oldInventoryAfterSync =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, supplyChannel.get().toReference());
        assertThat(oldInventoryAfterSync).isPresent();
        assertValues(oldInventoryAfterSync.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);
    }

    @Test
    public void sync_WithNewSupplyChannelAndChannelsEnsured_ShouldCreateNewSupplyChannel() {
        //Ensure that supply channel doesn't exist before sync.
        final Optional<Channel> oldSupplyChannelBeforeSync = getChannelByKey(CTP_TARGET_CLIENT, SUPPLY_CHANNEL_KEY_2);
        assertThat(oldSupplyChannelBeforeSync).isEmpty();

        //Prepare sync data.
        final Reference<Channel> newSupplyChannelReference = Channel.referenceOfId(SUPPLY_CHANNEL_KEY_2);
        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, newSupplyChannelReference)
            .build();
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .ensureChannels(true).build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

        //Sync and ensure that proper statistics were returned.
        final InventorySyncStatistics inventorySyncStatistics = inventorySync.sync(singletonList(newInventoryDraft))
            .toCompletableFuture().join();
        assertStatistics(inventorySyncStatistics, 1, 1, 0, 0);

        //Ensure that supply channel exists before sync.
        final Optional<Channel> oldSupplyChannelAfterSync = getChannelByKey(CTP_TARGET_CLIENT, SUPPLY_CHANNEL_KEY_2);
        assertThat(oldSupplyChannelAfterSync).isNotEmpty();
        assertThat(oldSupplyChannelAfterSync.get().getKey()).isEqualTo(SUPPLY_CHANNEL_KEY_2);
    }

    @Test
    public void sync_WithExpandedReferencesToSourceChannels_ShouldUpdateInventoriesWithoutChannelCreation() {
        //Ensure channels in target project before sync
        final ChannelQuery targetChannelsQuery = ChannelQuery.of().withPredicates(channelQueryModel ->
                channelQueryModel.roles().containsAny(singletonList(ChannelRole.INVENTORY_SUPPLY)));
        final List<Channel> targetChannelsBeforeSync = CTP_TARGET_CLIENT.execute(targetChannelsQuery)
            .toCompletableFuture().join().getResults();
        assertThat(targetChannelsBeforeSync).isNotEmpty();
        assertThat(targetChannelsBeforeSync).hasSize(1);
        assertThat(targetChannelsBeforeSync.get(0).getKey()).isEqualTo(SUPPLY_CHANNEL_KEY_1);

        //Fetch existing Channel of key SUPPLY_CHANNEL_KEY_1 from source project.
        final Optional<Channel> sourceSupplyChannel = getChannelByKey(CTP_SOURCE_CLIENT, SUPPLY_CHANNEL_KEY_1);
        assertThat(sourceSupplyChannel).isNotEmpty();

        //Make Reference from fetched Channel. Ensure that it is expanded.
        final Reference<Channel> sourceChannelReference = sourceSupplyChannel.get().toReference();
        assertThat(sourceChannelReference.getObj()).isNotNull();
        assertThat(sourceChannelReference.getObj().getKey()).isEqualTo(SUPPLY_CHANNEL_KEY_1);

        //Prepare InventoryEntryDraft of sku SKU_1 and reference to above supply channel key.
        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, sourceChannelReference).build();

        //Fetch existing Channel of key SUPPLY_CHANNEL_KEY_1 from target project.
        final Optional<Channel> targetSupplyChannel = getChannelByKey(CTP_TARGET_CLIENT, SUPPLY_CHANNEL_KEY_1);
        assertThat(targetSupplyChannel).isNotEmpty();
        final Reference<Channel> targetChannelReference = targetSupplyChannel.get().toReference();

        //Ensure old entry values before sync.
        final Optional<InventoryEntry> oldInventoryBeforeSync =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, targetChannelReference);
        assertThat(oldInventoryBeforeSync).isPresent();
        assertValues(oldInventoryBeforeSync.get(), QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1);
        assertThat(oldInventoryBeforeSync.get().getSupplyChannel().getId()).isEqualTo(targetChannelReference.getId());

        //Prepare sync options and perform sync of draft to target project.
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
        final InventorySyncStatistics inventorySyncStatistics = inventorySync.sync(singletonList(newInventoryDraft))
            .toCompletableFuture().join();
        assertStatistics(inventorySyncStatistics, 1, 0, 1, 0);

        //Ensure old entry values after sync.
        final Optional<InventoryEntry> oldInventoryAfterSync =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, targetChannelReference);
        assertThat(oldInventoryAfterSync).isPresent();
        assertValues(oldInventoryAfterSync.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);
        assertThat(oldInventoryAfterSync.get().getSupplyChannel().getId()).isEqualTo(targetChannelReference.getId());

        //Ensure channels in target project after sync.
        final List<Channel> targetChannelsAfterSync = CTP_TARGET_CLIENT.execute(targetChannelsQuery)
            .toCompletableFuture().join().getResults();
        assertThat(targetChannelsAfterSync).isNotEmpty();
        assertThat(targetChannelsAfterSync).hasSize(1);
        assertThat(targetChannelsAfterSync.get(0)).isEqualTo(targetChannelsBeforeSync.get(0));
    }

    @Test
    public void sync_FromSourceToTargetProjectWithChannelsEnsured_ShouldReturnProperStatistics() {
        //Fetch new inventories from source project. Convert them to drafts.
        final List<InventoryEntry> inventoryEntries = CTP_SOURCE_CLIENT
            .execute(InventoryEntryQuery.of()
                                        .withExpansionPaths(InventoryEntryExpansionModel::supplyChannel)
                                        .plusExpansionPaths(ExpansionPath.of("custom.type")))
            .toCompletableFuture().join().getResults();

        final List<InventoryEntryDraft> newInventories = replaceInventoriesReferenceIdsWithKeys(inventoryEntries);

        //Prepare sync options and perform sync of draft to target project.
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .ensureChannels(true).build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
        final InventorySyncStatistics inventorySyncStatistics = inventorySync.sync(newInventories).toCompletableFuture()
            .join();
        assertStatistics(inventorySyncStatistics, 3, 1,1, 0);
    }

    @Test
    public void sync_FromSourceToTargetWithoutChannelsEnsured_ShouldReturnProperStatistics() {
        //Fetch new inventories from source project. Convert them to drafts.
        final List<InventoryEntry> inventoryEntries = CTP_SOURCE_CLIENT
            .execute(InventoryEntryQuery.of()
                                        .withExpansionPaths(InventoryEntryExpansionModel::supplyChannel)
                                        .plusExpansionPaths(ExpansionPath.of("custom.type")))
            .toCompletableFuture().join().getResults();

        final List<InventoryEntryDraft> newInventories = replaceInventoriesReferenceIdsWithKeys(inventoryEntries);

        //Prepare sync options and perform sync of draft to target project.
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .ensureChannels(false).build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
        final InventorySyncStatistics inventorySyncStatistics = inventorySync.sync(newInventories).toCompletableFuture()
            .join();
        assertStatistics(inventorySyncStatistics, 3, 0,1, 1);
    }

    @Ignore
    @Test
    public void sync_WithBatchProcessing_ShouldCreateAllGivenInventories() {
        //Ensure inventory entries amount in target project before sync.
        final List<InventoryEntry> oldEntriesBeforeSync = CTP_TARGET_CLIENT.execute(InventoryEntryQuery.of())
            .toCompletableFuture().join().getResults();
        assertThat(oldEntriesBeforeSync).hasSize(2);

        //Create 10 drafts of new inventories.
        final List<InventoryEntryDraft> newInventories = LongStream.range(0, 10)
            .mapToObj(l -> InventoryEntryDraftBuilder.of(String.valueOf(l), l).build()).collect(toList());

        //Set batch size of number less that new inventories size.
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .setBatchSize(3).build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

        //Perform sync and ensure its results.
        final InventorySyncStatistics inventorySyncStatistics = inventorySync.sync(newInventories).toCompletableFuture()
            .join();
        assertStatistics(inventorySyncStatistics, 10, 10, 0,0);

        //Ensure inventory entries amount in target project after sync.
        final List<InventoryEntry> oldEntriesAfterSync = CTP_TARGET_CLIENT.execute(InventoryEntryQuery.of())
            .toCompletableFuture().join().getResults();
        assertThat(oldEntriesAfterSync).hasSize(12);
    }

    @Test
    public void sync_WithSphereClientDecorator_ShouldReturnProperStatistics() {
        //Fetch new inventories from source project. Convert them to drafts.
        final List<InventoryEntry> inventoryEntries = CTP_SOURCE_CLIENT
            .execute(InventoryEntryQuery.of()
                                        .withExpansionPaths(InventoryEntryExpansionModel::supplyChannel)
                                        .plusExpansionPaths(ExpansionPath.of("custom.type")))
            .toCompletableFuture().join().getResults();

        final List<InventoryEntryDraft> newInventories = replaceInventoriesReferenceIdsWithKeys(inventoryEntries);

        /*
         * Prepare sync options and perform sync of draft to target project.
         * Use QueueSphereClientDecorator to limit parallelisation on CTP requests.
         */
        final SphereClient client = QueueSphereClientDecorator.of(CTP_TARGET_CLIENT, 2);
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(client)
            .ensureChannels(true).build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
        final InventorySyncStatistics inventorySyncStatistics = inventorySync.sync(newInventories).toCompletableFuture()
            .join();
        assertStatistics(inventorySyncStatistics, 3, 1,1, 0);
    }

    @Test
    public void sync_ShouldReturnProperStatisticsObject() {
        //Fetch new inventories from source project. Convert them to drafts.
        final List<InventoryEntry> inventoryEntries = CTP_SOURCE_CLIENT
            .execute(InventoryEntryQuery.of()
                                        .withExpansionPaths(InventoryEntryExpansionModel::supplyChannel)
                                        .plusExpansionPaths(ExpansionPath.of("custom.type")))
            .toCompletableFuture().join().getResults();

        final List<InventoryEntryDraft> newInventories = replaceInventoriesReferenceIdsWithKeys(inventoryEntries);

        //Prepare sync options and perform sync of draft to target project.
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .ensureChannels(true).build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
        final InventorySyncStatistics inventorySyncStatistics = inventorySync.sync(newInventories).toCompletableFuture()
            .join();
        assertStatistics(inventorySyncStatistics, 3, 1,1, 0);
        assertThat(inventorySyncStatistics.getProcessed()).isEqualTo(3);
        assertThat(inventorySyncStatistics.getCreated()).isEqualTo(1);
        assertThat(inventorySyncStatistics.getUpdated()).isEqualTo(1);
        assertThat(inventorySyncStatistics.getFailed()).isEqualTo(0);
        assertThat(inventorySyncStatistics.getLatestBatchProcessingTimeInMillis()).isGreaterThan(0L);
        assertThat(inventorySyncStatistics.getReportMessage())
            .isEqualTo("Summary: 3 inventory entries were processed in total (1 created, 1 updated and 0 failed to sync"
                + ").");
    }

    @Test
    public void sync_WithCustomErrorCallback_ShouldExecuteCallbackOnError() {
        //Fetch new inventories from source project. Convert them to drafts.
        final List<InventoryEntry> inventoryEntries = CTP_SOURCE_CLIENT
            .execute(InventoryEntryQuery.of()
                                        .withExpansionPaths(InventoryEntryExpansionModel::supplyChannel)
                                        .plusExpansionPaths(ExpansionPath.of("custom.type")))
            .toCompletableFuture().join().getResults();

        final List<InventoryEntryDraft> newInventories = replaceInventoriesReferenceIdsWithKeys(inventoryEntries);

        //Prepare sync options and perform sync of draft to target project.
        final AtomicInteger invocationCounter = new AtomicInteger(0);
        BiConsumer<String, Throwable> countingErrorCallback = (string, throwable) ->
            invocationCounter.incrementAndGet();
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .setErrorCallBack(countingErrorCallback)
            .ensureChannels(false).build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
        final InventorySyncStatistics inventorySyncStatistics = inventorySync.sync(newInventories).toCompletableFuture()
            .join();
        assertStatistics(inventorySyncStatistics, 3, 0,1, 1);
        assertThat(invocationCounter.get()).isEqualTo(1);
    }

    @Test
    public void sync_WithSyncDifferentBatchesConcurrently_ShouldReturnProperStatistics() {
        //Prepare new inventories.
        final List<InventoryEntryDraft> newDrafts = IntStream.range(100,160)
            .mapToObj(i -> InventoryEntryDraftBuilder.of(String.valueOf(i), QUANTITY_ON_STOCK_1).build())
            .collect(toList());

        //Split them to batches.
        final List<InventoryEntryDraft> firstBatch = newDrafts.subList(0,20);
        final List<InventoryEntryDraft> secondBatch = newDrafts.subList(20,40);
        final List<InventoryEntryDraft> thirdBatch = newDrafts.subList(40,60);

        //Initialize sync.
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

        //Run batch syncing concurrently.
        final CompletableFuture<InventorySyncStatistics> firstResult = inventorySync.sync(firstBatch)
            .toCompletableFuture();
        final CompletableFuture<InventorySyncStatistics> secondResult = inventorySync.sync(secondBatch)
            .toCompletableFuture();
        final CompletableFuture<InventorySyncStatistics> thirdResult = inventorySync.sync(thirdBatch)
            .toCompletableFuture();

        CompletableFuture.allOf(firstResult, secondResult, thirdResult).join();

        //Ensure instance's statistics.
        assertThat(inventorySync.getStatistics()).isNotNull();

        //TODO check distinct results when ISSUE #23 is resolved
        //TODO uncomment assertions below when ISSUE #23 is resolved (otherwise they may fail)

        //assertThat(inventorySync.getStatistics().getProcessed()).isEqualTo(60);
        //assertThat(inventorySync.getStatistics().getCreated()).isEqualTo(60);
        //assertThat(inventorySync.getStatistics().getUpdated()).isEqualTo(0);
        //assertThat(inventorySync.getStatistics().getFailed()).isEqualTo(0);
    }

    private void assertStatistics(@Nullable final InventorySyncStatistics statistics,
                                  final int expectedProcessed,
                                  final int expectedCreated,
                                  final int expectedUpdated,
                                  final int expectedFailed) {
        assertThat(statistics).isNotNull();
        assertThat(statistics.getProcessed()).isEqualTo(expectedProcessed);
        assertThat(statistics.getCreated()).isEqualTo(expectedCreated);
        assertThat(statistics.getUpdated()).isEqualTo(expectedUpdated);
        assertThat(statistics.getFailed()).isEqualTo(expectedFailed);
    }

    private void assertValues(@Nonnull final InventoryEntry inventoryEntry,
                              @Nonnull final Long quantityOnStock,
                              @Nullable final ZonedDateTime expectedDelivery,
                              @Nullable final Integer restockableInDays) {
        assertThat(inventoryEntry.getQuantityOnStock()).isEqualTo(quantityOnStock);
        assertThat(inventoryEntry.getExpectedDelivery()).isEqualTo(expectedDelivery);
        assertThat(inventoryEntry.getRestockableInDays()).isEqualTo(restockableInDays);
    }
}
