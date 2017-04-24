package com.commercetools.sync.inventory.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncOptions;

import javax.annotation.Nonnull;

//TODO implement (GITHUB ISSUE #15)
//TODO document
public class InventorySyncOptions extends BaseSyncOptions {

    //Indicates whether create supply channel if it doesn't exists in system for key from draft.
    private boolean ensureChannels = false;

    //Indicates number of threads in a pool of executorService, that will process batches. (number of batches processed in parallel)
    private int parallelProcessing = 1;

    public InventorySyncOptions(@Nonnull final String ctpProjectKey,
                                @Nonnull final String ctpClientId,
                                @Nonnull final String ctpClientSecret) {
        super(ctpProjectKey, ctpClientId, ctpClientSecret);
    }

    public boolean isEnsureChannels() {
        return ensureChannels;
    }

    public int getParallelProcessing() {
        return parallelProcessing;
    }
}
