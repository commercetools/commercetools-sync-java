package com.commercetools.sync.products;

import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.models.WaitingToBeResolved;
import com.commercetools.sync.products.helpers.ProductBatchProcessor;
import com.commercetools.sync.products.helpers.ProductReferenceResolver;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.LazyResolutionService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import com.commercetools.sync.services.impl.ChannelServiceImpl;
import com.commercetools.sync.services.impl.CustomerGroupServiceImpl;
import com.commercetools.sync.services.impl.LazyResolutionServiceImpl;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static com.commercetools.sync.products.utils.ProductSyncUtils.buildActions;
import static io.sphere.sdk.states.StateType.PRODUCT_STATE;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class ProductSync extends BaseSync<ProductDraft, ProductSyncStatistics, ProductSyncOptions> {
    private static final String CTP_PRODUCT_FETCH_FAILED = "Failed to fetch existing products with keys:"
        + " '%s'.";
    private static final String UPDATE_FAILED = "Failed to update Product with key: '%s'. Reason: %s";
    private static final String FAILED_TO_RESOLVE_REFERENCES = "Failed to resolve references on "
        + "ProductDraft with key:'%s'. Reason: %s";
    private static final String FAILED_TO_FETCH_PRODUCT_TYPE = "Failed to fetch a productType for the product to "
        + "build the products' attributes metadata.";

    private final ProductService productService;
    private final ProductTypeService productTypeService;
    private final ProductReferenceResolver productReferenceResolver;
    private final LazyResolutionService lazyResolutionService;

    private ConcurrentHashMap.KeySetView<String, Boolean> readyToResolve;

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
                new CustomerGroupServiceImpl(productSyncOptions),
                new TaxCategoryServiceImpl(productSyncOptions),
                new StateServiceImpl(productSyncOptions, PRODUCT_STATE),
                new LazyResolutionServiceImpl(productSyncOptions));
    }

    ProductSync(@Nonnull final ProductSyncOptions productSyncOptions, @Nonnull final ProductService productService,
                @Nonnull final ProductTypeService productTypeService, @Nonnull final CategoryService categoryService,
                @Nonnull final TypeService typeService, @Nonnull final ChannelService channelService,
                @Nonnull final CustomerGroupService customerGroupService,
                @Nonnull final TaxCategoryService taxCategoryService, @Nonnull final StateService stateService,
                @Nonnull final LazyResolutionService lazyResolutionService) {
        super(new ProductSyncStatistics(), productSyncOptions);
        this.productService = productService;
        this.productTypeService = productTypeService;
        this.productReferenceResolver = new ProductReferenceResolver(productSyncOptions, productTypeService,
            categoryService, typeService, channelService, customerGroupService, taxCategoryService, stateService,
            productService);
        this.lazyResolutionService = lazyResolutionService;
    }

    @Override
    protected CompletionStage<ProductSyncStatistics> process(@Nonnull final List<ProductDraft> resourceDrafts) {
        final List<List<ProductDraft>> batches = batchElements(resourceDrafts, syncOptions.getBatchSize());
        return syncBatches(batches, CompletableFuture.completedFuture(statistics));
    }

    @Override
    protected CompletionStage<ProductSyncStatistics> processBatch(@Nonnull final List<ProductDraft> batch) {

        readyToResolve = ConcurrentHashMap.newKeySet();
        final ProductBatchProcessor batchProcessor = new ProductBatchProcessor(batch, this);
        batchProcessor.validateBatch();

        final Set<String> keysToCache = batchProcessor.getKeysToCache();
        return productService
            .cacheKeysToIds(keysToCache)
            .handle(ImmutablePair::new)
            .thenCompose(cachingResponse -> {

                final Map<String, String> keyToIdCache = cachingResponse.getKey();
                final Throwable cachingException = cachingResponse.getValue();

                if (cachingException != null) {
                    handleError("Failed to build a cache of keys to ids.", cachingException, keysToCache.size());
                    return CompletableFuture.completedFuture(null);
                } else {
                    return syncBatch(batchProcessor.getValidDrafts(), keyToIdCache);
                }
            })
            .thenApply(ignoredResult -> {
                statistics.incrementProcessed(batch.size());
                return statistics;
            });
    }

    @Nonnull
    private CompletionStage<Void> syncBatch(
        @Nonnull final Set<ProductDraft> productDrafts,
        @Nonnull final Map<String, String> keyToIdCache) {

        if (productDrafts.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        final Set<String> productDraftKeys = productDrafts
            .stream()
            .map(ProductDraft::getKey)
            .collect(Collectors.toSet());

        return productService
            .fetchMatchingProductsByKeys(productDraftKeys)
            .handle(ImmutablePair::new)
            .thenCompose(fetchResponse -> {
                final Set<Product> matchingProducts = fetchResponse.getKey();
                final Throwable fetchException = fetchResponse.getValue();

                if (fetchException != null) {
                    final String errorMessage = format(CTP_PRODUCT_FETCH_FAILED, productDraftKeys);
                    handleError(errorMessage, fetchException, productDraftKeys.size());
                    return CompletableFuture.completedFuture(null);
                } else {
                    return syncBatch(productDrafts, matchingProducts, keyToIdCache)
                        .thenCompose(aVoid -> lazilyResolveReferences(keyToIdCache));
                }
            });
    }

    @Nonnull
    private CompletionStage<Void> lazilyResolveReferences(final Map<String, String> keyToIdCache) {
        // ugly method

        final Set<String> referencingDraftKeys = readyToResolve
            .stream()
            .map(statistics::removeAndGetReferencingKeys) // because one could argue that doesn't matter failed or succeeeded, we should not keep track..
            .filter(Objects::nonNull)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

        if (referencingDraftKeys.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }


        final Set<ProductDraft> readyToSync = new HashSet<>();
        final Set<WaitingToBeResolved> waitingDraftsToBeUpdated = new HashSet<>();

        return lazyResolutionService
            .fetch(referencingDraftKeys)
            .thenCompose(waitingDrafts -> {

                waitingDrafts
                    .forEach(waitingDraft -> {
                        final Set<String> missingReferencedProductKeys = waitingDraft.getMissingReferencedProductKeys();

                        final boolean setChanged = missingReferencedProductKeys.removeAll(readyToResolve);

                        if (missingReferencedProductKeys.isEmpty()) {
                            readyToSync.add(waitingDraft.getProductDraft());
                        } else {
                            if (setChanged) {
                                waitingDraftsToBeUpdated.add(waitingDraft);
                            }
                        }
                    });


                return updateWaitingDrafts(waitingDraftsToBeUpdated)
                    .thenCompose(aVoid -> syncBatch(readyToSync, keyToIdCache))
                    .thenCompose(aVoid -> removeFromWaiting(readyToSync));
            });
    }

    @Nonnull
    private CompletableFuture<Void> updateWaitingDrafts(
        @Nonnull final Set<WaitingToBeResolved> waitingDraftsToBeUpdated) {

        return allOf(waitingDraftsToBeUpdated
            .stream()
            .map(lazyResolutionService::save)
            .toArray(CompletableFuture[]::new));
    }

    @Nonnull
    private CompletableFuture<Void> removeFromWaiting(
        @Nonnull final Set<ProductDraft> drafts) {
        //todo: not a very good idea to delete in the end, rather after every success.
        return allOf(drafts
            .stream()
            .map(ProductDraft::getKey)
            .map(lazyResolutionService::delete)
            .toArray(CompletableFuture[]::new));
    }

    /**
     * Given a set of product type drafts, attempts to sync the drafts with the existing products types in the target
     * CTP project. The product type and the draft are considered to match if they have the same key.
     *
     *
     * <p>Note: In order to support syncing product types with nested references in any order, this method will
     * remove any attribute which contains a nested reference on the drafts and keep track of it to be resolved as
     * soon as the referenced product type becomes available.
     *
     * @param oldProducts old product types.
     * @param newProducts drafts that need to be synced.
     * @return a {@link CompletionStage} which contains an empty result after execution of the update
     */
    @Nonnull
    private CompletionStage<Void> syncBatch(
        @Nonnull final Set<ProductDraft> newProducts,
        @Nonnull final Set<Product> oldProducts,
        @Nonnull final Map<String, String> keyToIdCache) {

        final Map<String, Product> oldProductMap =
            oldProducts.stream().collect(toMap(Product::getKey, identity()));

        return allOf(newProducts
            .stream()
            .map(newDraft -> {
                final Set<String> missingReferencedProductKeys =
                    getMissingReferencedProductKeys(newDraft, keyToIdCache);

                if (!missingReferencedProductKeys.isEmpty()) {
                    return keepTrackOfMissingReferences(newDraft, missingReferencedProductKeys);
                } else {

                    return productReferenceResolver
                        .resolveReferences(newDraft)
                        .thenCompose(resolvedDraft -> syncDraft(oldProductMap, resolvedDraft))
                        .exceptionally(completionException -> {
                            final ReferenceResolutionException referenceResolutionException =
                                (ReferenceResolutionException) completionException.getCause();
                            final String errorMessage = format(FAILED_TO_RESOLVE_REFERENCES, newDraft.getKey(),
                                referenceResolutionException.getMessage());
                            handleError(errorMessage, referenceResolutionException, 1);
                            return null;
                        });
                }}
                )
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
    }

    private Set<String> getMissingReferencedProductKeys(
        @Nonnull final ProductDraft newProduct,
        @Nonnull final Map<String, String> keyToIdCache) {

        final Set<String> referencedProductKeys = newProduct.getVariants()
                                                            .stream()
                                                            .map(ProductBatchProcessor::getReferencedProductKeys)
                                                            .flatMap(Collection::stream)
                                                            .collect(Collectors.toSet());
        // add also master variant keys
        ofNullable(newProduct.getMasterVariant())
            .map(ProductBatchProcessor::getReferencedProductKeys)
            .ifPresent(referencedProductKeys::addAll);

        return referencedProductKeys
            .stream()
            .filter(key -> !keyToIdCache.containsKey(key))
            .collect(Collectors.toSet());
    }

    private CompletionStage<Void> keepTrackOfMissingReferences(
        @Nonnull final ProductDraft newProduct,
        @Nonnull final Set<String> referencedProductKeys) {

        referencedProductKeys.forEach(parentKey -> statistics.putMissingParentProductChildKey(parentKey,
            newProduct.getKey()));

        final WaitingToBeResolved waitingToBeResolved =
            new WaitingToBeResolved(newProduct, referencedProductKeys);

        return lazyResolutionService.save(waitingToBeResolved)
                                    .thenAccept(o -> { } );
    }

    @Nonnull
    private CompletionStage<Void> syncDraft(
        @Nonnull final Map<String, Product> oldProductMap,
        @Nonnull final ProductDraft newProductDraft) {

        final Product oldProduct = oldProductMap.get(newProductDraft.getKey());

        return ofNullable(oldProduct)
            .map(product -> fetchProductAttributesMetadataAndUpdate(oldProduct, newProductDraft))
            .orElseGet(() -> applyCallbackAndCreate(newProductDraft));
    }

    @Nonnull
    private CompletionStage<Void> applyCallbackAndCreate(@Nonnull final ProductDraft productDraft) {
        return syncOptions
            .applyBeforeCreateCallBack(productDraft)
            .map(draft -> productService
                .createProduct(draft)
                .thenAccept(productOptional -> {
                    if (productOptional.isPresent()) {
                        readyToResolve.add(productDraft.getKey());
                        statistics.incrementCreated();
                    } else {
                        statistics.incrementFailed();
                    }
                })

            )
            .orElse(CompletableFuture.completedFuture(null));
    }

    @Nonnull
    private CompletionStage<Void> fetchProductAttributesMetadataAndUpdate(
        @Nonnull final Product oldProduct,
        @Nonnull final ProductDraft newProduct) {

        return productTypeService
            .fetchCachedProductAttributeMetaDataMap(oldProduct.getProductType().getId())
            .thenCompose(optionalAttributesMetaDataMap ->
                optionalAttributesMetaDataMap
                    .map(attributeMetaDataMap -> {
                        final List<UpdateAction<Product>> updateActions =
                            buildActions(oldProduct, newProduct, syncOptions, attributeMetaDataMap);

                        final List<UpdateAction<Product>> beforeUpdateCallBackApplied =
                            syncOptions.applyBeforeUpdateCallBack(updateActions, newProduct, oldProduct);

                        if (!beforeUpdateCallBackApplied.isEmpty()) {
                            return updateProduct(oldProduct, newProduct, beforeUpdateCallBackApplied);
                        }

                        return CompletableFuture.completedFuture((Void) null);

                    }).orElseGet(() -> {
                        final String errorMessage =
                            format(UPDATE_FAILED, oldProduct.getKey(), FAILED_TO_FETCH_PRODUCT_TYPE);
                        handleError(errorMessage);
                        return CompletableFuture.completedFuture(null);
                    })
            );
    }

    @Nonnull
    private CompletionStage<Void> updateProduct(
        @Nonnull final Product oldProduct,
        @Nonnull final ProductDraft newProduct,
        @Nonnull final List<UpdateAction<Product>> updateActions) {

        return productService
            .updateProduct(oldProduct, updateActions)
            .handle(ImmutablePair::new)
            .thenCompose(updateResponse -> {
                final Throwable sphereException = updateResponse.getValue();
                if (sphereException != null) {
                    return executeSupplierIfConcurrentModificationException(sphereException,
                        () -> fetchAndUpdate(oldProduct, newProduct),
                        () -> {
                        final String productKey = oldProduct.getKey();
                        handleError(format(UPDATE_FAILED, productKey, sphereException), sphereException);
                        return CompletableFuture.completedFuture(null);
                    });
                } else {
                    statistics.incrementUpdated();
                    return CompletableFuture.completedFuture(null);
                }
            });
    }



    /**
     * Given an existing {@link Product} and a new {@link ProductDraft}, first fetches a fresh copy of the existing
     * product, then it calculates all the update actions required to synchronize the existing product to be the
     * same as the new one. If there are update actions found, a request is made to CTP to update the
     * existing one, otherwise it doesn't issue a request.
     *
     * @param oldProduct the product which could be updated.
     * @param newProduct the product draft where we get the new data.
     * @return a future which contains an empty result after execution of the update.
     */
    @Nonnull
    private CompletionStage<Void> fetchAndUpdate(@Nonnull final Product oldProduct,
                                                              @Nonnull final ProductDraft newProduct) {

        final String key = oldProduct.getKey();
        return productService
            .fetchProduct(key)
            .handle(ImmutablePair::new)
            .thenCompose(fetchResponse -> {
                final Optional<Product> fetchedProductOptional = fetchResponse.getKey();
                final Throwable exception = fetchResponse.getValue();

                if (exception != null) {
                    final String errorMessage = format(UPDATE_FAILED, key, "Failed to fetch from CTP while "
                            + "retrying after concurrency modification.");
                    handleError(errorMessage, exception);
                    return CompletableFuture.completedFuture(null);
                }

                return fetchedProductOptional
                    .map(fetchedProduct -> fetchProductAttributesMetadataAndUpdate(fetchedProduct, newProduct))
                    .orElseGet(() -> {
                        final String errorMessage = format(UPDATE_FAILED, key, "Not found when attempting to fetch "
                                + "while retrying after concurrency modification.");
                        handleError(errorMessage);
                        return CompletableFuture.completedFuture(null);
                    });
            });
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

    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed product to sync with the supplied {@code failedTimes}.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     * @param exception    The exception that called caused the failure, if any.
     * @param failedTimes  The number of times that the failed products counter is incremented.
     */
    private void handleError(@Nonnull final String errorMessage,
                             @Nullable final Throwable exception,
                             final int failedTimes) {

        syncOptions.applyErrorCallback(errorMessage, exception);
        statistics.incrementFailed(failedTimes);
    }
}
