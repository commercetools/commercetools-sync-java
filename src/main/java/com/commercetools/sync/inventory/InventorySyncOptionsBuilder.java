package com.commercetools.sync.inventory;

import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import com.commercetools.sync.commons.helpers.CtpClient;

import javax.annotation.Nonnull;

/**
 * Builder for creation of {@link InventorySyncOptions}
 */
public final class InventorySyncOptionsBuilder extends
        BaseSyncOptionsBuilder<InventorySyncOptionsBuilder, InventorySyncOptions> {

    private boolean ensureChannels = false;
    private int parallelProcessing = 1;
    private int batchSize = 30;

    private InventorySyncOptionsBuilder(@Nonnull final CtpClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    /**
     * Creates a new instance of {@link InventorySyncOptionsBuilder} given a {@link CtpClient}, as a param, that contains
     * all the configuration of the CTP client.
     *
     * @param ctpClient wrapper that contains instance of the {@link io.sphere.sdk.client.SphereClientConfig} and
     *                  {@link io.sphere.sdk.client.BlockingSphereClient}
     * @return new instance of {@link InventorySyncOptionsBuilder}
     */
    public static InventorySyncOptionsBuilder of(@Nonnull final CtpClient ctpClient) {
        return new InventorySyncOptionsBuilder(ctpClient);
    }

    /**
     * Set option that indicates batch size for sync process. During the sync there is a need for fetching existing
     * inventory entries, so that they can be compared with entries provided in input. That's why input is sliced into
     * batches and then processed. It allows to reduce API calls by fetching existing inventories responding to
     * inventories from processed batch in one call.
     * E.g. value of 30 means that 30 entries from input list would be accumulated and one API call will be performed
     * for fetching entries responding to them. Then comparision and sync are performed.
     *
     * This property is {@code 30} by default.
     *
     * @param batchSize int that indicates capacity of batch of processed inventory entries. Has to be positive
     *                  or else will be ignored.
     * @return {@code this} instance of {@link InventorySyncOptionsBuilder}
     */
    public InventorySyncOptionsBuilder setBatchSize(final int batchSize) {
        if (batchSize > 0) {
            this.batchSize = batchSize;
        }
        return this;
    }

    /**
     * Set option that indicates whether sync process should create supply channel of given key when it doesn't exists
     * in a target system yet. If set to {@code true} sync process would try to create new supply channel of given key,
     * otherwise sync process would log error and fail to process draft with given supply channel key.
     *
     * This property is {@code false} by default.
     *
     * @param ensureChannels boolean that indicates whether sync process should create supply channel of given key when
     *                       it doesn't exists in a target system yet
     * @return {@code this} instance of {@link InventorySyncOptionsBuilder}
     */
    public InventorySyncOptionsBuilder ensureChannels(final boolean ensureChannels) {
        this.ensureChannels = ensureChannels;
        return this;
    }

    /**
     * Set option that indicates parallel factor. Parallel factor means number of threads in a pool,
     * that will process batches of inventories that would be synced.
     *
     * This property is {@code 1} by default.
     *
     * @param parallelProcessing int that indicates parallel factor. Has to be positive or else will be ignored.
     * @return {@code this} instance of {@link InventorySyncOptionsBuilder}
     */
    public InventorySyncOptionsBuilder setParallelProcessing(final int parallelProcessing) {
        if (parallelProcessing > 0) {
            this.parallelProcessing = parallelProcessing;
        }
        return this;
    }

    /**
     * Returns new instance of {@link InventorySyncOptions}, enriched with all attributes provided to {@code this} builder.
     *
     * @return new instance of {@link InventorySyncOptions}
     */
    @Override
    public InventorySyncOptions build() {
        return new InventorySyncOptions(
                this.ctpClient,
                this.errorCallBack,
                this.warningCallBack,
                this.removeOtherLocales,
                this.removeOtherSetEntries,
                this.removeOtherCollectionEntries,
                this.removeOtherProperties,
                this.ensureChannels,
                this.parallelProcessing,
                this.batchSize);
    }

    /**
     * Returns {@code this} instance of {@link InventorySyncOptionsBuilder}
     * <p>
     *      <strong>Inherited doc:</strong><br/>
     *      {@inheritDoc}
     * </p>
     */
    @Override
    protected InventorySyncOptionsBuilder getThis() {
        return this;
    }
}
