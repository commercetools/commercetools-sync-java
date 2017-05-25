package com.commercetools.sync.inventories;

import io.sphere.sdk.inventory.InventoryEntryDraft;

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
        String key = null;
        if (newEntryDraft.getSupplyChannel() != null) {
            if (newEntryDraft.getSupplyChannel().getObj() != null) {
                key = newEntryDraft.getSupplyChannel().getObj().getKey();
            } else {
                key = newEntryDraft.getSupplyChannel().getId();
            }
        }
        return key;
    }
}
