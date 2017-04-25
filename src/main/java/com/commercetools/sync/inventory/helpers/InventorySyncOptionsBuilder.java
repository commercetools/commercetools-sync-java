package com.commercetools.sync.inventory.helpers;

import io.sphere.sdk.client.SphereClientConfig;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

//TODO check if some "base" buider is released, adopt if so, implement if not (?).
/**
 * Builder for creation of {@link InventorySyncOptions}
 */
public class InventorySyncOptionsBuilder {

    private SphereClientConfig clientConfig;
    private BiConsumer<String, Throwable> updateActionErrorCallBack = (s,t) -> {};
    private Consumer<String> updateActionWarningCallBack = s -> {};
    private boolean ensureChannels = false;
    private int parallelProcessing = 1;
    private int batchSize = 30;

    private InventorySyncOptionsBuilder(@Nonnull final SphereClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    /**
     * Returns new instance of {@link InventorySyncOptionsBuilder}. Takes as params configuration needed for
     * creation of SphereClient.
     *
     * @param ctpProjectKey
     * @param ctpClientId
     * @param ctpClientSecret
     * @return new instance of {@link InventorySyncOptionsBuilder}
     */
    public static InventorySyncOptionsBuilder of(@Nonnull final String ctpProjectKey,
                                                 @Nonnull final String ctpClientId,
                                                 @Nonnull final String ctpClientSecret) {
        return new InventorySyncOptionsBuilder(SphereClientConfig.of(ctpProjectKey, ctpClientId, ctpClientSecret));
    }

    /**
     * Returns new instance of {@link InventorySyncOptionsBuilder}
     *
     * @param clientConfig configuration needed for creation of SphereClient
     * @return new instance of {@link InventorySyncOptionsBuilder}
     */
    public static InventorySyncOptionsBuilder of(@Nonnull final SphereClientConfig clientConfig) {
        return new InventorySyncOptionsBuilder(clientConfig);
    }

    //TODO delete if BaseSyncOptionsBuilder with analogue method is published, document and test otherwise
    public InventorySyncOptionsBuilder setUpdateActionErrorCallBack(BiConsumer<String, Throwable> updateActionErrorCallBack) {
        this.updateActionErrorCallBack = updateActionErrorCallBack;
        return this;
    }

    //TODO delete if BaseSyncOptionsBuilder with analogue method is published, document and test otherwise
    public InventorySyncOptionsBuilder setUpdateActionWarningCallBack(Consumer<String> updateActionWarningCallBack) {
        this.updateActionWarningCallBack = updateActionWarningCallBack;
        return this;
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
     * @param batchSize int that indicates capacity of batch of processed inventory entries. Have to be positive
     *                  or else will be ignored.
     * @return {@code this} instance of {@link InventorySyncOptionsBuilder}
     */
    public InventorySyncOptionsBuilder batchSize(int batchSize) {
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
    public InventorySyncOptionsBuilder ensureChannels(boolean ensureChannels) {
        this.ensureChannels = ensureChannels;
        return this;
    }

    /**
     * Set option that indicates parallel factor. Parallel factor means number of threads in a pool,
     * that will process batches of inventories that would be synced.
     *
     * This property is {@code 1} by default.
     *
     * @param parallelProcessing int that indicates parallel factor. Have to be positive or else will be ignored.
     * @return {@code this} instance of {@link InventorySyncOptionsBuilder}
     */
    public InventorySyncOptionsBuilder parallelProcessing(int parallelProcessing) {
        if (parallelProcessing > 0) {
            this.parallelProcessing = parallelProcessing;
        }
        return this;
    }

    /**
     * Returns new instance of {@link InventorySyncOptions}, fulfilled with data provided to {@code this} builder.
     *
     * @return new instance of {@link InventorySyncOptions}
     */
    public InventorySyncOptions build() {
        return new InventorySyncOptions(clientConfig, updateActionErrorCallBack, updateActionWarningCallBack,
                ensureChannels, parallelProcessing, batchSize);
    }
}
