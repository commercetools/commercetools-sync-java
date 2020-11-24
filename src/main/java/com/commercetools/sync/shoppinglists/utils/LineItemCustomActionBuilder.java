package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetLineItemCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetLineItemCustomType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public final class LineItemCustomActionBuilder implements GenericCustomActionBuilder<ShoppingList> {

    @Nonnull
    @Override
    public UpdateAction<ShoppingList> buildRemoveCustomTypeAction(
        @Nullable final Integer variantId,
        @Nullable final String lineItemId) {

        return SetLineItemCustomType.ofRemoveType(lineItemId);
    }

    @Nonnull
    @Override
    public UpdateAction<ShoppingList> buildSetCustomTypeAction(
        @Nullable final Integer variantId,
        @Nullable final String lineItemId,
        @Nonnull final String customTypeId,
        @Nullable final Map<String, JsonNode> customFieldsJsonMap) {

        return SetLineItemCustomType.ofTypeIdAndJson(customTypeId, customFieldsJsonMap, lineItemId);
    }

    @Nonnull
    @Override
    public UpdateAction<ShoppingList> buildSetCustomFieldAction(
        @Nullable final Integer variantId,
        @Nullable final String lineItemId,
        @Nullable final String customFieldName,
        @Nullable final JsonNode customFieldValue) {

        return SetLineItemCustomField.ofJson(customFieldName, customFieldValue, lineItemId);
    }
}
