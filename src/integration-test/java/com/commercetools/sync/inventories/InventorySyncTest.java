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
        //assert ctp entry before sync
        Optional<InventoryEntry> existingEntry =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1, null);
        assertThat(existingEntry).isNotEmpty();
        assertValues(existingEntry.get(), QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1);

        //prepare sync data
        final InventoryEntryDraft draftToSync = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null)
            .build();
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(getCtpClientOfTargetProject())
            .build();
        final InventorySync syncer = new InventorySync(options);

        //sync and assert sync result
        final InventorySyncStatistics syncResult = syncer.syncDrafts(singletonList(draftToSync))
            .toCompletableFuture()
            .join();
        assertStatistics(syncResult, 1, 0, 1, 0);

        //assert ctp entry after sync
        existingEntry = getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1, null);
        assertThat(existingEntry).isNotEmpty();
        assertValues(existingEntry.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);
    }

    @Test
    public void syncDrafts_WithNewDraft_ShouldCreateDraftInCtp() {
        //assert ctp entry before sync
        Optional<InventoryEntry> existingEntry =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_2, null);
        assertThat(existingEntry).isEmpty();

        //prepare sync data
        final InventoryEntryDraft draftToSync = InventoryEntryDraftBuilder
            .of(SKU_2, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null)
            .build();
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(getCtpClientOfTargetProject())
            .build();
        final InventorySync syncer = new InventorySync(options);

        //sync and assert sync result
        final InventorySyncStatistics syncResult = syncer.syncDrafts(singletonList(draftToSync))
            .toCompletableFuture()
            .join();
        assertStatistics(syncResult, 1, 1, 0, 0);

        //assert ctp entry after sync
        existingEntry = getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_2, null);
        assertThat(existingEntry).isNotEmpty();
        assertValues(existingEntry.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);
    }

    @Test
    public void syncDrafts_WithNewSupplyChannelAndChannelsEnsured_ShouldCreateNewSupplyChannelInCtp() {
        //assert ctp channel before sync
        Optional<Channel> existingChannel = getCtpClientOfTargetProject().getClient()
            .executeBlocking(ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_2))
            .head();
        assertThat(existingChannel).isEmpty();

        //prepare sync data
        final Reference<Channel> channelToSync = Channel.referenceOfId(SUPPLY_CHANNEL_KEY_2);
        final InventoryEntryDraft draftToSync = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, channelToSync)
            .build();
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(getCtpClientOfTargetProject())
            .ensureChannels(true)
            .build();
        final InventorySync syncer = new InventorySync(options);

        //sync and assert sync result
        final InventorySyncStatistics syncResult = syncer.syncDrafts(singletonList(draftToSync))
            .toCompletableFuture()
            .join();
        assertStatistics(syncResult, 1, 1, 0, 0);

        //assert ctp channel after sync
        assertStatistics(syncResult, 1, 1, 0, 0);
        existingChannel = getCtpClientOfTargetProject().getClient()
            .executeBlocking(ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_2))
            .head();
        assertThat(existingChannel).isNotEmpty();
    }

    @Test
    public void sync_WithoutEnsureChannels_ShouldCreateOrUpdateEntriesWithExistingOrNoChannels() {
        //assert ctp channel before sync
        final List<InventoryEntry> sourceInventoryEntries = sourceProjectClient.executeBlocking(
            InventoryEntryQuery.of().withExpansionPaths(model -> model.supplyChannel())
        ).getResults();
        assertThat(sourceInventoryEntries.size()).isEqualTo(3);

        //prepare sync data
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(getCtpClientOfTargetProject())
            .build();
        final InventorySync syncer = new InventorySync(options);

        //sync and assert sync result
        final InventorySyncStatistics syncResult = syncer.sync(sourceInventoryEntries)
            .toCompletableFuture()
            .join();
        assertStatistics(syncResult, 3, 0, 1, 1);

        //assert ctp state after sync
        //assert there exists 2 inventory entries
        final List<InventoryEntry> targetInventoryEntries = targetProjectClient.executeBlocking(
            InventoryEntryQuery.of().withExpansionPaths(model -> model.supplyChannel())
        ).getResults();
        assertThat(targetInventoryEntries.size()).isEqualTo(2);

        //assert inventory of SKU_1 wasn't changed
        Optional<InventoryEntry> existingEntry =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1, null);
        assertThat(existingEntry).isNotEmpty();
        assertValues(existingEntry.get(), QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1);

        //assert channel of SUPPLY_CHANNEL_KEY_1 exists
        Optional<Channel> existingChannel = getCtpClientOfTargetProject().getClient()
            .executeBlocking(ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_1))
            .head();
        assertThat(existingChannel).isNotEmpty();

        //assert inventory of SKU_1 and channel SUPPLY_CHANNEL_KEY_1 was updated
        existingEntry = getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1,
            Channel.referenceOfId(existingChannel.get().getId()));
        assertThat(existingEntry).isNotEmpty();
        assertValues(existingEntry.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);

        //assert channel of SUPPLY_CHANNEL_KEY_2 doesn't exist
        existingChannel = getCtpClientOfTargetProject().getClient()
            .executeBlocking(ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_2))
            .head();
        assertThat(existingChannel).isEmpty();
    }

    @Test
    public void sync_WithEnsureChannels_ShouldCreateOrUpdateEntries() {
        //assert ctp channel before sync
        final List<InventoryEntry> sourceInventoryEntries = sourceProjectClient.executeBlocking(
            InventoryEntryQuery.of().withExpansionPaths(model -> model.supplyChannel())
        ).getResults();
        assertThat(sourceInventoryEntries.size()).isEqualTo(3);

        //prepare sync data
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(getCtpClientOfTargetProject())
            .ensureChannels(true)
            .build();
        final InventorySync syncer = new InventorySync(options);

        //sync and assert sync result
        final InventorySyncStatistics syncResult = syncer.sync(sourceInventoryEntries)
            .toCompletableFuture()
            .join();
        assertStatistics(syncResult, 3, 1, 1, 0);

        //assert ctp state after sync
        //assert there exists 3 inventory entries
        final List<InventoryEntry> targetInventoryEntries = targetProjectClient.executeBlocking(
            InventoryEntryQuery.of().withExpansionPaths(model -> model.supplyChannel())
        ).getResults();
        assertThat(targetInventoryEntries.size()).isEqualTo(3);

        //assert inventory of SKU_1 wasn't changed
        Optional<InventoryEntry> existingEntry =
            getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1, null);
        assertThat(existingEntry).isNotEmpty();
        assertValues(existingEntry.get(), QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1);

        //assert channel of SUPPLY_CHANNEL_KEY_1 exists
        Optional<Channel> existingChannel = getCtpClientOfTargetProject().getClient()
            .executeBlocking(ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_1))
            .head();
        assertThat(existingChannel).isNotEmpty();

        //assert inventory of SKU_1 and channel SUPPLY_CHANNEL_KEY_1 was updated
        existingEntry = getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1,
            Channel.referenceOfId(existingChannel.get().getId()));
        assertThat(existingEntry).isNotEmpty();
        assertValues(existingEntry.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);

        //assert channel of SUPPLY_CHANNEL_KEY_2 exists
        existingChannel = getCtpClientOfTargetProject().getClient()
            .executeBlocking(ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_2))
            .head();
        assertThat(existingChannel).isNotEmpty();

        //assert inventory of SKU_1 and channel SUPPLY_CHANNEL_KEY_2 was created
        existingEntry = getInventoryEntryBySkuAndSupplyChannel(targetProjectClient, SKU_1,
            Channel.referenceOfId(existingChannel.get().getId()));
        assertThat(existingEntry).isNotEmpty();
        assertValues(existingEntry.get(), QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2);
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
