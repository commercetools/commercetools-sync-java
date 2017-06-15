package com.commercetools.sync.services;

import com.commercetools.sync.services.impl.ProductServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface ProductService {
    static ProductService of(final SphereClient ctpClient) {
        return new ProductServiceImpl(ctpClient);
    }

    CompletionStage<Optional<Product>> fetch(String productKey);

    CompletionStage<Product> create(ProductDraft productDraft);

    CompletionStage<Product> update(Product product, List<UpdateAction<Product>> updateActions);

    /**
     * Publishing product needs to be done independently of batch update action. Thus the service
     * provides such functionality in this method.
     *
     * @param product product to be published.
     * @return a completion stage of published product
     */
    CompletionStage<Product> publish(Product product);

    CompletionStage<Product> revert(Product product);
}
