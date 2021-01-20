package com.commercetools.sync.commons.models;

import io.sphere.sdk.products.ProductDraft;
import java.util.Set;
import javax.annotation.Nonnull;

public final class WaitingToBeResolvedProducts<T extends ProductDraft>
    extends WaitingToBeResolved<T> {
  private T waitingDraft;

  // Needed for the 'com.fasterxml.jackson' deserialization, for example, when fetching
  // from CTP custom objects.
  public WaitingToBeResolvedProducts() {}

  public WaitingToBeResolvedProducts(
      @Nonnull final T draft, @Nonnull final Set<String> missingReferencedKeys) {
    this.waitingDraft = draft;
    this.setMissingReferencedKeys(missingReferencedKeys);
  }

  public void setMissingReferencedKeys(@Nonnull final Set keys) {
    super.setMissingReferencedKeys(keys);
  }

  @Override
  public void setWaitingDraft(@Nonnull final ProductDraft draft) {
    waitingDraft = (T) draft;
  }

  @Override
  public T getWaitingDraft() {
    return waitingDraft;
  }
}
