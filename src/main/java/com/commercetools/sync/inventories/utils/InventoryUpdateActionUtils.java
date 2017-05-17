package com.commercetools.sync.inventories.utils;

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
     * Compares the {@code quantityOnStock} values of an {@link InventoryEntry} and an {@link InventoryEntryDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeQuantity"}
     * {@link UpdateAction}. If both {@link InventoryEntry} and {@link InventoryEntryDraft} have the same
     * {@code quantityOnStock} values, then no update action is needed and empty optional will be returned.
     * If the {@code quantityOnStock} from the {@code newEntry} is {@code null}, the new {@code quantityOnStock} will
     * have a value of 0L.
     *
     * @param oldEntry the inventory entry that should be updated
     * @param newEntry the inventory entry draft which contains new quantity on stock
     * @return optional containing update action or empty optional if quantities on stock are identical
     */
    @Nonnull
    public static Optional<UpdateAction<InventoryEntry>> buildChangeQuantityAction(@Nonnull final InventoryEntry
                                                                                           oldEntry,
                                                                                   @Nonnull final InventoryEntryDraft
                                                                                       newEntry) {
        final Long oldQuantityOnStock = oldEntry.getQuantityOnStock();
        final Long newQuantityOnStock = newEntry.getQuantityOnStock() == null ? Long.valueOf(0L) : newEntry
            .getQuantityOnStock();
        return buildUpdateAction(oldQuantityOnStock, newQuantityOnStock, () -> ChangeQuantity.of(newQuantityOnStock));
    }

    /**
     * Compares the {@code restockableInDays} values of an {@link InventoryEntry} and an {@link InventoryEntryDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setRestockableInDays"}
     * {@link UpdateAction}. If both {@link InventoryEntry} and the {@link InventoryEntryDraft} have the same
     * {@code restockableInDays} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldEntry the inventory entry that should be updated
     * @param newEntry the inventory entry draft which contains new restockable in days
     * @return optional containing update action or empty optional if restockable in days are identical
     */
    @Nonnull
    public static Optional<UpdateAction<InventoryEntry>> buildSetRestockableInDaysAction(@Nonnull final
                                                                                             InventoryEntry oldEntry,
                                                                                         @Nonnull final
                                                                                         InventoryEntryDraft newEntry) {
        final Integer oldRestockableInDays = oldEntry.getRestockableInDays();
        final Integer newRestockableInDays = newEntry.getRestockableInDays();
        return buildUpdateAction(oldRestockableInDays, newRestockableInDays,
            () -> SetRestockableInDays.of(newRestockableInDays));
    }

    /**
     * Compares the {@code expectedDelivery} values of an {@link InventoryEntry} and an {@link InventoryEntryDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setExpectedDelivery"}
     * {@link UpdateAction}. If both {@link InventoryEntry} and {@link InventoryEntryDraft} have the same
     * {@code expectedDelivery} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldEntry the inventory entry that should be updated
     * @param newEntry the inventory entry draft which contains new expected delivery
     * @return optional containing update action or empty optional if expected deliveries are identical
     */
    @Nonnull
    public static Optional<UpdateAction<InventoryEntry>> buildSetExpectedDeliveryAction(@Nonnull final InventoryEntry
                                                                                            oldEntry,
                                                                                        @Nonnull final
                                                                                        InventoryEntryDraft newEntry) {
        final ZonedDateTime oldExpectedDelivery = oldEntry.getExpectedDelivery();
        final ZonedDateTime newExpectedDelivery = newEntry.getExpectedDelivery();
        return buildUpdateAction(oldExpectedDelivery, newExpectedDelivery,
            () -> SetExpectedDelivery.of(newExpectedDelivery));
    }

    /**
     * Compares the {@code supplyChannel} references of an {@link InventoryEntry} and an {@link InventoryEntryDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setSupplyChannel"}
     * {@link UpdateAction}. If both {@link InventoryEntry} and {@link InventoryEntryDraft} have the same supply
     * channel, then no update action is needed and empty optional will be returned.
     *
     * @param oldEntry the inventory entry that should be updated
     * @param newEntry the inventory entry draft which contains new supply channel
     * @return optional containing update action or empty optional if supply channels are identical
     */
    @Nonnull
    public static Optional<UpdateAction<InventoryEntry>> buildSetSupplyChannelAction(@Nonnull final InventoryEntry
                                                                                         oldEntry,
                                                                                     @Nonnull final InventoryEntryDraft
                                                                                         newEntry) {
        final Reference<Channel> oldSupplyChannel = oldEntry.getSupplyChannel();
        final Reference<Channel> newSupplyChannel = newEntry.getSupplyChannel();
        return buildUpdateAction(oldSupplyChannel, newSupplyChannel, () -> SetSupplyChannel.of(newSupplyChannel));
    }
}
