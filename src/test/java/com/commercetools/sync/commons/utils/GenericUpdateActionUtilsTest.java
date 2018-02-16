package com.commercetools.sync.commons.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GenericUpdateActionUtilsTest {
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT).build();

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithNonHandledResource_ShouldNotBuildUpdateAction() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        when(cart.toReference()).thenReturn(Cart.referenceOfId("cartId"));

        final Map<String, JsonNode> fieldsJsonMap = new HashMap<>();
        final UpdateAction<Cart> updateAction =
            GenericUpdateActionUtils.<Cart, Cart>buildTypedSetCustomTypeUpdateAction("key", fieldsJsonMap,
                cart, null,  null, Cart::getId,
                cartResource -> cartResource.toReference().getTypeId(), cartResource -> null,
                categorySyncOptions).orElse(null);

        assertThat(updateAction).isNull();
    }

    @Test
    public void buildTypedSetCustomTypeAction_WithNonHandledResourceAndNullContainer_ShouldCallSyncOptionsCallBack() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn("cartId");
        when(cart.toReference()).thenReturn(Cart.referenceOfId("cartId"));

        final Map<String, JsonNode> fieldsJsonMap = new HashMap<>();

        // Mock custom options error callback
        final ArrayList<Object> callBackResponses = new ArrayList<>();
        final BiConsumer<String, Throwable> updateActionErrorCallBack = (errorMessage, exception) -> {
            callBackResponses.add(errorMessage);
            callBackResponses.add(exception);
        };

        // Mock sync options
        final CategorySyncOptions syncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                                          .errorCallback(updateActionErrorCallBack)
                                                                          .build();

        final UpdateAction<Cart> updateAction =
            GenericUpdateActionUtils.<Cart, Cart>buildTypedSetCustomTypeUpdateAction("key", fieldsJsonMap,
                cart, null,  null, Cart::getId,
                cartResource -> cartResource.toReference().getTypeId(), cartResource -> null, syncOptions)
                .orElse(null);

        assertThat(updateAction).isNull();
        assertThat(callBackResponses).hasSize(2);
        assertThat(callBackResponses.get(0)).isInstanceOf(String.class);
        assertThat((String) callBackResponses.get(0)).matches(
            "Failed to build 'setCustomType' update action on the cart with "
                + "id 'cartId'. Reason: Update actions for resource: 'Cart.*' "
                + "and container resource: 'null' is not implemented.");
        assertThat((Exception) callBackResponses.get(1)).isInstanceOf(BuildUpdateActionException.class);
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithNonHandledResource_ShouldNotBuildUpdateAction() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        when(cart.toReference()).thenReturn(Cart.referenceOfId("cartId"));

        final UpdateAction<Cart> updateAction =
            GenericUpdateActionUtils.<Cart, Cart>buildTypedRemoveCustomTypeUpdateAction(cart,  null, null,
                Cart::getId, cartResource -> cartResource.toReference().getTypeId(), cartResource -> null,
                categorySyncOptions).orElse(null);

        assertThat(updateAction).isNull();
    }

    @Test
    public void buildTypedRemoveCustomTypeAction_WithNonHandledResourceAndNullContainer_ShouldNotBuildUpdateAction() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn("cartId");
        when(cart.toReference()).thenReturn(Cart.referenceOfId("cartId"));

        // Mock custom options error callback
        final ArrayList<Object> callBackResponses = new ArrayList<>();
        final BiConsumer<String, Throwable> updateActionErrorCallBack = (errorMessage, exception) -> {
            callBackResponses.add(errorMessage);
            callBackResponses.add(exception);
        };

        // Mock sync options
        final CategorySyncOptions syncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                                          .errorCallback(updateActionErrorCallBack)
                                                                          .build();

        final UpdateAction<Cart> updateAction =
            GenericUpdateActionUtils.<Cart, Cart>buildTypedRemoveCustomTypeUpdateAction(cart,  null, null,
                Cart::getId, cartResource -> cartResource.toReference().getTypeId(),
                cartResource -> null, syncOptions).orElse(null);

        assertThat(updateAction).isNull();
        assertThat(callBackResponses).hasSize(2);
        assertThat(callBackResponses.get(0)).isInstanceOf(String.class);
        assertThat((String) callBackResponses.get(0)).matches(
            "Failed to build 'setCustomType' update action to remove the custom type on the cart with id 'cartId'."
                + " Reason: Update actions for resource: 'Cart.*' "
                + "and container resource: 'null' is not implemented.");
        assertThat((Exception) callBackResponses.get(1)).isInstanceOf(BuildUpdateActionException.class);
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithNonHandledResource_ShouldNotBuildUpdateAction() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn("cartId");
        when(cart.toReference()).thenReturn(Cart.referenceOfId("cartId"));

        final JsonNode customFieldValue = mock(JsonNode.class);
        final String customFieldName = "name";

        final UpdateAction<Cart> updateAction = GenericUpdateActionUtils.<Cart, Cart>
            buildTypedSetCustomFieldUpdateAction(customFieldName, customFieldValue, cart,  null, null,
            Cart::getId, cartResource -> cartResource.toReference().getTypeId(), cartResource -> null,
            mock(BaseSyncOptions.class)).orElse(null);

        assertThat(updateAction).isNull();
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithNonHandledResource_ShouldCallSyncOptionsCallBack() {
        // Cart resource is not handled by buildTypedUpdateAction()
        final Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn("cartId");
        when(cart.toReference()).thenReturn(Cart.referenceOfId("cartId"));

        final JsonNode customFieldValue = mock(JsonNode.class);
        final String customFieldName = "name";
        final ArrayList<Object> callBackResponses = new ArrayList<>();
        final BiConsumer<String, Throwable> updateActionErrorCallBack = (errorMessage, exception) -> {
            callBackResponses.add(errorMessage);
            callBackResponses.add(exception);
        };

        final BaseSyncOptions baseSyncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                                          .errorCallback(updateActionErrorCallBack)
                                                                          .build();

        final UpdateAction<Cart> updateAction = GenericUpdateActionUtils.<Cart, Cart>
            buildTypedSetCustomFieldUpdateAction(customFieldName, customFieldValue, cart,  null, null,
            Cart::getId, cartResource -> cartResource.toReference().getTypeId(), cartResource -> null, baseSyncOptions)
            .orElse(null);

        assertThat(updateAction).isNull();
        assertThat(callBackResponses).hasSize(2);
        assertThat(callBackResponses.get(0)).isInstanceOf(String.class);
        assertThat((String) callBackResponses.get(0)).matches(
            "Failed to build 'setCustomField' update action on the custom field with the name 'name' on the cart with"
                + " id 'cartId'. Reason: Update actions for resource: 'Cart.*' "
                + "and container resource: 'null' is not implemented.");
        assertThat((Exception) callBackResponses.get(1)).isInstanceOf(BuildUpdateActionException.class);
    }
}
