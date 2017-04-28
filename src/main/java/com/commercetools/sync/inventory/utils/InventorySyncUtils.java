package com.commercetools.sync.inventory.utils;

import com.commercetools.sync.inventory.InventorySyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildCustomUpdateActions;
import static com.commercetools.sync.inventory.utils.InventoryDraftTransformer.transformToDraft;
import static com.commercetools.sync.inventory.utils.InventoryUpdateActionUtils.*;

//TODO test
/**
 * This class provides static utility methods for synchronising inventory entries.
 */
public final class InventorySyncUtils {

    private InventorySyncUtils() {
        throw new AssertionError();
    }

    /**
     * This method returns list of {@link UpdateAction} that needs to be performed on {@code oldEntry} resource so
     * that it will be synced with {@code newEntry} resource.
     *
     * @param oldEntry entry which data should be updated
     * @param newEntry entry that contain current data (that should be applied to {@code oldEntry}
     * @param syncOptions synchronisation process options
     * @return list containing {@link UpdateAction} that need to be performed on {@code oldEntry} resource so
     * that it will be synced with {@code newEntry} or empty list when both entries are already synced.
     */
    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildActions(@Nonnull final InventoryEntry oldEntry,
                                                                  @Nonnull final InventoryEntry newEntry,
                                                                  @Nonnull final InventorySyncOptions syncOptions) {
        final InventoryEntryDraft newEntryDraft = transformToDraft(newEntry);
        return buildActions(oldEntry, newEntryDraft, syncOptions);
    }

    /**
     * This method returns list of {@link UpdateAction} that needs to be performed on {@code oldEntry} resource so
     * that it will be synced with {@code newEntry} draft.
     *
     * @param oldEntry entry which data should be updated
     * @param newEntry draft that contain current data (that should be applied to {@code oldEntry}
     * @param syncOptions synchronisation process options
     * @return list containing {@link UpdateAction} that need to be performed on {@code oldEntry} resource so
     * that it will be synced with {@code newEntry} or empty list when both entries are already synced.
     */
    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildActions(@Nonnull final InventoryEntry oldEntry,
                                                                  @Nonnull final InventoryEntryDraft newEntry,
                                                                  @Nonnull final InventorySyncOptions syncOptions) {
        final List<UpdateAction<InventoryEntry>> actions = new LinkedList<>();
        actions.addAll(buildChangeQuantityAction(oldEntry, newEntry));
        actions.addAll(buildSetRestockableInDaysAction(oldEntry, newEntry));
        actions.addAll(buildSetExpectedDeliveryAction(oldEntry, newEntry));
        actions.addAll(buildSetSupplyChannelAction(oldEntry, newEntry));
        actions.addAll(buildCustomUpdateActions(oldEntry, newEntry, syncOptions));
        return actions;
    }
}
