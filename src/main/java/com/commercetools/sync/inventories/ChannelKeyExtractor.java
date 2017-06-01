package com.commercetools.sync.inventories;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class ChannelKeyExtractor {

    /**
     * Returns {@link String} that is supposed to be supply channel {@code key} of given {@link InventoryEntryDraft}.
     * Function results in:
     * <ul>
     *     <li>
     *         referenced id - when supply channel {@link io.sphere.sdk.models.Reference} is present but not expanded
     *     </li>
     *     <li>
     *         referenced object's key - when supply channel {@link io.sphere.sdk.models.Reference} is present and
     *         expanded
     *     </li>
     *     <li>{@code null} - otherwise</li>
     * </ul>
     * @param newEntryDraft non-null {@link InventoryEntryDraft} instance
     * @return {@link String} that is supposed to be supply channel key or {@code null} if draft doesn't belong to
     *      any supply channel
     */
    @Nullable
    static String extractChannelKey(@Nonnull final InventoryEntryDraft newEntryDraft) {
        final Reference<Channel> supplyChannel = newEntryDraft.getSupplyChannel();
        if (supplyChannel != null) {
            if (supplyChannel.getObj() != null) {
                return supplyChannel.getObj().getKey();
            } else {
                return supplyChannel.getId();
            }
        }
        return null;
    }
}
