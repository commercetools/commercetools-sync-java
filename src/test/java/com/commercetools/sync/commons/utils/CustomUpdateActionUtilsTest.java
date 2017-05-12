package com.commercetools.sync.commons.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.helpers.CtpClient;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildCustomUpdateActions;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildNonNullCustomFieldsUpdateActions;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildSetCustomFieldsUpdateActions;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildNewOrModifiedCustomFieldsUpdateActions;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildRemovedCustomFieldsUpdateActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomUpdateActionUtilsTest {
    private static final CtpClient CTP_CLIENT = mock(CtpClient.class);
    private static final CategorySyncOptions CATEGORY_SYNC_OPTIONS = CategorySyncOptionsBuilder.of(CTP_CLIENT).build();

    @Test
    public void buildCustomUpdateActions_WithNonNullCustomFieldsWithDifferentKeys_ShouldBuildUpdateActions() {
        final String oldCategoryCustomTypeKey = "1";
        final Category oldCategory = mock(Category.class);
        final CustomFields oldCategoryCustomFields = mock(CustomFields.class);
        final Reference<Type> oldCategoryCustomFieldsDraftTypeReference = Reference.of("type", "anyId");
        when(oldCategoryCustomFields.getType()).thenReturn(oldCategoryCustomFieldsDraftTypeReference);
        when(oldCategory.getCustom()).thenReturn(oldCategoryCustomFields);

        final String newCategoryCustomTypeKey = "2";
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        final CustomFieldsDraft newCategoryCustomFieldsDraft = mock(CustomFieldsDraft.class);

        final ResourceIdentifier<Type> typeResourceIdentifier = ResourceIdentifier
            .ofIdOrKey("anyId", newCategoryCustomTypeKey);
        when(newCategoryCustomFieldsDraft.getType()).thenReturn(typeResourceIdentifier);
        when(newCategoryDraft.getCustom()).thenReturn(newCategoryCustomFieldsDraft);

        final TypeService typeService = mock(TypeServiceImpl.class);
        when(typeService.getCachedTypeKeyById(anyString())).thenReturn(oldCategoryCustomTypeKey);

        final List<UpdateAction<Category>> updateActions =
            buildCustomUpdateActions(oldCategory, newCategoryDraft, CATEGORY_SYNC_OPTIONS, typeService);

        // Should set custom type of old category.
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildCustomUpdateActions_WithNullOldCustomFields_ShouldBuildUpdateActions() {
        final Category oldCategory = mock(Category.class);
        when(oldCategory.getCustom()).thenReturn(null);

        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        final CustomFieldsDraft newCategoryCustomFieldsDraft = mock(CustomFieldsDraft.class);
        final ResourceIdentifier<Type> newCategoryCustomFieldsDraftTypeReference = ResourceIdentifier.ofId("anyId");
        when(newCategoryCustomFieldsDraft.getType()).thenReturn(newCategoryCustomFieldsDraftTypeReference);
        when(newCategoryDraft.getCustom()).thenReturn(newCategoryCustomFieldsDraft);

        final List<UpdateAction<Category>> updateActions =
            buildCustomUpdateActions(oldCategory, newCategoryDraft, CATEGORY_SYNC_OPTIONS,
                mock(TypeService.class));

        // Should add custom type to old category.
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildCustomUpdateActions_WithNullNewCustomFields_ShouldBuildUpdateActions() {
        final Category oldCategory = mock(Category.class);
        final CustomFields oldCategoryCustomFields = mock(CustomFields.class);
        when(oldCategory.getCustom()).thenReturn(oldCategoryCustomFields);

        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getCustom()).thenReturn(null);

        final List<UpdateAction<Category>> updateActions =
            buildCustomUpdateActions(oldCategory, newCategoryDraft, CATEGORY_SYNC_OPTIONS,
                mock(TypeService.class));

        // Should remove custom type from old category.
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildCustomUpdateActions_WithNullKeys_ShouldCallSyncOptionsCallBack() {
        final Reference<Type> categoryTypeReference = Reference.of("type", "anyId");

        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        when(oldCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock old Category
        final Category oldCategory = mock(Category.class);
        when(oldCategory.getId()).thenReturn("oldCategoryId");
        when(oldCategory.toReference()).thenReturn(Reference.of(Category.referenceTypeId(), "oldCategoryId"));
        when(oldCategory.getCustom()).thenReturn(oldCustomFieldsMock);

        // Mock new Category
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getCustom()).thenReturn(newCustomFieldsMock);

        // Mock custom options error callback
        final ArrayList<Object> callBackResponses = new ArrayList<>();
        final BiConsumer<String, Throwable> updateActionErrorCallBack = (errorMessage, exception) -> {
            callBackResponses.add(errorMessage);
            callBackResponses.add(exception);
        };

        // Mock sync options
        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(mock(CtpClient.class))
                                                                                  .setErrorCallBack(
                                                                                      updateActionErrorCallBack)
                                                                                  .build();

        // Mock type service and Category Custom Type key Cache.
        final TypeService typeServiceMock = mock(TypeServiceImpl.class);
        when(typeServiceMock.getCachedTypeKeyById(anyString())).thenReturn(null);

        final List<UpdateAction<Category>> updateActions =
            buildCustomUpdateActions(oldCategory, newCategoryDraft, categorySyncOptions, typeServiceMock);

        assertThat(callBackResponses).hasSize(2);
        assertThat(callBackResponses.get(0)).isEqualTo("Failed to build custom fields update actions on the category"
            + " with id 'oldCategoryId'. Reason: Custom type keys are not set for both the old and new category.");
        assertThat((Exception) callBackResponses.get(1)).isInstanceOf(BuildUpdateActionException.class);
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithBothNullCustomFields_ShouldNotBuildUpdateActions() {
        final Category oldCategory = mock(Category.class);
        when(oldCategory.getCustom()).thenReturn(null);

        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getCustom()).thenReturn(null);

        final List<UpdateAction<Category>> updateActions =
            buildCustomUpdateActions(oldCategory, newCategoryDraft, CATEGORY_SYNC_OPTIONS,
                mock(TypeService.class));

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithSameCategoryTypeKeys_ShouldBuildUpdateActions()
        throws BuildUpdateActionException {
        @SuppressWarnings("unchecked")
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
        final TypeService typeService = mock(TypeServiceImpl.class);
        when(typeService.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);

        final List<UpdateAction<Category>> updateActions =
            buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
                newCustomFieldsMock, mock(Category.class),
                CATEGORY_SYNC_OPTIONS,
                typeService);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomField");
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithDifferentCategoryTypeKeys_ShouldBuildUpdateActions()
        throws BuildUpdateActionException {
        @SuppressWarnings("unchecked")
        final Reference<Type> categoryTypeReference = mock(Reference.class);
        final String categoryCustomTypeInternalId = "categoryCustomTypeId";
        final String categoryCustomTypeKey = "categoryCustomTypeKey";
        when(categoryTypeReference.getId()).thenReturn(categoryCustomTypeInternalId);
        when(categoryTypeReference.getKey()).thenReturn(categoryCustomTypeKey);

        final ResourceIdentifier<Type> newCategoryTypeReference = ResourceIdentifier
            .ofIdOrKey("newCategoryCustomTypeId",
                "newCategoryCustomTypeKey");

        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        when(oldCustomFieldsMock.getType()).thenReturn(categoryTypeReference);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(newCategoryTypeReference);

        // Mock TypeService and Category Custom Type key Cache.
        final TypeService typeService = mock(TypeServiceImpl.class);
        when(typeService.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);

        final List<UpdateAction<Category>> updateActions = buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
            newCustomFieldsMock, mock(Category.class), CATEGORY_SYNC_OPTIONS, mock(TypeService.class));

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithNullOldCategoryTypeKey_ShouldBuildUpdateActions()
        throws BuildUpdateActionException {
        @SuppressWarnings("unchecked")
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
        final TypeService typeService = mock(TypeServiceImpl.class);
        when(typeService.getCachedTypeKeyById(anyString())).thenReturn(null);

        final List<UpdateAction<Category>> updateActions = buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
            newCustomFieldsMock, mock(Category.class), CATEGORY_SYNC_OPTIONS, mock(TypeService.class));

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithNullNewCategoryTypeKey_ShouldBuildUpdateActions()
        throws BuildUpdateActionException {
        @SuppressWarnings("unchecked")
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
        final TypeService typeService = mock(TypeServiceImpl.class);
        when(typeService.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);


        final List<UpdateAction<Category>> updateActions = buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
            newCustomFieldsMock, mock(Category.class), CATEGORY_SYNC_OPTIONS, typeService);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithSameKeysButNullNewCustomFields_ShouldBuildUpdateActions()
        throws BuildUpdateActionException {
        @SuppressWarnings("unchecked")
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
        final TypeService typeService = mock(TypeServiceImpl.class);
        when(typeService.getCachedTypeKeyById(anyString())).thenReturn(categoryCustomTypeKey);

        final List<UpdateAction<Category>> updateActions =
            buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
                newCustomFieldsMock, mock(Category.class), CATEGORY_SYNC_OPTIONS, typeService);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomType");
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithNullKeys_ShouldThrowBuildUpdateActionException()
        throws BuildUpdateActionException {
        @SuppressWarnings("unchecked")
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
        final TypeService typeService = mock(TypeServiceImpl.class);
        when(typeService.getCachedTypeKeyById(anyString())).thenReturn(null);

        final Category oldCategory = mock(Category.class);
        when(oldCategory.getId()).thenReturn("oldCategoryId");
        when(oldCategory.toReference()).thenReturn(Reference.of(Category.referenceTypeId(), "oldCategoryId"));

        assertThatThrownBy(() ->
            buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
                newCustomFieldsMock, oldCategory, CATEGORY_SYNC_OPTIONS, mock(TypeService.class)))
            .isInstanceOf(BuildUpdateActionException.class);
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
                CATEGORY_SYNC_OPTIONS);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isNotEmpty();
        assertThat(setCustomFieldsUpdateActions).hasSize(2);
        final UpdateAction<Category> categoryUpdateAction = setCustomFieldsUpdateActions.get(0);
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

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
            buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class),
                CATEGORY_SYNC_OPTIONS);

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
                CATEGORY_SYNC_OPTIONS);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isNotEmpty();
        assertThat(setCustomFieldsUpdateActions).hasSize(1);
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithSameCustomFieldValues_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
            buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class),
                CATEGORY_SYNC_OPTIONS);

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
                CATEGORY_SYNC_OPTIONS);

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
                CATEGORY_SYNC_OPTIONS);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithEmptyCustomFields_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();

        final Map<String, JsonNode> newCustomFields = new HashMap<>();

        final List<UpdateAction<Category>> setCustomFieldsUpdateActions =
            buildSetCustomFieldsUpdateActions(oldCustomFields, newCustomFields, mock(Category.class),
                CATEGORY_SYNC_OPTIONS);

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
                CATEGORY_SYNC_OPTIONS);

        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isNotEmpty();
        assertThat(customFieldsActions).hasSize(1);
    }

    @Test
    public void
        buildNewOrModifiedCustomFieldsUpdateActions_WithNewOrModifiedNonHandledResourceFields_ShouldNotBuildActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        // Cart resource is not handled in GenericUpdateActionUtils#buildTypedUpdateAction
        final Cart cart = mock(Cart.class);
        when(cart.toReference()).thenReturn(Reference.of(Cart.referenceTypeId(), "cartId"));

        final List<UpdateAction<Cart>> customFieldsActions =
            buildNewOrModifiedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, cart, CATEGORY_SYNC_OPTIONS);

        // Custom fields update actions should not be built
        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isEmpty();
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
                CATEGORY_SYNC_OPTIONS);

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
                CATEGORY_SYNC_OPTIONS);

        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isNotEmpty();
        assertThat(customFieldsActions).hasSize(1);
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
                CATEGORY_SYNC_OPTIONS);

        assertThat(customFieldsActions).isNotNull();
        assertThat(customFieldsActions).isEmpty();
    }
}
