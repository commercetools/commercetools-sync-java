package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;

public final class ProductSyncOptionsWithUndecoratedClientBuilder
    extends BaseSyncOptionsBuilder<ProductSyncOptionsWithUndecoratedClientBuilder, ProductSyncOptions, Product, ProductDraft> {
    public static final int BATCH_SIZE_DEFAULT = 30;
    private SyncFilter syncFilter;
    static final boolean ENSURE_CHANNELS_DEFAULT = false;
    private boolean ensurePriceChannels = ENSURE_CHANNELS_DEFAULT;

    private ProductSyncOptionsWithUndecoratedClientBuilder(final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    public static ProductSyncOptionsWithUndecoratedClientBuilder of(@Nonnull final SphereClient ctpClient) {
        return new ProductSyncOptionsWithUndecoratedClientBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
    }

    /**
     * Set option that defines {@link SyncFilter} for the sync, which defines either a blacklist or a whitelist for
     * filtering certain update action groups.
     *
     * <p>The action groups can be a list of any of the values of the enum {@link ActionGroup}, namely:
     * <ul>
     * <li>ATTRIBUTES</li>
     * <li>PRICES</li>
     * <li>IMAGES</li>
     * <li>CATEGORIES</li>
     * <li>.. and others</li>
     * </ul>
     *
     *
     * @param syncFilter defines either a blacklist or a whitelist for filtering certain update action groups.
     *
     * @return {@code this} instance of {@link ProductSyncOptionsWithUndecoratedClientBuilder}
     */
    @Nonnull
    public ProductSyncOptionsWithUndecoratedClientBuilder syncFilter(@Nonnull final SyncFilter syncFilter) {
        this.syncFilter = syncFilter;
        return this;
    }

    /**
     * Set option that indicates whether sync process should create a price channel of given key when it doesn't exist
     * in a target project yet. If set to {@code true}, the sync process would try to create the new price channel of
     * the given key, otherwise the sync process would log an error and fail to process the draft with the given price
     * channel key.
     *
     * <p>This property is {@link ProductSyncOptionsWithUndecoratedClientBuilder#ENSURE_CHANNELS_DEFAULT} by default.
     *
     * @param ensurePriceChannels boolean that indicates whether sync process should create price channel of given key
     *                            when it doesn't exist in a target project yet
     * @return {@code this} instance of {@link ProductSyncOptionsWithUndecoratedClientBuilder}
     */
    @Nonnull
    public ProductSyncOptionsWithUndecoratedClientBuilder ensurePriceChannels(final boolean ensurePriceChannels) {
        this.ensurePriceChannels = ensurePriceChannels;
        return this;
    }

    @Override
    @Nonnull
    public ProductSyncOptionsWithUndecoratedClient build() {
        return new ProductSyncOptionsWithUndecoratedClient(
            ctpClient,
            errorCallback,
            warningCallback,
            batchSize,
            syncFilter,
            beforeUpdateCallback,
            beforeCreateCallback,
            ensurePriceChannels
        );
    }


    @Override
    protected ProductSyncOptionsWithUndecoratedClientBuilder getThis() {
        return this;
    }
}
