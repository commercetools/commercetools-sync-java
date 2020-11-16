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
import io.sphere.sdk.shoppinglists.commands.updateactions.SetTextLineItemCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetTextLineItemCustomType;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.commercetools.sync.shoppinglists.utils.TextLineItemUpdateActionUtils.buildTextLineItemCustomUpdateActions;
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
                + "id '%s'. Reason: Custom type ids are not set for both the old and new shopping-list-text-line-item.",
                oldTextLineItem.getId()));
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
}
