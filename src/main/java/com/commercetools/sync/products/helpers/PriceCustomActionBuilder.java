package com.commercetools.sync.products.helpers;


import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomField;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class PriceCustomActionBuilder implements GenericCustomActionBuilder<Product> {

    @Override
    @Nonnull
    public UpdateAction<Product> buildRemoveCustomTypeAction(@Nullable final Integer variantId,
                                                             @Nullable final String priceId) {
        return SetProductPriceCustomType.ofRemoveType(priceId, true);
    }

    @Override
    @Nonnull
    public UpdateAction<Product> buildSetCustomTypeAction(@Nullable final Integer variantId,
                                                          @Nullable final String priceId,
                                                          @Nonnull final String customTypeId,
                                                          @Nullable final Map<String, JsonNode> customFieldsJsonMap) {
        return SetProductPriceCustomType.ofTypeIdAndJson(customTypeId, customFieldsJsonMap, priceId, true);
    }

    @Override
    @Nonnull
    public UpdateAction<Product> buildSetCustomFieldAction(@Nullable final Integer variantId,
                                                           @Nullable final String priceId,
                                                           @Nullable final String customFieldName,
                                                           @Nullable final JsonNode customFieldValue) {
        return SetProductPriceCustomField.ofJson(customFieldName, customFieldValue, priceId, true);
    }
}
