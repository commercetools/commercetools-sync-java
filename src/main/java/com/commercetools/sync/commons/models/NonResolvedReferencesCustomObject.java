package com.commercetools.sync.commons.models;

import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.List;

public class NonResolvedReferencesCustomObject implements Resource {

    @Nonnull
    private String productKey;

    @Nonnull
    private ProductDraft productDraft;

    private List<String> dependantProductKeys;

    public String getProductKey() {
        return productKey;
    }

    public void setProductKey(String productKey) {
        this.productKey = productKey;
    }

    public ProductDraft getProductDraft() {
        return productDraft;
    }

    public void setProductDraft(ProductDraft productDraft) {
        this.productDraft = productDraft;
    }

    public List<String> getDependantProductKeys() {
        return dependantProductKeys;
    }

    public void setDependantProductKeys(List<String> dependantProductKeys) {
        this.dependantProductKeys = dependantProductKeys;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public Long getVersion() {
        return null;
    }

    @Override
    public ZonedDateTime getCreatedAt() {
        return null;
    }

    @Override
    public ZonedDateTime getLastModifiedAt() {
        return null;
    }

    @Override
    public Reference toReference() {
        return null;
    }
}
