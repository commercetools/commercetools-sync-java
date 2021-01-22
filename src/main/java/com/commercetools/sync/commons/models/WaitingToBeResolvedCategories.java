package com.commercetools.sync.commons.models;

import io.sphere.sdk.categories.CategoryDraft;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

public final class WaitingToBeResolvedCategories extends WaitingToBeResolved<CategoryDraft> {
  private CategoryDraft waitingDraft;
  private Set<String> missingReferencedKeys;

  public Set<String> getMissingReferencedKeys() {
    return missingReferencedKeys;
  }

  public void setMissingReferencedKeys(Set<String> missingReferencedKeys) {
    this.missingReferencedKeys = missingReferencedKeys;
  }

  // Needed for the 'com.fasterxml.jackson' deserialization, for example, when fetching
  // from CTP custom objects.
  public WaitingToBeResolvedCategories() {}

  public WaitingToBeResolvedCategories(
      @Nonnull final CategoryDraft draft, @Nonnull final Set<String> missingReferencedKeys) {
    this.waitingDraft = draft;
    this.setMissingReferencedKeys(missingReferencedKeys);
  }

  public void setWaitingDraft(@Nonnull final CategoryDraft draft) {
    waitingDraft = draft;
  }

  public CategoryDraft getWaitingDraft() {
    return waitingDraft;
  }

  @Override
  public String getKey() {
    return getWaitingDraft().getKey();
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
    return Objects.equals(getWaitingDraft().getKey(), that.getWaitingDraft().getKey())
        && getMissingReferencedKeys().equals(that.getMissingReferencedKeys());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getWaitingDraft().getKey(), getMissingReferencedKeys());
  }
}
