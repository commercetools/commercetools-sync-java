package com.commercetools.sync.commons.models;

import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import java.util.Set;

public class ProductWithUnResolvedProductReferences {

    private String productKey;

    private ProductDraft productDraft;

    private Set<String> missingReferencedProductKeys;

    /**
     * Object represent the value for the custom object that will hold unresolved references
     * @param productKey product key that has non-resolved references
     * @param productDraft product draft which has non-resolved references
     * @param missingReferencedProductKeys product keys of non-resolved references
     */
    public ProductWithUnResolvedProductReferences(@Nonnull final String productKey, @Nonnull final ProductDraft productDraft,
                                                  final Set<String> missingReferencedProductKeys) {
        this.productKey = productKey;
        this.productDraft = productDraft;
        this.missingReferencedProductKeys = missingReferencedProductKeys;
    }

    public ProductWithUnResolvedProductReferences() {
    }

    public String getProductKey() {
        return productKey;
    }

    public void setProductKey(final String productKey) {
        this.productKey = productKey;
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

    public void setMissingReferencedProductKeys(final Set<String> missingReferencedProductKeys) {
        this.missingReferencedProductKeys = missingReferencedProductKeys;
    }

}
