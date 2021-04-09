package com.commercetools.sync.products;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static com.commercetools.sync.products.utils.ProductSyncUtils.buildActions;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.getAllVariants;
import static com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.models.WaitingToBeResolvedProducts;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.customobjects.CustomObjectSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductBatchValidator;
import com.commercetools.sync.products.helpers.ProductReferenceResolver;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.UnresolvedReferencesService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import com.commercetools.sync.services.impl.ChannelServiceImpl;
import com.commercetools.sync.services.impl.CustomObjectServiceImpl;
import com.commercetools.sync.services.impl.CustomerGroupServiceImpl;
import com.commercetools.sync.services.impl.CustomerServiceImpl;
import com.commercetools.sync.services.impl.ProductServiceImpl;
import com.commercetools.sync.services.impl.ProductTypeServiceImpl;
import com.commercetools.sync.services.impl.StateServiceImpl;
import com.commercetools.sync.services.impl.TaxCategoryServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptionsBuilder;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import java.util.Collection;
import java.util.Collections;
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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class ProductSync extends BaseSync<ProductDraft, ProductSyncStatistics, ProductSyncOptions> {
  private static final String CTP_PRODUCT_FETCH_FAILED =
      "Failed to fetch existing products with keys: '%s'.";
  private static final String UNRESOLVED_REFERENCES_STORE_FETCH_FAILED =
      "Failed to fetch ProductDrafts waiting to be resolved with keys '%s'.";
  private static final String UPDATE_FAILED = "Failed to update Product with key: '%s'. Reason: %s";
  private static final String FAILED_TO_PROCESS =
      "Failed to process the ProductDraft with key:'%s'. Reason: %s";
  private static final String FAILED_TO_FETCH_PRODUCT_TYPE =
      "Failed to fetch a productType for the product to "
          + "build the products' attributes metadata.";

  private final ProductService productService;
  private final ProductTypeService productTypeService;
  private final ProductReferenceResolver productReferenceResolver;
  private final UnresolvedReferencesService<WaitingToBeResolvedProducts>
      unresolvedReferencesService;
  private final ProductBatchValidator batchValidator;

  private ConcurrentHashMap.KeySetView<String, Boolean> readyToResolve;

  /**
   * Takes a {@link ProductSyncOptions} instance to instantiate a new {@link ProductSync} instance
   * that could be used to sync ProductProjection drafts with the given products in the CTP project
   * specified in the injected {@link ProductSyncOptions} instance.
   *
   * @param productSyncOptions the container of all the options of the sync process including the
   *     CTP project client and/or configuration and other sync-specific options.
   */
  public ProductSync(@Nonnull final ProductSyncOptions productSyncOptions) {
    this(
        productSyncOptions,
        new ProductServiceImpl(productSyncOptions),
        new ProductTypeServiceImpl(productSyncOptions),
        new CategoryServiceImpl(
            CategorySyncOptionsBuilder.of(productSyncOptions.getCtpClient()).build()),
        new TypeServiceImpl(productSyncOptions),
        new ChannelServiceImpl(
            productSyncOptions, Collections.singleton(ChannelRole.PRODUCT_DISTRIBUTION)),
        new CustomerGroupServiceImpl(productSyncOptions),
        new TaxCategoryServiceImpl(
            TaxCategorySyncOptionsBuilder.of(productSyncOptions.getCtpClient()).build()),
        new StateServiceImpl(StateSyncOptionsBuilder.of(productSyncOptions.getCtpClient()).build()),
        new UnresolvedReferencesServiceImpl<>(productSyncOptions),
        new CustomObjectServiceImpl(
            CustomObjectSyncOptionsBuilder.of(productSyncOptions.getCtpClient()).build()),
        new CustomerServiceImpl(
            CustomerSyncOptionsBuilder.of(productSyncOptions.getCtpClient()).build()));
  }

  ProductSync(
      @Nonnull final ProductSyncOptions productSyncOptions,
      @Nonnull final ProductService productService,
      @Nonnull final ProductTypeService productTypeService,
      @Nonnull final CategoryService categoryService,
      @Nonnull final TypeService typeService,
      @Nonnull final ChannelService channelService,
      @Nonnull final CustomerGroupService customerGroupService,
      @Nonnull final TaxCategoryService taxCategoryService,
      @Nonnull final StateService stateService,
      @Nonnull
          final UnresolvedReferencesService<WaitingToBeResolvedProducts>
              unresolvedReferencesService,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final CustomerService customerService) {
    super(new ProductSyncStatistics(), productSyncOptions);
    this.productService = productService;
    this.productTypeService = productTypeService;
    this.productReferenceResolver =
        new ProductReferenceResolver(
            getSyncOptions(),
            productTypeService,
            categoryService,
            typeService,
            channelService,
            customerGroupService,
            taxCategoryService,
            stateService,
            productService,
            customObjectService,
            customerService);
    this.unresolvedReferencesService = unresolvedReferencesService;
    this.batchValidator = new ProductBatchValidator(getSyncOptions(), getStatistics());
  }

  @Override
  protected CompletionStage<ProductSyncStatistics> process(
      @Nonnull final List<ProductDraft> resourceDrafts) {
    final List<List<ProductDraft>> batches =
        batchElements(resourceDrafts, syncOptions.getBatchSize());
    return syncBatches(batches, CompletableFuture.completedFuture(statistics));
  }

  @Override
  protected CompletionStage<ProductSyncStatistics> processBatch(
      @Nonnull final List<ProductDraft> batch) {

    readyToResolve = ConcurrentHashMap.newKeySet();

    final ImmutablePair<Set<ProductDraft>, ProductBatchValidator.ReferencedKeys> result =
        batchValidator.validateAndCollectReferencedKeys(batch);

    final Set<ProductDraft> validDrafts = result.getLeft();
    if (validDrafts.isEmpty()) {
      statistics.incrementProcessed(batch.size());
      return CompletableFuture.completedFuture(statistics);
    }

    return productReferenceResolver
        .populateKeyToIdCachesForReferencedKeys(result.getRight())
        .handle(ImmutablePair::new)
        .thenCompose(
            cachingResponse -> {
              final Throwable cachingException = cachingResponse.getValue();
              if (cachingException != null) {
                handleError(
                    new SyncException("Failed to build a cache of keys to ids.", cachingException),
                    validDrafts.size());
                return CompletableFuture.completedFuture(null);
              }

              final Map<String, String> productKeyToIdCache = cachingResponse.getKey();
              return syncBatch(validDrafts, productKeyToIdCache);
            })
        .thenApply(
            ignoredResult -> {
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

    final Set<String> productDraftKeys =
        productDrafts.stream().map(ProductDraft::getKey).collect(Collectors.toSet());

    return productService
        .fetchMatchingProductsByKeys(productDraftKeys)
        .handle(ImmutablePair::new)
        .thenCompose(
            fetchResponse -> {
              final Throwable fetchException = fetchResponse.getValue();
              if (fetchException != null) {
                final String errorMessage = format(CTP_PRODUCT_FETCH_FAILED, productDraftKeys);
                handleError(
                    new SyncException(errorMessage, fetchException), productDraftKeys.size());
                return CompletableFuture.completedFuture(null);
              } else {
                final Set<ProductProjection> matchingProducts = fetchResponse.getKey();
                return syncOrKeepTrack(productDrafts, matchingProducts, keyToIdCache)
                    .thenCompose(aVoid -> resolveNowReadyReferences(keyToIdCache));
              }
            });
  }

  /**
   * Given a set of ProductProjection drafts, for each new draft: if it doesn't have any
   * ProductProjection references which are missing, it syncs the new draft. However, if it does
   * have missing references, it keeps track of it by persisting it.
   *
   * @param oldProducts old ProductProjection types.
   * @param newProducts drafts that need to be synced.
   * @return a {@link CompletionStage} which contains an empty result after execution of the update
   */
  @Nonnull
  private CompletionStage<Void> syncOrKeepTrack(
      @Nonnull final Set<ProductDraft> newProducts,
      @Nonnull final Set<ProductProjection> oldProducts,
      @Nonnull final Map<String, String> keyToIdCache) {

    return allOf(
        newProducts.stream()
            .map(
                newDraft -> {
                  final Set<String> missingReferencedProductKeys =
                      getMissingReferencedProductKeys(newDraft, keyToIdCache);

                  if (!missingReferencedProductKeys.isEmpty()) {
                    return keepTrackOfMissingReferences(newDraft, missingReferencedProductKeys);
                  } else {
                    return syncDraft(oldProducts, newDraft);
                  }
                })
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
  }

  private Set<String> getMissingReferencedProductKeys(
      @Nonnull final ProductDraft newProduct, @Nonnull final Map<String, String> keyToIdCache) {

    final Set<String> referencedProductKeys =
        getAllVariants(newProduct).stream()
            .map(ProductBatchValidator::getReferencedProductKeys)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    return referencedProductKeys.stream()
        .filter(key -> !keyToIdCache.containsKey(key))
        .collect(Collectors.toSet());
  }

  private CompletionStage<Optional<WaitingToBeResolvedProducts>> keepTrackOfMissingReferences(
      @Nonnull final ProductDraft newProduct,
      @Nonnull final Set<String> missingReferencedProductKeys) {

    missingReferencedProductKeys.forEach(
        missingParentKey -> statistics.addMissingDependency(missingParentKey, newProduct.getKey()));
    return unresolvedReferencesService.save(
        new WaitingToBeResolvedProducts(newProduct, missingReferencedProductKeys),
        CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
        WaitingToBeResolvedProducts.class);
  }

  @Nonnull
  private CompletionStage<Void> resolveNowReadyReferences(final Map<String, String> keyToIdCache) {

    // We delete anyways the keys from the statistics before we attempt resolution, because even if
    // resolution fails
    // the products that failed to be synced would be counted as failed.

    final Set<String> referencingDraftKeys =
        readyToResolve.stream()
            .map(statistics::removeAndGetReferencingKeys)
            .filter(Objects::nonNull)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

    if (referencingDraftKeys.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    final Set<ProductDraft> readyToSync = new HashSet<>();
    final Set<WaitingToBeResolvedProducts> waitingDraftsToBeUpdated = new HashSet<>();

    return unresolvedReferencesService
        .fetch(
            referencingDraftKeys,
            CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
            WaitingToBeResolvedProducts.class)
        .handle(ImmutablePair::new)
        .thenCompose(
            fetchResponse -> {
              final Set<WaitingToBeResolvedProducts> waitingDrafts = fetchResponse.getKey();
              final Throwable fetchException = fetchResponse.getValue();

              if (fetchException != null) {
                final String errorMessage =
                    format(UNRESOLVED_REFERENCES_STORE_FETCH_FAILED, referencingDraftKeys);
                handleError(
                    new SyncException(errorMessage, fetchException), referencingDraftKeys.size());
                return CompletableFuture.completedFuture(null);
              }

              waitingDrafts.forEach(
                  waitingDraft -> {
                    final Set<String> missingReferencedProductKeys =
                        waitingDraft.getMissingReferencedProductKeys();
                    missingReferencedProductKeys.removeAll(readyToResolve);

                    if (missingReferencedProductKeys.isEmpty()) {
                      readyToSync.add(waitingDraft.getProductDraft());
                    } else {
                      waitingDraftsToBeUpdated.add(waitingDraft);
                    }
                  });

              return updateWaitingDrafts(waitingDraftsToBeUpdated)
                  .thenCompose(aVoid -> syncBatch(readyToSync, keyToIdCache))
                  .thenCompose(aVoid -> removeFromWaiting(readyToSync));
            });
  }

  @Nonnull
  private CompletableFuture<Void> updateWaitingDrafts(
      @Nonnull final Set<WaitingToBeResolvedProducts> waitingDraftsToBeUpdated) {

    return allOf(
        waitingDraftsToBeUpdated.stream()
            .map(
                draft ->
                    unresolvedReferencesService.save(
                        draft,
                        CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                        WaitingToBeResolvedProducts.class))
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
  }

  @Nonnull
  private CompletableFuture<Void> removeFromWaiting(@Nonnull final Set<ProductDraft> drafts) {
    return allOf(
        drafts.stream()
            .map(ProductDraft::getKey)
            .map(
                key ->
                    unresolvedReferencesService.delete(
                        key,
                        CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                        WaitingToBeResolvedProducts.class))
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
  }

  @Nonnull
  private CompletionStage<Void> syncDraft(
      @Nonnull final Set<ProductProjection> oldProducts,
      @Nonnull final ProductDraft newProductDraft) {

    final Map<String, ProductProjection> oldProductMap =
        oldProducts.stream().collect(toMap(ProductProjection::getKey, identity()));

    return productReferenceResolver
        .resolveReferences(newProductDraft)
        .thenCompose(
            resolvedDraft -> {
              final ProductProjection oldProduct = oldProductMap.get(newProductDraft.getKey());

              return ofNullable(oldProduct)
                  .map(
                      ProductProjection ->
                          fetchProductAttributesMetadataAndUpdate(oldProduct, resolvedDraft))
                  .orElseGet(() -> applyCallbackAndCreate(resolvedDraft));
            })
        .exceptionally(
            completionException -> {
              final String errorMessage =
                  format(
                      FAILED_TO_PROCESS,
                      newProductDraft.getKey(),
                      completionException.getMessage());
              handleError(new SyncException(errorMessage, completionException), 1);
              return null;
            });
  }

  @Nonnull
  private CompletionStage<Void> fetchProductAttributesMetadataAndUpdate(
      @Nonnull final ProductProjection oldProduct, @Nonnull final ProductDraft newProduct) {

    return productTypeService
        .fetchCachedProductAttributeMetaDataMap(oldProduct.getProductType().getId())
        .thenCompose(
            optionalAttributesMetaDataMap ->
                optionalAttributesMetaDataMap
                    .map(
                        attributeMetaDataMap -> {
                          final List<UpdateAction<Product>> updateActions =
                              buildActions(
                                  oldProduct, newProduct, syncOptions, attributeMetaDataMap);

                          final List<UpdateAction<Product>> beforeUpdateCallBackApplied =
                              syncOptions.applyBeforeUpdateCallback(
                                  updateActions, newProduct, oldProduct);

                          if (!beforeUpdateCallBackApplied.isEmpty()) {
                            return updateProduct(
                                oldProduct, newProduct, beforeUpdateCallBackApplied);
                          }

                          return CompletableFuture.completedFuture((Void) null);
                        })
                    .orElseGet(
                        () -> {
                          final String errorMessage =
                              format(
                                  UPDATE_FAILED, oldProduct.getKey(), FAILED_TO_FETCH_PRODUCT_TYPE);
                          handleError(errorMessage, oldProduct, newProduct);
                          return CompletableFuture.completedFuture(null);
                        }));
  }

  @Nonnull
  private CompletionStage<Void> updateProduct(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final ProductDraft newProduct,
      @Nonnull final List<UpdateAction<Product>> updateActions) {

    return productService
        .updateProduct(oldProduct, updateActions)
        .handle(ImmutablePair::new)
        .thenCompose(
            updateResponse -> {
              final Throwable sphereException = updateResponse.getValue();
              if (sphereException != null) {
                return executeSupplierIfConcurrentModificationException(
                    sphereException,
                    () -> fetchAndUpdate(oldProduct, newProduct),
                    () -> {
                      final String productKey = oldProduct.getKey();
                      handleError(
                          format(UPDATE_FAILED, productKey, sphereException),
                          sphereException,
                          oldProduct,
                          newProduct,
                          updateActions);
                      return CompletableFuture.completedFuture(null);
                    });
              } else {
                statistics.incrementUpdated();
                return CompletableFuture.completedFuture(null);
              }
            });
  }

  /**
   * Given an existing {@link Product} and a new {@link ProductDraft}, first fetches a fresh copy of
   * the existing product, then it calculates all the update actions required to synchronize the
   * existing product to be the same as the new one. If there are update actions found, a request is
   * made to CTP to update the existing one, otherwise it doesn't issue a request.
   *
   * @param oldProduct the product which could be updated.
   * @param newProduct the ProductProjection draft where we get the new data.
   * @return a future which contains an empty result after execution of the update.
   */
  @Nonnull
  private CompletionStage<Void> fetchAndUpdate(
      @Nonnull final ProductProjection oldProduct, @Nonnull final ProductDraft newProduct) {

    final String key = oldProduct.getKey();
    return productService
        .fetchProduct(key)
        .handle(ImmutablePair::new)
        .thenCompose(
            fetchResponse -> {
              final Optional<ProductProjection> fetchedProductOptional = fetchResponse.getKey();
              final Throwable exception = fetchResponse.getValue();

              if (exception != null) {
                final String errorMessage =
                    format(
                        UPDATE_FAILED,
                        key,
                        "Failed to fetch from CTP while "
                            + "retrying after concurrency modification.");
                handleError(errorMessage, exception, oldProduct, newProduct, null);
                return CompletableFuture.completedFuture(null);
              }

              return fetchedProductOptional
                  .map(
                      fetchedProduct ->
                          fetchProductAttributesMetadataAndUpdate(fetchedProduct, newProduct))
                  .orElseGet(
                      () -> {
                        final String errorMessage =
                            format(
                                UPDATE_FAILED,
                                key,
                                "Not found when attempting to fetch "
                                    + "while retrying after concurrency modification.");
                        handleError(errorMessage, oldProduct, newProduct);
                        return CompletableFuture.completedFuture(null);
                      });
            });
  }

  @Nonnull
  private CompletionStage<Void> applyCallbackAndCreate(@Nonnull final ProductDraft productDraft) {
    return syncOptions
        .applyBeforeCreateCallback(productDraft)
        .map(
            draft ->
                productService
                    .createProduct(draft)
                    .thenAccept(
                        productOptional -> {
                          if (productOptional.isPresent()) {
                            readyToResolve.add(productDraft.getKey());
                            statistics.incrementCreated();
                          } else {
                            statistics.incrementFailed();
                          }
                        }))
        .orElse(CompletableFuture.completedFuture(null));
  }

  /**
   * Given a {@link String} {@code errorMessage}, this method calls the optional error callback
   * specified in the {@code syncOptions} and updates the {@code statistics} instance by
   * incrementing the total number of failed products to sync.
   *
   * @param errorMessage The error message describing the reason(s) of failure.
   */
  private void handleError(
      @Nonnull final String errorMessage,
      @Nullable final ProductProjection oldResource,
      @Nullable final ProductDraft newResource) {
    handleError(errorMessage, null, oldResource, newResource, null);
  }

  /**
   * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this
   * method calls the optional error callback specified in the {@code syncOptions} and updates the
   * {@code statistics} instance by incrementing the total number of failed products to sync.
   *
   * @param errorMessage The error message describing the reason(s) of failure.
   * @param exception The exception that called caused the failure, if any.
   * @param oldProduct the ProductProjection which could be updated.
   * @param newProduct the ProductProjection draft where we get the new data.
   * @param updateActions the update actions to update the {@link Product} with.
   */
  private void handleError(
      @Nonnull final String errorMessage,
      @Nullable final Throwable exception,
      @Nullable final ProductProjection oldProduct,
      @Nullable final ProductDraft newProduct,
      @Nullable final List<UpdateAction<Product>> updateActions) {
    SyncException syncException =
        exception != null
            ? new SyncException(errorMessage, exception)
            : new SyncException(errorMessage);
    syncOptions.applyErrorCallback(syncException, oldProduct, newProduct, updateActions);
    statistics.incrementFailed();
  }

  /**
   * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this
   * method calls the optional error callback specified in the {@code syncOptions} and updates the
   * {@code statistics} instance by incrementing the total number of failed ProductProjection to
   * sync with the supplied {@code failedTimes}.
   *
   * @param syncException The exception that caused the failure.
   * @param failedTimes The number of times that the failed products counter is incremented.
   */
  private void handleError(@Nonnull final SyncException syncException, final int failedTimes) {
    syncOptions.applyErrorCallback(syncException);
    statistics.incrementFailed(failedTimes);
  }
}
