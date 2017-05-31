package com.commercetools.sync.inventories;

import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;

import javax.annotation.Nonnull;

/**
 * Builder for creation of {@link InventorySyncOptions}.
 */
public final class InventorySyncOptionsBuilder extends
        BaseSyncOptionsBuilder<InventorySyncOptionsBuilder, InventorySyncOptions> {

    static final boolean ENSURE_CHANNELS_DEFAULT = false;
    static final int BATCH_SIZE_DEFAULT = 30;

    private boolean ensureChannels = ENSURE_CHANNELS_DEFAULT;
    private int batchSize = BATCH_SIZE_DEFAULT;

    private InventorySyncOptionsBuilder(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    /**
     * Creates a new instance of {@link InventorySyncOptionsBuilder} given a {@link SphereClient} responsible for
     * interaction with the target CTP project.
     *
     * @param ctpClient {@link SphereClient} responsible for interaction with the target CTP project.
     * @return new instance of {@link InventorySyncOptionsBuilder}
     */
    public static InventorySyncOptionsBuilder of(@Nonnull final SphereClient ctpClient) {
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
     * <p>This property is {@link InventorySyncOptionsBuilder#BATCH_SIZE_DEFAULT} by default.
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
     * in a target project yet. If set to {@code true} sync process would try to create new supply channel of given key,
     * otherwise sync process would log error and fail to process draft with given supply channel key.
     *
     * <p>This property is {@link InventorySyncOptionsBuilder#ENSURE_CHANNELS_DEFAULT} by default.
     *
     * @param ensureChannels boolean that indicates whether sync process should create supply channel of given key when
     *                       it doesn't exists in a target project yet
     * @return {@code this} instance of {@link InventorySyncOptionsBuilder}
     */
    public InventorySyncOptionsBuilder ensureChannels(final boolean ensureChannels) {
        this.ensureChannels = ensureChannels;
        return this;
    }

    /**
     * Returns new instance of {@link InventorySyncOptions}, enriched with all attributes provided to {@code this}
     * builder.
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
                this.batchSize);
    }

    /**
     * Returns {@code this} instance of {@link InventorySyncOptionsBuilder}.
     *
     * <p><strong>Inherited doc:</strong><br/>
     *      {@inheritDoc}
     */
    @Override
    protected InventorySyncOptionsBuilder getThis() {
        return this;
    }
}
