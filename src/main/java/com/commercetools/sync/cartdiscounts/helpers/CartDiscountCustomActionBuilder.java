package com.commercetools.sync.cartdiscounts.helpers;

import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetCustomField;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetCustomType;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class CartDiscountCustomActionBuilder implements GenericCustomActionBuilder<CartDiscount> {

    @Nonnull
    @Override
    public UpdateAction<CartDiscount> buildRemoveCustomTypeAction(@Nullable final Integer variantId,
                                                                  @Nullable final String objectId) {
        return SetCustomType.ofRemoveType();
    }

    @Nonnull
    @Override
    public UpdateAction<CartDiscount> buildSetCustomTypeAction(
            @Nullable final Integer variantId,
            @Nullable final String objectId,
            @Nonnull final String customTypeId,
            @Nullable final Map<String, JsonNode> customFieldsJsonMap) {
        return SetCustomType.ofTypeIdAndJson(customTypeId, customFieldsJsonMap);
    }

    @Nonnull
    @Override
    public UpdateAction<CartDiscount> buildSetCustomFieldAction(@Nullable final Integer variantId,
                                                                @Nullable final String objectId,
                                                                @Nullable final String customFieldName,
                                                                @Nullable final JsonNode customFieldValue) {
        return SetCustomField.ofJson(customFieldName, customFieldValue);
    }
}
