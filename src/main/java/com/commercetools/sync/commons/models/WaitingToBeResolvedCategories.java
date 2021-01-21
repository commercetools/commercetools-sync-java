package com.commercetools.sync.commons.models;

import io.sphere.sdk.categories.CategoryDraft;
import java.util.Set;
import javax.annotation.Nonnull;

public final class WaitingToBeResolvedCategories extends WaitingToBeResolved<CategoryDraft> {
  private CategoryDraft waitingDraft;

  // Needed for the 'com.fasterxml.jackson' deserialization, for example, when fetching
  // from CTP custom objects.
  public WaitingToBeResolvedCategories() {}

  public WaitingToBeResolvedCategories(
      @Nonnull final CategoryDraft draft, @Nonnull final Set<String> missingReferencedKeys) {
    this.waitingDraft = draft;
    this.setMissingReferencedKeys(missingReferencedKeys);
  }

  public void setMissingReferencedKeys(@Nonnull final Set keys) {
    super.setMissingReferencedKeys(keys);
  }

  @Override
  public void setWaitingDraft(@Nonnull final CategoryDraft draft) {
    waitingDraft = draft;
  }

  @Override
  public CategoryDraft getWaitingDraft() {
    return waitingDraft;
  }
}
