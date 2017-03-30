package com.commercetools.sync.categories;


import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.*;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.categories.CategoryDiff.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryDiffTest {
    private final static Category MOCK_OLD_CATEGORY = mock(Category.class);
    private final static Locale LOCALE = Locale.GERMAN;
    private final static String MOCK_CATEGORY_REFERENCE_TYPE = "type";
    private final static String MOCK_OLD_CATEGORY_PARENT_ID = "1";
    private final static String MOCK_OLD_CATEGORY_NAME = "categoryName";
    private final static String MOCK_OLD_CATEGORY_SLUG = "categorySlug";
    private final static String MOCK_OLD_CATEGORY_EXTERNAL_ID = "externalId";
    private final static String MOCK_OLD_CATEGORY_DESCRIPTION = "categoryDesc";
    private final static String MOCK_OLD_CATEGORY_META_DESCRIPTION = "categoryMetaDesc";
    private final static String MOCK_OLD_CATEGORY_META_TITLE = "categoryMetaTitle";
    private final static String MOCK_OLD_CATEGORY_META_KEYWORDS = "categoryKeywords";
    private final static String MOCK_OLD_CATEGORY_ORDERHINT = "123";

    @BeforeClass
    public static void setup() {
        when(MOCK_OLD_CATEGORY.getName()).thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));
        when(MOCK_OLD_CATEGORY.getSlug()).thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_SLUG));
        when(MOCK_OLD_CATEGORY.getExternalId()).thenReturn(MOCK_OLD_CATEGORY_EXTERNAL_ID);
        when(MOCK_OLD_CATEGORY.getDescription()).thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_DESCRIPTION));
        when(MOCK_OLD_CATEGORY.getMetaDescription()).thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_META_DESCRIPTION));
        when(MOCK_OLD_CATEGORY.getMetaTitle()).thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_META_TITLE));
        when(MOCK_OLD_CATEGORY.getMetaKeywords()).thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_META_KEYWORDS));
        when(MOCK_OLD_CATEGORY.getOrderHint()).thenReturn(MOCK_OLD_CATEGORY_ORDERHINT);
        when(MOCK_OLD_CATEGORY.getParent()).thenReturn(Reference.of(MOCK_CATEGORY_REFERENCE_TYPE, MOCK_OLD_CATEGORY_PARENT_ID));
    }

    @Test
    public void buildChangeNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getName()).thenReturn(LocalizedString.of(LOCALE, "newName"));

        final Optional<UpdateAction<Category>> changeNameUpdateAction =
                buildChangeNameUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);

        UpdateAction<Category> categoryUpdateAction = changeNameUpdateAction.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) categoryUpdateAction).getName()).isEqualTo(LocalizedString.of(LOCALE, "newName"));
    }

    @Test
    public void buildChangeNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameName = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameName.getName())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> changeNameUpdateAction =
                buildChangeNameUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameName);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeSlugUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getSlug()).thenReturn(LocalizedString.of(LOCALE, "newSlug"));

        final Optional<UpdateAction<Category>> changeSlugUpdateAction =
                buildChangeSlugUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);

        UpdateAction<Category> categoryUpdateAction = changeSlugUpdateAction.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) categoryUpdateAction).getSlug()).isEqualTo(LocalizedString.of(LOCALE, "newSlug"));
    }

    @Test
    public void buildChangeSlugUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameSlug = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameSlug.getSlug())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_SLUG));

        final Optional<UpdateAction<Category>> changeSlugUpdateAction =
                buildChangeSlugUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameSlug);

        assertThat(changeSlugUpdateAction).isNotNull();
        assertThat(changeSlugUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getDescription()).thenReturn(LocalizedString.of(LOCALE, "newDescription"));

        final Optional<UpdateAction<Category>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);

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
                .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_DESCRIPTION));

        final Optional<UpdateAction<Category>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameDescription);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeParentUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getParent()).thenReturn(Reference.of("Category", "2"));

        final Optional<UpdateAction<Category>> changeParentUpdateAction =
                buildChangeParentUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);

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
                .thenReturn(Reference.of(MOCK_CATEGORY_REFERENCE_TYPE, MOCK_OLD_CATEGORY_PARENT_ID));

        final Optional<UpdateAction<Category>> changeParentUpdateAction =
                buildChangeParentUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameParent);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getOrderHint()).thenReturn("099");

        final Optional<UpdateAction<Category>> changeOrderHintUpdateAction =
                buildChangeOrderHintUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);

        UpdateAction<Category> categoryUpdateAction = changeOrderHintUpdateAction.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeOrderHint");
        assertThat(((ChangeOrderHint) categoryUpdateAction).getOrderHint())
                .isEqualTo("099");
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameOrderHint = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameOrderHint.getOrderHint()).thenReturn(MOCK_OLD_CATEGORY_ORDERHINT);

        final Optional<UpdateAction<Category>> changeOrderHintUpdateAction =
                buildChangeOrderHintUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameOrderHint);

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaTitle()).thenReturn(LocalizedString.of(LOCALE, "newMetaTitle"));

        final Optional<UpdateAction<Category>> setMetaTitleUpdateAction =
                buildSetMetaTitleUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);

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
                .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_META_TITLE));

        final Optional<UpdateAction<Category>> setMetaTitleUpdateAction =
                buildSetMetaTitleUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameTitle);

        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaKeywords()).thenReturn(LocalizedString.of(LOCALE, "newMetaKW"));

        final Optional<UpdateAction<Category>> setMetaKeywordsUpdateAction =
                buildSetMetaKeywordsUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);

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
                .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_META_KEYWORDS));

        final Optional<UpdateAction<Category>> setMetaKeywordsUpdateAction =
                buildSetMetaKeywordsUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameMetaKeywords);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaDescription()).thenReturn(LocalizedString.of(LOCALE, "newMetaDesc"));

        final Optional<UpdateAction<Category>> setMetaDescriptionUpdateAction =
                buildSetMetaDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);

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
                .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_META_DESCRIPTION));

        final Optional<UpdateAction<Category>> setMetaDescriptionUpdateAction =
                buildSetMetaDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameMetaDescription);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction).isNotPresent();
    }
}
