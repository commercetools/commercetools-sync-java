package com.commercetools.sync.commons.utils;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildNonNullCustomFieldsUpdateActions;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildPrimaryResourceCustomUpdateActions;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildSetCustomFieldsUpdateActions;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.categories.CategorySyncMockUtils;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.CategoryCustomActionBuilder;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.updateactions.SetCustomField;
import io.sphere.sdk.categories.commands.updateactions.SetCustomType;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourceCustomUpdateActionUtilsTest {
  private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
  private ArrayList<String> errorMessages;
  private ArrayList<Throwable> exceptions;
  private CategorySyncOptions categorySyncOptions;

  @BeforeEach
  void setupTest() {
    errorMessages = new ArrayList<>();
    exceptions = new ArrayList<>();
    final QuadConsumer<
            SyncException,
            Optional<CategoryDraft>,
            Optional<Category>,
            List<UpdateAction<Category>>>
        errorCallback =
            (exception, newResource, oldResource, updateActions) -> {
              errorMessages.add(exception.getMessage());
              exceptions.add(exception.getCause());
            };

    // Mock sync options
    categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT).errorCallback(errorCallback).build();
  }

  @Test
  void
      buildResourceCustomUpdateActions_WithNonNullCustomFieldsWithDifferentTypes_ShouldBuildUpdateActions() {
    final Category oldCategory = mock(Category.class);
    final CustomFields oldCategoryCustomFields = mock(CustomFields.class);
    final Reference<Type> oldCategoryCustomFieldsDraftTypeReference = Type.referenceOfId("2");
    when(oldCategoryCustomFields.getType()).thenReturn(oldCategoryCustomFieldsDraftTypeReference);
    when(oldCategory.getCustom()).thenReturn(oldCategoryCustomFields);

    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    final CustomFieldsDraft newCategoryCustomFieldsDraft = mock(CustomFieldsDraft.class);

    final ResourceIdentifier<Type> typeResourceIdentifier = Type.referenceOfId("1");
    when(newCategoryCustomFieldsDraft.getType()).thenReturn(typeResourceIdentifier);
    when(newCategoryDraft.getCustom()).thenReturn(newCategoryCustomFieldsDraft);

    final List<UpdateAction<Category>> updateActions =
        buildPrimaryResourceCustomUpdateActions(
            oldCategory, newCategoryDraft, new CategoryCustomActionBuilder(), categorySyncOptions);

    // Should set custom type of old category.
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(SetCustomType.class);
  }

  @Test
  void buildResourceCustomUpdateActions_WithNullOldCustomFields_ShouldBuildUpdateActions() {
    final Category oldCategory = mock(Category.class);
    when(oldCategory.getCustom()).thenReturn(null);

    final CategoryDraft newCategoryDraft =
        CategoryDraftBuilder.of(
                LocalizedString.ofEnglish("name"), LocalizedString.ofEnglish("testSlug"))
            .key("key")
            .parent(ResourceIdentifier.ofId("parentId"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson("customTypeId", new HashMap<>()))
            .build();

    final List<UpdateAction<Category>> updateActions =
        buildPrimaryResourceCustomUpdateActions(
            oldCategory, newCategoryDraft, new CategoryCustomActionBuilder(), categorySyncOptions);

    // Should add custom type to old category.
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(SetCustomType.class);
  }

  @Test
  void
      buildResourceCustomUpdateActions_WithNullOldCustomFieldsAndBlankNewTypeId_ShouldCallErrorCallBack() {
    final Category oldCategory = mock(Category.class);
    when(oldCategory.getCustom()).thenReturn(null);
    final String oldCategoryId = "oldCategoryId";
    when(oldCategory.getId()).thenReturn(oldCategoryId);
    when(oldCategory.toReference()).thenReturn(Category.referenceOfId(oldCategoryId));

    final CategoryDraft newCategoryDraft =
        CategorySyncMockUtils.getMockCategoryDraft(Locale.ENGLISH, "name", "slug", "key");
    final CustomFieldsDraft mockCustomFieldsDraft =
        CustomFieldsDraft.ofTypeKeyAndJson("key", new HashMap<>());
    when(newCategoryDraft.getCustom()).thenReturn(mockCustomFieldsDraft);

    final List<UpdateAction<Category>> updateActions =
        buildPrimaryResourceCustomUpdateActions(
            oldCategory, newCategoryDraft, new CategoryCustomActionBuilder(), categorySyncOptions);

    // Should add custom type to old category.
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(0);
    assertThat(errorMessages.get(0))
        .isEqualTo(
            format(
                "Failed to build custom fields update actions on the "
                    + "category with id '%s'. Reason: New resource's custom type id is blank (empty/null).",
                oldCategoryId));
    assertThat(exceptions.get(0)).isNull();
  }

  @Test
  void buildResourceCustomUpdateActions_WithNullNewCustomFields_ShouldBuildUpdateActions() {
    final Category oldCategory = mock(Category.class);
    final CustomFields oldCategoryCustomFields = mock(CustomFields.class);
    when(oldCategory.getCustom()).thenReturn(oldCategoryCustomFields);

    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getCustom()).thenReturn(null);

    final List<UpdateAction<Category>> updateActions =
        buildPrimaryResourceCustomUpdateActions(
            oldCategory, newCategoryDraft, new CategoryCustomActionBuilder(), categorySyncOptions);

    // Should remove custom type from old category.
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(SetCustomType.class);
  }

  @Test
  void buildResourceCustomUpdateActions_WithNullIds_ShouldCallSyncOptionsCallBack() {
    final Reference<Type> categoryTypeReference = Type.referenceOfId(null);

    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
    when(oldCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

    // Mock old Category
    final Category oldCategory = mock(Category.class);
    when(oldCategory.getId()).thenReturn("oldCategoryId");
    when(oldCategory.toReference()).thenReturn(Category.referenceOfId("oldCategoryId"));
    when(oldCategory.getCustom()).thenReturn(oldCustomFieldsMock);

    // Mock new Category
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getCustom()).thenReturn(newCustomFieldsMock);

    final List<UpdateAction<Category>> updateActions =
        buildPrimaryResourceCustomUpdateActions(
            oldCategory, newCategoryDraft, new CategoryCustomActionBuilder(), categorySyncOptions);

    assertThat(errorMessages).hasSize(1);
    assertThat(exceptions).hasSize(1);
    assertThat(errorMessages.get(0))
        .isEqualTo(
            "Failed to build custom fields update actions on the category"
                + " with id 'oldCategoryId'. Reason: Custom type ids are not set for both the old and new category.");
    assertThat(exceptions.get(0)).isInstanceOf(BuildUpdateActionException.class);
    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildNonNullCustomFieldsUpdateActions_WithBothNullCustomFields_ShouldNotBuildUpdateActions() {
    final Category oldCategory = mock(Category.class);
    when(oldCategory.getCustom()).thenReturn(null);

    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getCustom()).thenReturn(null);

    final List<UpdateAction<Category>> updateActions =
        buildPrimaryResourceCustomUpdateActions(
            oldCategory, newCategoryDraft, new CategoryCustomActionBuilder(), categorySyncOptions);

    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildNonNullCustomFieldsUpdateActions_WithSameCategoryTypesButDifferentFieldValues_ShouldBuildUpdateActions()
          throws BuildUpdateActionException {
    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
    final Map<String, JsonNode> oldCustomFieldsJsonMapMock = new HashMap<>();
    oldCustomFieldsJsonMapMock.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
    when(oldCustomFieldsMock.getType()).thenReturn(Type.referenceOfId("categoryCustomTypeId"));
    when(oldCustomFieldsMock.getFieldsJsonMap()).thenReturn(oldCustomFieldsJsonMapMock);

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    final Map<String, JsonNode> newCustomFieldsJsonMapMock = new HashMap<>();
    newCustomFieldsJsonMapMock.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
    when(newCustomFieldsMock.getType()).thenReturn(Type.referenceOfId("categoryCustomTypeId"));
    when(newCustomFieldsMock.getFields()).thenReturn(newCustomFieldsJsonMapMock);

    final List<UpdateAction<Category>> updateActions =
        buildNonNullCustomFieldsUpdateActions(
            oldCustomFieldsMock,
            newCustomFieldsMock,
            mock(Category.class),
            new CategoryCustomActionBuilder(),
            null,
            Category::getId,
            category -> category.toReference().getTypeId(),
            category -> null,
            categorySyncOptions);

    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(SetCustomField.class);
  }

  @Test
  void buildNonNullCustomFieldsUpdateActions_WithDifferentCategoryTypeIds_ShouldBuildUpdateActions()
      throws BuildUpdateActionException {
    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
    when(oldCustomFieldsMock.getType()).thenReturn(Type.referenceOfId("categoryCustomTypeId"));

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    when(newCustomFieldsMock.getType())
        .thenReturn(ResourceIdentifier.ofId("newCategoryCustomTypeId"));

    final List<UpdateAction<Category>> updateActions =
        buildNonNullCustomFieldsUpdateActions(
            oldCustomFieldsMock,
            newCustomFieldsMock,
            mock(Category.class),
            new CategoryCustomActionBuilder(),
            null,
            Category::getId,
            category -> category.toReference().getTypeId(),
            category -> null,
            categorySyncOptions);

    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(SetCustomType.class);
  }

  @Test
  void buildNonNullCustomFieldsUpdateActions_WithNullOldCategoryTypeId_ShouldBuildUpdateActions()
      throws BuildUpdateActionException {
    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
    when(oldCustomFieldsMock.getType()).thenReturn(Type.referenceOfId(null));

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    when(newCustomFieldsMock.getType()).thenReturn(Type.referenceOfId("categoryCustomTypeId"));

    final List<UpdateAction<Category>> updateActions =
        buildNonNullCustomFieldsUpdateActions(
            oldCustomFieldsMock,
            newCustomFieldsMock,
            mock(Category.class),
            new CategoryCustomActionBuilder(),
            null,
            Category::getId,
            category -> category.toReference().getTypeId(),
            category -> null,
            categorySyncOptions);

    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(SetCustomType.class);
  }

  @Test
  void
      buildNonNullCustomFieldsUpdateActions_WithNullNewCategoryTypeId_TriggersErrorCallbackAndNoAction()
          throws BuildUpdateActionException {
    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
    when(oldCustomFieldsMock.getType()).thenReturn(Type.referenceOfId("1"));

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    final Reference<Type> value = Type.referenceOfId(null);

    when(newCustomFieldsMock.getType()).thenReturn(value);

    final String categoryId = UUID.randomUUID().toString();
    final Category category = mock(Category.class);
    when(category.getId()).thenReturn(categoryId);
    when(category.toReference()).thenReturn(Category.referenceOfId(categoryId));

    final List<UpdateAction<Category>> updateActions =
        buildNonNullCustomFieldsUpdateActions(
            oldCustomFieldsMock,
            newCustomFieldsMock,
            category,
            new CategoryCustomActionBuilder(),
            null,
            Category::getId,
            categoryRes -> category.toReference().getTypeId(),
            categoryRes -> null,
            categorySyncOptions);

    assertThat(errorMessages).hasSize(1);
    assertThat(errorMessages.get(0))
        .isEqualTo(
            format(
                "Failed to build 'setCustomType' update action on the "
                    + "%s with id '%s'. Reason: New Custom Type id is blank (null/empty).",
                category.toReference().getTypeId(), categoryId));
    assertThat(exceptions).hasSize(1);
    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildNonNullCustomFieldsUpdateActions_WithSameIdsButNullNewCustomFields_ShouldBuildUpdateActions()
          throws BuildUpdateActionException {
    final Reference<Type> categoryTypeReference = Type.referenceOfId("categoryCustomTypeId");

    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
    final Map<String, JsonNode> oldCustomFieldsJsonMapMock = new HashMap<>();
    oldCustomFieldsJsonMapMock.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
    when(oldCustomFieldsMock.getType()).thenReturn(categoryTypeReference);
    when(oldCustomFieldsMock.getFieldsJsonMap()).thenReturn(oldCustomFieldsJsonMapMock);

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);
    when(newCustomFieldsMock.getFields()).thenReturn(null);

    final List<UpdateAction<Category>> updateActions =
        buildNonNullCustomFieldsUpdateActions(
            oldCustomFieldsMock,
            newCustomFieldsMock,
            mock(Category.class),
            new CategoryCustomActionBuilder(),
            null,
            Category::getId,
            category -> category.toReference().getTypeId(),
            category -> null,
            categorySyncOptions);

    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(SetCustomType.class);
  }

  @Test
  void buildNonNullCustomFieldsUpdateActions_WithNullIds_ShouldThrowBuildUpdateActionException() {
    final Reference<Type> categoryTypeReference = Type.referenceOfId(null);

    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
    when(oldCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

    final Category oldCategory = mock(Category.class);
    when(oldCategory.getId()).thenReturn("oldCategoryId");
    when(oldCategory.toReference()).thenReturn(Category.referenceOfId(null));

    assertThatThrownBy(
            () ->
                buildNonNullCustomFieldsUpdateActions(
                    oldCustomFieldsMock,
                    newCustomFieldsMock,
                    oldCategory,
                    new CategoryCustomActionBuilder(),
                    null,
                    Category::getId,
                    category -> category.toReference().getTypeId(),
                    category -> null,
                    categorySyncOptions))
        .isInstanceOf(BuildUpdateActionException.class)
        .hasMessageMatching("Custom type ids are not set for both the old and new category.");
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithDifferentCustomFieldValues_ShouldBuildUpdateActions() {
    final Map<String, JsonNode> oldCustomFields = new HashMap<>();
    oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
    oldCustomFields.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

    final Map<String, JsonNode> newCustomFields = new HashMap<>();
    newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
    newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

    final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            mock(Category.class),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    assertThat(setCustomFieldsUpdateActions).isNotNull();
    assertThat(setCustomFieldsUpdateActions).isNotEmpty();
    assertThat(setCustomFieldsUpdateActions).hasSize(2);
    final UpdateAction<Category> categoryUpdateAction = setCustomFieldsUpdateActions.get(0);
    assertThat(categoryUpdateAction).isNotNull();
    assertThat(categoryUpdateAction).isInstanceOf(SetCustomField.class);
  }

  @Test
  void
      buildSetCustomFieldsUpdateActions_WithNoNewCustomFieldsInOldCustomFields_ShouldBuildUpdateActions() {
    final Map<String, JsonNode> oldCustomFields = new HashMap<>();

    final Map<String, JsonNode> newCustomFields = new HashMap<>();
    newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
    newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));
    newCustomFields.put("url", JsonNodeFactory.instance.objectNode().put("domain", "domain.com"));
    newCustomFields.put("size", JsonNodeFactory.instance.objectNode().put("cm", 34));

    final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            mock(Category.class),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    assertThat(setCustomFieldsUpdateActions).isNotNull();
    assertThat(setCustomFieldsUpdateActions).isNotEmpty();
    assertThat(setCustomFieldsUpdateActions).hasSize(4);
  }

  @Test
  void
      buildSetCustomFieldsUpdateActions_WithOldCustomFieldNotInNewFields_ShouldBuildUpdateActions() {
    final Map<String, JsonNode> oldCustomFields = new HashMap<>();
    oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
    oldCustomFields.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

    final Map<String, JsonNode> newCustomFields = new HashMap<>();
    newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

    final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            mock(Category.class),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    assertThat(setCustomFieldsUpdateActions).isNotNull();
    assertThat(setCustomFieldsUpdateActions).isNotEmpty();
    assertThat(setCustomFieldsUpdateActions).hasSize(1);
    assertThat(setCustomFieldsUpdateActions.get(0)).isInstanceOf(SetCustomField.class);
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithSameCustomFieldValues_ShouldNotBuildUpdateActions() {
    final BooleanNode oldBooleanFieldValue = JsonNodeFactory.instance.booleanNode(true);
    final ObjectNode oldLocalizedFieldValue =
        JsonNodeFactory.instance.objectNode().put("de", "rot");

    final Map<String, JsonNode> oldCustomFields = new HashMap<>();
    oldCustomFields.put("invisibleInShop", oldBooleanFieldValue);
    oldCustomFields.put("backgroundColor", oldLocalizedFieldValue);

    final Map<String, JsonNode> newCustomFields = new HashMap<>();
    newCustomFields.put("invisibleInShop", oldBooleanFieldValue);
    newCustomFields.put("backgroundColor", oldLocalizedFieldValue);

    final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            mock(Category.class),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    assertThat(setCustomFieldsUpdateActions).isNotNull();
    assertThat(setCustomFieldsUpdateActions).isEmpty();
  }

  @Test
  void
      buildSetCustomFieldsUpdateActions_WithDifferentOrderOfCustomFieldValues_ShouldNotBuildUpdateActions() {
    final Map<String, JsonNode> oldCustomFields = new HashMap<>();
    oldCustomFields.put(
        "backgroundColor",
        JsonNodeFactory.instance.objectNode().put("de", "rot").put("es", "rojo"));

    final Map<String, JsonNode> newCustomFields = new HashMap<>();
    newCustomFields.put(
        "backgroundColor",
        JsonNodeFactory.instance.objectNode().put("es", "rojo").put("de", "rot"));

    final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            mock(Category.class),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    assertThat(setCustomFieldsUpdateActions).isNotNull();
    assertThat(setCustomFieldsUpdateActions).isEmpty();
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithEmptyCustomFieldValues_ShouldNotBuildUpdateActions() {
    final Map<String, JsonNode> oldCustomFields = new HashMap<>();
    oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode());

    final Map<String, JsonNode> newCustomFields = new HashMap<>();
    newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode());

    final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            mock(Category.class),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    assertThat(setCustomFieldsUpdateActions).isNotNull();
    assertThat(setCustomFieldsUpdateActions).isEmpty();
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithEmptyCustomFields_ShouldNotBuildUpdateActions() {
    final Map<String, JsonNode> oldCustomFields = new HashMap<>();
    final Map<String, JsonNode> newCustomFields = new HashMap<>();

    final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            mock(Category.class),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    assertThat(setCustomFieldsUpdateActions).isNotNull();
    assertThat(setCustomFieldsUpdateActions).isEmpty();
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithNullNewValue_ShouldBuildSetAction() {
    // preparation
    final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put(
        "setOfBooleans",
        JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.booleanNode(false)));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", null);

    // test
    final List<UpdateAction<Category>> updateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFieldsMap,
            newCustomFieldsMap,
            mock(Asset.class),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    // assertion
    assertThat(updateActions).containsExactly(SetCustomField.ofJson("setOfBooleans", null));
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithNullJsonNodeNewValue_ShouldBuildAction() {
    // preparation
    final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put(
        "setOfBooleans",
        JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.booleanNode(false)));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", JsonNodeFactory.instance.nullNode());

    // test
    final List<UpdateAction<Category>> updateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFieldsMap,
            newCustomFieldsMap,
            mock(Asset.class),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    // assertion
    assertThat(updateActions).containsExactly(SetCustomField.ofJson("setOfBooleans", null));
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithNullNewValueOfNewField_ShouldNotBuildAction() {
    // preparation
    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getFieldsJsonMap()).thenReturn(new HashMap<>());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", null);

    // test
    final List<UpdateAction<Category>> updateActions =
        buildSetCustomFieldsUpdateActions(
            new HashMap<>(),
            newCustomFieldsMap,
            mock(Asset.class),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    // assertion
    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithNullJsonNodeNewValueOfNewField_ShouldNotBuildAction() {
    // preparation
    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getFieldsJsonMap()).thenReturn(new HashMap<>());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", JsonNodeFactory.instance.nullNode());

    // test
    final List<UpdateAction<Category>> updateActions =
        buildSetCustomFieldsUpdateActions(
            new HashMap<>(),
            newCustomFieldsMap,
            mock(Asset.class),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    // assertion
    assertThat(updateActions).isEmpty();
  }
}
