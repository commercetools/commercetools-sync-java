package com.commercetools.sync.sdk2.inventories.utils;

import static com.commercetools.sync.sdk2.commons.MockUtils.getMockCustomFields;
import static com.commercetools.sync.sdk2.inventories.InventorySyncMockUtils.getMockInventoryEntry;
import static com.commercetools.sync.sdk2.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.channel.ChannelReferenceBuilder;
import com.commercetools.api.models.inventory.*;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.sync.sdk2.inventories.InventorySyncOptionsBuilder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InventorySyncUtilsTest {
  private static final String CUSTOM_TYPE_ID = "testId";
  private static final String CUSTOM_FIELD_1_NAME = "testField";
  private static final String CUSTOM_FIELD_2_NAME = "differentField";
  private static final String CUSTOM_FIELD_1_VALUE = "testValue";
  private static final String CUSTOM_FIELD_2_VALUE = "differentValue";

  private static final String SKU = "123";
  private static final long QUANTITY = 10L;
  private static final long RESTOCKABLE_IN_DAYS = 10L;
  private static final String SUPPLY_CHANNEL_ID = "111";
  private static final ZonedDateTime DATE_1 =
      ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
  private static final ZonedDateTime DATE_2 =
      ZonedDateTime.of(2017, 5, 1, 20, 0, 0, 0, ZoneId.of("UTC"));

  private static InventoryEntry inventoryEntry;
  private static InventoryEntry inventoryEntryWithCustomField1;

  /** Initialises test data. */
  @BeforeAll
  static void setup() {
    final Channel channel = getMockSupplyChannel(SUPPLY_CHANNEL_ID, "key1");
    final ChannelReference reference =
        ChannelReferenceBuilder.of().id(SUPPLY_CHANNEL_ID).obj(channel).build();
    final CustomFields customFields =
        getMockCustomFields(
            CUSTOM_TYPE_ID,
            CUSTOM_FIELD_1_NAME,
            JsonNodeFactory.instance.textNode(CUSTOM_FIELD_1_VALUE));

    inventoryEntry =
        getMockInventoryEntry(SKU, QUANTITY, RESTOCKABLE_IN_DAYS, DATE_1, reference, null);
    inventoryEntryWithCustomField1 =
        getMockInventoryEntry(SKU, QUANTITY, RESTOCKABLE_IN_DAYS, DATE_1, reference, customFields);
  }

  @Test
  void buildActions_WithSimilarEntries_ShouldReturnEmptyList() {
    final InventoryEntryDraft similarDraft =
        InventoryEntryDraftBuilder.of()
            .sku(SKU)
            .quantityOnStock(QUANTITY)
            .expectedDelivery(DATE_1)
            .restockableInDays(QUANTITY)
            .supplyChannel(
                channelResourceIdentifierBuilder ->
                    channelResourceIdentifierBuilder.id(SUPPLY_CHANNEL_ID))
            .build();
    List<InventoryEntryUpdateAction> actions =
        InventorySyncUtils.buildActions(
            inventoryEntry,
            similarDraft,
            InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions).isEmpty();
  }

  @Test
  void buildActions_WithVariousEntries_ShouldReturnActions() {
    final InventoryEntryDraft variousDraft =
        InventoryEntryDraftBuilder.of()
            .sku("321")
            .quantityOnStock(20L)
            .expectedDelivery(DATE_2)
            .restockableInDays(20L)
            .supplyChannel(
                channelResourceIdentifierBuilder -> channelResourceIdentifierBuilder.id("222"))
            .build();
    List<InventoryEntryUpdateAction> actions =
        InventorySyncUtils.buildActions(
            inventoryEntry,
            variousDraft,
            InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions).hasSize(4);
    assertThat(actions.get(0)).isNotNull();
    assertThat(actions.get(1)).isNotNull();
    assertThat(actions.get(2)).isNotNull();
    assertThat(actions.get(3)).isNotNull();
    assertThat(actions.get(0)).isInstanceOf(InventoryEntryChangeQuantityActionImpl.class);
    assertThat(actions.get(1)).isInstanceOf(InventoryEntrySetRestockableInDaysActionImpl.class);
    assertThat(actions.get(2)).isInstanceOf(InventoryEntrySetExpectedDeliveryActionImpl.class);
    assertThat(actions.get(3)).isInstanceOfAny(InventoryEntrySetSupplyChannelActionImpl.class);
  }

  @Test
  void buildActions_WithSimilarEntriesAndSameCustomFields_ShouldReturnEmptyList() {
    final InventoryEntryDraft newInventoryWithSameCustom =
        InventoryEntryDraftBuilder.of()
            .sku(SKU)
            .quantityOnStock(QUANTITY)
            .expectedDelivery(DATE_1)
            .restockableInDays(QUANTITY)
            .supplyChannel(
                channelResourceIdentifierBuilder ->
                    channelResourceIdentifierBuilder.id(SUPPLY_CHANNEL_ID))
            .custom(getDraftOfCustomField(CUSTOM_FIELD_1_NAME, CUSTOM_FIELD_1_VALUE))
            .build();
    final List<InventoryEntryUpdateAction> actions =
        InventorySyncUtils.buildActions(
            inventoryEntryWithCustomField1,
            newInventoryWithSameCustom,
            InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions).isEmpty();
  }

  @Test
  void buildActions_WithSimilarEntriesAndNewCustomTypeSet_ShouldReturnActions() {
    final InventoryEntryDraft newInventoryWithNewCustom =
        InventoryEntryDraftBuilder.of()
            .sku(SKU)
            .quantityOnStock(QUANTITY)
            .expectedDelivery(DATE_1)
            .restockableInDays(QUANTITY)
            .supplyChannel(
                channelResourceIdentifierBuilder ->
                    channelResourceIdentifierBuilder.id(SUPPLY_CHANNEL_ID))
            .custom(getDraftOfCustomField(CUSTOM_FIELD_2_NAME, CUSTOM_FIELD_2_VALUE))
            .build();

    final List<InventoryEntryUpdateAction> actions =
        InventorySyncUtils.buildActions(
            inventoryEntry,
            newInventoryWithNewCustom,
            InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions).hasSize(1);
    assertThat(actions.get(0)).isNotNull();
    assertThat(actions.get(0)).isInstanceOf(InventoryEntrySetCustomTypeActionImpl.class);
  }

  @Test
  void buildActions_WithSimilarEntriesAndRemovedExistingCustomType_ShouldReturnActions() {
    final InventoryEntryDraft newInventoryWithoutCustom =
        InventoryEntryDraftBuilder.of()
            .sku(SKU)
            .quantityOnStock(QUANTITY)
            .expectedDelivery(DATE_1)
            .restockableInDays(QUANTITY)
            .supplyChannel(
                channelResourceIdentifierBuilder ->
                    channelResourceIdentifierBuilder.id(SUPPLY_CHANNEL_ID))
            .build();
    final List<InventoryEntryUpdateAction> actions =
        InventorySyncUtils.buildActions(
            inventoryEntryWithCustomField1,
            newInventoryWithoutCustom,
            InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions).hasSize(1);
    assertThat(actions.get(0)).isNotNull();
    assertThat(actions.get(0)).isInstanceOf(InventoryEntrySetCustomTypeActionImpl.class);
  }

  @Test
  void buildActions_WithSimilarEntriesButDifferentCustomFields_ShouldReturnActions() {
    final InventoryEntryDraft newInventoryWithDifferentCustom =
        InventoryEntryDraftBuilder.of()
            .sku(SKU)
            .quantityOnStock(QUANTITY)
            .expectedDelivery(DATE_1)
            .restockableInDays(QUANTITY)
            .supplyChannel(
                channelResourceIdentifierBuilder ->
                    channelResourceIdentifierBuilder.id(SUPPLY_CHANNEL_ID))
            .custom(getDraftOfCustomField(CUSTOM_FIELD_2_NAME, CUSTOM_FIELD_2_VALUE))
            .build();

    final List<InventoryEntryUpdateAction> actions =
        InventorySyncUtils.buildActions(
            inventoryEntryWithCustomField1,
            newInventoryWithDifferentCustom,
            InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions).hasSize(2);
    assertThat(actions.get(0)).isNotNull();
    assertThat(actions.get(1)).isNotNull();
    assertThat(actions.get(0)).isInstanceOf(InventoryEntrySetCustomFieldActionImpl.class);
    assertThat(actions.get(1)).isInstanceOf(InventoryEntrySetCustomFieldActionImpl.class);
  }

  @Test
  void buildActions_WithSimilarEntriesButDifferentCustomFieldValues_ShouldReturnActions() {
    final InventoryEntryDraft newInventoryWithDifferenFieldValues =
        InventoryEntryDraftBuilder.of()
            .sku(SKU)
            .quantityOnStock(QUANTITY)
            .expectedDelivery(DATE_1)
            .restockableInDays(QUANTITY)
            .supplyChannel(
                channelResourceIdentifierBuilder ->
                    channelResourceIdentifierBuilder.id(SUPPLY_CHANNEL_ID))
            .custom(getDraftOfCustomField(CUSTOM_FIELD_1_NAME, CUSTOM_FIELD_2_VALUE))
            .build();
    final List<InventoryEntryUpdateAction> actions =
        InventorySyncUtils.buildActions(
            inventoryEntryWithCustomField1,
            newInventoryWithDifferenFieldValues,
            InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions).hasSize(1);
    assertThat(actions.get(0)).isNotNull();
    assertThat(actions.get(0)).isInstanceOf(InventoryEntrySetCustomFieldActionImpl.class);
  }

  private CustomFieldsDraft getDraftOfCustomField(final String fieldName, final String fieldValue) {
    return CustomFieldsDraftBuilder.of()
        .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(CUSTOM_TYPE_ID))
        .fields(fieldContainerBuilder -> fieldContainerBuilder.addValue(fieldName, fieldValue))
        .build();
  }
}
