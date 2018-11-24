package com.commercetools.sync.producttypes;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.impl.ProductTypeServiceImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static com.commercetools.sync.producttypes.utils.ProductTypeSyncUtils.buildActions;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * This class syncs product type drafts with the corresponding product types in the CTP project.
 */
public class ProductTypeSync extends BaseSync<ProductTypeDraft, ProductTypeSyncStatistics, ProductTypeSyncOptions> {
    private static final String CTP_PRODUCT_TYPE_FETCH_FAILED = "Failed to fetch existing product types of keys '%s'.";
    private static final String CTP_PRODUCT_TYPE_UPDATE_FAILED = "Failed to update product type of key '%s'.";
    private static final String CTP_PRODUCT_TYPE_CREATE_FAILED = "Failed to create product type of key '%s'.";
    private static final String PRODUCT_TYPE_DRAFT_HAS_NO_KEY = "Failed to process product type draft without key.";
    private static final String PRODUCT_TYPE_DRAFT_IS_NULL = "Failed to process null product type draft.";
    private static final String FETCH_ON_RETRY = "Failed to fetch category on retry.";

    private final ProductTypeService productTypeService;

    public ProductTypeSync(@Nonnull final ProductTypeSyncOptions productTypeSyncOptions) {
        super(new ProductTypeSyncStatistics(), productTypeSyncOptions);
        this.productTypeService = new ProductTypeServiceImpl(productTypeSyncOptions);
    }

