package com.commercetools.sync.producttypes;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.impl.ProductTypeServiceImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;

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

    private final ProductTypeService productTypeService;

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

        final Set<ProductTypeDraft> validProductTypeDrafts = batch.stream()
                                                                  .filter(this::validateDraft)
                                                                  .collect(toSet());

        if (validProductTypeDrafts.isEmpty()) {
            statistics.incrementProcessed(batch.size());
            return completedFuture(statistics);
        } else {
            final Set<String> keys = validProductTypeDrafts.stream().map(ProductTypeDraft::getKey).collect(toSet());


            return productTypeService
                .fetchMatchingProductTypesByKeys(keys)
                .handle(ImmutablePair::new)
                .thenCompose(fetchResponse -> {
                    final Set<ProductType> fetchedProductTypes = fetchResponse.getKey();
                    final Throwable exception = fetchResponse.getValue();

                    if (exception != null) {
                        final String errorMessage = format(CTP_PRODUCT_TYPE_FETCH_FAILED, keys);
                        handleError(errorMessage, exception, keys.size());
                        return CompletableFuture.completedFuture(null);
                    } else {
                        return syncBatch(fetchedProductTypes, validProductTypeDrafts);
                    }
                })
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
            @Nonnull final Set<ProductTypeDraft> newProductTypes) {

        final Map<String, ProductType> oldProductTypeMap =
            oldProductTypes.stream().collect(toMap(ProductType::getKey, identity()));

        return CompletableFuture.allOf(newProductTypes
            .stream()
            .map(newProductType -> {
                final ProductType oldProductType = oldProductTypeMap.get(newProductType.getKey());

                return ofNullable(oldProductType)
                    .map(productType -> updateProductType(oldProductType, newProductType))
                    .orElseGet(() -> applyCallbackAndCreate(newProductType));
            })
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
    }

    /**
     * Given a product type draft, this method applies the beforeCreateCallback and then issues a create request to the
     * CTP project to create the corresponding Product Type.
     *
     * @param productTypeDraft the product type draft to create the product type from.
     * @return a {@link CompletionStage} which contains an empty result after execution of the create.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    @Nonnull
    private CompletionStage<Void> applyCallbackAndCreate(
        @Nonnull final ProductTypeDraft productTypeDraft) {

        return syncOptions
            .applyBeforeCreateCallBack(productTypeDraft)
            .map(draft -> productTypeService
                .createProductType(draft)
                .thenAccept(productTypeOptional -> {
                    if (productTypeOptional.isPresent()) {
                        statistics.incrementCreated();
                    } else {
                        statistics.incrementFailed();
                    }
                })
            )
            .orElse(CompletableFuture.completedFuture(null));
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
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    @Nonnull
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
