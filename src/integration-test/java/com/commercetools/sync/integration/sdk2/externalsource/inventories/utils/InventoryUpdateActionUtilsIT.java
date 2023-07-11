package com.commercetools.sync.integration.sdk2.externalsource.inventories.utils;

import static com.commercetools.sync.integration.sdk2.commons.utils.ChannelITUtils.deleteChannelsFromTargetAndSource;
import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.createTypeIfNotAlreadyExisting;
import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.sdk2.commons.utils.InventoryITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils.buildPrimaryResourceCustomUpdateActions;
import static com.commercetools.sync.sdk2.commons.utils.CustomValueConverter.convertCustomValueObjDataToJsonNode;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelResourceIdentifier;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.inventory.*;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.sync.integration.sdk2.commons.utils.ChannelITUtils;
import com.commercetools.sync.sdk2.inventories.InventorySyncOptions;
import com.commercetools.sync.sdk2.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.sdk2.inventories.helpers.InventoryCustomActionBuilder;
import com.commercetools.sync.sdk2.inventories.models.InventoryEntryCustomTypeAdapter;
import com.commercetools.sync.sdk2.inventories.models.InventoryEntryDraftCustomTypeAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryUpdateActionUtilsIT {

  private static final ObjectNode CUSTOM_FIELD_VALUE =
      JsonNodeFactory.instance.objectNode().put("en", "purple");

  @BeforeEach
  void setup() {
    deleteInventoryEntriesFromTargetAndSource();
    final Channel channel = ChannelITUtils.populateTargetProject();
    final ChannelResourceIdentifier supplyChannelReference =
        ChannelResourceIdentifierBuilder.of().id(channel.getId()).build();

    createTypeIfNotAlreadyExisting(
        CUSTOM_TYPE,
        Locale.ENGLISH,
        CUSTOM_TYPE,
        Collections.singletonList(ResourceTypeId.INVENTORY_ENTRY),
        CTP_TARGET_CLIENT);

    populateTargetProjectWithChannelsAndTypes(supplyChannelReference);
  }

  /**
   * Deletes all the test data from the {@code CTP_SOURCE_CLIENT} and the {@code CTP_SOURCE_CLIENT}
   * projects that were set up in this test class.
   */
  @AfterAll
  static void tearDown() {
    deleteInventoryEntriesFromTargetAndSource();
    deleteTypesFromTargetAndSource();
    deleteChannelsFromTargetAndSource();
  }

  @Test
  void buildCustomUpdateActions_ShouldBuildActionThatSetCustomField() {
    // Fetch old inventory and ensure it has custom fields.
    final Optional<InventoryEntry> oldInventoryOptional =
        getInventoryEntryBySkuAndSupplyChannel(CTP_TARGET_CLIENT, SKU_1, null).stream().findFirst();
    assertThat(oldInventoryOptional).isNotEmpty();
    final InventoryEntry oldInventoryBeforeSync = oldInventoryOptional.get();
    assertThat(oldInventoryBeforeSync.getCustom()).isNotNull();
    assertThat(oldInventoryBeforeSync.getCustom().getType().getObj()).isNotNull();
    assertThat(oldInventoryBeforeSync.getCustom().getType().getObj().getKey())
        .isEqualTo(CUSTOM_TYPE);

    // Prepare draft with updated data.
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(
                typeResourceIdentifierBuilder ->
                    typeResourceIdentifierBuilder.id(
                        oldInventoryBeforeSync.getCustom().getType().getId()))
            .fields(
                fieldContainerBuilder ->
                    fieldContainerBuilder.addValue(CUSTOM_FIELD_NAME, CUSTOM_FIELD_VALUE))
            .build();

    final InventoryEntryDraft newInventory =
        InventoryEntryDraftBuilder.of()
            .sku(SKU_1)
            .quantityOnStock(QUANTITY_ON_STOCK_2)
            .expectedDelivery(EXPECTED_DELIVERY_2)
            .restockableInDays(RESTOCKABLE_IN_DAYS_2)
            .custom(customFieldsDraft)
            .build();

    // Build update actions.
    final InventorySyncOptions options = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    final List<InventoryEntryUpdateAction> updateActions =
        buildPrimaryResourceCustomUpdateActions(
            InventoryEntryCustomTypeAdapter.of(oldInventoryBeforeSync),
            InventoryEntryDraftCustomTypeAdapter.of(newInventory),
            new InventoryCustomActionBuilder(),
            options);
    assertThat(updateActions).isNotEmpty();

    // Execute update command and ensure returned entry is properly updated.
    final InventoryEntry oldEntryAfterSync =
        CTP_TARGET_CLIENT
            .inventory()
            .withId(oldInventoryBeforeSync.getId())
            .post(
                InventoryEntryUpdateBuilder.of()
                    .actions(updateActions)
                    .version(oldInventoryBeforeSync.getVersion())
                    .build())
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();
    assertThat(oldEntryAfterSync.getCustom()).isNotNull();
    final JsonNode syncedCustomFieldDataAsJson =
        convertCustomValueObjDataToJsonNode(
            oldEntryAfterSync.getCustom().getFields().values().get(CUSTOM_FIELD_NAME));
    assertThat(syncedCustomFieldDataAsJson).isEqualTo(CUSTOM_FIELD_VALUE);
  }
}
