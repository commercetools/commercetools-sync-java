package com.commercetools.sync.services.impl;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.expansion.ProductExpansionModel;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.products.queries.ProductQueryModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.commons.utils.CtpQueryUtils.buildResourceKeysQueryPredicate;
import static java.util.Collections.singleton;


public final class ProductServiceImpl extends BaseServiceWithKey<ProductDraft, Product, ProductSyncOptions,
    ProductQuery, ProductQueryModel, ProductExpansionModel<Product>> implements ProductService {

    public ProductServiceImpl(@Nonnull final ProductSyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> getIdFromCacheOrFetch(@Nullable final String key) {

        return fetchCachedResourceId(key,
            () -> ProductQuery.of()
                              .withPredicates(buildResourceKeysQueryPredicate(singleton(key))));
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> productKeys) {

        return cacheKeysToIds(
            productKeys,
            keysNotCached -> ProductQuery
                .of()
                .withPredicates(buildResourceKeysQueryPredicate(keysNotCached)));
    }

    @Nonnull
    @Override
    public CompletionStage<Set<Product>> fetchMatchingProductsByKeys(@Nonnull final Set<String> productKeys) {

        return fetchMatchingResources(productKeys,
            () -> ProductQuery.of().withPredicates(buildResourceKeysQueryPredicate(productKeys)));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Product>> fetchProduct(@Nullable final String key) {

        return fetchResource(key,
            () -> ProductQuery
                .of().withPredicates(buildResourceKeysQueryPredicate(singleton(key))));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Product>> createProduct(@Nonnull final ProductDraft productDraft) {
        return createResource(productDraft, ProductCreateCommand::of);
    }

    @Nonnull
    @Override
    public CompletionStage<Product> updateProduct(@Nonnull final Product product,
                                                  @Nonnull final List<UpdateAction<Product>> updateActions) {
        return updateResource(product, ProductUpdateCommand::of, updateActions);
    }
}