package com.commercetools.sync.categories;

import static com.commercetools.sync.categories.helpers.CategoryReferenceResolver.getParentCategoryKey;
import static com.commercetools.sync.categories.utils.CategorySyncUtils.buildActions;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.areResourceIdentifiersEqual;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.toResourceIdentifierIfNotNull;
import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_CATEGORY_CONTAINER_KEY;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toSet;

import com.commercetools.sync.categories.helpers.CategoryBatchValidator;
import com.commercetools.sync.categories.helpers.CategoryReferenceResolver;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.models.WaitingToBeResolvedCategories;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.UnresolvedReferencesService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.commands.UpdateAction;
import java.util.ArrayList;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class CategorySync
    extends BaseSync<CategoryDraft, CategorySyncStatistics, CategorySyncOptions> {

  private static final String FAILED_TO_PROCESS =
      "Failed to process the CategoryDraft with key: '%s'. Reason: %s";
  private static final String UPDATE_FAILED =
      "Failed to update Category with key: '%s'. Reason: %s";
  private static final String FAILED_TO_FETCH_WAITING_DRAFTS =
      "Failed to fetch CategoryDraft waiting to be resolved with parent keys: '%s'.";
  private final CategoryService categoryService;
  private final UnresolvedReferencesService<WaitingToBeResolvedCategories>
      unresolvedReferencesService;
  private final CategoryReferenceResolver referenceResolver;
  private final CategoryBatchValidator batchValidator;

  /**
   * The following set ({@code processedCategoryKeys}) is thread-safe because it is
   * accessed/modified in a concurrent context, specifically when updating products in parallel in
   * {@link #updateCategory(Category, CategoryDraft, List)}. It also has a global scope across the
   * entire sync process, which means the same instance is used across all batch executions.
   */
  private final ConcurrentHashMap.KeySetView<String, Boolean> processedCategoryKeys =
      ConcurrentHashMap.newKeySet();

  /**
   * The following map ({@code categoryDraftsToUpdate}) are thread-safe because they are
   * accessed/modified in a concurrent context, specifically when updating products in parallel in
   * {@link #updateCategoriesInParallel(Map)}. They have a local scope within every batch execution,
   * which means that they are re-initialized on every {@link #processBatch(List)} call.
   */
  private ConcurrentHashMap<CategoryDraft, Category> categoryDraftsToUpdate =
      new ConcurrentHashMap<>();

  /**
   * The following set {@code referencesResolvedDrafts} is not thread-safe because they are
   * accessed/modified in a non-concurrent/sequential context. They have a local scope within every
   * batch execution, which means that they are re-initialized on every {@link #processBatch(List)}
   * call.
   */
  private final Set<CategoryDraft> referencesResolvedDrafts = new HashSet<>();

  /**
   * Takes a {@link CategorySyncOptions} instance to instantiate a new {@link CategorySync} instance
   * that could be used to sync category drafts with the given categories in the CTP project
   * specified in the injected {@link CategorySyncOptions} instance.
   *
   * @param syncOptions the container of all the options of the sync process including the CTP
   *     project client and/or configuration and other sync-specific options.
   */
  public CategorySync(@Nonnull final CategorySyncOptions syncOptions) {
    this(
        syncOptions,
        new TypeServiceImpl(syncOptions),
        new CategoryServiceImpl(syncOptions),
        new UnresolvedReferencesServiceImpl<>(syncOptions));
  }

  /**
   * Takes a {@link CategorySyncOptions}, a {@link TypeService} and {@link CategoryService}
   * instances to instantiate a new {@link CategorySync} instance that could be used to sync
   * categories or category drafts with the given categories in the CTP project specified in the
   * injected {@link CategorySyncOptions} instance.
   *
   * <p>NOTE: This constructor is mainly to be used for tests where the services can be mocked and
   * passed to.
   *
   * @param syncOptions the container of all the options of the sync process including the CTP
   *     project client and/or configuration and other sync-specific options.
   * @param typeService the type service which is responsible for fetching/caching the Types from
   *     the CTP project.
   * @param categoryService the category service which is responsible for fetching, creating and
   *     updating categories from and to the CTP project.
   */
  CategorySync(
      @Nonnull final CategorySyncOptions syncOptions,
      @Nonnull final TypeService typeService,
      @Nonnull final CategoryService categoryService,
      @Nonnull
          final UnresolvedReferencesService<WaitingToBeResolvedCategories>
              unresolvedReferencesService) {
    super(new CategorySyncStatistics(), syncOptions);
    this.categoryService = categoryService;
    this.unresolvedReferencesService = unresolvedReferencesService;
    this.referenceResolver =
        new CategoryReferenceResolver(getSyncOptions(), typeService, categoryService);
    this.batchValidator = new CategoryBatchValidator(getSyncOptions(), getStatistics());
  }

  /**
   * Given a list of {@code CategoryDraft} that represent a batch of category drafts, this method
   * for the first batch only caches a list of all the categories in the CTP project in a cached map
   * that representing each category's key to the id. It then validates the category drafts, then
   * resolves all the references. Then it creates all categories that need to be created in parallel
   * while keeping track of the categories that have their non-existing parents. Then it does update
   * actions that don't require parent changes in parallel. Then in a blocking fashion issues update
   * actions that don't involve parent changes sequentially.
   *
   * <p>More on the exact implementation of how the sync works here:
   * https://sphere.atlassian.net/wiki/spaces/PS/pages/145193124/Category+Parallelisation+Technical+Concept
   *
   * @param categoryDrafts the list of new category drafts to sync to the CTP project.
   * @return an instance of {@link CompletionStage}&lt;{@link CategorySyncStatistics}&gt; which
   *     contains as a result an instance of {@link CategorySyncStatistics} representing the {@code
   *     statistics} instance attribute of {@code this} {@link CategorySync}.
   */
  @Override
  protected CompletionStage<CategorySyncStatistics> process(
      @Nonnull final List<CategoryDraft> categoryDrafts) {
    final List<List<CategoryDraft>> batches =
        batchElements(categoryDrafts, syncOptions.getBatchSize());
    return syncBatches(batches, completedFuture(statistics));
  }

  /**
   * Given a list of {@code CategoryDraft} that represent a batch of category drafts, this method
   * for the first batch only caches a mapping of key to the id of <b>all categories</b> in the CTP
   * project. It then validates the category drafts, then resolves all the references. Then it
   * creates all categories that need to be created in parallel while keeping track of the
   * categories that have their non-existing parents. Then it does update actions that don't require
   * parent changes in parallel. Then in a blocking fashion issues update actions that don't involve
   * parent changes sequentially.
   *
   * <p>In case of error during of fetch during the caching of category keys or during of fetching
   * of existing categories, the error callback will be triggered. And the sync process would stop
   * for the given batch.
   *
   * <p>More on the exact implementation of how the sync works here:
   * https://github.com/commercetools/commercetools-sync-java/wiki/Category-Sync-Underlying-Concept
   *
   * @param categoryDrafts the list of new category drafts to sync to the CTP project.
   * @return an instance of {@link CompletionStage}&lt;{@link CategorySyncStatistics}&gt; which
   *     contains as a result an instance of {@link CategorySyncStatistics} representing the {@code
   *     statistics} instance attribute of {@code this} {@link CategorySync}.
   */
  @Override
  protected CompletionStage<CategorySyncStatistics> processBatch(
      @Nonnull final List<CategoryDraft> categoryDrafts) {

    categoryDraftsToUpdate = new ConcurrentHashMap<>();
    final int numberOfNewDraftsToProcess = getNumberOfDraftsToProcess(categoryDrafts);
    final ImmutablePair<Set<CategoryDraft>, CategoryBatchValidator.ReferencedKeys> result =
        batchValidator.validateAndCollectReferencedKeys(categoryDrafts);

    final Set<CategoryDraft> validDrafts = result.getLeft();
    if (validDrafts.isEmpty()) {
      statistics.incrementProcessed(categoryDrafts.size());
      return completedFuture(statistics);
    }

    return referenceResolver
        .populateKeyToIdCachesForReferencedKeys(result.getRight())
        .handle(ImmutablePair::new)
        .thenCompose(
            cachingResponse -> {
              final Throwable cachingException = cachingResponse.getValue();
              if (cachingException != null) {
                handleError(
                    new SyncException("Failed to build a cache of keys to ids.", cachingException),
                    validDrafts.size());
                return completedFuture(null);
              }

              final Map<String, String> categoryKeyToIdCache = cachingResponse.getKey();
              return createAndUpdate(
                  prepareDraftsForProcessing(new ArrayList<>(validDrafts), categoryKeyToIdCache),
                  categoryKeyToIdCache);
            })
        .thenApply(
            ignoredResult -> {
              statistics.incrementProcessed(numberOfNewDraftsToProcess);
              return statistics;
            });
  }

  /**
   * Given a list of {@code CategoryDraft} elements, this method calculates the number of drafts
   * that need to be processed in this batch, given they weren't processed before, plus the number
   * of null drafts and drafts with null keys. Drafts which were processed before, will have their
   * keys stored in {@code processedCategoryKeys}.
   *
   * @param categoryDrafts the input list of category drafts in the sync batch.
   * @return the number of drafts that are needed to be processed.
   */
  private int getNumberOfDraftsToProcess(@Nonnull final List<CategoryDraft> categoryDrafts) {
    final long numberOfNullCategoryDrafts = categoryDrafts.stream().filter(Objects::isNull).count();

    final long numberOfCategoryDraftsNotProcessedBefore =
        categoryDrafts.stream()
            .filter(Objects::nonNull)
            .map(CategoryDraft::getKey)
            .filter(
                categoryDraftKey ->
                    categoryDraftKey == null || !processedCategoryKeys.contains(categoryDraftKey))
            .count();

    return (int) (numberOfCategoryDraftsNotProcessedBefore + numberOfNullCategoryDrafts);
  }

  private CompletionStage<Void> fetchAndUpdate(
      @Nonnull final Set<CategoryDraft> existingCategories,
      @Nonnull final Map<String, String> keyToIdCache) {

    Set<String> categoryKeysToFetch =
        existingCategories.stream().map(CategoryDraft::getKey).collect(toSet());
    return categoryService
        .fetchMatchingCategoriesByKeys(categoryKeysToFetch)
        .handle(ImmutablePair::new)
        .thenCompose(
            fetchResponse -> {
              final Set<Category> fetchedCategories = fetchResponse.getKey();
              final Throwable exception = fetchResponse.getValue();

              if (exception != null) {
                final String errorMessage =
                    format(
                        "Failed to fetch existing categories with keys: '%s'.",
                        categoryKeysToFetch);
                handleError(new SyncException(errorMessage, exception), categoryKeysToFetch.size());
                return completedFuture(null);
              }

              return processFetchedCategoriesAndUpdate(keyToIdCache, fetchedCategories);
            });
  }

  @Nonnull
  private CompletionStage<Void> processFetchedCategoriesAndUpdate(
      @Nonnull final Map<String, String> keyToIdCache,
      @Nonnull final Set<Category> fetchedCategories) {

    processFetchedCategories(fetchedCategories, referencesResolvedDrafts, keyToIdCache);
    updateCategoriesSequentially(categoryDraftsToUpdate);
    return updateCategoriesInParallel(categoryDraftsToUpdate);
  }

  /**
   * This method does the following on each category draft input in the sync batch:
   *
   * <ol>
   *   <li>First it checks if the category, have a resolvable parent category. If not, the category
   *       is saved in a customobject and its key is added to the map {@code
   *       statistics#categoryKeysWithMissingParents} (mapping from parent key to list of
   *       subcategory keys) and it is skipped in the further process.
   *   <li>Then it resolves the references (parent category reference and custom type reference) on
   *       each remaining draft. For each draft with resolved references:
   *       <ol>
   *         <li>Checks if the draft exists, then it adds it to the {@code existingCategoryDrafts}
   *             array.
   *         <li>If the draft doesn't exist, then it adds it to the {@code newCategoryDrafts} array.
   *       </ol>
   * </ol>
   *
   * If reference resolution failed either during getting the parent category key or during actual
   * reference resolution, the error callback is triggered and the category is skipped.
   *
   * @param categoryDrafts the input list of category drafts in the sync batch.
   * @param keyToIdCache the cache containing the mapping of all existing category keys to ids.
   */
  private ImmutablePair<Set<CategoryDraft>, Set<CategoryDraft>> prepareDraftsForProcessing(
      @Nonnull final List<CategoryDraft> categoryDrafts,
      @Nonnull final Map<String, String> keyToIdCache) {

    final Set<CategoryDraft> existingCategoryDrafts = new HashSet<>();
    final Set<CategoryDraft> newCategoryDrafts = new HashSet<>();
    for (CategoryDraft draft : categoryDrafts) {
      final String categoryKey = draft.getKey();
      try {
        checkParentCategoriesAndKeepTrack(draft, keyToIdCache)
            .ifPresent(
                categoryDraft -> {
                  referenceResolver
                      .resolveReferences(categoryDraft)
                      .thenAccept(
                          referencesResolvedDraft -> {
                            referencesResolvedDrafts.add(referencesResolvedDraft);
                            if (keyToIdCache.containsKey(categoryKey)) {
                              existingCategoryDrafts.add(referencesResolvedDraft);
                            } else {
                              newCategoryDrafts.add(referencesResolvedDraft);
                            }
                          })
                      .exceptionally(
                          completionException -> {
                            final String errorMessage =
                                format(
                                    FAILED_TO_PROCESS,
                                    categoryKey,
                                    completionException.getMessage());
                            handleError(errorMessage, completionException);
                            return null;
                          })
                      .toCompletableFuture()
                      .join();
                });
      } catch (Exception exception) {
        final String errorMessage = format(FAILED_TO_PROCESS, categoryKey, exception);
        handleError(errorMessage, exception);
      }
    }
    return ImmutablePair.of(newCategoryDrafts, existingCategoryDrafts);
  }

  @Nonnull
  private CompletionStage<Void> createAndUpdate(
      ImmutablePair<Set<CategoryDraft>, Set<CategoryDraft>> preparedDraftsForProcessing,
      @Nonnull final Map<String, String> keyToIdCache) {
    return createCategories(preparedDraftsForProcessing.getLeft())
        .thenAccept(createdCategories -> processCreatedCategories(createdCategories, keyToIdCache))
        .thenCompose(
            ignoredResult -> fetchAndUpdate(preparedDraftsForProcessing.getRight(), keyToIdCache));
  }

  @Nonnull
  private CompletionStage<Set<Category>> createCategories(
      @Nonnull final Set<CategoryDraft> categoryDrafts) {
    return mapValuesToFutureOfCompletedValues(categoryDrafts, this::applyCallbackAndCreate)
        .thenApply(results -> results.filter(Optional::isPresent).map(Optional::get))
        .thenApply(
            result -> {
              Set<Category> createdCategories = result.collect(toSet());
              statistics.incrementCreated(createdCategories.size());

              final int numberOfFailedCategories = categoryDrafts.size() - createdCategories.size();
              if (numberOfFailedCategories > 0) {
                statistics.incrementFailed(numberOfFailedCategories);
              }
              return createdCategories;
            });
  }

  @Nonnull
  private CompletionStage<Optional<Category>> applyCallbackAndCreate(
      @Nonnull final CategoryDraft categoryDraft) {
    return syncOptions
        .applyBeforeCreateCallback(categoryDraft)
        .map(categoryService::createCategory)
        .orElse(completedFuture(Optional.empty()));
  }

  /**
   * This method first gets the parent key either from the expanded category object or from the id
   * field on the reference and validates it. If it is valid, then it checks if the parent category
   * is missing, this is done by checking if the key exists in the {@code keyToIdCache} map. If it
   * is missing, the category is added to a customobject,its key is added to the map {@code
   * statistics#categoryKeysWithMissingParents} and it is skipped for the futher
   * processing.Otherwise an optinal of the given category draft is returned,
   *
   * @param categoryDraft the category draft to check whether it's parent is missing or not.
   * @param keyToIdCache the cache containing the mapping of all existing category keys to ids.
   * @return Optional of the categorydraft, if its parent exists, otherwise an Optional.empty
   * @throws ReferenceResolutionException thrown if the parent key is not valid.
   */
  @Nonnull
  private Optional<CategoryDraft> checkParentCategoriesAndKeepTrack(
      @Nonnull final CategoryDraft categoryDraft, @Nonnull final Map<String, String> keyToIdCache)
      throws ReferenceResolutionException {
    String parentCategoryKey = getParentCategoryKey(categoryDraft).orElse("");
    if (StringUtils.isBlank(parentCategoryKey)
        || !isMissingCategory(parentCategoryKey, keyToIdCache)) {
      return Optional.of(categoryDraft);
    }

    unresolvedReferencesService
        .save(
            new WaitingToBeResolvedCategories(
                categoryDraft, Collections.singleton(parentCategoryKey)),
            CUSTOM_OBJECT_CATEGORY_CONTAINER_KEY,
            WaitingToBeResolvedCategories.class)
        .toCompletableFuture()
        .join();

    statistics.putMissingParentCategoryChildKey(parentCategoryKey, categoryDraft.getKey());
    return Optional.empty();
  }

  /**
   * Checks if the category with the given {@code categoryKey} exists or not, by checking if its key
   * exists in the {@code keyToIdCache} map.
   *
   * @param categoryKey the key of the category to check for existence.
   * @param keyToIdCache the cache of existing category keys to ids.
   * @return true or false, whether the category exists or not.
   */
  private boolean isMissingCategory(
      @Nonnull final String categoryKey, @Nonnull final Map<String, String> keyToIdCache) {
    return !keyToIdCache.containsKey(categoryKey);
  }

  /**
   * This method does the following on each category created from the provided {@link Set} of
   * categories:
   *
   * <ol>
   *   <li>It fetches all unresolvable categories, whose parent is one of the created categories,
   *       from the custom objects.
   *   <li>Then it prepares the fetched categories for syncing by resolving references.
   *   <li>Then it updates/creates the fetched categories and deletes the corresponding
   *       customobjects.
   * </ol>
   *
   * * @param keyToIdCache the cache containing mapping of all existing category keys to ids.
   *
   * @param createdCategories the set of created categories that needs to be processed.
   */
  private void processCreatedCategories(
      @Nonnull final Set<Category> createdCategories,
      @Nonnull final Map<String, String> keyToIdCache) {
    if (!createdCategories.isEmpty()) {
      final Set<String> resolvedParentKeys =
          createdCategories.stream().map(c -> c.getKey()).collect(toSet());

      fetchResolvableCategories(resolvedParentKeys)
          .thenAccept(
              readyToSync -> {
                if (!readyToSync.isEmpty()) {
                  // process ready drafts
                  ImmutablePair<Set<CategoryDraft>, Set<CategoryDraft>> preparedDraft =
                      prepareDraftsForProcessing(readyToSync, keyToIdCache);
                  createAndUpdate(preparedDraft, keyToIdCache).toCompletableFuture().join();
                  removeFromWaiting(
                      readyToSync.stream()
                          .filter(c -> keyToIdCache.containsKey(c.getKey()))
                          .collect(Collectors.toList()));
                }
              })
          .toCompletableFuture()
          .join();
    }
  }

  @Nonnull
  private CompletionStage<List<CategoryDraft>> fetchResolvableCategories(
      Set<String> resolvedParent) {
    final List<CategoryDraft> readyToSync = new ArrayList<>();
    final Set<String> resolvableCategoryKeys =
        resolvedParent.stream()
            .map(statistics::removeAndGetChildrenKeys)
            .filter(Objects::nonNull)
            .flatMap(Set::stream)
            .collect(toSet());
    if (resolvableCategoryKeys.isEmpty()) {
      return completedFuture(Collections.emptyList());
    }
    return unresolvedReferencesService
        .fetch(
            resolvableCategoryKeys,
            CUSTOM_OBJECT_CATEGORY_CONTAINER_KEY,
            WaitingToBeResolvedCategories.class)
        .handle(ImmutablePair::new)
        .thenApply(
            fetchResponse -> {
              final Throwable fetchException = fetchResponse.getValue();
              if (fetchException != null) {
                final String errorMessage =
                    format(
                        FAILED_TO_FETCH_WAITING_DRAFTS,
                        String.join(",", resolvableCategoryKeys.toString()));
                handleError(new SyncException(errorMessage, fetchException), 1);
                return readyToSync;
              }
              // Each waitingdraft have only one parents, so we can sync the waitingdraft right
              // away, because only waitingdraft with resolved categories was fetched
              final Set<WaitingToBeResolvedCategories> waitingDrafts = fetchResponse.getKey();
              waitingDrafts.forEach(
                  draft -> {
                    final CategoryDraft categoryDraft = draft.getCategoryDraft();
                    readyToSync.add(categoryDraft);
                  });
              return readyToSync;
            });
  }

  private void removeFromWaiting(@Nonnull final List<CategoryDraft> drafts) {
    allOf(
            drafts.stream()
                .map(CategoryDraft::getKey)
                .map(
                    key ->
                        unresolvedReferencesService.delete(
                            key,
                            CUSTOM_OBJECT_CATEGORY_CONTAINER_KEY,
                            WaitingToBeResolvedCategories.class))
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .toCompletableFuture()
        .join();
  }

  /**
   * Given a {@code Set} of categories which have just been fetched, this method does the following
   * on each category:
   *
   * <ol>
   *   <li>If the the draft exists in the input list:
   *       <ol>
   *         <li>a copy is created from it.
   *         <li>If the parent reference on the draft is null, It means the parent might not yet be
   *             created, therefore the parent from the fetched category is used. This is to avoid
   *             having the error callback being called for un setting a parent (which is not
   *             possible by the CTP API).
   *       </ol>
   *   <li>If not, the draft is copied from the fetched category.
   *   <li>If the key of this draft exists in the {@code categoryKeysWithResolvedParents}, overwrite
   *       the parent with the parent saved in {@code statistics#categoryKeysWithMissingParents} for
   *       the draft.
   *   <li>After a draft has been created, it is added to {@code categoryDraftsToUpdate} map as a
   *       key and the value is the fetched {@link Category}.
   * </ol>
   *
   * @param fetchedCategories {@code Set} of categories which have just been fetched and
   * @param resolvedReferencesDrafts {@code Set} of CategoryDrafts with resolved references, they
   *     are used to get a draft with a resolved reference for the input list of drafts.
   * @param keyToIdCache the cache containing mapping of all existing category keys to ids.
   */
  private void processFetchedCategories(
      @Nonnull final Set<Category> fetchedCategories,
      @Nonnull final Set<CategoryDraft> resolvedReferencesDrafts,
      @Nonnull final Map<String, String> keyToIdCache) {
    fetchedCategories.forEach(
        fetchedCategory -> {
          final String fetchedCategoryKey = fetchedCategory.getKey();
          final Optional<CategoryDraft> draftByKeyIfExists =
              getDraftByKeyIfExists(resolvedReferencesDrafts, fetchedCategoryKey);
          final CategoryDraftBuilder categoryDraftBuilder =
              draftByKeyIfExists
                  .map(
                      categoryDraft -> {
                        if (categoryDraft.getParent() == null) {
                          return CategoryDraftBuilder.of(categoryDraft)
                              .parent(toResourceIdentifierIfNotNull(fetchedCategory.getParent()));
                        }
                        return CategoryDraftBuilder.of(categoryDraft);
                      })
                  .orElseGet(() -> CategoryDraftBuilder.of(fetchedCategory));

          categoryDraftsToUpdate.put(categoryDraftBuilder.build(), fetchedCategory);
        });
  }

  /**
   * Given a {@link Set} of categoryDrafts and a {@code key}. This method tries to find a
   * categoryDraft with this key in this set and returns an optional containing it or an empty
   * optional if no categoryDraft exists with such key.
   *
   * @param categoryDrafts set of categoryDrafts to look for a categoryDraft with such key.
   * @param key the key to look for a categoryDraft for in the supplied set of categoryDrafts.
   * @return an optional containing the categoryDraft or an empty optional if no category exists
   *     with such key.
   */
  private static Optional<CategoryDraft> getDraftByKeyIfExists(
      @Nonnull final Set<CategoryDraft> categoryDrafts, @Nonnull final String key) {
    return categoryDrafts.stream()
        .filter(categoryDraft -> Objects.equals(categoryDraft.getKey(), key))
        .findFirst();
  }

  /**
   * Given a {@link Map} of categoryDrafts to Categories that require syncing, this method filters
   * out the pairs that need a {@link io.sphere.sdk.categories.commands.updateactions.ChangeParent}
   * update action, and in turn performs the sync on them in a sequential/blocking fashion as
   * advised by the CTP documentation:
   * http://dev.commercetools.com/http-api-projects-categories.html#change-parent
   *
   * @param matchingCategories a {@link Map} of categoryDrafts to Categories that require syncing.
   */
  private void updateCategoriesSequentially(
      @Nonnull final Map<CategoryDraft, Category> matchingCategories) {
    matchingCategories.entrySet().stream()
        .filter(entry -> requiresChangeParentUpdateAction(entry.getValue(), entry.getKey()))
        .map(entry -> buildUpdateActionsAndUpdate(entry.getValue(), entry.getKey()))
        .map(CompletionStage::toCompletableFuture)
        .forEach(CompletableFuture::join);
  }

  /**
   * Compares the parent references of a {@link Category} and a {@link CategoryDraft} to check
   * whether a {@link io.sphere.sdk.categories.commands.updateactions.ChangeParent} update action is
   * required to sync the draft to the category or not
   *
   * @param category the old category to sync to.
   * @param categoryDraft the new category draft to sync.
   * @return true or false whether a {@link
   *     io.sphere.sdk.categories.commands.updateactions.ChangeParent} is needed to sync the draft
   *     to the category.
   */
  static boolean requiresChangeParentUpdateAction(
      @Nonnull final Category category, @Nonnull final CategoryDraft categoryDraft) {
    return !areResourceIdentifiersEqual(category.getParent(), categoryDraft.getParent());
  }

  /**
   * Given a {@link Map} of categoryDrafts to Categories that require syncing, this method filters
   * out the pairs that don't need a {@link
   * io.sphere.sdk.categories.commands.updateactions.ChangeParent} update action, and in turn
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
   * Given an existing {@link Category} and a new {@link CategoryDraft}, first resolves all
   * references on the category draft, then it calculates all the update actions required to
   * synchronize the existing category to be the same as the new one. Then the method applies the
   * {@code ProductSyncOptions#beforeUpdateCallback} on the resultant list of actions.
   *
   * <p>If there are update actions in the resulting list, a request is made to CTP to update the
   * existing category, otherwise it doesn't issue a request.
   *
   * @param oldCategory the category which could be updated.
   * @param newCategory the category draft where we get the new data.
   * @return a future which contains an empty result after execution of the update.
   */
  private CompletionStage<Void> buildUpdateActionsAndUpdate(
      @Nonnull final Category oldCategory, @Nonnull final CategoryDraft newCategory) {

    final List<UpdateAction<Category>> updateActions =
        buildActions(oldCategory, newCategory, syncOptions);
    final List<UpdateAction<Category>> beforeUpdateCallBackApplied =
        syncOptions.applyBeforeUpdateCallback(updateActions, newCategory, oldCategory);

    if (!beforeUpdateCallBackApplied.isEmpty()) {
      return updateCategory(oldCategory, newCategory, beforeUpdateCallBackApplied);
    }

    return completedFuture(null);
  }

  /**
   * Given a {@link Category} and a {@link List} of {@link UpdateAction} elements, this method
   * issues a request to the CTP project defined by the client configuration stored in the {@code
   * syncOptions} instance of this class to update the specified category with this list of update
   * actions. If the update request failed due to a {@link ConcurrentModificationException}, the
   * method recalculates the update actions required for syncing the {@link Category} and reissues
   * the update request.
   *
   * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was
   * carried out successfully or not. If an exception was thrown on executing the request to CTP,
   * the optional error callback specified in the {@code syncOptions} is called.
   *
   * @param oldCategory the category to update.
   * @param newCategory the category draft where we get the new data.
   * @param updateActions the list of update actions to update the category with.
   * @return a future which contains an empty result after execution of the update.
   */
  private CompletionStage<Void> updateCategory(
      @Nonnull final Category oldCategory,
      @Nonnull final CategoryDraft newCategory,
      @Nonnull final List<UpdateAction<Category>> updateActions) {
    final String categoryKey = oldCategory.getKey();
    return categoryService
        .updateCategory(oldCategory, updateActions)
        .handle((updatedCategory, sphereException) -> sphereException)
        .thenCompose(
            sphereException -> {
              if (sphereException != null) {
                return executeSupplierIfConcurrentModificationException(
                    sphereException,
                    () -> refetchAndUpdate(oldCategory, newCategory),
                    () -> {
                      if (!processedCategoryKeys.contains(categoryKey)) {
                        handleError(
                            format(UPDATE_FAILED, categoryKey, sphereException),
                            sphereException,
                            oldCategory,
                            newCategory,
                            updateActions);
                        processedCategoryKeys.add(categoryKey);
                      }
                      return completedFuture(null);
                    });
              } else {
                if (!processedCategoryKeys.contains(categoryKey)) {
                  statistics.incrementUpdated();
                  processedCategoryKeys.add(categoryKey);
                }
                return completedFuture(null);
              }
            });
  }

  private CompletionStage<Void> refetchAndUpdate(
      @Nonnull final Category oldCategory, @Nonnull final CategoryDraft newCategory) {

    final String key = oldCategory.getKey();
    return categoryService
        .fetchCategory(key)
        .handle(ImmutablePair::new)
        .thenCompose(
            fetchResponse -> {
              final Optional<Category> fetchedCategoryOptional = fetchResponse.getKey();
              final Throwable exception = fetchResponse.getValue();

              if (exception != null) {
                final String errorMessage =
                    format(
                        UPDATE_FAILED,
                        key,
                        "Failed to fetch from CTP while "
                            + "retrying after concurrency modification.");
                handleError(errorMessage, exception, oldCategory, newCategory, null);
                return completedFuture(null);
              }

              return fetchedCategoryOptional
                  .map(fetchedCategory -> buildUpdateActionsAndUpdate(fetchedCategory, newCategory))
                  .orElseGet(
                      () -> {
                        final String errorMessage =
                            format(
                                UPDATE_FAILED,
                                key,
                                "Not found when attempting to fetch while retrying "
                                    + "after concurrency modification.");
                        handleError(errorMessage, null, oldCategory, newCategory, null);
                        return completedFuture(null);
                      });
            });
  }

  /**
   * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this
   * method calls the optional error callback specified in the {@code syncOptions} and updates the
   * {@code statistics} instance by incrementing the total number of failed categories to sync.
   *
   * @param errorMessage The error message describing the reason(s) of failure.
   * @param exception The exception that called caused the failure, if any.
   */
  private void handleError(
      @Nonnull final String errorMessage, @Nullable final Throwable exception) {
    handleError(errorMessage, exception, null, null, null);
  }

  /**
   * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this
   * method calls the optional error callback specified in the {@code syncOptions} and updates the
   * {@code statistics} instance by incrementing the total number of failed categories to sync.
   *
   * @param errorMessage The error message describing the reason(s) of failure.
   * @param exception The exception that called caused the failure, if any.
   * @param oldCategory the category to update.
   * @param newCategory the category draft where we get the new data.
   * @param updateActions the list of update actions to update the category with.
   */
  private void handleError(
      @Nonnull final String errorMessage,
      @Nullable final Throwable exception,
      @Nullable final Category oldCategory,
      @Nullable final CategoryDraft newCategory,
      @Nullable final List<UpdateAction<Category>> updateActions) {
    SyncException syncException =
        exception != null
            ? new SyncException(errorMessage, exception)
            : new SyncException(errorMessage);
    syncOptions.applyErrorCallback(syncException, oldCategory, newCategory, updateActions);
    statistics.incrementFailed();
  }

  /**
   * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this
   * method calls the optional error callback specified in the {@code syncOptions} and updates the
   * {@code statistics} instance by incrementing the total number of failed category to sync with
   * the supplied {@code failedTimes}.
   *
   * @param syncException The exception that caused the failure.
   * @param failedTimes The number of times that the failed category counter is incremented.
   */
  private void handleError(@Nonnull final SyncException syncException, final int failedTimes) {
    syncOptions.applyErrorCallback(syncException);
    statistics.incrementFailed(failedTimes);
  }
}
