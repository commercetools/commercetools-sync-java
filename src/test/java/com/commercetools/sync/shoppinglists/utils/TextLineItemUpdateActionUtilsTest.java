package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.TextLineItem;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeTextLineItemName;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeTextLineItemQuantity;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetTextLineItemCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetTextLineItemCustomType;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetTextLineItemDescription;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.commercetools.sync.shoppinglists.utils.TextLineItemUpdateActionUtils.buildChangeTextLineItemNameUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.TextLineItemUpdateActionUtils.buildChangeTextLineItemQuantityUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.TextLineItemUpdateActionUtils.buildSetTextLineItemDescriptionUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.TextLineItemUpdateActionUtils.buildTextLineItemCustomUpdateActions;
import static com.commercetools.sync.shoppinglists.utils.TextLineItemUpdateActionUtils.buildTextLineItemUpdateActions;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TextLineItemUpdateActionUtilsTest {

    private static final ShoppingListSyncOptions SYNC_OPTIONS =
        ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);

    @Test
    void buildTextLineItemCustomUpdateActions_WithSameValues_ShouldNotBuildUpdateAction() {
        final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
        oldCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFieldsMap.put("field2", JsonNodeFactory.instance.objectNode().put("de", "val"));

        final CustomFields oldCustomFields = mock(CustomFields.class);
        when(oldCustomFields.getType()).thenReturn(Type.referenceOfId("1"));
        when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

        final CustomFieldsDraft newCustomFieldsDraft =
            CustomFieldsDraft.ofTypeIdAndJson("1", oldCustomFieldsMap);

        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getCustom()).thenReturn(oldCustomFields);

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name"), 1L)
                                    .description(LocalizedString.ofEnglish("desc"))
                                    .addedAt(ZonedDateTime.now())
                                    .custom(newCustomFieldsDraft)
                                    .build();

        final List<UpdateAction<ShoppingList>> updateActions = buildTextLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, SYNC_OPTIONS);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildTextLineItemCustomUpdateActions_WithDifferentValues_ShouldBuildUpdateAction() {
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

        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getCustom()).thenReturn(oldCustomFields);

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name"), 1L)
                                    .description(LocalizedString.ofEnglish("desc"))
                                    .addedAt(ZonedDateTime.now())
                                    .custom(newCustomFieldsDraft)
                                    .build();

        final List<UpdateAction<ShoppingList>> updateActions = buildTextLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            SetTextLineItemCustomField.ofJson("field1",
                JsonNodeFactory.instance.booleanNode(false), "text_line_item_id"),
            SetTextLineItemCustomField.ofJson("field2",
                JsonNodeFactory.instance.objectNode().put("es", "val2"), "text_line_item_id")
        );
    }

    @Test
    void buildTextLineItemCustomUpdateActions_WithNullOldValues_ShouldBuildUpdateAction() {
        final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
        newCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(false));
        newCustomFieldsMap.put("field2", JsonNodeFactory.instance.objectNode().put("es", "val"));

        final CustomFieldsDraft newCustomFieldsDraft =
            CustomFieldsDraft.ofTypeIdAndJson("1", newCustomFieldsMap);

        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getCustom()).thenReturn(null);

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name"), 1L)
                                    .description(LocalizedString.ofEnglish("desc"))
                                    .addedAt(ZonedDateTime.now())
                                    .custom(newCustomFieldsDraft)
                                    .build();

        final List<UpdateAction<ShoppingList>> updateActions = buildTextLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            SetTextLineItemCustomType.ofTypeIdAndJson("1", newCustomFieldsMap, "text_line_item_id")
        );
    }

    @Test
    void buildTextLineItemCustomUpdateActions_WithBadCustomFieldData_ShouldNotBuildUpdateActionAndTriggerCallback() {
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

        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getCustom()).thenReturn(oldCustomFields);

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name"), 1L)
                                    .description(LocalizedString.ofEnglish("desc"))
                                    .addedAt(ZonedDateTime.now())
                                    .custom(newCustomFieldsDraft)
                                    .build();

        final List<String> errors = new ArrayList<>();

        final ShoppingListSyncOptions syncOptions =
            ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class))
                                          .errorCallback((exception, oldResource, newResource, updateActions) ->
                                              errors.add(exception.getMessage()))
                                          .build();

        final List<UpdateAction<ShoppingList>> updateActions = buildTextLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, syncOptions);

        assertThat(updateActions).isEmpty();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0))
            .isEqualTo(format("Failed to build custom fields update actions on the shopping-list-text-line-item with "
                + "id '%s'. Reason: Custom type ids are not set for both the old "
                + "and new shopping-list-text-line-item.", oldTextLineItem.getId()));
    }

    @Test
    void buildTextLineItemCustomUpdateActions_WithNullValue_ShouldCorrectlyBuildAction() {
        final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
        oldCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFieldsMap.put("field2", JsonNodeFactory
            .instance
            .arrayNode()
            .add(JsonNodeFactory.instance.booleanNode(false)));

        final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
        newCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(true));
        newCustomFieldsMap.put("field2", null);


        final CustomFields oldCustomFields = mock(CustomFields.class);
        final String typeId = UUID.randomUUID().toString();
        when(oldCustomFields.getType()).thenReturn(Type.referenceOfId(typeId));
        when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

        final CustomFieldsDraft newCustomFieldsDraft =
            CustomFieldsDraft.ofTypeIdAndJson(typeId, newCustomFieldsMap);

        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getCustom()).thenReturn(oldCustomFields);

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name"), 1L)
                                    .description(LocalizedString.ofEnglish("desc"))
                                    .addedAt(ZonedDateTime.now())
                                    .custom(newCustomFieldsDraft)
                                    .build();

        final List<String> errors = new ArrayList<>();

        final ShoppingListSyncOptions syncOptions =
            ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class))
                                          .errorCallback((exception, oldResource, newResource, updateActions) ->
                                              errors.add(exception.getMessage()))
                                          .build();

        final List<UpdateAction<ShoppingList>> updateActions = buildTextLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, syncOptions);

        assertThat(errors).isEmpty();
        assertThat(updateActions)
            .containsExactly(SetTextLineItemCustomField.ofJson("field2", null, "text_line_item_id"));
    }

    @Test
    void buildTextLineItemCustomUpdateActions_WithNullJsonNodeValue_ShouldCorrectlyBuildAction() {
        final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
        oldCustomFieldsMap.put("field", JsonNodeFactory
            .instance
            .arrayNode()
            .add(JsonNodeFactory.instance.booleanNode(false)));

        final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
        newCustomFieldsMap.put("field", JsonNodeFactory.instance.nullNode());


        final CustomFields oldCustomFields = mock(CustomFields.class);
        final String typeId = UUID.randomUUID().toString();
        when(oldCustomFields.getType()).thenReturn(Type.referenceOfId(typeId));
        when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

        final CustomFieldsDraft newCustomFieldsDraft =
            CustomFieldsDraft.ofTypeIdAndJson(typeId, newCustomFieldsMap);

        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getCustom()).thenReturn(oldCustomFields);

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name"), 1L)
                                    .description(LocalizedString.ofEnglish("desc"))
                                    .addedAt(ZonedDateTime.now())
                                    .custom(newCustomFieldsDraft)
                                    .build();

        final List<String> errors = new ArrayList<>();

        final ShoppingListSyncOptions syncOptions =
            ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class))
                                          .errorCallback((exception, oldResource, newResource, updateActions) ->
                                              errors.add(exception.getMessage()))
                                          .build();

        final List<UpdateAction<ShoppingList>> updateActions = buildTextLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, syncOptions);

        assertThat(errors).isEmpty();
        assertThat(updateActions)
            .containsExactly(SetTextLineItemCustomField.ofJson("field", null, "text_line_item_id"));
    }

    @Test
    void buildChangeTextLineItemQuantityUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final TextLineItem oldLineItem = mock(TextLineItem.class);
        when(oldLineItem.getId()).thenReturn("text_line_item_id");
        when(oldLineItem.getQuantity()).thenReturn(2L);

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name"), 2L)
                                    .addedAt(ZonedDateTime.now())
                                    .build();

        final Optional<UpdateAction<ShoppingList>> updateAction =
            buildChangeTextLineItemQuantityUpdateAction(oldLineItem, newTextLineItem);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isNotPresent();
    }

    @Test
    void buildChangeTextLineItemQuantityUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final TextLineItem oldLineItem = mock(TextLineItem.class);
        when(oldLineItem.getId()).thenReturn("text_line_item_id");
        when(oldLineItem.getQuantity()).thenReturn(2L);

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name"), 4L)
                                    .addedAt(ZonedDateTime.now())
                                    .build();

        final UpdateAction<ShoppingList> updateAction =
            buildChangeTextLineItemQuantityUpdateAction(oldLineItem, newTextLineItem).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("changeTextLineItemQuantity");
        assertThat((ChangeTextLineItemQuantity) updateAction)
            .isEqualTo(ChangeTextLineItemQuantity.of("text_line_item_id", 4L));
    }

    @Test
    void buildChangeTextLineItemQuantityUpdateAction_WithNewNullValue_ShouldBuildUpdateAction() {
        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getQuantity()).thenReturn(2L);

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name"), null)
                                    .addedAt(ZonedDateTime.now())
                                    .build();

        final UpdateAction<ShoppingList> updateAction =
            buildChangeTextLineItemQuantityUpdateAction(oldTextLineItem, newTextLineItem).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("changeTextLineItemQuantity");
        assertThat((ChangeTextLineItemQuantity) updateAction)
            .isEqualTo(ChangeTextLineItemQuantity.of("text_line_item_id", 1L));
    }

    @Test
    void buildChangeTextLineItemQuantityUpdateAction_WithNewZeroValue_ShouldBuildUpdateAction() {
        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getQuantity()).thenReturn(2L);

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name"), 0L)
                                    .addedAt(ZonedDateTime.now())
                                    .build();

        final UpdateAction<ShoppingList> updateAction =
            buildChangeTextLineItemQuantityUpdateAction(oldTextLineItem, newTextLineItem).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("changeTextLineItemQuantity");
        assertThat((ChangeTextLineItemQuantity) updateAction)
            .isEqualTo(ChangeTextLineItemQuantity.of("text_line_item_id", 0L));
    }

    @Test
    void buildChangeTextLineItemQuantityUpdateAction_WithNewNullValueAndOldDefaultValue_ShouldNotBuildAction() {
        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getQuantity()).thenReturn(1L);

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name"), null)
                                    .addedAt(ZonedDateTime.now())
                                    .build();

        final Optional<UpdateAction<ShoppingList>> updateAction =
            buildChangeTextLineItemQuantityUpdateAction(oldTextLineItem, newTextLineItem);

        assertThat(updateAction).isNotPresent();
    }

    @Test
    void buildSetTextLineItemDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getDescription()).thenReturn(LocalizedString.ofEnglish("oldDescription"));

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name"), 1L)
                                    .description(LocalizedString.ofEnglish("newDescription"))
                                    .build();

        final UpdateAction<ShoppingList> updateAction =
            buildSetTextLineItemDescriptionUpdateAction(oldTextLineItem, newTextLineItem).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("setTextLineItemDescription");
        assertThat(((SetTextLineItemDescription) updateAction))
            .isEqualTo(SetTextLineItemDescription.of("text_line_item_id")
                                                 .withDescription(LocalizedString.ofEnglish("newDescription")));
    }

    @Test
    void buildSetTextLineItemDescriptionUpdateAction_WithNullNewValue_ShouldBuildUpdateAction() {
        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getDescription()).thenReturn(LocalizedString.ofEnglish("oldDescription"));

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(null, 1L)
                                    .build();

        final UpdateAction<ShoppingList> updateAction =
            buildSetTextLineItemDescriptionUpdateAction(oldTextLineItem, newTextLineItem).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("setTextLineItemDescription");
        assertThat(((SetTextLineItemDescription) updateAction))
            .isEqualTo(SetTextLineItemDescription.of("text_line_item_id")
                                                 .withDescription(null));
    }

    @Test
    void buildSetTextLineItemDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getDescription()).thenReturn(LocalizedString.ofEnglish("oldDescription"));

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name"), 1L)
                                    .description(LocalizedString.ofEnglish("oldDescription"))
                                    .build();

        final Optional<UpdateAction<ShoppingList>> updateAction =
            buildSetTextLineItemDescriptionUpdateAction(oldTextLineItem, newTextLineItem);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isNotPresent();
    }

    @Test
    void buildChangeTextLineItemNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getName()).thenReturn(LocalizedString.ofEnglish("oldName"));

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("newName"), 1L)
                                    .build();

        final UpdateAction<ShoppingList> updateAction =
            buildChangeTextLineItemNameUpdateAction(oldTextLineItem, newTextLineItem).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("changeTextLineItemName");
        assertThat(((ChangeTextLineItemName) updateAction)).isEqualTo(
            ChangeTextLineItemName.of("text_line_item_id", LocalizedString.ofEnglish("newName")));
    }

    @Test
    void buildChangeTextLineItemNameUpdateAction_WithNullNewValue_ShouldBuildUpdateAction() {
        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getName()).thenReturn(LocalizedString.ofEnglish("oldName"));

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(null, 1L)
                                    .build();

        final UpdateAction<ShoppingList> updateAction =
            buildChangeTextLineItemNameUpdateAction(oldTextLineItem, newTextLineItem).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction.getAction()).isEqualTo("changeTextLineItemName");
        assertThat(((ChangeTextLineItemName) updateAction))
            .isEqualTo(ChangeTextLineItemName.of("text_line_item_id", null));
    }

    @Test
    void buildChangeTextLineItemNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getName()).thenReturn(LocalizedString.ofEnglish("oldName"));

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("oldName"), 1L)
                                    .build();

        final Optional<UpdateAction<ShoppingList>> updateAction =
            buildChangeTextLineItemNameUpdateAction(oldTextLineItem, newTextLineItem);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isNotPresent();
    }

    @Test
    void buildTextLineItemUpdateActions_WithSameValues_ShouldNotBuildUpdateAction() {
        final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
        oldCustomFieldsMap.put("field1", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFieldsMap.put("field2", JsonNodeFactory.instance.objectNode().put("de", "val"));

        final CustomFields oldCustomFields = mock(CustomFields.class);
        when(oldCustomFields.getType()).thenReturn(Type.referenceOfId("1"));
        when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

        final CustomFieldsDraft newCustomFieldsDraft =
            CustomFieldsDraft.ofTypeIdAndJson("1", oldCustomFieldsMap);

        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getName()).thenReturn(LocalizedString.ofEnglish("name"));
        when(oldTextLineItem.getDescription()).thenReturn(LocalizedString.ofEnglish("desc"));
        when(oldTextLineItem.getQuantity()).thenReturn(1L);
        when(oldTextLineItem.getCustom()).thenReturn(oldCustomFields);

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name"), 1L)
                                    .description(LocalizedString.ofEnglish("desc"))
                                    .custom(newCustomFieldsDraft)
                                    .build();

        final List<UpdateAction<ShoppingList>> updateActions = buildTextLineItemUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, SYNC_OPTIONS);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildTextLineItemUpdateActions_WithDifferentValues_ShouldBuildUpdateAction() {
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

        final TextLineItem oldTextLineItem = mock(TextLineItem.class);
        when(oldTextLineItem.getId()).thenReturn("text_line_item_id");
        when(oldTextLineItem.getName()).thenReturn(LocalizedString.ofEnglish("name"));
        when(oldTextLineItem.getDescription()).thenReturn(LocalizedString.ofEnglish("desc"));
        when(oldTextLineItem.getQuantity()).thenReturn(1L);
        when(oldTextLineItem.getCustom()).thenReturn(oldCustomFields);

        final TextLineItemDraft newTextLineItem =
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("newName"), 2L)
                                    .description(LocalizedString.ofEnglish("newDesc"))
                                    .custom(newCustomFieldsDraft)
                                    .build();

        final List<UpdateAction<ShoppingList>> updateActions = buildTextLineItemUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            ChangeTextLineItemName.of("text_line_item_id", LocalizedString.ofEnglish("newName")),
            SetTextLineItemDescription.of("text_line_item_id").withDescription(LocalizedString.ofEnglish("newDesc")),
            ChangeTextLineItemQuantity.of("text_line_item_id", 2L),
            SetTextLineItemCustomField.ofJson("field1",
                JsonNodeFactory.instance.booleanNode(false), "text_line_item_id"),
            SetTextLineItemCustomField.ofJson("field2",
                JsonNodeFactory.instance.objectNode().put("es", "val2"), "text_line_item_id")
        );
    }
}
