package com.commercetools.sync.channels.helpers;

import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.commands.updateactions.SetCustomField;
import io.sphere.sdk.channels.commands.updateactions.SetCustomType;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;


public class ChannelCustomActionBuilder extends GenericCustomActionBuilder<Channel> {

    @Override
    public Optional<UpdateAction<Channel>> buildRemoveCustomTypeAction() {
        return Optional.of(SetCustomType.ofRemoveType());
    }

    @Override
    public Optional<UpdateAction<Channel>> buildSetCustomTypeAction(@Nullable final String customTypeKey,
                                                                    @Nullable final Map<String, JsonNode> customFieldsJsonMap) {
        return Optional.of(SetCustomType.ofTypeKeyAndJson(customTypeKey, customFieldsJsonMap));
    }

    @Override
    public Optional<UpdateAction<Channel>> buildSetCustomFieldAction(@Nullable final String customFieldName,
                                                                     @Nullable final JsonNode customFieldValue) {
        return Optional.of(SetCustomField.ofJson(customFieldName, customFieldValue));
    }
}
