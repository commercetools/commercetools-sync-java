package com.commercetools.sync.inventories.utils;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.commands.InventoryEntryUpdateCommand;
import io.sphere.sdk.models.Reference;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static com.commercetools.sync.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.EXPECTED_DELIVERY_1;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.EXPECTED_DELIVERY_2;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.QUANTITY_ON_STOCK_1;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.QUANTITY_ON_STOCK_2;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.RESTOCKABLE_IN_DAYS_1;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.RESTOCKABLE_IN_DAYS_2;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.SKU_1;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.deleteInventoriesAndSupplyChannels;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.getInventoryEntryBySkuAndSupplyChannel;
import static com.commercetools.sync.inventories.InventoryIntegrationTestUtils.populateTargetProject;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildChangeQuantityAction;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildSetExpectedDeliveryAction;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildSetRestockableInDaysAction;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildSetSupplyChannelAction;
import static org.assertj.core.api.Assertions.assertThat;

public class InventoryUpdateActionUtilsItTest {

    /**
     * Deletes inventories and supply channels from source and target CTP projects.
     * Populates target CTP projects with test data.
     */
    @Before
    public void setup() {
        deleteInventoriesAndSupplyChannels();
        populateTargetProject();
    }

    @AfterClass
    public static void delete() {
        deleteInventoriesAndSupplyChannels();
    }

    @Test
    public void buildChangeQuantityAction_ShouldBuildActionThatUpdatesQuantity() {
        //Fetch old inventory and ensure its quantity on stock.
        final Optional<InventoryEntry> oldEntryOptional =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null);
        assertThat(oldEntryOptional).isNotEmpty();

        final InventoryEntry oldEntryBeforeSync = oldEntryOptional.get();
        assertThat(oldEntryBeforeSync.getQuantityOnStock()).isEqualTo(QUANTITY_ON_STOCK_1);

        //Prepare draft with updated data.
        final InventoryEntryDraft newEntry =
            InventoryEntryDraft.of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null);

        //Build update action.
        final Optional<UpdateAction<InventoryEntry>> updateActionOptional =
            buildChangeQuantityAction(oldEntryBeforeSync, newEntry);
        assertThat(updateActionOptional).isNotEmpty();
        final UpdateAction<InventoryEntry> updateAction = updateActionOptional.get();

        //Execute update command and ensure returned entry is properly updated.
        final InventoryEntry oldEntryAfterSync = CTP_TARGET_CLIENT
            .execute(InventoryEntryUpdateCommand.of(oldEntryBeforeSync, updateAction))
            .toCompletableFuture().join();
        assertThat(oldEntryAfterSync.getQuantityOnStock()).isEqualTo(QUANTITY_ON_STOCK_2);
    }

    @Test
    public void buildSetRestockableInDaysAction_ShouldBuildActionThatUpdatesRestockableInDays() {
        //Fetch old inventory and ensure its restockable in days.
        final Optional<InventoryEntry> oldEntryOptional =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null);
        assertThat(oldEntryOptional).isNotEmpty();

        final InventoryEntry oldEntryBeforeSync = oldEntryOptional.get();
        assertThat(oldEntryBeforeSync.getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS_1);

        //Prepare draft with updated data.
        final InventoryEntryDraft newEntry =
            InventoryEntryDraft.of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null);

        //Build update action.
        final Optional<UpdateAction<InventoryEntry>> updateActionOptional =
            buildSetRestockableInDaysAction(oldEntryBeforeSync, newEntry);
        assertThat(updateActionOptional).isNotEmpty();
        final UpdateAction<InventoryEntry> updateAction = updateActionOptional.get();

        //Execute update command and ensure returned entry is properly updated.
        final InventoryEntry oldEntryAfterSync = CTP_TARGET_CLIENT
            .execute(InventoryEntryUpdateCommand.of(oldEntryBeforeSync, updateAction))
            .toCompletableFuture().join();
        assertThat(oldEntryAfterSync.getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS_2);
    }

    @Test
    public void buildSetExpectedDeliveryAction_ShouldBuildActionThatUpdatesExpectedDelivery() {
        //Fetch old inventory and ensure its expected delivery.
        final Optional<InventoryEntry> oldEntryOptional =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null);
        assertThat(oldEntryOptional).isNotEmpty();

        final InventoryEntry oldEntryBeforeSync = oldEntryOptional.get();
        assertThat(oldEntryBeforeSync.getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY_1);

        //Prepare draft with updated data.
        final InventoryEntryDraft newEntry =
            InventoryEntryDraft.of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null);

        //Build update action.
        final Optional<UpdateAction<InventoryEntry>> updateActionOptional =
            buildSetExpectedDeliveryAction(oldEntryBeforeSync, newEntry);
        assertThat(updateActionOptional).isNotEmpty();
        final UpdateAction<InventoryEntry> updateAction = updateActionOptional.get();

        //Execute update command and ensure returned entry is properly updated.
        final InventoryEntry oldEntryAfterSync = CTP_TARGET_CLIENT
            .execute(InventoryEntryUpdateCommand.of(oldEntryBeforeSync, updateAction))
            .toCompletableFuture().join();
        assertThat(oldEntryAfterSync.getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY_2);
    }

    @Test
    public void buildSetSupplyChannelAction_ShouldBuildActionThatUpdatesSupplyChannel() {
        //Fetch old inventory and ensure it has no supply channel.
        final Optional<InventoryEntry> oldInventoryOptional =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null);
        assertThat(oldInventoryOptional).isNotEmpty();
        final InventoryEntry oldInventoryBeforeSync = oldInventoryOptional.get();
        assertThat(oldInventoryBeforeSync.getSupplyChannel()).isNull();

        //Create some channel and get reference to it.
        final ChannelDraft channelDraft = ChannelDraft.of("newChannelKey")
            .withRoles(ChannelRole.INVENTORY_SUPPLY);
        final Channel channel = CTP_TARGET_CLIENT.execute(ChannelCreateCommand.of(channelDraft))
            .toCompletableFuture().join();
        final Reference<Channel> channelReference = channel.toReference();

        //Prepare draft with updated data.
        final InventoryEntryDraft newInventory =
            InventoryEntryDraft.of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2,
                channelReference);

        //Build update action.
        final Optional<UpdateAction<InventoryEntry>> updateActionOptional =
            buildSetSupplyChannelAction(oldInventoryBeforeSync, newInventory);
        assertThat(updateActionOptional).isNotEmpty();
        final UpdateAction<InventoryEntry> updateAction = updateActionOptional.get();

        //Execute update command and ensure returned entry is properly updated.
        final InventoryEntry oldEntryAfterSync = CTP_TARGET_CLIENT
            .execute(InventoryEntryUpdateCommand.of(oldInventoryBeforeSync, updateAction))
            .toCompletableFuture().join();
        assertThat(oldEntryAfterSync.getId()).isEqualTo(oldInventoryBeforeSync.getId());
        assertThat(oldEntryAfterSync.getSupplyChannel()).isEqualTo(channelReference);
    }

    @Test
    public void buildCustomUpdateActions_WithDeletedCustom_ShouldBuildActionThatDeletesCustom() {
        // TODO implement
    }

    @Test
    public void buildCustomUpdateActions_WithNewCustom_ShouldBuildActionThatAddsCustom() {
        // TODO implement
    }

    @Test
    public void buildCustomUpdateActions_WithUpdatedCustom_ShouldBuildActionThatUpdatesCustom() {
        // TODO implement
    }
}
