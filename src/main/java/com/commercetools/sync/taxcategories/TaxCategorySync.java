package com.commercetools.sync.taxcategories;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.services.impl.TaxCategoryServiceImpl;
import com.commercetools.sync.taxcategories.helpers.TaxCategorySyncStatistics;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static com.commercetools.sync.taxcategories.utils.TaxCategorySyncUtils.buildActions;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class TaxCategorySync extends BaseSync<TaxCategoryDraft, TaxCategorySyncStatistics, TaxCategorySyncOptions> {

    private static final String TAX_CATEGORY_FETCH_FAILED = "Failed to fetch existing tax categories with keys: '%s'.";
    private static final String TAX_CATEGORY_UPDATE_FAILED = "Failed to update tax category with key: '%s'. Reason: %s";
    private static final String TAX_CATEGORY_DRAFT_HAS_NO_KEY = "Failed to process tax category draft without key.";
    private static final String TAX_CATEGORY_DRAFT_IS_NULL = "Failed to process null tax category draft.";
    private static final String TAX_CATEGORY_DUPLICATED_COUNTRY = "Tax rate drafts have duplicated country "
            + "codes. Duplicated tax rate country code: '%s'. Tax rate country codes and "
            + "states are expected to be unique inside their tax category.";
    private static final String TAX_CATEGORY_DUPLICATED_COUNTRY_AND_STATE = "Tax rate drafts have duplicated country "
        + "codes and states. Duplicated tax rate country code: '%s'. state : '%s'. Tax rate country codes and "
        + "states are expected to be unique inside their tax category.";

    private final TaxCategoryService taxCategoryService;

    public TaxCategorySync(@Nonnull final TaxCategorySyncOptions taxCategorySyncOptions) {
        this(taxCategorySyncOptions, new TaxCategoryServiceImpl(taxCategorySyncOptions));
    }

    /**
     * Takes a {@link TaxCategorySyncOptions} and a {@link TaxCategorySync} instances to instantiate
     * a new {@link TaxCategorySync} instance that could be used to sync tax category drafts in the CTP project
     * specified in the injected {@link TaxCategorySyncOptions} instance.
     *
     * <p>NOTE: This constructor is mainly to be used for tests where the services can be mocked and passed to.
     *
     * @param taxCategorySyncOptions the container of all the options of the sync process including the CTP project
     *                               client and/or configuration and other sync-specific options.
     * @param taxCategoryService     the tax category service which is responsible for fetching/caching the
     *                               tax categories from the CTP project.
     */
    TaxCategorySync(@Nonnull final TaxCategorySyncOptions taxCategorySyncOptions,
                    @Nonnull final TaxCategoryService taxCategoryService) {
        super(new TaxCategorySyncStatistics(), taxCategorySyncOptions);
        this.taxCategoryService = taxCategoryService;
    }

    @Override
    protected CompletionStage<TaxCategorySyncStatistics> process(@Nonnull final List<TaxCategoryDraft> resourceDrafts) {
        List<List<TaxCategoryDraft>> batches = batchElements(resourceDrafts, syncOptions.getBatchSize());
        return syncBatches(batches, completedFuture(statistics));
    }

    /**
     * This method first creates a new {@link Set} of valid {@link TaxCategoryDraft} elements. For more on the rules of
     * validation, check: {@link TaxCategorySync#validateDraft(TaxCategoryDraft)}. Using the resulting set of
     * {@code validTaxCategoryDrafts}, the matching tax categories in the target CTP project are fetched then the method
     * {@link TaxCategorySync#syncBatch(Set, Set)} is called to perform the sync (<b>update</b> or <b>create</b>
     * requests accordingly) on the target project.
     *
     * <p> In case of error during of fetching of existing tax categories, the error callback will be triggered.
     * And the sync process would stop for the given batch.
     * </p>
     *
     * @param batch batch of drafts that need to be synced
     * @return a {@link CompletionStage} containing an instance
     *         of {@link TaxCategorySyncStatistics} which contains information about the result of syncing the supplied
     *         batch to the target project.
     */
    @Override
    protected CompletionStage<TaxCategorySyncStatistics> processBatch(@Nonnull final List<TaxCategoryDraft> batch) {
        final Set<TaxCategoryDraft> validTaxCategoryDrafts = batch.stream()
            .filter(this::validateDraft)
            .collect(toSet());

        if (validTaxCategoryDrafts.isEmpty()) {
            statistics.incrementProcessed(batch.size());
            return completedFuture(statistics);
        } else {
            final Set<String> keys = validTaxCategoryDrafts.stream().map(TaxCategoryDraft::getKey).collect(toSet());
            return taxCategoryService
                .fetchMatchingTaxCategoriesByKeys(keys)
                .handle(ImmutablePair::new)
                .thenCompose(fetchResponse -> {
                    Set<TaxCategory> fetchedTaxCategories = fetchResponse.getKey();
                    final Throwable exception = fetchResponse.getValue();

                    if (exception != null) {
                        final String errorMessage = format(TAX_CATEGORY_FETCH_FAILED, keys);
                        handleError(errorMessage, exception, keys.size());
                        return completedFuture(null);
                    } else {
                        return syncBatch(fetchedTaxCategories, validTaxCategoryDrafts);
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
     * and returns {@code false}. A valid draft is a {@link TaxCategoryDraft} object that is not {@code null} and its
     * key is not empty and tax rates are not duplicated.
     *
     * @param draft nullable draft
     * @return boolean that indicate if given {@code draft} is valid for sync
     */
    private boolean validateDraft(@Nullable final TaxCategoryDraft draft) {
        if (draft == null) {
            handleError(TAX_CATEGORY_DRAFT_IS_NULL, null);
        } else if (isBlank(draft.getKey())) {
            handleError(TAX_CATEGORY_DRAFT_HAS_NO_KEY, null);
        } else if (draft.getTaxRates() != null && !draft.getTaxRates().isEmpty()) {
            return validateIfDuplicateCountryAndState(draft.getTaxRates());
        } else {
            return true;
        }

        return false;
    }

    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed tax categories to sync.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     * @param exception    The exception that called caused the failure, if any.
     * @param failedTimes  The number of times that the failed tax categories counter is incremented.
     */
    private void handleError(@Nonnull final String errorMessage, @Nullable final Throwable exception, int failedTimes) {
        syncOptions.applyErrorCallback(errorMessage, exception);
        statistics.incrementFailed(failedTimes);
    }

    private void handleError(@Nonnull final String errorMessage, @Nullable final Throwable exception) {
        handleError(errorMessage, exception, 1);
    }

    /**
     * Given a set of tax category drafts, attempts to sync the drafts with the existing tax categories in the CTP
     * project. The tax category and the draft are considered to match if they have the same key. When there will be no
     * error it will attempt to sync the drafts transactions.
     *
     * @param oldTaxCategories old tax categories.
     * @param newTaxCategories drafts that need to be synced.
     * @return a {@link CompletionStage} which contains an empty result after execution of the update
     */
    @Nonnull
    private CompletionStage<Void> syncBatch(
        @Nonnull final Set<TaxCategory> oldTaxCategories,
        @Nonnull final Set<TaxCategoryDraft> newTaxCategories) {

        final Map<String, TaxCategory> oldTaxCategoryMap = oldTaxCategories
            .stream()
            .collect(toMap(TaxCategory::getKey, identity()));

        return allOf(newTaxCategories
            .stream()
            .map(newTaxCategory -> {
                final TaxCategory oldTaxCategory = oldTaxCategoryMap.get(newTaxCategory.getKey());

                return ofNullable(oldTaxCategory)
                    .map(taxCategory -> buildActionsAndUpdate(oldTaxCategory, newTaxCategory))
                    .orElseGet(() -> applyCallbackAndCreate(newTaxCategory));
            })
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
    }

    /**
     * Given a tax category draft, this method applies the beforeCreateCallback and then issues a create request to the
     * CTP project to create the corresponding TaxCategory.
     *
     * @param taxCategoryDraft the tax category draft to create the tax category from.
     * @return a {@link CompletionStage} which contains an empty result after execution of the create.
     */
    @Nonnull
    private CompletionStage<Optional<TaxCategory>> applyCallbackAndCreate(
        @Nonnull final TaxCategoryDraft taxCategoryDraft) {

        return syncOptions
            .applyBeforeCreateCallBack(taxCategoryDraft)
            .map(draft -> taxCategoryService
                .createTaxCategory(draft)
                .thenApply(taxCategoryOptional -> {
                    if (taxCategoryOptional.isPresent()) {
                        statistics.incrementCreated();
                    } else {
                        statistics.incrementFailed();
                    }
                    return taxCategoryOptional;
                })
            )
            .orElse(completedFuture(Optional.empty()));
    }

    private boolean validateIfDuplicateCountryAndState(final List<TaxRateDraft> taxRateDrafts) {
        /*
        For TaxRates uniqueness could be ensured by country code and states.
        So in tax category sync are using country code and states for matching.

        Representation of the commercetools platform error when country code is duplicated,
            {
                "statusCode": 400,
                "message": "A duplicate value '{\"country\":\"DE\"}' exists for field 'country'.",
                "errors": [
                    {
                        "code": "DuplicateField",
                        ....
                ]
            }
        */
        Map<String,Map<String, Long>> map = taxRateDrafts.stream().collect(
                Collectors.groupingBy(draft -> Objects.toString(draft.getCountry(), ""),
                    Collectors.groupingBy(draft -> Objects.toString(draft.getState(), ""),
                        Collectors.counting())));

        for (Map.Entry<String, Map<String, Long>> countryEntry : map.entrySet()) {
            for (Map.Entry<String, Long> stateEntry: countryEntry.getValue().entrySet()) {
                if (stateEntry.getValue() > 1L) {
                    String errorMessage = StringUtils.isBlank(stateEntry.getKey())
                        ? format(TAX_CATEGORY_DUPLICATED_COUNTRY, countryEntry.getKey())
                        : format(TAX_CATEGORY_DUPLICATED_COUNTRY_AND_STATE, countryEntry.getKey(), stateEntry.getKey());
                    handleError(errorMessage, null);
                    return false;
                }
            }
        }

        return true;
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    @Nonnull
    private CompletionStage<Optional<TaxCategory>> buildActionsAndUpdate(
        @Nonnull final TaxCategory oldTaxCategory,
        @Nonnull final TaxCategoryDraft newTaxCategory) {

        final List<UpdateAction<TaxCategory>> updateActions = buildActions(oldTaxCategory, newTaxCategory);

        List<UpdateAction<TaxCategory>> updateActionsAfterCallback =
            syncOptions.applyBeforeUpdateCallBack(updateActions, newTaxCategory, oldTaxCategory);

        if (!updateActionsAfterCallback.isEmpty()) {
            return updateTaxCategory(oldTaxCategory, newTaxCategory, updateActionsAfterCallback);
        }

        return completedFuture(null);
    }

    /**
     * Given an existing {@link TaxCategory} and a new {@link TaxCategoryDraft}, the method calculates all the
     * update actions required to synchronize the existing tax category to be the same as the new one. If there are
     * update actions found, a request is made to CTP to update the existing tax category, otherwise it doesn't issue a
     * request.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP, the error handling method
     * is called.
     *
     * @param oldTaxCategory existing tax category that could be updated.
     * @param newTaxCategory draft containing data that could differ from data in {@code oldTaxCategory}.
     * @return a {@link CompletionStage} which contains an empty result after execution of the update.
     */
    @Nonnull
    private CompletionStage<Optional<TaxCategory>> updateTaxCategory(
        @Nonnull final TaxCategory oldTaxCategory,
        @Nonnull final TaxCategoryDraft newTaxCategory,
        @Nonnull final List<UpdateAction<TaxCategory>> updateActions) {

        return taxCategoryService
            .updateTaxCategory(oldTaxCategory, updateActions)
            .handle(ImmutablePair::new)
            .thenCompose(updateResponse -> {
                final TaxCategory updatedTaxCategory = updateResponse.getKey();
                final Throwable sphereException = updateResponse.getValue();

                if (sphereException != null) {
                    return executeSupplierIfConcurrentModificationException(
                        sphereException,
                        () -> fetchAndUpdate(oldTaxCategory, newTaxCategory),
                        () -> {
                            final String errorMessage =
                                format(TAX_CATEGORY_UPDATE_FAILED, newTaxCategory.getKey(),
                                    sphereException.getMessage());
                            handleError(errorMessage, sphereException);
                            return completedFuture(Optional.empty());
                        });
                } else {
                    statistics.incrementUpdated();
                    return completedFuture(Optional.of(updatedTaxCategory));
                }
            });
    }

    @Nonnull
    private CompletionStage<Optional<TaxCategory>> fetchAndUpdate(
        @Nonnull final TaxCategory oldTaxCategory,
        @Nonnull final TaxCategoryDraft newTaxCategory) {

        final String key = oldTaxCategory.getKey();
        return taxCategoryService
            .fetchTaxCategory(key)
            .handle(ImmutablePair::new)
            .thenCompose(fetchResponse -> {
                final Optional<TaxCategory> fetchedTaxCategoryOptional = fetchResponse.getKey();
                final Throwable exception = fetchResponse.getValue();

                if (exception != null) {
                    final String errorMessage = format(TAX_CATEGORY_UPDATE_FAILED, key,
                        "Failed to fetch from CTP while retrying after concurrency modification.");
                    handleError(errorMessage, exception);
                    return completedFuture(null);
                }

                return fetchedTaxCategoryOptional
                    .map(fetchedTaxCategory -> buildActionsAndUpdate(fetchedTaxCategory, newTaxCategory))
                    .orElseGet(() -> {
                        final String errorMessage = format(TAX_CATEGORY_UPDATE_FAILED, key,
                            "Not found when attempting to fetch while retrying "
                                + "after concurrency modification.");
                        handleError(errorMessage, null);
                        return completedFuture(null);
                    });
            });
    }
}
