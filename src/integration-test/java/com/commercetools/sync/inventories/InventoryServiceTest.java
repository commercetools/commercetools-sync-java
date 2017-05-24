package com.commercetools.sync.inventories;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.commands.updateactions.ChangeQuantity;
import io.sphere.sdk.inventory.commands.updateactions.SetExpectedDelivery;
import io.sphere.sdk.inventory.commands.updateactions.SetRestockableInDays;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.EXPECTED_DELIVERY_1;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.EXPECTED_DELIVERY_2;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.QUANTITY_ON_STOCK_1;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.QUANTITY_ON_STOCK_2;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.RESTOCKABLE_IN_DAYS_1;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.RESTOCKABLE_IN_DAYS_2;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.SKU_1;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.SKU_2;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.SUPPLY_CHANNEL_KEY_2;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.deleteInventoriesAndSupplyChannels;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.getInventoryEntryBySkuAndSupplyChannel;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.populateTargetProject;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class InventoryServiceTest {

    private InventoryService inventoryService;

    /**
     * Deletes inventories and supply channels from source and target CTP projects.
     * Populates target CTP project with test data.
     */
    @Before
    public void setup() {
        deleteInventoriesAndSupplyChannels();
        populateTargetProject();
        inventoryService = new InventoryServiceImpl(CTP_TARGET_CLIENT);
    }

    @AfterClass
    public static void cleanup() {
        deleteInventoriesAndSupplyChannels();
    }

    @Test
    public void fetchInventoryEntriesBySku_ShouldReturnProperEntries() {
        final Set<String> skus = singleton(SKU_1);
        final List<InventoryEntry> result = inventoryService.fetchInventoryEntriesBySkus(skus)
            .toCompletableFuture()
            .join();

        // assert result size
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        //assert SKU is proper
        result.stream()
            .map(InventoryEntry::getSku)
            .forEach(sku -> assertThat(sku).isEqualTo(SKU_1));

        //assert references are expanded
        result.stream()
            .filter(inventoryEntry -> inventoryEntry.getSupplyChannel() != null)
            .map(InventoryEntry::getSupplyChannel)
            .forEach(supplyChannel -> assertThat(supplyChannel.getObj()).isNotNull());
    }

    @Test
    public void fetchAllSupplyChannels_ShouldReturnProperChannels() {
        final List<Channel> result = inventoryService.fetchAllSupplyChannels()
            .toCompletableFuture()
            .join();

        //assert result size
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        //assert role of results
        assertThat(result.get(0).getRoles()).containsExactly(ChannelRole.INVENTORY_SUPPLY);
    }

    @Test
    public void createSupplyChannel_ShouldCreateProperChannel() {
        //create channel
        final Channel result = inventoryService.createSupplyChannel(SUPPLY_CHANNEL_KEY_2)
            .toCompletableFuture()
            .join();

        //assert returned data
        assertThat(result).isNotNull();
        assertThat(result.getRoles()).containsExactly(ChannelRole.INVENTORY_SUPPLY);
        assertThat(result.getKey()).isEqualTo(SUPPLY_CHANNEL_KEY_2);

        //assert CTP state
        final Optional<Channel> createdChannelOptional = CTP_TARGET_CLIENT
            .execute(ChannelQuery.of().byKey(SUPPLY_CHANNEL_KEY_2))
            .toCompletableFuture()
            .join()
            .head();
        assertThat(createdChannelOptional).isNotEmpty();
        assertThat(createdChannelOptional.get()).isEqualTo(result);
    }

    @Test
    public void createInventoryEntry_ShouldCreateProperInventoryEntry() {
        //prepare draft and create
        final InventoryEntryDraft inventoryEntryDraft = InventoryEntryDraftBuilder
            .of(SKU_2, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null)
            .build();

        final InventoryEntry result = inventoryService.createInventoryEntry(inventoryEntryDraft)
            .toCompletableFuture()
            .join();

        //assert returned data
        assertThat(result).isNotNull();
        assertThat(result.getQuantityOnStock()).isEqualTo(QUANTITY_ON_STOCK_2);
        assertThat(result.getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS_2);
        assertThat(result.getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY_2);

        //assert CTP state
        final Optional<InventoryEntry> updatedInventoryEntry =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_2, null);
        assertThat(updatedInventoryEntry).isNotEmpty();
        assertThat(updatedInventoryEntry.get()).isEqualTo(result);
    }

    @Test
    public void updateInventoryEntry_ShouldUpdateInventoryEntryProperly() {
        //fetch existing inventory entry and assert its state
        final Optional<InventoryEntry> existingEntryOptional =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null);
        assertThat(existingEntryOptional).isNotEmpty();

        final InventoryEntry existingEntry = existingEntryOptional.get();
        assertThat(existingEntry.getQuantityOnStock()).isEqualTo(QUANTITY_ON_STOCK_1);
        assertThat(existingEntry.getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS_1);
        assertThat(existingEntry.getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY_1);

        //build update actions and do update
        final List<UpdateAction<InventoryEntry>> updateActions = Stream.of(
            ChangeQuantity.of(QUANTITY_ON_STOCK_2),
            SetExpectedDelivery.of(EXPECTED_DELIVERY_2),
            SetRestockableInDays.of(RESTOCKABLE_IN_DAYS_2)
        ).collect(toList());

        final InventoryEntry result = inventoryService.updateInventoryEntry(existingEntry, updateActions)
            .toCompletableFuture()
            .join();

        //assert returned data
        assertThat(result).isNotNull();
        assertThat(result.getQuantityOnStock()).isEqualTo(QUANTITY_ON_STOCK_2);
        assertThat(result.getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS_2);
        assertThat(result.getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY_2);

        //assert CTP state
        final Optional<InventoryEntry> updatedInventoryEntry =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null);
        assertThat(updatedInventoryEntry).isNotEmpty();
        assertThat(updatedInventoryEntry.get()).isEqualTo(result);
    }
}