    public ProductTypeSync(@Nonnull final ProductTypeSyncOptions productTypeSyncOptions,
                           @Nonnull final ProductTypeService productTypeService) {
        super(new ProductTypeSyncStatistics(), productTypeSyncOptions);
        this.productTypeService = productTypeService;
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
     * validation, check: {@link ProductTypeSync#validateDraft(ProductTypeDraft)}. Using the resulting set of
     * {@code validProductTypeDrafts}, the matching productTypes in the target CTP project are fetched then the method
     * {@link ProductTypeSync#syncBatch(Set, Set)} is called to perform the sync (<b>update</b> or <b>create</b>
     * requests accordingly) on the target project.
     *
     * @param batch batch of drafts that need to be synced
     * @return a {@link CompletionStage} containing an instance
     *         of {@link ProductTypeSyncStatistics} which contains information about the result of syncing the supplied
     *         batch to the target project.
     */
    @Override
    protected CompletionStage<ProductTypeSyncStatistics> processBatch(@Nonnull final List<ProductTypeDraft> batch) {

        final Set<ProductTypeDraft> validProductTypeDrafts = batch.stream()
                                                                  .filter(this::validateDraft)
                                                                  .collect(toSet());

        if (validProductTypeDrafts.isEmpty()) {
            statistics.incrementProcessed(batch.size());
            return completedFuture(statistics);
        } else {
            final Set<String> keys = validProductTypeDrafts.stream().map(ProductTypeDraft::getKey).collect(toSet());

            return fetchExistingProductTypes(keys)
                .thenCompose(oldProductTypes -> syncBatch(oldProductTypes, validProductTypeDrafts))
                .thenApply(ignored -> {
                    statistics.incrementProcessed(batch.size());
                    return statistics;
                });
        }
    }

    /**
     * Checks if a draft is valid for further processing. If so, then returns {@code true}. Otherwise handles an error
     * and returns {@code false}. A valid draft is a {@link ProductTypeDraft} object that is not {@code null} and its
     * key is not empty.
     *
     * @param draft nullable draft
     * @return boolean that indicate if given {@code draft} is valid for sync
     */
    private boolean validateDraft(@Nullable final ProductTypeDraft draft) {
        if (draft == null) {
            handleError(PRODUCT_TYPE_DRAFT_IS_NULL, null, 1);
        } else if (isBlank(draft.getKey())) {
            handleError(PRODUCT_TYPE_DRAFT_HAS_NO_KEY, null, 1);
        } else {
            return true;
        }

        return false;
    }

    /**
     * Given a set of product type keys, fetches the corresponding product types from CTP if they exist.
     *
     * @param keys the keys of the product types that are wanted to be fetched.
     * @return a {@link CompletionStage} which contains the set of product types corresponding to the keys.
     */
    private CompletionStage<Set<ProductType>> fetchExistingProductTypes(@Nonnull final Set<String> keys) {
        return productTypeService
                .fetchMatchingProductTypesByKeys(keys)
                .exceptionally(exception -> {
                    final String errorMessage = format(CTP_PRODUCT_TYPE_FETCH_FAILED, keys);
                    handleError(errorMessage, exception, keys.size());

                    return emptySet();
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
    private CompletionStage<ProductTypeSyncStatistics> syncBatch(
            @Nonnull final Set<ProductType> oldProductTypes,
            @Nonnull final Set<ProductTypeDraft> newProductTypes) {

        final Map<String, ProductType> oldProductTypeMap =
            oldProductTypes.stream().collect(toMap(ProductType::getKey, identity()));

        return CompletableFuture.allOf(newProductTypes
            .stream()
            .map(newProductType -> {
                final ProductType oldProductType = oldProductTypeMap.get(newProductType.getKey());

                return ofNullable(oldProductType)
                    .map(productType -> buildActionsAndUpdate(oldProductType, newProductType))
                    .orElseGet(() -> createProductType(newProductType));
            })
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new)).thenApply(result -> statistics);
    }

    /**
     * Given a product type draft, issues a request to the CTP project to create a corresponding Product Type.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP, the error handling method
     * is called.
     *
     * @param productTypeDraft the product type draft to create the product type from.
     * @return a {@link CompletionStage} which contains an empty result after execution of the create.
     */
    private CompletionStage<Void> createProductType(@Nonnull final ProductTypeDraft productTypeDraft) {
        return syncOptions.applyBeforeCreateCallBack(productTypeDraft)
                .map(productTypeService::createProductType)
                .map(creationFuture -> creationFuture
                        .thenAccept(createdProductType -> statistics.incrementCreated())
                        .exceptionally(exception -> {
                            final String errorMessage = format(CTP_PRODUCT_TYPE_CREATE_FAILED,
                                    productTypeDraft.getKey());
                            handleError(errorMessage, exception, 1);

                            return null;
                        }))
                .orElseGet(() -> CompletableFuture.completedFuture(null));
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<Void> buildActionsAndUpdate(@Nonnull final ProductType oldProductType,
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
    private CompletionStage<Void> updateProductType(@Nonnull final ProductType oldProductType,
                                                    @Nonnull final ProductTypeDraft newProductType,
                                                    @Nonnull final List<UpdateAction<ProductType>> updateActions) {

        return productTypeService
                .updateProductType(oldProductType, updateActions)
                .handle((updatedCategory, sphereException) -> sphereException)
                .thenCompose(sphereException -> {
                    if (sphereException != null) {
                        return executeSupplierIfConcurrentModificationException(
                            sphereException,
                            () -> fetchAndUpdate(oldProductType, newProductType),
                            () -> {
                                final String errorMessage =
                                        format(CTP_PRODUCT_TYPE_UPDATE_FAILED, newProductType.getKey());
                                handleError(errorMessage, sphereException, 1);
                                return CompletableFuture.completedFuture(null);
                            });
                    } else {
                        statistics.incrementUpdated();
                        return CompletableFuture.completedFuture(null);
                    }
                });
    }

    private CompletionStage<Void> fetchAndUpdate(@Nonnull final ProductType oldProductType,
                                                 @Nonnull final ProductTypeDraft newProductType) {
        final String key = oldProductType.getKey();
        return productTypeService
                .fetchProductType(key)
                .thenCompose(productTypeOptional ->
                        productTypeOptional
                                .map(productType -> buildActionsAndUpdate(productType, newProductType))
                                .orElseGet(() -> {
                                    final String errorMessage = format("%s%s",
                                            format(CTP_PRODUCT_TYPE_UPDATE_FAILED, key), FETCH_ON_RETRY);
                                    handleError(errorMessage, null, 1);
                                    return CompletableFuture.completedFuture(null);
                                }));
    }
}
