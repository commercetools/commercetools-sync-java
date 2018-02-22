package com.commercetools.sync.commons.utils;

import com.commercetools.sync.channels.helpers.ChannelCustomActionBuilder;
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

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
        final Channel channel = mock(Channel.class);
        final Map<String, JsonNode> fieldsJsonMap = new HashMap<>();

        final UpdateAction<Channel> updateAction =
            GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction("key",
                fieldsJsonMap, channel, new ChannelCustomActionBuilder(), null, Channel::getId,
                channelResource -> channelResource.toReference().getTypeId(),
                channelResource -> null,
                InventorySyncOptionsBuilder.of(mock(SphereClient.class)).build()).orElse(null);

        assertThat(updateAction).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildRemoveCustomTypeAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
        final UpdateAction<Channel> updateAction =
            new ChannelCustomActionBuilder().buildRemoveCustomTypeAction(null, null);

        assertThat(updateAction).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildSetCustomFieldAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
        final UpdateAction<Channel> updateAction =
            new ChannelCustomActionBuilder().buildSetCustomFieldAction(null, null, "name", mock(JsonNode.class));

        assertThat(updateAction).isInstanceOf(SetCustomField.class);
    }
}
