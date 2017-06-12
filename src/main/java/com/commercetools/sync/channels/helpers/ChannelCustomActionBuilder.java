package com.commercetools.sync.channels.helpers;

import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.commands.updateactions.SetCustomField;
import io.sphere.sdk.channels.commands.updateactions.SetCustomType;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;


public class ChannelCustomActionBuilder extends GenericCustomActionBuilder<Channel> {

    @Nonnull
    @Override
    public UpdateAction<Channel> buildRemoveCustomTypeAction() {
        return SetCustomType.ofRemoveType();
    }

    @Nonnull
    @Override

    public UpdateAction<Channel> buildSetCustomTypeAction(@Nullable final String customTypeId,
                                                          @Nullable final Map<String, JsonNode> customFieldsJsonMap) {
        return SetCustomType.ofTypeIdAndJson(customTypeId, customFieldsJsonMap);
    }

    @Nonnull
    @Override
    public UpdateAction<Channel> buildSetCustomFieldAction(@Nullable final String customFieldName,
                                                           @Nullable final JsonNode customFieldValue) {
        return SetCustomField.ofJson(customFieldName, customFieldValue);
    }
}
