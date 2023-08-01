package com.commercetools.sync.sdk2.commons.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.channel.*;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.channels.helpers.ChannelCustomActionBuilder;
import com.commercetools.sync.sdk2.channels.models.ChannelCustomTypeAdapter;
import com.commercetools.sync.sdk2.commons.models.Custom;
import com.commercetools.sync.sdk2.inventories.InventorySyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChannelCustomUpdateActionUtilsTest {

  @Test
  void buildTypedSetCustomTypeUpdateAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
    final String customTypeId = UUID.randomUUID().toString();

    final ChannelUpdateAction updateAction =
        GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
                customTypeId,
                new HashMap<>(),
                ChannelCustomTypeAdapter.of(mock(Channel.class)),
                new ChannelCustomActionBuilder(),
                null,
                ChannelCustomTypeAdapter::getId,
                Custom::getTypeId,
                channelResource -> null,
                InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build())
            .orElse(null);

    assertThat(updateAction).isInstanceOf(ChannelSetCustomTypeAction.class);

    assertThat((ChannelSetCustomTypeAction) updateAction)
        .satisfies(
            action -> {
              assertThat(action.getType())
                  .isEqualTo(TypeResourceIdentifierBuilder.of().id(customTypeId).build());
              assertThat(action.getFields().values()).isEmpty();
            });
  }

  @Test
  void buildRemoveCustomTypeAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
    final ChannelUpdateAction updateAction =
        new ChannelCustomActionBuilder().buildRemoveCustomTypeAction(null, null);

    assertThat(updateAction).isInstanceOf(ChannelSetCustomTypeAction.class);
    assertThat((ChannelSetCustomTypeAction) updateAction)
        .satisfies(
            action -> {
              assertThat(action.getType()).isNull();
              assertThat(action.getFields()).isNull();
            });
  }

  @Test
  void buildSetCustomFieldAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
    final String customFieldName = "name";
    final JsonNode customFieldValue = JsonNodeFactory.instance.textNode("foo");

    final ChannelUpdateAction updateAction =
        new ChannelCustomActionBuilder()
            .buildSetCustomFieldAction(null, null, customFieldName, customFieldValue);

    assertThat(updateAction).isInstanceOf(ChannelSetCustomFieldAction.class);
    assertThat((ChannelSetCustomFieldAction) updateAction)
        .satisfies(
            action -> {
              assertThat(action.getName()).isEqualTo(customFieldName);
              assertThat(action.getValue()).isEqualTo(customFieldValue);
            });
  }
}
