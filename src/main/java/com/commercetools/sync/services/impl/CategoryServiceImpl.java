package com.commercetools.sync.services.impl;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.services.CategoryService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryUpdateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.queries.QueryExecutionUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.lang.String.format;

public final class CategoryServiceImpl implements CategoryService {
    private final CategorySyncOptions syncOptions;
    private boolean isCached = false;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();
    private static final String CREATE_FAILED = "Failed to create CategoryDraft with key: '%s'. Reason: %s";
    private static final String FETCH_FAILED = "Failed to fetch CategoryDrafts with keys: '%s'. Reason: %s";
    private static final String UPDATE_FAILED = "Failed to update Category with key: '%s'. Reason: %s";

    public CategoryServiceImpl(@Nonnull final CategorySyncOptions syncOptions) {
        this.syncOptions = syncOptions;
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> cacheKeysToIds() {
        if (isCached) {
            return CompletableFuture.completedFuture(keyToIdCache);
        }
        isCached = true;
        return QueryExecutionUtils.queryAll(syncOptions.getCtpClient(), CategoryQuery.of())
                                  .thenApply(categories -> {
                                      categories.forEach(category ->
                                          keyToIdCache.put(category.getKey(), category.getId()));
                                      return keyToIdCache;
                                  });
    }

    @Nonnull
    @Override
    public CompletionStage<Set<Category>> fetchMatchingCategoriesByKeys(@Nonnull final Set<String> categoryKeys) {
        if (categoryKeys.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }
        return QueryExecutionUtils.queryAll(syncOptions.getCtpClient(),
            CategoryQuery.of().plusPredicates(categoryQueryModel -> categoryQueryModel.key().isIn(categoryKeys)))
                                  .handle((fetchedCategories, sphereException) -> {
                                      if (sphereException != null) {
                                          syncOptions
                                              .applyErrorCallback(format(FETCH_FAILED, categoryKeys, sphereException),
                                                  sphereException);
                                          return Collections.emptySet();
                                      } else {
                                          return fetchedCategories.stream()
                                                           .collect(Collectors.toSet());
                                      }
                                  });
    }

    @Nonnull
    @Override
    public CompletionStage<Set<Category>> createCategories(@Nonnull final Set<CategoryDraft> categoryDrafts) {
        final List<CompletableFuture<Optional<Category>>> futureCreations = categoryDrafts.stream()
                                                                        .map(this::createCategory)
                                                                        .map(CompletionStage::toCompletableFuture)
                                                                        .collect(Collectors.toList());
        return CompletableFuture.allOf(futureCreations.toArray(new CompletableFuture[futureCreations.size()]))
                                .thenApply(result -> futureCreations.stream()
                                    .map(CompletionStage::toCompletableFuture)
                                    .map(CompletableFuture::join)
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toSet()));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedCategoryId(@Nonnull final String key) {
        if (isCached) {
            return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
        }
        return cacheAndFetch(key);
    }

    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key) {
        return cacheKeysToIds()
            .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Category>> createCategory(@Nonnull final CategoryDraft categoryDraft) {
        final CategoryCreateCommand categoryCreateCommand = CategoryCreateCommand.of(categoryDraft);
        return syncOptions.getCtpClient().execute(categoryCreateCommand)
                          .handle((createdCategory, sphereException) -> {
                              if (sphereException != null) {
                                  syncOptions
                                      .applyErrorCallback(format(CREATE_FAILED, categoryDraft.getKey(),
                                          sphereException), sphereException);
                                  return Optional.empty();
                              } else {
                                  keyToIdCache.put(createdCategory.getKey(), createdCategory.getId());
                                  return Optional.of(createdCategory);
                              }
                          });
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Category>> updateCategory(@Nonnull final Category category,
                                                              @Nonnull final List<UpdateAction<Category>>
                                                                  updateActions) {
        final CategoryUpdateCommand categoryUpdateCommand = CategoryUpdateCommand.of(category, updateActions);
        return syncOptions.getCtpClient().execute(categoryUpdateCommand)
                          .handle((updatedCategory, sphereException) -> {
                              if (sphereException != null) {
                                  syncOptions
                                      .applyErrorCallback(format(UPDATE_FAILED, category.getKey(), sphereException),
                                          sphereException);
                                  return Optional.empty();
                              } else {
                                  return Optional.of(updatedCategory);
                              }
                          });
    }
}
