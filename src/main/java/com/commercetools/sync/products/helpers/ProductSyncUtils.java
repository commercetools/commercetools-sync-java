package com.commercetools.sync.products.helpers;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;

import javax.annotation.Nullable;

public class ProductSyncUtils {

    /**
     * Provides the {@link ProductData} from {@code product} resource for its staged or current projection
     * based on configuration given in {@code syncOptions}.
     *
     * @param product the product resource
     * @param syncOptions the configuration of synchronization
     * @return the {@link ProductData} for staged or current projection
     */
    @Nullable
    public static ProductData masterData(final Product product, final ProductSyncOptions syncOptions) {
        return syncOptions.shouldCompareStaged()
            ? product.getMasterData().getStaged()
            : product.getMasterData().getCurrent();
    }
}
