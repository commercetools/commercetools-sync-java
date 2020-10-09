package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.internals.helpers.CustomHeaderSphereClientDecorator;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;


public class ProductSyncOptionsWithUndecoratedClient extends ProductSyncOptions {

    private SphereClient ctpClient;

    public ProductSyncOptionsWithUndecoratedClient(@Nonnull final SphereClient ctpClient,
                                            @Nullable final QuadConsumer<SyncException, Optional<ProductDraft>, Optional<Product>,
                                                                             List<UpdateAction<Product>>> errorCallBack,
                                            @Nullable final TriConsumer<SyncException, Optional<ProductDraft>, Optional<Product>>
                           warningCallBack,
                                            final int batchSize,
                                            @Nullable final SyncFilter syncFilter,
                                            @Nullable final TriFunction<List<UpdateAction<Product>>, ProductDraft, Product,
                           List<UpdateAction<Product>>> beforeUpdateCallback,
                                            @Nullable final Function<ProductDraft, ProductDraft> beforeCreateCallback,
                                            boolean ensurePriceChannels) {
        super(ctpClient, errorCallBack, warningCallBack, batchSize, syncFilter, beforeUpdateCallback,
            beforeCreateCallback, ensurePriceChannels);
        this.ctpClient = ctpClient;
    }

    @Nonnull
    @Override
    public SphereClient getCtpClient() {
        return this.ctpClient;
    }
}
