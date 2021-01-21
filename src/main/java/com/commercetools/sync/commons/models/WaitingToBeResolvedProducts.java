package com.commercetools.sync.commons.models;

import io.sphere.sdk.products.ProductDraft;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

public final class WaitingToBeResolvedProducts extends WaitingToBeResolved<ProductDraft> {
  private ProductDraft productDraft;
  private Set<String> missingReferencedProductKeys;

  /**
   * Represents a productDraft that is waiting for some product references, which are on this
   * productDraft as attributes, to be resolved.
   *
   * @param productDraft product draft which has irresolvable references as attributes.
   * @param missingReferencedProductKeys product keys of irresolvable references.
   */
  public WaitingToBeResolvedProducts(
      @Nonnull final ProductDraft productDraft,
      @Nonnull final Set<String> missingReferencedProductKeys) {
    this.productDraft = productDraft;
    this.missingReferencedProductKeys = missingReferencedProductKeys;
  }

  // Needed for the 'com.fasterxml.jackson' deserialization, for example, when fetching
  // from CTP custom objects.
  public WaitingToBeResolvedProducts() {}

  @Nonnull
  public ProductDraft getProductDraft() {
    return productDraft;
  }

  @Override
  public String getKey() {
    return getProductDraft().getKey();
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
    if (!(other instanceof WaitingToBeResolvedProducts)) {
      return false;
    }
    final WaitingToBeResolvedProducts that = (WaitingToBeResolvedProducts) other;
    return Objects.equals(getProductDraft().getKey(), that.getProductDraft().getKey())
        && getMissingReferencedProductKeys().equals(that.getMissingReferencedProductKeys());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getProductDraft().getKey(), getMissingReferencedProductKeys());
  }
}
