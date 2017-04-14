package com.commercetools.sync.inventory.utils;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.commands.updateactions.ChangeQuantity;
import io.sphere.sdk.inventory.commands.updateactions.SetExpectedDelivery;
import io.sphere.sdk.inventory.commands.updateactions.SetRestockableInDays;
import io.sphere.sdk.inventory.commands.updateactions.SetSupplyChannel;
import io.sphere.sdk.models.Reference;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.BiFunction;

import static com.commercetools.sync.inventory.utils.InventoryUpdateActionUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InventoryUpdateActionUtilsTest {

    private InventoryEntry old;
    private InventoryEntryDraft newSame;
    private InventoryEntryDraft newDifferent;
    private InventoryEntryDraft newWithNullValues;

    {
        final ZonedDateTime date1 = ZonedDateTime.of(2017, 5, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
        final ZonedDateTime date2 = ZonedDateTime.of(2017, 4, 1, 12, 0, 0, 0, ZoneId.of("UTC"));
        
        final Channel channel = mock(Channel.class);
        final Reference<Channel> supplyChannel1 = Channel.referenceOfId("456");
        final Reference<Channel> supplyChannel1WithObject = Channel.referenceOfId("456").filled(channel);
        final Reference<Channel> supplyChannel2 = Channel.referenceOfId("789");

        old = mock(InventoryEntry.class);
        when(old.getSku()).thenReturn("123");
        when(old.getQuantityOnStock()).thenReturn(10l);
        when(old.getRestockableInDays()).thenReturn(10);
        when(old.getExpectedDelivery()).thenReturn(date1);
        when(old.getSupplyChannel()).thenReturn(supplyChannel1WithObject);

        newSame = InventoryEntryDraft.of("123", 10l, date1, 10, supplyChannel1);
        newDifferent = InventoryEntryDraft.of("123", 20l, date2, 20, supplyChannel2);
        newWithNullValues = InventoryEntryDraft.of("123", 20l, null, null, null);
    }

    @Test
    public void buildChangeQuantityAction_returnsAction_havingDifferentValues() {
        final List<UpdateAction<InventoryEntry>> result = buildChangeQuantityAction(old, newDifferent);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isExactlyInstanceOf(ChangeQuantity.class);
        assertThat(((ChangeQuantity) result.get(0)).getQuantity()).isEqualTo(newDifferent.getQuantityOnStock());
    }

    @Test
    public void buildChangeQuantityAction_returnsEmptyList_havingNewNullValue() {
        final InventoryEntryDraft draft = mock(InventoryEntryDraft.class);
        when(draft.getQuantityOnStock()).thenReturn(null);
        final List<UpdateAction<InventoryEntry>> result = buildChangeQuantityAction(old, draft);
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    public void buildChangeQuantityAction_returnsEmptyList_havingSameValues() {
        assertNoUpdatesForSameValues(InventoryUpdateActionUtils::buildChangeQuantityAction);
    }

    @Test
    public void buildSetRestockableInDaysAction_returnsAction_havingDifferentValues() {
        final List<UpdateAction<InventoryEntry>> result = buildSetRestockableInDaysAction(old, newDifferent);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isExactlyInstanceOf(SetRestockableInDays.class);
        assertThat(((SetRestockableInDays) result.get(0)).getRestockableInDays())
                .isEqualTo(newDifferent.getRestockableInDays());
    }

    @Test
    public void buildSetRestockableInDaysAction_returnsAction_havingNewNullValue() {
        final List<UpdateAction<InventoryEntry>> result = buildSetRestockableInDaysAction(old, newWithNullValues);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isExactlyInstanceOf(SetRestockableInDays.class);
        assertThat(((SetRestockableInDays) result.get(0)).getRestockableInDays()).isNull();
    }

    @Test
    public void buildSetRestockableInDaysAction_returnsEmptyList_havingSameValues() {
        assertNoUpdatesForSameValues(InventoryUpdateActionUtils::buildSetRestockableInDaysAction);
    }

    @Test
    public void buildSetExpectedDeliveryAction_returnsAction_havingDifferentValues() {
        final List<UpdateAction<InventoryEntry>> result = buildSetExpectedDeliveryAction(old, newDifferent);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isExactlyInstanceOf(SetExpectedDelivery.class);
        assertThat(((SetExpectedDelivery) result.get(0)).getExpectedDelivery())
                .isEqualTo(newDifferent.getExpectedDelivery());
    }

    @Test
    public void buildSetExpectedDeliveryAction_returnsAction_havingNewNullValue() {
        final List<UpdateAction<InventoryEntry>> result = buildSetExpectedDeliveryAction(old, newWithNullValues);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isExactlyInstanceOf(SetExpectedDelivery.class);
        assertThat(((SetExpectedDelivery) result.get(0)).getExpectedDelivery()).isNull();
    }

    @Test
    public void buildSetExpectedDeliveryAction_returnsEmptyList_havingSameValues() {
        assertNoUpdatesForSameValues(InventoryUpdateActionUtils::buildSetExpectedDeliveryAction);
    }

    @Test
    public void buildSetSupplyChannelAction_returnsAction_havingDifferentValues() {
        final List<UpdateAction<InventoryEntry>> result = buildSetSupplyChannelAction(old, newDifferent);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isExactlyInstanceOf(SetSupplyChannel.class);
        assertThat(((SetSupplyChannel) result.get(0)).getSupplyChannel())
                .isEqualTo(newDifferent.getSupplyChannel());
    }

    @Test
    public void buildSetSupplyChannelAction_returnsAction_havingNewNullValue() {
        final List<UpdateAction<InventoryEntry>> result = buildSetSupplyChannelAction(old, newWithNullValues);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isExactlyInstanceOf(SetSupplyChannel.class);
        assertThat(((SetSupplyChannel) result.get(0)).getSupplyChannel()).isNull();
    }

    @Test
    public void buildSetSupplyChannelAction_returnsEmptyList_havingSameValues() {
        assertNoUpdatesForSameValues(InventoryUpdateActionUtils::buildSetSupplyChannelAction);
    }

    private void assertNoUpdatesForSameValues
            (BiFunction<InventoryEntry, InventoryEntryDraft, List<UpdateAction<InventoryEntry>>> buildFunction) {
        final List<UpdateAction<InventoryEntry>> result = buildFunction.apply(old, newSame);
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }
}
