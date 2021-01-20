package com.commercetools.sync.shoppinglists.utils;

import static com.commercetools.sync.shoppinglists.utils.LineItemUpdateActionUtils.buildChangeLineItemQuantityUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.LineItemUpdateActionUtils.buildLineItemCustomUpdateActions;
import static com.commercetools.sync.shoppinglists.utils.LineItemUpdateActionUtils.buildLineItemUpdateActions;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeLineItemQuantity;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetLineItemCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetLineItemCustomType;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LineItemUpdateActionUtilsTest {

  private static final ShoppingListSyncOptions SYNC_OPTIONS =
      ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build();

  final ShoppingList oldShoppingList = mock(ShoppingList.class);
  final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);

  @Test
  void buildLineItemCustomUpdateActions_WithSameValues_ShouldNotBuildUpdateAction() {
    final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(true));
    oldCustomFieldsMap.put("field2", JsonNodeFactory.instance.objectNode().put("de", "val"));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(Type.referenceOfId("1"));
    when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraft.ofTypeIdAndJson("1", oldCustomFieldsMap);

    final LineItem oldLineItem = mock(LineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getCustom()).thenReturn(oldCustomFields);

    final LineItemDraft newLineItem =
        LineItemDraftBuilder.ofSku("sku", 1L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLineItemCustomUpdateActions_WithDifferentValues_ShouldBuildUpdateAction() {
    final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(true));
    oldCustomFieldsMap.put("field2", JsonNodeFactory.instance.objectNode().put("de", "val1"));

    final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(false));
    newCustomFieldsMap.put("field2", JsonNodeFactory.instance.objectNode().put("es", "val2"));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(Type.referenceOfId("1"));
    when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraft.ofTypeIdAndJson("1", newCustomFieldsMap);

    final LineItem oldLineItem = mock(LineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getCustom()).thenReturn(oldCustomFields);

    final LineItemDraft newLineItem =
        LineItemDraftBuilder.ofSku("sku", 1L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            SetLineItemCustomField.ofJson(
                "field1", JsonNodeFactory.instance.booleanNode(false), "line_item_id"),
            SetLineItemCustomField.ofJson(
                "field2", JsonNodeFactory.instance.objectNode().put("es", "val2"), "line_item_id"));
  }

  @Test
  void buildLineItemCustomUpdateActions_WithNullOldValues_ShouldBuildUpdateAction() {
    final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(false));
    newCustomFieldsMap.put("field2", JsonNodeFactory.instance.objectNode().put("es", "val"));

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraft.ofTypeIdAndJson("1", newCustomFieldsMap);

    final LineItem oldLineItem = mock(LineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getCustom()).thenReturn(null);

    final LineItemDraft newLineItem =
        LineItemDraftBuilder.ofSku("sku", 1L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            SetLineItemCustomType.ofTypeIdAndJson("1", newCustomFieldsMap, "line_item_id"));
  }

  @Test
  void
      buildLineItemCustomUpdateActions_WithBadCustomFieldData_ShouldNotBuildUpdateActionAndTriggerErrorCallback() {
    final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(true));
    oldCustomFieldsMap.put("field2", JsonNodeFactory.instance.objectNode().put("de", "val1"));

    final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(false));
    newCustomFieldsMap.put("field2", JsonNodeFactory.instance.objectNode().put("es", "val2"));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(Type.referenceOfId(""));
    when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraft.ofTypeIdAndJson("", newCustomFieldsMap);

    final LineItem oldLineItem = mock(LineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getCustom()).thenReturn(oldCustomFields);

    final LineItemDraft newLineItem =
        LineItemDraftBuilder.ofSku("sku", 1L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<String> errors = new ArrayList<>();

    final ShoppingListSyncOptions syncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errors.add(exception.getMessage()))
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(errors).hasSize(1);
    assertThat(errors.get(0))
        .isEqualTo(
            format(
                "Failed to build custom fields update actions on the line-item with id '%s'."
                    + " Reason: Custom type ids are not set for both the old and new line-item.",
                oldLineItem.getId()));
  }

  @Test
  void buildLineItemCustomUpdateActions_WithNullValue_ShouldCorrectlyBuildAction() {
    final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(true));
    oldCustomFieldsMap.put(
        "field2",
        JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.booleanNode(false)));

    final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(true));
    newCustomFieldsMap.put("field2", null);

    final CustomFields oldCustomFields = mock(CustomFields.class);
    final String typeId = UUID.randomUUID().toString();
    when(oldCustomFields.getType()).thenReturn(Type.referenceOfId(typeId));
    when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraft.ofTypeIdAndJson(typeId, newCustomFieldsMap);

    final LineItem oldLineItem = mock(LineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getCustom()).thenReturn(oldCustomFields);

    final LineItemDraft newLineItem =
        LineItemDraftBuilder.ofSku("sku", 1L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<String> errors = new ArrayList<>();

    final ShoppingListSyncOptions syncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errors.add(exception.getMessage()))
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, syncOptions);

    assertThat(errors).isEmpty();
    assertThat(updateActions)
        .containsExactly(SetLineItemCustomField.ofJson("field2", null, "line_item_id"));
  }

  @Test
  void buildLineItemCustomUpdateActions_WithNullJsonNodeValue_ShouldCorrectlyBuildAction() {
    final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put(
        "field",
        JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.booleanNode(false)));

    final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("field", JsonNodeFactory.instance.nullNode());

    final CustomFields oldCustomFields = mock(CustomFields.class);
    final String typeId = UUID.randomUUID().toString();
    when(oldCustomFields.getType()).thenReturn(Type.referenceOfId(typeId));
    when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraft.ofTypeIdAndJson(typeId, newCustomFieldsMap);

    final LineItem oldLineItem = mock(LineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getCustom()).thenReturn(oldCustomFields);

    final LineItemDraft newLineItem =
        LineItemDraftBuilder.ofSku("sku", 1L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<String> errors = new ArrayList<>();

    final ShoppingListSyncOptions syncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errors.add(exception.getMessage()))
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, syncOptions);

    assertThat(errors).isEmpty();
    assertThat(updateActions)
        .containsExactly(SetLineItemCustomField.ofJson("field", null, "line_item_id"));
  }

  @Test
  void buildChangeLineItemQuantityUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final LineItem oldLineItem = mock(LineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(2L);

    final LineItemDraft newLineItem =
        LineItemDraftBuilder.ofSku("sku", 2L).addedAt(ZonedDateTime.now()).build();

    final Optional<UpdateAction<ShoppingList>> updateAction =
        buildChangeLineItemQuantityUpdateAction(oldLineItem, newLineItem);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isNotPresent();
  }

  @Test
  void buildChangeLineItemQuantityUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final LineItem oldLineItem = mock(LineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(2L);

    final LineItemDraft newLineItem =
        LineItemDraftBuilder.ofSku("sku", 4L).addedAt(ZonedDateTime.now()).build();

    final UpdateAction<ShoppingList> updateAction =
        buildChangeLineItemQuantityUpdateAction(oldLineItem, newLineItem).orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction.getAction()).isEqualTo("changeLineItemQuantity");
    assertThat((ChangeLineItemQuantity) updateAction)
        .isEqualTo(ChangeLineItemQuantity.of("line_item_id", 4L));
  }

  @Test
  void buildChangeLineItemQuantityUpdateAction_WithNewNullValue_ShouldBuildUpdateAction() {
    final LineItem oldLineItem = mock(LineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(2L);

    final LineItemDraft newLineItem =
        LineItemDraftBuilder.ofSku("sku", null).addedAt(ZonedDateTime.now()).build();

    final UpdateAction<ShoppingList> updateAction =
        buildChangeLineItemQuantityUpdateAction(oldLineItem, newLineItem).orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction.getAction()).isEqualTo("changeLineItemQuantity");
    assertThat((ChangeLineItemQuantity) updateAction)
        .isEqualTo(ChangeLineItemQuantity.of("line_item_id", 1L));
  }

  @Test
  void buildChangeLineItemQuantityUpdateAction_WithNewZeroValue_ShouldBuildUpdateAction() {
    final LineItem oldLineItem = mock(LineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(2L);

    final LineItemDraft newLineItem =
        LineItemDraftBuilder.ofSku("sku", null).quantity(0L).addedAt(ZonedDateTime.now()).build();

    final UpdateAction<ShoppingList> updateAction =
        buildChangeLineItemQuantityUpdateAction(oldLineItem, newLineItem).orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction.getAction()).isEqualTo("changeLineItemQuantity");
    assertThat((ChangeLineItemQuantity) updateAction)
        .isEqualTo(ChangeLineItemQuantity.of("line_item_id", 0L));
  }

  @Test
  void
      buildChangeLineItemQuantityUpdateAction_WithNewNullValueAndOldDefaultValue_ShouldNotBuildAction() {
    final LineItem oldLineItem = mock(LineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(1L);

    final LineItemDraft newLineItem =
        LineItemDraftBuilder.ofSku("sku", null).addedAt(ZonedDateTime.now()).build();

    final Optional<UpdateAction<ShoppingList>> updateAction =
        buildChangeLineItemQuantityUpdateAction(oldLineItem, newLineItem);

    assertThat(updateAction).isNotPresent();
  }

  @Test
  void buildLineItemUpdateActions_WithSameValues_ShouldNotBuildUpdateAction() {
    final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(true));
    oldCustomFieldsMap.put("field2", JsonNodeFactory.instance.objectNode().put("de", "val"));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(Type.referenceOfId("1"));
    when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraft.ofTypeIdAndJson("1", oldCustomFieldsMap);

    final LineItem oldLineItem = mock(LineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(1L);
    when(oldLineItem.getCustom()).thenReturn(oldCustomFields);

    final LineItemDraft newLineItem =
        LineItemDraftBuilder.ofSku("sku", 1L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLineItemUpdateActions_WithDifferentValues_ShouldBuildUpdateAction() {
    final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(true));
    oldCustomFieldsMap.put("field2", JsonNodeFactory.instance.objectNode().put("de", "val1"));

    final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(false));
    newCustomFieldsMap.put("field2", JsonNodeFactory.instance.objectNode().put("es", "val2"));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(Type.referenceOfId("1"));
    when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraft.ofTypeIdAndJson("1", newCustomFieldsMap);

    final LineItem oldLineItem = mock(LineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(2L);
    when(oldLineItem.getCustom()).thenReturn(oldCustomFields);

    final LineItemDraft newLineItem =
        LineItemDraftBuilder.ofSku("sku", 4L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ChangeLineItemQuantity.of("line_item_id", 4L),
            SetLineItemCustomField.ofJson(
                "field1", JsonNodeFactory.instance.booleanNode(false), "line_item_id"),
            SetLineItemCustomField.ofJson(
                "field2", JsonNodeFactory.instance.objectNode().put("es", "val2"), "line_item_id"));
  }
}
