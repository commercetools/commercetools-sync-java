package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.RevertStagedChanges;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.queries.QueryPredicate;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.StringUtils.isBlank;


public final class ProductServiceImpl extends BaseService<Product, ProductDraft> implements ProductService {
    public ProductServiceImpl(@Nonnull final ProductSyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> getIdFromCacheOrFetch(@Nullable final String key) {
        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        if (keyToIdCache.containsKey(key)) {
            return CompletableFuture.completedFuture(Optional.of(keyToIdCache.get(key)));
        }
        return fetchAndCache(key);
    }

    @Nonnull
    private CompletionStage<Optional<String>> fetchAndCache(@Nonnull final String key) {
        return cacheKeysToIds(singleton(key)).thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> productKeys) {
        final Set<String> keysNotCached = productKeys.stream()
                                                     .filter(StringUtils::isNotBlank)
                                                     .filter(key -> !keyToIdCache.containsKey(key))
                                                     .collect(Collectors.toSet());

        if (keysNotCached.isEmpty()) {
            return CompletableFuture.completedFuture(keyToIdCache);
        }

        final ProductQuery productQuery = ProductQuery.of()
                                                      .withPredicates(buildProductKeysQueryPredicate(keysNotCached));

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), productQuery, this::cacheProductIds)
                            .thenApply(result -> keyToIdCache);
    }

    private void cacheProductIds(@Nonnull final List<Product> products) {
        products.forEach(product -> keyToIdCache.put(product.getKey(), product.getId()));
    }

    QueryPredicate<Product> buildProductKeysQueryPredicate(@Nonnull final Set<String> productKeys) {
        final List<String> keysSurroundedWithDoubleQuotes = productKeys.stream()
                                                                       .filter(StringUtils::isNotBlank)
                                                                       .map(productKey -> format("\"%s\"", productKey))
                                                                       .collect(Collectors.toList());
        String keysQueryString = keysSurroundedWithDoubleQuotes.toString();
        // Strip square brackets from list string. For example: ["key1", "key2"] -> "key1", "key2"
        keysQueryString = keysQueryString.substring(1, keysQueryString.length() - 1);
        return QueryPredicate.of(format("key in (%s)", keysQueryString));
    }

    @Nonnull
    @Override
    public CompletionStage<Set<Product>> fetchMatchingProductsByKeys(@Nonnull final Set<String> productKeys) {
        if (productKeys.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        final ProductQuery productQuery = ProductQuery.of().withPredicates(buildProductKeysQueryPredicate(productKeys));

        return CtpQueryUtils
                .queryAll(syncOptions.getCtpClient(), productQuery, Function.identity())
                .thenApply(fetchedProducts -> fetchedProducts
                        .stream()
                        .flatMap(List::stream)
                        .peek(product -> keyToIdCache.put(product.getKey(), product.getId()))
                        .collect(Collectors.toSet()));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Product>> fetchProduct(@Nullable final String key) {
        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        final ProductQuery productQuery = ProductQuery
                .of().withPredicates(buildProductKeysQueryPredicate(singleton(key)));

        return syncOptions
                .getCtpClient()
                .execute(productQuery)
                .thenApply(productPagedQueryResult ->
                        productPagedQueryResult
                                .head()
                                .map(product -> {
                                    keyToIdCache.put(product.getKey(), product.getId());
                                    return product;
                                }));
    }

    @Nonnull
    @Override
    public CompletionStage<Set<Product>> createProducts(@Nonnull final Set<ProductDraft> productsDrafts) {
        return mapValuesToFutureOfCompletedValues(productsDrafts, this::createProduct)
            .thenApply(results -> results.filter(Optional::isPresent).map(Optional::get))
            .thenApply(createdProducts -> createdProducts.collect(Collectors.toSet()));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Product>> createProduct(@Nonnull final ProductDraft productDraft) {
        return applyCallbackAndCreate(productDraft, productDraft.getKey(), ProductCreateCommand::of);
    }

    @Nonnull
    @Override
    public CompletionStage<Product> updateProduct(@Nonnull final Product product,
                                                  @Nonnull final List<UpdateAction<Product>> updateActions) {
        return updateResource(product, ProductUpdateCommand::of, updateActions);
    }

    @Nonnull
    @Override
    public CompletionStage<Product> publishProduct(@Nonnull final Product product) {
        return syncOptions.getCtpClient().execute(ProductUpdateCommand.of(product, Publish.of()));
    }

    @Nonnull
    @Override
    public CompletionStage<Product> revertProduct(@Nonnull final Product product) {
        return syncOptions.getCtpClient().execute(ProductUpdateCommand.of(product, RevertStagedChanges.of()));
    }
}
