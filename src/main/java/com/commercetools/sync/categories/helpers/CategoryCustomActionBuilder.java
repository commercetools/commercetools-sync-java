package com.commercetools.sync.categories.helpers;


import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.SetCustomField;
import io.sphere.sdk.categories.commands.updateactions.SetCustomType;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public class CategoryCustomActionBuilder extends GenericCustomActionBuilder<Category> {
    @Nonnull
    @Override
    public UpdateAction<Category> buildRemoveCustomTypeAction() {
        return SetCustomType.ofRemoveType();
    }

    @Nonnull
    @Override
    public UpdateAction<Category> buildSetCustomTypeAction(@Nullable final String customTypeKey,
                                                                     @Nullable final Map<String, JsonNode> customFieldsJsonMap) {
        return SetCustomType.ofTypeKeyAndJson(customTypeKey, customFieldsJsonMap);
    }

    @Nonnull
    @Override
    public UpdateAction<Category> buildSetCustomFieldAction(@Nullable final String customFieldName,
                                                                      @Nullable final JsonNode customFieldValue) {
        return SetCustomField.ofJson(customFieldName, customFieldValue);
    }
}
