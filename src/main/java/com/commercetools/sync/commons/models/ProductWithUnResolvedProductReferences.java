package com.commercetools.sync.commons.models;

import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import java.util.List;

public final class ProductWithUnResolvedProductReferences {
    private ProductDraft productDraft;
    private List<String> missingReferencedProductKeys;

    /**
     * Object represent the value for the custom object that will hold unresolved references
     *
     * @param productDraft       product draft which has non-resolved references
     * @param missingProductKeys product keys of non-resolved references
     */
    public ProductWithUnResolvedProductReferences(
        @Nonnull final ProductDraft productDraft,
        @Nonnull final List<String> missingProductKeys) {
        this.productDraft = productDraft;
        this.missingReferencedProductKeys = missingProductKeys;
    }

    public ProductWithUnResolvedProductReferences() {
    }

    public ProductDraft getProductDraft() {
        return productDraft;
    }

    public List<String> getMissingReferencedProductKeys() {
        return missingReferencedProductKeys;
    }

    public void setProductDraft(@Nonnull final ProductDraft productDraft) {
        this.productDraft = productDraft;
    }

    public void setMissingReferencedProductKeys(@Nonnull final List<String> missingReferencedProductKeys) {
        this.missingReferencedProductKeys = missingReferencedProductKeys;
    }
}
