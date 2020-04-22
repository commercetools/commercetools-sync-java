package com.commercetools.sync.producttypes;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.exceptions.InvalidReferenceException;
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
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static com.commercetools.sync.producttypes.helpers.ProductTypeBatchProcessor.getProductTypeKey;
import static com.commercetools.sync.producttypes.utils.ProductTypeSyncUtils.buildActions;
import static java.lang.String.format;
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
    private ConcurrentHashMap.KeySetView<String, Boolean> readyToResolve;

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
     * {@link ProductTypeSync#syncBatch(Set, Set, Map)} is called to perform the sync (<b>update</b> or <b>create</b>
     * requests accordingly) on the target project.
     *
     * <p>After the batch is synced, the method resolves all missing nested references that could have been created
     * after execution of sync of batch.
     * For more info check {@link ProductTypeSync#resolveMissingNestedReferences(Map)}.
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
                    handleError("Failed to build a cache of keys to ids.", cachingException,
                        keysToCache.size());
                    return CompletableFuture.completedFuture(null);
                }


                final Set<String> batchDraftKeys = batchProcessor
                    .getValidDrafts()
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
                            final String errorMessage = format(CTP_PRODUCT_TYPE_FETCH_FAILED, batchDraftKeys);
                            handleError(errorMessage, exception, batchDraftKeys.size());
                            return CompletableFuture.completedFuture(null);
                        } else {
                            return syncBatch(matchingProductTypes, batchProcessor.getValidDrafts(), keyToIdCache)
                                .thenApply(ignoredResult -> buildProductTypesToUpdateMap())
                                .thenCompose(this::resolveMissingNestedReferences);
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
     * Given a set of product type drafts, attempts to sync the drafts with the existing products types in the target
     * CTP project. The product type and the draft are considered to match if they have the same key.
     *
     *
     * <p>Note: In order to support syncing product types with nested references in any order, this method will
     * remove any attribute which contains a nested reference on the drafts and keep track of it to be resolved as
     * soon as the referenced product type becomes available.
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
                removeAndKeepTrackOfMissingNestedAttributes(newProductType, keyToIdCache))
            .map(draftWithoutMissingRefAttrs -> referenceResolver
                .resolveReferences(draftWithoutMissingRefAttrs)
                .thenCompose(resolvedDraft -> syncDraft(oldProductTypeMap, resolvedDraft))
                .exceptionally(completionException -> {
                    final String errorMessage = format(FAILED_TO_RESOLVE_REFERENCES,
                            draftWithoutMissingRefAttrs.getKey(),
                            completionException.getMessage());
                    handleError(errorMessage, completionException, 1);
                    return null;

                })
            )
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
    }


    /**
     * First, cleans up all occurrences of {@code productTypeDraft}'s key waiting in
     * statistics#putMissingNestedProductType. This is because it should not be existing there, and if it is then the
     * values are outdated. This is to support the case, if an already visited attribute is supplied again in a later
     * batch (maybe with a different reference or a reference that doesn't exist anymore).
     *
     *
     * <p>Then, makes a copy of {@code productTypeDraft} and then goes through all its attribute definition drafts.
     * For each attribute, attempts to find a nested product type in its attribute type, if it has a
     * nested type. It checks if the key, of the productType reference, is cached in {@code keyToIdCache}. If it is,
     * then it means the referenced product type exists in the target project, so there is no need to remove or keep
     * track of it. If it is not, it means it doesn't exist yet and needs to be tracked as a
     * missing reference and also remove this attribute definition from the supplied {@code draftCopy} to be able to
     * create product type without this attribute containing the missing reference.
     *
     *
     * @param productTypeDraft         the productTypeDraft containing the attribute which should be updated by removing
     *                                 the attribute which contains the missing reference.
     * @param keyToIdCache             a map of productType key to id. It represents a cache of the existing
     *                                 productTypes in the target project.
     */
    @SuppressWarnings("ConstantConditions") // since the batch is validate before, key is assured to be non-blank here.
    @Nonnull
    private ProductTypeDraft removeAndKeepTrackOfMissingNestedAttributes(
        @Nonnull final ProductTypeDraft productTypeDraft,
        @Nonnull final Map<String, String> keyToIdCache) {

        statistics.removeReferencingProductTypeKey(productTypeDraft.getKey());

        final List<AttributeDefinitionDraft> attributeDefinitionDrafts = productTypeDraft.getAttributes();
        if (attributeDefinitionDrafts == null || attributeDefinitionDrafts.isEmpty()) {
            return productTypeDraft;
        }

        // copies to avoid mutation of attributes array supplied by user.
        final ProductTypeDraft draftCopy = ProductTypeDraftBuilder
            .of(productTypeDraft)
            .attributes(new ArrayList<>(productTypeDraft.getAttributes()))
            .build();

        for (AttributeDefinitionDraft attributeDefinitionDraft : attributeDefinitionDrafts) {
            if (attributeDefinitionDraft != null) {
                removeAndKeepTrackOfMissingNestedAttribute(attributeDefinitionDraft, draftCopy, keyToIdCache);
            }
        }

        return draftCopy;
    }

    /**
     * Attempts to find a nested product type in the attribute type of {@code attributeDefinitionDraft}, if it has a
     * nested type. It checks if the key, of the productType reference, is cached in {@code keyToIdCache}. If it is,
     * then it means the referenced product type exists in the target project, so there is no need to remove or keep
     * track of it. However, if it is not, it means it doesn't exist yet, which means we need to keep track of it as a
     * missing reference and also remove this attribute definition from the supplied {@code draftCopy} to be able to
     * create product type without this attribute containing the missing reference.
     *
     *
     * <p>Note: This method mutates in the supplied {@code productTypeDraft} attribute definition list by removing
     * the attribute containing a missing reference.
     *
     * @param attributeDefinitionDraft the attribute definition being checked for any product references.
     * @param productTypeDraft         the productTypeDraft containing the attribute which should be updated by removing
     *                                 the attribute which contains the missing reference.
     * @param keyToIdCache             a map of productType key to id. It represents a cache of the existing
     *                                 productTypes in the target project.
     */
    @SuppressWarnings("ConstantConditions") // since the batch is validate before, key is assured to be non-blank here.
    private void removeAndKeepTrackOfMissingNestedAttribute(
        @Nonnull final AttributeDefinitionDraft attributeDefinitionDraft,
        @Nonnull final ProductTypeDraft productTypeDraft,
        @Nonnull final Map<String, String> keyToIdCache) {

        final AttributeType attributeType = attributeDefinitionDraft.getAttributeType();

        try {
            getProductTypeKey(attributeType)
                .ifPresent(key -> {
                    if (!keyToIdCache.keySet().contains(key)) {
                        productTypeDraft.getAttributes().remove(attributeDefinitionDraft);
                        statistics.putMissingNestedProductType(
                            key, productTypeDraft.getKey(), attributeDefinitionDraft);
                    }
                });
        } catch (InvalidReferenceException invalidReferenceException) {
            handleError("This exception is unexpectedly thrown since the draft batch has been"
                    + "already validated for blank keys at an earlier stage, which means this draft should"
                    + " have a valid reference. Please communicate this error with the maintainer of the library.",
                invalidReferenceException, 1);
        }
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


    /**
     * Every key in the {@code readyToResolve} set, represents a product type which is now existing on the target
     * project and can now be resolved on any of the referencing product types which were kept track of in
     * {@link ProductTypeSyncStatistics#missingNestedProductTypes} map.
     *
     * <p>Based on the contents of the {@link ProductTypeSyncStatistics#missingNestedProductTypes} and the
     * {@code readyToResolve} set, this method builds a map of product type keys pointing to a set of attribute
     * definition drafts which are now ready to be added for this product type. The purpose of this is to aggregate
     * all the definitions that are needed to be added to every product type together so that we can issue them
     * together in the same update request.
     *
     * @return a map of product type keys pointing to a set of attribute definition drafts which are now ready to be
     *         added for this product type.
     */
    @Nonnull
    private Map<String, Set<AttributeDefinitionDraft>> buildProductTypesToUpdateMap() {

        final Map<String, Set<AttributeDefinitionDraft>> productTypesToUpdate = new HashMap<>();

        readyToResolve
            .forEach(readyToResolveProductTypeKey -> {
                final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
                    referencingProductTypes = statistics
                    .getProductTypeKeysWithMissingParents()
                    .get(readyToResolveProductTypeKey);

                if (referencingProductTypes != null) {
                    referencingProductTypes
                        .forEach((productTypeKey, attributes) -> {

                            final Set<AttributeDefinitionDraft> attributeDefinitionsToAdd =
                                productTypesToUpdate.get(productTypeKey);

                            if (attributeDefinitionsToAdd != null) {
                                attributeDefinitionsToAdd.addAll(attributes);
                            } else {
                                productTypesToUpdate.put(productTypeKey, attributes);
                            }
                        });
                }
            });

        return productTypesToUpdate;
    }

    /**
     * Given a map of product type keys pointing to a set of attribute definition drafts which are now ready to be added
     * for this product type. This method first converts the drafts to {@link AddAttributeDefinition} actions in which
     * the reference id value (which is a key) is resolved to an actual UUID of the product type key pointed by this
     * key. Then, for each product type, the method issues an update request containing all the actions.
     *
     * @return a {@link CompletionStage} which contains an empty result after execution of all the update requests.
     */
    @Nonnull
    private CompletionStage<Void> resolveMissingNestedReferences(
        @Nonnull final Map<String, Set<AttributeDefinitionDraft>> productTypesToUpdate) {

        final Set<String> keys = productTypesToUpdate.keySet();
        return productTypeService
            .fetchMatchingProductTypesByKeys(keys)
            .handle(ImmutablePair::new)
            .thenCompose(fetchResponse -> {

                final Set<ProductType> matchingProductTypes = fetchResponse.getKey();
                final Throwable exception = fetchResponse.getValue();
                if (exception != null) {
                    final String errorMessage = format(CTP_PRODUCT_TYPE_FETCH_FAILED, keys);
                    syncOptions.applyErrorCallback(errorMessage, exception);
                    return CompletableFuture.completedFuture(null);
                } else {
                    final Map<String, ProductType> keyToProductType =
                        matchingProductTypes
                            .stream()
                            .collect(Collectors.toMap(ProductType::getKey, productType -> productType));
                    return CompletableFuture.allOf(
                        productTypesToUpdate
                            .entrySet()
                            .stream()
                            .map(entry -> {
                                final String productTypeToUpdateKey = entry.getKey();
                                final Set<AttributeDefinitionDraft> attributeDefinitionDrafts = entry.getValue();

                                final List<UpdateAction<ProductType>> actionsWithResolvedReferences =
                                    draftsToActions(attributeDefinitionDrafts);

                                final ProductType productTypeToUpdate = keyToProductType.get(productTypeToUpdateKey);

                                return resolveMissingNestedReferences(
                                    productTypeToUpdate,
                                    actionsWithResolvedReferences);

                            })
                            .map(CompletionStage::toCompletableFuture)
                            .toArray(CompletableFuture[]::new));
                }
            });
    }

    /**
     * Given an existing {@link ProductType} and a list of {@link UpdateAction}s, required to resolve the productType
     * with nestedType references.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP, the error handling method
     * is called.
     *
     * @param oldProductType existing product type that could be updated.
     * @param updateActions actions to update the product type with.
     * @return a {@link CompletionStage} which contains an empty result after execution of the update.
     */
    @SuppressWarnings("ConstantConditions") // since the batch is validate before, key is assured to be non-blank here.
    @Nonnull
    private CompletionStage<Void> resolveMissingNestedReferences(
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
                        () -> fetchAndUpdate(oldProductType,
                            fetchedProductType -> resolveMissingNestedReferences(fetchedProductType, updateActions)),
                        () -> {
                            final String errorMessage =
                                format(CTP_PRODUCT_TYPE_UPDATE_FAILED, oldProductType.getKey(),
                                    sphereException.getMessage());
                            handleError(errorMessage, sphereException, 1);
                            return CompletableFuture.completedFuture(null);
                        });
                } else {
                    // Update missing parents by removing parent keys in ready to resolve.
                    statistics.removeReferencingProductTypeKey(oldProductType.getKey());
                    return CompletableFuture.completedFuture(null);
                }
            });
    }

    /**
     * Given a set of {@link AttributeDefinitionDraft}, for every draft, this method resolves the nested type reference
     * on the attribute definition draft and creates an {@link AddAttributeDefinition} action out of it and returns
     * a list of update actions.
     *
     * @return a list of update actions corresponding to the supplied set of {@link AttributeDefinitionDraft}s.
     */
    @Nonnull
    private List<UpdateAction<ProductType>> draftsToActions(
        @Nonnull final Set<AttributeDefinitionDraft> attributeDefinitionDrafts) {

        return attributeDefinitionDrafts
            .stream()
            .map(attributeDefinitionDraft -> {
                final AttributeDefinitionReferenceResolver attributeDefinitionReferenceResolver =
                    new AttributeDefinitionReferenceResolver(syncOptions, productTypeService);
                final AttributeDefinitionDraft resolvedDraft = attributeDefinitionReferenceResolver
                    .resolveReferences(attributeDefinitionDraft)
                    .toCompletableFuture()
                    .join();
                return AddAttributeDefinition.of(resolvedDraft);
            })
            .collect(Collectors.toList());
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
                        () -> fetchAndUpdate(oldProductType,
                            fetchedProductType -> buildActionsAndUpdate(fetchedProductType, newProductType)),
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
        @Nonnull final Function<ProductType, CompletionStage<Void>> fetchedProductMapper) {

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
                    .map(fetchedProductMapper)
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
