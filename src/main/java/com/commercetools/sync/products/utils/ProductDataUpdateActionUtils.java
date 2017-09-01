package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActions;
import static com.commercetools.sync.products.utils.ProductDataUtils.masterData;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;

public final class ProductDataUpdateActionUtils {

    private ProductDataUpdateActionUtils() {
    }

    static <T> Optional<UpdateAction<Product>> buildProductDataUpdateAction(final Product product,
                                                                            final ProductSyncOptions syncOptions,
                                                                            final Function<ProductData, T> oldValueGetter,
                                                                            final T newValue,
                                                                            final Supplier<UpdateAction<Product>> action) {
        final ProductData productData = masterData(product, syncOptions);
        return isNull(productData)
            ? Optional.empty() : buildUpdateAction(oldValueGetter.apply(productData), newValue, action);
    }

    static <T> List<UpdateAction<Product>> buildProductDataUpdateActions(final Product product,
                                                                         final ProductSyncOptions syncOptions,
                                                                         final Function<ProductData, T> oldValueGetter,
                                                                         final T newValue,
                                                                         final Function<T, List<UpdateAction<Product>>> actions) {
        final ProductData productData = masterData(product, syncOptions);
        return isNull(productData)
            ? emptyList() : buildUpdateActions(oldValueGetter.apply(productData), newValue, actions);
    }
}
