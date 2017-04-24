package com.commercetools.sync.inventory.utils;

import com.commercetools.sync.inventory.helpers.InventorySyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildCustomUpdateActions;
import static com.commercetools.sync.inventory.utils.InventoryDraftTransformer.transformToDraft;
import static com.commercetools.sync.inventory.utils.InventoryUpdateActionUtils.*;

//TODO document
//TODO test
public final class InventorySyncUtils {

    private InventorySyncUtils() {
        throw new AssertionError();
    }

    @Nonnull
    public static List<UpdateAction<InventoryEntry>> buildActions(@Nonnull final InventoryEntry oldEntry,
                                                                  @Nonnull final InventoryEntry newEntry,
                                                                  @Nonnull final InventorySyncOptions syncOptions) {
        final InventoryEntryDraft newEntryDraft = transformToDraft(newEntry);
        return buildActions(oldEntry, newEntryDraft, syncOptions);
    }

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
