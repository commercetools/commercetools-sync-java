package com.commercetools.sync.inventories;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.helpers.CtpClient;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Options for customization of inventory synchronisation process.
 */
public final class InventorySyncOptions extends BaseSyncOptions {

    //Indicates whether create supply channel if it doesn't exists in system for key from draft.
    private final boolean ensureChannels;

    //Indicates capacity of processed inventory entries batch (also limit of sku, that can be query in single API call)
    private final int batchSize;

    InventorySyncOptions(@Nonnull final CtpClient ctpClient,
                                   final BiConsumer<String, Throwable> updateActionErrorCallBack,
                                   final Consumer<String> updateActionWarningCallBack,
                                   final boolean removeOtherLocales,
                                   final boolean removeOtherSetEntries,
                                   final boolean removeOtherCollectionEntries,
                                   final boolean removeOtherProperties,
                                   boolean ensureChannels,
                                   int batchSize) {
        super(ctpClient,
            updateActionErrorCallBack,
            updateActionWarningCallBack,
            removeOtherLocales,
            removeOtherSetEntries,
            removeOtherCollectionEntries,
            removeOtherProperties);
        this.ensureChannels = ensureChannels;
        this.batchSize = batchSize;
    }

    /**
     *
     * @return option that indicates whether sync process should create supply channel of given key when it doesn't
     *      exists in a target system yet.
     */
    public boolean shouldEnsureChannels() {
        return ensureChannels;
    }

    /**
     *
     * @return option that indicates capacity of batch of processed inventory entries.
     * @see InventorySyncOptionsBuilder#setBatchSize(int)
     */
    public int getBatchSize() {
        return batchSize;
    }
}
