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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * This class provides static utility methods for building update actions related to inventories.
 */
public final class InventoryUpdateActionUtils {

    private InventoryUpdateActionUtils() {
        throw new AssertionError();
    }

    /**
     * Compares the {@code quantityOnStock} values of a {@link InventoryEntry} and {@link InventoryEntryDraft}
     * and returns a {@link List} of update actions, which would contain the {@code "changeQuantity"}
     * {@link UpdateAction}. If quantity on stock from {@code newEntry} is {@code null} or no update action is needed,
     * for example in case where both the {@link InventoryEntry} and the {@link InventoryEntryDraft} have the same
     * {@code quantityOnStock} values, the empty list would be returned.
     *
     * @param oldEntry the inventory entry that should be updated
     * @param newEntry the inventory entry draft which contains new quantity on stock.
     * @return list containing update action or empty list if quantities on stock are indentical or quantity on stock
     * from {@code newEntry} is null.
     */
    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildChangeQuantityAction(@Nonnull final InventoryEntry oldEntry,
                                                                               @Nonnull final InventoryEntryDraft newEntry) {
        final Long oldValue = oldEntry.getQuantityOnStock();
        final Long newValue = newEntry.getQuantityOnStock();
        return newValue == null || Objects.equals(oldValue, newValue)
                ? emptyList() : asList(ChangeQuantity.of(newValue));
    }

    /**
     * Compares the {@code restockableInDays} values of a {@link InventoryEntry} and {@link InventoryEntryDraft}
     * and returns a {@link List} of update actions, which would contain the {@code "setRestockableInDays"}
     * {@link UpdateAction}. If no update action is needed, for example in case where both the
     * {@link InventoryEntry} and the {@link InventoryEntryDraft} have the same {@code restockableInDays} values,
     * the empty list would be returned.
     *
     * @param oldEntry the inventory entry that should be updated
     * @param newEntry the inventory entry draft which contains new restockable in days.
     * @return list containing update action or empty list if restockable in days are indentical.
     */
    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildSetRestockableInDaysAction(@Nonnull final InventoryEntry oldEntry,
                                                                                     @Nonnull final InventoryEntryDraft newEntry) {
        final Integer oldValue = oldEntry.getRestockableInDays();
        final Integer newValue = newEntry.getRestockableInDays();
        return Objects.equals(oldValue, newValue)
                ? emptyList() : asList(SetRestockableInDays.of(newValue));
    }

    /**
     * Compares the {@code expectedDelivery} values of a {@link InventoryEntry} and {@link InventoryEntryDraft}
     * and returns a {@link List} of update actions, which would contain the {@code "setExpectedDelivery"}
     * {@link UpdateAction}. If no update action is needed, for example in case where both the
     * {@link InventoryEntry} and the {@link InventoryEntryDraft} have the same {@code expectedDelivery} values,
     * the empty list would be returned.
     *
     * @param oldEntry the inventory entry that should be updated
     * @param newEntry the inventory entry draft which contains new expected delivery.
     * @return list containing update action or empty list if expected deliveries are indentical.
     */
    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildSetExpectedDeliveryAction(@Nonnull final InventoryEntry oldEntry,
                                                                                    @Nonnull final InventoryEntryDraft newEntry) {
        final ZonedDateTime oldValue = oldEntry.getExpectedDelivery();
        final ZonedDateTime newValue = newEntry.getExpectedDelivery();
        return Objects.equals(oldValue, newValue)
                ? emptyList() : asList(SetExpectedDelivery.of(newValue));
    }

    /**
     * Compares the {@code supplyChannel} references of a {@link InventoryEntry} and {@link InventoryEntryDraft}
     * and returns a {@link List} of update actions, which would contain the {@code "setSupplyChannel"}
     * {@link UpdateAction}. If no update action is needed, for example in case where both the {@link InventoryEntry}
     * and the {@link InventoryEntryDraft} have the same supply channel, the empty list would be returned.
     *
     * @param oldEntry the inventory entry that should be updated
     * @param newEntry the inventory entry draft which contains new supply channel.
     * @return list containing update action or empty list if supply channels are indentical.
     */
    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildSetSupplyChannelAction(@Nonnull final InventoryEntry oldEntry,
                                                                                 @Nonnull final InventoryEntryDraft newEntry) {
        final Reference<Channel> oldValue = oldEntry.getSupplyChannel();
        final Reference<Channel> newValue = newEntry.getSupplyChannel();
        return Objects.equals(oldValue, newValue)
                ? emptyList() : asList(SetSupplyChannel.of(newEntry.getSupplyChannel()));
    }
}
