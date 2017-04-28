package com.commercetools.sync.commons.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.helpers.CtpClient;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GenericUpdateActionUtilsTest {
    private CategorySyncOptions categorySyncOptions;
    private final CtpClient ctpClient = mock(CtpClient.class);

    @Before
    public void setup() {
        final SphereClientConfig clientConfig = SphereClientConfig.of("testPK", "testCI", "testCS");
        when(ctpClient.getClientConfig()).thenReturn(clientConfig);
        categorySyncOptions = CategorySyncOptionsBuilder.of(ctpClient)
                .build();
    }

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
        final Category category = mock(Category.class);
        final Map<String, JsonNode> fieldsJsonMap = new HashMap<>();
        final UpdateAction<Category> updateAction = buildTypedSetCustomTypeUpdateAction("key",
                fieldsJsonMap, category, categorySyncOptions).orElse(null);

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
                fieldsJsonMap, channel, categorySyncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("setCustomType");
        assertThat(updateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.channels.commands.updateactions.SetCustomType");
    }

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithNonHandledResource_ShouldNotBuildUpdateAction() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        when(cart.toReference()).thenReturn(Reference.of(Cart.referenceTypeId(), "cartId"));

        final Map<String, JsonNode> fieldsJsonMap = new HashMap<>();
        final UpdateAction<Cart> updateAction = buildTypedSetCustomTypeUpdateAction("key", fieldsJsonMap,
                cart, categorySyncOptions).orElse(null);

        assertThat(updateAction).isNull();
    }

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithNonHandledResource_ShouldCallSyncOptionsCallBack() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn("cartId");
        when(cart.toReference()).thenReturn(Reference.of(Cart.referenceTypeId(), "cartId"));

        final Map<String, JsonNode> fieldsJsonMap = new HashMap<>();

        // Mock custom options error callback
        final ArrayList<Object> callBackResponses = new ArrayList<>();
        final BiConsumer<String, Throwable> updateActionErrorCallBack = (errorMessage, exception) -> {
            callBackResponses.add(errorMessage);
            callBackResponses.add(exception);
        };

        // Mock sync options
        final CategorySyncOptions syncOptions = CategorySyncOptionsBuilder.of(mock(CtpClient.class))
                .setErrorCallBack(updateActionErrorCallBack)
                .build();

        final UpdateAction<Cart> updateAction = buildTypedSetCustomTypeUpdateAction("key", fieldsJsonMap,
                cart, syncOptions).orElse(null);

        assertThat(updateAction).isNull();
        assertThat(callBackResponses).hasSize(2);
        assertThat(callBackResponses.get(0)).isEqualTo("Failed to build 'setCustomType' update action on the cart with " +
                "id 'cartId'. Reason: Update actions for resource: 'cart' is not implemented.");
        assertThat((Exception) callBackResponses.get(1)).isInstanceOf(BuildUpdateActionException.class);
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
        final Category category = mock(Category.class);
        final UpdateAction<Category> updateAction = buildTypedRemoveCustomTypeUpdateAction(category,
                categorySyncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("setCustomType");
        assertThat(updateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.categories.commands.updateactions.SetCustomType");
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithChannelResource_ShouldBuildChannelUpdateAction() {
        final Channel channel = mock(Channel.class);
        final UpdateAction<Channel> updateAction = buildTypedRemoveCustomTypeUpdateAction(channel,
                categorySyncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("setCustomType");
        assertThat(updateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.channels.commands.updateactions.SetCustomType");
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithNonHandledResource_ShouldNotBuildUpdateAction() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        when(cart.toReference()).thenReturn(Reference.of(Cart.referenceTypeId(), "cartId"));

        final UpdateAction<Cart> updateAction = buildTypedRemoveCustomTypeUpdateAction(cart,
                categorySyncOptions).orElse(null);

        assertThat(updateAction).isNull();
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithNonHandledResource_ShouldCallSyncOptionsCallBack() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn("cartId");
        when(cart.toReference()).thenReturn(Reference.of(Cart.referenceTypeId(), "cartId"));

        final Map<String, JsonNode> fieldsJsonMap = new HashMap<>();

        // Mock custom options error callback
        final ArrayList<Object> callBackResponses = new ArrayList<>();
        final BiConsumer<String, Throwable> updateActionErrorCallBack = (errorMessage, exception) -> {
            callBackResponses.add(errorMessage);
            callBackResponses.add(exception);
        };

        // Mock sync options
        final CategorySyncOptions syncOptions = CategorySyncOptionsBuilder.of(mock(CtpClient.class))
                .setErrorCallBack(updateActionErrorCallBack)
                .build();

        final UpdateAction<Cart> updateAction = buildTypedRemoveCustomTypeUpdateAction(cart,
                syncOptions).orElse(null);

        assertThat(updateAction).isNull();
        assertThat(callBackResponses).hasSize(2);
        assertThat(callBackResponses.get(0)).isEqualTo("Failed to build 'setCustomType' update action to remove the" +
                " custom type on the cart with id 'cartId'. Reason: Update actions for " +
                "resource: 'cart' is not implemented.");
        assertThat((Exception) callBackResponses.get(1)).isInstanceOf(BuildUpdateActionException.class);
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
        final Category category = mock(Category.class);
        final JsonNode customFieldValue = mock(JsonNode.class);
        final String customFieldName = "name";
        final UpdateAction<Category> updateAction = buildTypedSetCustomFieldUpdateAction(customFieldName,
                customFieldValue, category, categorySyncOptions).orElse(null);

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
                customFieldValue, channel, categorySyncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("setCustomField");
        assertThat(updateAction.getClass().getName())
                .isEqualTo("io.sphere.sdk.channels.commands.updateactions.SetCustomField");
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithNonHandledResource_ShouldNotBuildUpdateAction() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn("cartId");
        when(cart.toReference()).thenReturn(Reference.of(Cart.referenceTypeId(), "cartId"));

        final JsonNode customFieldValue = mock(JsonNode.class);
        final String customFieldName = "name";

        final UpdateAction<Cart> updateAction =
                buildTypedSetCustomFieldUpdateAction(customFieldName, customFieldValue, cart,
                        mock(BaseSyncOptions.class)).orElse(null);

        assertThat(updateAction).isNull();
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithNonHandledResource_ShouldCallSyncOptionsCallBack() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn("cartId");
        when(cart.toReference()).thenReturn(Reference.of(Cart.referenceTypeId(), "cartId"));

        final JsonNode customFieldValue = mock(JsonNode.class);
        final String customFieldName = "name";
        final ArrayList<Object> callBackResponses = new ArrayList<>();
        final BiConsumer<String, Throwable> updateActionErrorCallBack = (errorMessage, exception) -> {
            callBackResponses.add(errorMessage);
            callBackResponses.add(exception);
        };

        final BaseSyncOptions baseSyncOptions = CategorySyncOptionsBuilder.of(mock(CtpClient.class))
                .setErrorCallBack(updateActionErrorCallBack)
                .build();

        final UpdateAction<Cart> updateAction =
                buildTypedSetCustomFieldUpdateAction(customFieldName, customFieldValue, cart,
                        baseSyncOptions).orElse(null);

        assertThat(updateAction).isNull();
        assertThat(callBackResponses).hasSize(2);
        assertThat(callBackResponses.get(0)).isEqualTo("Failed to build 'setCustomField' update action on the custom " +
                "field with the name 'name' on the cart with id 'cartId'. Reason: Update actions for resource:" +
                " 'cart' is not implemented.");
        assertThat((Exception) callBackResponses.get(1)).isInstanceOf(BuildUpdateActionException.class);
    }
}
