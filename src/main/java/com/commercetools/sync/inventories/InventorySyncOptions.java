package com.commercetools.sync.inventories;

import com.commercetools.sync.commons.BaseSyncOptions;
import io.sphere.sdk.client.SphereClient;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class InventorySyncOptions extends BaseSyncOptions {

    private final boolean ensureChannels;

    private final int batchSize;

    InventorySyncOptions(@Nonnull final SphereClient ctpClient,
                         final BiConsumer<String, Throwable> updateActionErrorCallBack,
                         final Consumer<String> updateActionWarningCallBack,
                         final boolean removeOtherLocales,
                         final boolean removeOtherSetEntries,
                         final boolean removeOtherCollectionEntries,
                         final boolean removeOtherProperties,
                         final boolean allowUuid,
                         boolean ensureChannels,
                         int batchSize) {
        super(ctpClient,
            updateActionErrorCallBack,
            updateActionWarningCallBack,
            removeOtherLocales,
            removeOtherSetEntries,
            removeOtherCollectionEntries,
            removeOtherProperties,
            allowUuid);
        this.ensureChannels = ensureChannels;
        this.batchSize = batchSize;
    }

    /**
     * @return option that indicates whether sync process should create supply channel of given key when it doesn't
     *      exists in a target system yet.
     */
    public boolean shouldEnsureChannels() {
        return ensureChannels;
    }

    /**
     * @return option that indicates capacity of batch of processed inventory entries.
     * @see InventorySyncOptionsBuilder#setBatchSize(int)
     */
    public int getBatchSize() {
        return batchSize;
    }
}
