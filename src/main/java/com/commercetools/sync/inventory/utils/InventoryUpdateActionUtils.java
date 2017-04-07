package com.commercetools.sync.inventory.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import java.util.List;

public final class InventoryUpdateActionUtils {

    private InventoryUpdateActionUtils() {
        throw new AssertionError();
    }

    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildChangeQuantityAction(@Nonnull InventoryEntry oldEntry,
                                                                               @Nonnull InventoryEntryDraft newEntry) {
        //TODO implement
        //TODO document
        //TODO test

        return null;
    }

    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildSetRestockableInDaysAction(@Nonnull InventoryEntry oldEntry,
                                                                                     @Nonnull InventoryEntryDraft newEntry) {
        //TODO implement
        //TODO document
        //TODO test

        return null;
    }

    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildSetExpectedDeliveryAction(@Nonnull InventoryEntry oldEntry,
                                                                                    @Nonnull InventoryEntryDraft newEntry) {
        //TODO implement
        //TODO document
        //TODO test

        return null;
    }

    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildSetSupplyChannelAction(@Nonnull InventoryEntry oldEntry,
                                                                                 @Nonnull InventoryEntryDraft newEntry) {
        //TODO implement
        //TODO document
        //TODO test

        return null;
    }

    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildSetCustomTypeAction(@Nonnull InventoryEntry oldEntry,
                                                                              @Nonnull InventoryEntryDraft newEntry) {
        //TODO implement
        //TODO document
        //TODO test

        return null;
    }

    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildSetCustomFieldAction(@Nonnull InventoryEntry oldEntry,
                                                                               @Nonnull InventoryEntryDraft newEntry) {
        //TODO implement
        //TODO document
        //TODO test

        return null;
    }
}
