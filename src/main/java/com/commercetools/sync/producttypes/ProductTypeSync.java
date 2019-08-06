package com.commercetools.sync.producttypes;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.producttypes.helpers.AttributeDefinitionReferenceResolver;
import com.commercetools.sync.producttypes.helpers.ProductTypeBatchProcessor;
import com.commercetools.sync.producttypes.helpers.ProductTypeReferenceResolver;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.impl.ProductTypeServiceImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeType;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.updateactions.AddAttributeDefinition;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static com.commercetools.sync.producttypes.utils.ProductTypeSyncUtils.buildActions;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * This class syncs product type drafts with the corresponding product types in the CTP project.
 */
public class ProductTypeSync extends BaseSync<ProductTypeDraft, ProductTypeSyncStatistics, ProductTypeSyncOptions> {
    private static final String CTP_PRODUCT_TYPE_FETCH_FAILED = "Failed to fetch existing product types with keys:"
        + " '%s'.";
    private static final String CTP_PRODUCT_TYPE_UPDATE_FAILED = "Failed to update product type with key: '%s'."
        + " Reason: %s";
    private static final String FAILED_TO_RESOLVE_REFERENCES = "Failed to resolve references on "
        + "productTypeDraft with key:'%s'. Reason: %s";

    private final ProductTypeService productTypeService;
    private final ProductTypeReferenceResolver referenceResolver;

    /**
     * The following set ({@code readyToResolve}) is thread-safe because it is accessed/modified in a concurrent
     * context, specifically when creating productTypes in parallel in
     * {@link #applyCallbackAndCreate(ProductTypeDraft)}.  It has a local scope within every batch execution, which
     * means that it is re-initialized on every {@link #processBatch(List)} call.
     */
    private ConcurrentHashMap.KeySetView<String, Boolean> readyToResolve = ConcurrentHashMap.newKeySet();

    public ProductTypeSync(@Nonnull final ProductTypeSyncOptions productTypeSyncOptions) {
        this(productTypeSyncOptions, new ProductTypeServiceImpl(productTypeSyncOptions));
    }

    /**
     * Takes a {@link ProductTypeSyncOptions} and a {@link ProductTypeService} instances to instantiate
     * a new {@link ProductTypeSync} instance that could be used to sync productType drafts in the CTP project specified
     * in the injected {@link ProductTypeSyncOptions} instance.
     *
     * <p>NOTE: This constructor is mainly to be used for tests where the services can be mocked and passed to.
     *
     * @param productTypeSyncOptions the container of all the options of the sync process including the CTP project
     *                               client and/or configuration and other sync-specific options.
     * @param productTypeService     the type service which is responsible for fetching/caching the Types from the CTP
     *                               project.
     */
    ProductTypeSync(@Nonnull final ProductTypeSyncOptions productTypeSyncOptions,
                    @Nonnull final ProductTypeService productTypeService) {

        super(new ProductTypeSyncStatistics(), productTypeSyncOptions);
        this.productTypeService = productTypeService;
        this.referenceResolver = new ProductTypeReferenceResolver(productTypeSyncOptions, productTypeService);
    }

    /**
     * Iterates through the whole {@code productTypeDrafts} list and accumulates its valid drafts to batches.
     * Every batch is then processed by {@link ProductTypeSync#processBatch(List)}.
     *
     * <p><strong>Inherited doc:</strong>
     * {@inheritDoc}
     *
     * @param productTypeDrafts {@link List} of {@link ProductTypeDraft}'s that would be synced into CTP project.
     * @return {@link CompletionStage} with {@link ProductTypeSyncStatistics} holding statistics of all sync
     *         processes performed by this sync instance.
     */
    @Override
    protected CompletionStage<ProductTypeSyncStatistics> process(
            @Nonnull final List<ProductTypeDraft> productTypeDrafts) {

        final List<List<ProductTypeDraft>> batches = batchElements(productTypeDrafts, syncOptions.getBatchSize());
        return syncBatches(batches, CompletableFuture.completedFuture(statistics));
    }

