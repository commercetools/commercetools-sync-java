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

import static com.commercetools.sync.categories.CategoryDiff.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryDiffTest {
    private final static Category MOCK_EXISTING_CATEGORY = mock(Category.class);
    private final static Locale LOCALE = Locale.GERMAN;
    private final static String MOCK_CATEGORY_REFERENCE_TYPE = "type";
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
    public void buildChangeNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getName()).thenReturn(LocalizedString.of(LOCALE, "newName"));

        final Optional<UpdateAction<Category>> changeNameUpdateAction =
                buildChangeNameUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        UpdateAction<Category> categoryUpdateAction = changeNameUpdateAction.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) categoryUpdateAction).getName()).isEqualTo(LocalizedString.of(LOCALE, "newName"));
    }

    @Test
    public void buildChangeNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameName = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameName.getName())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> changeNameUpdateAction =
                buildChangeNameUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameName);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeSlugUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getSlug()).thenReturn(LocalizedString.of(LOCALE, "newSlug"));

        final Optional<UpdateAction<Category>> changeSlugUpdateAction =
                buildChangeSlugUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        UpdateAction<Category> categoryUpdateAction = changeSlugUpdateAction.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) categoryUpdateAction).getSlug()).isEqualTo(LocalizedString.of(LOCALE, "newSlug"));
    }

    @Test
    public void buildChangeSlugUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameSlug = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameSlug.getSlug())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_SLUG));

        final Optional<UpdateAction<Category>> changeSlugUpdateAction =
                buildChangeSlugUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameSlug);

        assertThat(changeSlugUpdateAction).isNotNull();
        assertThat(changeSlugUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getDescription()).thenReturn(LocalizedString.of(LOCALE, "newDescription"));

        final Optional<UpdateAction<Category>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        UpdateAction<Category> categoryUpdateAction = setDescriptionUpdateAction.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) categoryUpdateAction).getDescription())
                .isEqualTo(LocalizedString.of(LOCALE, "newDescription"));
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameDescription = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameDescription.getDescription())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_DESCRIPTION));

        final Optional<UpdateAction<Category>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameDescription);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeParentUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getParent()).thenReturn(Reference.of("Category", "2"));

        final Optional<UpdateAction<Category>> changeParentUpdateAction =
                buildChangeParentUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        UpdateAction<Category> categoryUpdateAction = changeParentUpdateAction.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent) categoryUpdateAction).getParent())
                .isEqualTo(Reference.of("Category", "2"));
    }

    @Test
    public void buildChangeParentUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameParent = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameParent.getParent())
                .thenReturn(Reference.of(MOCK_CATEGORY_REFERENCE_TYPE, MOCK_EXISTING_CATEGORY_PARENT_ID));

        final Optional<UpdateAction<Category>> changeParentUpdateAction =
                buildChangeParentUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameParent);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getOrderHint()).thenReturn("099");

        final Optional<UpdateAction<Category>> changeOrderHintUpdateAction =
                buildChangeOrderHintUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        UpdateAction<Category> categoryUpdateAction = changeOrderHintUpdateAction.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeOrderHint");
        assertThat(((ChangeOrderHint) categoryUpdateAction).getOrderHint())
                .isEqualTo("099");
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameOrderHint = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameOrderHint.getOrderHint()).thenReturn(MOCK_EXISTING_CATEGORY_ORDERHINT);

        final Optional<UpdateAction<Category>> changeOrderHintUpdateAction =
                buildChangeOrderHintUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameOrderHint);

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaTitle()).thenReturn(LocalizedString.of(LOCALE, "newMetaTitle"));

        final Optional<UpdateAction<Category>> setMetaTitleUpdateAction =
                buildSetMetaTitleUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        UpdateAction<Category> categoryUpdateAction = setMetaTitleUpdateAction.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) categoryUpdateAction).getMetaTitle())
                .isEqualTo(LocalizedString.of(LOCALE, "newMetaTitle"));
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameTitle = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameTitle.getMetaTitle())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_META_TITLE));

        final Optional<UpdateAction<Category>> setMetaTitleUpdateAction =
                buildSetMetaTitleUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameTitle);

        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaKeywords()).thenReturn(LocalizedString.of(LOCALE, "newMetaKW"));

        final Optional<UpdateAction<Category>> setMetaKeywordsUpdateAction =
                buildSetMetaKeywordsUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        UpdateAction<Category> categoryUpdateAction = setMetaKeywordsUpdateAction.orElse(null);
        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) categoryUpdateAction).getMetaKeywords())
                .isEqualTo(LocalizedString.of(LOCALE, "newMetaKW"));
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameMetaKeywords = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameMetaKeywords.getMetaKeywords())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_META_KEYWORDS));

        final Optional<UpdateAction<Category>> setMetaKeywordsUpdateAction =
                buildSetMetaKeywordsUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameMetaKeywords);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaDescription()).thenReturn(LocalizedString.of(LOCALE, "newMetaDesc"));

        final Optional<UpdateAction<Category>> setMetaDescriptionUpdateAction =
                buildSetMetaDescriptionUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraft);

        UpdateAction<Category> categoryUpdateAction = setMetaDescriptionUpdateAction.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) categoryUpdateAction).getMetaDescription())
                .isEqualTo(LocalizedString.of(LOCALE, "newMetaDesc"));
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameMetaDescription = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameMetaDescription.getMetaDescription())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_META_DESCRIPTION));

        final Optional<UpdateAction<Category>> setMetaDescriptionUpdateAction =
                buildSetMetaDescriptionUpdateAction(MOCK_EXISTING_CATEGORY, newCategoryDraftWithSameMetaDescription);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_WithDifferentFieldValues_ShouldBuildUpdateAction() {
        final LocalizedString existingLocalisedString = LocalizedString.of(LOCALE, "Apfel");
        final LocalizedString newLocalisedString = LocalizedString.of(LOCALE, "Milch");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForLocalizedStrings =
                buildUpdateActionForLocalizedStrings(existingLocalisedString, newLocalisedString, mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForLocalizedStrings.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_WithEmptyExistingFields_ShouldBuildUpdateAction() {
        final LocalizedString existingLocalisedString = LocalizedString.of(new HashMap<>());
        final LocalizedString newLocalisedString = LocalizedString.of(LOCALE, "Milch");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForLocalizedStrings =
                buildUpdateActionForLocalizedStrings(existingLocalisedString, newLocalisedString, mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForLocalizedStrings.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_WithEmptyFields_ShouldNotBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));
        final Optional<UpdateAction<Category>> updateActionForLocalizedStrings =
                buildUpdateActionForLocalizedStrings(LocalizedString.of(new HashMap<>()),
                        LocalizedString.of(new HashMap<>()), mockUpdateAction);

        assertThat(updateActionForLocalizedStrings).isNotNull();
        assertThat(updateActionForLocalizedStrings).isNotPresent();
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
    public void buildUpdateActionForReferences_WithDifferentTypeReferenceIds_ShouldBuildUpdateAction() {
        final Reference<Category> existingCategoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "1");
        Reference<Category> newCategoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "2");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForReferences =
                buildUpdateActionForReferences(existingCategoryReference, newCategoryReference, mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForReferences.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForReferences_WithOneNullReference_ShouldBuildUpdateAction() {
        final Reference<Category> categoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "1");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForReferences =
                buildUpdateActionForReferences(null, categoryReference, mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForReferences.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForReferences_WithSameCategoryReferences_ShouldNotBuildUpdateAction() {
        final Reference<Category> categoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "1");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForReferences =
                buildUpdateActionForReferences(categoryReference, categoryReference, mockUpdateAction);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isNotPresent();
    }

    @Test
    public void buildUpdateActionForReferences_WithNullReferences_ShouldNotBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));
        final Optional<UpdateAction<Category>> updateActionForReferences =
                buildUpdateActionForReferences(null, null, mockUpdateAction);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isNotPresent();
    }

    @Test
    public void buildUpdateActionForStrings_WithDifferentStrings_ShouldBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForStrings =
                buildUpdateActionForStrings("1", "2", mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForStrings.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForStrings_WithOneNullString_ShouldBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForStrings =
                buildUpdateActionForStrings(null, "2", mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForStrings.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForStrings_WithSameStrings_ShouldNotBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForStrings =
                buildUpdateActionForStrings("1", "1", mockUpdateAction);

        assertThat(updateActionForStrings).isNotNull();
        assertThat(updateActionForStrings).isNotPresent();
    }

    @Test
    public void buildUpdateActionForStrings_WithNullStrings_ShouldNotBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));
        final Optional<UpdateAction<Category>> updateActionForStrings =
                buildUpdateActionForStrings(null, null, mockUpdateAction);

        assertThat(updateActionForStrings).isNotNull();
        assertThat(updateActionForStrings).isNotPresent();
    }
}
