package com.commercetools.sync.integration.inventories;

import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.inventories.helpers.InventoryCustomActionBuilder;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.commands.InventoryEntryUpdateCommand;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.CustomFieldsDraftBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildPrimaryResourceCustomUpdateActions;
import static com.commercetools.sync.integration.commons.utils.ChannelITUtils.deleteChannelsFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.CUSTOM_TYPE;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.EXPECTED_DELIVERY_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.QUANTITY_ON_STOCK_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.RESTOCKABLE_IN_DAYS_2;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.SKU_1;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.deleteInventoryEntriesFromTargetAndSource;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.getInventoryEntryBySkuAndSupplyChannel;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.populateTargetProject;
import static org.assertj.core.api.Assertions.assertThat;

class InventoryUpdateActionUtilsIT {

    private static final String CUSTOM_FIELD_VALUE = "custom-value-1";

    /**
     * Deletes inventories and supply channels from source and target CTP projects.
     * Populates target CTP projects with test data.
     */
    @BeforeEach
    void setup() {
        deleteInventoryEntriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
        deleteChannelsFromTargetAndSource();
        populateTargetProject();
    }

    /**
     * Deletes all the test data from the {@code CTP_SOURCE_CLIENT} and the {@code CTP_SOURCE_CLIENT} projects that
     * were set up in this test class.
     */
    @AfterAll
    static void tearDown() {
        deleteInventoryEntriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
        deleteChannelsFromTargetAndSource();
    }

    @Test
    void buildCustomUpdateActions_ShouldBuildActionThatSetCustomField() {
        //Fetch old inventory and ensure it has custom fields.
        final Optional<InventoryEntry> oldInventoryOptional =
            getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null);
        assertThat(oldInventoryOptional).isNotEmpty();
        final InventoryEntry oldInventoryBeforeSync = oldInventoryOptional.get();
        assertThat(oldInventoryBeforeSync.getCustom()).isNotNull();
        assertThat(oldInventoryBeforeSync.getCustom().getType().getObj()).isNotNull();
        assertThat(oldInventoryBeforeSync.getCustom().getType().getObj().getKey()).isEqualTo(CUSTOM_TYPE);

        //Prepare draft with updated data.
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraftBuilder
            .ofTypeId(oldInventoryBeforeSync.getCustom().getType().getId())
            .addObject(CUSTOM_FIELD_NAME, CUSTOM_FIELD_VALUE).build();
        final InventoryEntryDraft newInventory =
            InventoryEntryDraft.of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, null)
            .withCustom(customFieldsDraft);

        //Build update actions.
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
        final List<UpdateAction<InventoryEntry>> updateActions =
            buildPrimaryResourceCustomUpdateActions(oldInventoryBeforeSync, newInventory,
                new InventoryCustomActionBuilder(), options);
        assertThat(updateActions).isNotEmpty();

        //Execute update command and ensure returned entry is properly updated.
        final InventoryEntry oldEntryAfterSync = CTP_TARGET_CLIENT
            .execute(InventoryEntryUpdateCommand.of(oldInventoryBeforeSync, updateActions))
            .toCompletableFuture().join();
        assertThat(oldEntryAfterSync.getCustom()).isNotNull();
        assertThat(oldEntryAfterSync.getCustom().getFieldAsString(CUSTOM_FIELD_NAME)).isEqualTo(CUSTOM_FIELD_VALUE);
    }
}
