package com.commercetools.sync.inventory.helpers;

import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.commands.updateactions.SetCustomField;
import io.sphere.sdk.inventory.commands.updateactions.SetCustomType;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public class InventoryCustomActionBuilder extends GenericCustomActionBuilder<InventoryEntry> {

    @Override
    public UpdateAction<InventoryEntry> buildRemoveCustomTypeAction() {
        return SetCustomType.ofRemoveType();
    }

    @Override
    public UpdateAction<InventoryEntry> buildSetCustomTypeAction(@Nullable final String customTypeKey,
                                                                           @Nullable final Map<String, JsonNode> customFieldsJsonMap) {
        return SetCustomType.ofTypeKeyAndJson(customTypeKey, customFieldsJsonMap);
    }

    @Override
    public UpdateAction<InventoryEntry> buildSetCustomFieldAction(@Nullable final String customFieldName,
                                                                            @Nullable final JsonNode customFieldValue) {
        return SetCustomField.ofJson(customFieldName, customFieldValue);
    }
}
