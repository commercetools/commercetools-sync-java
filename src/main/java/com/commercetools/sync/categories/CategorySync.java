package com.commercetools.sync.categories;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.areResourceIdentifiersEqual;
import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.api.models.common.ResourceIdentifier;
import com.commercetools.sync.categories.helpers.CategoryBatchValidator;
import com.commercetools.sync.categories.helpers.CategoryReferenceResolver;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.categories.utils.CategorySyncUtils;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.models.WaitingToBeResolvedCategories;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.UnresolvedReferencesService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class CategorySync
    extends BaseSync<
        Category,
        CategoryDraft,
        CategoryUpdateAction,
        CategorySyncStatistics,
        CategorySyncOptions> {

  private static final String FAILED_TO_FETCH =
      "Failed to fetch existing categories with keys: '%s'. Reason: %s";
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

  private ConcurrentHashMap.KeySetView<String, Boolean> readyToResolve;
  private ConcurrentHashMap<CategoryDraft, Category> categoryDraftsToUpdateSequentially;

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

  @Override
  protected CompletionStage<CategorySyncStatistics> process(
      @Nonnull final List<CategoryDraft> categoryDrafts) {
    final List<List<CategoryDraft>> batches =
        batchElements(categoryDrafts, syncOptions.getBatchSize());
    return syncBatches(batches, completedFuture(statistics));
  }

  @Override
  protected CompletionStage<CategorySyncStatistics> processBatch(
      @Nonnull final List<CategoryDraft> categoryDrafts) {

    setBatchState();

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
                    "Failed to build a cache of keys to ids.",
                    cachingException,
                    null,
                    null,
                    null,
                    validDrafts.size());
                return completedFuture(null);
              }

              final Map<String, String> keyToIdCache = cachingResponse.getKey();
              return syncBatch(validDrafts, keyToIdCache);
            })
        .thenApply(
            ignoredResult -> {
              statistics.incrementProcessed(categoryDrafts.size());
              return statistics;
            });
  }

  private void setBatchState() {
    readyToResolve = ConcurrentHashMap.newKeySet();
    categoryDraftsToUpdateSequentially = new ConcurrentHashMap<>();
  }

  @Nonnull
  private CompletionStage<Void> syncBatch(
      @Nonnull final Set<CategoryDraft> categoryDrafts,
      @Nonnull final Map<String, String> keyToIdCache) {

    final Set<String> categoryDraftKeys =
        categoryDrafts.stream().map(CategoryDraft::getKey).collect(Collectors.toSet());

    return categoryService
        .fetchMatchingCategoriesByKeys(categoryDraftKeys)
        .handle(ImmutablePair::new)
        .thenCompose(
            fetchResponse -> {
              final Throwable fetchException = fetchResponse.getValue();
              if (fetchException != null) {
                final String errorMessage =
                    format(FAILED_TO_FETCH, categoryDraftKeys, fetchException.getMessage());
                handleError(
                    errorMessage, fetchException, null, null, null, categoryDraftKeys.size());
                return CompletableFuture.completedFuture(null);
              } else {
                final Set<Category> matchingCategories = fetchResponse.getKey();
                return syncOrKeepTrack(categoryDrafts, matchingCategories, keyToIdCache)
                    .thenCompose(
                        aVoid -> updateCategoriesSequentially(categoryDraftsToUpdateSequentially))
                    .thenCompose(aVoid -> resolveNowReadyReferences(keyToIdCache));
              }
            });
  }

  /**
   * Given a set of category drafts, for each new draft: if it doesn't have any parent category
   * reference which are missing, it syncs the new draft. However, if it does have missing parent
   * category reference, it keeps track of it by persisting it.
   *
   * @param oldCategories old category types.
   * @param newCategories drafts that need to be synced.
   * @return a {@link CompletionStage} which contains an empty result after execution of the update
   */
  @Nonnull
  private CompletionStage<Void> syncOrKeepTrack(
      @Nonnull final Set<CategoryDraft> newCategories,
      @Nonnull final Set<Category> oldCategories,
      @Nonnull final Map<String, String> keyToIdCache) {

    return allOf(
        newCategories.stream()
            .map(
                newDraft -> {
                  final Optional<String> missingReferencedParentCategoryKey =
                      getMissingReferencedParentCategoryKey(newDraft, keyToIdCache);

                  if (missingReferencedParentCategoryKey.isPresent()) {
                    return keepTrackOfMissingReference(
                        newDraft, missingReferencedParentCategoryKey.get());
                  } else {
                    return syncDraft(oldCategories, newDraft);
                  }
                })
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
  }

  private Optional<String> getMissingReferencedParentCategoryKey(
      @Nonnull final CategoryDraft newCategory, @Nonnull final Map<String, String> keyToIdCache) {

    final String parentCategoryKey =
        Optional.ofNullable(newCategory.getParent()).map(ResourceIdentifier::getKey).orElse(null);

    if (StringUtils.isBlank(parentCategoryKey) || keyToIdCache.containsKey(parentCategoryKey)) {
      return Optional.empty();
    }

    return Optional.of(parentCategoryKey);
  }

  private CompletionStage<Optional<WaitingToBeResolvedCategories>> keepTrackOfMissingReference(
      @Nonnull final CategoryDraft newCategory, @Nonnull final String parentCategoryKey) {

    statistics.addMissingDependency(parentCategoryKey, newCategory.getKey());
    return unresolvedReferencesService.save(
        new WaitingToBeResolvedCategories(newCategory, Collections.singleton(parentCategoryKey)),
        UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_CATEGORY_CONTAINER_KEY,
        WaitingToBeResolvedCategories.class);
  }

  @Nonnull
  private CompletionStage<Void> syncDraft(
      @Nonnull final Set<Category> oldCategories, @Nonnull final CategoryDraft newCategory) {

    final Map<String, Category> oldCategoryMap =
        oldCategories.stream().collect(toMap(Category::getKey, identity()));

    return referenceResolver
        .resolveReferences(newCategory)
        .thenCompose(
            resolvedDraft -> {
              final Category oldCategory = oldCategoryMap.get(newCategory.getKey());

              if (oldCategory != null) {
                return fetchAndUpdate(oldCategory, resolvedDraft);
              } else {
                return applyCallbackAndCreate(resolvedDraft);
              }
            })
        .exceptionally(
            completionException -> {
              final String errorMessage =
                  format(FAILED_TO_PROCESS, newCategory.getKey(), completionException.getMessage());
              handleError(errorMessage, completionException, null, newCategory, null, 1);
              return null;
            });
  }

  @Nonnull
  private CompletionStage<Void> applyCallbackAndCreate(@Nonnull final CategoryDraft categoryDraft) {
    return syncOptions
        .applyBeforeCreateCallback(categoryDraft)
        .map(
            draft ->
                categoryService
                    .createCategory(draft)
                    .thenAccept(
                        categoryOptional -> {
                          if (categoryOptional.isPresent()) {
                            readyToResolve.add(categoryDraft.getKey());
                            statistics.incrementCreated();
                          } else {
                            statistics.incrementFailed();
                          }
                        }))
        .orElse(completedFuture(null));
  }

  @Nonnull
  private CompletionStage<Void> fetchAndUpdate(
      @Nonnull final Category oldCategory, @Nonnull final CategoryDraft newCategory) {

    String key = oldCategory.getKey();
    return categoryService
        .fetchCategory(key)
        .handle(ImmutablePair::new)
        .thenCompose(
            fetchResponse -> {
              Optional<Category> fetchedCategoryOptional = fetchResponse.getKey();
              final Throwable exception = fetchResponse.getValue();

              if (exception != null) {
                final String errorMessage =
                    format(
                        FAILED_TO_FETCH,
                        key,
                        "Failed to fetch from CTP while retrying after concurrency modification.");
                handleError(errorMessage, exception, oldCategory, newCategory, null, 1);
                return completedFuture(null);
              }

              if (requiresChangeParentUpdateAction(oldCategory, newCategory)) {
                categoryDraftsToUpdateSequentially.putIfAbsent(newCategory, oldCategory);
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
                        handleError(errorMessage, null, oldCategory, newCategory, null, 1);
                        return completedFuture(null);
                      });
            });
  }

  /**
   * Compares the parent references of a {@link Category} and a {@link CategoryDraft} to check
   * whether a {@link com.commercetools.api.models.category.CategoryChangeParentAction} update
   * action is required to sync the draft to the category or not
   *
   * @param category the old category to sync to.
   * @param categoryDraft the new category draft to sync.
   * @return true or false whether a {@link
   *     com.commercetools.api.models.category.CategoryChangeParentAction} is needed to sync the
   *     draft to the category.
   */
  static boolean requiresChangeParentUpdateAction(
      @Nonnull final Category category, @Nonnull final CategoryDraft categoryDraft) {
    return !areResourceIdentifiersEqual(category.getParent(), categoryDraft.getParent());
  }

  private CompletionStage<Void> updateCategory(
      @Nonnull final Category oldCategory,
      @Nonnull final CategoryDraft newCategory,
      @Nonnull final List<CategoryUpdateAction> updateActions) {
    final String categoryKey = oldCategory.getKey();
    return categoryService
        .updateCategory(oldCategory, updateActions)
        .handle(ImmutablePair::new)
        .thenCompose(
            updateResponse -> {
              final Throwable ctpException = updateResponse.getValue();

              if (ctpException != null) {
                return executeSupplierIfConcurrentModificationException(
                    ctpException,
                    () -> fetchAndUpdate(oldCategory, newCategory),
                    () -> {
                      final String errorMessage =
                          format(UPDATE_FAILED, categoryKey, ctpException.getMessage());
                      handleError(
                          errorMessage, ctpException, oldCategory, newCategory, updateActions, 1);

                      categoryDraftsToUpdateSequentially.remove(newCategory);
                      return completedFuture(null);
                    });
              } else {
                categoryDraftsToUpdateSequentially.remove(newCategory);
                statistics.incrementUpdated();
                return completedFuture(null);
              }
            });
  }

  @Nonnull
  private CompletionStage<Void> resolveNowReadyReferences(
      @Nonnull final Map<String, String> keyToIdCache) {

    final Set<String> referencingDraftKeys =
        readyToResolve.stream()
            .map(statistics::getChildrenKeys)
            .filter(Objects::nonNull)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

    if (referencingDraftKeys.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    final Set<CategoryDraft> readyToSync = new HashSet<>();

    return unresolvedReferencesService
        .fetch(
            referencingDraftKeys,
            UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_CATEGORY_CONTAINER_KEY,
            WaitingToBeResolvedCategories.class)
        .handle(ImmutablePair::new)
        .thenCompose(
            fetchResponse -> {
              final Set<WaitingToBeResolvedCategories> waitingDrafts = fetchResponse.getKey();
              final Throwable fetchException = fetchResponse.getValue();

              if (fetchException != null) {
                final String errorMessage =
                    format(FAILED_TO_FETCH_WAITING_DRAFTS, referencingDraftKeys);
                handleError(
                    errorMessage, fetchException, null, null, null, referencingDraftKeys.size());
                return CompletableFuture.completedFuture(null);
              }

              waitingDrafts.forEach(
                  waitingDraft -> {
                    waitingDraft
                        .getMissingReferencedCategoriesKeys()
                        .forEach(
                            parentKey -> {
                              statistics.removeChildCategoryKeyFromMissingParentsMap(
                                  parentKey, waitingDraft.getKey());
                            });
                    readyToSync.add(waitingDraft.getCategoryDraft());
                  });

              return syncBatch(readyToSync, keyToIdCache)
                  .thenCompose(aVoid -> removeFromWaiting(readyToSync));
            });
  }

  @Nonnull
  private CompletableFuture<Void> removeFromWaiting(@Nonnull final Set<CategoryDraft> drafts) {
    return allOf(
        drafts.stream()
            .map(CategoryDraft::getKey)
            .map(
                key ->
                    unresolvedReferencesService.delete(
                        key,
                        UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_CATEGORY_CONTAINER_KEY,
                        WaitingToBeResolvedCategories.class))
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
  }

  private CompletionStage<Void> buildUpdateActionsAndUpdate(
      @Nonnull final Category oldCategory, @Nonnull final CategoryDraft newCategory) {

    final List<CategoryUpdateAction> updateActions =
        CategorySyncUtils.buildActions(oldCategory, newCategory, syncOptions);

    final List<CategoryUpdateAction> beforeUpdateCallBackApplied =
        syncOptions.applyBeforeUpdateCallback(updateActions, newCategory, oldCategory);

    if (!beforeUpdateCallBackApplied.isEmpty()) {
      return updateCategory(oldCategory, newCategory, beforeUpdateCallBackApplied);
    }

    return completedFuture(null);
  }

  /**
   * Given a {@link Map} of categoryDrafts to Categories that require syncing, this method updates
   * only categories which need a {@link
   * com.commercetools.api.models.category.CategoryChangeParentAction} update action, and in turn
   * performs the sync on them in a sequential/blocking fashion as advised by the CTP documentation:
   * https://docs.commercetools.com/api/projects/categories#change-parent
   *
   * @param matchingCategories a {@link Map} of categoryDrafts to Categories that require syncing.
   */
  private CompletableFuture<Void> updateCategoriesSequentially(
      @Nonnull final Map<CategoryDraft, Category> matchingCategories) {
    matchingCategories.entrySet().stream()
        .map(entry -> buildUpdateActionsAndUpdate(entry.getValue(), entry.getKey()))
        .map(CompletionStage::toCompletableFuture)
        .forEach(CompletableFuture::join);
    return completedFuture(null);
  }
}
