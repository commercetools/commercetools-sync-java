package com.commercetools.sync.commons.utils;

import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.inventories.helpers.InventoryCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.commands.updateactions.SetCustomField;
import io.sphere.sdk.inventory.commands.updateactions.SetCustomType;
import org.junit.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class InventoryEntryCustomUpdateActionUtilsTest {

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithInventoryResource_ShouldBuildInventoryUpdateAction() {
        final UpdateAction<InventoryEntry> updateAction =
            GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction("key",
                new HashMap<>(), mock(InventoryEntry.class), new InventoryCustomActionBuilder(), null,
                InventoryEntry::getId, inventoryResource -> inventoryResource.toReference().getTypeId(),
                inventoryResource -> null, InventorySyncOptionsBuilder.of(mock(SphereClient.class)).build())
                                    .orElse(null);

        assertThat(updateAction).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildRemoveCustomTypeAction_WithInventoryResource_ShouldBuildChannelUpdateAction() {
        final UpdateAction<InventoryEntry> updateAction =
            new InventoryCustomActionBuilder().buildRemoveCustomTypeAction(null, null);

        assertThat(updateAction).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildSetCustomFieldAction_WithInventoryResource_ShouldBuildInventoryUpdateAction() {
        final UpdateAction<InventoryEntry> updateAction =
            new InventoryCustomActionBuilder().buildSetCustomFieldAction(null, null, "name", mock(JsonNode.class));

        assertThat(updateAction).isInstanceOf(SetCustomField.class);
    }
}
