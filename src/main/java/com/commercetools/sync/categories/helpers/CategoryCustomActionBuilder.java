package com.commercetools.sync.categories.helpers;


import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.SetCustomField;
import io.sphere.sdk.categories.commands.updateactions.SetCustomType;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public class CategoryCustomActionBuilder extends GenericCustomActionBuilder<Category> {
    @Override
    public Optional<UpdateAction<Category>> buildRemoveCustomTypeAction() {
        return Optional.of(SetCustomType.ofRemoveType());
    }

    @Override
    public Optional<UpdateAction<Category>> buildSetCustomTypeAction(@Nullable final String customTypeKey,
                                                                     @Nullable final Map<String, JsonNode>
                                                                         customFieldsJsonMap) {
        return Optional.of(SetCustomType.ofTypeKeyAndJson(customTypeKey, customFieldsJsonMap));
    }

    @Override
    public Optional<UpdateAction<Category>> buildSetCustomFieldAction(@Nullable final String customFieldName,
                                                                      @Nullable final JsonNode customFieldValue) {
        return Optional.of(SetCustomField.ofJson(customFieldName, customFieldValue));
    }
}
