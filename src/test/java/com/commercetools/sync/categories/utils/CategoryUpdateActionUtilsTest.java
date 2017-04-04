package com.commercetools.sync.categories.utils;


import com.commercetools.sync.commons.helpers.SyncResult;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.*;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryUpdateActionUtilsTest {
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

        final SyncResult<Category> syncResult = buildChangeNameUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);

        final UpdateAction<Category> categoryUpdateAction = syncResult.getUpdateActions().get(0);
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) categoryUpdateAction).getName()).isEqualTo(LocalizedString.of(LOCALE, "newName"));
    }

    @Test
    public void buildChangeNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameName = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameName.getName())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));

        final SyncResult<Category> syncResult =
                buildChangeNameUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameName);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildChangeSlugUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getSlug()).thenReturn(LocalizedString.of(LOCALE, "newSlug"));

        final SyncResult<Category> syncResult =
                buildChangeSlugUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);

        final UpdateAction<Category> categoryUpdateAction = syncResult.getUpdateActions().get(0);
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) categoryUpdateAction).getSlug()).isEqualTo(LocalizedString.of(LOCALE, "newSlug"));
    }

    @Test
    public void buildChangeSlugUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameSlug = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameSlug.getSlug())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_SLUG));

        final SyncResult<Category> syncResult =
                buildChangeSlugUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameSlug);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getDescription()).thenReturn(LocalizedString.of(LOCALE, "newDescription"));

        final SyncResult<Category> syncResult =
                buildSetDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);

        final UpdateAction<Category> categoryUpdateAction = syncResult.getUpdateActions().get(0);
        assertThat(categoryUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) categoryUpdateAction).getDescription())
                .isEqualTo(LocalizedString.of(LOCALE, "newDescription"));
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameDescription = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameDescription.getDescription())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_DESCRIPTION));

        final SyncResult<Category> syncResult =
                buildSetDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameDescription);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildChangeParentUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getParent()).thenReturn(Reference.of("Category", "2"));

        final SyncResult<Category> syncResult =
                buildChangeParentUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);

        final UpdateAction<Category> categoryUpdateAction = syncResult.getUpdateActions().get(0);
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent) categoryUpdateAction).getParent())
                .isEqualTo(Reference.of("Category", "2"));
    }

    @Test
    public void buildChangeParentUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameParent = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameParent.getParent())
                .thenReturn(Reference.of(MOCK_CATEGORY_REFERENCE_TYPE, MOCK_OLD_CATEGORY_PARENT_ID));

        final SyncResult<Category> syncResult =
                buildChangeParentUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameParent);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getOrderHint()).thenReturn("099");

        final SyncResult<Category> syncResult =
                buildChangeOrderHintUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);

        final UpdateAction<Category> categoryUpdateAction = syncResult.getUpdateActions().get(0);
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeOrderHint");
        assertThat(((ChangeOrderHint) categoryUpdateAction).getOrderHint())
                .isEqualTo("099");
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameOrderHint = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameOrderHint.getOrderHint()).thenReturn(MOCK_OLD_CATEGORY_ORDERHINT);

        final SyncResult<Category> syncResult =
                buildChangeOrderHintUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameOrderHint);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaTitle()).thenReturn(LocalizedString.of(LOCALE, "newMetaTitle"));

        final SyncResult<Category> syncResult =
                buildSetMetaTitleUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);

        final UpdateAction<Category> categoryUpdateAction = syncResult.getUpdateActions().get(0);
        assertThat(categoryUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) categoryUpdateAction).getMetaTitle())
                .isEqualTo(LocalizedString.of(LOCALE, "newMetaTitle"));
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameTitle = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameTitle.getMetaTitle())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_META_TITLE));

        final SyncResult<Category> syncResult =
                buildSetMetaTitleUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameTitle);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaKeywords()).thenReturn(LocalizedString.of(LOCALE, "newMetaKW"));

        final SyncResult<Category> syncResult =
                buildSetMetaKeywordsUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);

        final UpdateAction<Category> categoryUpdateAction = syncResult.getUpdateActions().get(0);
        assertThat(categoryUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) categoryUpdateAction).getMetaKeywords())
                .isEqualTo(LocalizedString.of(LOCALE, "newMetaKW"));
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameMetaKeywords = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameMetaKeywords.getMetaKeywords())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_META_KEYWORDS));

        final SyncResult<Category> syncResult =
                buildSetMetaKeywordsUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameMetaKeywords);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaDescription()).thenReturn(LocalizedString.of(LOCALE, "newMetaDesc"));

        final SyncResult<Category> syncResult =
                buildSetMetaDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).hasSize(1);

        final UpdateAction<Category> categoryUpdateAction = syncResult.getUpdateActions().get(0);
        assertThat(categoryUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) categoryUpdateAction).getMetaDescription())
                .isEqualTo(LocalizedString.of(LOCALE, "newMetaDesc"));
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameMetaDescription = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameMetaDescription.getMetaDescription())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_META_DESCRIPTION));

        final SyncResult<Category> syncResult =
                buildSetMetaDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameMetaDescription);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.getUpdateActions()).isEmpty();
    }
}
