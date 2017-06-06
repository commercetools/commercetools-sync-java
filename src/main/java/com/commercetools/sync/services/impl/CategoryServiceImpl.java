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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class CategoryServiceImpl implements CategoryService {
    private final SphereClient ctpClient;
    private final Map<String, String> externalIdToIdCache = new HashMap<>();

    public CategoryServiceImpl(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedCategoryId(@Nonnull final String externalId) {
        if (externalIdToIdCache.isEmpty()) {
            return cacheAndFetch(externalId);
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(externalIdToIdCache.get(externalId)));
    }

    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String externalId) {
        return QueryExecutionUtils.queryAll(ctpClient, CategoryQuery.of())
                                  .thenApply(categories -> {
                                      categories.forEach(category ->
                                          externalIdToIdCache.put(category.getExternalId(), category.getId()));
                                      return Optional.ofNullable(externalIdToIdCache.get(externalId));
                                  });
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Category>> fetchCategoryByExternalId(@Nonnull final String externalId) {
        final CategoryQuery categoryQuery = CategoryQuery.of().byExternalId(externalId);
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
