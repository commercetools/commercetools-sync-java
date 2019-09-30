package com.commercetools.sync.commons.models;

import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import java.util.Set;

public final class ProductWithUnResolvedProductReferences {

    private ProductDraft productDraft;

    private Set<String> missingReferencedProductKeys;

    /**
     * Object represent the value for the custom object that will hold unresolved references
     * @param productDraft product draft which has non-resolved references
     * @param missingReferencedProductKeys product keys of non-resolved references
     */
    public ProductWithUnResolvedProductReferences(@Nonnull final ProductDraft productDraft,
                                                  @Nonnull final Set<String> missingReferencedProductKeys) {
        this.productDraft = productDraft;
        this.missingReferencedProductKeys = missingReferencedProductKeys;
    }

    public ProductWithUnResolvedProductReferences() {
    }

    public ProductDraft getProductDraft() {
        return productDraft;
    }

    public void setProductDraft(final ProductDraft productDraft) {
        this.productDraft = productDraft;
    }

    public Set<String> getMissingReferencedProductKeys() {
        return missingReferencedProductKeys;
    }

    public void setMissingReferencedProductKeys(@Nonnull final Set<String> missingReferencedProductKeys) {
        this.missingReferencedProductKeys = missingReferencedProductKeys;
    }

}
