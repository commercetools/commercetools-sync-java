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
    public Optional<UpdateAction<InventoryEntry>> buildRemoveCustomTypeAction() {
        return Optional.of(SetCustomType.ofRemoveType());
    }

    @Override
    public Optional<UpdateAction<InventoryEntry>> buildSetCustomTypeAction(@Nullable String customTypeKey,
                                                                           @Nullable Map<String, JsonNode> customFieldsJsonMap) {
        return Optional.of(SetCustomType.ofTypeKeyAndJson(customTypeKey, customFieldsJsonMap));
    }

    @Override
    public Optional<UpdateAction<InventoryEntry>> buildSetCustomFieldAction(@Nullable String customFieldName,
                                                                            @Nullable JsonNode customFieldValue) {
        return Optional.of(SetCustomField.ofJson(customFieldName, customFieldValue));
    }
}
