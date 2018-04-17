package com.commercetools.sync.commons.helpers;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.CategoryOrderHints;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Container for a {@link List} of {@link Category} {@link Reference}s and a {@link CategoryOrderHints}.
 */
public final class CategoryReferencePair {
    private Set<ResourceIdentifier<Category>> categoryResourceIdentifiers;
    private CategoryOrderHints categoryOrderHints;

    private CategoryReferencePair(@Nonnull final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers,
                                  @Nullable final CategoryOrderHints categoryOrderHints) {
        this.categoryResourceIdentifiers = categoryResourceIdentifiers;
        this.categoryOrderHints = categoryOrderHints;
    }

    public static CategoryReferencePair of(@Nonnull final Set<ResourceIdentifier<Category>> categoryReferences,
                                           @Nullable final CategoryOrderHints categoryOrderHints) {
        return new CategoryReferencePair(categoryReferences, categoryOrderHints);
    }


    public Set<ResourceIdentifier<Category>> getCategoryResourceIdentifiers() {
        return categoryResourceIdentifiers;
    }

    public CategoryOrderHints getCategoryOrderHints() {
        return categoryOrderHints;
    }
}
