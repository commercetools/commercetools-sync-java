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
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetAnonymousId;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomType;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDeleteDaysAfterLastModification;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDescription;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetSlug;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.CustomFieldsDraftBuilder;
import io.sphere.sdk.types.Type;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.commercetools.sync.shoppinglists.utils.ShoppingListSyncUtils.buildActions;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShoppingListSyncUtilsTest {
    private static final Locale LOCALE = Locale.GERMAN;

    private static final String CUSTOM_TYPE_ID = "id";
    private static final String CUSTOM_FIELD_NAME = "field";
    private static final String CUSTOM_FIELD_VALUE = "value";

    private ShoppingList oldShoppingList;

    @BeforeEach
    void setup() {
        oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getName()).thenReturn(LocalizedString.ofEnglish("name"));
        when(oldShoppingList.getDeleteDaysAfterLastModification()).thenReturn(null);

        final CustomFields customFields = mock(CustomFields.class);
        when(customFields.getType()).thenReturn(Type.referenceOfId(CUSTOM_TYPE_ID));

        final Map<String, JsonNode> customFieldsJsonMapMock = new HashMap<>();
        customFieldsJsonMapMock.put(CUSTOM_FIELD_NAME, JsonNodeFactory.instance.textNode(CUSTOM_FIELD_VALUE));
        when(customFields.getFieldsJsonMap()).thenReturn(customFieldsJsonMapMock);

        when(oldShoppingList.getCustom()).thenReturn(customFields);
    }

    @Test
    void buildActions_WithDifferentValues_ShouldReturnActions() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "oldSlug"));
        when(oldShoppingList.getName()).thenReturn(LocalizedString.of(LOCALE, "oldName"));
        when(oldShoppingList.getDescription()).thenReturn(LocalizedString.of(LOCALE, "oldDescription"));
        when(oldShoppingList.getAnonymousId()).thenReturn("oldAnonymousId");
        when(oldShoppingList.getDeleteDaysAfterLastModification()).thenReturn(50);

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "newSlug"));
        when(newShoppingList.getName()).thenReturn(LocalizedString.of(LOCALE, "newName"));
        when(newShoppingList.getDescription()).thenReturn(LocalizedString.of(LOCALE, "newDescription"));
        when(newShoppingList.getAnonymousId()).thenReturn("newAnonymousId");
        when(newShoppingList.getDeleteDaysAfterLastModification()).thenReturn(70);

        final List<UpdateAction<ShoppingList>> updateActions = buildActions(oldShoppingList, newShoppingList,
            mock(ShoppingListSyncOptions.class));

        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).contains(SetSlug.of(newShoppingList.getSlug()));
        assertThat(updateActions).contains(ChangeName.of(newShoppingList.getName()));
        assertThat(updateActions).contains(SetDescription.of(newShoppingList.getDescription()));
        assertThat(updateActions).contains(SetAnonymousId.of(newShoppingList.getAnonymousId()));
        assertThat(updateActions).contains(
            SetDeleteDaysAfterLastModification.of(newShoppingList.getDeleteDaysAfterLastModification()));
    }

    @Test
    void buildActions_WithDifferentCustomType_ShouldBuildUpdateAction() {
        final CustomFieldsDraft customFieldsDraft =
            CustomFieldsDraftBuilder.ofTypeId("newId")
                                    .addObject("newField", "newValue")
                                    .build();

        final ShoppingListDraft newShoppingList =
            ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name"))
                                    .custom(customFieldsDraft)
                                    .build();

        final List<UpdateAction<ShoppingList>> actions = buildActions(oldShoppingList, newShoppingList,
            ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build());

        Assertions.assertThat(actions).containsExactly(
            SetCustomType.ofTypeIdAndJson(customFieldsDraft.getType().getId(), customFieldsDraft.getFields()));
    }

    @Test
    void buildActions_WithSameCustomTypeWithNewCustomFields_ShouldBuildUpdateAction() {
        final CustomFieldsDraft sameCustomFieldDraftWithNewCustomField =
            CustomFieldsDraftBuilder.ofTypeId(CUSTOM_TYPE_ID)
                                    .addObject(CUSTOM_FIELD_NAME, CUSTOM_FIELD_VALUE)
                                    .addObject("name_2", "value_2")
                                    .build();

        final ShoppingListDraft newShoppingList =
            ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name"))
                                    .custom(sameCustomFieldDraftWithNewCustomField)
                                    .build();

        final List<UpdateAction<ShoppingList>> actions = buildActions(oldShoppingList, newShoppingList,
            ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build());

        Assertions.assertThat(actions).containsExactly(
            SetCustomField.ofJson("name_2", JsonNodeFactory.instance.textNode("value_2")));
    }

    @Test
    void buildActions_WithSameCustomTypeWithDifferentCustomFieldValues_ShouldBuildUpdateAction() {

        final CustomFieldsDraft sameCustomFieldDraftWithNewValue =
            CustomFieldsDraftBuilder.ofTypeId(CUSTOM_TYPE_ID)
                                    .addObject(CUSTOM_FIELD_NAME,
                                        "newValue")
                                    .build();

        final ShoppingListDraft newShoppingList =
            ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name"))
                                    .custom(sameCustomFieldDraftWithNewValue)
                                    .build();

        final List<UpdateAction<ShoppingList>> actions = buildActions(oldShoppingList, newShoppingList,
            ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build());

        Assertions.assertThat(actions).containsExactly(
            SetCustomField.ofJson(CUSTOM_FIELD_NAME, JsonNodeFactory.instance.textNode("newValue")));
    }

    @Test
    void buildActions_WithJustNewShoppingListHasNullCustomType_ShouldBuildUpdateAction() {
        final ShoppingListDraft newShoppingList =
            ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name"))
                                    .custom(null)
                                    .build();

        final List<UpdateAction<ShoppingList>> actions = buildActions(oldShoppingList, newShoppingList,
            ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build());

        Assertions.assertThat(actions).containsExactly(SetCustomType.ofRemoveType());
    }
}
