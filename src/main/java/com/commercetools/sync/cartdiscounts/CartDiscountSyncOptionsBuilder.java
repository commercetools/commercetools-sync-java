package com.commercetools.sync.cartdiscounts;

import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.client.SphereClient;

import javax.annotation.Nonnull;

public final class CartDiscountSyncOptionsBuilder extends BaseSyncOptionsBuilder<CartDiscountSyncOptionsBuilder,
    CartDiscountSyncOptions, CartDiscount, CartDiscountDraft> {

    public static final int BATCH_SIZE_DEFAULT = 50;

    private CartDiscountSyncOptionsBuilder(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    /**
     * Creates a new instance of {@link CartDiscountSyncOptionsBuilder} given a {@link SphereClient} responsible for
     * interaction with the target CTP project, with the default batch size ({@code BATCH_SIZE_DEFAULT} = 50).
     *
     * @param ctpClient instance of the {@link SphereClient} responsible for interaction with the target CTP project.
     * @return new instance of {@link CartDiscountSyncOptionsBuilder}
     */
    public static CartDiscountSyncOptionsBuilder of(@Nonnull final SphereClient ctpClient) {
        return new CartDiscountSyncOptionsBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
    }

    /**
     * Creates new instance of {@link CartDiscountSyncOptions} enriched with all attributes provided to {@code this}
     * builder.
     *
     * @return new instance of {@link CartDiscountSyncOptions}
     */
    @Override
    public CartDiscountSyncOptions build() {
        return new CartDiscountSyncOptions(
            ctpClient,
            errorCallback,
            warningCallback,
            batchSize,
            beforeUpdateCallback,
            beforeCreateCallback,
            cacheSize
        );
    }

    /**
     * Returns an instance of this class to be used in the superclass's generic methods. Please see the JavaDoc in the
     * overridden method for further details.
     *
     * @return an instance of this class.
     */
    @Override
    protected CartDiscountSyncOptionsBuilder getThis() {
        return this;
    }
}
