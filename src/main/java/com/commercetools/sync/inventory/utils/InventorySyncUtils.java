package com.commercetools.sync.inventory.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import java.util.List;

public final class InventorySyncUtils {

    private InventorySyncUtils() {
        throw new AssertionError();
    }

    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildActions(@Nonnull final InventoryEntry oldInventoryEntry,
                                                                  @Nonnull final InventoryEntry newInventoryEntry) {
        //TODO implement
        //TODO document
        //TODO test

        return null;
    }

    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildActions(@Nonnull final InventoryEntry oldInventoryEntry,
                                                                  @Nonnull final InventoryEntryDraft newInventoryEntry) {
        //TODO implement
        //TODO document
        //TODO test

        return null;
    }
}
