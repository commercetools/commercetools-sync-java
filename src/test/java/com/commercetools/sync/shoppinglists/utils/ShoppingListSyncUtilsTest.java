package com.commercetools.sync.shoppinglists.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.LocalizedStringBuilder;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListChangeNameActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetAnonymousIdActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetCustomFieldActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetCustomTypeActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetDeleteDaysAfterLastModificationActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetDescriptionActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetSlugActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ShoppingListSyncUtilsTest {
  private static final Locale LOCALE = Locale.GERMAN;

  private static final String CUSTOM_TYPE_ID = "id";
  private static final String CUSTOM_FIELD_NAME = "field";
  private static final String CUSTOM_FIELD_VALUE = "value";

  private ShoppingList oldShoppingList;

  @BeforeEach
  void setup() {
    oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getName()).thenReturn(ofEnglish("name"));
    when(oldShoppingList.getDeleteDaysAfterLastModification()).thenReturn(null);

    final CustomFields customFields = mock(CustomFields.class);
    when(customFields.getType()).thenReturn(TypeReferenceBuilder.of().id(CUSTOM_TYPE_ID).build());

    final Map<String, Object> customFieldsJsonMapMock = new HashMap<>();
    customFieldsJsonMapMock.put(CUSTOM_FIELD_NAME, CUSTOM_FIELD_VALUE);
    when(customFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(customFieldsJsonMapMock).build());

    when(oldShoppingList.getCustom()).thenReturn(customFields);
  }

  @Test
  void buildActions_WithDifferentValuesExceptLineItems_ShouldReturnActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getSlug())
        .thenReturn(LocalizedStringBuilder.of().addValue(LOCALE.getLanguage(), "oldSlug").build());
    when(oldShoppingList.getName())
        .thenReturn(LocalizedStringBuilder.of().addValue(LOCALE.getLanguage(), "oldName").build());
    when(oldShoppingList.getDescription())
        .thenReturn(
            LocalizedStringBuilder.of().addValue(LOCALE.getLanguage(), "oldDescription").build());
    when(oldShoppingList.getAnonymousId()).thenReturn("oldAnonymousId");
    when(oldShoppingList.getDeleteDaysAfterLastModification()).thenReturn(50L);

    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getSlug())
        .thenReturn(LocalizedStringBuilder.of().addValue(LOCALE.getLanguage(), "newSlug").build());
    when(newShoppingList.getName())
        .thenReturn(LocalizedStringBuilder.of().addValue(LOCALE.getLanguage(), "newName").build());
    when(newShoppingList.getDescription())
        .thenReturn(
            LocalizedStringBuilder.of().addValue(LOCALE.getLanguage(), "newDescription").build());
    when(newShoppingList.getAnonymousId()).thenReturn("newAnonymousId");
    when(newShoppingList.getDeleteDaysAfterLastModification()).thenReturn(70L);

    final List<ShoppingListUpdateAction> updateActions =
        ShoppingListSyncUtils.buildActions(
            oldShoppingList, newShoppingList, Mockito.mock(ShoppingListSyncOptions.class));

    assertThat(updateActions).isNotEmpty();
    assertThat(updateActions)
        .contains(ShoppingListSetSlugActionBuilder.of().slug(newShoppingList.getSlug()).build());
    assertThat(updateActions)
        .contains(ShoppingListChangeNameActionBuilder.of().name(newShoppingList.getName()).build());
    assertThat(updateActions)
        .contains(
            ShoppingListSetDescriptionActionBuilder.of()
                .description(newShoppingList.getDescription())
                .build());
    assertThat(updateActions)
        .contains(
            ShoppingListSetAnonymousIdActionBuilder.of()
                .anonymousId(newShoppingList.getAnonymousId())
                .build());
    assertThat(updateActions)
        .contains(
            ShoppingListSetDeleteDaysAfterLastModificationActionBuilder.of()
                .deleteDaysAfterLastModification(
                    newShoppingList.getDeleteDaysAfterLastModification())
                .build());
  }

  @Test
  void buildActions_WithDifferentCustomType_ShouldBuildUpdateAction() {
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("newId"))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.addValue("newField", "newValue"))
            .build();

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of().name(ofEnglish("name")).custom(customFieldsDraft).build();

    final List<ShoppingListUpdateAction> actions =
        ShoppingListSyncUtils.buildActions(
            oldShoppingList,
            newShoppingList,
            ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions)
        .containsExactly(
            ShoppingListSetCustomTypeActionBuilder.of()
                .type(
                    typeResourceIdentifierBuilder ->
                        typeResourceIdentifierBuilder.id(customFieldsDraft.getType().getId()))
                .fields(customFieldsDraft.getFields())
                .build());
  }

  @Test
  void buildActions_WithSameCustomTypeWithNewCustomFields_ShouldBuildUpdateAction() {
    final CustomFieldsDraft sameCustomFieldDraftWithNewCustomField =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(CUSTOM_TYPE_ID))
            .fields(
                fieldContainerBuilder ->
                    fieldContainerBuilder
                        .addValue(CUSTOM_FIELD_NAME, CUSTOM_FIELD_VALUE)
                        .addValue("name_2", "value_2"))
            .build();

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("name"))
            .custom(sameCustomFieldDraftWithNewCustomField)
            .build();

    final List<ShoppingListUpdateAction> actions =
        ShoppingListSyncUtils.buildActions(
            oldShoppingList,
            newShoppingList,
            ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions)
        .containsExactly(
            ShoppingListSetCustomFieldActionBuilder.of().name("name_2").value("value_2").build());
  }

  @Test
  void buildActions_WithSameCustomTypeWithDifferentCustomFieldValues_ShouldBuildUpdateAction() {

    final CustomFieldsDraft sameCustomFieldDraftWithNewValue =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(CUSTOM_TYPE_ID))
            .fields(
                fieldContainerBuilder ->
                    fieldContainerBuilder.addValue(CUSTOM_FIELD_NAME, "newValue"))
            .build();

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("name"))
            .custom(sameCustomFieldDraftWithNewValue)
            .build();

    final List<ShoppingListUpdateAction> actions =
        ShoppingListSyncUtils.buildActions(
            oldShoppingList,
            newShoppingList,
            ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions)
        .containsExactly(
            ShoppingListSetCustomFieldActionBuilder.of()
                .name(CUSTOM_FIELD_NAME)
                .value("newValue")
                .build());
  }

  @Test
  void buildActions_WithJustNewShoppingListHasNullCustomType_ShouldBuildUpdateAction() {
    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("name"))
            .custom((CustomFieldsDraft) null)
            .build();

    final List<ShoppingListUpdateAction> actions =
        ShoppingListSyncUtils.buildActions(
            oldShoppingList,
            newShoppingList,
            ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions).containsExactly(ShoppingListSetCustomTypeActionBuilder.of().build());
  }
}
