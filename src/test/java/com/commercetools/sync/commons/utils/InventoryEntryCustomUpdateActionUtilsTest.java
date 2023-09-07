package com.commercetools.sync.commons.utils;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.inventory.*;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.inventories.helpers.InventoryCustomActionBuilder;
import com.commercetools.sync.inventories.models.InventoryEntryCustomTypeAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryEntryCustomUpdateActionUtilsTest {

  @Test
  void
      buildTypedSetCustomTypeUpdateAction_WithInventoryResource_ShouldBuildInventoryUpdateAction() {
    final String newCustomTypeId = UUID.randomUUID().toString();

    final InventoryEntryUpdateAction updateAction =
        GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
                newCustomTypeId,
                new HashMap<>(),
                InventoryEntryCustomTypeAdapter.of(mock(InventoryEntry.class)),
                new InventoryCustomActionBuilder(),
                null,
                InventoryEntryCustomTypeAdapter::getId,
                inventoryResource -> ResourceTypeId.INVENTORY_ENTRY.getJsonName(),
                inventoryResource -> null,
                InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build())
            .orElse(null);

    assertThat(updateAction).isInstanceOf(InventoryEntrySetCustomTypeActionImpl.class);
    InventoryEntrySetCustomTypeAction inventorySetCustomTypeAction =
        (InventoryEntrySetCustomTypeAction) updateAction;
    assertThat(inventorySetCustomTypeAction.getType().getId()).isEqualTo(newCustomTypeId);
    assertThat(inventorySetCustomTypeAction.getFields().values()).isEqualTo(emptyMap());
  }

  @Test
  void buildRemoveCustomTypeAction_WithInventoryResource_ShouldBuildChannelUpdateAction() {
    final InventoryEntryUpdateAction updateAction =
        new InventoryCustomActionBuilder().buildRemoveCustomTypeAction(null, null);

    assertThat(updateAction).isInstanceOf(InventoryEntrySetCustomTypeActionImpl.class);
    InventoryEntrySetCustomTypeAction inventorySetCustomTypeAction =
        (InventoryEntrySetCustomTypeAction) updateAction;
    assertThat(inventorySetCustomTypeAction.getType()).isNull();
    assertThat(inventorySetCustomTypeAction.getFields()).isNull();
  }

  @Test
  void buildSetCustomFieldAction_WithInventoryResource_ShouldBuildInventoryUpdateAction() {
    final JsonNode customFieldValue = JsonNodeFactory.instance.textNode("foo");
    final String customFieldName = "name";

    final InventoryEntryUpdateAction updateAction =
        new InventoryCustomActionBuilder()
            .buildSetCustomFieldAction(null, null, customFieldName, customFieldValue);

    assertThat(updateAction).isInstanceOf(InventoryEntrySetCustomFieldActionImpl.class);
    InventoryEntrySetCustomFieldAction inventorySetCustomFieldAction =
        (InventoryEntrySetCustomFieldAction) updateAction;
    assertThat(inventorySetCustomFieldAction.getName()).isEqualTo(customFieldName);
    assertThat(inventorySetCustomFieldAction.getValue()).isEqualTo(customFieldValue);
  }
}
