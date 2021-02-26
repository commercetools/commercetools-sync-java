package com.commercetools.sync.commons.models;

import io.sphere.sdk.categories.CategoryDraft;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

public final class WaitingToBeResolvedCategories implements WaitingToBeResolved {
  private CategoryDraft categoryDraft;
  private Set<String> missingReferencedCategoriesKeys;

  public Set<String> getMissingReferencedCategoriesKeys() {
    return missingReferencedCategoriesKeys;
  }

  public void setMissingReferencedCategoriesKeys(Set<String> missingReferencedCategoriesKeys) {
    this.missingReferencedCategoriesKeys = missingReferencedCategoriesKeys;
  }

  // Needed for the 'com.fasterxml.jackson' deserialization, for example, when fetching
  // from CTP custom objects.
  public WaitingToBeResolvedCategories() {}

  public WaitingToBeResolvedCategories(
      @Nonnull final CategoryDraft draft,
      @Nonnull final Set<String> missingReferencedCategoriesKeys) {
    this.categoryDraft = draft;
    this.setMissingReferencedCategoriesKeys(missingReferencedCategoriesKeys);
  }

  public void setCategoryDraft(@Nonnull final CategoryDraft draft) {
    categoryDraft = draft;
  }

  public CategoryDraft getCategoryDraft() {
    return categoryDraft;
  }

  @Override
  public String getKey() {
    return getCategoryDraft().getKey();
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof WaitingToBeResolvedCategories)) {
      return false;
    }
    final WaitingToBeResolvedCategories that = (WaitingToBeResolvedCategories) other;
    return Objects.equals(getCategoryDraft().getKey(), that.getCategoryDraft().getKey())
        && getMissingReferencedCategoriesKeys().equals(that.getMissingReferencedCategoriesKeys());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCategoryDraft().getKey(), getMissingReferencedCategoriesKeys());
  }
}
