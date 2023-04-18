package com.commercetools.sync.sdk2.commons.helpers;

import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.product.CategoryOrderHints;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Container for a {@link List} of {@link CategoryResourceIdentifier} and a {@link
 * CategoryOrderHints}.
 */
public final class CategoryResourceIdentifierPair {
  private List<CategoryResourceIdentifier> categoryResourceIdentifiers;
  private CategoryOrderHints categoryOrderHints;

  private CategoryResourceIdentifierPair(
      @Nonnull final List<CategoryResourceIdentifier> categoryResourceIdentifiers,
      @Nullable final CategoryOrderHints categoryOrderHints) {
    this.categoryResourceIdentifiers = categoryResourceIdentifiers;
    this.categoryOrderHints = categoryOrderHints;
  }

  public static CategoryResourceIdentifierPair of(
      @Nonnull final List<CategoryResourceIdentifier> categoryResourceIdentifiers,
      @Nullable final CategoryOrderHints categoryOrderHints) {
    return new CategoryResourceIdentifierPair(categoryResourceIdentifiers, categoryOrderHints);
  }

  public List<CategoryResourceIdentifier> getCategoryResourceIdentifiers() {
    return categoryResourceIdentifiers;
  }

  public CategoryOrderHints getCategoryOrderHints() {
    return categoryOrderHints;
  }
}
