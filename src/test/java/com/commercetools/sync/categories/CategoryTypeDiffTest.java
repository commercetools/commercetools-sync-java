package com.commercetools.sync.categories;


import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.Category;
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
import static com.commercetools.sync.categories.CategoryTypeDiff.buildRemovedCustomFieldsActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryTypeDiffTest {

    //TODO
    @Test
    public void buildTypeActions_WithNonNullCustomFields_ShouldBuildUpdateActions() {
    }

    //TODO
    @Test
    public void buildTypeActions_WithNullExistingCustomFields_ShouldBuildUpdateActions() {
    }

    //TODO
    @Test
    public void buildTypeActions_WithNullNewCustomFields_ShouldBuildUpdateActions() {
    }

    //TODO
    @Test
    public void buildTypeActions_WithBothNullCustomFields_ShouldNotBuildUpdateActions() {
    }

    @Test
    public void buildNonNullCustomFieldsActions_WithSameCategoryTypeKeys_ShouldBuildUpdateActions() {
        // Unit test custom fields comparison with the same Type, but different field values
        Reference<Type> categoryTypeReference = mock(Reference.class);
        String categoryCustomTypeInternalId = "categoryCustomTypeId";
        String categoryCustomTypeKey = "categoryCustomTypeKey";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);
        when(categoryTypeReference.getKey()).thenReturn(categoryCustomTypeKey);

        // Mock existing CustomFields
        CustomFields existingCustomFieldsMock = mock(CustomFields.class);
        Map<String, JsonNode> existingCustomFieldsJsonMapMock = new HashMap<>();
        existingCustomFieldsJsonMapMock.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        when(existingCustomFieldsMock.getType()).thenReturn(categoryTypeReference);
        when(existingCustomFieldsMock.getFieldsJsonMap()).thenReturn(existingCustomFieldsJsonMapMock);

        // Mock new CustomFieldsDraft
        CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        Map<String, JsonNode> newCustomFieldsJsonMapMock = new HashMap<>();
        newCustomFieldsJsonMapMock.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);
        when(newCustomFieldsMock.getFields()).thenReturn(newCustomFieldsJsonMapMock);

        // Mock TypeService and Category Custom Type key Cache.
        TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);

        List<UpdateAction<Category>> updateActions =
                buildNonNullCustomFieldsActions(existingCustomFieldsMock,
                        newCustomFieldsMock, typeServiceMock);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomField");
    }

    @Test
    public void buildNonNullCustomFieldsActions_WithDifferentCategoryTypeKeys_ShouldBuildUpdateActions() {
        Reference<Type> categoryTypeReference = mock(Reference.class);
        String categoryCustomTypeInternalId = "categoryCustomTypeId";
        String categoryCustomTypeKey = "categoryCustomTypeKey";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);
        when(categoryTypeReference.getKey()).thenReturn(categoryCustomTypeKey);

        Reference<Type> newCategoryTypeReference = mock(Reference.class);
        String newCategoryCustomTypeId = "newCategoryCustomTypeId";
        String newCategoryCustomTypeKey = "newCategoryCustomTypeKey";
        when(newCategoryTypeReference.getId()).thenReturn(newCategoryCustomTypeId);
        when(newCategoryTypeReference.getKey()).thenReturn(newCategoryCustomTypeKey);

        // Mock existing CustomFields
        CustomFields existingCustomFieldsMock = mock(CustomFields.class);
        when(existingCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock new CustomFieldsDraft
        CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(newCategoryTypeReference);

        // Mock TypeService and Category Custom Type key Cache.
        TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);

        List<UpdateAction<Category>> updateActions = buildNonNullCustomFieldsActions(existingCustomFieldsMock,
                newCustomFieldsMock, typeServiceMock);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildNonNullCustomFieldsActions_WithNullExistingCategoryTypeKey_ShouldBuildUpdateActions() {
        Reference<Type> categoryTypeReference = mock(Reference.class);
        String categoryCustomTypeInternalId = "categoryCustomTypeId";
        String categoryCustomTypeKey = "categoryCustomTypeKey";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);
        when(categoryTypeReference.getKey()).thenReturn(categoryCustomTypeKey);

        // Mock existing CustomFields
        CustomFields existingCustomFieldsMock = mock(CustomFields.class);
        when(existingCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock new CustomFieldsDraft
        CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock TypeService and Category Custom Type key Cache.
        TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(null);

        List<UpdateAction<Category>> updateActions = buildNonNullCustomFieldsActions(existingCustomFieldsMock,
                newCustomFieldsMock, typeServiceMock);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildNonNullCustomFieldsActions_WithNullNewCategoryTypeKey_ShouldBuildUpdateActions() {
        Reference<Type> categoryTypeReference = mock(Reference.class);
        String categoryCustomTypeInternalId = "categoryCustomTypeId";
        String categoryCustomTypeKey = "categoryCustomTypeKey";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);

        // Mock existing CustomFields
        CustomFields existingCustomFieldsMock = mock(CustomFields.class);
        when(existingCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock new CustomFieldsDraft
        CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock TypeService and Category Custom Type key Cache.
        TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);

        List<UpdateAction<Category>> updateActions = buildNonNullCustomFieldsActions(existingCustomFieldsMock,
                newCustomFieldsMock, typeServiceMock);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildNonNullCustomFieldsActions_WithNullKeys_ShouldNotBuildUpdateActions() {
        Reference<Type> categoryTypeReference = mock(Reference.class);
        String categoryCustomTypeInternalId = "categoryCustomTypeId";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);

        // Mock existing CustomFields
        CustomFields existingCustomFieldsMock = mock(CustomFields.class);
        when(existingCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock new CustomFieldsDraft
        CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock TypeService and Category Custom Type key Cache.
        TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(null);

        List<UpdateAction<Category>> updateActions =
                buildNonNullCustomFieldsActions(existingCustomFieldsMock,
                        newCustomFieldsMock, typeServiceMock);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildNonNullCustomFieldsActions_WithSameKeysButNullNewCustomFields_ShouldNotBuildUpdateActions() {
        Reference<Type> categoryTypeReference = mock(Reference.class);
        String categoryCustomTypeInternalId = "categoryCustomTypeId";
        String categoryCustomTypeKey = "categoryCustomTypeKey";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);
        when(categoryTypeReference.getKey()).thenReturn(categoryCustomTypeKey);

        // Mock existing CustomFields
        CustomFields existingCustomFieldsMock = mock(CustomFields.class);
        Map<String, JsonNode> existingCustomFieldsJsonMapMock = new HashMap<>();
        existingCustomFieldsJsonMapMock.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        when(existingCustomFieldsMock.getType()).thenReturn(categoryTypeReference);
        when(existingCustomFieldsMock.getFieldsJsonMap()).thenReturn(existingCustomFieldsJsonMapMock);

        // Mock new CustomFieldsDraft
        CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);
        when(newCustomFieldsMock.getFields()).thenReturn(null);

        // Mock TypeService and Category Custom Type key Cache.
        TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);

        List<UpdateAction<Category>> updateActions =
                buildNonNullCustomFieldsActions(existingCustomFieldsMock,
                        newCustomFieldsMock, typeServiceMock);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsActions_WithDifferentCustomFieldValues_ShouldBuildUpdateActions() {
        Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        List<UpdateAction<Category>> setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isNotEmpty();
        assertThat(setCustomFieldsUpdateActions).hasSize(2);
        UpdateAction<Category> categoryUpdateAction = setCustomFieldsUpdateActions.get(0);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("setCustomField");
    }

    @Test
    public void buildSetCustomFieldsActions_WithNoNewCustomFieldsInExistingCustomFields_ShouldBuildUpdateActions() {
        Map<String, JsonNode> existingCustomFields = new HashMap<>();

        Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));
        newCustomFields.put("url", JsonNodeFactory.instance.objectNode().put("domain", "domain.com"));
        newCustomFields.put("size", JsonNodeFactory.instance.objectNode().put("cm", 34));

        List<UpdateAction<Category>> setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isNotEmpty();
        assertThat(setCustomFieldsUpdateActions).hasSize(4);
    }

    @Test
    public void buildSetCustomFieldsActions_WithExistingCustomFieldNotInNewFields_ShouldBuildUpdateActions() {
        Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        List<UpdateAction<Category>> setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isNotEmpty();
        assertThat(setCustomFieldsUpdateActions).hasSize(1);
    }

    @Test
    public void buildSetCustomFieldsActions_WithSameCustomFieldValues_ShouldNotBuildUpdateActions() {
        Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        List<UpdateAction<Category>> setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsActions_WithDifferentOrderOfCustomFieldValues_ShouldNotBuildUpdateActions() {
        Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("es", "rojo"));

        Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo").put("de", "rot"));

        List<UpdateAction<Category>> setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsActions_WithEmptyCustomFieldValues_ShouldNotBuildUpdateActions() {
        Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode());

        Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode());

        List<UpdateAction<Category>> setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsActions_WithEmptyCustomFields_ShouldNotBuildUpdateActions() {
        Map<String, JsonNode> existingCustomFields = new HashMap<>();

        Map<String, JsonNode> newCustomFields = new HashMap<>();

        List<UpdateAction<Category>> setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();
    }

    @Test
    public void buildNewOrModifiedCustomFieldsActions_WithNewOrModifiedCustomFields_ShouldBuildUpdateActions() {
        Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        List<UpdateAction<Category>> customFieldsActions =
                buildNewOrModifiedCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isNotEmpty();
        assertThat(customFieldsActions).hasSize(1);
    }

    @Test
    public void buildNewOrModifiedCustomFieldsActions_WithNoNewOrModifiedCustomFields_ShouldNotBuildUpdateActions() {
        Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        List<UpdateAction<Category>> customFieldsActions =
                buildNewOrModifiedCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isEmpty();
    }

    @Test
    public void buildRemovedCustomFieldsActions_WithRemovedCustomField_ShouldBuildUpdateActions() {
        Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        List<UpdateAction<Category>> customFieldsActions =
                buildRemovedCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isNotEmpty();
        assertThat(customFieldsActions).hasSize(1);
    }

    @Test
    public void buildRemovedCustomFieldsActions_WithNoRemovedCustomField_ShouldNotBuildUpdateActions() {
        Map<String, JsonNode> existingCustomFields = new HashMap<>();
        existingCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        List<UpdateAction<Category>> customFieldsActions =
                buildRemovedCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isEmpty();
    }
}
