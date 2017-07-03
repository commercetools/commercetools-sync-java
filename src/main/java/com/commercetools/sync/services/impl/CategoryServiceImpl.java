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
    private boolean invalidCache = false;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();

    public CategoryServiceImpl(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedCategoryId(@Nonnull final String key) {
        if (keyToIdCache.isEmpty() || invalidCache) {
            return cacheAndFetch(key);
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
    }

    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key) {
        return QueryExecutionUtils.queryAll(ctpClient, CategoryQuery.of())
                                  .thenApply(categories -> {
                                      categories.forEach(category ->
                                          keyToIdCache.put(category.getKey(), category.getId()));
                                      return Optional.ofNullable(keyToIdCache.get(key));
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

    @Override
    public void invalidateCache() {
        invalidCache = true;
    }
}
