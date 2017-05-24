package com.commercetools.sync.categories.models;

import com.commercetools.sync.commons.models.OldResourceBuilder;
import io.sphere.sdk.categories.Category;

import javax.annotation.Nonnull;

public class OldCategoryBuilder extends OldResourceBuilder<Category, OldCategory> {
    private String parentCategoryExternalId;
    private String customTypeKey;

    private OldCategoryBuilder(@Nonnull final Category category) {
        this.resource = category;
    }

    public static OldCategoryBuilder of(@Nonnull final Category category) {
        return new OldCategoryBuilder(category);
    }

    public OldCategoryBuilder setParentCategoryExternalId(@Nonnull final String parentCategoryExternalId) {
        this.parentCategoryExternalId = parentCategoryExternalId;
        return this;
    }

    public OldCategoryBuilder setCustomTypeKey(@Nonnull final String customTypeKey) {
        this.customTypeKey = customTypeKey;
        return this;
    }

    @Override
    protected OldCategory build() {
        return new OldCategory(this.resource, this.parentCategoryExternalId, this.customTypeKey);
    }
}
