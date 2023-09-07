package com.commercetools.sync.shoppinglists.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListChangeTextLineItemNameAction;
import com.commercetools.api.models.shopping_list.ShoppingListChangeTextLineItemNameActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListChangeTextLineItemQuantityAction;
import com.commercetools.api.models.shopping_list.ShoppingListChangeTextLineItemQuantityActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListSetTextLineItemCustomFieldActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetTextLineItemCustomTypeActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetTextLineItemDescriptionAction;
import com.commercetools.api.models.shopping_list.ShoppingListSetTextLineItemDescriptionActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.api.models.shopping_list.TextLineItem;
import com.commercetools.api.models.shopping_list.TextLineItemDraft;
import com.commercetools.api.models.shopping_list.TextLineItemDraftBuilder;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TextLineItemUpdateActionUtilsTest {

  private static final ShoppingListSyncOptions SYNC_OPTIONS =
      ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

  final ShoppingList oldShoppingList = mock(ShoppingList.class);
  final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);

  @Test
  void buildTextLineItemCustomUpdateActions_WithSameValues_ShouldNotBuildUpdateAction() {
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

    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
    when(oldTextLineItem.getCustom()).thenReturn(oldCustomFields);

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("name"))
            .quantity(1L)
            .description(ofEnglish("desc"))
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildTextLineItemCustomUpdateActions_WithDifferentValues_ShouldBuildUpdateAction() {
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
            .fields(FieldContainerBuilder.of().values(newCustomFieldsMap).build())
            .build();

    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
    when(oldTextLineItem.getCustom()).thenReturn(oldCustomFields);

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("name"))
            .quantity(1L)
            .description(ofEnglish("desc"))
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
                .name("field1")
                .value(false)
                .textLineItemId("text_line_item_id")
                .build(),
            ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
                .name("field2")
                .value(Map.of("es", "val2"))
                .textLineItemId("text_line_item_id")
                .build());
  }

  @Test
  void buildTextLineItemCustomUpdateActions_WithNullOldValues_ShouldBuildUpdateAction() {
    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("field1", false);
    newCustomFieldsMap.put("field2", Map.of("es", "val"));

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("1"))
            .fields(FieldContainerBuilder.of().values(newCustomFieldsMap).build())
            .build();

    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
    when(oldTextLineItem.getCustom()).thenReturn(null);

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("name"))
            .quantity(1L)
            .description(ofEnglish("desc"))
            .addedAt(ZonedDateTime.now())
            .custom(newCustomFieldsDraft)
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListSetTextLineItemCustomTypeActionBuilder.of()
                .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("1"))
                .fields(fieldContainerBuilder -> fieldContainerBuilder.values(newCustomFieldsMap))
                .textLineItemId("text_line_item_id")
                .build());
  }

  @Test
  void
      buildTextLineItemCustomUpdateActions_WithBadCustomFieldData_ShouldNotBuildUpdateActionAndTriggerCallback() {
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

    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
    when(oldTextLineItem.getCustom()).thenReturn(oldCustomFields);

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("name"))
            .quantity(1L)
            .description(ofEnglish("desc"))
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
        TextLineItemUpdateActionUtils.buildTextLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(errors).hasSize(1);
    assertThat(errors.get(0))
        .isEqualTo(
            format(
                "Failed to build custom fields update actions on the shopping-list-text-line-item with "
                    + "id '%s'. Reason: Custom type ids are not set for both the old "
                    + "and new shopping-list-text-line-item.",
                oldTextLineItem.getId()));
  }

  @Test
  void buildTextLineItemCustomUpdateActions_WithNullValue_ShouldCorrectlyBuildAction() {
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
            .fields(FieldContainerBuilder.of().values(newCustomFieldsMap).build())
            .build();

    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
    when(oldTextLineItem.getCustom()).thenReturn(oldCustomFields);

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("name"))
            .quantity(1L)
            .description(ofEnglish("desc"))
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
        TextLineItemUpdateActionUtils.buildTextLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, syncOptions);

    assertThat(errors).isEmpty();
    assertThat(updateActions)
        .containsExactly(
            ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
                .name("field2")
                .value(null)
                .textLineItemId("text_line_item_id")
                .build());
  }

  @Test
  void buildTextLineItemCustomUpdateActions_WithNullJsonNodeValue_ShouldCorrectlyBuildAction() {
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

    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
    when(oldTextLineItem.getCustom()).thenReturn(oldCustomFields);

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("name"))
            .quantity(1L)
            .description(ofEnglish("desc"))
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
        TextLineItemUpdateActionUtils.buildTextLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, syncOptions);

    assertThat(errors).isEmpty();
    assertThat(updateActions)
        .containsExactly(
            ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
                .name("field")
                .value(null)
                .textLineItemId("text_line_item_id")
                .build());
  }

  @Test
  void buildChangeTextLineItemQuantityUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final TextLineItem oldLineItem = mock(TextLineItem.class);
    when(oldLineItem.getId()).thenReturn("text_line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(2L);

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("name"))
            .quantity(2L)
            .addedAt(ZonedDateTime.now())
            .build();

    final Optional<ShoppingListUpdateAction> updateAction =
        TextLineItemUpdateActionUtils.buildChangeTextLineItemQuantityUpdateAction(
            oldLineItem, newTextLineItem);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isNotPresent();
  }

  @Test
  void buildChangeTextLineItemQuantityUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final TextLineItem oldLineItem = mock(TextLineItem.class);
    when(oldLineItem.getId()).thenReturn("text_line_item_id");
    when(oldLineItem.getQuantity()).thenReturn(2L);

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("name"))
            .quantity(4L)
            .addedAt(ZonedDateTime.now())
            .build();

    final ShoppingListUpdateAction updateAction =
        TextLineItemUpdateActionUtils.buildChangeTextLineItemQuantityUpdateAction(
                oldLineItem, newTextLineItem)
            .orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction.getAction()).isEqualTo("changeTextLineItemQuantity");
    assertThat((ShoppingListChangeTextLineItemQuantityAction) updateAction)
        .isEqualTo(
            ShoppingListChangeTextLineItemQuantityActionBuilder.of()
                .textLineItemId("text_line_item_id")
                .quantity(4L)
                .build());
  }

  @Test
  void buildChangeTextLineItemQuantityUpdateAction_WithNewNullValue_ShouldBuildUpdateAction() {
    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
    when(oldTextLineItem.getQuantity()).thenReturn(2L);

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("name"))
            .quantity(null)
            .addedAt(ZonedDateTime.now())
            .build();

    final ShoppingListUpdateAction updateAction =
        TextLineItemUpdateActionUtils.buildChangeTextLineItemQuantityUpdateAction(
                oldTextLineItem, newTextLineItem)
            .orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction.getAction()).isEqualTo("changeTextLineItemQuantity");
    assertThat((ShoppingListChangeTextLineItemQuantityAction) updateAction)
        .isEqualTo(
            ShoppingListChangeTextLineItemQuantityActionBuilder.of()
                .textLineItemId("text_line_item_id")
                .quantity(1L)
                .build());
  }

  @Test
  void buildChangeTextLineItemQuantityUpdateAction_WithNewZeroValue_ShouldBuildUpdateAction() {
    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
    when(oldTextLineItem.getQuantity()).thenReturn(2L);

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("name"))
            .quantity(0L)
            .addedAt(ZonedDateTime.now())
            .build();

    final ShoppingListUpdateAction updateAction =
        TextLineItemUpdateActionUtils.buildChangeTextLineItemQuantityUpdateAction(
                oldTextLineItem, newTextLineItem)
            .orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction.getAction()).isEqualTo("changeTextLineItemQuantity");
    assertThat((ShoppingListChangeTextLineItemQuantityAction) updateAction)
        .isEqualTo(
            ShoppingListChangeTextLineItemQuantityActionBuilder.of()
                .textLineItemId("text_line_item_id")
                .quantity(0L)
                .build());
  }

  @Test
  void
      buildChangeTextLineItemQuantityUpdateAction_WithNewNullValueAndOldDefaultValue_ShouldNotBuildAction() {
    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
    when(oldTextLineItem.getQuantity()).thenReturn(1L);

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("name"))
            .quantity(null)
            .addedAt(ZonedDateTime.now())
            .build();

    final Optional<ShoppingListUpdateAction> updateAction =
        TextLineItemUpdateActionUtils.buildChangeTextLineItemQuantityUpdateAction(
            oldTextLineItem, newTextLineItem);

    assertThat(updateAction).isNotPresent();
  }

  @Test
  void buildSetTextLineItemDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");

    when(oldTextLineItem.getDescription()).thenReturn(ofEnglish("oldDescription"));

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("name"))
            .quantity(1L)
            .description(ofEnglish("newDescription"))
            .build();

    final ShoppingListUpdateAction updateAction =
        TextLineItemUpdateActionUtils.buildSetTextLineItemDescriptionUpdateAction(
                oldTextLineItem, newTextLineItem)
            .orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction.getAction()).isEqualTo("setTextLineItemDescription");
    assertThat(((ShoppingListSetTextLineItemDescriptionAction) updateAction))
        .isEqualTo(
            ShoppingListSetTextLineItemDescriptionActionBuilder.of()
                .textLineItemId("text_line_item_id")
                .description(ofEnglish("newDescription"))
                .build());
  }

  @Test
  void buildSetTextLineItemDescriptionUpdateAction_WithNullNewValue_ShouldBuildUpdateAction() {
    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");

    when(oldTextLineItem.getDescription()).thenReturn(ofEnglish("oldDescription"));

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of().name(LocalizedString.of()).quantity(1L).build();

    final ShoppingListUpdateAction updateAction =
        TextLineItemUpdateActionUtils.buildSetTextLineItemDescriptionUpdateAction(
                oldTextLineItem, newTextLineItem)
            .orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction.getAction()).isEqualTo("setTextLineItemDescription");
    assertThat(((ShoppingListSetTextLineItemDescriptionAction) updateAction))
        .isEqualTo(
            ShoppingListSetTextLineItemDescriptionActionBuilder.of()
                .description((LocalizedString) null)
                .textLineItemId("text_line_item_id")
                .build());
  }

  @Test
  void buildSetTextLineItemDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");

    when(oldTextLineItem.getDescription()).thenReturn(ofEnglish("oldDescription"));

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("name"))
            .quantity(1L)
            .description(ofEnglish("oldDescription"))
            .build();

    final Optional<ShoppingListUpdateAction> updateAction =
        TextLineItemUpdateActionUtils.buildSetTextLineItemDescriptionUpdateAction(
            oldTextLineItem, newTextLineItem);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isNotPresent();
  }

  @Test
  void buildChangeTextLineItemNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
    when(oldTextLineItem.getName()).thenReturn(ofEnglish("oldName"));

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of().name(ofEnglish("newName")).quantity(1L).build();

    final ShoppingListUpdateAction updateAction =
        TextLineItemUpdateActionUtils.buildChangeTextLineItemNameUpdateAction(
                oldTextLineItem, newTextLineItem)
            .orElse(null);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction.getAction()).isEqualTo("changeTextLineItemName");
    assertThat(((ShoppingListChangeTextLineItemNameAction) updateAction))
        .isEqualTo(
            ShoppingListChangeTextLineItemNameActionBuilder.of()
                .textLineItemId("text_line_item_id")
                .name(ofEnglish("newName"))
                .build());
  }

  @Test
  void buildChangeTextLineItemNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
    when(oldTextLineItem.getName()).thenReturn(ofEnglish("oldName"));

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of().name(ofEnglish("oldName")).quantity(1L).build();

    final Optional<ShoppingListUpdateAction> updateAction =
        TextLineItemUpdateActionUtils.buildChangeTextLineItemNameUpdateAction(
            oldTextLineItem, newTextLineItem);

    assertThat(updateAction).isNotNull();
    assertThat(updateAction).isNotPresent();
  }

  @Test
  void buildTextLineItemUpdateActions_WithSameValues_ShouldNotBuildUpdateAction() {
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

    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
    when(oldTextLineItem.getName()).thenReturn(ofEnglish("name"));
    when(oldTextLineItem.getDescription()).thenReturn(ofEnglish("desc"));
    when(oldTextLineItem.getQuantity()).thenReturn(1L);
    when(oldTextLineItem.getCustom()).thenReturn(oldCustomFields);

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("name"))
            .quantity(1L)
            .description(ofEnglish("desc"))
            .custom(newCustomFieldsDraft)
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildTextLineItemUpdateActions_WithDifferentValues_ShouldBuildUpdateAction() {
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

    final TextLineItem oldTextLineItem = mock(TextLineItem.class);
    when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
    when(oldTextLineItem.getName()).thenReturn(ofEnglish("name"));
    when(oldTextLineItem.getDescription()).thenReturn(ofEnglish("desc"));
    when(oldTextLineItem.getQuantity()).thenReturn(1L);
    when(oldTextLineItem.getCustom()).thenReturn(oldCustomFields);

    final TextLineItemDraft newTextLineItem =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("newName"))
            .quantity(2L)
            .description(ofEnglish("newDesc"))
            .custom(newCustomFieldsDraft)
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListChangeTextLineItemNameActionBuilder.of()
                .textLineItemId("text_line_item_id")
                .name(ofEnglish("newName"))
                .build(),
            ShoppingListSetTextLineItemDescriptionActionBuilder.of()
                .textLineItemId("text_line_item_id")
                .description(ofEnglish("newDesc"))
                .build(),
            ShoppingListChangeTextLineItemQuantityActionBuilder.of()
                .textLineItemId("text_line_item_id")
                .quantity(2L)
                .build(),
            ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
                .name("field1")
                .value(false)
                .textLineItemId("text_line_item_id")
                .build(),
            ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
                .name("field2")
                .value(Map.of("es", "val2"))
                .textLineItemId("text_line_item_id")
                .build());
  }
}
