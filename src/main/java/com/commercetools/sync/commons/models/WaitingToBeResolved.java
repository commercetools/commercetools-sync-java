package com.commercetools.sync.commons.models;

import io.sphere.sdk.products.ProductDraft;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

public final class WaitingToBeResolved {
  private ProductDraft productDraft;
  private Set<String> missingReferencedProductKeys;

  /**
   * Represents a productDraft that is waiting for some product references, which are on this
   * productDraft as attributes, to be resolved.
   *
   * @param productDraft product draft which has irresolvable references as attributes.
   * @param missingReferencedProductKeys product keys of irresolvable references.
   */
  public WaitingToBeResolved(
      @Nonnull final ProductDraft productDraft,
      @Nonnull final Set<String> missingReferencedProductKeys) {
    this.productDraft = productDraft;
    this.missingReferencedProductKeys = missingReferencedProductKeys;
  }

  // Needed for the 'com.fasterxml.jackson' deserialization, for example, when fetching
  // from CTP custom objects.
  public WaitingToBeResolved() {}

  @Nonnull
  public ProductDraft getProductDraft() {
    return productDraft;
  }

  @Nonnull
  public Set<String> getMissingReferencedProductKeys() {
    return missingReferencedProductKeys;
  }

  public void setProductDraft(@Nonnull final ProductDraft productDraft) {
    this.productDraft = productDraft;
  }

  public void setMissingReferencedProductKeys(
      @Nonnull final Set<String> missingReferencedProductKeys) {
    this.missingReferencedProductKeys = missingReferencedProductKeys;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof WaitingToBeResolved)) {
      return false;
    }
    final WaitingToBeResolved that = (WaitingToBeResolved) other;
    return Objects.equals(getProductDraft().getKey(), that.getProductDraft().getKey())
        && getMissingReferencedProductKeys().equals(that.getMissingReferencedProductKeys());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getProductDraft().getKey(), getMissingReferencedProductKeys());
  }
}
