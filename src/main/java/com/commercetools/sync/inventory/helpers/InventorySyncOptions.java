package com.commercetools.sync.inventory.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncOptions;
import io.sphere.sdk.client.SphereClientConfig;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Options for customization of inventory synchronisation process
 */
public class InventorySyncOptions extends BaseSyncOptions {

    //Indicates whether create supply channel if it doesn't exists in system for key from draft.
    private boolean ensureChannels = false;

    //Indicates number of threads in a pool of executorService, that will process batches. (number of batches processed in parallel)
    private int parallelProcessing = 1;

    //Indicates capacity of batch of processed inventory entries (also limit of sku, that can be query in single API call)
    private int batchSize = 30;

    protected InventorySyncOptions(@Nonnull final SphereClientConfig clientConfig,
                                   BiConsumer<String, Throwable> updateActionErrorCallBack,
                                   Consumer<String> updateActionWarningCallBack,
                                   boolean ensureChannels, int parallelProcessing, int batchSize) {
        super(clientConfig, updateActionErrorCallBack, updateActionWarningCallBack);
        this.ensureChannels = ensureChannels;
        this.parallelProcessing = parallelProcessing;
        this.batchSize = batchSize;
    }

    /**
     *
     * @return option that indicates whether sync process should create supply channel of given key when it doesn't
     * exists in a target system yet.
     */
    public boolean isEnsureChannels() {
        return ensureChannels;
    }

    /**
     *
     * @return option that indicates parallel factor. Parallel factor means number of threads in a pool,
     * that will process batches of drafts that would be synced.
     */
    public int getParallelProcessing() {
        return parallelProcessing;
    }

    /**
     *
     * @return option that indicates capacity of batch of processed inventory entries.
     * @see InventorySyncOptionsBuilder#batchSize(int)
     */
    public int getBatchSize() {
        return batchSize;
    }
}
