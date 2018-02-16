package com.commercetools.sync.commons.utils;

import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.commands.updateactions.SetCustomField;
import io.sphere.sdk.channels.commands.updateactions.SetCustomType;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ChannelCustomUpdateActionUtilsTest {
    private InventorySyncOptions syncOptions = InventorySyncOptionsBuilder.of(mock(SphereClient.class)).build();

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
        final Channel channel = mock(Channel.class);
        final Map<String, JsonNode> fieldsJsonMap = new HashMap<>();

        final UpdateAction<Channel> updateAction =
            GenericUpdateActionUtils.<Channel, Channel>buildTypedSetCustomTypeUpdateAction("key",
                fieldsJsonMap, channel, null, null, Channel::getId,
                channelResource -> channelResource.toReference().getTypeId(),
                channelResource -> null,
                syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
        final Channel channel = mock(Channel.class);
        final UpdateAction<Channel> updateAction =
            GenericUpdateActionUtils.<Channel, Channel>buildTypedRemoveCustomTypeUpdateAction(channel, null, null,
                Channel::getId, channelResource -> channelResource.toReference().getTypeId(),
                channelResource -> null, syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
        final Channel channel = mock(Channel.class);
        final JsonNode customFieldValue = mock(JsonNode.class);
        final String customFieldName = "name";
        final UpdateAction<Channel> updateAction =
            GenericUpdateActionUtils.<Channel, Channel>buildTypedSetCustomFieldUpdateAction(customFieldName,
                customFieldValue, channel, null, null, Channel::getId,
                channelResource -> channelResource.toReference().getTypeId(),
                channelResource -> null,
                syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetCustomField.class);
    }
}
