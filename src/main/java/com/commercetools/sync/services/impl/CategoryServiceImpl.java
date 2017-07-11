package com.commercetools.sync.services.impl;


import com.commercetools.sync.services.CategoryService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryUpdateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.queries.PagedResult;
import io.sphere.sdk.queries.QueryExecutionUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public final class CategoryServiceImpl implements CategoryService {
    private final SphereClient ctpClient;
    private boolean isCached = false;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();

    public CategoryServiceImpl(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> cacheKeysToIds() {
        if (isCached) {
            return CompletableFuture.completedFuture(keyToIdCache);
        }
        isCached = true;
        return QueryExecutionUtils.queryAll(ctpClient, CategoryQuery.of())
                                  .thenApply(categories -> {
                                      categories.forEach(category ->
                                          keyToIdCache.put(category.getKey(), category.getId()));
                                      return keyToIdCache;
                                  });
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Category>> fetchCategoryByKey(@Nonnull final String key) {
        final CategoryQuery categoryQuery = CategoryQuery.of()
                                                         .withPredicates(categoryQueryModel ->
                                                             categoryQueryModel.key().is(key));
        return ctpClient.execute(categoryQuery).thenApply(PagedResult::head);
    }

    @Nonnull
    @Override
    public CompletionStage<Category> createCategory(@Nonnull final CategoryDraft categoryDraft) {
        final CategoryCreateCommand categoryCreateCommand = CategoryCreateCommand.of(categoryDraft);
        return ctpClient.execute(categoryCreateCommand);
    }

    @Nonnull
    @Override
    public CompletionStage<Category> updateCategory(@Nonnull final Category category,
                                                    @Nonnull final List<UpdateAction<Category>> updateActions) {
        final CategoryUpdateCommand categoryUpdateCommand = CategoryUpdateCommand.of(category, updateActions);
        return ctpClient.execute(categoryUpdateCommand);
    }
}
