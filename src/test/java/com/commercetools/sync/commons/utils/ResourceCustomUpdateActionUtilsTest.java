package com.commercetools.sync.commons.utils;

import com.commercetools.sync.categories.CategorySyncMockUtils;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.CategoryCustomActionBuilder;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.SetCustomField;
import io.sphere.sdk.categories.commands.updateactions.SetCustomType;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildNewOrModifiedCustomFieldsUpdateActions;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildNonNullCustomFieldsUpdateActions;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildRemovedCustomFieldsUpdateActions;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildResourceCustomUpdateActions;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildSetCustomFieldsUpdateActions;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceCustomUpdateActionUtilsTest {
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private ArrayList<String> errorMessages;
    private ArrayList<Throwable> exceptions;
    private CategorySyncOptions categorySyncOptions;

    @Before
    public void setupTest() {
        errorMessages = new ArrayList<>();
        exceptions = new ArrayList<>();
        final BiConsumer<String, Throwable> updateActionErrorCallBack = (errorMessage, exception) -> {
            errorMessages.add(errorMessage);
            exceptions.add(exception);
        };

        // Mock sync options
        categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                        .errorCallback(updateActionErrorCallBack)
                                                        .build();
    }

    @Test
    public void buildResourceCustomUpdateActions_WithNonNullCustomFieldsWithDifferentTypes_ShouldBuildUpdateActions() {
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
            buildResourceCustomUpdateActions(oldCategory, newCategoryDraft, new CategoryCustomActionBuilder(),
                categorySyncOptions);

        // Should set custom type of old category.
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildResourceCustomUpdateActions_WithNullOldCustomFields_ShouldBuildUpdateActions() {
        final Category oldCategory = mock(Category.class);
        when(oldCategory.getCustom()).thenReturn(null);

        final CategoryDraft newCategoryDraft = CategorySyncMockUtils.getMockCategoryDraft(Locale.ENGLISH, "name",
            "key", "parentId", "customTypeId", new HashMap<>());
        final List<UpdateAction<Category>> updateActions =
            buildResourceCustomUpdateActions(oldCategory, newCategoryDraft, new CategoryCustomActionBuilder(),
                categorySyncOptions);

        // Should add custom type to old category.
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildResourceCustomUpdateActions_WithNullOldCustomFieldsAndBlankNewTypeId_ShouldCallErrorCallBack() {
        final Category oldCategory = mock(Category.class);
        when(oldCategory.getCustom()).thenReturn(null);
        final String oldCategoryId = "oldCategoryId";
        when(oldCategory.getId()).thenReturn(oldCategoryId);
        when(oldCategory.toReference()).thenReturn(Category.referenceOfId(oldCategoryId));

        final CategoryDraft newCategoryDraft = CategorySyncMockUtils.getMockCategoryDraft(Locale.ENGLISH, "name",
            "slug", "key");
        final CustomFieldsDraft mockCustomFieldsDraft = CustomFieldsDraft.ofTypeKeyAndJson("key", new HashMap<>());
        when(newCategoryDraft.getCustom()).thenReturn(mockCustomFieldsDraft);


        final List<UpdateAction<Category>> updateActions =
            buildResourceCustomUpdateActions(oldCategory, newCategoryDraft, new CategoryCustomActionBuilder(),
                categorySyncOptions);

        // Should add custom type to old category.
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(0);
        assertThat(errorMessages.get(0)).isEqualTo(format("Failed to build custom fields update actions on the "
            + "category with id '%s'. Reason: New resource's custom type id is blank (empty/null).", oldCategoryId));
        assertThat(exceptions.get(0)).isNull();
    }

    @Test
    public void buildResourceCustomUpdateActions_WithNullNewCustomFields_ShouldBuildUpdateActions() {
        final Category oldCategory = mock(Category.class);
        final CustomFields oldCategoryCustomFields = mock(CustomFields.class);
        when(oldCategory.getCustom()).thenReturn(oldCategoryCustomFields);

        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getCustom()).thenReturn(null);

        final List<UpdateAction<Category>> updateActions =
            buildResourceCustomUpdateActions(oldCategory, newCategoryDraft, new CategoryCustomActionBuilder(),
                categorySyncOptions);

        // Should remove custom type from old category.
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildResourceCustomUpdateActions_WithNullIds_ShouldCallSyncOptionsCallBack() {
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
            buildResourceCustomUpdateActions(oldCategory, newCategoryDraft, new CategoryCustomActionBuilder(),
                categorySyncOptions);

        assertThat(errorMessages).hasSize(1);
        assertThat(exceptions).hasSize(1);
        assertThat(errorMessages.get(0)).isEqualTo("Failed to build custom fields update actions on the category"
            + " with id 'oldCategoryId'. Reason: Custom type ids are not set for both the old and new category.");
        assertThat(exceptions.get(0)).isInstanceOf(BuildUpdateActionException.class);
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithBothNullCustomFields_ShouldNotBuildUpdateActions() {
        final Category oldCategory = mock(Category.class);
        when(oldCategory.getCustom()).thenReturn(null);

        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getCustom()).thenReturn(null);

        final List<UpdateAction<Category>> updateActions =
            buildResourceCustomUpdateActions(oldCategory, newCategoryDraft, new CategoryCustomActionBuilder(),
                categorySyncOptions);

        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void
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
            buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
                newCustomFieldsMock, mock(Category.class), new CategoryCustomActionBuilder(), null, Category::getId,
                category -> category.toReference().getTypeId(), category -> null,
                categorySyncOptions);

        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetCustomField.class);
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithDifferentCategoryTypeIds_ShouldBuildUpdateActions()
        throws BuildUpdateActionException {
        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        when(oldCustomFieldsMock.getType()).thenReturn(Type.referenceOfId("categoryCustomTypeId"));

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(ResourceIdentifier.ofId("newCategoryCustomTypeId"));

        final List<UpdateAction<Category>> updateActions = buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
            newCustomFieldsMock, mock(Category.class), new CategoryCustomActionBuilder(), null, Category::getId,
            category -> category.toReference().getTypeId(), category -> null, categorySyncOptions);

        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithNullOldCategoryTypeId_ShouldBuildUpdateActions()
        throws BuildUpdateActionException {
        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        when(oldCustomFieldsMock.getType()).thenReturn(Type.referenceOfId(null));

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(Type.referenceOfId("categoryCustomTypeId"));

        final List<UpdateAction<Category>> updateActions = buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
            newCustomFieldsMock, mock(Category.class), new CategoryCustomActionBuilder(), null, Category::getId,
            category -> category.toReference().getTypeId(), category -> null, categorySyncOptions);

        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithNullNewCategoryTypeId_TriggersErrorCallbackAndNoAction()
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

        final List<UpdateAction<Category>> updateActions = buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
            newCustomFieldsMock, category, new CategoryCustomActionBuilder(), null, Category::getId,
            categoryRes -> category.toReference().getTypeId(), categoryRes -> null, categorySyncOptions);

        assertThat(errorMessages).hasSize(1);
        assertThat(errorMessages.get(0)).isEqualTo(format("Failed to build 'setCustomType' update action on the "
            + "%s with id '%s'. Reason: New Custom Type id is blank (null/empty).", category.toReference().getTypeId(),
            categoryId));
        assertThat(exceptions).hasSize(1);
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithSameIdsButNullNewCustomFields_ShouldBuildUpdateActions()
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
            buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
                newCustomFieldsMock, mock(Category.class), new CategoryCustomActionBuilder(), null, Category::getId,
                category -> category.toReference().getTypeId(), category -> null, categorySyncOptions);

        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithNullIds_ShouldThrowBuildUpdateActionException() {
        final Reference<Type> categoryTypeReference = Type.referenceOfId(null);

        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        when(oldCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        final Category oldCategory = mock(Category.class);
        when(oldCategory.getId()).thenReturn("oldCategoryId");
        when(oldCategory.toReference()).thenReturn(Category.referenceOfId( null));

        assertThatThrownBy(() ->
            buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
                newCustomFieldsMock, oldCategory, new CategoryCustomActionBuilder(), null, Category::getId,
                category -> category.toReference().getTypeId(), category -> null, categorySyncOptions))
            .isInstanceOf(BuildUpdateActionException.class)
            .hasMessageMatching("Custom type ids are not set for both the old and new category.");
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithDifferentCustomFieldValues_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
            buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class),
                new CategoryCustomActionBuilder(),null, category -> null);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isNotEmpty();
        assertThat(setCustomFieldsUpdateActions).hasSize(2);
        final UpdateAction<Category> categoryUpdateAction = setCustomFieldsUpdateActions.get(0);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction).isInstanceOf(SetCustomField.class);
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithNoNewCustomFieldsInOldCustomFields_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));
        newCustomFields.put("url", JsonNodeFactory.instance.objectNode().put("domain", "domain.com"));
        newCustomFields.put("size", JsonNodeFactory.instance.objectNode().put("cm", 34));

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
            buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class),
                new CategoryCustomActionBuilder(),null, category -> null);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isNotEmpty();
        assertThat(setCustomFieldsUpdateActions).hasSize(4);
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithOldCustomFieldNotInNewFields_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
            buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class),
                new CategoryCustomActionBuilder(),null, category -> null);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isNotEmpty();
        assertThat(setCustomFieldsUpdateActions).hasSize(1);
        assertThat(setCustomFieldsUpdateActions.get(0)).isInstanceOf(SetCustomField.class);
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithSameCustomFieldValues_ShouldNotBuildUpdateActions() {
        final BooleanNode oldBooleanFieldValue = JsonNodeFactory.instance.booleanNode(true);
        final ObjectNode oldLocalizedFieldValue = JsonNodeFactory.instance.objectNode().put("de", "rot");

        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", oldBooleanFieldValue);
        oldCustomFields.put("backgroundColor", oldLocalizedFieldValue);

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", oldBooleanFieldValue);
        newCustomFields.put("backgroundColor", oldLocalizedFieldValue);

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
            buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class),
                new CategoryCustomActionBuilder(),null, category -> null);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithDifferentOrderOfCustomFieldValues_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("backgroundColor",
            JsonNodeFactory.instance.objectNode().put("de", "rot").put("es", "rojo"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("backgroundColor",
            JsonNodeFactory.instance.objectNode().put("es", "rojo").put("de", "rot"));

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
            buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class),
                new CategoryCustomActionBuilder(),null, category -> null);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithEmptyCustomFieldValues_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode());

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode());

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
            buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class),
                new CategoryCustomActionBuilder(),null, category -> null);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithEmptyCustomFields_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        final Map<String, JsonNode> newCustomFields = new HashMap<>();

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
            buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class),
                new CategoryCustomActionBuilder(),null, category -> null);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();
    }

    @Test
    public void buildNewOrModifiedCustomFieldsUpdateActions_WithNewOrModifiedCustomFields_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final List<UpdateAction<Category>> customFieldsActions =
            buildNewOrModifiedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class),
                new CategoryCustomActionBuilder(),null, category -> null);

        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isNotEmpty();
        assertThat(customFieldsActions).hasSize(1);
        assertThat(customFieldsActions.get(0)).isInstanceOf(SetCustomField.class);
    }


    @Test
    public void buildNewOrModifiedCustomFieldsUpdateActions_WithNoNewOrModifiedCustomFields_ShouldNotBuildActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final List<UpdateAction<Category>> customFieldsActions =
            buildNewOrModifiedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class),
                new CategoryCustomActionBuilder(), null, categoryResource -> null);

        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isEmpty();
    }

    @Test
    public void buildRemovedCustomFieldsUpdateActions_WithRemovedCustomField_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final List<UpdateAction<Category>> customFieldsActions =
            buildRemovedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class),
                new CategoryCustomActionBuilder(), null, category -> null);

        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isNotEmpty();
        assertThat(customFieldsActions).hasSize(1);
        assertThat(customFieldsActions.get(0)).isInstanceOf(SetCustomField.class);
    }

    @Test
    public void buildRemovedCustomFieldsUpdateActions_WithNoRemovedCustomField_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final List<UpdateAction<Category>> customFieldsActions =
            buildRemovedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class),
                new CategoryCustomActionBuilder(), null, category -> null);

        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isEmpty();
    }
}
