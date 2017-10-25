package com.commercetools.sync.commons.helpers;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.CategoryOrderHints;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Container for a {@link List} of {@link Category} {@link Reference}s and a {@link CategoryOrderHints}.
 */
public final class CategoryReferencePair {
    private List<Reference<Category>> categoryReferences;
    private CategoryOrderHints categoryOrderHints;

    private CategoryReferencePair(@Nonnull final List<Reference<Category>> categoryReferences,
                                  @Nullable final CategoryOrderHints categoryOrderHints) {
        this.categoryReferences = categoryReferences;
        this.categoryOrderHints = categoryOrderHints;
    }

    public static CategoryReferencePair of(@Nonnull final List<Reference<Category>> categoryReferences,
                                           @Nullable final CategoryOrderHints categoryOrderHints) {
        return new CategoryReferencePair(categoryReferences, categoryOrderHints);
    }


    public List<Reference<Category>> getCategoryReferences() {
        return categoryReferences;
    }

    public CategoryOrderHints getCategoryOrderHints() {
        return categoryOrderHints;
    }
}
