package com.commercetools.sync.commons.utils;

import com.commercetools.sync.commons.helpers.SyncResult;
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
        final SyncResult<Category> syncResult = buildTypedSetCustomTypeUpdateAction("key",
                fieldsJsonMap, category);

        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);

        final UpdateAction<Category> categoryUpdateAction = syncResult.getUpdateActions().get(0);
        assertThat(categoryUpdateAction.getAction()).isEqualTo("setCustomType");
        assertThat(categoryUpdateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.categories.commands.updateactions.SetCustomType");
    }

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
        final Channel channel = mock(Channel.class);
        final Map<String, JsonNode> fieldsJsonMap = new HashMap<>();
        final SyncResult<Channel> syncResult = buildTypedSetCustomTypeUpdateAction("key",
                fieldsJsonMap, channel);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);

        final UpdateAction<Channel> channelUpdateAction = syncResult.getUpdateActions().get(0);
        assertThat(channelUpdateAction.getAction()).isEqualTo("setCustomType");
        assertThat(channelUpdateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.channels.commands.updateactions.SetCustomType");
    }

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithNonHandledResource_ShouldNotBuildUpdateAction() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        final Map<String, JsonNode> fieldsJsonMap = new HashMap<>();
        final SyncResult<Cart> syncResult = buildTypedSetCustomTypeUpdateAction("key", fieldsJsonMap,
                cart);

        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
        final Category category = mock(Category.class);
        final SyncResult<Category> syncResult = buildTypedRemoveCustomTypeUpdateAction(category);
        assertThat(syncResult).isNotNull();

        final UpdateAction<Category> categoryUpdateAction = syncResult.getUpdateActions().get(0);
        assertThat(categoryUpdateAction.getAction()).isEqualTo("setCustomType");
        assertThat(categoryUpdateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.categories.commands.updateactions.SetCustomType");
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
        final Channel channel = mock(Channel.class);
        final SyncResult<Channel> syncResult = buildTypedRemoveCustomTypeUpdateAction(channel);
        assertThat(syncResult).isNotNull();

        final UpdateAction<Channel> channelUpdateAction = syncResult.getUpdateActions().get(0);
        assertThat(channelUpdateAction.getAction()).isEqualTo("setCustomType");
        assertThat(channelUpdateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.channels.commands.updateactions.SetCustomType");
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithNonHandledResource_ShouldNotBuildUpdateAction() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        final SyncResult<Cart> syncResult = buildTypedRemoveCustomTypeUpdateAction(cart);

        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
        final Category category = mock(Category.class);
        final JsonNode customFieldValue = mock(JsonNode.class);
        final String customFieldName = "name";
        final SyncResult<Category> syncResult = buildTypedSetCustomFieldUpdateAction(customFieldName,
                customFieldValue, category);
        assertThat(syncResult).isNotNull();

        final UpdateAction<Category> categoryUpdateAction = syncResult.getUpdateActions().get(0);
        assertThat(categoryUpdateAction.getAction()).isEqualTo("setCustomField");
        assertThat(categoryUpdateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.categories.commands.updateactions.SetCustomField");
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
        final Channel channel = mock(Channel.class);
        final JsonNode customFieldValue = mock(JsonNode.class);
        final String customFieldName = "name";
        final SyncResult<Channel> syncResult = buildTypedSetCustomFieldUpdateAction(customFieldName,
                customFieldValue, channel);
        assertThat(syncResult).isNotNull();

        final UpdateAction<Channel> channelUpdateAction = syncResult.getUpdateActions().get(0);
        assertThat(channelUpdateAction.getAction()).isEqualTo("setCustomField");
        assertThat(channelUpdateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.channels.commands.updateactions.SetCustomField");
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithNonHandledResource_ShouldNotBuildUpdateAction() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        final JsonNode customFieldValue = mock(JsonNode.class);
        final String customFieldName = "name";
        final SyncResult<Cart> syncResult =
                buildTypedSetCustomFieldUpdateAction(customFieldName, customFieldValue, cart);

        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildTypedUpdateAction_WithNonHandledCategoryUpdateAction_ShouldNotBuildUpdateAction() {
        final Category category = mock(Category.class);
        final String nonHandledUpdateActionName = "someUpdateActionName";
        final SyncResult<Category> syncResult = buildTypedUpdateAction(category, nonHandledUpdateActionName);

        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildTypedUpdateAction_WithNonHandledChannelUpdateAction_ShouldNotBuildUpdateAction() {
        final Channel channel = mock(Channel.class);
        final String nonHandledUpdateActionName = "someUpdateActionName";
        final SyncResult<Channel> syncResult = buildTypedUpdateAction(channel, nonHandledUpdateActionName);

        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }
}
