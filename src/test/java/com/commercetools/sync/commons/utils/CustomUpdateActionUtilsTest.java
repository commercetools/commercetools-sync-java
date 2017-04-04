package com.commercetools.sync.commons.utils;


import com.commercetools.sync.commons.helpers.SyncResult;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomUpdateActionUtilsTest {
    @Test
    public void buildCustomUpdateActions_WithNonNullCustomFieldsWithDifferentKeys_ShouldBuildUpdateActions() {
        final String oldCategoryCustomTypeKey = "1";
        final Category oldCategory = mock(Category.class);
        final CustomFields oldCategoryCustomFields = mock(CustomFields.class);
        final Reference<Type> oldCategoryCustomFieldsDraftTypeReference = mock(Reference.class);
        when(oldCategoryCustomFields.getType()).thenReturn(oldCategoryCustomFieldsDraftTypeReference);
        when(oldCategory.getCustom()).thenReturn(oldCategoryCustomFields);

        final String newCategoryCustomTypeKey = "2";
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        final CustomFieldsDraft newCategoryCustomFieldsDraft = mock(CustomFieldsDraft.class);
        final Reference<Type> newCategoryCustomFieldsDraftTypeReference = mock(Reference.class);
        when(newCategoryCustomFieldsDraftTypeReference.getKey()).thenReturn(newCategoryCustomTypeKey);
        when(newCategoryCustomFieldsDraft.getType()).thenReturn(newCategoryCustomFieldsDraftTypeReference);
        when(newCategoryDraft.getCustom()).thenReturn(newCategoryCustomFieldsDraft);

        final TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(oldCategoryCustomTypeKey);

        final SyncResult<Category> syncResult =
                buildCustomUpdateActions(oldCategory, newCategoryDraft, typeServiceMock);
        // Should set custom type of old category.
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);
        assertThat(syncResult.getUpdateActions().get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildCustomUpdateActions_WithNullOldCustomFields_ShouldBuildUpdateActions() {
        final Category oldCategory = mock(Category.class);
        when(oldCategory.getCustom()).thenReturn(null);

        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        final CustomFieldsDraft newCategoryCustomFieldsDraft = mock(CustomFieldsDraft.class);
        final Reference<Type> newCategoryCustomFieldsDraftTypeReference = mock(Reference.class);
        when(newCategoryCustomFieldsDraft.getType()).thenReturn(newCategoryCustomFieldsDraftTypeReference);
        when(newCategoryDraft.getCustom()).thenReturn(newCategoryCustomFieldsDraft);

        final TypeService typeServiceMock = mock(TypeServiceImpl.class);

        final SyncResult<Category> syncResult =
                buildCustomUpdateActions(oldCategory, newCategoryDraft, typeServiceMock);
        // Should add custom type to old category.
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);
        assertThat(syncResult.getUpdateActions().get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildCustomUpdateActions_WithNullNewCustomFields_ShouldBuildUpdateActions() {
        final Category oldCategory = mock(Category.class);
        final CustomFields oldCategoryCustomFields = mock(CustomFields.class);
        when(oldCategory.getCustom()).thenReturn(oldCategoryCustomFields);

        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getCustom()).thenReturn(null);

        final TypeService typeServiceMock = mock(TypeServiceImpl.class);

        final SyncResult<Category> syncResult =
                buildCustomUpdateActions(oldCategory, newCategoryDraft, typeServiceMock);
        // Should remove custom type from old category.
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);
        assertThat(syncResult.getUpdateActions().get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithBothNullCustomFields_ShouldNotBuildUpdateActions() {
        final Category oldCategory = mock(Category.class);
        when(oldCategory.getCustom()).thenReturn(null);

        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getCustom()).thenReturn(null);

        final TypeService typeServiceMock = mock(TypeServiceImpl.class);

        final SyncResult<Category> syncResult =
                buildCustomUpdateActions(oldCategory, newCategoryDraft, typeServiceMock);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithSameCategoryTypeKeys_ShouldBuildUpdateActions() {
        final Reference<Type> categoryTypeReference = mock(Reference.class);
        final String categoryCustomTypeInternalId = "categoryCustomTypeId";
        final String categoryCustomTypeKey = "categoryCustomTypeKey";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);
        when(categoryTypeReference.getKey()).thenReturn(categoryCustomTypeKey);

        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        final Map<String, JsonNode> oldCustomFieldsJsonMapMock = new HashMap<>();
        oldCustomFieldsJsonMapMock.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        when(oldCustomFieldsMock.getType()).thenReturn(categoryTypeReference);
        when(oldCustomFieldsMock.getFieldsJsonMap()).thenReturn(oldCustomFieldsJsonMapMock);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        final Map<String, JsonNode> newCustomFieldsJsonMapMock = new HashMap<>();
        newCustomFieldsJsonMapMock.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);
        when(newCustomFieldsMock.getFields()).thenReturn(newCustomFieldsJsonMapMock);

        // Mock TypeService and Category Custom Type key Cache.
        final TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);

        final SyncResult<Category> syncResult =
                buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
                        newCustomFieldsMock, typeServiceMock, mock(Category.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);
        assertThat(syncResult.getUpdateActions().get(0).getAction()).isEqualTo("setCustomField");
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithDifferentCategoryTypeKeys_ShouldBuildUpdateActions() {
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

        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        when(oldCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(newCategoryTypeReference);

        // Mock TypeService and Category Custom Type key Cache.
        final TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);

        final SyncResult<Category> syncResult = buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
                newCustomFieldsMock, typeServiceMock, mock(Category.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);
        assertThat(syncResult.getUpdateActions().get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithNullOldCategoryTypeKey_ShouldBuildUpdateActions() {
        final Reference<Type> categoryTypeReference = mock(Reference.class);
        final String categoryCustomTypeInternalId = "categoryCustomTypeId";
        final String categoryCustomTypeKey = "categoryCustomTypeKey";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);
        when(categoryTypeReference.getKey()).thenReturn(categoryCustomTypeKey);

        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        when(oldCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock TypeService and Category Custom Type key Cache.
        final TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(null);

        final SyncResult<Category> syncResult = buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
                newCustomFieldsMock, typeServiceMock, mock(Category.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);
        assertThat(syncResult.getUpdateActions().get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithNullNewCategoryTypeKey_ShouldBuildUpdateActions() {
        final Reference<Type> categoryTypeReference = mock(Reference.class);
        final String categoryCustomTypeInternalId = "categoryCustomTypeId";
        final String categoryCustomTypeKey = "categoryCustomTypeKey";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);

        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        when(oldCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock TypeService and Category Custom Type key Cache.
        final TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);

        final SyncResult<Category> syncResult = buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
                newCustomFieldsMock, typeServiceMock, mock(Category.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);
        assertThat(syncResult.getUpdateActions().get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithNullKeys_ShouldNotBuildUpdateActions() {
        final Reference<Type> categoryTypeReference = mock(Reference.class);
        final String categoryCustomTypeInternalId = "categoryCustomTypeId";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);

        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        when(oldCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock TypeService and Category Custom Type key Cache.
        final TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(null);

        final SyncResult<Category> syncResult =
                buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
                        newCustomFieldsMock, typeServiceMock, mock(Category.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithSameKeysButNullNewCustomFields_ShouldNotBuildUpdateActions() {
        final Reference<Type> categoryTypeReference = mock(Reference.class);
        final String categoryCustomTypeInternalId = "categoryCustomTypeId";
        final String categoryCustomTypeKey = "categoryCustomTypeKey";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);
        when(categoryTypeReference.getKey()).thenReturn(categoryCustomTypeKey);

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

        // Mock TypeService and Category Custom Type key Cache.
        final TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);

        final SyncResult<Category> syncResult =
                buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
                        newCustomFieldsMock, typeServiceMock, mock(Category.class));

        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);
        assertThat(syncResult.getUpdateActions().get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithDifferentCustomFieldValues_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final SyncResult<Category> syncResult =
                buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class));

        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isNotEmpty();
        assertThat(syncResult.getUpdateActions()).hasSize(2);

        final UpdateAction<Category> categoryUpdateAction = syncResult.getUpdateActions().get(0);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("setCustomField");
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithNoNewCustomFieldsInOldCustomFields_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));
        newCustomFields.put("url", JsonNodeFactory.instance.objectNode().put("domain", "domain.com"));
        newCustomFields.put("size", JsonNodeFactory.instance.objectNode().put("cm", 34));

        final SyncResult<Category> syncResult =
                buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isNotEmpty();
        assertThat(syncResult.getUpdateActions()).hasSize(4);
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithOldCustomFieldNotInNewFields_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final SyncResult<Category> syncResult =
                buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isNotEmpty();
        assertThat(syncResult.getUpdateActions()).hasSize(1);
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithSameCustomFieldValues_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final SyncResult<Category> syncResult =
                buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithDifferentOrderOfCustomFieldValues_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("es", "rojo"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo").put("de", "rot"));

        final SyncResult<Category> syncResult =
                buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithEmptyCustomFieldValues_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode());

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode());

        final SyncResult<Category> syncResult =
                buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithEmptyCustomFields_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();

        final Map<String, JsonNode> newCustomFields = new HashMap<>();

        final SyncResult<Category> syncResult =
                buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildNewOrModifiedCustomFieldsUpdateActions_WithNewOrModifiedCustomFields_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final SyncResult<Category> syncResult =
                buildNewOrModifiedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);
    }

    @Test
    public void buildNewOrModifiedCustomFieldsUpdateActions_WithNewOrModifiedNonHandledResourceCustomFields_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        // Cart resource is not handled in GenericUpdateActionUtils#buildTypedUpdateAction
        final SyncResult<Cart> syncResult =
                buildNewOrModifiedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Cart.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildNewOrModifiedCustomFieldsUpdateActions_WithNoNewOrModifiedCustomFields_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final SyncResult<Category> syncResult =
                buildNewOrModifiedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildRemovedCustomFieldsUpdateActions_WithRemovedCustomField_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final SyncResult<Category> syncResult =
                buildRemovedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);
    }

    @Test
    public void buildRemovedCustomFieldsUpdateActions_WithNoRemovedCustomField_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final SyncResult<Category> syncResult =
                buildRemovedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class));
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }
}
