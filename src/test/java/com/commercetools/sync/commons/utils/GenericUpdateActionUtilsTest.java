package com.commercetools.sync.commons.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class GenericUpdateActionUtilsTest {

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
        final Category category = mock(Category.class);
        final Map<String, JsonNode> fieldsJsonMap = new HashMap<>();
        final UpdateAction<Category> updateAction = buildTypedSetCustomTypeUpdateAction("key",
                fieldsJsonMap, category).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("setCustomType");
        assertThat(updateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.categories.commands.updateactions.SetCustomType");
    }

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
        final Channel channel = mock(Channel.class);
        final Map<String, JsonNode> fieldsJsonMap = new HashMap<>();
        final UpdateAction<Channel> updateAction = buildTypedSetCustomTypeUpdateAction("key",
                fieldsJsonMap, channel).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("setCustomType");
        assertThat(updateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.channels.commands.updateactions.SetCustomType");
    }

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithNonHandledResource_ShouldNotBuildUpdateAction() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        final Map<String, JsonNode> fieldsJsonMap = new HashMap<>();
        final UpdateAction<Cart> updateAction = buildTypedSetCustomTypeUpdateAction("key", fieldsJsonMap,
                cart).orElse(null);

        assertThat(updateAction).isNull();
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
        final Category category = mock(Category.class);
        final UpdateAction<Category> updateAction = buildTypedRemoveCustomTypeUpdateAction(category).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("setCustomType");
        assertThat(updateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.categories.commands.updateactions.SetCustomType");
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
        final Channel channel = mock(Channel.class);
        final UpdateAction<Channel> updateAction = buildTypedRemoveCustomTypeUpdateAction(channel).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("setCustomType");
        assertThat(updateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.channels.commands.updateactions.SetCustomType");
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithNonHandledResource_ShouldNotBuildUpdateAction() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        final UpdateAction<Cart> updateAction = buildTypedRemoveCustomTypeUpdateAction(cart).orElse(null);

        assertThat(updateAction).isNull();
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
        final Category category = mock(Category.class);
        final JsonNode customFieldValue = mock(JsonNode.class);
        final String customFieldName = "name";
        final UpdateAction<Category> updateAction = buildTypedSetCustomFieldUpdateAction(customFieldName,
                customFieldValue, category).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("setCustomField");
        assertThat(updateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.categories.commands.updateactions.SetCustomField");
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
        final Channel channel = mock(Channel.class);
        final JsonNode customFieldValue = mock(JsonNode.class);
        final String customFieldName = "name";
        final UpdateAction<Channel> updateAction = buildTypedSetCustomFieldUpdateAction(customFieldName,
                customFieldValue, channel).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("setCustomField");
        assertThat(updateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.channels.commands.updateactions.SetCustomField");
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithNonHandledResource_ShouldNotBuildUpdateAction() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        final JsonNode customFieldValue = mock(JsonNode.class);
        final String customFieldName = "name";
        final UpdateAction<Cart> updateAction =
                buildTypedSetCustomFieldUpdateAction(customFieldName, customFieldValue, cart).orElse(null);

        assertThat(updateAction).isNull();
    }

    @Test
    public void buildTypedUpdateAction_WithNonHandledCategoryUpdateAction_ShouldNotBuildUpdateAction() {
        final Category category = mock(Category.class);
        final String nonHandledUpdateActionName = "someUpdateActionName";
        final UpdateAction<Category> updateAction = buildTypedUpdateAction(category, nonHandledUpdateActionName)
                .orElse(null);

        assertThat(updateAction).isNull();
    }

    @Test
    public void buildTypedUpdateAction_WithNonHandledChannelUpdateAction_ShouldNotBuildUpdateAction() {
        final Channel channel = mock(Channel.class);
        final String nonHandledUpdateActionName = "someUpdateActionName";
        final UpdateAction<Channel> updateAction = buildTypedUpdateAction(channel, nonHandledUpdateActionName)
                .orElse(null);

        assertThat(updateAction).isNull();
    }
}
