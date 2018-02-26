package com.commercetools.sync.commons.utils;

import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.inventories.helpers.InventoryCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.commands.updateactions.SetCustomField;
import io.sphere.sdk.inventory.commands.updateactions.SetCustomType;
import org.junit.Test;

import java.util.HashMap;

import static com.commercetools.sync.commons.asserts.actions.AssertionsForUpdateActions.assertThat;
import static io.sphere.sdk.models.ResourceIdentifier.ofId;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class InventoryEntryCustomUpdateActionUtilsTest {

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithInventoryResource_ShouldBuildInventoryUpdateAction() {
        final String newCustomTypeId = "key";

        final UpdateAction<InventoryEntry> updateAction =
            GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(newCustomTypeId,
                new HashMap<>(), mock(InventoryEntry.class), new InventoryCustomActionBuilder(), null,
                InventoryEntry::getId, inventoryResource -> inventoryResource.toReference().getTypeId(),
                inventoryResource -> null, InventorySyncOptionsBuilder.of(mock(SphereClient.class)).build())
                                    .orElse(null);

        assertThat(updateAction).isInstanceOf(SetCustomType.class);
        assertThat((SetCustomType) updateAction).hasValues("setCustomType", emptyMap(), ofId(newCustomTypeId));
    }

    @Test
    public void buildRemoveCustomTypeAction_WithInventoryResource_ShouldBuildChannelUpdateAction() {
        final UpdateAction<InventoryEntry> updateAction =
            new InventoryCustomActionBuilder().buildRemoveCustomTypeAction(null, null);

        assertThat(updateAction).isInstanceOf(SetCustomType.class);
        assertThat((SetCustomType) updateAction).hasValues("setCustomType", null, ofId(null));
    }

    @Test
    public void buildSetCustomFieldAction_WithInventoryResource_ShouldBuildInventoryUpdateAction() {
        final JsonNode customFieldValue = JsonNodeFactory.instance.textNode("foo");
        final String customFieldName = "name";

        final UpdateAction<InventoryEntry> updateAction =
            new InventoryCustomActionBuilder().buildSetCustomFieldAction(null, null, customFieldName, customFieldValue);

        assertThat(updateAction).isInstanceOf(SetCustomField.class);
        assertThat((SetCustomField) updateAction).hasValues("setCustomField", customFieldName, customFieldValue);
    }
}
