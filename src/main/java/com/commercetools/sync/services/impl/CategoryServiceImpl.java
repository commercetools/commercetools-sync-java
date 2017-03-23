package com.commercetools.sync.services.impl;


import com.commercetools.sync.services.CategoryService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryUpdateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class CategoryServiceImpl implements CategoryService {
    private final BlockingSphereClient ctpClient;

    public CategoryServiceImpl(BlockingSphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Nonnull
    @Override
    public Category fetchCategoryByExternalId(@Nonnull final String externalId) {
        CategoryQuery categoryQuery = CategoryQuery.of()
                .byExternalId(externalId);
        return ctpClient.executeBlocking(categoryQuery)
                .head().orElse(null);
    }

    @Nullable
    @Override
    public Category createCategory(@Nonnull CategoryDraft categoryDraft) {
        final CategoryCreateCommand categoryCreateCommand = CategoryCreateCommand.of(categoryDraft);
        return ctpClient.executeBlocking(categoryCreateCommand);
    }

    @Nullable
    @Override
    public Category updateCategory(@Nonnull Category category, @Nonnull List<UpdateAction<Category>> updateActions) {
        final CategoryUpdateCommand categoryUpdateCommand = CategoryUpdateCommand.of(category, updateActions);
        return ctpClient.executeBlocking(categoryUpdateCommand);
    }
}
