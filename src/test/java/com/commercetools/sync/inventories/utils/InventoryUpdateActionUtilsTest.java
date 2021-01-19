package com.commercetools.sync.inventories.utils;

import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildChangeQuantityAction;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildSetExpectedDeliveryAction;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildSetRestockableInDaysAction;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildSetSupplyChannelAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.commands.updateactions.ChangeQuantity;
import io.sphere.sdk.inventory.commands.updateactions.SetExpectedDelivery;
import io.sphere.sdk.inventory.commands.updateactions.SetRestockableInDays;
import io.sphere.sdk.inventory.commands.updateactions.SetSupplyChannel;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InventoryUpdateActionUtilsTest {

  private static InventoryEntry old;
  private static InventoryEntryDraft newSame;
  private static InventoryEntryDraft newDifferent;
  private static InventoryEntryDraft newWithNullValues;

  /** Initialises test data. */
  @BeforeAll
  static void setup() {
    final ZonedDateTime date1 = ZonedDateTime.of(2017, 5, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
    final ZonedDateTime date2 = ZonedDateTime.of(2017, 4, 1, 12, 0, 0, 0, ZoneId.of("UTC"));

    final Reference<Channel> supplyChannel1 = Channel.referenceOfId("456");
    final Reference<Channel> supplyChannel2 = Channel.referenceOfId("789");

    old = mock(InventoryEntry.class);
    when(old.getSku()).thenReturn("123");
    when(old.getQuantityOnStock()).thenReturn(10L);
    when(old.getRestockableInDays()).thenReturn(10);
    when(old.getExpectedDelivery()).thenReturn(date1);
    when(old.getSupplyChannel()).thenReturn(supplyChannel1);

    newSame =
        InventoryEntryDraftBuilder.of(
                "123", 10L, date1, 10, ResourceIdentifier.ofId(supplyChannel1.getId()))
            .build();
    newDifferent =
        InventoryEntryDraftBuilder.of(
                "123", 20L, date2, 20, ResourceIdentifier.ofId(supplyChannel2.getId()))
            .build();
    newWithNullValues = InventoryEntryDraftBuilder.of("123", 20L, null, null, null).build();
  }

  @Test
  void buildChangeQuantityAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<UpdateAction<InventoryEntry>> result =
        buildChangeQuantityAction(old, newDifferent);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isExactlyInstanceOf(ChangeQuantity.class);
    assertThat(((ChangeQuantity) result.get()).getQuantity())
        .isEqualTo(newDifferent.getQuantityOnStock());
  }

  @Test
  void buildChangeQuantityAction_WithNewNullValue_ShouldReturnAction() {
    final InventoryEntryDraft draft = mock(InventoryEntryDraft.class);
    when(draft.getQuantityOnStock()).thenReturn(null);
    final Optional<UpdateAction<InventoryEntry>> result = buildChangeQuantityAction(old, draft);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isExactlyInstanceOf(ChangeQuantity.class);
    assertThat(((ChangeQuantity) result.get()).getQuantity()).isEqualTo(0L);
  }

  @Test
  void buildChangeQuantityAction_WithNewNullValueAndOldZeroValue_ShouldReturnEmptyOptional() {
    final InventoryEntryDraft draft = mock(InventoryEntryDraft.class);
    when(draft.getQuantityOnStock()).thenReturn(null);
    final InventoryEntry entry = mock(InventoryEntry.class);
    when(draft.getQuantityOnStock()).thenReturn(0L);
    final Optional<UpdateAction<InventoryEntry>> result = buildChangeQuantityAction(entry, draft);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  void buildChangeQuantityAction_WithSameValues_ShouldReturnEmptyOptional() {
    assertNoUpdatesForSameValues(InventoryUpdateActionUtils::buildChangeQuantityAction);
  }

  @Test
  void buildSetRestockableInDaysAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<UpdateAction<InventoryEntry>> result =
        buildSetRestockableInDaysAction(old, newDifferent);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isExactlyInstanceOf(SetRestockableInDays.class);
    assertThat(((SetRestockableInDays) result.get()).getRestockableInDays())
        .isEqualTo(newDifferent.getRestockableInDays());
  }

  @Test
  void buildSetRestockableInDaysAction_WithNewNullValue_ShouldReturnAction() {
    final Optional<UpdateAction<InventoryEntry>> result =
        buildSetRestockableInDaysAction(old, newWithNullValues);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isExactlyInstanceOf(SetRestockableInDays.class);
    assertThat(((SetRestockableInDays) result.get()).getRestockableInDays()).isNull();
  }

  @Test
  void buildSetRestockableInDaysAction_WithSameValues_ShouldReturnEmptyOptional() {
    assertNoUpdatesForSameValues(InventoryUpdateActionUtils::buildSetRestockableInDaysAction);
  }

  @Test
  void buildSetExpectedDeliveryAction_WithDifferentValue_ShouldReturnActions() {
    final Optional<UpdateAction<InventoryEntry>> result =
        buildSetExpectedDeliveryAction(old, newDifferent);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isExactlyInstanceOf(SetExpectedDelivery.class);
    assertThat(((SetExpectedDelivery) result.get()).getExpectedDelivery())
        .isEqualTo(newDifferent.getExpectedDelivery());
  }

  @Test
  void buildSetExpectedDeliveryAction_WithNewNullValue_ShouldReturnAction() {
    final Optional<UpdateAction<InventoryEntry>> result =
        buildSetExpectedDeliveryAction(old, newWithNullValues);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isExactlyInstanceOf(SetExpectedDelivery.class);
    assertThat(((SetExpectedDelivery) result.get()).getExpectedDelivery()).isNull();
  }

  @Test
  void buildSetExpectedDeliveryAction_WithSameValues_ShouldReturnEmptyOptional() {
    assertNoUpdatesForSameValues(InventoryUpdateActionUtils::buildSetExpectedDeliveryAction);
  }

  @Test
  void buildSetSupplyChannelAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<UpdateAction<InventoryEntry>> result =
        buildSetSupplyChannelAction(old, newDifferent);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isExactlyInstanceOf(SetSupplyChannel.class);
    assertThat(((SetSupplyChannel) result.get()).getSupplyChannel())
        .isEqualTo(newDifferent.getSupplyChannel());
  }

  @Test
  void buildSetSupplyChannelAction_WithNewNullValue_ShouldReturnAction() {
    final Optional<UpdateAction<InventoryEntry>> result =
        buildSetSupplyChannelAction(old, newWithNullValues);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isExactlyInstanceOf(SetSupplyChannel.class);
    assertThat(((SetSupplyChannel) result.get()).getSupplyChannel()).isNull();
  }

  @Test
  void buildSetSupplyChannelAction_WithSameValues_ShouldReturnEmptyOptional() {
    assertNoUpdatesForSameValues(InventoryUpdateActionUtils::buildSetSupplyChannelAction);
  }

  private void assertNoUpdatesForSameValues(
      final BiFunction<InventoryEntry, InventoryEntryDraft, Optional<UpdateAction<InventoryEntry>>>
          buildFunction) {
    final Optional<UpdateAction<InventoryEntry>> result = buildFunction.apply(old, newSame);
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isFalse();
  }
}
