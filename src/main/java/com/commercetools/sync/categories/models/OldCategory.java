package com.commercetools.sync.categories.models;

import com.commercetools.sync.commons.models.OldResource;
import io.sphere.sdk.categories.Category;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OldCategory extends OldResource<Category>{
    private String parentCategoryExternalId;
    private String customTypeKey;

    OldCategory(@Nonnull final Category category,
                @Nullable final String parentCategoryExternalId,
                @Nullable final String customTypeKey) {
        super(category);
        this.parentCategoryExternalId = parentCategoryExternalId;
        this.customTypeKey = customTypeKey;
    }

    public String getParentCategoryExternalId() {
        return parentCategoryExternalId;
    }

    public String getCustomTypeKey() {
        return customTypeKey;
    }
}
