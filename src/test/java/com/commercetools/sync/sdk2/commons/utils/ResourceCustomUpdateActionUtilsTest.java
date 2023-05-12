package com.commercetools.sync.sdk2.commons.utils;

import static com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils.*;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.category.CategoryReferenceBuilder;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.category.CategorySetCustomFieldAction;
import com.commercetools.api.models.category.CategorySetCustomFieldActionBuilder;
import com.commercetools.api.models.category.CategorySetCustomTypeAction;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifier;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.categories.CategorySyncMockUtils;
import com.commercetools.sync.sdk2.categories.CategorySyncOptions;
import com.commercetools.sync.sdk2.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.sdk2.categories.helpers.CategoryCustomActionBuilder;
import com.commercetools.sync.sdk2.categories.models.CategoryCustomTypeAdapter;
import com.commercetools.sync.sdk2.categories.models.CategoryDraftCustomTypeAdapter;
import com.commercetools.sync.sdk2.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.models.AssetCustomTypeAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourceCustomUpdateActionUtilsTest {
  private static final ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);
  private ArrayList<String> errorMessages;
  private ArrayList<Throwable> exceptions;
  private CategorySyncOptions categorySyncOptions;

  @BeforeEach
  void setupTest() {
    errorMessages = new ArrayList<>();
    exceptions = new ArrayList<>();
    final QuadConsumer<
            SyncException, Optional<CategoryDraft>, Optional<Category>, List<CategoryUpdateAction>>
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
    final TypeReference oldCategoryCustomFieldsDraftTypeReference =
        TypeReferenceBuilder.of().id("2").build();
    final CustomFields oldCategoryCustomFields =
        CustomFieldsBuilder.of()
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(Collections.emptyMap()))
            .type(oldCategoryCustomFieldsDraftTypeReference)
            .build();
    final Category oldCategory = mock(Category.class);
    when(oldCategory.getCustom()).thenReturn(oldCategoryCustomFields);

    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    final CustomFieldsDraft newCategoryCustomFieldsDraft = mock(CustomFieldsDraft.class);

    final TypeResourceIdentifier typeResourceIdentifier =
        TypeResourceIdentifierBuilder.of().id("1").build();
    when(newCategoryCustomFieldsDraft.getType()).thenReturn(typeResourceIdentifier);
    when(newCategoryDraft.getCustom()).thenReturn(newCategoryCustomFieldsDraft);

    final List<CategoryUpdateAction> updateActions =
        buildPrimaryResourceCustomUpdateActions(
            CategoryCustomTypeAdapter.of(oldCategory),
            CategoryDraftCustomTypeAdapter.of(newCategoryDraft),
            new CategoryCustomActionBuilder(),
            categorySyncOptions);

    // Should set custom type of old category.
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(CategorySetCustomTypeAction.class);
  }

  @Test
  void buildResourceCustomUpdateActions_WithNullOldCustomFields_ShouldBuildUpdateActions() {
    final Category oldCategory = mock(Category.class);
    when(oldCategory.getCustom()).thenReturn(null);

    final CategoryDraft newCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.ofEnglish("name"))
            .slug(LocalizedString.ofEnglish("testSlug"))
            .key("key")
            .parent(CategoryResourceIdentifierBuilder.of().id("parentId").build())
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(
                        typeResourceIdentifierBuilder ->
                            typeResourceIdentifierBuilder.id("customTypeId"))
                    .fields(fieldContainerBuilder -> fieldContainerBuilder.values(new HashMap<>()))
                    .build())
            .build();

    final List<CategoryUpdateAction> updateActions =
        buildPrimaryResourceCustomUpdateActions(
            CategoryCustomTypeAdapter.of(oldCategory),
            CategoryDraftCustomTypeAdapter.of(newCategoryDraft),
            new CategoryCustomActionBuilder(),
            categorySyncOptions);

    // Should add custom type to old category.
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(CategorySetCustomTypeAction.class);
  }

  @Test
  void
      buildResourceCustomUpdateActions_WithNullOldCustomFieldsAndBlankNewTypeId_ShouldCallErrorCallBack() {
    final Category oldCategory = mock(Category.class);
    when(oldCategory.getCustom()).thenReturn(null);
    final String oldCategoryId = "oldCategoryId";
    when(oldCategory.getId()).thenReturn(oldCategoryId);
    when(oldCategory.toReference())
        .thenReturn(CategoryReferenceBuilder.of().id(oldCategoryId).build());

    final CategoryDraft newCategoryDraft =
        CategorySyncMockUtils.getMockCategoryDraft(Locale.ENGLISH, "name", "slug", "key");
    final CustomFieldsDraft mockCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key("key"))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(new HashMap<>()))
            .build();
    when(newCategoryDraft.getCustom()).thenReturn(mockCustomFieldsDraft);

    final List<CategoryUpdateAction> updateActions =
        buildPrimaryResourceCustomUpdateActions(
            CategoryCustomTypeAdapter.of(oldCategory),
            CategoryDraftCustomTypeAdapter.of(newCategoryDraft),
            new CategoryCustomActionBuilder(),
            categorySyncOptions);

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

    final List<CategoryUpdateAction> updateActions =
        buildPrimaryResourceCustomUpdateActions(
            CategoryCustomTypeAdapter.of(oldCategory),
            CategoryDraftCustomTypeAdapter.of(newCategoryDraft),
            new CategoryCustomActionBuilder(),
            categorySyncOptions);

    // Should remove custom type from old category.
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(CategorySetCustomTypeAction.class);
  }

  @Test
  void buildResourceCustomUpdateActions_WithEmptyIds_ShouldCallSyncOptionsCallBack() {
    final TypeReference categoryTypeReference = TypeReferenceBuilder.of().id("").build();

    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock =
        CustomFieldsBuilder.of()
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(Collections.emptyMap()))
            .type(categoryTypeReference)
            .build();

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference.toResourceIdentifier());

    // Mock old Category
    final Category oldCategory = mock(Category.class);
    when(oldCategory.getId()).thenReturn("oldCategoryId");
    when(oldCategory.toReference())
        .thenReturn(CategoryReferenceBuilder.of().id("oldCategoryId").build());
    when(oldCategory.getCustom()).thenReturn(oldCustomFieldsMock);

    // Mock new Category
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getCustom()).thenReturn(newCustomFieldsMock);

    final List<CategoryUpdateAction> updateActions =
        buildPrimaryResourceCustomUpdateActions(
            CategoryCustomTypeAdapter.of(oldCategory),
            CategoryDraftCustomTypeAdapter.of(newCategoryDraft),
            new CategoryCustomActionBuilder(),
            categorySyncOptions);

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

    final List<CategoryUpdateAction> updateActions =
        buildPrimaryResourceCustomUpdateActions(
            CategoryCustomTypeAdapter.of(oldCategory),
            CategoryDraftCustomTypeAdapter.of(newCategoryDraft),
            new CategoryCustomActionBuilder(),
            categorySyncOptions);

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
    final Map<String, Object> oldCustomFieldsJsonMapMock = new HashMap<>();
    oldCustomFieldsJsonMapMock.put("invisibleInShop", true);
    when(oldCustomFieldsMock.getType())
        .thenReturn(TypeReferenceBuilder.of().id("categoryCustomTypeId").build());
    when(oldCustomFieldsMock.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsJsonMapMock).build());

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    final Map<String, Object> newCustomFieldsJsonMapMock = new HashMap<>();
    newCustomFieldsJsonMapMock.put("invisibleInShop", false);
    when(newCustomFieldsMock.getType())
        .thenReturn(TypeResourceIdentifierBuilder.of().id("categoryCustomTypeId").build());
    when(newCustomFieldsMock.getFields())
        .thenReturn(FieldContainerBuilder.of().values(newCustomFieldsJsonMapMock).build());

    final List<CategoryUpdateAction> updateActions =
        buildNonNullCustomFieldsUpdateActions(
            oldCustomFieldsMock,
            newCustomFieldsMock,
            CategoryCustomTypeAdapter.of(mock(Category.class)),
            new CategoryCustomActionBuilder(),
            null,
            CategoryCustomTypeAdapter::getId,
            CategoryCustomTypeAdapter::getTypeId,
            category -> null,
            categorySyncOptions);

    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(CategorySetCustomFieldAction.class);
  }

  @Test
  void buildNonNullCustomFieldsUpdateActions_WithDifferentCategoryTypeIds_ShouldBuildUpdateActions()
      throws BuildUpdateActionException {
    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock =
        CustomFieldsBuilder.of()
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(Collections.emptyMap()))
            .type(TypeReferenceBuilder.of().id("categoryCustomTypeId").build())
            .build();

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    when(newCustomFieldsMock.getType())
        .thenReturn(TypeResourceIdentifierBuilder.of().id("newCategoryCustomTypeId").build());

    final List<CategoryUpdateAction> updateActions =
        buildNonNullCustomFieldsUpdateActions(
            oldCustomFieldsMock,
            newCustomFieldsMock,
            CategoryCustomTypeAdapter.of(mock(Category.class)),
            new CategoryCustomActionBuilder(),
            null,
            CategoryCustomTypeAdapter::getId,
            CategoryCustomTypeAdapter::getTypeId,
            category -> null,
            categorySyncOptions);

    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(CategorySetCustomTypeAction.class);
  }

  @Test
  void buildNonNullCustomFieldsUpdateActions_WithEmptyOldCategoryTypeId_ShouldBuildUpdateActions()
      throws BuildUpdateActionException {
    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock =
        CustomFieldsBuilder.of()
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(Collections.emptyMap()))
            .type(TypeReferenceBuilder.of().id("").build())
            .build();

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    when(newCustomFieldsMock.getType())
        .thenReturn(TypeResourceIdentifierBuilder.of().id("categoryCustomTypeId").build());

    final List<CategoryUpdateAction> updateActions =
        buildNonNullCustomFieldsUpdateActions(
            oldCustomFieldsMock,
            newCustomFieldsMock,
            CategoryCustomTypeAdapter.of(mock(Category.class)),
            new CategoryCustomActionBuilder(),
            null,
            CategoryCustomTypeAdapter::getId,
            CategoryCustomTypeAdapter::getTypeId,
            category -> null,
            categorySyncOptions);

    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(CategorySetCustomTypeAction.class);
  }

  @Test
  void
      buildNonNullCustomFieldsUpdateActions_WithNullNewCategoryTypeId_TriggersErrorCallbackAndNoAction()
          throws BuildUpdateActionException {
    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock =
        CustomFieldsBuilder.of()
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(Collections.emptyMap()))
            .type(TypeReferenceBuilder.of().id("1").build())
            .build();

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    final TypeResourceIdentifier value = TypeResourceIdentifierBuilder.of().id(null).build();

    when(newCustomFieldsMock.getType()).thenReturn(value);

    final String categoryId = UUID.randomUUID().toString();
    final Category category = mock(Category.class);
    when(category.getId()).thenReturn(categoryId);
    when(category.toReference()).thenReturn(CategoryReferenceBuilder.of().id(categoryId).build());

    final List<CategoryUpdateAction> updateActions =
        buildNonNullCustomFieldsUpdateActions(
            oldCustomFieldsMock,
            newCustomFieldsMock,
            CategoryCustomTypeAdapter.of(category),
            new CategoryCustomActionBuilder(),
            null,
            CategoryCustomTypeAdapter::getId,
            CategoryCustomTypeAdapter::getTypeId,
            cat -> null,
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
    final TypeReference categoryTypeReference =
        TypeReferenceBuilder.of().id("categoryCustomTypeId").build();

    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
    final Map<String, Object> oldCustomFieldsJsonMapMock = new HashMap<>();
    oldCustomFieldsJsonMapMock.put("invisibleInShop", true);

    when(oldCustomFieldsMock.getType()).thenReturn(categoryTypeReference);
    when(oldCustomFieldsMock.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsJsonMapMock).build());

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference.toResourceIdentifier());
    when(newCustomFieldsMock.getFields()).thenReturn(null);

    final List<CategoryUpdateAction> updateActions =
        buildNonNullCustomFieldsUpdateActions(
            oldCustomFieldsMock,
            newCustomFieldsMock,
            CategoryCustomTypeAdapter.of(mock(Category.class)),
            new CategoryCustomActionBuilder(),
            null,
            CategoryCustomTypeAdapter::getId,
            CategoryCustomTypeAdapter::getTypeId,
            category -> null,
            categorySyncOptions);

    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(CategorySetCustomFieldAction.class);
  }

  @Test
  void buildNonNullCustomFieldsUpdateActions_WithEmptyIds_ShouldThrowBuildUpdateActionException() {
    final TypeReference categoryTypeReference = TypeReferenceBuilder.of().id("").build();
    final CustomFields oldCustomFieldsMock =
        CustomFieldsBuilder.of()
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(Collections.emptyMap()))
            .type(categoryTypeReference)
            .build();

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference.toResourceIdentifier());

    final Category oldCategory = mock(Category.class);
    when(oldCategory.getId()).thenReturn("oldCategoryId");
    when(oldCategory.toReference()).thenReturn(CategoryReferenceBuilder.of().id("").build());

    assertThatThrownBy(
            () ->
                buildNonNullCustomFieldsUpdateActions(
                    oldCustomFieldsMock,
                    newCustomFieldsMock,
                    CategoryCustomTypeAdapter.of(mock(Category.class)),
                    new CategoryCustomActionBuilder(),
                    null,
                    CategoryCustomTypeAdapter::getId,
                    CategoryCustomTypeAdapter::getTypeId,
                    category -> null,
                    categorySyncOptions))
        .isInstanceOf(BuildUpdateActionException.class)
        .hasMessageMatching("Custom type ids are not set for both the old and new category.");
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithDifferentCustomFieldValues_ShouldBuildUpdateActions() {
    final Map<String, Object> oldCustomFields = new HashMap<>();
    oldCustomFields.put("invisibleInShop", false);
    oldCustomFields.put("backgroundColor", Map.of("de", "rot", "en", "red"));

    final Map<String, Object> newCustomFields = new HashMap<>();
    newCustomFields.put("invisibleInShop", true);
    newCustomFields.put("backgroundColor", Map.of("de", "rot"));

    final List<CategoryUpdateAction> setCustomFieldsUpdateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            CategoryCustomTypeAdapter.of(mock(Category.class)),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    assertThat(setCustomFieldsUpdateActions).isNotNull();
    assertThat(setCustomFieldsUpdateActions).isNotEmpty();
    assertThat(setCustomFieldsUpdateActions).hasSize(2);
    final CategoryUpdateAction categoryUpdateAction = setCustomFieldsUpdateActions.get(0);
    assertThat(categoryUpdateAction).isNotNull();
    assertThat(categoryUpdateAction).isInstanceOf(CategorySetCustomFieldAction.class);
  }

  @Test
  void
      buildSetCustomFieldsUpdateActions_WithNoNewCustomFieldsInOldCustomFields_ShouldBuildUpdateActions() {
    final Map<String, Object> oldCustomFields = new HashMap<>();

    final Map<String, Object> newCustomFields = new HashMap<>();
    newCustomFields.put("invisibleInShop", true);
    newCustomFields.put("backgroundColor", Map.of("de", "rot"));
    newCustomFields.put("url", Map.of("domain", "domain.com"));
    newCustomFields.put("size", Map.of("cm", 34));

    final List<CategoryUpdateAction> setCustomFieldsUpdateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            CategoryCustomTypeAdapter.of(mock(Category.class)),
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
    final Map<String, Object> oldCustomFields = new HashMap<>();
    oldCustomFields.put("invisibleInShop", true);
    oldCustomFields.put("backgroundColor", Map.of("de", "rot", "en", "red"));

    final Map<String, Object> newCustomFields = new HashMap<>();
    newCustomFields.put("invisibleInShop", true);

    final List<CategoryUpdateAction> setCustomFieldsUpdateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            CategoryCustomTypeAdapter.of(mock(Category.class)),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    assertThat(setCustomFieldsUpdateActions).isNotNull();
    assertThat(setCustomFieldsUpdateActions).isNotEmpty();
    assertThat(setCustomFieldsUpdateActions).hasSize(1);
    assertThat(setCustomFieldsUpdateActions.get(0))
        .isInstanceOf(CategorySetCustomFieldAction.class);
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithSameCustomFieldValues_ShouldNotBuildUpdateActions() {
    final Boolean oldBooleanFieldValue = true;
    final Map<String, Object> oldLocalizedFieldValue = Map.of("de", "rot");

    final Map<String, Object> oldCustomFields = new HashMap<>();
    oldCustomFields.put("invisibleInShop", oldBooleanFieldValue);
    oldCustomFields.put("backgroundColor", oldLocalizedFieldValue);

    final Map<String, Object> newCustomFields = new HashMap<>();
    newCustomFields.put("invisibleInShop", oldBooleanFieldValue);
    newCustomFields.put("backgroundColor", oldLocalizedFieldValue);

    final List<CategoryUpdateAction> setCustomFieldsUpdateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            CategoryCustomTypeAdapter.of(mock(Category.class)),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    assertThat(setCustomFieldsUpdateActions).isNotNull();
    assertThat(setCustomFieldsUpdateActions).isEmpty();
  }

  @Test
  void
      buildSetCustomFieldsUpdateActions_WithDifferentOrderOfCustomFieldValues_ShouldNotBuildUpdateActions() {
    final Map<String, Object> oldCustomFields = new HashMap<>();
    oldCustomFields.put("backgroundColor", Map.of("de", "rot", "es", "rojo"));

    final Map<String, Object> newCustomFields = new HashMap<>();
    newCustomFields.put("backgroundColor", Map.of("es", "rojo", "de", "rot"));

    final List<CategoryUpdateAction> setCustomFieldsUpdateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            CategoryCustomTypeAdapter.of(mock(Category.class)),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    assertThat(setCustomFieldsUpdateActions).isNotNull();
    assertThat(setCustomFieldsUpdateActions).isEmpty();
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithEmptyCustomFieldValues_ShouldNotBuildUpdateActions() {
    final Map<String, Object> oldCustomFields = new HashMap<>();
    oldCustomFields.put("backgroundColor", null);

    final Map<String, Object> newCustomFields = new HashMap<>();
    newCustomFields.put("backgroundColor", null);

    final List<CategoryUpdateAction> setCustomFieldsUpdateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            CategoryCustomTypeAdapter.of(mock(Category.class)),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    assertThat(setCustomFieldsUpdateActions).isNotNull();
    assertThat(setCustomFieldsUpdateActions).isEmpty();
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithEmptyCustomFields_ShouldNotBuildUpdateActions() {
    final Map<String, Object> oldCustomFields = new HashMap<>();
    final Map<String, Object> newCustomFields = new HashMap<>();

    final List<CategoryUpdateAction> setCustomFieldsUpdateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            CategoryCustomTypeAdapter.of(mock(Category.class)),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    assertThat(setCustomFieldsUpdateActions).isNotNull();
    assertThat(setCustomFieldsUpdateActions).isEmpty();
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithNullNewValue_ShouldBuildSetAction() {
    // preparation
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("setOfBooleans", List.of(false));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsMap).build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", null);

    // test
    final List<CategoryUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFieldsMap,
            newCustomFieldsMap,
            AssetCustomTypeAdapter.of(mock(Asset.class)),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    // assertion
    assertThat(updateActions)
        .containsExactly(
            CategorySetCustomFieldActionBuilder.of().name("setOfBooleans").value(null).build());
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithNullNewValueOfNewField_ShouldNotBuildAction() {
    // preparation
    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(new HashMap<>()).build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", null);

    // test
    final List<CategoryUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            new HashMap<>(),
            newCustomFieldsMap,
            AssetCustomTypeAdapter.of(mock(Asset.class)),
            new CategoryCustomActionBuilder(),
            null,
            category -> null);

    // assertion
    assertThat(updateActions).isEmpty();
  }
}
