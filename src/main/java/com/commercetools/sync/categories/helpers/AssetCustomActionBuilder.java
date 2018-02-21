package com.commercetools.sync.categories.helpers;


import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.SetAssetCustomField;
import io.sphere.sdk.categories.commands.updateactions.SetAssetCustomType;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

import static io.sphere.sdk.types.CustomFieldsDraft.ofTypeIdAndJson;

public class AssetCustomActionBuilder implements GenericCustomActionBuilder<Category> {

    @Override
    @Nonnull
    public UpdateAction<Category> buildRemoveCustomTypeAction(@Nullable final Integer variantId,
                                                              @Nullable final String assetKey) {
        return SetAssetCustomType.ofKey(assetKey, null);
    }

    @Override
    @Nonnull
    public UpdateAction<Category> buildSetCustomTypeAction(@Nullable final Integer variantId,
                                                           @Nullable final String assetKey,
                                                           @Nonnull final String customTypeId,
                                                           @Nullable final Map<String, JsonNode> customFieldsJsonMap) {
        return SetAssetCustomType.ofKey(assetKey, ofTypeIdAndJson(customTypeId, customFieldsJsonMap));
    }

    @Override
    @Nonnull
    public UpdateAction<Category> buildSetCustomFieldAction(@Nullable final Integer variantId,
                                                            @Nullable final String assetKey,
                                                            @Nullable final String customFieldName,
                                                            @Nullable final JsonNode customFieldValue) {
        return SetAssetCustomField.ofJsonValueWithKey(assetKey, customFieldName, customFieldValue);
    }
}
