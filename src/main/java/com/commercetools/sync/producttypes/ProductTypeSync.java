package com.commercetools.sync.producttypes;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.impl.ProductTypeServiceImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.WithKey;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static com.commercetools.sync.producttypes.utils.ProductTypeSyncUtils.buildActions;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ProductTypeSync extends BaseSync<ProductTypeDraft, ProductTypeSyncStatistics, ProductTypeSyncOptions> {
    private static final String CTP_PRODUCT_TYPE_FETCH_FAILED = "Failed to fetch existing product types of keys '%s'.";
    private static final String CTP_PRODUCT_TYPE_UPDATE_FAILED = "Failed to update product type of key '%s'.";
    private static final String CTP_PRODUCT_TYPE_CREATE_FAILED = "Failed to create product type of key '%s'.";
    private static final String PRODUCT_TYPE_DRAFT_HAS_NO_KEY = "Failed to process product type draft without key.";
    private static final String PRODUCT_TYPE_DRAFT_IS_NULL = "Failed to process null product type draft.";

    private final ProductTypeService productTypeService;

    /**
     * Takes a {@link ProductTypeSyncOptions} instance to instantiate a new {@link ProductTypeSync} instance that
     * could be used to sync product type drafts with the given product types in the CTP project specified in the
     * injected {@link ProductTypeSyncOptions} instance.
     *
     * @param productTypeSyncOptions the container of all the options of the sync process including the CTP project
     *                               client and/ormconfiguration and other sync-specific options.
     */
    public ProductTypeSync(@Nonnull final ProductTypeSyncOptions productTypeSyncOptions) {
        super(new ProductTypeSyncStatistics(), productTypeSyncOptions);
        this.productTypeService = new ProductTypeServiceImpl(productTypeSyncOptions);
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

    @Override
    protected CompletionStage<ProductTypeSyncStatistics> syncBatches(
            @Nonnull final List<List<ProductTypeDraft>> batches,
            @Nonnull final CompletionStage<ProductTypeSyncStatistics> result) {

        if (batches.isEmpty()) {
            return result;
        }

        final List<ProductTypeDraft> firstBatch = batches.remove(0);
        return syncBatches(batches, result.thenCompose(subResult -> processBatch(firstBatch)));
    }

    /**
     * Fetches existing {@link ProductType} objects from CTP project that correspond to passed {@code batch}.
     * Having existing product types fetched, {@code batch} is compared and synced with fetched objects by
     * {@link ProductTypeSync#syncBatch(List, List)} function. When fetching existing product types results in
     * an empty optional then {@code batch} isn't processed.
     *
     * @param batch batch of drafts that need to be synced
     * @return {@link CompletionStage} of {@link Void} that indicates method progress.
     */
    @Override
    protected CompletionStage<ProductTypeSyncStatistics> processBatch(@Nonnull final List<ProductTypeDraft> batch) {
        final List<ProductTypeDraft> validProductTypeDrafts = batch.stream()
                .filter(this::validateDraft)
                .collect(toList());

        if (validProductTypeDrafts.isEmpty()) {
            statistics.incrementProcessed(batch.size());
            return completedFuture(statistics);
        } else {
            final Map<String, ProductTypeDraft> keysProductTypeDraftMap = getKeysProductTypeMap(validProductTypeDrafts);


            return fetchExistingProductTypes(keysProductTypeDraftMap.keySet())
                .thenCompose(oldProductTypeOptional -> oldProductTypeOptional
                    .map(oldProductTypes -> syncBatch(oldProductTypes, validProductTypeDrafts))
                    .orElseGet(() -> completedFuture(statistics)))
                .thenApply(v -> {
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
     * @return a future which contains the list of product types corresponding to the keys.
     */
    private CompletionStage<Optional<List<ProductType>>> fetchExistingProductTypes(@Nonnull final Set<String> keys) {
        return productTypeService
                .fetchMatchingProductsTypesByKeys(keys)
                .thenApply(Optional::of)
                .exceptionally(exception -> {
                    final String errorMessage = format(CTP_PRODUCT_TYPE_FETCH_FAILED, keys);
                    handleError(errorMessage, exception, keys.size());

                    return Optional.empty();
                });
    }

    /**
     * Given a list of {@link ProductType} or {@link ProductTypeDraft}, returns a map of keys to the
     * {@link ProductType}/{@link ProductTypeDraft} instances.
     *
     * @param productTypes list of {@link ProductType}/{@link ProductTypeDraft}
     * @param <T>          a type that extends of {@link WithKey}.
     * @return the map of keys to {@link ProductType}/{@link ProductTypeDraft} instances.
     */
    protected <T extends WithKey> Map<String, T> getKeysProductTypeMap(@Nonnull final List<T> productTypes) {
        return productTypes.stream().collect(Collectors.toMap(WithKey::getKey, p -> p,
            (productTypeA, productTypeB) -> productTypeB));
    }

    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed product types to sync.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     * @param exception    The exception that called caused the failure, if any.
     */
    private void handleError(@Nonnull final String errorMessage, @Nullable final Throwable exception,
                             final int failedTimes) {

        syncOptions.applyErrorCallback(errorMessage, exception);
        statistics.incrementFailed(failedTimes);
    }

    /**
     * Given a list of product type drafts, attempts to sync the drafts with the existing products types in the CTP
     * project. The product type and the draft are considered to match if they have the same key.
     *
     * @param oldProductTypes old product types.
     * @param newProductTypes drafts that need to be synced.
     * @return a future which contains an empty result after execution of the update
     */
    private CompletionStage<ProductTypeSyncStatistics> syncBatch(
            @Nonnull final List<ProductType> oldProductTypes,
            @Nonnull final List<ProductTypeDraft> newProductTypes) {

        final Map<String, ProductType> oldProductTypeMap = getKeysProductTypeMap(oldProductTypes);

        List<CompletableFuture<Void>> futures = newProductTypes
                .stream()
                .map(newProductType -> {
                    final ProductType oldProductType = oldProductTypeMap.get(newProductType.getKey());

                    if (oldProductType == null) {
                        return createProductType(newProductType);
                    } else {
                        return updateProductType(oldProductType, newProductType);
                    }
                })
                .map(CompletionStage::toCompletableFuture)
                .collect(toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(result -> statistics);
    }

    /**
     * Given a product type draft, issues a request to the CTP project to create a corresponding Product Type.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP, the error handling method
     * is called.
     *
     * @param productTypeDraft the product type draft to create the product type from.
     * @return a future which contains an empty result after execution of the create.
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
     * @return a future which contains an empty result after execution of the update.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<Void> updateProductType(@Nonnull final ProductType oldProductType,
                                                    @Nonnull final ProductTypeDraft newProductType) {

        final List<UpdateAction<ProductType>> updateActions = buildActions(oldProductType, newProductType, syncOptions);

        final List<UpdateAction<ProductType>> updateActionsAfterCallback = syncOptions.applyBeforeUpdateCallBack(
            updateActions,
            newProductType,
            oldProductType
        );

        if (!updateActionsAfterCallback.isEmpty()) {
            return productTypeService.updateProductType(oldProductType, updateActionsAfterCallback)
                    .thenAccept(updatedProductType -> statistics.incrementUpdated())
                    .exceptionally(exception -> {
                        final String errorMessage = format(CTP_PRODUCT_TYPE_UPDATE_FAILED, newProductType.getKey());
                        handleError(errorMessage, exception, 1);

                        return null;
                    });
        }

        return completedFuture(null);
    }
}
