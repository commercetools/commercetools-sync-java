package com.commercetools.sync.commons.utils;

import static com.commercetools.sync.commons.asserts.actions.AssertionsForUpdateActions.assertThat;
import static io.sphere.sdk.models.ResourceIdentifier.ofId;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.utils.LineItemCustomActionBuilder;
import com.commercetools.sync.shoppinglists.utils.ShoppingListCustomActionBuilder;
import com.commercetools.sync.shoppinglists.utils.TextLineItemCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.TextLineItem;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomType;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetLineItemCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetLineItemCustomType;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetTextLineItemCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetTextLineItemCustomType;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShoppingListCustomUpdateActionUtilsTest {

  @Test
  void
      buildTypedSetCustomTypeUpdateAction_WithShoppingListResource_ShouldBuildShoppingListUpdateAction() {
    final String newCustomTypeId = UUID.randomUUID().toString();

    final UpdateAction<ShoppingList> updateAction =
        GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
                newCustomTypeId,
                new HashMap<>(),
                mock(ShoppingList.class),
                ShoppingListCustomActionBuilder.of(),
                null,
                ShoppingList::getId,
                shoppingListResource -> shoppingListResource.toReference().getTypeId(),
                shoppingListResource -> null,
                ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build())
            .orElse(null);

    assertThat(updateAction).isInstanceOf(SetCustomType.class);
    assertThat((SetCustomType) updateAction)
        .hasValues("setCustomType", emptyMap(), ofId(newCustomTypeId));
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

    final UpdateAction<ShoppingList> updateAction =
        ShoppingListCustomActionBuilder.of()
            .buildSetCustomFieldAction(null, null, customFieldName, customFieldValue);

    assertThat(updateAction).isInstanceOf(SetCustomField.class);
    assertThat((SetCustomField) updateAction)
        .hasValues("setCustomField", customFieldName, customFieldValue);
  }

  @Test
  void
      buildTypedSetLineItemCustomTypeUpdateAction_WithLineItemResource_ShouldBuildShoppingListUpdateAction() {
    final LineItem lineItem = mock(LineItem.class);
    when(lineItem.getId()).thenReturn("line_item_id");

    final String newCustomTypeId = UUID.randomUUID().toString();

    final UpdateAction<ShoppingList> updateAction =
        GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
                newCustomTypeId,
                new HashMap<>(),
                lineItem::getCustom,
                new LineItemCustomActionBuilder(),
                -1,
                t -> lineItem.getId(),
                lineItemResource -> LineItem.resourceTypeId(),
                t -> lineItem.getId(),
                ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build())
            .orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isInstanceOf(SetLineItemCustomType.class);
    assertThat((SetLineItemCustomType) updateAction)
        .hasValues("setLineItemCustomType", emptyMap(), ofId(newCustomTypeId));
    assertThat(((SetLineItemCustomType) updateAction).getLineItemId()).isEqualTo("line_item_id");
  }

  @Test
  void
      buildRemoveLineItemCustomTypeAction_WithLineItemResource_ShouldBuildShoppingListUpdateAction() {
    final UpdateAction<ShoppingList> updateAction =
        new LineItemCustomActionBuilder().buildRemoveCustomTypeAction(-1, "line_item_id");

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isInstanceOf(SetLineItemCustomType.class);
    assertThat((SetLineItemCustomType) updateAction)
        .hasValues("setLineItemCustomType", null, ofId(null));
    assertThat(((SetLineItemCustomType) updateAction).getLineItemId()).isEqualTo("line_item_id");
  }

  @Test
  void
      buildSetLineItemCustomFieldAction_WithLineItemResource_ShouldBuildShoppingListUpdateAction() {
    final JsonNode customFieldValue = JsonNodeFactory.instance.textNode("foo");
    final String customFieldName = "name";

    final UpdateAction<ShoppingList> updateAction =
        new LineItemCustomActionBuilder()
            .buildSetCustomFieldAction(-1, "line_item_id", customFieldName, customFieldValue);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isInstanceOf(SetLineItemCustomField.class);
    assertThat((SetLineItemCustomField) updateAction)
        .hasValues("setLineItemCustomField", customFieldName, customFieldValue);
    assertThat(((SetLineItemCustomField) updateAction).getLineItemId()).isEqualTo("line_item_id");
  }

  @Test
  void
      buildTypedSetTextLineItemCustomTypeUpdateAction_WithTextLineItemRes_ShouldBuildShoppingListUpdateAction() {
    final TextLineItem textLineItem = mock(TextLineItem.class);
    when(textLineItem.getId()).thenReturn("text_line_item_id");

    final String newCustomTypeId = UUID.randomUUID().toString();

    final UpdateAction<ShoppingList> updateAction =
        GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
                newCustomTypeId,
                new HashMap<>(),
                textLineItem::getCustom,
                new TextLineItemCustomActionBuilder(),
                -1,
                t -> textLineItem.getId(),
                textLineItemResource -> TextLineItem.resourceTypeId(),
                t -> textLineItem.getId(),
                ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build())
            .orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isInstanceOf(SetTextLineItemCustomType.class);
    assertThat((SetTextLineItemCustomType) updateAction)
        .hasValues("setTextLineItemCustomType", emptyMap(), ofId(newCustomTypeId));
    assertThat(((SetTextLineItemCustomType) updateAction).getTextLineItemId())
        .isEqualTo("text_line_item_id");
  }

  @Test
  void
      buildRemoveTextLineItemCustomTypeAction_WithTextLineItemResource_ShouldBuildShoppingListUpdateAction() {
    final UpdateAction<ShoppingList> updateAction =
        new TextLineItemCustomActionBuilder().buildRemoveCustomTypeAction(-1, "text_line_item_id");

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isInstanceOf(SetTextLineItemCustomType.class);
    assertThat((SetTextLineItemCustomType) updateAction)
        .hasValues("setTextLineItemCustomType", null, ofId(null));
    assertThat(((SetTextLineItemCustomType) updateAction).getTextLineItemId())
        .isEqualTo("text_line_item_id");
  }

  @Test
  void
      buildSetTextLineItemCustomFieldAction_WithTextLineItemResource_ShouldBuildShoppingListUpdateAction() {
    final JsonNode customFieldValue = JsonNodeFactory.instance.textNode("foo");
    final String customFieldName = "name";

    final UpdateAction<ShoppingList> updateAction =
        new TextLineItemCustomActionBuilder()
            .buildSetCustomFieldAction(-1, "text_line_item_id", customFieldName, customFieldValue);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isInstanceOf(SetTextLineItemCustomField.class);
    assertThat((SetTextLineItemCustomField) updateAction)
        .hasValues("setTextLineItemCustomField", customFieldName, customFieldValue);
    assertThat(((SetTextLineItemCustomField) updateAction).getTextLineItemId())
        .isEqualTo("text_line_item_id");
  }
}
