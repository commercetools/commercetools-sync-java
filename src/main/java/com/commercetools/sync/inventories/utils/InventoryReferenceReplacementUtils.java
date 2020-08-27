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

import static com.commercetools.sync.commons.utils.CustomTypeReferenceReplacementUtils.mapToCustomFieldsDraft;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKey;

/**
 * Util class which provides utilities that can be used when syncing resources from a source commercetools project
 * to a target one.
 */
public final class InventoryReferenceReplacementUtils {

    /**
     * Takes a list of inventoryEntries that are supposed to have their custom type and supply channel reference
     * expanded in order to be able to fetch the keys and then return a new list of inventory entry drafts with
     * their references containing keys.
     * Note that if the references are not expanded for an inventory entry, the references will not have keys.
     *
     * @param inventoryEntries the inventory entries expanded with keys
     * @return a list of inventoryEntry drafts with keys for references.
     */
    @Nonnull
    public static List<InventoryEntryDraft> mapToInventoryEntryDrafts(
        @Nonnull final List<InventoryEntry> inventoryEntries) {
        return inventoryEntries
            .stream()
            .map(inventoryEntry -> {
                final CustomFieldsDraft customTypeWithKeysInReference = mapToCustomFieldsDraft(inventoryEntry);
                final ResourceIdentifier<Channel> channelReferenceWithKeysInReference =
                    mapToChannelResourceIdentifier(inventoryEntry);
                return InventoryEntryDraftBuilder.of(inventoryEntry)
                                                 .custom(customTypeWithKeysInReference)
                                                 .supplyChannel(channelReferenceWithKeysInReference)
                                                 .build();
            })
            .collect(Collectors.toList());
    }


    /**
     * Takes an inventoryEntry that is supposed to have its channel reference expanded in order to be able to fetch the
     * key and return a new {@link Channel} {@link ResourceIdentifier} containing the key field.
     *
     * <p><b>Note:</b> The Channel reference should be expanded for the {@code inventoryEntry}, otherwise the reference
     * will not have a key in place.
     *
     * @param inventoryEntry the inventoryEntry with {@link Channel} {@link ResourceIdentifier} containing the key.
     *
     * @return a new {@link Channel} {@link ResourceIdentifier} containing the key field.
     */
    @Nullable
    @SuppressWarnings("ConstantConditions") // NPE cannot occur due to being checked in replaceReferenceIdWithKey
    static ResourceIdentifier<Channel> mapToChannelResourceIdentifier(@Nonnull final InventoryEntry inventoryEntry) {

        final Reference<Channel> inventoryEntrySupplyChannel = inventoryEntry.getSupplyChannel();
        return getResourceIdentifierWithKey(inventoryEntrySupplyChannel,
            () -> ResourceIdentifier.ofKey(inventoryEntrySupplyChannel.getObj().getKey()));
    }


    private InventoryReferenceReplacementUtils() {
    }
}
