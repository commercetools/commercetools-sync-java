package com.commercetools.sync.sdk2.shoppinglists.utils;

import static com.commercetools.sync.sdk2.shoppinglists.utils.LineItemUpdateActionUtils.*;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListChangeLineItemQuantityActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItem;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetLineItemCustomFieldActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetLineItemCustomTypeActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.sync.sdk2.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.sdk2.shoppinglists.ShoppingListSyncOptionsBuilder;
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
      ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

  final ShoppingList oldShoppingList = mock(ShoppingList.class);
  final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);

  @Test
  void buildLineItemCustomUpdateActions_WithSameValues_ShouldNotBuildUpdateAction() {
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("field1", true);
    oldCustomFieldsMap.put("field2", Map.of("de", "val"));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id("1").build());
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsMap).build());

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("1"))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(oldCustomFieldsMap))
            .build();

    final ShoppingListLineItem oldLineItem = mock(ShoppingListLineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getCustom()).thenReturn(oldCustomFields);

    final ShoppingListLineItemDraft newLineItem =
        ShoppingListLineItemDraftBuilder.of()
            .sku("sku")
            .quantity(1L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        buildLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLineItemCustomUpdateActions_WithDifferentValues_ShouldBuildUpdateAction() {
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("field1", true);
    oldCustomFieldsMap.put("field2", Map.of("de", "val1"));

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("field1", false);
    newCustomFieldsMap.put("field2", Map.of("es", "val2"));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id("1").build());
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsMap).build());

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("1"))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(newCustomFieldsMap))
            .build();

    final ShoppingListLineItem oldLineItem = mock(ShoppingListLineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getCustom()).thenReturn(oldCustomFields);

    final ShoppingListLineItemDraft newLineItem =
        ShoppingListLineItemDraftBuilder.of()
            .sku("sku")
            .quantity(1L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        buildLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("field1")
                .value(false)
                .lineItemId("line_item_id")
                .build(),
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("field2")
                .value(Map.of("es", "val2"))
                .lineItemId("line_item_id")
                .build());
  }

  @Test
  void buildLineItemCustomUpdateActions_WithNullOldValues_ShouldBuildUpdateAction() {
    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("field1", false);
    newCustomFieldsMap.put("field2", Map.of("es", "val"));

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("1"))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(newCustomFieldsMap))
            .build();

    final ShoppingListLineItem oldLineItem = mock(ShoppingListLineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getCustom()).thenReturn(null);

    final ShoppingListLineItemDraft newLineItem =
        ShoppingListLineItemDraftBuilder.of()
            .sku("sku")
            .quantity(1L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        buildLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListSetLineItemCustomTypeActionBuilder.of()
                .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("1"))
                .fields(fieldContainerBuilder -> fieldContainerBuilder.values(newCustomFieldsMap))
                .lineItemId("line_item_id")
                .build());
  }

  @Test
  void
      buildLineItemCustomUpdateActions_WithBadCustomFieldData_ShouldNotBuildUpdateActionAndTriggerErrorCallback() {
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("field1", true);
    oldCustomFieldsMap.put("field2", Map.of("de", "val1"));

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("field1", false);
    newCustomFieldsMap.put("field2", Map.of("es", "val2"));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id("").build());
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsMap).build());

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(""))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(newCustomFieldsMap))
            .build();

    final ShoppingListLineItem oldLineItem = mock(ShoppingListLineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getCustom()).thenReturn(oldCustomFields);

    final ShoppingListLineItemDraft newLineItem =
        ShoppingListLineItemDraftBuilder.of()
            .sku("sku")
            .quantity(1L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<String> errors = new ArrayList<>();

    final ShoppingListSyncOptions syncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errors.add(exception.getMessage()))
            .build();

    final List<ShoppingListUpdateAction> updateActions =
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
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("field1", true);
    oldCustomFieldsMap.put("field2", List.of(false));

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("field1", true);
    newCustomFieldsMap.put("field2", null);

    final CustomFields oldCustomFields = mock(CustomFields.class);
    final String typeId = UUID.randomUUID().toString();
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id(typeId).build());
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsMap).build());

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(typeId))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(newCustomFieldsMap))
            .build();

    final ShoppingListLineItem oldLineItem = mock(ShoppingListLineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getCustom()).thenReturn(oldCustomFields);

    final ShoppingListLineItemDraft newLineItem =
        ShoppingListLineItemDraftBuilder.of()
            .sku("sku")
            .quantity(1L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<String> errors = new ArrayList<>();

    final ShoppingListSyncOptions syncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errors.add(exception.getMessage()))
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        buildLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, syncOptions);

    assertThat(errors).isEmpty();
    assertThat(updateActions)
        .containsExactly(
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("field2")
                .value(null)
                .lineItemId("line_item_id")
                .build());
  }

  @Test
  void buildLineItemCustomUpdateActions_WithNullJsonNodeValue_ShouldCorrectlyBuildAction() {
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("field", List.of(false));

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("field", null);

    final CustomFields oldCustomFields = mock(CustomFields.class);
    final String typeId = UUID.randomUUID().toString();
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id(typeId).build());
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsMap).build());

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(typeId))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(newCustomFieldsMap))
            .build();

    final ShoppingListLineItem oldLineItem = mock(ShoppingListLineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getCustom()).thenReturn(oldCustomFields);

    final ShoppingListLineItemDraft newLineItem =
        ShoppingListLineItemDraftBuilder.of()
            .sku("sku")
            .quantity(1L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<String> errors = new ArrayList<>();

    final ShoppingListSyncOptions syncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errors.add(exception.getMessage()))
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        buildLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, syncOptions);

    assertThat(errors).isEmpty();
    assertThat(updateActions)
        .containsExactly(
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("field")
                .value(null)
                .lineItemId("line_item_id")
                .build());
  }

  @Test
  void buildChangeLineItemQuantityUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final ShoppingListLineItem oldLineItem = mock(ShoppingListLineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(2L);

    final ShoppingListLineItemDraft newLineItem =
        ShoppingListLineItemDraftBuilder.of()
            .sku("sku")
            .quantity(2L)
            .addedAt(ZonedDateTime.now())
            .build();

    final Optional<ShoppingListUpdateAction> updateAction =
        buildChangeLineItemQuantityUpdateAction(oldLineItem, newLineItem);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isNotPresent();
  }

  @Test
  void buildChangeLineItemQuantityUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final ShoppingListLineItem oldLineItem = mock(ShoppingListLineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(2L);

    final ShoppingListLineItemDraft newLineItem =
        ShoppingListLineItemDraftBuilder.of()
            .sku("sku")
            .quantity(4L)
            .addedAt(ZonedDateTime.now())
            .build();

    final ShoppingListUpdateAction updateAction =
        buildChangeLineItemQuantityUpdateAction(oldLineItem, newLineItem).orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction.getAction()).isEqualTo("changeLineItemQuantity");
    assertThat(updateAction)
        .isEqualTo(
            ShoppingListChangeLineItemQuantityActionBuilder.of()
                .lineItemId("line_item_id")
                .quantity(4L)
                .build());
  }

  @Test
  void buildChangeLineItemQuantityUpdateAction_WithNewNullValue_ShouldBuildUpdateAction() {
    final ShoppingListLineItem oldLineItem = mock(ShoppingListLineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(2L);

    final ShoppingListLineItemDraft newLineItem =
        ShoppingListLineItemDraftBuilder.of()
            .sku("sku")
            .quantity(null)
            .addedAt(ZonedDateTime.now())
            .build();

    final ShoppingListUpdateAction updateAction =
        buildChangeLineItemQuantityUpdateAction(oldLineItem, newLineItem).orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction.getAction()).isEqualTo("changeLineItemQuantity");
    assertThat(updateAction)
        .isEqualTo(
            ShoppingListChangeLineItemQuantityActionBuilder.of()
                .lineItemId("line_item_id")
                .quantity(1L)
                .build());
  }

  @Test
  void buildChangeLineItemQuantityUpdateAction_WithNewZeroValue_ShouldBuildUpdateAction() {
    final ShoppingListLineItem oldLineItem = mock(ShoppingListLineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(2L);

    final ShoppingListLineItemDraft newLineItem =
        ShoppingListLineItemDraftBuilder.of()
            .sku("sku")
            .quantity(0L)
            .addedAt(ZonedDateTime.now())
            .build();

    final ShoppingListUpdateAction updateAction =
        buildChangeLineItemQuantityUpdateAction(oldLineItem, newLineItem).orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction.getAction()).isEqualTo("changeLineItemQuantity");
    assertThat(updateAction)
        .isEqualTo(
            ShoppingListChangeLineItemQuantityActionBuilder.of()
                .lineItemId("line_item_id")
                .quantity(0L)
                .build());
  }

  @Test
  void
      buildChangeLineItemQuantityUpdateAction_WithNewNullValueAndOldDefaultValue_ShouldNotBuildAction() {
    final ShoppingListLineItem oldLineItem = mock(ShoppingListLineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(1L);

    final ShoppingListLineItemDraft newLineItem =
        ShoppingListLineItemDraftBuilder.of()
            .sku("sku")
            .quantity(null)
            .addedAt(ZonedDateTime.now())
            .build();

    final Optional<ShoppingListUpdateAction> updateAction =
        buildChangeLineItemQuantityUpdateAction(oldLineItem, newLineItem);

    assertThat(updateAction).isNotPresent();
  }

  @Test
  void buildLineItemUpdateActions_WithSameValues_ShouldNotBuildUpdateAction() {
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("field1", true);
    oldCustomFieldsMap.put("field2", Map.of("de", "val"));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id("1").build());
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsMap).build());

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("1"))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(oldCustomFieldsMap))
            .build();

    final ShoppingListLineItem oldLineItem = mock(ShoppingListLineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(1L);
    when(oldLineItem.getCustom()).thenReturn(oldCustomFields);

    final ShoppingListLineItemDraft newLineItem =
        ShoppingListLineItemDraftBuilder.of()
            .sku("sku")
            .quantity(1L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        buildLineItemUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLineItemUpdateActions_WithDifferentValues_ShouldBuildUpdateAction() {
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("field1", true);
    oldCustomFieldsMap.put("field2", Map.of("de", "val1"));

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("field1", false);
    newCustomFieldsMap.put("field2", Map.of("es", "val2"));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id("1").build());
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsMap).build());

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("1"))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(newCustomFieldsMap))
            .build();

    final ShoppingListLineItem oldLineItem = mock(ShoppingListLineItem.class);
    when(oldLineItem.getId()).thenReturn("line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(2L);
    when(oldLineItem.getCustom()).thenReturn(oldCustomFields);

    final ShoppingListLineItemDraft newLineItem =
        ShoppingListLineItemDraftBuilder.of()
            .sku("sku")
            .quantity(4L)
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        buildLineItemUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListChangeLineItemQuantityActionBuilder.of()
                .lineItemId("line_item_id")
                .quantity(4L)
                .build(),
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("field1")
                .value(false)
                .lineItemId("line_item_id")
                .build(),
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("field2")
                .value(Map.of("es", "val2"))
                .lineItemId("line_item_id")
                .build());
  }
}
