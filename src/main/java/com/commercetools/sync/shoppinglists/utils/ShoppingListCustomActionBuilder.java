package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public final class ShoppingListCustomActionBuilder implements GenericCustomActionBuilder<ShoppingList> {

    private static final ShoppingListCustomActionBuilder builder = new ShoppingListCustomActionBuilder();

    private ShoppingListCustomActionBuilder() {
        super();
    }

    @Nonnull
    public static ShoppingListCustomActionBuilder of() {
        return builder;
    }

    @Nonnull
    @Override
    public UpdateAction<ShoppingList> buildRemoveCustomTypeAction(
        @Nullable final Integer variantId,
        @Nullable final String objectId) {

        return SetCustomType.ofRemoveType();
    }

    @Nonnull
    @Override
    public UpdateAction<ShoppingList> buildSetCustomTypeAction(
        @Nullable final Integer variantId,
        @Nullable final String objectId,
        @Nonnull final String customTypeId,
        @Nullable final Map<String, JsonNode> customFieldsJsonMap) {

        return SetCustomType.ofTypeIdAndJson(customTypeId, customFieldsJsonMap);
    }

    @Nonnull
    @Override
    public UpdateAction<ShoppingList> buildSetCustomFieldAction(
        @Nullable final Integer variantId,
        @Nullable final String objectId,
        @Nullable final String customFieldName,
        @Nullable final JsonNode customFieldValue) {

        return SetCustomField.ofJson(customFieldName, customFieldValue);
    }
}
