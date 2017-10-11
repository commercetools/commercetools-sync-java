package com.commercetools.sync.inventories.utils;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildCustomUpdateActions;
import static com.commercetools.sync.commons.utils.SyncUtils.replaceCustomTypeIdWithKeys;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildChangeQuantityAction;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildSetExpectedDeliveryAction;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildSetRestockableInDaysAction;
import static com.commercetools.sync.inventories.utils.InventoryUpdateActionUtils.buildSetSupplyChannelAction;
import static java.util.stream.Collectors.toList;

/**
 * This class provides factory methods for assembling update actions of inventory entries.
 */
public final class InventorySyncUtils {

    private InventorySyncUtils() { }

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
        final List<UpdateAction<InventoryEntry>> actions =
            Stream.of(
                buildChangeQuantityAction(oldEntry, newEntry),
                buildSetRestockableInDaysAction(oldEntry, newEntry),
                buildSetExpectedDeliveryAction(oldEntry, newEntry),
                buildSetSupplyChannelAction(oldEntry, newEntry))
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .collect(toList());
        actions.addAll(buildCustomUpdateActions(oldEntry, newEntry, syncOptions));
        return actions;
    }

    /**
     * Takes a list of inventoryEntries that are supposed to have their custom type reference expanded
     * in order to be able to fetch the keys and replace the reference ids with the corresponding keys and then return
     * a new list of inventory entry drafts with their references containing keys instead of the ids.  Note that if the
     * references are not expanded for an inventory entry, the reference ids will not be replaced with keys and will
     * still have their ids in place.
     *
     * @param inventoryEntries the inventory entries to replace their reference ids with keys
     * @return a list of inventoryEntry drafts with keys instead of ids for references.
     */
    @Nonnull
    public static List<InventoryEntryDraft> replaceInventoriesReferenceIdsWithKeys(@Nonnull final List<InventoryEntry>
                                                                                       inventoryEntries) {
        return inventoryEntries
            .stream()
            .map(inventoryEntry -> {
                final CustomFieldsDraft customTypeWithKeysInReference = replaceCustomTypeIdWithKeys(inventoryEntry);
                return InventoryEntryDraftBuilder.of(inventoryEntry)
                                                 .custom(customTypeWithKeysInReference)
                                                 .build();
            })
            .collect(Collectors.toList());
    }

}
