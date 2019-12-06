package com.commercetools.sync.commons.utils;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetCustomField;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetCustomType;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

import static com.commercetools.sync.commons.asserts.actions.AssertionsForUpdateActions.assertThat;
import static io.sphere.sdk.models.ResourceIdentifier.ofId;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CartDiscountCustomUpdateActionUtilsTest {

    @Test
    void buildTypedSetCustomTypeUpdateAction_WithCartDiscountResource_ShouldBuildCartDiscountUpdateAction() {
        final String newCustomTypeId = UUID.randomUUID().toString();

        final UpdateAction<CartDiscount> updateAction =
            GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(newCustomTypeId, new HashMap<>(),
                mock(CartDiscount.class), new CartDiscountCustomActionBuilder(), null, CartDiscount::getId,
                cartDiscountResource -> cartDiscountResource.toReference().getTypeId(), cartDiscountResource -> null,
                CartDiscountSyncOptionsBuilder.of(mock(SphereClient.class)).build()).orElse(null);

        assertThat(updateAction).isInstanceOf(SetCustomType.class);
        assertThat((SetCustomType) updateAction).hasValues("setCustomType", emptyMap(), ofId(newCustomTypeId));
    }

    @Test
    void buildRemoveCustomTypeAction_WithCartDiscountResource_ShouldBuildCartDiscountUpdateAction() {
        final UpdateAction<CartDiscount> updateAction =
            new CartDiscountCustomActionBuilder().buildRemoveCustomTypeAction(null, null);

        assertThat(updateAction).isInstanceOf(SetCustomType.class);
        assertThat((SetCustomType) updateAction).hasValues("setCustomType", null, ofId(null));
    }

    @Test
    void buildSetCustomFieldAction_WithCartDiscountResource_ShouldBuildCartDiscountUpdateAction() {
        final JsonNode customFieldValue = JsonNodeFactory.instance.textNode("foo");
        final String customFieldName = "name";

        final UpdateAction<CartDiscount> updateAction = new CartDiscountCustomActionBuilder()
            .buildSetCustomFieldAction(null, null, customFieldName, customFieldValue);

        assertThat(updateAction).isInstanceOf(SetCustomField.class);
        assertThat((SetCustomField) updateAction).hasValues("setCustomField", customFieldName, customFieldValue);
    }
}
