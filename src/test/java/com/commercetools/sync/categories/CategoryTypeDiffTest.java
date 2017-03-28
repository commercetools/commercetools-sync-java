package com.commercetools.sync.categories;


import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.commercetools.sync.categories.CategoryTypeDiff.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryTypeDiffTest {
    @Test
    public void buildTypeActions_WithNonNullCustomFieldsWithDifferentKeys_ShouldBuildUpdateActions() {
        final String existingCategoryCustomTypeKey = "1";
        final Category existingCategory = mock(Category.class);
        final CustomFields existingCategoryCustomFields = mock(CustomFields.class);
        final Reference<Type> existingCategoryCustomFieldsDraftTypeReference = mock(Reference.class);
        when(existingCategoryCustomFields.getType()).thenReturn(existingCategoryCustomFieldsDraftTypeReference);
        when(existingCategory.getCustom()).thenReturn(existingCategoryCustomFields);

        final String newCategoryCustomTypeKey = "2";
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        final CustomFieldsDraft newCategoryCustomFieldsDraft = mock(CustomFieldsDraft.class);
        final Reference<Type> newCategoryCustomFieldsDraftTypeReference = mock(Reference.class);
        when(newCategoryCustomFieldsDraftTypeReference.getKey()).thenReturn(newCategoryCustomTypeKey);
        when(newCategoryCustomFieldsDraft.getType()).thenReturn(newCategoryCustomFieldsDraftTypeReference);
        when(newCategoryDraft.getCustom()).thenReturn(newCategoryCustomFieldsDraft);

        final TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(existingCategoryCustomTypeKey);

        final List<UpdateAction<Category>> updateActions =
                buildTypeActions(existingCategory, newCategoryDraft, typeServiceMock);

        // Should set custom type of existing category.
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildTypeActions_WithNullExistingCustomFields_ShouldBuildUpdateActions() {
        final Category existingCategory = mock(Category.class);
        when(existingCategory.getCustom()).thenReturn(null);

        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        final CustomFieldsDraft newCategoryCustomFieldsDraft = mock(CustomFieldsDraft.class);
        final Reference<Type> newCategoryCustomFieldsDraftTypeReference = mock(Reference.class);
        when(newCategoryCustomFieldsDraft.getType()).thenReturn(newCategoryCustomFieldsDraftTypeReference);
        when(newCategoryDraft.getCustom()).thenReturn(newCategoryCustomFieldsDraft);

        final TypeService typeServiceMock = mock(TypeServiceImpl.class);

        final List<UpdateAction<Category>> updateActions =
                buildTypeActions(existingCategory, newCategoryDraft, typeServiceMock);

        // Should add custom type to existing category.
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildTypeActions_WithNullNewCustomFields_ShouldBuildUpdateActions() {
        final Category existingCategory = mock(Category.class);
        final CustomFields existingCategoryCustomFields = mock(CustomFields.class);
        when(existingCategory.getCustom()).thenReturn(existingCategoryCustomFields);

        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getCustom()).thenReturn(null);

        final TypeService typeServiceMock = mock(TypeServiceImpl.class);

        final List<UpdateAction<Category>> updateActions =
                buildTypeActions(existingCategory, newCategoryDraft, typeServiceMock);

        // Should remove custom type from existing category.
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildTypeActions_WithBothNullCustomFields_ShouldNotBuildUpdateActions() {
        final Category existingCategory = mock(Category.class);
        when(existingCategory.getCustom()).thenReturn(null);

        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getCustom()).thenReturn(null);

        final TypeService typeServiceMock = mock(TypeServiceImpl.class);

        final List<UpdateAction<Category>> updateActions =
                buildTypeActions(existingCategory, newCategoryDraft, typeServiceMock);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildNonNullCustomFieldsActions_WithSameCategoryTypeKeys_ShouldBuildUpdateActions() {
        final Reference<Type> categoryTypeReference = mock(Reference.class);
        final String categoryCustomTypeInternalId = "categoryCustomTypeId";
        final String categoryCustomTypeKey = "categoryCustomTypeKey";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);
        when(categoryTypeReference.getKey()).thenReturn(categoryCustomTypeKey);

        // Mock existing CustomFields
        final CustomFields existingCustomFieldsMock = mock(CustomFields.class);
        final Map<String, JsonNode> existingCustomFieldsJsonMapMock = new HashMap<>();
        existingCustomFieldsJsonMapMock.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        when(existingCustomFieldsMock.getType()).thenReturn(categoryTypeReference);
        when(existingCustomFieldsMock.getFieldsJsonMap()).thenReturn(existingCustomFieldsJsonMapMock);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        final Map<String, JsonNode> newCustomFieldsJsonMapMock = new HashMap<>();
        newCustomFieldsJsonMapMock.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);
        when(newCustomFieldsMock.getFields()).thenReturn(newCustomFieldsJsonMapMock);

        // Mock TypeService and Category Custom Type key Cache.
        final TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);

        final List<UpdateAction<Category>> updateActions =
                buildNonNullCustomFieldsActions(existingCustomFieldsMock,
                        newCustomFieldsMock, typeServiceMock);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomField");
    }

    @Test
    public void buildNonNullCustomFieldsActions_WithDifferentCategoryTypeKeys_ShouldBuildUpdateActions() {
        final Reference<Type> categoryTypeReference = mock(Reference.class);
        final String categoryCustomTypeInternalId = "categoryCustomTypeId";
        final String categoryCustomTypeKey = "categoryCustomTypeKey";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);
        when(categoryTypeReference.getKey()).thenReturn(categoryCustomTypeKey);

        final Reference<Type> newCategoryTypeReference = mock(Reference.class);
        final String newCategoryCustomTypeId = "newCategoryCustomTypeId";
        final String newCategoryCustomTypeKey = "newCategoryCustomTypeKey";
        when(newCategoryTypeReference.getId()).thenReturn(newCategoryCustomTypeId);
        when(newCategoryTypeReference.getKey()).thenReturn(newCategoryCustomTypeKey);

        // Mock existing CustomFields
        final CustomFields existingCustomFieldsMock = mock(CustomFields.class);
        when(existingCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(newCategoryTypeReference);

        // Mock TypeService and Category Custom Type key Cache.
        final TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);

        final List<UpdateAction<Category>> updateActions = buildNonNullCustomFieldsActions(existingCustomFieldsMock,
                newCustomFieldsMock, typeServiceMock);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildNonNullCustomFieldsActions_WithNullExistingCategoryTypeKey_ShouldBuildUpdateActions() {
        final Reference<Type> categoryTypeReference = mock(Reference.class);
        final String categoryCustomTypeInternalId = "categoryCustomTypeId";
        final String categoryCustomTypeKey = "categoryCustomTypeKey";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);
        when(categoryTypeReference.getKey()).thenReturn(categoryCustomTypeKey);

        // Mock existing CustomFields
        final CustomFields existingCustomFieldsMock = mock(CustomFields.class);
        when(existingCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock TypeService and Category Custom Type key Cache.
        final TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(null);

        final List<UpdateAction<Category>> updateActions = buildNonNullCustomFieldsActions(existingCustomFieldsMock,
                newCustomFieldsMock, typeServiceMock);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildNonNullCustomFieldsActions_WithNullNewCategoryTypeKey_ShouldBuildUpdateActions() {
        final Reference<Type> categoryTypeReference = mock(Reference.class);
        final String categoryCustomTypeInternalId = "categoryCustomTypeId";
        final String categoryCustomTypeKey = "categoryCustomTypeKey";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);

        // Mock existing CustomFields
        final CustomFields existingCustomFieldsMock = mock(CustomFields.class);
        when(existingCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock TypeService and Category Custom Type key Cache.
        final TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);

        final List<UpdateAction<Category>> updateActions = buildNonNullCustomFieldsActions(existingCustomFieldsMock,
                newCustomFieldsMock, typeServiceMock);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildNonNullCustomFieldsActions_WithNullKeys_ShouldNotBuildUpdateActions() {
        final Reference<Type> categoryTypeReference = mock(Reference.class);
        final String categoryCustomTypeInternalId = "categoryCustomTypeId";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);

        // Mock existing CustomFields
        final CustomFields existingCustomFieldsMock = mock(CustomFields.class);
        when(existingCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock TypeService and Category Custom Type key Cache.
        final TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(null);

        final List<UpdateAction<Category>> updateActions =
                buildNonNullCustomFieldsActions(existingCustomFieldsMock,
                        newCustomFieldsMock, typeServiceMock);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildNonNullCustomFieldsActions_WithSameKeysButNullNewCustomFields_ShouldNotBuildUpdateActions() {
        final Reference<Type> categoryTypeReference = mock(Reference.class);
        final String categoryCustomTypeInternalId = "categoryCustomTypeId";
        final String categoryCustomTypeKey = "categoryCustomTypeKey";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);
        when(categoryTypeReference.getKey()).thenReturn(categoryCustomTypeKey);

        // Mock existing CustomFields
        final CustomFields existingCustomFieldsMock = mock(CustomFields.class);
        final Map<String, JsonNode> existingCustomFieldsJsonMapMock = new HashMap<>();
        existingCustomFieldsJsonMapMock.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        when(existingCustomFieldsMock.getType()).thenReturn(categoryTypeReference);
        when(existingCustomFieldsMock.getFieldsJsonMap()).thenReturn(existingCustomFieldsJsonMapMock);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);
        when(newCustomFieldsMock.getFields()).thenReturn(null);

        // Mock TypeService and Category Custom Type key Cache.
        final TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);

        final List<UpdateAction<Category>> updateActions =
                buildNonNullCustomFieldsActions(existingCustomFieldsMock,
                        newCustomFieldsMock, typeServiceMock);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsActions_WithDifferentCustomFieldValues_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isNotEmpty();
        assertThat(setCustomFieldsUpdateActions).hasSize(2);
        final UpdateAction<Category> categoryUpdateAction = setCustomFieldsUpdateActions.get(0);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("setCustomField");
    }

    @Test
    public void buildSetCustomFieldsActions_WithNoNewCustomFieldsInExistingCustomFields_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> existingCustomFields = new HashMap<>();

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));
        newCustomFields.put("url", JsonNodeFactory.instance.objectNode().put("domain", "domain.com"));
        newCustomFields.put("size", JsonNodeFactory.instance.objectNode().put("cm", 34));

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isNotEmpty();
        assertThat(setCustomFieldsUpdateActions).hasSize(4);
    }

    @Test
    public void buildSetCustomFieldsActions_WithExistingCustomFieldNotInNewFields_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isNotEmpty();
        assertThat(setCustomFieldsUpdateActions).hasSize(1);
    }

    @Test
    public void buildSetCustomFieldsActions_WithSameCustomFieldValues_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsActions_WithDifferentOrderOfCustomFieldValues_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("es", "rojo"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo").put("de", "rot"));

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsActions_WithEmptyCustomFieldValues_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode());

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode());

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsActions_WithEmptyCustomFields_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> existingCustomFields = new HashMap<>();

        final Map<String, JsonNode> newCustomFields = new HashMap<>();

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();
    }

    @Test
    public void buildNewOrModifiedCustomFieldsActions_WithNewOrModifiedCustomFields_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final List<UpdateAction<Category>> customFieldsActions =
                buildNewOrModifiedCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isNotEmpty();
        assertThat(customFieldsActions).hasSize(1);
    }

    @Test
    public void buildNewOrModifiedCustomFieldsActions_WithNoNewOrModifiedCustomFields_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final List<UpdateAction<Category>> customFieldsActions =
                buildNewOrModifiedCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isEmpty();
    }

    @Test
    public void buildRemovedCustomFieldsActions_WithRemovedCustomField_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final List<UpdateAction<Category>> customFieldsActions =
                buildRemovedCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isNotEmpty();
        assertThat(customFieldsActions).hasSize(1);
    }

    @Test
    public void buildRemovedCustomFieldsActions_WithNoRemovedCustomField_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final List<UpdateAction<Category>> customFieldsActions =
                buildRemovedCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isEmpty();
    }
}
