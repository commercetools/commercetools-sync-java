package com.commercetools.sync.commons.utils;

import static com.commercetools.sync.commons.asserts.actions.AssertionsForUpdateActions.assertThat;
import static io.sphere.sdk.models.ResourceIdentifier.ofId;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.sync.channels.helpers.ChannelCustomActionBuilder;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.commands.updateactions.SetCustomField;
import io.sphere.sdk.channels.commands.updateactions.SetCustomType;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChannelCustomUpdateActionUtilsTest {

  @Test
  void buildTypedSetCustomTypeUpdateAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
    final Channel channel = mock(Channel.class);
    final Map<String, JsonNode> fieldsJsonMap = new HashMap<>();
    final String customTypeId = UUID.randomUUID().toString();

    final UpdateAction<Channel> updateAction =
        GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
                customTypeId,
                fieldsJsonMap,
                channel,
                new ChannelCustomActionBuilder(),
                null,
                Channel::getId,
                channelResource -> channelResource.toReference().getTypeId(),
                channelResource -> null,
                InventorySyncOptionsBuilder.of(mock(SphereClient.class)).build())
            .orElse(null);

    assertThat(updateAction).isInstanceOf(SetCustomType.class);
    assertThat((SetCustomType) updateAction)
        .hasValues("setCustomType", emptyMap(), ofId(customTypeId));
  }

  @Test
  void buildRemoveCustomTypeAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
    final UpdateAction<Channel> updateAction =
        new ChannelCustomActionBuilder().buildRemoveCustomTypeAction(null, null);

    assertThat(updateAction).isInstanceOf(SetCustomType.class);
    assertThat((SetCustomType) updateAction).hasValues("setCustomType", null, ofId(null));
  }

  @Test
  void buildSetCustomFieldAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
    final String customFieldName = "name";
    final JsonNode customFieldValue = JsonNodeFactory.instance.textNode("foo");

    final UpdateAction<Channel> updateAction =
        new ChannelCustomActionBuilder()
            .buildSetCustomFieldAction(null, null, customFieldName, customFieldValue);

    assertThat(updateAction).isInstanceOf(SetCustomField.class);
    assertThat((SetCustomField) updateAction)
        .hasValues("setCustomField", customFieldName, customFieldValue);
  }
}
