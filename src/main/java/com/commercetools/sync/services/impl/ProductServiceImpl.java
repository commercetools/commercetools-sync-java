package com.commercetools.sync.services.impl;

import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class ProductServiceImpl implements ProductService {
    public ProductServiceImpl() {
    }

    @Override
    public CompletionStage<Optional<Product>> fetch(final String productKey) {
        return null;
    }

    @Override
    public CompletionStage<Void> create(final ProductDraft productDraft) {
        return null;
    }

    @Override
    public CompletionStage<Void> update(final Product product, final List<UpdateAction<Product>> updateActions) {
        return null;
    }
}
