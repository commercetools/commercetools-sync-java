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
    public static final int BATCH_SIZE_DEFAULT = 50;
    private boolean removeOtherVariants = true;
    private List<String> whiteList = emptyList();
    private List<String> blackList = emptyList();
    private Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> updateActionsFilter;

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
            updateActionsFilter
        );
    }


    @Override
    protected ProductSyncOptionsBuilder getThis() {
        return this;
    }
}
