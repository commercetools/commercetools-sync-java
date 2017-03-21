package com.commercetools.sync.categories;


import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.*;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

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
        CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getName()).thenReturn(LocalizedString.of(LOCALE, "newName"));

        Optional<UpdateAction<Category>> changeNameUpdateAction =
                buildChangeNameUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isPresent();
        assertThat(changeNameUpdateAction.get().getAction()).isEqualTo("changeName");
        assertThat(((ChangeName)(changeNameUpdateAction.get())).getName()).isEqualTo(LocalizedString.of(LOCALE, "newName"));
    }

    @Test
    public void buildChangeNameUpdateAction_ShouldNotBuildUpdateAction() {
        CategoryDraft newCategoryDraftWithSameName = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameName.getName())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        Optional<UpdateAction<Category>> changeNameUpdateAction =
                buildChangeNameUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameName);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeSlugUpdateAction_ShouldBuildUpdateAction() {
        CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getSlug()).thenReturn(LocalizedString.of(LOCALE, "newSlug"));

        Optional<UpdateAction<Category>> changeSlugUpdateAction =
                buildChangeSlugUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(changeSlugUpdateAction).isNotNull();
        assertThat(changeSlugUpdateAction).isPresent();
        assertThat(changeSlugUpdateAction.get().getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug)(changeSlugUpdateAction.get())).getSlug()).isEqualTo(LocalizedString.of(LOCALE, "newSlug"));
    }

    @Test
    public void buildChangeSlugUpdateAction_ShouldNotBuildUpdateAction() {
        CategoryDraft newCategoryDraftWithSameSlug = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameSlug.getSlug())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_SLUG));

        Optional<UpdateAction<Category>> changeSlugUpdateAction =
                buildChangeSlugUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameSlug);

        assertThat(changeSlugUpdateAction).isNotNull();
        assertThat(changeSlugUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetDescriptionUpdateAction_ShouldBuildUpdateAction() {
        CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getDescription()).thenReturn(LocalizedString.of(LOCALE, "newDescription"));

        Optional<UpdateAction<Category>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction).isPresent();
        assertThat(setDescriptionUpdateAction.get().getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription)(setDescriptionUpdateAction.get())).getDescription())
                .isEqualTo(LocalizedString.of(LOCALE, "newDescription"));
    }

    @Test
    public void buildSetDescriptionUpdateAction_ShouldNotBuildUpdateAction() {
        CategoryDraft newCategoryDraftWithSameDescription = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameDescription.getDescription())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_DESCRIPTION));

        Optional<UpdateAction<Category>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameDescription);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeParentUpdateAction_ShouldBuildUpdateAction() {
        CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getParent()).thenReturn(Reference.of("Category", "2"));

        Optional<UpdateAction<Category>> changeParentUpdateAction =
                buildChangeParentUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction).isPresent();
        assertThat(changeParentUpdateAction.get().getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent)(changeParentUpdateAction.get())).getParent())
                .isEqualTo(Reference.of("Category", "2"));
    }

    @Test
    public void buildChangeParentUpdateAction_ShouldNotBuildUpdateAction() {
        CategoryDraft newCategoryDraftWithSameParent = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameParent.getParent())
                .thenReturn(Reference.of(MOCK_CATEGORY_REFERENCE_TYPE, MOCK_EXISTING_CATEGORY_PARENT_ID));

        Optional<UpdateAction<Category>> changeParentUpdateAction =
                buildChangeParentUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameParent);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeOrderHintUpdateAction_ShouldBuildUpdateAction() {
        CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getOrderHint()).thenReturn("099");

        Optional<UpdateAction<Category>> changeOrderHintUpdateAction =
                buildChangeOrderHintUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction).isPresent();
        assertThat(changeOrderHintUpdateAction.get().getAction()).isEqualTo("changeOrderHint");
        assertThat(((ChangeOrderHint)(changeOrderHintUpdateAction.get())).getOrderHint())
                .isEqualTo("099");
    }

    @Test
    public void buildChangeOrderHintUpdateAction_ShouldNotBuildUpdateAction() {
        CategoryDraft newCategoryDraftWithSameOrderHint = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameOrderHint.getOrderHint()).thenReturn(MOCK_EXISTING_CATEGORY_ORDERHINT);

        Optional<UpdateAction<Category>> changeOrderHintUpdateAction =
                buildChangeOrderHintUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameOrderHint);

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaTitleUpdateAction_ShouldBuildUpdateAction() {
        CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaTitle()).thenReturn(LocalizedString.of(LOCALE, "newMetaTitle"));

        Optional<UpdateAction<Category>> setMetaTitleUpdateAction =
                buildSetMetaTitleUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction).isPresent();
        assertThat(setMetaTitleUpdateAction.get().getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle)(setMetaTitleUpdateAction.get())).getMetaTitle())
                .isEqualTo(LocalizedString.of(LOCALE, "newMetaTitle"));
    }

    @Test
    public void buildSetMetaTitleUpdateAction_ShouldNotBuildUpdateAction() {
        CategoryDraft newCategoryDraftWithSameTitle = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameTitle.getMetaTitle())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_META_TITLE));

        Optional<UpdateAction<Category>> setMetaTitleUpdateAction =
                buildSetMetaTitleUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameTitle);

        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_ShouldBuildUpdateAction() {
        CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaKeywords()).thenReturn(LocalizedString.of(LOCALE, "newMetaKW"));

        Optional<UpdateAction<Category>> setMetaKeywordsUpdateAction =
                buildSetMetaKeywordsUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction).isPresent();
        assertThat(setMetaKeywordsUpdateAction.get().getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords)(setMetaKeywordsUpdateAction.get())).getMetaKeywords())
                .isEqualTo(LocalizedString.of(LOCALE, "newMetaKW"));
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_ShouldNotBuildUpdateAction() {
        CategoryDraft newCategoryDraftWithSameMetaKeywords = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameMetaKeywords.getMetaKeywords())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_META_KEYWORDS));

        Optional<UpdateAction<Category>> setMetaKeywordsUpdateAction =
                buildSetMetaKeywordsUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameMetaKeywords);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_ShouldBuildUpdateAction() {
        CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaDescription()).thenReturn(LocalizedString.of(LOCALE, "newMetaDesc"));

        Optional<UpdateAction<Category>> setMetaDescriptionUpdateAction =
                buildSetMetaDescriptionUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction).isPresent();
        assertThat(setMetaDescriptionUpdateAction.get().getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription)(setMetaDescriptionUpdateAction.get())).getMetaDescription())
                .isEqualTo(LocalizedString.of(LOCALE, "newMetaDesc"));
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_ShouldNotBuildUpdateAction() {
        CategoryDraft newCategoryDraftWithSameMetaDescription = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameMetaDescription.getMetaDescription())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_META_DESCRIPTION));

        Optional<UpdateAction<Category>> setMetaDescriptionUpdateAction =
                buildSetMetaDescriptionUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameMetaDescription);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_ShouldBuildUpdateAction() {
        LocalizedString existingLocalisedString = LocalizedString.of(LOCALE, "Apfel");
        LocalizedString newLocalisedString = LocalizedString.of(LOCALE, "Milch");
        UpdateAction<Category> mockUpdateAction = ChangeName.of(
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
        UpdateAction<Category> mockUpdateAction = ChangeName.of(
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
        Reference<Category> existingCategoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "1");
        Reference<Category> newCategoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "2");
        UpdateAction<Category> mockUpdateAction = ChangeName.of(
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
        Reference<Category> existingCategoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "1");
        Reference<Category> newCategoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "1");
        UpdateAction<Category> mockUpdateAction = ChangeName.of(
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
        String existingString = "1";
        String newString = "2";
        UpdateAction<Category> mockUpdateAction = ChangeName.of(
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
        String existingString = "1";
        String newString = "1";
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
}
