package com.commercetools.sync.services;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface CategoryService {

    @Nullable
    Category fetchCategoryByExternalId(@Nonnull final String externalId);

    @Nullable
    Category createCategory(@Nonnull final CategoryDraft categoryDraft);

    @Nullable
    Category updateCategory(@Nonnull final Category category,
                            @Nonnull final List<UpdateAction<Category>> updateActions);
}
