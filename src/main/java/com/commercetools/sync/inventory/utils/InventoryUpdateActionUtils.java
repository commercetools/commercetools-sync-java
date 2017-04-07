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

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public final class InventoryUpdateActionUtils {

    private InventoryUpdateActionUtils() {
        throw new AssertionError();
    }

    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildChangeQuantityAction(@Nonnull final InventoryEntry oldEntry,
                                                                               @Nonnull final InventoryEntryDraft newEntry) {
        final Long oldValue = oldEntry.getQuantityOnStock();
        final Long newValue = newEntry.getQuantityOnStock();
        return Objects.equals(oldValue, newValue)
                ? emptyList() : singletonList(ChangeQuantity.of(newValue));
    }

    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildSetRestockableInDaysAction(@Nonnull final InventoryEntry oldEntry,
                                                                                     @Nonnull final InventoryEntryDraft newEntry) {
        final Integer oldValue = oldEntry.getRestockableInDays();
        final Integer newValue = newEntry.getRestockableInDays();
        return Objects.equals(oldValue, newValue)
                ? emptyList() : singletonList(SetRestockableInDays.of(newValue));
    }

    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildSetExpectedDeliveryAction(@Nonnull final InventoryEntry oldEntry,
                                                                                    @Nonnull final InventoryEntryDraft newEntry) {
        final ZonedDateTime oldValue = oldEntry.getExpectedDelivery();
        final ZonedDateTime newValue = newEntry.getExpectedDelivery();
        return Objects.equals(oldValue, newValue)
                ? emptyList() : singletonList(SetExpectedDelivery.of(newValue));
    }

    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildSetSupplyChannelAction(@Nonnull final InventoryEntry oldEntry,
                                                                                 @Nonnull final InventoryEntryDraft newEntry) {
        final Reference<Channel> oldValue = oldEntry.getSupplyChannel();
        final Reference<Channel> newValue = newEntry.getSupplyChannel();
        return Objects.equals(oldValue, newValue)
                ? emptyList() : singletonList(SetSupplyChannel.of(newEntry.getSupplyChannel()));
    }
}
