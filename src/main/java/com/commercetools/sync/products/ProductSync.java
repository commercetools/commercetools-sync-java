package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.products.helpers.ProductReferenceResolver;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import com.commercetools.sync.services.impl.ChannelServiceImpl;
import com.commercetools.sync.services.impl.ProductServiceImpl;
import com.commercetools.sync.services.impl.ProductTypeServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
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
    private static final String FAILED_TO_RESOLVE_REFERENCES = "Failed to resolve references on "
        + "ProductDraft with key:'%s'. Reason: %s";
    private static final String FAILED_TO_FETCH_PRODUCT_TYPE = "Failed to fetch a productType for the product to "
        + "build the products' attributes metadata.";

    private final ProductService productService;
    private final ProductTypeService productTypeService;
    private final ProductReferenceResolver productReferenceResolver;

    private Map<ProductDraft, Product> productsToSync = new HashMap<>();
    private Set<ProductDraft> draftsToCreate = new HashSet<>();

    /**
     * Takes a {@link ProductSyncOptions} instance to instantiate a new {@link ProductSync} instance that could be
     * used to sync product drafts with the given products in the CTP project specified in the injected
     * {@link ProductSyncOptions} instance.
     *
     * @param productSyncOptions the container of all the options of the sync process including the CTP project client
     *                           and/or configuration and other sync-specific options.
     */
    public ProductSync(@Nonnull final ProductSyncOptions productSyncOptions) {
        this(productSyncOptions, new ProductServiceImpl(productSyncOptions),
            new ProductTypeServiceImpl(productSyncOptions), new CategoryServiceImpl(productSyncOptions),
            new TypeServiceImpl(productSyncOptions), new ChannelServiceImpl(productSyncOptions));
    }

    ProductSync(@Nonnull final ProductSyncOptions productSyncOptions, @Nonnull final ProductService productService,
                @Nonnull final ProductTypeService productTypeService, @Nonnull final CategoryService categoryService,
                @Nonnull final TypeService typeService, @Nonnull final ChannelService channelService) {
        super(new ProductSyncStatistics(), productSyncOptions);
        this.productService = productService;
        this.productTypeService = productTypeService;
        this.productReferenceResolver = new ProductReferenceResolver(productSyncOptions, productTypeService,
            categoryService, typeService, channelService);
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
        productsToSync = new HashMap<>();
        draftsToCreate = new HashSet<>();
        return productService.cacheKeysToIds()
                             .thenCompose(keyToIdCache -> {
                                 final Set<String> productDraftKeys = getProductDraftKeys(batch);
                                 return productService.fetchMatchingProductsByKeys(productDraftKeys)
                                                      .thenAccept(matchingProducts ->
                                                          processFetchedProducts(matchingProducts, batch))
                                                      .thenCompose(result -> createOrUpdateProducts())
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

    private void processFetchedProducts(@Nonnull final Set<Product> matchingProducts,
                                        @Nonnull final List<ProductDraft> productDrafts) {
        for (ProductDraft productDraft : productDrafts) {
            if (productDraft != null) {
                final String productKey = productDraft.getKey();
                if (isNotBlank(productKey)) {
                    productReferenceResolver.resolveReferences(productDraft)
                                            .thenAccept(referencesResolvedDraft -> {
                                                final Optional<Product> existingProduct =
                                                    getProductByKeyIfExists(matchingProducts, productKey);
                                                if (existingProduct.isPresent()) {
                                                    productsToSync.put(referencesResolvedDraft, existingProduct.get());
                                                } else {
                                                    draftsToCreate.add(referencesResolvedDraft);
                                                }
                                            })
                                            .exceptionally(referenceResolutionException -> {
                                                Throwable actualException = referenceResolutionException;
                                                if (referenceResolutionException instanceof CompletionException) {
                                                    actualException = referenceResolutionException.getCause();
                                                }
                                                final String errorMessage = format(FAILED_TO_RESOLVE_REFERENCES,
                                                    productDraft.getKey(), actualException);
                                                handleError(errorMessage, referenceResolutionException);
                                                return null;
                                            }).toCompletableFuture().join();
                } else {
                    final String errorMessage = format(PRODUCT_DRAFT_KEY_NOT_SET, productDraft.getName());
                    handleError(errorMessage, null);
                }
            } else {
                handleError(PRODUCT_DRAFT_IS_NULL, null);
            }
        }
    }

    @Nonnull
    private CompletionStage<Void> createOrUpdateProducts() {
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
                          .map(entry -> fetchProductAttributesMetadataAndUpdate(entry.getValue(), entry.getKey()))
                          .map(CompletionStage::toCompletableFuture)
                          .collect(Collectors.toList());
        return CompletableFuture.allOf(futureUpdates.toArray(new CompletableFuture[futureUpdates.size()]));
    }

    @Nonnull
    private CompletionStage<Optional<Product>> fetchProductAttributesMetadataAndUpdate(@Nonnull final Product
                                                                                           oldProduct,
                                                                                       @Nonnull final ProductDraft
                                                                                           newProduct) {
        return productTypeService.fetchCachedProductAttributeMetaDataMap(oldProduct.getProductType().getId())
                .thenCompose(optionalAttributesMetaDataMap ->
                        optionalAttributesMetaDataMap.map(attributeMetaDataMap -> {
                            final List<UpdateAction<Product>> updateActions =
                                    buildActions(oldProduct, newProduct, syncOptions, attributeMetaDataMap);
                            if (!updateActions.isEmpty()) {
                                return updateProduct(oldProduct, newProduct, updateActions);
                            }
                            return CompletableFuture.completedFuture(Optional.of(oldProduct));
                        }).orElseGet(() -> {
                            final String errorMessage = format(UPDATE_FAILED, oldProduct.getKey(),
                                    FAILED_TO_FETCH_PRODUCT_TYPE);
                            handleError(errorMessage, null);
                            return CompletableFuture.completedFuture(Optional.of(oldProduct));
                        })
                );
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
                                     return executeSupplierIfConcurrentModificationException(sphereException,
                                         () -> fetchAndUpdate(oldProduct, newProduct),
                                         () -> {
                                             final String productKey = oldProduct.getKey();
                                             handleError(format(UPDATE_FAILED, productKey, sphereException),
                                                 sphereException);
                                             return CompletableFuture.completedFuture(Optional.empty());
                                         });
                                 } else {
                                     statistics.incrementUpdated();
                                     return CompletableFuture.completedFuture(Optional.of(updatedProduct));
                                 }
                             });
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
    private CompletionStage<Optional<Product>> fetchAndUpdate(@Nonnull final Product oldProduct,
                                                              @Nonnull final ProductDraft newProduct) {
        final String key = oldProduct.getKey();
        return productService.fetchProduct(key)
                .thenCompose(productOptional -> productOptional
                        .map(fetchedProduct -> fetchProductAttributesMetadataAndUpdate(fetchedProduct, newProduct))
                        .orElseGet(() -> {
                            handleError(format(UPDATE_FAILED, key, UNEXPECTED_DELETE), null);
                            return CompletableFuture.completedFuture(productOptional);
                        })
                );
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
