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
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;

/**
 * This class provides static utility methods for building update actions related to inventories.
 */
public final class InventoryUpdateActionUtils {

    private InventoryUpdateActionUtils() {
        throw new AssertionError();
    }

    /**
     * Compares the {@code quantityOnStock} values of a {@link InventoryEntry} and {@link InventoryEntryDraft}
     * and returns a {@link Optional} of update action, which would contain the {@code "changeQuantity"}
     * {@link UpdateAction}. If quantity on stock from {@code newEntry} is {@code null} or no update action is needed,
     * for example in case where both the {@link InventoryEntry} and the {@link InventoryEntryDraft} have the same
     * {@code quantityOnStock} values, the empty optional would be returned.
     *
     * @param oldEntry the inventory entry that should be updated
     * @param newEntry the inventory entry draft which contains new quantity on stock.
     * @return optional containing update action or empty optional if quantities on stock are indentical or quantity on stock
     * from {@code newEntry} is null.
     */
    @Nonnull
    public static Optional<UpdateAction<InventoryEntry>> buildChangeQuantityAction(@Nonnull final InventoryEntry oldEntry,
                                                                                   @Nonnull final InventoryEntryDraft newEntry) {
        final Long newQuantityOnStock = newEntry.getQuantityOnStock();
        if (newQuantityOnStock == null) {
            return Optional.empty();
        }
        return buildUpdateAction(oldEntry.getQuantityOnStock(), newQuantityOnStock,
                () -> ChangeQuantity.of(newQuantityOnStock));
    }

    /**
     * Compares the {@code restockableInDays} values of a {@link InventoryEntry} and {@link InventoryEntryDraft}
     * and returns a {@link Optional} of update actions, which would contain the {@code "setRestockableInDays"}
     * {@link UpdateAction}. If no update action is needed, for example in case where both the
     * {@link InventoryEntry} and the {@link InventoryEntryDraft} have the same {@code restockableInDays} values,
     * the empty optional would be returned.
     *
     * @param oldEntry the inventory entry that should be updated
     * @param newEntry the inventory entry draft which contains new restockable in days.
     * @return optional containing update action or empty optional if restockable in days are indentical.
     */
    @Nonnull
    public static Optional<UpdateAction<InventoryEntry>> buildSetRestockableInDaysAction(@Nonnull final InventoryEntry oldEntry,
                                                                                     @Nonnull final InventoryEntryDraft newEntry) {
        final Integer newRestockableInDays = newEntry.getRestockableInDays();
        return buildUpdateAction(oldEntry.getRestockableInDays(), newRestockableInDays,
                () -> SetRestockableInDays.of(newRestockableInDays));
    }

    /**
     * Compares the {@code expectedDelivery} values of a {@link InventoryEntry} and {@link InventoryEntryDraft}
     * and returns a {@link Optional} of update actions, which would contain the {@code "setExpectedDelivery"}
     * {@link UpdateAction}. If no update action is needed, for example in case where both the
     * {@link InventoryEntry} and the {@link InventoryEntryDraft} have the same {@code expectedDelivery} values,
     * the empty optional would be returned.
     *
     * @param oldEntry the inventory entry that should be updated
     * @param newEntry the inventory entry draft which contains new expected delivery.
     * @return optional containing update action or empty optional if expected deliveries are indentical.
     */
    @Nonnull
    public static Optional<UpdateAction<InventoryEntry>> buildSetExpectedDeliveryAction(@Nonnull final InventoryEntry oldEntry,
                                                                                    @Nonnull final InventoryEntryDraft newEntry) {
        final ZonedDateTime newExpectedDelivery = newEntry.getExpectedDelivery();
        return buildUpdateAction(oldEntry.getExpectedDelivery(), newExpectedDelivery,
                () -> SetExpectedDelivery.of(newExpectedDelivery));
    }

    /**
     * Compares the {@code supplyChannel} references of a {@link InventoryEntry} and {@link InventoryEntryDraft}
     * and returns a {@link Optional} of update actions, which would contain the {@code "setSupplyChannel"}
     * {@link UpdateAction}. If no update action is needed, for example in case where both the {@link InventoryEntry}
     * and the {@link InventoryEntryDraft} have the same supply channel, the empty optional would be returned.
     *
     * @param oldEntry the inventory entry that should be updated
     * @param newEntry the inventory entry draft which contains new supply channel.
     * @return optional containing update action or empty optional if supply channels are indentical.
     */
    @Nonnull
    public static Optional<UpdateAction<InventoryEntry>> buildSetSupplyChannelAction(@Nonnull final InventoryEntry oldEntry,
                                                                                 @Nonnull final InventoryEntryDraft newEntry) {
        final Reference<Channel> newSupplyChannel = newEntry.getSupplyChannel();
        return buildUpdateAction(oldEntry.getSupplyChannel(), newSupplyChannel,
                () -> SetSupplyChannel.of(newSupplyChannel));
    }
}
