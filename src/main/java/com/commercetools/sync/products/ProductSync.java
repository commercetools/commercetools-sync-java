package com.commercetools.sync.products;

import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.products.helpers.BatchProcessor;
import com.commercetools.sync.products.helpers.ProductReferenceResolver;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import com.commercetools.sync.services.impl.ChannelServiceImpl;
import com.commercetools.sync.services.impl.ProductServiceImpl;
import com.commercetools.sync.services.impl.ProductTypeServiceImpl;
import com.commercetools.sync.services.impl.StateServiceImpl;
import com.commercetools.sync.services.impl.TaxCategoryServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
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

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static com.commercetools.sync.products.utils.ProductSyncUtils.buildActions;
import static io.sphere.sdk.states.StateType.PRODUCT_STATE;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class ProductSync extends BaseSync<ProductDraft, ProductSyncStatistics, ProductSyncOptions> {
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
    private Set<ProductDraft> existingDrafts = new HashSet<>();
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
                new ProductTypeServiceImpl(productSyncOptions),
                new CategoryServiceImpl(CategorySyncOptionsBuilder.of(productSyncOptions.getCtpClient()).build()),
                new TypeServiceImpl(productSyncOptions),
                new ChannelServiceImpl(productSyncOptions),
                new TaxCategoryServiceImpl(productSyncOptions),
                new StateServiceImpl(productSyncOptions, PRODUCT_STATE));
    }

    ProductSync(@Nonnull final ProductSyncOptions productSyncOptions, @Nonnull final ProductService productService,
                @Nonnull final ProductTypeService productTypeService, @Nonnull final CategoryService categoryService,
                @Nonnull final TypeService typeService, @Nonnull final ChannelService channelService,
                @Nonnull final TaxCategoryService taxCategoryService, @Nonnull final StateService stateService) {
        super(new ProductSyncStatistics(), productSyncOptions);
        this.productService = productService;
        this.productTypeService = productTypeService;
        this.productReferenceResolver = new ProductReferenceResolver(productSyncOptions, productTypeService,
            categoryService, typeService, channelService, taxCategoryService, stateService, productService);
    }

    @Override
    protected CompletionStage<ProductSyncStatistics> process(@Nonnull final List<ProductDraft> resourceDrafts) {
        final List<List<ProductDraft>> batches = batchElements(resourceDrafts, syncOptions.getBatchSize());
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
        existingDrafts = new HashSet<>();


        final BatchProcessor batchProcessor = new BatchProcessor(batch, this);
        batchProcessor.validateBatch();

        return productService.cacheKeysToIds(batchProcessor.getKeysToCache())
                             .thenCompose(keyToIdCache -> {
                                 prepareDraftsForProcessing(batchProcessor.getValidDrafts(), keyToIdCache);
                                 final Set<String> productDraftKeys = getProductDraftKeys(existingDrafts);
                                 return productService.fetchMatchingProductsByKeys(productDraftKeys)
                                                      .thenAccept(this::processFetchedProducts)
                                                      .thenCompose(ignoredResult -> createOrUpdateProducts())
                                                      .thenApply(ignoredResult -> {
                                                          statistics.incrementProcessed(batch.size());
                                                          return statistics;
                                                      });
                             });
    }


    private void prepareDraftsForProcessing(@Nonnull final Set<ProductDraft> productDrafts,
                                            @Nonnull final Map<String, String> keyToIdCache) {
        productDrafts
            .forEach(productDraft ->
                productReferenceResolver.resolveReferences(productDraft)
                                        .thenAccept(referencesResolvedDraft -> {
                                            if (keyToIdCache.containsKey(productDraft.getKey())) {
                                                existingDrafts.add(referencesResolvedDraft);
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
                                            handleError(errorMessage, actualException);
                                            return null;
                                        }).toCompletableFuture().join());
    }


    @Nonnull
    private Set<String> getProductDraftKeys(@Nonnull final Set<ProductDraft> productDrafts) {
        return productDrafts.stream()
                            .map(ProductDraft::getKey)
                            .collect(Collectors.toSet());
    }


    private void processFetchedProducts(@Nonnull final Set<Product> fetchedProducts) {
        existingDrafts.forEach(existingDraft ->
            getProductByKeyIfExists(fetchedProducts, requireNonNull(existingDraft.getKey()))
                .ifPresent(product -> productsToSync.put(existingDraft, product)));
    }

    @Nonnull
    private static Optional<Product> getProductByKeyIfExists(@Nonnull final Set<Product> products,
                                                             @Nonnull final String key) {
        return products.stream()
                       .filter(product -> Objects.equals(product.getKey(), key))
                       .findFirst();
    }

    @Nonnull
    private CompletionStage<List<Optional<Product>>> createOrUpdateProducts() {
        return productService.createProducts(draftsToCreate)
                             .thenAccept(createdProducts ->
                                 updateStatistics(createdProducts, draftsToCreate.size()))
                             .thenCompose(ignoredResult -> syncProducts(productsToSync));
    }

    private void updateStatistics(@Nonnull final Set<Product> createdProducts,
                                  final int totalNumberOfDraftsToCreate) {
        final int numberOfFailedCreations = totalNumberOfDraftsToCreate - createdProducts.size();
        statistics.incrementFailed(numberOfFailedCreations);
        statistics.incrementCreated(createdProducts.size());
    }

    @Nonnull
    private CompletionStage<List<Optional<Product>>> syncProducts(
        @Nonnull final Map<ProductDraft, Product> productsToSync) {
        return mapValuesToFutureOfCompletedValues(productsToSync.entrySet(),
            entry -> fetchProductAttributesMetadataAndUpdate(entry.getValue(), entry.getKey()), toList());
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
                            handleError(errorMessage);
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
    private CompletionStage<Optional<Product>> fetchAndUpdate(@Nonnull final Product oldProduct,
                                                              @Nonnull final ProductDraft newProduct) {
        final String key = oldProduct.getKey();
        return productService.fetchProduct(key)
                .thenCompose(productOptional -> productOptional
                        .map(fetchedProduct -> fetchProductAttributesMetadataAndUpdate(fetchedProduct, newProduct))
                        .orElseGet(() -> {
                            handleError(format(UPDATE_FAILED, key, UNEXPECTED_DELETE));
                            return CompletableFuture.completedFuture(productOptional);
                        })
                );
    }

    /**
     * Given a {@link String} {@code errorMessage}, this method calls the optional error callback specified in the
     * {@code syncOptions} and updates the {@code statistics} instance by incrementing the total number of failed
     * products to sync.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     */
    private void handleError(@Nonnull final String errorMessage) {
        handleError(errorMessage, null);
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
