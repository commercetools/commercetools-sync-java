package com.commercetools.sync.categories;

import com.commercetools.sync.categories.helpers.CategoryReferenceResolver;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.ResourceIdentifier;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

import static com.commercetools.sync.categories.helpers.CategoryReferenceResolver.getParentCategoryKey;
import static com.commercetools.sync.categories.utils.CategorySyncUtils.buildActions;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.areResourceIdentifiersEqual;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.toResourceIdentifierIfNotNull;
import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class CategorySync extends BaseSync<CategoryDraft, CategorySyncStatistics, CategorySyncOptions> {

    private static final String CATEGORY_DRAFT_KEY_NOT_SET = "CategoryDraft with name: %s doesn't have a key.";
    private static final String CATEGORY_DRAFT_IS_NULL = "CategoryDraft is null.";
    private static final String FAILED_TO_PROCESS  = "Failed to process the CategoryDraft with key:'%s'. Reason: %s";
    private static final String UPDATE_FAILED = "Failed to update Category with key: '%s'. Reason: %s";

    private final CategoryService categoryService;
    private final CategoryReferenceResolver referenceResolver;

    /**
     * The following set ({@code processedCategoryKeys}) is thread-safe because it is accessed/modified in a concurrent
     * context, specifically when updating products in parallel in
     * {@link #updateCategory(Category, CategoryDraft, List)}. It also has a global scope across the entire sync
     * process, which means the same instance is used across all batch executions.
     */
    private final ConcurrentHashMap.KeySetView<String, Boolean> processedCategoryKeys = ConcurrentHashMap.newKeySet();

    /**
     * The following set ({@code categoryKeysWithResolvedParents}) and map ({@code categoryDraftsToUpdate}) are
     * thread-safe because they are accessed/modified in a concurrent context, specifically when updating products in
     * parallel in {@link #updateCategoriesInParallel(Map)}. They have a local scope within every batch execution, which
     * means that they are re-initialized on every {@link #processBatch(List)} call.
     */
    private ConcurrentHashMap.KeySetView<String, Boolean> categoryKeysWithResolvedParents =
        ConcurrentHashMap.newKeySet();
    private ConcurrentHashMap<CategoryDraft, Category> categoryDraftsToUpdate = new ConcurrentHashMap<>();

    /**
     * The following sets ({@code existingCategoryDrafts}, {@code newCategoryDrafts}, {@code referencesResolvedDrafts}
     * and {@code categoryKeysToFetch}) are not thread-safe because they are accessed/modified in a
     * non-concurrent/sequential context. They have a local scope within every batch execution, which
     * means that they are re-initialized on every {@link #processBatch(List)} call.
     */
    private Set<CategoryDraft> existingCategoryDrafts = new HashSet<>();
    private Set<CategoryDraft> newCategoryDrafts = new HashSet<>();
    private Set<CategoryDraft> referencesResolvedDrafts = new HashSet<>();
    private Set<String> categoryKeysToFetch = new HashSet<>();


    /**
     * Takes a {@link CategorySyncOptions} instance to instantiate a new {@link CategorySync} instance that could be
     * used to sync category drafts with the given categories in the CTP project specified in the injected
     * {@link CategorySyncOptions} instance.
     *
     * @param syncOptions the container of all the options of the sync process including the CTP project client and/or
     *                    configuration and other sync-specific options.
     */
    public CategorySync(@Nonnull final CategorySyncOptions syncOptions) {
        this(syncOptions, new TypeServiceImpl(syncOptions), new CategoryServiceImpl(syncOptions));
    }

    /**
     * Takes a {@link CategorySyncOptions}, a {@link TypeService} and {@link CategoryService} instances to instantiate
     * a new {@link CategorySync} instance that could be used to sync categories or category drafts with the given
     * categories in the CTP project specified in the injected {@link CategorySyncOptions} instance.
     *
     * <p>NOTE: This constructor is mainly to be used for tests where the services can be mocked and passed to.
     *
     * @param syncOptions     the container of all the options of the sync process including the CTP project
     *                        client and/or configuration and other sync-specific options.
     * @param typeService     the type service which is responsible for fetching/caching the Types from the CTP project.
     * @param categoryService the category service which is responsible for fetching, creating and updating categories
     *                        from and to the CTP project.
     */
    CategorySync(@Nonnull final CategorySyncOptions syncOptions,
                 @Nonnull final TypeService typeService,
                 @Nonnull final CategoryService categoryService) {
        super(new CategorySyncStatistics(), syncOptions);
        this.categoryService = categoryService;
        this.referenceResolver = new CategoryReferenceResolver(syncOptions, typeService, categoryService);
    }

    /**
     * Given a list of {@code CategoryDraft} that represent a batch of category drafts, this method for the first batch
     * only caches a list of all the categories in the CTP project in a cached map that representing each category's
     * key to the id. It then validates the category drafts, then resolves all the references. Then it creates all
     * categories that need to be created in parallel while keeping track of the categories that have their
     * non-existing parents. Then it does update actions that don't require parent changes in parallel. Then in a
     * blocking fashion issues update actions that don't involve parent changes sequentially.
     *
     * <p>More on the exact implementation of how the sync works here:
     * https://sphere.atlassian.net/wiki/spaces/PS/pages/145193124/Category+Parallelisation+Technical+Concept
     *
     *
     * @param categoryDrafts the list of new category drafts to sync to the CTP project.
     * @return an instance of {@link CompletionStage}&lt;{@link CategorySyncStatistics}&gt; which contains as a result
     *         an instance of {@link CategorySyncStatistics} representing the {@code statistics} instance attribute of
     *         {@code this} {@link CategorySync}.
     */
    @Override
    protected CompletionStage<CategorySyncStatistics> process(@Nonnull final List<CategoryDraft> categoryDrafts) {
        final List<List<CategoryDraft>> batches = batchElements(categoryDrafts, syncOptions.getBatchSize());
        return syncBatches(batches, CompletableFuture.completedFuture(statistics));
    }

    /**
     * Given a list of {@code CategoryDraft} that represent a batch of category drafts, this method for the first batch
     * only caches a mapping of key to the id of <b>all categories</b> in the CTP project. It then validates the
     * category drafts, then resolves all the references. Then it creates all categories that need to be created in
     * parallel while keeping track of the categories that have their non-existing parents. Then it does update actions
     * that don't require parent changes in parallel. Then in a blocking fashion issues update actions that don't
     * involve parent changes sequentially.
     *
     *
     * <p> In case of error during of fetch during the caching of category keys or during of fetching of existing
     * categories, the error callback will be triggered. And the sync process would stop for the given batch.
     * </p>
     *
     *
     * <p>More on the exact implementation of how the sync works here:
     * https://github.com/commercetools/commercetools-sync-java/wiki/Category-Sync-Underlying-Concept
     *
     *
     * @param categoryDrafts the list of new category drafts to sync to the CTP project.
     * @return an instance of {@link CompletionStage}&lt;{@link CategorySyncStatistics}&gt; which contains as a result
     *         an instance of {@link CategorySyncStatistics} representing the {@code statistics} instance attribute of
     *         {@code this} {@link CategorySync}.
     */
    @Override
    protected CompletionStage<CategorySyncStatistics> processBatch(@Nonnull final List<CategoryDraft> categoryDrafts) {
        final int numberOfNewDraftsToProcess = getNumberOfDraftsToProcess(categoryDrafts);
        referencesResolvedDrafts = new HashSet<>();
        existingCategoryDrafts = new HashSet<>();
        newCategoryDrafts = new HashSet<>();

        categoryKeysWithResolvedParents = ConcurrentHashMap.newKeySet();
        categoryDraftsToUpdate = new ConcurrentHashMap<>();

        return categoryService
                .cacheKeysToIds()
                .handle(ImmutablePair::new)
                .thenCompose(cachingResponse -> {

                    final Map<String, String> keyToIdCache = cachingResponse.getKey();
                    final Throwable cachingException = cachingResponse.getValue();

                    if (cachingException != null) {
                        handleError("Failed to build a cache of keys to ids.", cachingException, categoryDrafts.size());
                        return CompletableFuture.completedFuture(null);
                    }

                    prepareDraftsForProcessing(categoryDrafts, keyToIdCache);

                    categoryKeysToFetch = existingCategoryDrafts
                        .stream()
                        .map(CategoryDraft::getKey)
                        .collect(Collectors.toSet());

                    return createAndUpdate(keyToIdCache);
                })
                .thenApply(ignoredResult -> {
                    statistics.incrementProcessed(numberOfNewDraftsToProcess);
                    return statistics;
                });
    }


    private CompletionStage<Void> fetchAndUpdate(@Nonnull final Map<String, String> keyToIdCache) {
        return categoryService
            .fetchMatchingCategoriesByKeys(categoryKeysToFetch)
            .handle(ImmutablePair::new)
            .thenCompose(fetchResponse -> {
                final Set<Category> fetchedCategories = fetchResponse.getKey();
                final Throwable exception = fetchResponse.getValue();

                if (exception != null) {
                    final String errorMessage =
                        format("Failed to fetch existing categories with keys: '%s'.", categoryKeysToFetch);
                    handleError(errorMessage, exception, categoryKeysToFetch.size());
                    return CompletableFuture.completedFuture(null);
                }

                return processFetchedCategoriesAndUpdate(keyToIdCache, fetchedCategories);
            });
    }

    @Nonnull
    private CompletionStage<Void> processFetchedCategoriesAndUpdate(@Nonnull final Map<String, String> keyToIdCache,
                                                                    @Nonnull final Set<Category> fetchedCategories) {

        processFetchedCategories(fetchedCategories, referencesResolvedDrafts, keyToIdCache);
        updateCategoriesSequentially(categoryDraftsToUpdate);
        return updateCategoriesInParallel(categoryDraftsToUpdate);
    }

    /**
     * Given a list of {@code CategoryDraft} elements, this method calculates the number of drafts that need to be
     * processed in this batch, given they weren't processed before, plus the number of null drafts and drafts with null
     * keys. Drafts which were processed before, will have their keys stored in {@code processedCategoryKeys}.
     *
     * @param categoryDrafts the input list of category drafts in the sync batch.
     * @return the number of drafts that are needed to be processed.
     */
    private int getNumberOfDraftsToProcess(@Nonnull final List<CategoryDraft> categoryDrafts) {
        final int numberOfNullCategoryDrafts = categoryDrafts.stream()
                                                             .filter(Objects::isNull)
                                                             .collect(Collectors.toList()).size();
        final int numberOfCategoryDraftsNotProcessedBefore =
            categoryDrafts.stream()
                          .filter(Objects::nonNull)
                          .map(CategoryDraft::getKey)
                          .filter(categoryDraftKey ->
                              categoryDraftKey == null || !processedCategoryKeys.contains(categoryDraftKey))
                          .collect(Collectors.toList()).size();

        return numberOfCategoryDraftsNotProcessedBefore + numberOfNullCategoryDrafts;
    }

    /**
     * This method does the following on each category draft input in the sync batch:
     * <ol>
     *     <li>First checks if the key is set on the draft, if not then the error callback is triggered and the
     *     draft is skipped.</li>
     *     <li>Checks if the draft is {@code null}, then the error callback is triggered and the
     *     draft is skipped.</li>
     *     <li>Then for each draft adds each with a non-existing parent in keyToId cached map to a map
     *     {@code statistics#categoryKeysWithMissingParents} (mapping from parent key to list of subcategory keys)</li>
     *     <li>Then it resolves the references (parent category reference and custom type reference) on each draft. For
     *     each draft with resolved references:
     *      <ol>
     *          <li>Checks if the draft exists, then it adds it to the {@code existingCategoryDrafts} array.</li>
     *          <li>If the draft doesn't exist, then it adds it to the {@code newCategoryDrafts} array.</li>
     *      </ol>
     *      </li>
     *</ol>
     * If reference resolution failed either during getting the parent category key or during actual reference
     * resolution, the error callback is triggered and the category is skipped.
     *
     * @param categoryDrafts the input list of category drafts in the sync batch.
     * @param keyToIdCache   the cache containing the mapping of all existing category keys to ids.
     */
    private void prepareDraftsForProcessing(@Nonnull final List<CategoryDraft> categoryDrafts,
                                            @Nonnull final Map<String, String> keyToIdCache) {
        for (CategoryDraft categoryDraft : categoryDrafts) {
            if (categoryDraft != null) {
                final String categoryKey = categoryDraft.getKey();
                if (isNotBlank(categoryKey)) {
                    try {
                        categoryDraft = updateCategoriesWithMissingParents(categoryDraft, keyToIdCache);
                        referenceResolver.resolveReferences(categoryDraft)
                                         .thenAccept(referencesResolvedDraft -> {
                                             referencesResolvedDrafts.add(referencesResolvedDraft);
                                             if (keyToIdCache.containsKey(categoryKey)) {
                                                 existingCategoryDrafts.add(referencesResolvedDraft);
                                             } else {
                                                 newCategoryDrafts.add(referencesResolvedDraft);
                                             }
                                         })
                                         .exceptionally(completionException -> {
                                             final String errorMessage = format(FAILED_TO_PROCESS, categoryKey,
                                                 completionException.getMessage());
                                             handleError(errorMessage, completionException);
                                             return null;
                                         }).toCompletableFuture().join();
                    } catch (Exception exception) {
                        final String errorMessage = format(FAILED_TO_PROCESS, categoryKey,
                            exception);
                        handleError(errorMessage, exception);
                    }
                } else {
                    final String errorMessage = format(CATEGORY_DRAFT_KEY_NOT_SET, categoryDraft.getName());
                    handleError(errorMessage, null);
                }
            } else {
                handleError(CATEGORY_DRAFT_IS_NULL, null);
            }
        }
    }

    @Nonnull
    private CompletionStage<Void> createAndUpdate(@Nonnull final Map<String, String> keyToIdCache) {
        return createCategories(newCategoryDrafts)
            .thenAccept(this::processCreatedCategories)
            .thenCompose(ignoredResult -> fetchAndUpdate(keyToIdCache));
    }

    @Nonnull
    private CompletionStage<Set<Category>> createCategories(@Nonnull final Set<CategoryDraft> categoryDrafts) {
        return mapValuesToFutureOfCompletedValues(categoryDrafts, this::applyCallbackAndCreate)
            .thenApply(results -> results.filter(Optional::isPresent).map(Optional::get))
            .thenApply(createdCategories -> createdCategories.collect(Collectors.toSet()));
    }

    @Nonnull
    private CompletionStage<Optional<Category>> applyCallbackAndCreate(@Nonnull final CategoryDraft categoryDraft) {
        return syncOptions
            .applyBeforeCreateCallBack(categoryDraft)
            .map(categoryService::createCategory)
            .orElse(CompletableFuture.completedFuture(Optional.empty()));
    }

    /**
     * This method first gets the parent key either from the expanded category object or from the id field on the
     * reference and validates it. If it is valid, then it checks if the parent category is missing, this is done by
     * checking if the key exists in the {@code keyToIdCache} map. If it is missing, then it adds the key to the map
     * {@code statistics#categoryKeysWithMissingParents}, then it returns a category draft identical to the supplied one
     * but with a {@code null} parent. If it is not missing, then the same identical category draft is returned with the
     * same parent.
     *
     * @param categoryDraft the category draft to check whether it's parent is missing or not.
     * @param keyToIdCache  the cache containing the mapping of all existing category keys to ids.
     * @return the same identical supplied category draft. However, with a null parent field, if the parent is missing.
     * @throws ReferenceResolutionException thrown if the parent key is not valid.
     */
    private CategoryDraft updateCategoriesWithMissingParents(@Nonnull final CategoryDraft categoryDraft,
                                                             @Nonnull final Map<String, String> keyToIdCache)
        throws ReferenceResolutionException {
        return getParentCategoryKey(categoryDraft)
            .map(parentCategoryKey -> {
                if (isMissingCategory(parentCategoryKey, keyToIdCache)) {
                    statistics.putMissingParentCategoryChildKey(parentCategoryKey, categoryDraft.getKey());
                    return CategoryDraftBuilder.of(categoryDraft)
                                               .parent((ResourceIdentifier<Category>) null)
                                               .build();
                }
                return categoryDraft;
            }).orElse(categoryDraft);
    }

    /**
     * Checks if the category with the given {@code categoryKey} exists or not, by checking if its key
     * exists in the {@code keyToIdCache} map.
     *
     * @param categoryKey  the key of the category to check for existence.
     * @param keyToIdCache the cache of existing category keys to ids.
     * @return true or false, whether the category exists or not.
     */
    private boolean isMissingCategory(@Nonnull final String categoryKey,
                                      @Nonnull final Map<String, String> keyToIdCache) {
        return !keyToIdCache.containsKey(categoryKey);
    }


    /**
     * This method does the following on each category created from the provided {@link Set} of categories:
     * <ol>
     * <li>Adds its keys to {@code processedCategoryKeys} in order to not increment updated and processed counters of
     * statistics more than needed. For example, when updating the parent later on of this created category.
     * </li>
     *
     * <li>Check if it exists in {@code statistics#categoryKeysWithMissingParents} as a missing parent, if it does then
     * its children are now ready to update their parent field references with the created parent category. For each of
     * these child categories do the following:
     * <ol>
     * <li>Add its key to the list {@code categoryKeysWithResolvedParents}.</li>
     * <li>If the key was in the {@code newCategoryDrafts} list, then it means it should have just been created.
     * Then it adds the category created to the {@code categoryDraftsToUpdate}. The draft is created from the
     * created Category response from CTP but parent is taken from the {@code statistics#categoryKeysWithMissingParents}
     * </li>
     * <li>Otherwise, if it wasn't in the {@code newCategoryDrafts} list, then it means it needs to be
     * fetched. Therefore, its key is added it to the {@code categoryKeysToFetch}</li>
     * </ol>
     * </li>
     * </ol>
     *
     * @param createdCategories the set of created categories that needs to be processed.
     */
    private void processCreatedCategories(@Nonnull final Set<Category> createdCategories) {
        final int numberOfFailedCategories = newCategoryDrafts.size() - createdCategories.size();
        statistics.incrementFailed(numberOfFailedCategories);

        statistics.incrementCreated(createdCategories.size());
        createdCategories.forEach(createdCategory -> {
            final String createdCategoryKey = createdCategory.getKey();
            processedCategoryKeys.add(createdCategoryKey);
            final Set<String> childCategoryKeys = statistics.getCategoryKeysWithMissingParents()
                                                            .get(createdCategoryKey);

            if (childCategoryKeys != null) {
                for (String childCategoryKey : childCategoryKeys) {
                    categoryKeysWithResolvedParents.add(childCategoryKey);
                    final Optional<Category> createdChild = getCategoryByKeyIfExists(createdCategories,
                        childCategoryKey);
                    if (createdChild.isPresent()) {
                        final Category category = createdChild.get();
                        final CategoryDraft categoryDraft =
                            CategoryDraftBuilder.of(category)
                                                .parent(createdCategory.toResourceIdentifier())
                                                .build();
                        categoryDraftsToUpdate.put(categoryDraft, category);
                    } else {
                        categoryKeysToFetch.add(childCategoryKey);
                    }
                }
            }
        });
    }

    /**
     * Given a {@code Set} of categories which have just been fetched, this method does the following on each category:
     * <ol>
     * <li>If the the draft exists in the input list:
     *   <ol>
     *       <li>a copy is created from it.</li>
     *       <li>If the parent reference on the draft is null, It means the parent might not yet be created, therefore
     *       the parent from the fetched category is used. This is to avoid having the error callback being called for
     *       un setting a parent (which is not possible by the CTP API).</li>
     *   </ol>
     * </li>
     * <li>If not, the draft is copied from the fetched category. </li>
     * <li>If the key of this draft exists in the {@code categoryKeysWithResolvedParents}, overwrite the parent with
     * the parent saved in {@code statistics#categoryKeysWithMissingParents} for the draft.</li>
     * <li>After a draft has been created, it is added to {@code categoryDraftsToUpdate} map as a key and
     * the value is the fetched {@link Category}.</li>
     * </ol>
     *
     * @param fetchedCategories        {@code Set} of categories which have just been fetched and
     * @param resolvedReferencesDrafts {@code Set} of CategoryDrafts with resolved references, they are used to get
     *                                 a draft with a resolved reference for the input list of drafts.
     * @param keyToIdCache             the cache containing mapping of all existing category keys to ids.
     */
    private void processFetchedCategories(@Nonnull final Set<Category> fetchedCategories,
                                          @Nonnull final Set<CategoryDraft> resolvedReferencesDrafts,
                                          @Nonnull final Map<String, String> keyToIdCache) {
        fetchedCategories.forEach(fetchedCategory -> {
            final String fetchedCategoryKey = fetchedCategory.getKey();
            final Optional<CategoryDraft> draftByKeyIfExists =
                getDraftByKeyIfExists(resolvedReferencesDrafts, fetchedCategoryKey);
            final CategoryDraftBuilder categoryDraftBuilder =
                draftByKeyIfExists.map(categoryDraft -> {
                    if (categoryDraft.getParent() == null) {
                        return CategoryDraftBuilder.of(categoryDraft)
                                                   .parent(toResourceIdentifierIfNotNull(fetchedCategory.getParent()));
                    }
                    return CategoryDraftBuilder.of(categoryDraft);
                })
                                  .orElseGet(() -> CategoryDraftBuilder.of(fetchedCategory));
            if (categoryKeysWithResolvedParents.contains(fetchedCategoryKey)) {
                statistics.getMissingParentKey(fetchedCategoryKey)
                          .ifPresent(missingParentKey -> {
                              final String parentId = keyToIdCache.get(missingParentKey);
                              categoryDraftBuilder
                                  .parent(Category.referenceOfId(parentId).toResourceIdentifier());
                          });
            }
            categoryDraftsToUpdate.put(categoryDraftBuilder.build(), fetchedCategory);
        });
    }


    /**
     * Given a {@link Set} of categories and a {@code key}, this method tries to find a category with this key in this
     * set and returns an optional containing it or an empty optional if no category exists with such key.
     *
     * @param categories set of categories to look for a category with such key.
     * @param key        the key to look for a category for in the supplied set of categories.
     * @return an optional containing the category or an empty optional if no category exists with such key.
     */
    private static Optional<Category> getCategoryByKeyIfExists(@Nonnull final Set<Category> categories,
                                                               @Nonnull final String key) {
        return categories.stream()
                         .filter(category -> Objects.equals(category.getKey(), key))
                         .findFirst();
    }

    /**
     * Given a {@link Set} of categoryDrafts and a {@code key}. This method tries to find a categoryDraft with this key
     * in this set and returns an optional containing it or an empty optional if no categoryDraft exists with such key.
     *
     * @param categoryDrafts set of categoryDrafts to look for a categoryDraft with such key.
     * @param key            the key to look for a categoryDraft for in the supplied set of categoryDrafts.
     * @return an optional containing the categoryDraft or an empty optional if no category exists with such key.
     */
    private static Optional<CategoryDraft> getDraftByKeyIfExists(@Nonnull final Set<CategoryDraft> categoryDrafts,
                                                                 @Nonnull final String key) {
        return categoryDrafts.stream()
                             .filter(categoryDraft -> Objects.equals(categoryDraft.getKey(), key))
                             .findFirst();
    }

    /**
     * Given a {@link Map} of categoryDrafts to Categories that require syncing, this method filters out the pairs that
     * need a {@link io.sphere.sdk.categories.commands.updateactions.ChangeParent} update action, and in turn performs
     * the sync on them in a sequential/blocking fashion as advised by the CTP documentation:
     * http://dev.commercetools.com/http-api-projects-categories.html#change-parent
     *
     * @param matchingCategories a {@link Map} of categoryDrafts to Categories that require syncing.
     */
    private void updateCategoriesSequentially(@Nonnull final Map<CategoryDraft, Category> matchingCategories) {
        matchingCategories.entrySet().stream()
                          .filter(entry -> requiresChangeParentUpdateAction(entry.getValue(), entry.getKey()))
                          .map(entry -> buildUpdateActionsAndUpdate(entry.getValue(), entry.getKey()))
                          .map(CompletionStage::toCompletableFuture)
                          .forEach(CompletableFuture::join);
    }

    /**
     * Compares the parent references of a {@link Category} and a {@link CategoryDraft} to check whether a
     * {@link io.sphere.sdk.categories.commands.updateactions.ChangeParent} update action is required to sync the
     * draft to the category or not
     *
     * @param category      the old category to sync to.
     * @param categoryDraft the new category draft to sync.
     * @return true or false whether a {@link io.sphere.sdk.categories.commands.updateactions.ChangeParent} is needed to
     *          sync the draft to the category.
     */
    static boolean requiresChangeParentUpdateAction(@Nonnull final Category category,
                                                    @Nonnull final CategoryDraft categoryDraft) {
        return !areResourceIdentifiersEqual(category.getParent(), categoryDraft.getParent());
    }

    /**
     * Given a {@link Map} of categoryDrafts to Categories that require syncing, this method filters out the pairs that
     * don't need a {@link io.sphere.sdk.categories.commands.updateactions.ChangeParent} update action, and in turn
     * performs the sync on them in a parallel/non-blocking fashion.
     *
     * @param matchingCategories a {@link Map} of categoryDrafts to Categories that require syncing.
     */
    private CompletionStage<Void> updateCategoriesInParallel(
        @Nonnull final Map<CategoryDraft, Category> matchingCategories) {

        final List<CompletableFuture<Void>> futures =
            matchingCategories.entrySet().stream()
                              .filter(entry -> !requiresChangeParentUpdateAction(entry.getValue(), entry.getKey()))
                              .map(entry -> buildUpdateActionsAndUpdate(entry.getValue(), entry.getKey()))
                              .map(CompletionStage::toCompletableFuture)
                              .collect(Collectors.toList());
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    }

    /**
     * Given an existing {@link Category} and a new {@link CategoryDraft}, first resolves all references on the category
     * draft, then it calculates all the update actions required to synchronize the existing category to be the same as
     * the new one. Then the method applies the {@code ProductSyncOptions#beforeUpdateCallback} on the resultant list
     * of actions.
     *
     * <p>If there are update actions in the resulting list, a request is made to CTP to update the existing category,
     * otherwise it doesn't issue a request.
     *
     * @param oldCategory the category which could be updated.
     * @param newCategory the category draft where we get the new data.
     * @return a future which contains an empty result after execution of the update.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<Void> buildUpdateActionsAndUpdate(@Nonnull final Category oldCategory,
                                                              @Nonnull final CategoryDraft newCategory) {

        final List<UpdateAction<Category>> updateActions = buildActions(oldCategory, newCategory, syncOptions);
        final List<UpdateAction<Category>> beforeUpdateCallBackApplied =
            syncOptions.applyBeforeUpdateCallBack(updateActions, newCategory, oldCategory);

        if (!beforeUpdateCallBackApplied.isEmpty()) {
            return updateCategory(oldCategory, newCategory, beforeUpdateCallBackApplied);
        }

        return CompletableFuture.completedFuture(null);
    }


    /**
     * Given a {@link Category} and a {@link List} of {@link UpdateAction} elements, this method issues a request to
     * the CTP project defined by the client configuration stored in the {@code syncOptions} instance
     * of this class to update the specified category with this list of update actions. If the update request failed
     * due to a {@link ConcurrentModificationException}, the method recalculates the update actions required for
     * syncing the {@link Category} and reissues the update request.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP,
     * the optional error callback specified in the {@code syncOptions} is called.
     *
     * @param category      the category to update.
     * @param updateActions the list of update actions to update the category with.
     * @return a future which contains an empty result after execution of the update.
     */
    private CompletionStage<Void> updateCategory(@Nonnull final Category category,
                                                 @Nonnull final CategoryDraft newCategory,
                                                 @Nonnull final List<UpdateAction<Category>> updateActions) {
        final String categoryKey = category.getKey();
        return categoryService.updateCategory(category, updateActions)
                              .handle((updatedCategory, sphereException) -> sphereException)
                              .thenCompose(sphereException -> {
                                  if (sphereException != null) {
                                      return executeSupplierIfConcurrentModificationException(
                                          sphereException,
                                          () -> refetchAndUpdate(category, newCategory),
                                          () -> {
                                              if (!processedCategoryKeys.contains(categoryKey)) {
                                                  handleError(format(UPDATE_FAILED, categoryKey, sphereException),
                                                      sphereException);
                                                  processedCategoryKeys.add(categoryKey);
                                              }
                                              return CompletableFuture.completedFuture(null);
                                          });
                                  } else {
                                      if (!processedCategoryKeys.contains(categoryKey)) {
                                          statistics.incrementUpdated();
                                          processedCategoryKeys.add(categoryKey);
                                      }
                                      if (categoryKeysWithResolvedParents.contains(categoryKey)) {
                                          statistics.removeChildCategoryKeyFromMissingParentsMap(categoryKey);
                                      }
                                      return CompletableFuture.completedFuture(null);
                                  }
                              });
    }

    private CompletionStage<Void> refetchAndUpdate(@Nonnull final Category oldCategory,
                                                   @Nonnull final CategoryDraft newCategory) {

        final String key = oldCategory.getKey();
        return categoryService
                .fetchCategory(key)
                .handle(ImmutablePair::new)
                .thenCompose(fetchResponse -> {
                    final Optional<Category> fetchedCategoryOptional = fetchResponse.getKey();
                    final Throwable exception = fetchResponse.getValue();

                    if (exception != null) {
                        final String errorMessage = format(UPDATE_FAILED, key, "Failed to fetch from CTP while "
                                + "retrying after concurrency modification.");
                        handleError(errorMessage, exception);
                        return CompletableFuture.completedFuture(null);
                    }

                    return fetchedCategoryOptional
                        .map(fetchedCategory -> buildUpdateActionsAndUpdate(fetchedCategory, newCategory))
                        .orElseGet(() -> {
                            final String errorMessage =
                                format(UPDATE_FAILED, key, "Not found when attempting to fetch while retrying "
                                    + "after concurrency modification.");
                            handleError(errorMessage, null);
                            return CompletableFuture.completedFuture(null);
                        });
                });
    }

    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed categories to sync.
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
     * incrementing the total number of failed category to sync with the supplied {@code failedTimes}.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     * @param exception    The exception that called caused the failure, if any.
     * @param failedTimes  The number of times that the failed category counter is incremented.
     */
    private void handleError(@Nonnull final String errorMessage,
                             @Nullable final Throwable exception,
                             final int failedTimes) {

        syncOptions.applyErrorCallback(errorMessage, exception);
        statistics.incrementFailed(failedTimes);
    }
}
