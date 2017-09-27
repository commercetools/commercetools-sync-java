package com.commercetools.sync.inventories.helpers;

import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.commands.updateactions.SetCustomField;
import io.sphere.sdk.inventory.commands.updateactions.SetCustomType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class InventoryCustomActionBuilder extends GenericCustomActionBuilder<InventoryEntry> {

    @Nonnull
    @Override
    public UpdateAction<InventoryEntry> buildRemoveCustomTypeAction() {
        return SetCustomType.ofRemoveType();
    }

    @Nonnull
    @Override
    public UpdateAction<InventoryEntry> buildSetCustomTypeAction(@Nullable final String customTypeId,
                                                                 @Nullable final Map<String, JsonNode>
                                                                     customFieldsJsonMap) {
        return SetCustomType.ofTypeIdAndJson(customTypeId, customFieldsJsonMap);
    }

    @Nonnull
    @Override
    public UpdateAction<InventoryEntry> buildSetCustomFieldAction(@Nullable final String customFieldName,
                                                                  @Nullable final JsonNode customFieldValue) {
        return SetCustomField.ofJson(customFieldName, customFieldValue);
    }
}
