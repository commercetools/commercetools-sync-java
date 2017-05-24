package com.commercetools.sync.categories.models;


import com.commercetools.sync.commons.models.NewResource;
import io.sphere.sdk.categories.CategoryDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NewCategory extends NewResource<CategoryDraft> {
    private String parentCategoryExternalId;
    private String customTypeKey;

    NewCategory(@Nonnull final CategoryDraft categoryDraft,
                @Nullable final String parentCategoryExternalId,
                @Nullable final String customTypeKey) {
        super(categoryDraft);
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
