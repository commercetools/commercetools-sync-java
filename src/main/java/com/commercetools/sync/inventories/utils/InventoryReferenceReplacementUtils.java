package com.commercetools.sync.inventories.utils;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceReplacementUtils.replaceCustomTypeIdWithKeys;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKeyReplaced;

/**
 * Util class which provides utilities that can be used when syncing resources from a source commercetools project
 * to a target one.
 */
public final class InventoryReferenceReplacementUtils {

    /**
     * Takes a list of inventoryEntries that are supposed to have their custom type and supply channel reference
     * expanded in order to be able to fetch the keys and replace the reference ids with the corresponding keys and then
     * return a new list of inventory entry drafts with their references containing keys instead of the ids.  Note that
     * if the references are not expanded for an inventory entry, the reference ids will not be replaced with keys and
     * will still have their ids in place.
     *
     * @param inventoryEntries the inventory entries to replace their reference ids with keys
     * @return a list of inventoryEntry drafts with keys instead of ids for references.
     */
    @Nonnull
    public static List<InventoryEntryDraft> replaceInventoriesReferenceIdsWithKeys(
        @Nonnull final List<InventoryEntry> inventoryEntries) {
        return inventoryEntries
            .stream()
            .map(inventoryEntry -> {
                final CustomFieldsDraft customTypeWithKeysInReference = replaceCustomTypeIdWithKeys(inventoryEntry);
                final ResourceIdentifier<Channel> channelReferenceWithKeysInReference =
                    replaceChannelReferenceIdWithKey(inventoryEntry);
                return InventoryEntryDraftBuilder.of(inventoryEntry)
                                                 .custom(customTypeWithKeysInReference)
                                                 .supplyChannel(channelReferenceWithKeysInReference)
                                                 .build();
            })
            .collect(Collectors.toList());
    }


    /**
     * Takes an inventoryEntry that is supposed to have its channel reference expanded in order to be able to fetch the
     * key and replace the reference id with the corresponding key and then return a new {@link Channel}
     * {@link ResourceIdentifier} containing the key in the id field.
     *
     * <p><b>Note:</b> The Channel reference should be expanded for the {@code inventoryEntry}, otherwise the reference
     * id will not be replaced with the key and will still have the id in place.
     *
     * @param inventoryEntry the inventoryEntry to replace its channel reference id with the key.
     *
     * @return a new {@link Channel} {@link ResourceIdentifier} containing the key in the id field.
     */
    @Nullable
    @SuppressWarnings("ConstantConditions") // NPE cannot occur due to being checked in replaceReferenceIdWithKey
    static ResourceIdentifier<Channel> replaceChannelReferenceIdWithKey(@Nonnull final InventoryEntry inventoryEntry) {

        final Reference<Channel> inventoryEntrySupplyChannel = inventoryEntry.getSupplyChannel();
        return getResourceIdentifierWithKeyReplaced(inventoryEntrySupplyChannel,
            () -> ResourceIdentifier.ofId(inventoryEntrySupplyChannel.getObj().getKey()));
    }


    private InventoryReferenceReplacementUtils() {
    }
}
