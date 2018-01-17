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

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.StringUtils.isBlank;


public class ProductServiceImpl extends BaseService<Product, ProductDraft> implements ProductService {
    private static final String FETCH_FAILED = "Failed to fetch products with keys: '%s'. Reason: %s";

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
        return cacheAndFetch(key);
    }

    @Nonnull
    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key) {
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

    private void cacheProductIds(@Nonnull final List<Product> products){
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

        final Function<List<Product>, List<Product>> productPageCallBack = productsPage -> productsPage;
        final QueryPredicate<Product> queryPredicate = buildProductKeysQueryPredicate(productKeys);

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), ProductQuery.of().withPredicates(queryPredicate),
            productPageCallBack)
                            .handle((fetchedProducts, sphereException) -> {
                                if (sphereException != null) {
                                    syncOptions
                                        .applyErrorCallback(format(FETCH_FAILED, productKeys, sphereException),
                                            sphereException);
                                    return Collections.emptySet();
                                }
                                return fetchedProducts.stream()
                                                      .flatMap(List::stream)
                                                      .peek(product ->
                                                          keyToIdCache.put(product.getKey(), product.getId()))
                                                      .collect(Collectors.toSet());
                            });
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Product>> fetchProduct(@Nullable final String key) {
        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        final QueryPredicate<Product> queryPredicate = buildProductKeysQueryPredicate(singleton(key));
        return syncOptions.getCtpClient().execute(ProductQuery.of().withPredicates(queryPredicate))
                          .thenApply(productPagedQueryResult -> //Cache after fetch
                              productPagedQueryResult.head()
                                                     .map(product -> {
                                                         keyToIdCache.put(product.getKey(), product.getId());
                                                         return product;
                                                     })
                          )
                          .exceptionally(sphereException -> {
                              syncOptions
                                      .applyErrorCallback(format(FETCH_FAILED, key, sphereException), sphereException);
                              return Optional.empty();
                          });
    }

    @Nonnull
    @Override
    public CompletionStage<Set<Product>> createProducts(@Nonnull final Set<ProductDraft> productsDrafts) {
        final List<CompletableFuture<Optional<Product>>> futureCreations =
            productsDrafts.stream()
                          .map(this::createProduct)
                          .map(CompletionStage::toCompletableFuture)
                          .collect(Collectors.toList());
        return CompletableFuture.allOf(futureCreations.toArray(new CompletableFuture[futureCreations.size()]))
                                .thenApply(result -> futureCreations.stream()
                                                                    .map(CompletionStage::toCompletableFuture)
                                                                    .map(CompletableFuture::join)
                                                                    .filter(Optional::isPresent)
                                                                    .map(Optional::get)
                                                                    .collect(Collectors.toSet()));
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