    /**
     * This method first creates a new {@link Set} of valid {@link ProductTypeDraft} elements. For more on the rules of
     * validation, check: {@link ProductTypeBatchProcessor#validateBatch()}. Using the resulting set of
     * {@code validProductTypeDrafts}, the matching productTypes in the target CTP project are fetched then the method
     * {@link ProductTypeSync#syncBatch(Set, Set)} is called to perform the sync (<b>update</b> or <b>create</b>
     * requests accordingly) on the target project.
     *
     * <p> In case of error during of fetching of existing productTypes, the error callback will be triggered.
     * And the sync process would stop for the given batch.
     * </p>
     *
     * @param batch batch of drafts that need to be synced
     * @return a {@link CompletionStage} containing an instance
     *         of {@link ProductTypeSyncStatistics} which contains information about the result of syncing the supplied 
     *         batch to the target project.
     */
    @Override
    protected CompletionStage<ProductTypeSyncStatistics> processBatch(@Nonnull final List<ProductTypeDraft> batch) {

        readyToResolve = ConcurrentHashMap.newKeySet();
        final ProductTypeBatchProcessor batchProcessor = new ProductTypeBatchProcessor(batch, this);
        batchProcessor.validateBatch();

        final Set<String> keysToCache = batchProcessor.getKeysToCache();

        return productTypeService
            .cacheKeysToIds(keysToCache)
            .handle(ImmutablePair::new)
            .thenCompose(cachingResponse -> {

                final Map<String, String> keyToIdCache = cachingResponse.getKey();
                final Throwable cachingException = cachingResponse.getValue();

                if (cachingException != null) {
                    handleError("Failed to build a cache of keys to ids.", cachingException, batch.size());
                    return CompletableFuture.completedFuture(null);
                }


                final Set<String> batchDraftKeys = batchProcessor.getValidDrafts()
                                                                 .stream()
                                                                 .map(ProductTypeDraft::getKey)
                                                                 .collect(Collectors.toSet());

                return productTypeService
                    .fetchMatchingProductTypesByKeys(batchDraftKeys)
                    .handle(ImmutablePair::new)
                    .thenCompose(fetchResponse -> {
                        final Set<ProductType> matchingProductTypes = fetchResponse.getKey();
                        final Throwable exception = fetchResponse.getValue();

                        if (exception != null) {
                            final String errorMessage = format(CTP_PRODUCT_TYPE_FETCH_FAILED, keysToCache);
                            handleError(errorMessage, exception, keysToCache.size());
                            return CompletableFuture.completedFuture(null);
                        } else {
                            return syncBatch(matchingProductTypes, batchProcessor.getValidDrafts(), keyToIdCache)
                                .thenCompose(ignoredResult -> updateToBeUpdatedInParallel(buildToBeUpdatedMap()));
                        }
                    });
            })
            .thenApply(ignored -> {
                statistics.incrementProcessed(batch.size());
                return statistics;
            });

    }

    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed product types to sync.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     * @param exception    The exception that called caused the failure, if any.
     * @param failedTimes  The number of times that the failed product types counter is incremented.
     */
    private void handleError(@Nonnull final String errorMessage, @Nullable final Throwable exception,
                             final int failedTimes) {

        syncOptions.applyErrorCallback(errorMessage, exception);
        statistics.incrementFailed(failedTimes);
    }



