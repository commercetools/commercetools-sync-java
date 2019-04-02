package com.commercetools.sync.inventories;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadriConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class InventorySyncOptions extends BaseSyncOptions<InventoryEntry, InventoryEntryDraft> {
    private final boolean ensureChannels;

    InventorySyncOptions(
        @Nonnull final SphereClient ctpClient,
        @Nullable final QuadriConsumer<SyncException, Optional<InventoryEntry>, Optional<InventoryEntryDraft>,
            Optional<List<UpdateAction<InventoryEntry>>>> updateActionErrorCallBack,
        @Nullable final TriConsumer<SyncException, Optional<InventoryEntry>, Optional<InventoryEntryDraft>>
            updateActionWarningCallBack,
        final int batchSize,
        boolean ensureChannels,
        @Nullable final TriFunction<List<UpdateAction<InventoryEntry>>, InventoryEntryDraft,
            InventoryEntry, List<UpdateAction<InventoryEntry>>> beforeUpdateCallback,
        @Nullable final Function<InventoryEntryDraft, InventoryEntryDraft> beforeCreateCallback) {
        super(ctpClient,
            updateActionErrorCallBack,
            updateActionWarningCallBack,
            batchSize,
            beforeUpdateCallback,
            beforeCreateCallback);
        this.ensureChannels = ensureChannels;

    }

    /**
     * @return option that indicates whether the sync process should create a supply channel of the given key
     *      when it doesn't exist in a target system yet.
     */
    public boolean shouldEnsureChannels() {
        return ensureChannels;
    }


}
