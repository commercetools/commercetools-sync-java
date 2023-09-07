package com.commercetools.sync.inventories.utils;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActionForReferences;

import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.channel.ChannelResourceIdentifier;
import com.commercetools.api.models.inventory.*;
import java.time.ZonedDateTime;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * This class provides static utility methods for building update actions related to inventories.
 */
public final class InventoryUpdateActionUtils {

  /**
   * Compares the {@code quantityOnStock} values of an {@link InventoryEntry} and an {@link
   * InventoryEntryDraft} and returns an {@link Optional} of update action, which would contain the
   * {@code "changeQuantity"} {@link InventoryEntryUpdateAction}. If both {@link InventoryEntry} and
   * {@link InventoryEntryDraft} have the same {@code quantityOnStock} values, then no update action
   * is needed and empty optional will be returned. If the {@code quantityOnStock} from the {@code
   * newEntry} is {@code null}, the new {@code quantityOnStock} will have a value of 0L.
   *
   * @param oldEntry the inventory entry that should be updated
   * @param newEntry the inventory entry draft which contains new quantity on stock
   * @return optional containing update action or empty optional if quantities on stock are
   *     identical
   */
  @Nonnull
  public static Optional<InventoryEntryUpdateAction> buildChangeQuantityAction(
      @Nonnull final InventoryEntry oldEntry, @Nonnull final InventoryEntryDraft newEntry) {
    final Long oldQuantityOnStock = oldEntry.getQuantityOnStock();
    final Long newQuantityOnStock =
        newEntry.getQuantityOnStock() == null
            ? NumberUtils.LONG_ZERO
            : newEntry.getQuantityOnStock();
    return buildUpdateAction(
        oldQuantityOnStock,
        newQuantityOnStock,
        () -> InventoryEntryChangeQuantityActionBuilder.of().quantity(newQuantityOnStock).build());
  }

  /**
   * Compares the {@code restockableInDays} values of an {@link InventoryEntry} and an {@link
   * InventoryEntryDraft} and returns an {@link Optional} of update action, which would contain the
   * {@code "setRestockableInDays"} {@link InventoryEntryUpdateAction}. If both {@link
   * InventoryEntry} and the {@link InventoryEntryDraft} have the same {@code restockableInDays}
   * values, then no update action is needed and empty optional will be returned.
   *
   * @param oldEntry the inventory entry that should be updated
   * @param newEntry the inventory entry draft which contains a new {@code restockableInDays} value
   * @return optional containing update action or empty optional if restockable in days are
   *     identical
   */
  @Nonnull
  public static Optional<InventoryEntryUpdateAction> buildSetRestockableInDaysAction(
      @Nonnull final InventoryEntry oldEntry, @Nonnull final InventoryEntryDraft newEntry) {
    final Long oldRestockableInDays = oldEntry.getRestockableInDays();
    final Long newRestockableInDays = newEntry.getRestockableInDays();
    return buildUpdateAction(
        oldRestockableInDays,
        newRestockableInDays,
        () ->
            InventoryEntrySetRestockableInDaysActionBuilder.of()
                .restockableInDays(newRestockableInDays)
                .build());
  }

  /**
   * Compares the {@code expectedDelivery} values of an {@link InventoryEntry} and an {@link
   * InventoryEntryDraft} and returns an {@link Optional} of update action, which would contain the
   * {@code "setExpectedDelivery"} {@link InventoryEntryUpdateAction}. If both {@link
   * InventoryEntry} and {@link InventoryEntryDraft} have the same {@code expectedDelivery} values,
   * then no update action is needed and empty optional will be returned.
   *
   * @param oldEntry the inventory entry that should be updated
   * @param newEntry the inventory entry draft which contains new expected delivery
   * @return optional containing update action or empty optional if expected deliveries are
   *     identical
   */
  @Nonnull
  public static Optional<InventoryEntryUpdateAction> buildSetExpectedDeliveryAction(
      @Nonnull final InventoryEntry oldEntry, @Nonnull final InventoryEntryDraft newEntry) {
    final ZonedDateTime oldExpectedDelivery = oldEntry.getExpectedDelivery();
    final ZonedDateTime newExpectedDelivery = newEntry.getExpectedDelivery();
    return buildUpdateAction(
        oldExpectedDelivery,
        newExpectedDelivery,
        () ->
            InventoryEntrySetExpectedDeliveryActionBuilder.of()
                .expectedDelivery(newExpectedDelivery)
                .build());
  }

  /**
   * Compares the {@code supplyChannel}s of an {@link InventoryEntry} and an {@link
   * InventoryEntryDraft} and returns an {@link Optional} of update action, which would contain the
   * {@code "setSupplyChannel"} {@link InventoryEntryUpdateAction}. If both {@link InventoryEntry}
   * and {@link InventoryEntryDraft} have the same supply channel, then no update action is needed
   * and empty optional will be returned.
   *
   * @param oldEntry the inventory entry that should be updated
   * @param newEntry the inventory entry draft which contains new supply channel
   * @return optional containing update action or empty optional if supply channels are identical
   */
  @Nonnull
  public static Optional<InventoryEntryUpdateAction> buildSetSupplyChannelAction(
      @Nonnull final InventoryEntry oldEntry, @Nonnull final InventoryEntryDraft newEntry) {
    final ChannelReference oldSupplyChannel = oldEntry.getSupplyChannel();
    final ChannelResourceIdentifier newSupplyChannel = newEntry.getSupplyChannel();
    return buildUpdateActionForReferences(
        oldSupplyChannel,
        newSupplyChannel,
        () ->
            InventoryEntrySetSupplyChannelActionBuilder.of()
                .supplyChannel(newSupplyChannel)
                .build());
  }

  private InventoryUpdateActionUtils() {}
}
