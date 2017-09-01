package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;

public final class ProductSyncOptionsBuilder
    extends BaseSyncOptionsBuilder<ProductSyncOptionsBuilder, ProductSyncOptions> {
    public static final int BATCH_SIZE_DEFAULT = 50;

    private boolean updateStaged = true;
    private boolean publish = false;
    private boolean revertStagedChanges = false;
    private boolean removeOtherVariants = true;

    private List<String> whiteList = emptyList();
    private List<String> blackList = emptyList();
    private Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> actionsFilter = identity();

    private ProductSyncOptionsBuilder(final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    public static ProductSyncOptionsBuilder of(@Nonnull final SphereClient ctpClient) {
        return new ProductSyncOptionsBuilder(ctpClient).setBatchSize(BATCH_SIZE_DEFAULT);
    }

    public ProductSyncOptionsBuilder updateStaged(final boolean updateStaged) {
        this.updateStaged = updateStaged;
        return this;
    }

    public ProductSyncOptionsBuilder publish(final boolean publish) {
        this.publish = publish;
        return this;
    }

    public ProductSyncOptionsBuilder revertStagedChanges(final boolean revertStagedChanges) {
        this.revertStagedChanges = revertStagedChanges;
        return this;
    }

    public ProductSyncOptionsBuilder removeOtherVariants(final boolean removeOtherVariants) {
        this.removeOtherVariants = removeOtherVariants;
        return this;
    }

    public ProductSyncOptionsBuilder whiteList(final List<String> whiteList) {
        this.whiteList = whiteList;
        return this;
    }

    public ProductSyncOptionsBuilder blackList(final List<String> blackList) {
        this.blackList = blackList;
        return this;
    }

    public ProductSyncOptionsBuilder setUpdateActionsFilter(final Function<List<UpdateAction<Product>>,
        List<UpdateAction<Product>>> actionsFilter) {
        this.actionsFilter = actionsFilter;
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
            updateStaged,
            publish,
            revertStagedChanges,
            removeOtherVariants,
            whiteList,
            blackList,
            actionsFilter
        );
    }


    @Override
    protected ProductSyncOptionsBuilder getThis() {
        return this;
    }
}
