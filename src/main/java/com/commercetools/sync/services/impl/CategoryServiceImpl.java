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
import io.sphere.sdk.queries.PagedResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class CategoryServiceImpl implements CategoryService {
    private final BlockingSphereClient ctpClient;

    public CategoryServiceImpl(@Nonnull final BlockingSphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Category>> fetchCategoryByExternalId(@Nullable final String externalId)
            throws SphereException {
        final CategoryQuery categoryQuery = CategoryQuery.of().byExternalId(externalId);
        return ctpClient.execute(categoryQuery).thenApply(PagedResult::head);
    }

    @Nonnull
    @Override
    public CompletionStage<Category> createCategory(@Nonnull final CategoryDraft categoryDraft) throws SphereException {
        final CategoryCreateCommand categoryCreateCommand = CategoryCreateCommand.of(categoryDraft);
        return ctpClient.execute(categoryCreateCommand);

    }

    @Nonnull
    @Override
    public CompletionStage<Category> updateCategory(@Nonnull final Category category,
                                   @Nonnull final List<UpdateAction<Category>> updateActions)
            throws SphereException {
        final CategoryUpdateCommand categoryUpdateCommand = CategoryUpdateCommand.of(category, updateActions);
        return ctpClient.execute(categoryUpdateCommand);
    }
}
