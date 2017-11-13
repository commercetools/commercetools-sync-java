package com.commercetools.sync.inventories;

import com.commercetools.sync.commons.BaseSyncOptions;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class InventorySyncOptions extends BaseSyncOptions<InventoryEntry> {
    private final boolean ensureChannels;

    InventorySyncOptions(@Nonnull final SphereClient ctpClient,
                         @Nullable final BiConsumer<String, Throwable> updateActionErrorCallBack,
                         @Nullable final Consumer<String> updateActionWarningCallBack,
                         final int batchSize,
                         final boolean removeOtherLocales,
                         final boolean removeOtherSetEntries,
                         final boolean removeOtherCollectionEntries,
                         final boolean removeOtherProperties,
                         final boolean allowUuid,
                         boolean ensureChannels,
                         @Nullable final Function<List<UpdateAction<InventoryEntry>>,
                             List<UpdateAction<InventoryEntry>>> beforeUpdateCallback) {
        super(ctpClient,
            updateActionErrorCallBack,
            updateActionWarningCallBack,
            batchSize,
            removeOtherLocales,
            removeOtherSetEntries,
            removeOtherCollectionEntries,
            removeOtherProperties,
            allowUuid,
            beforeUpdateCallback);
        this.ensureChannels = ensureChannels;

    }

    /**
     * @return option that indicates whether sync process should create supply channel of given key when it doesn't
     *      exists in a target system yet.
     */
    public boolean shouldEnsureChannels() {
        return ensureChannels;
    }


}
