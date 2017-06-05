package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;

public class ProductSyncOptionsBuilder extends BaseSyncOptionsBuilder<ProductSyncOptionsBuilder, ProductSyncOptions> {
    private final BiConsumer<String, Throwable> errorCallBack;
    private final Consumer<String> warningCallBack;

    private boolean compareStaged = true;
    private boolean publish = false;
    private boolean removeOtherVariants = true;

    private List<String> whiteList = emptyList();
    private List<String> blackList = emptyList();
    private Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> actionsFilter = identity();

    private ProductSyncOptionsBuilder(final SphereClient ctpClient,
                                      final BiConsumer<String, Throwable> errorCallBack,
                                      final Consumer<String> warningCallBack) {
        this.ctpClient = ctpClient;
        this.errorCallBack = errorCallBack;
        this.warningCallBack = warningCallBack;
    }

    @Override
    public ProductSyncOptions build() {
        return new ProductSyncOptions(
                ctpClient,
                errorCallBack,
                warningCallBack,
                removeOtherLocales,
                removeOtherSetEntries,
                removeOtherCollectionEntries,
                removeOtherProperties,
                compareStaged,
                publish,
                removeOtherVariants,
                whiteList,
                blackList,
                actionsFilter
        );
    }

    public ProductSyncOptionsBuilder compareStaged(final boolean compareStaged) {
        this.compareStaged = compareStaged;
        return this;
    }

    public ProductSyncOptionsBuilder publish(final boolean publish) {
        this.publish = publish;
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

    public ProductSyncOptionsBuilder actionsFilter(final Function<List<UpdateAction<Product>>,
            List<UpdateAction<Product>>> actionsFilter) {
        this.actionsFilter = actionsFilter;
        return this;
    }

    public static ProductSyncOptionsBuilder of(@Nonnull final SphereClient ctpClient,
                                               @Nonnull final BiConsumer<String, Throwable> errorCallBack,
                                               @Nonnull final Consumer<String> warningCallBack) {
        return new ProductSyncOptionsBuilder(ctpClient, errorCallBack, warningCallBack);
    }

    @Override
    protected ProductSyncOptionsBuilder getThis() {
        return this;
    }
}
