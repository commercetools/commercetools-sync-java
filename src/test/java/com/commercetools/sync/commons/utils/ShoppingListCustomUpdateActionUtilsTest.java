package com.commercetools.sync.commons.utils;

import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.utils.ShoppingListCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

import static com.commercetools.sync.commons.asserts.actions.AssertionsForUpdateActions.assertThat;
import static io.sphere.sdk.models.ResourceIdentifier.ofId;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ShoppingListCustomUpdateActionUtilsTest {

    @Test
    void buildTypedSetCustomTypeUpdateAction_WithCustomerResource_ShouldBuildCustomerUpdateAction() {
        final String newCustomTypeId = UUID.randomUUID().toString();

        final UpdateAction<ShoppingList> updateAction =
            GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(newCustomTypeId, new HashMap<>(),
                mock(ShoppingList.class), ShoppingListCustomActionBuilder.of(), null, ShoppingList::getId,
                shoppingListResource -> shoppingListResource.toReference().getTypeId(), shoppingListResource -> null,
                ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build()).orElse(null);

        assertThat(updateAction).isInstanceOf(SetCustomType.class);
        assertThat((SetCustomType) updateAction).hasValues("setCustomType", emptyMap(), ofId(newCustomTypeId));
    }

    @Test
    void buildRemoveCustomTypeAction_WithShoppingListResource_ShouldBuildShoppingListUpdateAction() {
        final UpdateAction<ShoppingList> updateAction =
            ShoppingListCustomActionBuilder.of().buildRemoveCustomTypeAction(null, null);

        assertThat(updateAction).isInstanceOf(SetCustomType.class);
        assertThat((SetCustomType) updateAction).hasValues("setCustomType", null, ofId(null));
    }

    @Test
    void buildSetCustomFieldAction_WithShoppingListResource_ShouldBuildShoppingListUpdateAction() {
        final JsonNode customFieldValue = JsonNodeFactory.instance.textNode("foo");
        final String customFieldName = "name";

        final UpdateAction<ShoppingList> updateAction = ShoppingListCustomActionBuilder.of()
            .buildSetCustomFieldAction(null, null, customFieldName, customFieldValue);

        assertThat(updateAction).isInstanceOf(SetCustomField.class);
        assertThat((SetCustomField) updateAction).hasValues("setCustomField", customFieldName, customFieldValue);
    }
}
