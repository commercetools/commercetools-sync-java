package com.commercetools.sync.commons.utils;

import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
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
    private InventorySyncOptions syncOptions = InventorySyncOptionsBuilder.of(mock(SphereClient.class)).build();

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithInventoryResource_ShouldBuildInventoryUpdateAction() {
        final UpdateAction<InventoryEntry> updateAction =
            GenericUpdateActionUtils.<InventoryEntry, InventoryEntry>buildTypedSetCustomTypeUpdateAction("key",
                new HashMap<>(), mock(InventoryEntry.class), null, null, InventoryEntry::getId,
                inventoryResource -> inventoryResource.toReference().getTypeId(), inventoryResource -> null,
                syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithInventoryResource_ShouldBuildChannelUpdateAction() {
        final UpdateAction<InventoryEntry> updateAction =
            GenericUpdateActionUtils.<InventoryEntry, InventoryEntry>buildTypedRemoveCustomTypeUpdateAction(
                mock(InventoryEntry.class), null, null,
                InventoryEntry::getId, inventoryResource -> inventoryResource.toReference().getTypeId(),
                inventoryResource -> null, syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithInventoryResource_ShouldBuildInventoryUpdateAction() {
        final UpdateAction<InventoryEntry> updateAction =
            GenericUpdateActionUtils.<InventoryEntry, InventoryEntry>buildTypedSetCustomFieldUpdateAction(
                "name", mock(JsonNode.class), mock(InventoryEntry.class), null, null, InventoryEntry::getId,
                inventoryEntryResource -> inventoryEntryResource.toReference().getTypeId(),
                inventoryEntryResource -> null,
                syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetCustomField.class);
    }
}
