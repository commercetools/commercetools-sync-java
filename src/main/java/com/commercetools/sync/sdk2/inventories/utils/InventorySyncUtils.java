package com.commercetools.sync.sdk2.inventories.utils;

import static com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils.buildPrimaryResourceCustomUpdateActions;
import static com.commercetools.sync.sdk2.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.sdk2.inventories.utils.InventoryUpdateActionUtils.*;

import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.inventory.InventoryEntryUpdateAction;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.inventories.InventorySyncOptions;
import com.commercetools.sync.sdk2.inventories.helpers.InventoryCustomActionBuilder;
import com.commercetools.sync.sdk2.inventories.models.InventoryEntryCustomTypeAdapter;
import com.commercetools.sync.sdk2.inventories.models.InventoryEntryDraftCustomTypeAdapter;
import java.util.List;
import javax.annotation.Nonnull;

/** This class provides factory methods for assembling update actions of inventory entries. */
public final class InventorySyncUtils {
  private static final InventoryCustomActionBuilder inventoryCustomActionBuilder =
      InventoryCustomActionBuilder.of();

  /**
   * Compares the quantityOnStock, the restockableInDays, the expectedDelivery, the supply channel
   * and Custom fields/ type fields of an {@link InventoryEntry} and an {@link InventoryEntryDraft}.
   * It returns a {@link List} of {@link InventoryEntryUpdateAction} as a result. If no update
   * action is needed an empty {@link List} is returned.
   *
   * @param oldEntry the inventory entry which should be updated
   * @param newEntry the inventory entry draft that contains new data that should be applied to
   *     {@code oldEntry}
   * @param syncOptions the sync options wrapper which contains options related to the sync process
   *     supplied by the user. For example, custom callbacks to call in case of warnings or errors
   *     occurring on the build update action process. And other options (See {@link
   *     BaseSyncOptions} for more info.
   * @return list containing {@link InventoryEntryUpdateAction} that need to be performed on {@code
   *     oldEntry} resource so that it will be synced with {@code newEntry} or empty list when both
   *     entries are already in sync.
   */
  @Nonnull
  public static List<InventoryEntryUpdateAction> buildActions(
      @Nonnull final InventoryEntry oldEntry,
      @Nonnull final InventoryEntryDraft newEntry,
      @Nonnull final InventorySyncOptions syncOptions) {

    final List<InventoryEntryUpdateAction> actions =
        filterEmptyOptionals(
            buildChangeQuantityAction(oldEntry, newEntry),
            buildSetRestockableInDaysAction(oldEntry, newEntry),
            buildSetExpectedDeliveryAction(oldEntry, newEntry),
            buildSetSupplyChannelAction(oldEntry, newEntry));

    actions.addAll(
        buildPrimaryResourceCustomUpdateActions(
            InventoryEntryCustomTypeAdapter.of(oldEntry),
            InventoryEntryDraftCustomTypeAdapter.of(newEntry),
            inventoryCustomActionBuilder,
            syncOptions));
    return actions;
  }

  private InventorySyncUtils() {}
}
