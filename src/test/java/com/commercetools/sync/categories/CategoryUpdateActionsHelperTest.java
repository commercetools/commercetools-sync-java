package com.commercetools.sync.categories;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.*;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static com.commercetools.sync.categories.CategoryUpdateActionsHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryUpdateActionsHelperTest {
    private final static Category MOCK_EXISTING_CATEGORY = mock(Category.class);
    private final static Locale LOCALE = Locale.GERMAN;
    private final static String MOCK_CATEGORY_REFERENCE_TYPE = "Category";
    private final static String MOCK_EXISTING_CATEGORY_PARENT_ID = "1";
    private final static String MOCK_EXISTING_CATEGORY_NAME = "categoryName";
    private final static String MOCK_EXISTING_CATEGORY_SLUG = "categorySlug";
    private final static String MOCK_EXISTING_CATEGORY_EXTERNAL_ID = "externalId";
    private final static String MOCK_EXISTING_CATEGORY_DESCRIPTION = "categoryDesc";
    private final static String MOCK_EXISTING_CATEGORY_META_DESCRIPTION = "categoryMetaDesc";
    private final static String MOCK_EXISTING_CATEGORY_META_TITLE = "categoryMetaTitle";
    private final static String MOCK_EXISTING_CATEGORY_META_KEYWORDS = "categoryKeywords";
    private final static String MOCK_EXISTING_CATEGORY_ORDERHINT = "123";


    @BeforeClass
    public static void setup() {
        when(MOCK_EXISTING_CATEGORY.getName()).thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));
        when(MOCK_EXISTING_CATEGORY.getSlug()).thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_SLUG));
        when(MOCK_EXISTING_CATEGORY.getExternalId()).thenReturn(MOCK_EXISTING_CATEGORY_EXTERNAL_ID);
        when(MOCK_EXISTING_CATEGORY.getDescription()).thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_DESCRIPTION));
        when(MOCK_EXISTING_CATEGORY.getMetaDescription()).thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_META_DESCRIPTION));
        when(MOCK_EXISTING_CATEGORY.getMetaTitle()).thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_META_TITLE));
        when(MOCK_EXISTING_CATEGORY.getMetaKeywords()).thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_META_KEYWORDS));
        when(MOCK_EXISTING_CATEGORY.getOrderHint()).thenReturn(MOCK_EXISTING_CATEGORY_ORDERHINT);
        when(MOCK_EXISTING_CATEGORY.getParent()).thenReturn(Reference.of(MOCK_CATEGORY_REFERENCE_TYPE, MOCK_EXISTING_CATEGORY_PARENT_ID));
    }

    @Test
    public void buildChangeNameUpdateAction_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getName()).thenReturn(LocalizedString.of(LOCALE, "newName"));

        final Optional<UpdateAction<Category>> changeNameUpdateAction =
                buildChangeNameUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isPresent();
        assertThat(changeNameUpdateAction.get().getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) (changeNameUpdateAction.get())).getName()).isEqualTo(LocalizedString.of(LOCALE, "newName"));
    }

    @Test
    public void buildChangeNameUpdateAction_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameName = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameName.getName())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> changeNameUpdateAction =
                buildChangeNameUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameName);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeSlugUpdateAction_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getSlug()).thenReturn(LocalizedString.of(LOCALE, "newSlug"));

        final Optional<UpdateAction<Category>> changeSlugUpdateAction =
                buildChangeSlugUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(changeSlugUpdateAction).isNotNull();
        assertThat(changeSlugUpdateAction).isPresent();
        assertThat(changeSlugUpdateAction.get().getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) (changeSlugUpdateAction.get())).getSlug()).isEqualTo(LocalizedString.of(LOCALE, "newSlug"));
    }

    @Test
    public void buildChangeSlugUpdateAction_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameSlug = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameSlug.getSlug())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_SLUG));

        final Optional<UpdateAction<Category>> changeSlugUpdateAction =
                buildChangeSlugUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameSlug);

        assertThat(changeSlugUpdateAction).isNotNull();
        assertThat(changeSlugUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetDescriptionUpdateAction_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getDescription()).thenReturn(LocalizedString.of(LOCALE, "newDescription"));

        final Optional<UpdateAction<Category>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction).isPresent();
        assertThat(setDescriptionUpdateAction.get().getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) (setDescriptionUpdateAction.get())).getDescription())
                .isEqualTo(LocalizedString.of(LOCALE, "newDescription"));
    }

    @Test
    public void buildSetDescriptionUpdateAction_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameDescription = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameDescription.getDescription())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_DESCRIPTION));

        final Optional<UpdateAction<Category>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameDescription);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeParentUpdateAction_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getParent()).thenReturn(Reference.of("Category", "2"));

        final Optional<UpdateAction<Category>> changeParentUpdateAction =
                buildChangeParentUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction).isPresent();
        assertThat(changeParentUpdateAction.get().getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent) (changeParentUpdateAction.get())).getParent())
                .isEqualTo(Reference.of("Category", "2"));
    }

    @Test
    public void buildChangeParentUpdateAction_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameParent = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameParent.getParent())
                .thenReturn(Reference.of(MOCK_CATEGORY_REFERENCE_TYPE, MOCK_EXISTING_CATEGORY_PARENT_ID));

        final Optional<UpdateAction<Category>> changeParentUpdateAction =
                buildChangeParentUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameParent);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeOrderHintUpdateAction_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getOrderHint()).thenReturn("099");

        final Optional<UpdateAction<Category>> changeOrderHintUpdateAction =
                buildChangeOrderHintUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction).isPresent();
        assertThat(changeOrderHintUpdateAction.get().getAction()).isEqualTo("changeOrderHint");
        assertThat(((ChangeOrderHint) (changeOrderHintUpdateAction.get())).getOrderHint())
                .isEqualTo("099");
    }

    @Test
    public void buildChangeOrderHintUpdateAction_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameOrderHint = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameOrderHint.getOrderHint()).thenReturn(MOCK_EXISTING_CATEGORY_ORDERHINT);

        final Optional<UpdateAction<Category>> changeOrderHintUpdateAction =
                buildChangeOrderHintUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameOrderHint);

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaTitleUpdateAction_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaTitle()).thenReturn(LocalizedString.of(LOCALE, "newMetaTitle"));

        final Optional<UpdateAction<Category>> setMetaTitleUpdateAction =
                buildSetMetaTitleUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction).isPresent();
        assertThat(setMetaTitleUpdateAction.get().getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) (setMetaTitleUpdateAction.get())).getMetaTitle())
                .isEqualTo(LocalizedString.of(LOCALE, "newMetaTitle"));
    }

    @Test
    public void buildSetMetaTitleUpdateAction_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameTitle = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameTitle.getMetaTitle())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_META_TITLE));

        final Optional<UpdateAction<Category>> setMetaTitleUpdateAction =
                buildSetMetaTitleUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameTitle);

        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaKeywords()).thenReturn(LocalizedString.of(LOCALE, "newMetaKW"));

        final Optional<UpdateAction<Category>> setMetaKeywordsUpdateAction =
                buildSetMetaKeywordsUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction).isPresent();
        assertThat(setMetaKeywordsUpdateAction.get().getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) (setMetaKeywordsUpdateAction.get())).getMetaKeywords())
                .isEqualTo(LocalizedString.of(LOCALE, "newMetaKW"));
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameMetaKeywords = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameMetaKeywords.getMetaKeywords())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_META_KEYWORDS));

        final Optional<UpdateAction<Category>> setMetaKeywordsUpdateAction =
                buildSetMetaKeywordsUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameMetaKeywords);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaDescription()).thenReturn(LocalizedString.of(LOCALE, "newMetaDesc"));

        final Optional<UpdateAction<Category>> setMetaDescriptionUpdateAction =
                buildSetMetaDescriptionUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction).isPresent();
        assertThat(setMetaDescriptionUpdateAction.get().getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) (setMetaDescriptionUpdateAction.get())).getMetaDescription())
                .isEqualTo(LocalizedString.of(LOCALE, "newMetaDesc"));
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameMetaDescription = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameMetaDescription.getMetaDescription())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_META_DESCRIPTION));

        final Optional<UpdateAction<Category>> setMetaDescriptionUpdateAction =
                buildSetMetaDescriptionUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameMetaDescription);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_ShouldBuildUpdateAction() {
        LocalizedString existingLocalisedString = LocalizedString.of(LOCALE, "Apfel");
        final LocalizedString newLocalisedString = LocalizedString.of(LOCALE, "Milch");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        Optional<UpdateAction<Category>> updateActionForLocalizedStrings =
                buildUpdateActionForLocalizedStrings(existingLocalisedString, newLocalisedString, mockUpdateAction);

        assertThat(updateActionForLocalizedStrings).isNotNull();
        assertThat(updateActionForLocalizedStrings).isPresent();
        assertThat(updateActionForLocalizedStrings.get().getAction()).isEqualTo("changeName");

        // Assert that empty existing localised fields means that the localised strings are different.
        existingLocalisedString = LocalizedString.of(new HashMap<>());

        updateActionForLocalizedStrings =
                buildUpdateActionForLocalizedStrings(existingLocalisedString, newLocalisedString, mockUpdateAction);

        assertThat(updateActionForLocalizedStrings).isNotNull();
        assertThat(updateActionForLocalizedStrings).isPresent();
        assertThat(updateActionForLocalizedStrings.get().getAction()).isEqualTo("changeName");

        // Assert that one null LocalisedString means that the localised strings are different.
        updateActionForLocalizedStrings =
                buildUpdateActionForLocalizedStrings(null, newLocalisedString, mockUpdateAction);

        assertThat(updateActionForLocalizedStrings).isNotNull();
        assertThat(updateActionForLocalizedStrings).isPresent();
        assertThat(updateActionForLocalizedStrings.get().getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_ShouldNotBuildUpdateAction() {
        LocalizedString existingLocalisedString = LocalizedString.of(LOCALE, "Milch");
        LocalizedString newLocalisedString = LocalizedString.of(LOCALE, "Milch");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        Optional<UpdateAction<Category>> updateActionForLocalizedStrings =
                buildUpdateActionForLocalizedStrings(existingLocalisedString, newLocalisedString, mockUpdateAction);

        assertThat(updateActionForLocalizedStrings).isNotNull();
        assertThat(updateActionForLocalizedStrings).isNotPresent();

        // Assert that order of localised fields shouldn't mean localised strings are different
        newLocalisedString = newLocalisedString.plus(Locale.ENGLISH, "Milk");
        newLocalisedString = newLocalisedString.plus(Locale.FRENCH, "Lait");
        existingLocalisedString = existingLocalisedString.plus(Locale.FRENCH, "Lait");
        existingLocalisedString = existingLocalisedString.plus(Locale.ENGLISH, "Milk");

        updateActionForLocalizedStrings =
                buildUpdateActionForLocalizedStrings(existingLocalisedString, newLocalisedString, mockUpdateAction);

        assertThat(updateActionForLocalizedStrings).isNotNull();
        assertThat(updateActionForLocalizedStrings).isNotPresent();

        // Assert that both null localised fields shouldn't mean localised strings are different.
        updateActionForLocalizedStrings =
                buildUpdateActionForLocalizedStrings(null, null, mockUpdateAction);

        assertThat(updateActionForLocalizedStrings).isNotNull();
        assertThat(updateActionForLocalizedStrings).isNotPresent();

        // Assert that both empty maps of localised fields shouldn't mean localised strings are different.
        updateActionForLocalizedStrings =
                buildUpdateActionForLocalizedStrings(LocalizedString.of(new HashMap<>()),
                        LocalizedString.of(new HashMap<>()), mockUpdateAction);

        assertThat(updateActionForLocalizedStrings).isNotNull();
        assertThat(updateActionForLocalizedStrings).isNotPresent();
    }

    @Test
    public void buildUpdateActionForReferences_ShouldBuildUpdateAction() {
        final Reference<Category> existingCategoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "1");
        Reference<Category> newCategoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "2");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        Optional<UpdateAction<Category>> updateActionForReferences =
                buildUpdateActionForReferences(existingCategoryReference, newCategoryReference, mockUpdateAction);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isPresent();
        assertThat(updateActionForReferences.get().getAction()).isEqualTo("changeName");

        // Assert if references differ by type not by id, then the Resources are different.
        newCategoryReference = Reference.ofResourceTypeIdAndId("newType", "1");

        updateActionForReferences =
                buildUpdateActionForReferences(existingCategoryReference, newCategoryReference, mockUpdateAction);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isPresent();
        assertThat(updateActionForReferences.get().getAction()).isEqualTo("changeName");

        // Assert if one of the references is null, then the Resources are different.
        updateActionForReferences =
                buildUpdateActionForReferences(null, newCategoryReference, mockUpdateAction);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isPresent();
        assertThat(updateActionForReferences.get().getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForReferences_ShouldNotBuildUpdateAction() {
        final Reference<Category> existingCategoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "1");
        final Reference<Category> newCategoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "1");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        Optional<UpdateAction<Category>> updateActionForReferences =
                buildUpdateActionForReferences(existingCategoryReference, newCategoryReference, mockUpdateAction);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isNotPresent();

        // Assert if both references are null, then they still are not different.
        updateActionForReferences =
                buildUpdateActionForReferences(null, null, mockUpdateAction);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isNotPresent();
    }

    @Test
    public void buildUpdateActionForStrings_ShouldBuildUpdateAction() {
        final String existingString = "1";
        final String newString = "2";
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        Optional<UpdateAction<Category>> updateActionForStrings =
                buildUpdateActionForStrings(existingString, newString, mockUpdateAction);

        assertThat(updateActionForStrings).isNotNull();
        assertThat(updateActionForStrings).isPresent();
        assertThat(updateActionForStrings.get().getAction()).isEqualTo("changeName");

        // Assert if one of the Strings is null, then they are different.
        updateActionForStrings =
                buildUpdateActionForStrings(null, newString, mockUpdateAction);

        assertThat(updateActionForStrings).isNotNull();
        assertThat(updateActionForStrings).isPresent();
        assertThat(updateActionForStrings.get().getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForStrings_ShouldNotBuildUpdateAction() {
        final String existingString = "1";
        final String newString = "1";
        UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        Optional<UpdateAction<Category>> updateActionForStrings =
                buildUpdateActionForStrings(existingString, newString, mockUpdateAction);

        assertThat(updateActionForStrings).isNotNull();
        assertThat(updateActionForStrings).isNotPresent();

        // Assert if both strings are null, then they still are not different.
        updateActionForStrings =
                buildUpdateActionForStrings(null, null, mockUpdateAction);

        assertThat(updateActionForStrings).isNotNull();
        assertThat(updateActionForStrings).isNotPresent();
    }

    /***
     * ===================================================================================================================
     * ===================================================================================================================
     */

    @Test
    public void buildSetCustomFieldsActions_ShouldBuildUpdateAction() {
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

        // Assert that existing empty custom fields should be updated with all values in new custom fields
        existingCustomFields = new HashMap<>();
        newCustomFields.put("url", JsonNodeFactory.instance.objectNode().put("domain", "domain.com"));
        newCustomFields.put("size", JsonNodeFactory.instance.objectNode().put("cm", 34));


        setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isNotEmpty();
        assertThat(setCustomFieldsUpdateActions).hasSize(4);

        // Assert that existing custom field value should be removed if it doesn't exist in new custom fields
        existingCustomFields = new HashMap<>();
        existingCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));


        setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isNotEmpty();
        assertThat(setCustomFieldsUpdateActions).hasSize(1);
    }

    @Test
    public void buildSetCustomFieldsActions_ShouldNotBuildUpdateAction() {
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

        // Assert that order of JSON fields in the custom field value should be result in no update actions
        existingCustomFields = new HashMap<>();
        newCustomFields = new HashMap<>();

        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("es", "rojo"));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo").put("de", "rot"));

        setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();

        // Assert that empty custom field values should result in no update actions
        existingCustomFields = new HashMap<>();
        newCustomFields = new HashMap<>();

        existingCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode());
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode());

        setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();

        // Assert that empty custom fields should result in no update actions
        existingCustomFields = new HashMap<>();
        newCustomFields = new HashMap<>();

        setCustomFieldsUpdateActions =
                buildSetCustomFieldsActions(existingCustomFields, newCustomFields);

        assertThat(setCustomFieldsUpdateActions).isNotNull();
        assertThat(setCustomFieldsUpdateActions).isEmpty();
    }

    @Test
    public void buildNewOrModifiedCustomFieldsActions_ShouldBuildUpdateActions() {
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
    public void buildNewOrModifiedCustomFieldsActions_ShouldNotBuildUpdateActions() {
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
    public void buildRemovedCustomFieldsActions_ShouldBuildUpdateActions() {
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
    public void buildRemovedCustomFieldsActions_ShouldNotBuildUpdateActions() {
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
