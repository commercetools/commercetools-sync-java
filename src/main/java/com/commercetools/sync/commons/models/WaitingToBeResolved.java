package com.commercetools.sync.commons.models;

import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import java.util.Set;

public final class WaitingToBeResolved {
    private ProductDraft productDraft;
    private Set<String> missingReferencedProductKeys;

    /**
     * Object represent the value for the custom object that will hold unresolved references
     *
     * @param productDraft       product draft which has non-resolved references
     * @param missingProductKeys product keys of non-resolved references
     */
    public WaitingToBeResolved(
        @Nonnull final ProductDraft productDraft,
        @Nonnull final Set<String> missingProductKeys) {
        this.productDraft = productDraft;
        this.missingReferencedProductKeys = missingProductKeys;
    }

    public WaitingToBeResolved() {
    }

    public ProductDraft getProductDraft() {
        return productDraft;
    }

    public Set<String> getMissingReferencedProductKeys() {
        return missingReferencedProductKeys;
    }

    public void setProductDraft(@Nonnull final ProductDraft productDraft) {
        this.productDraft = productDraft;
    }

    public void setMissingReferencedProductKeys(@Nonnull final Set<String> missingReferencedProductKeys) {
        this.missingReferencedProductKeys = missingReferencedProductKeys;
    }
}
