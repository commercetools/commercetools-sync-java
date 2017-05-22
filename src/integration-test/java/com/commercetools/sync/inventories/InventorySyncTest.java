package com.commercetools.sync.inventories;

import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.Reference;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.SphereClientUtils.getCtpClientOfSourceProject;
import static com.commercetools.sync.commons.utils.SphereClientUtils.getCtpClientOfTargetProject;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.EXPECTED_DELIVERY_1;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.EXPECTED_DELIVERY_2;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.QUANTITY_ON_STOCK_1;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.QUANTITY_ON_STOCK_2;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.RESTOCKABLE_IN_DAYS_1;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.RESTOCKABLE_IN_DAYS_2;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.SKU_1;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.SKU_2;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.SUPPLY_CHANNEL_KEY_1;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.SUPPLY_CHANNEL_KEY_2;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.deleteInventoriesAndSupplyChannels;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.getInventoryEntryBySkuAndSupplyChannel;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.populateSourceProject;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.populateTargetProject;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contains integration tests of inventory sync.
 */
public class InventorySyncTest {

    private BlockingSphereClient targetProjectClient;
    private BlockingSphereClient sourceProjectClient;

    /**
     * Deletes inventories and supply channels from source and target CTP projects.
     * Populates source and target CTP projects with test data.
     */
    @Before
    public void setup() {
        this.sourceProjectClient = getCtpClientOfSourceProject().getClient();
        this.targetProjectClient = getCtpClientOfTargetProject().getClient();
        deleteInventoriesAndSupplyChannels();
        populateSourceProject();
        populateTargetProject();
    }

    @AfterClass
    public static void cleanup() {
        deleteInventoriesAndSupplyChannels();
    }

    @Test
    public void syncDrafts_WithUpdatedDraft_ShouldUpdateEntryInCtp() {
        //Make sure that old entry has correct values before sync.
        final Optional<InventoryEntry> oldInventoryBeforeSync =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1, null);
        assertThat(oldInventoryBeforeSync).isNotEmpty();
        assertValues(oldInventoryBeforeSync.get(), QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1);

