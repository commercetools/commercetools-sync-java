package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

public final class ProductSyncOptionsBuilder
    extends BaseSyncOptionsBuilder<ProductSyncOptionsBuilder, ProductSyncOptions> {
    public static final int BATCH_SIZE_DEFAULT = 30;
    private boolean removeOtherVariants = true;
    private SyncFilter syncFilter;
    private Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> updateActionsCallBack;
    static final boolean ENSURE_CHANNELS_DEFAULT = false;
    private boolean ensurePriceChannels = ENSURE_CHANNELS_DEFAULT;

    private ProductSyncOptionsBuilder(final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    public static ProductSyncOptionsBuilder of(@Nonnull final SphereClient ctpClient) {
        return new ProductSyncOptionsBuilder(ctpClient).setBatchSize(BATCH_SIZE_DEFAULT);
    }

    public ProductSyncOptionsBuilder removeOtherVariants(final boolean removeOtherVariants) {
        this.removeOtherVariants = removeOtherVariants;
        return this;
    }

    /**
     * TODO
     * @param filters
     * @param filterType
     * @return
     */
    public ProductSyncOptionsBuilder setSyncFilter(@Nonnull final List<UpdateFilter> filters,
                                                   @Nonnull final UpdateFilterType filterType) {
        this.syncFilter = SyncFilter.of(filters, filterType);
        return this;
    }

    /**
     * TODO
     * @param syncFilter
     * @return
     */
    public ProductSyncOptionsBuilder setSyncFilter(@Nonnull final SyncFilter syncFilter) {
        this.syncFilter = syncFilter;
        return this;
    }

    /**
     * TODO
     * @param updateActionsCallBack
     * @return
     */
    public ProductSyncOptionsBuilder setUpdateActionsFilterCallBack(@Nonnull final Function<List<UpdateAction<Product>>,
        List<UpdateAction<Product>>> updateActionsCallBack) {
        this.updateActionsCallBack = updateActionsCallBack;
        return this;
    }

    /**
     * Set option that indicates whether sync process should create price channel of given key when it doesn't exists
     * in a target project yet. If set to {@code true} sync process would try to create new price channel of given key,
     * otherwise sync process would log error and fail to process draft with given price channel key.
     *
     * <p>This property is {@link ProductSyncOptionsBuilder#ENSURE_CHANNELS_DEFAULT} by default.
     *
     * @param ensurePriceChannels boolean that indicates whether sync process should create price channel of given key
     *                            when it doesn't exists in a target project yet
     * @return {@code this} instance of {@link ProductSyncOptionsBuilder}
     */
    public ProductSyncOptionsBuilder ensurePriceChannels(final boolean ensurePriceChannels) {
        this.ensurePriceChannels = ensurePriceChannels;
        return this;
    }

    @Override
    public ProductSyncOptions build() {
        return new ProductSyncOptions(
            ctpClient,
            errorCallBack,
            warningCallBack,
            batchSize,
            removeOtherLocales,
            removeOtherSetEntries,
            removeOtherCollectionEntries,
            removeOtherProperties,
            allowUuid,
            removeOtherVariants,
            syncFilter,
            updateActionsCallBack,
            ensurePriceChannels
        );
    }


    @Override
    protected ProductSyncOptionsBuilder getThis() {
        return this;
    }
}
