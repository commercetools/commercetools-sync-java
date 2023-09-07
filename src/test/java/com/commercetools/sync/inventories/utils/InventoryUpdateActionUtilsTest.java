package com.commercetools.sync.inventories.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.channel.ChannelReferenceBuilder;
import com.commercetools.api.models.inventory.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InventoryUpdateActionUtilsTest {

  private static final String SKU = "123";
  private static final Long QUANTITY = 10L;
  private static final Long RESTOCKABLE_DAYS = 10L;
  private static final ZonedDateTime DATE_OLD =
      ZonedDateTime.of(2017, 5, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
  private static final ChannelReference SUPPLY_CHANNEL_OLD =
      ChannelReferenceBuilder.of().id("456").build();

  private static final Long QUANTITY_NEW = 20L;
  private static final Long RESTOCKABLE_DAYS_NEW = 20L;
  private static final ZonedDateTime DATE_NEW =
      ZonedDateTime.of(2017, 4, 1, 12, 0, 0, 0, ZoneId.of("UTC"));
  private static final ChannelReference SUPPLY_CHANNEL_NEW =
      ChannelReferenceBuilder.of().id("789").build();

  private static InventoryEntry old;

  /** Initialises test data. */
  @BeforeAll
  static void setup() {
    old = mock(InventoryEntry.class);
    when(old.getSku()).thenReturn(SKU);
    when(old.getQuantityOnStock()).thenReturn(QUANTITY);
    when(old.getRestockableInDays()).thenReturn(RESTOCKABLE_DAYS);
    when(old.getExpectedDelivery()).thenReturn(DATE_OLD);
    when(old.getSupplyChannel()).thenReturn(SUPPLY_CHANNEL_OLD);
  }

  @Test
  void buildChangeQuantityAction_WithDifferentValues_ShouldReturnAction() {
    final InventoryEntryDraft newDifferent =
        InventoryEntryDraftBuilder.of()
            .sku(SKU)
            .quantityOnStock(QUANTITY_NEW)
            .expectedDelivery(DATE_NEW)
            .restockableInDays(RESTOCKABLE_DAYS_NEW)
            .supplyChannel(
                channelResourceIdentifierBuilder ->
                    channelResourceIdentifierBuilder.id(SUPPLY_CHANNEL_NEW.getId()))
            .build();
    final Optional<InventoryEntryUpdateAction> result =
        InventoryUpdateActionUtils.buildChangeQuantityAction(old, newDifferent);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isExactlyInstanceOf(InventoryEntryChangeQuantityActionImpl.class);
    assertThat(((InventoryEntryChangeQuantityAction) result.get()).getQuantity())
        .isEqualTo(newDifferent.getQuantityOnStock());
  }

  @Test
  void buildChangeQuantityAction_WithNewNullValue_ShouldReturnAction() {
    final InventoryEntryDraft draft = mock(InventoryEntryDraft.class);
    when(draft.getQuantityOnStock()).thenReturn(null);
    final Optional<InventoryEntryUpdateAction> result =
        InventoryUpdateActionUtils.buildChangeQuantityAction(old, draft);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isExactlyInstanceOf(InventoryEntryChangeQuantityActionImpl.class);
    assertThat(((InventoryEntryChangeQuantityAction) result.get()).getQuantity()).isEqualTo(0L);
  }

  @Test
  void buildChangeQuantityAction_WithNewNullValueAndOldZeroValue_ShouldReturnEmptyOptional() {
    final InventoryEntryDraft draft = mock(InventoryEntryDraft.class);
    when(draft.getQuantityOnStock()).thenReturn(null);
    final InventoryEntry entry = mock(InventoryEntry.class);
    when(draft.getQuantityOnStock()).thenReturn(0L);
    final Optional<InventoryEntryUpdateAction> result =
        InventoryUpdateActionUtils.buildChangeQuantityAction(entry, draft);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  void buildChangeQuantityAction_WithSameValues_ShouldReturnEmptyOptional() {
    assertNoUpdatesForSameValues(InventoryUpdateActionUtils::buildChangeQuantityAction);
  }

  @Test
  void buildSetRestockableInDaysAction_WithDifferentValues_ShouldReturnAction() {
    final InventoryEntryDraft newDifferent =
        InventoryEntryDraftBuilder.of()
            .sku(SKU)
            .quantityOnStock(QUANTITY_NEW)
            .expectedDelivery(DATE_NEW)
            .restockableInDays(RESTOCKABLE_DAYS_NEW)
            .supplyChannel(
                channelResourceIdentifierBuilder ->
                    channelResourceIdentifierBuilder.id(SUPPLY_CHANNEL_NEW.getId()))
            .build();
    final Optional<InventoryEntryUpdateAction> result =
        InventoryUpdateActionUtils.buildSetRestockableInDaysAction(old, newDifferent);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get())
        .isExactlyInstanceOf(InventoryEntrySetRestockableInDaysActionImpl.class);
    assertThat(((InventoryEntrySetRestockableInDaysAction) result.get()).getRestockableInDays())
        .isEqualTo(newDifferent.getRestockableInDays());
  }

  @Test
  void buildSetRestockableInDaysAction_WithNewNullValue_ShouldReturnAction() {
    final InventoryEntryDraft newWithNullValues =
        InventoryEntryDraftBuilder.of().sku(SKU).quantityOnStock(20L).build();
    final Optional<InventoryEntryUpdateAction> result =
        InventoryUpdateActionUtils.buildSetRestockableInDaysAction(old, newWithNullValues);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get())
        .isExactlyInstanceOf(InventoryEntrySetRestockableInDaysActionImpl.class);
    assertThat(((InventoryEntrySetRestockableInDaysAction) result.get()).getRestockableInDays())
        .isNull();
  }

  @Test
  void buildSetRestockableInDaysAction_WithSameValues_ShouldReturnEmptyOptional() {
    assertNoUpdatesForSameValues(InventoryUpdateActionUtils::buildSetRestockableInDaysAction);
  }

  @Test
  void buildSetExpectedDeliveryAction_WithDifferentValue_ShouldReturnActions() {
    final InventoryEntryDraft newDifferent =
        InventoryEntryDraftBuilder.of()
            .sku(SKU)
            .quantityOnStock(QUANTITY_NEW)
            .expectedDelivery(DATE_NEW)
            .restockableInDays(RESTOCKABLE_DAYS_NEW)
            .supplyChannel(
                channelResourceIdentifierBuilder ->
                    channelResourceIdentifierBuilder.id(SUPPLY_CHANNEL_NEW.getId()))
            .build();
    final Optional<InventoryEntryUpdateAction> result =
        InventoryUpdateActionUtils.buildSetExpectedDeliveryAction(old, newDifferent);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isExactlyInstanceOf(InventoryEntrySetExpectedDeliveryActionImpl.class);
    assertThat(((InventoryEntrySetExpectedDeliveryAction) result.get()).getExpectedDelivery())
        .isEqualTo(newDifferent.getExpectedDelivery());
  }

  @Test
  void buildSetExpectedDeliveryAction_WithNewNullValue_ShouldReturnAction() {
    final InventoryEntryDraft newWithNullValues =
        InventoryEntryDraftBuilder.of().sku(SKU).quantityOnStock(20L).build();
    final Optional<InventoryEntryUpdateAction> result =
        InventoryUpdateActionUtils.buildSetExpectedDeliveryAction(old, newWithNullValues);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isExactlyInstanceOf(InventoryEntrySetExpectedDeliveryActionImpl.class);
    assertThat(((InventoryEntrySetExpectedDeliveryAction) result.get()).getExpectedDelivery())
        .isNull();
  }

  @Test
  void buildSetExpectedDeliveryAction_WithSameValues_ShouldReturnEmptyOptional() {
    assertNoUpdatesForSameValues(InventoryUpdateActionUtils::buildSetExpectedDeliveryAction);
  }

  @Test
  void buildSetSupplyChannelAction_WithDifferentValues_ShouldReturnAction() {
    final InventoryEntryDraft newDifferent =
        InventoryEntryDraftBuilder.of()
            .sku(SKU)
            .quantityOnStock(QUANTITY_NEW)
            .expectedDelivery(DATE_NEW)
            .restockableInDays(RESTOCKABLE_DAYS_NEW)
            .supplyChannel(
                channelResourceIdentifierBuilder ->
                    channelResourceIdentifierBuilder.id(SUPPLY_CHANNEL_NEW.getId()))
            .build();
    final Optional<InventoryEntryUpdateAction> result =
        InventoryUpdateActionUtils.buildSetSupplyChannelAction(old, newDifferent);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isExactlyInstanceOf(InventoryEntrySetSupplyChannelActionImpl.class);
    assertThat(((InventoryEntrySetSupplyChannelAction) result.get()).getSupplyChannel())
        .isEqualTo(newDifferent.getSupplyChannel());
  }

  @Test
  void buildSetSupplyChannelAction_WithNewNullValue_ShouldReturnAction() {
    final InventoryEntryDraft newWithNullValues =
        InventoryEntryDraftBuilder.of().sku(SKU).quantityOnStock(20L).build();
    final Optional<InventoryEntryUpdateAction> result =
        InventoryUpdateActionUtils.buildSetSupplyChannelAction(old, newWithNullValues);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isExactlyInstanceOf(InventoryEntrySetSupplyChannelActionImpl.class);
    assertThat(((InventoryEntrySetSupplyChannelAction) result.get()).getSupplyChannel()).isNull();
  }

  @Test
  void buildSetSupplyChannelAction_WithSameValues_ShouldReturnEmptyOptional() {
    assertNoUpdatesForSameValues(InventoryUpdateActionUtils::buildSetSupplyChannelAction);
  }

  private void assertNoUpdatesForSameValues(
      final BiFunction<InventoryEntry, InventoryEntryDraft, Optional<InventoryEntryUpdateAction>>
          buildFunction) {
    InventoryEntryDraft newSame =
        InventoryEntryDraftBuilder.of()
            .sku(SKU)
            .quantityOnStock(QUANTITY)
            .expectedDelivery(DATE_OLD)
            .restockableInDays(RESTOCKABLE_DAYS)
            .supplyChannel(
                channelResourceIdentifierBuilder ->
                    channelResourceIdentifierBuilder.id(SUPPLY_CHANNEL_OLD.getId()))
            .build();
    final Optional<InventoryEntryUpdateAction> result = buildFunction.apply(old, newSame);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isFalse();
  }
}
