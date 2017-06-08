package com.commercetools.sync.products.actions;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.commercetools.sync.products.helpers.ProductSyncUtils.masterData;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;

final class ActionUtils {

    private ActionUtils() {
    }

    static <X> Optional<UpdateAction<Product>> actionOnProductData(final Product product,
                                                                   final ProductSyncOptions syncOptions,
                                                                   final Function<ProductData, X> productValue,
                                                                   final X draftValue,
                                                                   final Function<X, UpdateAction<Product>> action) {
        ProductData productData = masterData(product, syncOptions);
        return isNull(productData)
            ? Optional.empty()
            : action(productValue.apply(productData), draftValue, action);
    }

    private static <X> Optional<UpdateAction<Product>> action(final X oldValue,
                                                              final X newValue,
                                                              final Function<X, UpdateAction<Product>> action) {
        return !Objects.equals(oldValue, newValue)
            ? Optional.of(action.apply(newValue))
            : Optional.empty();
    }

    static <X> List<UpdateAction<Product>> actionsOnProductData(final Product product,
                                                                final ProductSyncOptions syncOptions,
                                                                final Function<ProductData, X> productValue,
                                                                final X draftValue,
                                                                final BiFunction<X, X, List<UpdateAction<Product>>>
                                                                    actions) {
        ProductData productData = masterData(product, syncOptions);
        return isNull(productData)
            ? emptyList()
            : actions(productValue.apply(productData), draftValue, actions);
    }

    private static <X> List<UpdateAction<Product>> actions(final X oldValue,
                                                           final X newValue,
                                                           final BiFunction<X, X, List<UpdateAction<Product>>>
                                                               actions) {
        return !Objects.equals(oldValue, newValue)
            ? actions.apply(oldValue, newValue)
            : emptyList();
    }
}
