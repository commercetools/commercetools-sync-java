package com.commercetools.sync.sdk2.commons.helpers;

import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.product.CategoryOrderHints;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Container for a {@link java.util.List} of {@link
 * com.commercetools.api.models.category.CategoryResourceIdentifier}s and a {@link
 * CategoryOrderHints}.
 */
public final class CategoryReferencePair {
  private List<CategoryResourceIdentifier> categoryResourceIdentifiers;
  private CategoryOrderHints categoryOrderHints;

  private CategoryReferencePair(
      @Nonnull final List<CategoryResourceIdentifier> categoryResourceIdentifiers,
      @Nullable final CategoryOrderHints categoryOrderHints) {
    this.categoryResourceIdentifiers = categoryResourceIdentifiers;
    this.categoryOrderHints = categoryOrderHints;
  }

  public static CategoryReferencePair of(
      @Nonnull final List<CategoryResourceIdentifier> categoryReferences,
      @Nullable final CategoryOrderHints categoryOrderHints) {
    return new CategoryReferencePair(categoryReferences, categoryOrderHints);
  }

  public List<CategoryResourceIdentifier> getCategoryResourceIdentifiers() {
    return categoryResourceIdentifiers;
  }

  public CategoryOrderHints getCategoryOrderHints() {
    return categoryOrderHints;
  }
}
