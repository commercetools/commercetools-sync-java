package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;

public final class ProductSyncOptionsBuilder
    extends BaseSyncOptionsBuilder<ProductSyncOptionsBuilder, ProductSyncOptions> {
    public static final int BATCH_SIZE_DEFAULT = 30;
    private boolean removeOtherVariants = true;
    private List<String> whiteList = emptyList();
    private List<String> blackList = emptyList();
    private Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> updateActionsFilter;
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

    public ProductSyncOptionsBuilder whiteList(@Nonnull final List<String> whiteList) {
        this.whiteList = whiteList;
        return this;
    }

    public ProductSyncOptionsBuilder blackList(@Nonnull final List<String> blackList) {
        this.blackList = blackList;
        return this;
    }

    public ProductSyncOptionsBuilder setUpdateActionsFilter(@Nonnull final Function<List<UpdateAction<Product>>,
        List<UpdateAction<Product>>> updateActionsFilter) {
        this.updateActionsFilter = updateActionsFilter;
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
            whiteList,
            blackList,
            updateActionsFilter,
            ensurePriceChannels
        );
    }


    @Override
    protected ProductSyncOptionsBuilder getThis() {
        return this;
    }
}
