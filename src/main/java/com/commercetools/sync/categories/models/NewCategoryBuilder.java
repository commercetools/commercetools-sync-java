package com.commercetools.sync.categories.models;


import com.commercetools.sync.commons.models.NewResourceBuilder;
import io.sphere.sdk.categories.CategoryDraft;

import javax.annotation.Nonnull;

public class NewCategoryBuilder extends NewResourceBuilder<CategoryDraft, NewCategory> {
    private String parentCategoryExternalId;
    private String customTypeKey;

    private NewCategoryBuilder(@Nonnull final CategoryDraft categoryDraft) {
        this.resourceDraft = categoryDraft;
    }

    public static NewCategoryBuilder of(@Nonnull final CategoryDraft categoryDraft) {
        return new NewCategoryBuilder(categoryDraft);
    }

    public NewCategoryBuilder setParentCategoryExternalId(@Nonnull final String parentCategoryExternalId) {
        this.parentCategoryExternalId = parentCategoryExternalId;
        return this;
    }

    public NewCategoryBuilder setCustomTypeKey(@Nonnull final String customTypeKey) {
        this.customTypeKey = customTypeKey;
        return this;
    }

    @Override
    public NewCategory build() {
        return new NewCategory(this.resourceDraft, this.parentCategoryExternalId, this.customTypeKey);
    }
}
