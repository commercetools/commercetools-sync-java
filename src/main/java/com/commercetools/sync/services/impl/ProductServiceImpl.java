package com.commercetools.sync.services.impl;

import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.queries.PagedResult;
import io.sphere.sdk.queries.QueryPredicate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class ProductServiceImpl implements ProductService {
    private final SphereClient ctpClient;

    public ProductServiceImpl(final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Override
    public CompletionStage<Optional<Product>> fetch(final String productKey) {
        ProductQuery productQuery = ProductQuery.of()
                .withPredicates(QueryPredicate.of("key=\"" + productKey + "\""));
        return ctpClient.execute(productQuery).thenApply(PagedResult::head);
    }

    @Override
    public CompletionStage<Product> create(final ProductDraft productDraft) {
        return ctpClient.execute(ProductCreateCommand.of(productDraft));
    }

    @Override
    public CompletionStage<Product> update(final Product product, final List<UpdateAction<Product>> updateActions) {
        return ctpClient.execute(ProductUpdateCommand.of(product, updateActions));
    }

    @Override
    public CompletionStage<Product> publish(final Product product) {
        return ctpClient.execute(ProductUpdateCommand.of(product, Publish.of()));
    }
}
