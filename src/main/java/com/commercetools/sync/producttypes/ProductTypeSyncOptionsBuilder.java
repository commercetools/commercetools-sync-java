package com.commercetools.sync.producttypes;

import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;

import javax.annotation.Nonnull;

public final class ProductTypeSyncOptionsBuilder extends BaseSyncOptionsBuilder<ProductTypeSyncOptionsBuilder,
    ProductTypeSyncOptions, ProductType, ProductTypeDraft> {

    public static final int BATCH_SIZE_DEFAULT = 50;

    private ProductTypeSyncOptionsBuilder(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    /**
     * Creates a new instance of {@link ProductTypeSyncOptionsBuilder} given a {@link SphereClient} responsible for
     * interaction with the target CTP project, with the default batch size ({@code BATCH_SIZE_DEFAULT} = 50).
     *
     * @param ctpClient instance of the {@link SphereClient} responsible for interaction with the target CTP project.
     * @return new instance of {@link ProductTypeSyncOptionsBuilder}
     */
    public static ProductTypeSyncOptionsBuilder of(@Nonnull final SphereClient ctpClient) {
        return new ProductTypeSyncOptionsBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
    }

    /**
     * Creates new instance of {@link ProductTypeSyncOptions} enriched with all attributes provided to {@code this}
     * builder.
     *
     * @return new instance of {@link ProductTypeSyncOptions}
     */
    @Override
    public ProductTypeSyncOptions build() {
        return new ProductTypeSyncOptions(
            ctpClient,
            errorCallback,
            warningCallback,
            batchSize,
            beforeUpdateCallback,
            beforeCreateCallback
        );
    }

    /**
     * Returns an instance of this class to be used in the super class generic methods. Please see the JavaDoc in the
     * overridden method for further details.
     *
     * @return an instance of this class.
     */
    @Override
    protected ProductTypeSyncOptionsBuilder getThis() {
        return this;
    }
}
