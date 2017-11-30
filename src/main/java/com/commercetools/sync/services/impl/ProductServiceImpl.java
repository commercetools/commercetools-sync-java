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
import io.sphere.sdk.queries.PagedResult;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ProductServiceImpl implements ProductService {
    private boolean isCached = false;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();
    private final ProductSyncOptions syncOptions;

    private static final int MAXIMUM_ALLOWED_UPDATE_ACTIONS = 500;
    private static final String CREATE_FAILED = "Failed to create ProductDraft with key: '%s'. Reason: %s";
    private static final String FETCH_FAILED = "Failed to fetch products with keys: '%s'. Reason: %s";
    private static final String PRODUCT_KEY_NOT_SET = "Product with id: '%s' has no key set. Keys are required for "
        + "product matching.";

    public ProductServiceImpl(@Nonnull final ProductSyncOptions syncOptions) {
        this.syncOptions = syncOptions;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedProductId(@Nullable final String key) {
        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        if (isCached) {
            return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
        }
        return cacheAndFetch(key);
    }

    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key) {
        return cacheKeysToIds()
            .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> cacheKeysToIds() {
        if (isCached) {
            return CompletableFuture.completedFuture(keyToIdCache);
        }

        final Consumer<List<Product>> productPageConsumer = productsPage ->
            productsPage.forEach(product -> {
                final String key = product.getKey();
                final String id = product.getId();
                if (StringUtils.isNotBlank(key)) {
                    keyToIdCache.put(key, id);
                } else {
                    syncOptions.applyWarningCallback(format(PRODUCT_KEY_NOT_SET, id));
                }
            });

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), ProductQuery.of(), productPageConsumer)
                            .thenAccept(result -> isCached = true)
                            .thenApply(result -> keyToIdCache);
    }

    QueryPredicate<Product> buildProductKeysQueryPredicate(@Nonnull final Set<String> productKeys) {
        final List<String> keysSurroundedWithDoubleQuotes = productKeys.stream()
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
                                                      .collect(Collectors.toSet());
                            });
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Product>> fetchProduct(@Nullable final String key) {
        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        final QueryPredicate<Product> queryPredicate = buildProductKeysQueryPredicate(Collections.singleton(key));
        return syncOptions.getCtpClient().execute(ProductQuery.of().withPredicates(queryPredicate))
                          .thenApply(PagedResult::head)
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
        return syncOptions.applyCallbackAndCreate(productDraft, ProductCreateCommand::of, this::handleProductCreation);
    }

    @Nonnull
    private Optional<Product> handleProductCreation(
        @Nonnull final ProductDraft draft,
        @Nullable final Product createdProduct,
        @Nullable final Throwable sphereException) {
        if (createdProduct != null) {
            keyToIdCache.put(createdProduct.getKey(), createdProduct.getId());
            return Optional.of(createdProduct);
        } else {
            syncOptions.applyErrorCallback(format(CREATE_FAILED, draft.getKey(), sphereException), sphereException);
            return Optional.empty();
        }
    }

    @Nonnull
    @Override
    public CompletionStage<Product> updateProduct(@Nonnull final Product product,
                                                  @Nonnull final List<UpdateAction<Product>> updateActions) {

        final List<List<UpdateAction<Product>>> actionBatches =
            batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);
        return updateBatches(actionBatches, CompletableFuture.completedFuture(product));
    }

    private CompletionStage<Product> updateBatches(
        @Nonnull final List<List<UpdateAction<Product>>> batches,
        @Nonnull final CompletionStage<Product> result) {
        if (batches.isEmpty()) {
            return result;
        }
        final List<UpdateAction<Product>> firstBatch = batches.remove(0);
        return updateBatches(batches, result.thenCompose(updatedProduct ->
            syncOptions.getCtpClient().execute(ProductUpdateCommand.of(updatedProduct, firstBatch))));
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
