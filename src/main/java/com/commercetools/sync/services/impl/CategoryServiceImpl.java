package com.commercetools.sync.services.impl;


import com.commercetools.sync.services.CategoryService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryUpdateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.SphereException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class CategoryServiceImpl implements CategoryService {
    private final BlockingSphereClient ctpClient;

    public CategoryServiceImpl(@Nonnull final BlockingSphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Nonnull
    @Override
    public Category fetchCategoryByExternalId(@Nonnull final String externalId)
            throws SphereException {
        CategoryQuery categoryQuery = CategoryQuery.of()
                .byExternalId(externalId);
        return ctpClient.executeBlocking(categoryQuery)
                .head().orElse(null);
    }

    @Nullable
    @Override
    public Category createCategory(@Nonnull final CategoryDraft categoryDraft) throws SphereException {
        final CategoryCreateCommand categoryCreateCommand = CategoryCreateCommand.of(categoryDraft);
        return ctpClient.executeBlocking(categoryCreateCommand);
    }

    @Nullable
    @Override
    public Category updateCategory(@Nonnull final Category category,
                                   @Nonnull final List<UpdateAction<Category>> updateActions)
            throws SphereException {
        final CategoryUpdateCommand categoryUpdateCommand = CategoryUpdateCommand.of(category, updateActions);
        return ctpClient.executeBlocking(categoryUpdateCommand);
    }
}