    /**
     * Given a set of product type drafts, attempts to sync the drafts with the existing products types in the CTP
     * project. The product type and the draft are considered to match if they have the same key.
     *
     * @param oldProductTypes old product types.
     * @param newProductTypes drafts that need to be synced.
     * @return a {@link CompletionStage} which contains an empty result after execution of the update
     */
    @Nonnull
    private CompletionStage<Void> syncBatch(
        @Nonnull final Set<ProductType> oldProductTypes,
        @Nonnull final Set<ProductTypeDraft> newProductTypes,
        @Nonnull final Map<String, String> keyToIdCache) {

        final Map<String, ProductType> oldProductTypeMap =
            oldProductTypes.stream().collect(toMap(ProductType::getKey, identity()));

        return CompletableFuture.allOf(newProductTypes
            .stream()
            .map(newProductType ->
                removeMissingReferenceAttributeAndUpdateMissingParentMap(newProductType, keyToIdCache))
            .map(draftWithoutMissingRefAttrs ->
                referenceResolver
                    .resolveReferences(draftWithoutMissingRefAttrs)
                    .thenCompose(resolvedDraft -> syncDraft(oldProductTypeMap, resolvedDraft))
                    .exceptionally(completionException -> {
                        final ReferenceResolutionException referenceResolutionException =
                            (ReferenceResolutionException) completionException.getCause();
                        final String errorMessage = format(FAILED_TO_RESOLVE_REFERENCES,
                            draftWithoutMissingRefAttrs.getKey(),
                            referenceResolutionException.getMessage());
                        handleError(errorMessage, referenceResolutionException, 1);
                        return null;
                    })
            )
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
    }

    @Nonnull
    private ProductTypeDraft removeMissingReferenceAttributeAndUpdateMissingParentMap(
        @Nonnull final ProductTypeDraft newProductTypeDraft,
        @Nonnull final Map<String, String> keyToIdCache) {


        // 1. Check referenced keys not in keyToId
        final Map<String, List<AttributeDefinitionDraft>> referencedProductTypeKeys =
            getReferencedProductTypeKeys(newProductTypeDraft);

        // 2. Clean up all occurrences of newProductTypeDraft's key waiting in
        // statistics#putMissingReferencedProductTypeKey. This is because it should not be existing there,
        // and if it is then the values are outdated. This is to support the case, if a new version of the product
        // type is supplied again in a later batch.
        // TODO: TEST THIS CASE!
        statistics.removeProductTypeWaitingToBeResolvedKey(newProductTypeDraft.getKey());

        // TODO: Wrong, it could be many attributes referncing this KEY ----> DONE!
        // TODO: TEST THIS CASE!
        referencedProductTypeKeys
            .keySet()
            .stream()
            .filter(key -> !keyToIdCache.keySet().contains(key))
            .forEach(referencedKeyNotCached -> {

                final List<AttributeDefinitionDraft> attributeDefinitionDraftsWithMissingReferences =
                    referencedProductTypeKeys.get(referencedKeyNotCached);

                // 1.1. Remove attributeDefinition with missing key reference
                // IMP: This mutates in newProductTypeDraft..
                newProductTypeDraft.getAttributes()
                                   .removeAll(attributeDefinitionDraftsWithMissingReferences);

                attributeDefinitionDraftsWithMissingReferences
                    .forEach(attributeDefinitionDraft -> {
                        // 1.2. Add pairs (productTypeDraftKey, attributeDefinition) to missing parent map.

                        // TODO: USE MAP OF MAP INSTEAD OF PAIR! TO BE ABLE TO PUT AND OVERWRITE CHANGES IN LATER BATCHES.
                        // TODO: APPEND CHANGE ORDER ACTION AFTER EVERY KEPT TRACK OF ACTION.
                        statistics.putMissingReferencedProductTypeKey(referencedKeyNotCached,
                            newProductTypeDraft.getKey(), attributeDefinitionDraft);
                    });
            });

        return newProductTypeDraft;
    }

    @Nonnull
    private static Map<String, List<AttributeDefinitionDraft>> getReferencedProductTypeKeys(
        @Nonnull final ProductTypeDraft productTypeDraft) {

        final List<AttributeDefinitionDraft> attributeDefinitionDrafts = productTypeDraft.getAttributes();
        if (attributeDefinitionDrafts == null || attributeDefinitionDrafts.isEmpty()) {
            return emptyMap();
        }

        final Map<String, List<AttributeDefinitionDraft>> referencedProductTypeKeys = new HashMap<>();

        for (AttributeDefinitionDraft attributeDefinitionDraft : attributeDefinitionDrafts) {
            if (attributeDefinitionDraft != null) {
                final AttributeType attributeType = attributeDefinitionDraft.getAttributeType();

                getProductTypeKey(attributeType).ifPresent(key -> {

                    final List<AttributeDefinitionDraft> attributesReferencingCurrentKey = referencedProductTypeKeys
                        .get(key);

                    if (attributesReferencingCurrentKey != null) {
                        attributesReferencingCurrentKey.add(attributeDefinitionDraft);
                    } else {
                        final ArrayList<AttributeDefinitionDraft> newAttributesReferencingCurrentKey
                            = new ArrayList<>();
                        newAttributesReferencingCurrentKey.add(attributeDefinitionDraft);
                        referencedProductTypeKeys.put(key, newAttributesReferencingCurrentKey);
                    }
                });
            }
        }

        return referencedProductTypeKeys;
    }

    @Nonnull
    private static Optional<String> getProductTypeKey(@Nonnull final AttributeType attributeType) {
        if (attributeType instanceof NestedAttributeType) {
            final NestedAttributeType nestedElementType = (NestedAttributeType) attributeType;
            return Optional.of(nestedElementType.getTypeReference().getId());
        } else if (attributeType instanceof SetAttributeType) {
            final SetAttributeType setAttributeType = (SetAttributeType) attributeType;
            return getProductTypeKey(setAttributeType.getElementType());
        }
        return Optional.empty();
    }


    @Nonnull
    private CompletionStage<Void> syncDraft(
        @Nonnull final Map<String, ProductType> oldProductTypeMap,
        @Nonnull final ProductTypeDraft newProductTypeDraft) {

        final ProductType oldProductType = oldProductTypeMap.get(newProductTypeDraft.getKey());

        return ofNullable(oldProductType)
            .map(productType -> buildActionsAndUpdate(oldProductType, newProductTypeDraft))
            .orElseGet(() -> applyCallbackAndCreate(newProductTypeDraft));
    }


    private Map<String, Set<UpdateAction<ProductType>>> buildToBeUpdatedMap() {

        final Map<String, Set<UpdateAction<ProductType>>> toBeUpdatedMap = new HashMap<>();

        readyToResolve
            .forEach(missingProductTypeKey -> {
                final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<UpdateAction<ProductType>, Boolean>>
                    productTypesWaitingForMissingReference = statistics
                    .getProductTypeKeysWithMissingParents()
                    .get(missingProductTypeKey);

                if (productTypesWaitingForMissingReference != null) {
                    productTypesWaitingForMissingReference
                        .forEach((productTypeKey, attributes) ->
                            putInToBeUpdated(toBeUpdatedMap, productTypeKey, attributes));
                }
            });

        return toBeUpdatedMap;
    }

    private static void putInToBeUpdated(
        @Nonnull final Map<String, Set<UpdateAction<ProductType>>> toBeUpdatedMap,
        @Nonnull final String productTypeKey,
        @Nonnull final Set<UpdateAction<ProductType>> actions) {

        final Set<UpdateAction<ProductType>> missingParentChildrenActions = toBeUpdatedMap.get(productTypeKey);

        if (missingParentChildrenActions != null) {
            missingParentChildrenActions.addAll(actions);
        } else {
            toBeUpdatedMap.put(productTypeKey, actions);
        }
    }


    private CompletionStage<Void> updateToBeUpdatedInParallel(
        @Nonnull final Map<String, Set<UpdateAction<ProductType>>> toBeUpdatedMap) {

        final Set<String> keysToFetchToUpdate = toBeUpdatedMap.keySet();
        return productTypeService
            .fetchMatchingProductTypesByKeys(keysToFetchToUpdate)
            .handle(ImmutablePair::new)
            .thenCompose(fetchResponse -> {
                final Set<ProductType> matchingProductTypes = fetchResponse.getKey();
                final Map<String, ProductType> keyToProductType = new HashMap<>();
                matchingProductTypes.forEach(productType -> keyToProductType.put(productType.getKey(), productType));


                final Throwable exception = fetchResponse.getValue();
                if (exception != null) {
                    //not sure yet
                    final String errorMessage = format(CTP_PRODUCT_TYPE_FETCH_FAILED, keysToFetchToUpdate);
                    handleError(errorMessage, exception, keysToFetchToUpdate.size());
                    return CompletableFuture.completedFuture(null);
                } else {
                    return CompletableFuture.allOf(toBeUpdatedMap
                        .entrySet()
                        .stream()
                        .map(entry -> {
                            final String productTypeToUpdateKey = entry.getKey();
                            final Set<UpdateAction<ProductType>> updateActions = entry.getValue();
                            //TODO: Wrong, update actions should be resolved before because they contain keys in the references not ids. --> DONE

                            // First make sure attr is resolved, so as not to have ids. Can't resolve here. Since reference is not in place.
                            final List<UpdateAction<ProductType>> actionsWithResolvedReferences = resolveReferences(
                                new ArrayList<>(updateActions));
                            final ProductType productTypeToUpdate = keyToProductType.get(productTypeToUpdateKey);

                            return updateProductType(productTypeToUpdate, actionsWithResolvedReferences);

                        })
                        .map(CompletionStage::toCompletableFuture)
                        .toArray(CompletableFuture[]::new));
                }
            });
    }

    @Nonnull
    private List<UpdateAction<ProductType>> resolveReferences(
        @Nonnull final List<UpdateAction<ProductType>> updateActions) {

        return updateActions
            .stream()
            .map(action -> {
                final AddAttributeDefinition addAttributeDefinition = (AddAttributeDefinition) (action);
                final AttributeDefinitionDraft attribute = addAttributeDefinition.getAttribute();
                final AttributeDefinitionReferenceResolver attributeDefinitionReferenceResolver =
                    new AttributeDefinitionReferenceResolver(syncOptions, productTypeService);
                final AttributeDefinitionDraft resolvedDraft = attributeDefinitionReferenceResolver
                    .resolveReferences(attribute)
                    .toCompletableFuture()
                    .join();
                return AddAttributeDefinition.of(resolvedDraft);
            })
            .collect(Collectors.toList());
    }

    @Nonnull
    private CompletionStage<Void> updateProductType(
        @Nonnull final ProductType oldProductType,
        @Nonnull final List<UpdateAction<ProductType>> updateActions) {

        return productTypeService
            .updateProductType(oldProductType, updateActions)
            .handle(ImmutablePair::new)
            .thenCompose(updateResponse -> {
                final Throwable sphereException = updateResponse.getValue();
                if (sphereException != null) {
                    return executeSupplierIfConcurrentModificationException(
                        sphereException,
                        () -> fetchAndUpdate(oldProductType, updateActions),
                        () -> {
                            final String errorMessage =
                                format(CTP_PRODUCT_TYPE_UPDATE_FAILED, oldProductType.getKey(),
                                    sphereException.getMessage());
                            handleError(errorMessage, sphereException, 1);
                            return CompletableFuture.completedFuture(null);
                        });
                } else {
                    // Update missing parents by removing parent keys in ready to resolve.
                    statistics.removeProductTypeWaitingToBeResolvedKey(oldProductType.getKey());
                    return CompletableFuture.completedFuture(null);
                }
            });
    }

    @Nonnull
    private CompletionStage<Void> fetchAndUpdate(
        @Nonnull final ProductType oldProductType,
        @Nonnull final List<UpdateAction<ProductType>> updateActions) {

        final String key = oldProductType.getKey();
        return productTypeService
            .fetchProductType(key)
            .handle(ImmutablePair::new)
            .thenCompose(fetchResponse -> {
                final Optional<ProductType> fetchedProductTypeOptional = fetchResponse.getKey();
                final Throwable exception = fetchResponse.getValue();

                if (exception != null) {
                    final String errorMessage = format(CTP_PRODUCT_TYPE_UPDATE_FAILED, key,
                        "Failed to fetch from CTP while retrying after concurrency modification.");
                    handleError(errorMessage, exception, 1);
                    return CompletableFuture.completedFuture(null);
                }

                return fetchedProductTypeOptional
                    .map(fetchedProductType -> updateProductType(oldProductType, updateActions))
                    .orElseGet(() -> {
                        final String errorMessage =
                            format(CTP_PRODUCT_TYPE_UPDATE_FAILED, key,
                                "Not found when attempting to fetch while retrying "
                                    + "after concurrency modification.");
                        handleError(errorMessage, null, 1);
                        return CompletableFuture.completedFuture(null);
                    });
            });
    }


    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    @Nonnull
    private CompletionStage<Void> buildActionsAndUpdate(
        @Nonnull final ProductType oldProductType,
        @Nonnull final ProductTypeDraft newProductType) {

        final List<UpdateAction<ProductType>> updateActions = buildActions(oldProductType, newProductType, syncOptions);

        final List<UpdateAction<ProductType>> updateActionsAfterCallback =
            syncOptions.applyBeforeUpdateCallBack(updateActions, newProductType, oldProductType);

        if (!updateActionsAfterCallback.isEmpty()) {
            return updateProductType(oldProductType, newProductType, updateActionsAfterCallback);
        }

        return completedFuture(null);
    }

    /**
     * Given an existing {@link ProductType} and a new {@link ProductTypeDraft}, the method calculates all the
     * update actions required to synchronize the existing product type to be the same as the new one. If there are
     * update actions found, a request is made to CTP to update the existing product type, otherwise it doesn't issue a
     * request.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP, the error handling method
     * is called.
     *
     * @param oldProductType existing product type that could be updated.
     * @param newProductType draft containing data that could differ from data in {@code oldProductType}.
     * @return a {@link CompletionStage} which contains an empty result after execution of the update.
     */
    @Nonnull
    private CompletionStage<Void> updateProductType(
        @Nonnull final ProductType oldProductType,
        @Nonnull final ProductTypeDraft newProductType,
        @Nonnull final List<UpdateAction<ProductType>> updateActions) {

        return productTypeService
            .updateProductType(oldProductType, updateActions)
            .handle(ImmutablePair::new)
            .thenCompose(updateResponse -> {
                final Throwable sphereException = updateResponse.getValue();
                if (sphereException != null) {
                    return executeSupplierIfConcurrentModificationException(
                        sphereException,
                        () -> fetchAndUpdate(oldProductType, newProductType),
                        () -> {
                            final String errorMessage =
                                format(CTP_PRODUCT_TYPE_UPDATE_FAILED, newProductType.getKey(),
                                    sphereException.getMessage());
                            handleError(errorMessage, sphereException, 1);
                            return CompletableFuture.completedFuture(null);
                        });
                } else {
                    statistics.incrementUpdated();
                    return CompletableFuture.completedFuture(null);
                }
            });
    }

    @Nonnull
    private CompletionStage<Void> fetchAndUpdate(
        @Nonnull final ProductType oldProductType,
        @Nonnull final ProductTypeDraft newProductType) {

        final String key = oldProductType.getKey();
        return productTypeService
            .fetchProductType(key)
            .handle(ImmutablePair::new)
            .thenCompose(fetchResponse -> {
                final Optional<ProductType> fetchedProductTypeOptional = fetchResponse.getKey();
                final Throwable exception = fetchResponse.getValue();

                if (exception != null) {
                    final String errorMessage = format(CTP_PRODUCT_TYPE_UPDATE_FAILED, key,
                        "Failed to fetch from CTP while retrying after concurrency modification.");
                    handleError(errorMessage, exception, 1);
                    return CompletableFuture.completedFuture(null);
                }

                return fetchedProductTypeOptional
                    .map(fetchedProductType -> buildActionsAndUpdate(fetchedProductType, newProductType))
                    .orElseGet(() -> {
                        final String errorMessage =
                            format(CTP_PRODUCT_TYPE_UPDATE_FAILED, key,
                                "Not found when attempting to fetch while retrying "
                                    + "after concurrency modification.");
                        handleError(errorMessage, null, 1);
                        return CompletableFuture.completedFuture(null);
                    });
            });
    }

    /**
     * Given a product type draft, this method applies the beforeCreateCallback and then issues a create request to the
     * CTP project to create the corresponding Product Type.
     *
     * @param productTypeDraft the product type draft to create the product type from.
     * @return a {@link CompletionStage} which contains an empty result after execution of the create.
     */
    @Nonnull
    private CompletionStage<Void> applyCallbackAndCreate(
        @Nonnull final ProductTypeDraft productTypeDraft) {

        return syncOptions
            .applyBeforeCreateCallBack(productTypeDraft)
            .map(draft -> productTypeService
                .createProductType(draft)
                .thenAccept(productTypeOptional -> {
                    if (productTypeOptional.isPresent()) {
                        readyToResolve.add(productTypeDraft.getKey());
                        statistics.incrementCreated();
                    } else {
                        statistics.incrementFailed();
                    }
                })
            )
            .orElseGet(() -> CompletableFuture.completedFuture(null));
    }
}
