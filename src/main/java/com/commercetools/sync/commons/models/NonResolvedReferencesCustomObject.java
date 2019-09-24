package com.commercetools.sync.commons.models;

import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import java.util.List;

public class NonResolvedReferencesCustomObject {

    private String productKey;

    private ProductDraft productDraft;

    private List<String> dependantProductKeys;

    /**
     * Object represent the value for the custom object that will hold unresolved references
     * @param productKey product key that has non-resolved references
     * @param productDraft product draft which has non-resolved references
     * @param dependantProductKeys product keys of non-resolved references
     */
    public NonResolvedReferencesCustomObject(@Nonnull final String productKey, @Nonnull final ProductDraft productDraft,
                                             final List<String> dependantProductKeys) {
        this.productKey = productKey;
        this.productDraft = productDraft;
        this.dependantProductKeys = dependantProductKeys;
    }

    public NonResolvedReferencesCustomObject() {
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

    public List<String> getDependantProductKeys() {
        return dependantProductKeys;
    }

    public void setDependantProductKeys(final List<String> dependantProductKeys) {
        this.dependantProductKeys = dependantProductKeys;
    }

}
