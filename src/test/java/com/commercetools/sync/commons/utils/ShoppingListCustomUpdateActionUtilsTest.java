package com.commercetools.sync.commons.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListLineItem;
import com.commercetools.api.models.shopping_list.ShoppingListSetCustomFieldAction;
import com.commercetools.api.models.shopping_list.ShoppingListSetCustomTypeAction;
import com.commercetools.api.models.shopping_list.ShoppingListSetLineItemCustomFieldAction;
import com.commercetools.api.models.shopping_list.ShoppingListSetLineItemCustomTypeAction;
import com.commercetools.api.models.shopping_list.ShoppingListSetTextLineItemCustomFieldAction;
import com.commercetools.api.models.shopping_list.ShoppingListSetTextLineItemCustomTypeAction;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.api.models.shopping_list.TextLineItem;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.models.ShoppingListCustomTypeAdapter;
import com.commercetools.sync.shoppinglists.models.ShoppingListLineItemCustomTypeAdapter;
import com.commercetools.sync.shoppinglists.models.TextLineItemCustomTypeAdapter;
import com.commercetools.sync.shoppinglists.utils.LineItemCustomActionBuilder;
import com.commercetools.sync.shoppinglists.utils.ShoppingListCustomActionBuilder;
import com.commercetools.sync.shoppinglists.utils.TextLineItemCustomActionBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShoppingListCustomUpdateActionUtilsTest {

  private static final String LINE_ITEM_ID = "line_item_id";
  private static final String TEXT_LINE_ITEM_ID = "text_line_item_id";

  @Test
  void
      buildTypedSetCustomTypeUpdateAction_WithShoppingListResource_ShouldBuildShoppingListUpdateAction() {
    final String newCustomTypeId = UUID.randomUUID().toString();

    final ShoppingListUpdateAction updateAction =
        GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
                newCustomTypeId,
                new HashMap<>(),
                ShoppingListCustomTypeAdapter.of(mock(ShoppingList.class)),
                ShoppingListCustomActionBuilder.of(),
                null,
                ShoppingListCustomTypeAdapter::getId,
                shoppingListResource -> ResourceTypeId.SHOPPING_LIST.getJsonName(),
                shoppingListResource -> null,
                ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build())
            .orElse(null);

    assertThat(updateAction).isInstanceOf(ShoppingListSetCustomTypeAction.class);
    final ShoppingListSetCustomTypeAction shoppingListSetCustomTypeAction =
        (ShoppingListSetCustomTypeAction) updateAction;
    assertThat(shoppingListSetCustomTypeAction.getFields())
        .isEqualTo(FieldContainerBuilder.of().values(Map.of()).build());
    assertThat(shoppingListSetCustomTypeAction.getType().getId()).isEqualTo(newCustomTypeId);
  }

  @Test
  void buildRemoveCustomTypeAction_WithShoppingListResource_ShouldBuildShoppingListUpdateAction() {
    final ShoppingListUpdateAction updateAction =
        ShoppingListCustomActionBuilder.of().buildRemoveCustomTypeAction(null, null);

    assertThat(updateAction).isInstanceOf(ShoppingListSetCustomTypeAction.class);
    final ShoppingListSetCustomTypeAction shoppingListSetCustomTypeAction =
        (ShoppingListSetCustomTypeAction) updateAction;
    assertThat(shoppingListSetCustomTypeAction.getFields()).isEqualTo(null);
    assertThat(shoppingListSetCustomTypeAction.getType()).isEqualTo(null);
  }

  @Test
  void buildSetCustomFieldAction_WithShoppingListResource_ShouldBuildShoppingListUpdateAction() {
    final String customFieldValue = "foo";
    final String customFieldName = "name";

    final ShoppingListUpdateAction updateAction =
        ShoppingListCustomActionBuilder.of()
            .buildSetCustomFieldAction(null, null, customFieldName, customFieldValue);

    assertThat(updateAction).isInstanceOf(ShoppingListSetCustomFieldAction.class);
    final ShoppingListSetCustomFieldAction shoppingListSetCustomFieldAction =
        (ShoppingListSetCustomFieldAction) updateAction;
    assertThat(shoppingListSetCustomFieldAction.getName()).isEqualTo(customFieldName);
    assertThat(shoppingListSetCustomFieldAction.getValue()).isEqualTo(customFieldValue);
  }

  @Test
  void
      buildTypedSetLineItemCustomTypeUpdateAction_WithLineItemResource_ShouldBuildShoppingListUpdateAction() {
    final ShoppingListLineItem lineItem = mock(ShoppingListLineItem.class);
    when(lineItem.getId()).thenReturn(LINE_ITEM_ID);

    final String newCustomTypeId = UUID.randomUUID().toString();

    final ShoppingListUpdateAction updateAction =
        GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
                newCustomTypeId,
                new HashMap<>(),
                ShoppingListLineItemCustomTypeAdapter.of(mock(ShoppingListLineItem.class)),
                new LineItemCustomActionBuilder(),
                -1L,
                t -> lineItem.getId(),
                shoppingListResource -> ResourceTypeId.SHOPPING_LIST.getJsonName(),
                t -> lineItem.getId(),
                ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build())
            .orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isInstanceOf(ShoppingListSetLineItemCustomTypeAction.class);
    final ShoppingListSetLineItemCustomTypeAction shoppingListSetLineItemCustomTypeAction =
        (ShoppingListSetLineItemCustomTypeAction) updateAction;

    assertThat(shoppingListSetLineItemCustomTypeAction.getLineItemId()).isEqualTo(LINE_ITEM_ID);
    assertThat(shoppingListSetLineItemCustomTypeAction.getType().getId())
        .isEqualTo(newCustomTypeId);
    assertThat(shoppingListSetLineItemCustomTypeAction.getFields().values()).isEqualTo(Map.of());
  }

  @Test
  void
      buildRemoveLineItemCustomTypeAction_WithLineItemResource_ShouldBuildShoppingListUpdateAction() {
    final ShoppingListUpdateAction updateAction =
        new LineItemCustomActionBuilder().buildRemoveCustomTypeAction(-1L, LINE_ITEM_ID);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isInstanceOf(ShoppingListSetLineItemCustomTypeAction.class);
    final ShoppingListSetLineItemCustomTypeAction shoppingListSetLineItemCustomTypeAction =
        (ShoppingListSetLineItemCustomTypeAction) updateAction;
    assertThat(shoppingListSetLineItemCustomTypeAction.getLineItemId()).isEqualTo(LINE_ITEM_ID);
    assertThat(shoppingListSetLineItemCustomTypeAction.getType()).isEqualTo(null);
    assertThat(shoppingListSetLineItemCustomTypeAction.getFields()).isEqualTo(null);
  }

  @Test
  void
      buildSetLineItemCustomFieldAction_WithLineItemResource_ShouldBuildShoppingListUpdateAction() {
    final String customFieldValue = "foo";
    final String customFieldName = "name";

    final ShoppingListUpdateAction updateAction =
        new LineItemCustomActionBuilder()
            .buildSetCustomFieldAction(-1L, LINE_ITEM_ID, customFieldName, customFieldValue);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isInstanceOf(ShoppingListSetLineItemCustomFieldAction.class);

    final ShoppingListSetLineItemCustomFieldAction shoppingListSetLineItemCustomFieldAction =
        (ShoppingListSetLineItemCustomFieldAction) updateAction;
    assertThat(shoppingListSetLineItemCustomFieldAction.getName()).isEqualTo(customFieldName);
    assertThat(shoppingListSetLineItemCustomFieldAction.getValue()).isEqualTo(customFieldValue);
    assertThat(shoppingListSetLineItemCustomFieldAction.getLineItemId()).isEqualTo(LINE_ITEM_ID);
  }

  @Test
  void
      buildTypedSetTextLineItemCustomTypeUpdateAction_WithTextLineItemRes_ShouldBuildShoppingListUpdateAction() {
    final TextLineItem textLineItem = mock(TextLineItem.class);
    when(textLineItem.getId()).thenReturn(TEXT_LINE_ITEM_ID);

    final String newCustomTypeId = UUID.randomUUID().toString();

    final ShoppingListUpdateAction updateAction =
        GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
                newCustomTypeId,
                new HashMap<>(),
                TextLineItemCustomTypeAdapter.of(mock(TextLineItem.class)),
                new TextLineItemCustomActionBuilder(),
                -1L,
                t -> textLineItem.getId(),
                textLineItemResource -> ResourceTypeId.SHOPPING_LIST_TEXT_LINE_ITEM.getJsonName(),
                t -> textLineItem.getId(),
                ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build())
            .orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isInstanceOf(ShoppingListSetTextLineItemCustomTypeAction.class);
    final ShoppingListSetTextLineItemCustomTypeAction shoppingListSetTextLineItemCustomTypeAction =
        (ShoppingListSetTextLineItemCustomTypeAction) updateAction;
    assertThat(shoppingListSetTextLineItemCustomTypeAction.getTextLineItemId())
        .isEqualTo(TEXT_LINE_ITEM_ID);
    assertThat(shoppingListSetTextLineItemCustomTypeAction.getType().getId())
        .isEqualTo(newCustomTypeId);
    assertThat(shoppingListSetTextLineItemCustomTypeAction.getFields().values())
        .isEqualTo(Map.of());
  }

  @Test
  void
      buildRemoveTextLineItemCustomTypeAction_WithTextLineItemResource_ShouldBuildShoppingListUpdateAction() {
    final ShoppingListUpdateAction updateAction =
        new TextLineItemCustomActionBuilder().buildRemoveCustomTypeAction(-1L, TEXT_LINE_ITEM_ID);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isInstanceOf(ShoppingListSetTextLineItemCustomTypeAction.class);
    final ShoppingListSetTextLineItemCustomTypeAction shoppingListSetTextLineItemCustomTypeAction =
        (ShoppingListSetTextLineItemCustomTypeAction) updateAction;
    assertThat(shoppingListSetTextLineItemCustomTypeAction.getTextLineItemId())
        .isEqualTo(TEXT_LINE_ITEM_ID);
    assertThat(shoppingListSetTextLineItemCustomTypeAction.getType()).isEqualTo(null);
    assertThat(shoppingListSetTextLineItemCustomTypeAction.getFields()).isEqualTo(null);
  }

  @Test
  void
      buildSetTextLineItemCustomFieldAction_WithTextLineItemResource_ShouldBuildShoppingListUpdateAction() {
    final String customFieldValue = "foo";
    final String customFieldName = "name";

    final ShoppingListUpdateAction updateAction =
        new TextLineItemCustomActionBuilder()
            .buildSetCustomFieldAction(-1L, TEXT_LINE_ITEM_ID, customFieldName, customFieldValue);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isInstanceOf(ShoppingListSetTextLineItemCustomFieldAction.class);
    final ShoppingListSetTextLineItemCustomFieldAction
        shoppingListSetTextLineItemCustomFieldAction =
            (ShoppingListSetTextLineItemCustomFieldAction) updateAction;
    assertThat(shoppingListSetTextLineItemCustomFieldAction.getName()).isEqualTo(customFieldName);
    assertThat(shoppingListSetTextLineItemCustomFieldAction.getValue()).isEqualTo(customFieldValue);
    assertThat(shoppingListSetTextLineItemCustomFieldAction.getTextLineItemId())
        .isEqualTo(TEXT_LINE_ITEM_ID);
  }
}
