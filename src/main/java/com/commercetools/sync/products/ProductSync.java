package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.impl.ProductServiceImpl;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.SyncUtils.batchDrafts;
import static com.commercetools.sync.products.utils.ProductSyncUtils.buildActions;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ProductSync extends BaseSync<ProductDraft, ProductSyncStatistics, ProductSyncOptions> {
    private static final String PRODUCT_DRAFT_KEY_NOT_SET = "ProductDraft with name: %s doesn't have a key.";
    private static final String PRODUCT_DRAFT_IS_NULL = "ProductDraft is null.";
    private static final String UPDATE_FAILED = "Failed to update Product with key: '%s'. Reason: %s";
    private static final String UNEXPECTED_DELETE = "Product with key: '%s' was deleted unexpectedly.";
    private final ProductService productService;

    public ProductSync(@Nonnull final ProductSyncOptions productSyncOptions) {
        this(productSyncOptions, new ProductServiceImpl(productSyncOptions));
    }

    ProductSync(@Nonnull final ProductSyncOptions productSyncOptions, @Nonnull final ProductService productService) {
        super(new ProductSyncStatistics(), productSyncOptions);
        this.productService = productService;
    }

    @Override
    protected CompletionStage<ProductSyncStatistics> process(@Nonnull final List<ProductDraft> resourceDrafts) {
        final List<List<ProductDraft>> batches = batchDrafts(resourceDrafts, syncOptions.getBatchSize());
        return syncBatches(batches, CompletableFuture.completedFuture(statistics));
    }

    @Override
    protected CompletionStage<ProductSyncStatistics> syncBatches(@Nonnull final List<List<ProductDraft>> batches,
                                                                 @Nonnull final CompletionStage<ProductSyncStatistics>
                                                                     result) {
        if (batches.isEmpty()) {
            return result;
        }
        final List<ProductDraft> firstBatch = batches.remove(0);
        return syncBatches(batches, result.thenCompose(subResult -> processBatch(firstBatch)));
    }

    @Override
    protected CompletionStage<ProductSyncStatistics> processBatch(@Nonnull final List<ProductDraft> batch) {
        return productService.cacheKeysToIds()
                             .thenCompose(keyToIdCache -> {
                                 final Set<String> productDraftKeys = getProductDraftKeys(batch);
                                 return productService.fetchMatchingProductsByKeys(productDraftKeys)
                                                      .thenCompose(matchingProducts ->
                                                          createOrUpdateProducts(matchingProducts, batch))
                                                      .thenApply(result -> {
                                                          statistics.incrementProcessed(batch.size());
                                                          return statistics;
                                                      });
                             });
    }

    @Nonnull
    private Set<String> getProductDraftKeys(@Nonnull final List<ProductDraft> productDrafts) {
        return productDrafts.stream()
                            .filter(Objects::nonNull)
                            .map(ProductDraft::getKey)
                            .filter(StringUtils::isNotBlank)
                            .collect(Collectors.toSet());
    }

    @Nonnull
    private CompletionStage<Void> createOrUpdateProducts(@Nonnull final Set<Product> matchingProducts,
                                                         @Nonnull final List<ProductDraft> productDrafts) {
        final Map<ProductDraft, Product> productsToSync = new HashMap<>();
        final Set<ProductDraft> draftsToCreate = new HashSet<>();

        for (ProductDraft productDraft : productDrafts) {
            if (productDraft != null) {
                final String productKey = productDraft.getKey();
                if (isNotBlank(productKey)) {
                    final Optional<Product> existingProduct = getProductByKeyIfExists(matchingProducts, productKey);
                    if (existingProduct.isPresent()) {
                        productsToSync.put(productDraft, existingProduct.get());
                    } else {
                        draftsToCreate.add(productDraft);
                    }
                } else {
                    final String errorMessage = format(PRODUCT_DRAFT_KEY_NOT_SET, productDraft.getName());
                    handleError(errorMessage, null);
                }
            } else {
                handleError(PRODUCT_DRAFT_IS_NULL, null);
            }
        }
        return productService.createProducts(draftsToCreate)
                             .thenAccept(createdProducts ->
                                 processCreatedProducts(createdProducts, draftsToCreate.size()))
                             .thenCompose(result -> syncProducts(productsToSync));

    }

    @Nonnull
    private static Optional<Product> getProductByKeyIfExists(@Nonnull final Set<Product> products,
                                                             @Nonnull final String key) {
        return products.stream()
                       .filter(product -> Objects.equals(product.getKey(), key))
                       .findFirst();
    }

    private void processCreatedProducts(@Nonnull final Set<Product> createdProducts,
                                        final int totalNumberOfDraftsToCreate) {
        final int numberOfFailedCreations = totalNumberOfDraftsToCreate - createdProducts.size();
        statistics.incrementFailed(numberOfFailedCreations);
        statistics.incrementCreated(createdProducts.size());
    }

    @Nonnull
    private CompletionStage<Void> syncProducts(@Nonnull final Map<ProductDraft, Product> productsToSync) {
        final List<CompletableFuture<Optional<Product>>> futureUpdates =
            productsToSync.entrySet().stream()
                          .map(entry -> buildUpdateActionsAndUpdate(entry.getValue(), entry.getKey(), false))
                          .map(CompletionStage::toCompletableFuture)
                          .collect(Collectors.toList());
        return CompletableFuture.allOf(futureUpdates.toArray(new CompletableFuture[futureUpdates.size()]));
    }

    /**
     * Given an existing {@link Product} and a new {@link ProductDraft}, first resolves all references on the category
     * draft, then it calculates all the update actions required to synchronize the existing category to be the
     * same as the new one. If there are update actions found, a request is made to CTP to update the
     * existing category, otherwise it doesn't issue a request.
     *
     * @param oldProduct the category which could be updated.
     * @param newProduct the category draft where we get the new data.
     * @return a future which contains an empty result after execution of the update.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    private CompletionStage<Optional<Product>> buildUpdateActionsAndUpdate(@Nonnull final Product oldProduct,
                                                                           @Nonnull final ProductDraft newProduct,
                                                                           final boolean retry) {
        if (retry) {
            final String key = oldProduct.getKey();
            return productService.fetchProduct(key)
                                 .thenCompose(productOptional -> {
                                     if (productOptional.isPresent()) {
                                         final Product fetchedProduct = productOptional.get();
                                         final List<UpdateAction<Product>> updateActions =
                                             buildActions(fetchedProduct, newProduct, syncOptions);
                                         if (!updateActions.isEmpty()) {
                                             return updateProduct(fetchedProduct, newProduct, updateActions);
                                         }
                                         return CompletableFuture.completedFuture(productOptional);
                                     } else {
                                         handleError(format(UPDATE_FAILED, key, UNEXPECTED_DELETE), null);
                                         return CompletableFuture.completedFuture(productOptional);
                                     }
                                 });
        } else {
            final List<UpdateAction<Product>> updateActions = buildActions(oldProduct, newProduct, syncOptions);
            if (!updateActions.isEmpty()) {
                return updateProduct(oldProduct, newProduct, updateActions);
            }
            return CompletableFuture.completedFuture(Optional.of(oldProduct));
        }
    }

    @Nonnull
    private CompletionStage<Optional<Product>> updateProduct(@Nonnull final Product oldProduct,
                                                             @Nonnull final ProductDraft newProduct,
                                                             @Nonnull final List<UpdateAction<Product>> updateActions) {
        return productService.updateProduct(oldProduct, updateActions)
                             .handle(ImmutablePair::new)
                             .thenCompose(updateResponse -> {
                                 final Product updatedProduct = updateResponse.getKey();
                                 final Throwable sphereException = updateResponse.getValue();
                                 if (sphereException != null) {
                                     return retryRequestIfConcurrentModificationException(
                                         sphereException, oldProduct,
                                         () -> buildUpdateActionsAndUpdate(oldProduct, newProduct,
                                             true), UPDATE_FAILED);
                                 } else {
                                     statistics.incrementUpdated();
                                     return CompletableFuture.completedFuture(Optional.of(updatedProduct));
                                 }
                             });
    }

    /**
     * This method checks if the {@code sphereException} (thrown when trying to sync the old {@link Product} and the
     * new {@link ProductDraft}) is an instance of {@link ConcurrentModificationException}. If it is, then it executes
     * the supplied {@code request} to rebuild update actions and reissue the CTP update request. Otherwise, if it is
     * not an instance of a  {@link ConcurrentModificationException} then it is counted as a failed product to sync.
     *
     * @param sphereException the sphere exception thrown after issuing an update request.
     * @param oldProduct      the product to update.
     * @param request         the request to re execute in case of a {@link ConcurrentModificationException}.
     * @return a future which contains an empty result after execution of the update.
     */
    @Nonnull
    private CompletionStage<Optional<Product>> retryRequestIfConcurrentModificationException(
        @Nonnull final Throwable sphereException, @Nonnull final Product oldProduct,
        @Nonnull final Supplier<CompletionStage<Optional<Product>>> request,
        @Nonnull final String errorMessage) {
        if (sphereException instanceof ConcurrentModificationException) {
            return request.get();
        } else {
            final String productKey = oldProduct.getKey();
            handleError(format(errorMessage, productKey, sphereException), sphereException);
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed products to sync.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     * @param exception    The exception that called caused the failure, if any.
     */
    private void handleError(@Nonnull final String errorMessage, @Nullable final Throwable exception) {
        syncOptions.applyErrorCallback(errorMessage, exception);
        statistics.incrementFailed();
    }
}