        //Prepare sync data.
        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null)
            .build();
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(getCtpClientOfTargetProject())
            .build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

        //Sync and make sure that proper statistics were returned.
        final InventorySyncStatistics inventorySyncStatistics = inventorySync
            .syncDrafts(singletonList(newInventoryDraft))
            .toCompletableFuture()
            .join();
        assertStatistics(inventorySyncStatistics, 1, 0, 1, 0);

        //Make sure that old entry has correct values after sync.
        final Optional<InventoryEntry> oldInventoryAfterSync =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1, null);
        assertThat(oldInventoryAfterSync).isNotEmpty();
        assertValues(oldInventoryAfterSync.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);
    }

    @Test
    public void syncDrafts_WithNewDraft_ShouldCreateDraftInCtp() {
        //Make sure that old entry has correct values before sync.
        final Optional<InventoryEntry> oldInventoryBeforeSync =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_2, null);
        assertThat(oldInventoryBeforeSync).isEmpty();

        //Prepare sync data.
        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraftBuilder
            .of(SKU_2, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null)
            .build();
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(getCtpClientOfTargetProject())
            .build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

        //Sync and make sure that proper statistics were returned.
        final InventorySyncStatistics inventorySyncStatistics = inventorySync
            .syncDrafts(singletonList(newInventoryDraft))
            .toCompletableFuture()
            .join();
        assertStatistics(inventorySyncStatistics, 1, 1, 0, 0);

        //Make sure that old entry has correct values after sync.
        final Optional<InventoryEntry> oldInventoryAfterSync =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_2, null);
        assertThat(oldInventoryAfterSync).isNotEmpty();
        assertValues(oldInventoryAfterSync.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);
    }

    /**
     * This test is additionally a showcase of providing valid input to {@link InventorySync#syncDrafts(List)}.
     * In this case it is attempted to update old inventory entry of SKU and supply channel.
     * Draft given in input would have reference to an expected channel. Mentioned reference will be expanded.
     */
    @Test
    public void syncDrafts_WithExpandedReferenceToExistingSupplyChannel_ShouldUpdateEntry() {
        //Fetch existing Channel of key SUPPLY_CHANNEL_KEY_1 from target project.
        final ChannelQuery supplyChannelQuery = ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_1);
        final Optional<Channel> supplyChannel = targetProjectClient
            .executeBlocking(supplyChannelQuery)
            .head();
        assertThat(supplyChannel).isNotEmpty();

        //Make Reference from fetched Channel. Ensure that it is expanded.
        final Reference<Channel> supplyChannelReference = supplyChannel.get().toReference();
        assertThat(supplyChannelReference.getObj()).isNotNull();
        assertThat(supplyChannelReference.getObj().getKey()).isEqualTo(SUPPLY_CHANNEL_KEY_1);

        //Prepare InventoryEntryDraft of sku SKU_1 and reference to above supply channel key.
        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, supplyChannelReference)
            .build();

        //Ensure old entry values before sync.
        final Optional<InventoryEntry> oldInventoryBeforeSync =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1, supplyChannelReference);
        assertThat(oldInventoryBeforeSync).isPresent();
        assertThat(oldInventoryBeforeSync.get().getQuantityOnStock()).isEqualTo(QUANTITY_ON_STOCK_1);
        assertThat(oldInventoryBeforeSync.get().getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY_1);
        assertThat(oldInventoryBeforeSync.get().getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS_1);
        assertThat(oldInventoryBeforeSync.get().getSupplyChannel().getId()).isEqualTo(supplyChannelReference.getId());

        //Prepare sync options and perform sync of draft to "target project.
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(getCtpClientOfTargetProject())
            .build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
        final InventorySyncStatistics inventorySyncStatistics = inventorySync
            .syncDrafts(singletonList(newInventoryDraft))
            .toCompletableFuture()
            .join();
        assertStatistics(inventorySyncStatistics, 1, 0, 1, 0);

        //Ensure old entry values after sync.
        final Optional<InventoryEntry> oldInventoryAfterSync =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1, supplyChannelReference);
        assertThat(oldInventoryAfterSync).isPresent();
        assertThat(oldInventoryAfterSync.get().getQuantityOnStock()).isEqualTo(QUANTITY_ON_STOCK_2);
        assertThat(oldInventoryAfterSync.get().getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY_2);
        assertThat(oldInventoryAfterSync.get().getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS_2);
        assertThat(oldInventoryAfterSync.get().getSupplyChannel().getId()).isEqualTo(supplyChannelReference.getId());
    }

    /**
     * This test is additionally a showcase of providing valid input to {@link InventorySync#syncDrafts(List)}.
     * In this case it is attempted to update old inventory entry of SKU and supply channel.
     * Draft given in input would have reference to an expected channel. Mentioned reference won't be expanded, but it
     * will hold channel `key` in place of referece `id`
     */
    @Test
    public void syncDrafts_WithKeyToExistingSupplyChannelInPlaceOfReferenceId_ShouldUpdateEntry() {
        /*
         * Fetch existing Channel of key SUPPLY_CHANNEL_KEY_1 from target project.
         * This is done only for test assertion reasons, not necessary for sync.
         */
        final ChannelQuery supplyChannelQuery = ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_1);
        final Optional<Channel> supplyChannel = targetProjectClient
            .executeBlocking(supplyChannelQuery)
            .head();
        assertThat(supplyChannel).isNotEmpty();

        /*
         * Prepare InventoryEntryDraft of sku SKU_1 and reference to supply channel of key SUPPLY_CHANNEL_KEY_1.
         * Please note that the key is provided in place of Referenced id.
         */
        final Reference<Channel> supplyChannelReference = Channel.referenceOfId(SUPPLY_CHANNEL_KEY_1);
        assertThat(supplyChannelReference.getObj()).isNull();
        assertThat(supplyChannelReference.getId()).isEqualTo(SUPPLY_CHANNEL_KEY_1);

        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, supplyChannelReference)
            .build();

        //Ensure old entry values before sync.
        final Optional<InventoryEntry> oldInventoryBeforeSync =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1,
                supplyChannel.get().toReference());
        assertThat(oldInventoryBeforeSync).isPresent();
        assertThat(oldInventoryBeforeSync.get().getQuantityOnStock()).isEqualTo(QUANTITY_ON_STOCK_1);
        assertThat(oldInventoryBeforeSync.get().getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY_1);
        assertThat(oldInventoryBeforeSync.get().getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS_1);

        //Prepare sync options and perform sync of draft to target project.
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(getCtpClientOfTargetProject())
            .build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
        final InventorySyncStatistics inventorySyncStatistics = inventorySync
            .syncDrafts(singletonList(newInventoryDraft))
            .toCompletableFuture()
            .join();
        assertStatistics(inventorySyncStatistics, 1, 0, 1, 0);

        //Ensure old entry values after sync.
        final Optional<InventoryEntry> oldInventoryAfterSync =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1,
                supplyChannel.get().toReference());
        assertThat(oldInventoryAfterSync).isPresent();
        assertThat(oldInventoryAfterSync.get().getQuantityOnStock()).isEqualTo(QUANTITY_ON_STOCK_2);
        assertThat(oldInventoryAfterSync.get().getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY_2);
        assertThat(oldInventoryAfterSync.get().getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS_2);
    }

    @Test
    public void syncDrafts_WithNewSupplyChannelAndChannelsEnsured_ShouldCreateNewSupplyChannelInCtp() {
        //Make sure that supply channel doesn't exist before sync.
        final ChannelQuery oldSupplyChannelQuery = ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_2);
        final Optional<Channel> oldSupplyChannelBeforeSync = getCtpClientOfTargetProject().getClient()
            .executeBlocking(oldSupplyChannelQuery)
            .head();
        assertThat(oldSupplyChannelBeforeSync).isEmpty();

        //Prepare sync data.
        final Reference<Channel> newSupplyChannelReference = Channel.referenceOfId(SUPPLY_CHANNEL_KEY_2);
        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, newSupplyChannelReference)
            .build();
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(getCtpClientOfTargetProject())
            .ensureChannels(true)
            .build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

        //Sync and make sure that proper statistics were returned.
        final InventorySyncStatistics inventorySyncStatistics = inventorySync
            .syncDrafts(singletonList(newInventoryDraft))
            .toCompletableFuture()
            .join();
        assertStatistics(inventorySyncStatistics, 1, 1, 0, 0);

        //Make sure that supply channel exists before sync.
        final Optional<Channel> oldSupplyChannelAfterSync = getCtpClientOfTargetProject().getClient()
            .executeBlocking(oldSupplyChannelQuery)
            .head();
        assertThat(oldSupplyChannelAfterSync).isNotEmpty();
        assertThat(oldSupplyChannelAfterSync.get().getKey()).isEqualTo(SUPPLY_CHANNEL_KEY_2);
    }

    @Test
    public void sync_WithoutEnsureChannels_ShouldCreateOrUpdateEntriesWithExistingOrNoChannels() {
        //Fetch new inventory entries from source project.
        final List<InventoryEntry> newInventories = sourceProjectClient.executeBlocking(
            InventoryEntryQuery.of().withExpansionPaths(model -> model.supplyChannel())
        ).getResults();
        assertThat(newInventories.size()).isEqualTo(3);

        //Prepare sync instance.
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(getCtpClientOfTargetProject())
            .build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

        //Sync and make sure that proper statistics were returned.
        final InventorySyncStatistics inventorySyncStatistics = inventorySync.sync(newInventories)
            .toCompletableFuture()
            .join();
        assertStatistics(inventorySyncStatistics, 3, 0, 1, 1);

        //Make sure that ctp state after sync is correct.
        //Make sure there exists 2 inventory entries.
        final List<InventoryEntry> oldInventoriesAfterSync = targetProjectClient.executeBlocking(
            InventoryEntryQuery.of().withExpansionPaths(model -> model.supplyChannel())
        ).getResults();
        assertThat(oldInventoriesAfterSync.size()).isEqualTo(2);

        //Make sure that inventory of SKU_1 wasn't changed.
        Optional<InventoryEntry> oldInventoryAfterSync =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1, null);
        assertThat(oldInventoryAfterSync).isNotEmpty();
        assertValues(oldInventoryAfterSync.get(), QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1);

        //Make sure that channel of SUPPLY_CHANNEL_KEY_1 exists.
        Optional<Channel> oldSupplyChannelAfterSync = getCtpClientOfTargetProject().getClient()
            .executeBlocking(ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_1))
            .head();
        assertThat(oldSupplyChannelAfterSync).isNotEmpty();

        //Make sure that inventory of SKU_1 and channel SUPPLY_CHANNEL_KEY_1 was updated.
        oldInventoryAfterSync = getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1,
            Channel.referenceOfId(oldSupplyChannelAfterSync.get().getId()));
        assertThat(oldInventoryAfterSync).isNotEmpty();
        assertValues(oldInventoryAfterSync.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);

        //Make sure that channel of SUPPLY_CHANNEL_KEY_2 doesn't exist.
        oldSupplyChannelAfterSync = getCtpClientOfTargetProject().getClient()
            .executeBlocking(ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_2))
            .head();
        assertThat(oldSupplyChannelAfterSync).isEmpty();
    }

    @Test
    public void sync_WithEnsureChannels_ShouldCreateOrUpdateEntries() {
        //Fetch new inventory entries from source project.
        final List<InventoryEntry> newInventories = sourceProjectClient.executeBlocking(
            InventoryEntryQuery.of().withExpansionPaths(model -> model.supplyChannel())
        ).getResults();
        assertThat(newInventories.size()).isEqualTo(3);

        //Prepare sync instance.
        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(getCtpClientOfTargetProject())
            .ensureChannels(true)
            .build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

        //Sync and make sure that proper statistics were returned.
        final InventorySyncStatistics inventorySyncStatistics = inventorySync.sync(newInventories)
            .toCompletableFuture()
            .join();
        assertStatistics(inventorySyncStatistics, 3, 1, 1, 0);

        //Make sure that ctp state after sync is correct.
        //Make sure there exists 3 inventory entries.
        final List<InventoryEntry> oldInventoriesAfterSync = targetProjectClient.executeBlocking(
            InventoryEntryQuery.of().withExpansionPaths(model -> model.supplyChannel())
        ).getResults();
        assertThat(oldInventoriesAfterSync.size()).isEqualTo(3);

        //Make sure that inventory of SKU_1 wasn't changed.
        Optional<InventoryEntry> oldInventoryAfterSync =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1, null);
        assertThat(oldInventoryAfterSync).isNotEmpty();
        assertValues(oldInventoryAfterSync.get(), QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1);

        //Make sure that channel of SUPPLY_CHANNEL_KEY_1 exists.
        Optional<Channel> oldSupplyChannelAfterSync = getCtpClientOfTargetProject().getClient()
            .executeBlocking(ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_1))
            .head();
        assertThat(oldSupplyChannelAfterSync).isNotEmpty();

        //Make sure that inventory of SKU_1 and channel SUPPLY_CHANNEL_KEY_1 was updated.
        oldInventoryAfterSync = getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1,
            Channel.referenceOfId(oldSupplyChannelAfterSync.get().getId()));
        assertThat(oldInventoryAfterSync).isNotEmpty();
        assertValues(oldInventoryAfterSync.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);

        //Make sure that channel of SUPPLY_CHANNEL_KEY_2 exists.
        oldSupplyChannelAfterSync = getCtpClientOfTargetProject().getClient()
            .executeBlocking(ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_2))
            .head();
        assertThat(oldSupplyChannelAfterSync).isNotEmpty();

        //Make sure that inventory of SKU_1 and channel SUPPLY_CHANNEL_KEY_2 was created.
        oldInventoryAfterSync = getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1,
            Channel.referenceOfId(oldSupplyChannelAfterSync.get().getId()));
        assertThat(oldInventoryAfterSync).isNotEmpty();
        assertValues(oldInventoryAfterSync.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);
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
