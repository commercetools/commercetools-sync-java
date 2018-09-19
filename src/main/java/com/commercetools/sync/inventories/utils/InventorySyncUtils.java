package com.commercetools.sync.inventories.utils;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.helpers.InventoryCustomActionBuilder;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import java.util.List;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildPrimaryResourceCustomUpdateActions;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildChangeQuantityAction;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildSetExpectedDeliveryAction;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildSetRestockableInDaysAction;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildSetSupplyChannelAction;
import static java.util.Arrays.asList;

/**
 * This class provides factory methods for assembling update actions of inventory entries.
 */
public final class InventorySyncUtils {
    private static final InventoryCustomActionBuilder inventoryCustomActionBuilder = new InventoryCustomActionBuilder();

    /**
     * Compares the quantityOnStock, the restockableInDays, the expectedDelivery, the supply channel and Custom
     * fields/ type fields of an {@link InventoryEntry} and an {@link InventoryEntryDraft}. It returns a {@link List} of
     * {@link UpdateAction}&lt;{@link InventoryEntry}&gt; as a result. If no update action is needed an empty
     * {@link List} is returned.
     *
     * @param oldEntry    the inventory entry which should be updated
     * @param newEntry    the inventory entry draft that contains new data that should be applied to {@code oldEntry}
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied by
     *                    the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link BaseSyncOptions}
     *                    for more info.
     * @return list containing {@link UpdateAction} that need to be performed on {@code oldEntry} resource so
     *      that it will be synced with {@code newEntry} or empty list when both entries are already in sync.
     */
    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildActions(@Nonnull final InventoryEntry oldEntry,
                                                                  @Nonnull final InventoryEntryDraft newEntry,
                                                                  @Nonnull final InventorySyncOptions syncOptions) {
        final List<UpdateAction<InventoryEntry>> actions = filterEmptyOptionals(
            asList(
                buildChangeQuantityAction(oldEntry, newEntry),
                buildSetRestockableInDaysAction(oldEntry, newEntry),
                buildSetExpectedDeliveryAction(oldEntry, newEntry),
                buildSetSupplyChannelAction(oldEntry, newEntry)
            ));

        actions.addAll(buildPrimaryResourceCustomUpdateActions(oldEntry, newEntry, inventoryCustomActionBuilder,
            syncOptions));
        return syncOptions.applyBeforeUpdateCallBack(actions, newEntry, oldEntry);
    }

    private InventorySyncUtils() { }
}
