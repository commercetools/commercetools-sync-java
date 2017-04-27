package com.commercetools.sync.categories.utils;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.helpers.CtpClient;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.*;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategory;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryUpdateActionUtilsTest {
    private static Category MOCK_OLD_CATEGORY;
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
        MOCK_OLD_CATEGORY = getMockCategory(LOCALE,
                MOCK_OLD_CATEGORY_NAME,
                MOCK_OLD_CATEGORY_SLUG,
                MOCK_OLD_CATEGORY_EXTERNAL_ID,
                MOCK_OLD_CATEGORY_DESCRIPTION,
                MOCK_OLD_CATEGORY_META_DESCRIPTION,
                MOCK_OLD_CATEGORY_META_TITLE,
                MOCK_OLD_CATEGORY_META_KEYWORDS,
                MOCK_OLD_CATEGORY_ORDERHINT,
                MOCK_OLD_CATEGORY_PARENT_ID);
    }

    @Test
    public void buildChangeNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getName()).thenReturn(LocalizedString.of(LOCALE, "newName"));

        final UpdateAction<Category> changeNameUpdateAction =
                buildChangeNameUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) changeNameUpdateAction).getName()).isEqualTo(LocalizedString.of(LOCALE, "newName"));
    }

    @Test
    public void buildChangeNameUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getName()).thenReturn(null);

        final UpdateAction<Category> changeNameUpdateAction =
                buildChangeNameUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) changeNameUpdateAction).getName()).isNull();
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

        final UpdateAction<Category> changeSlugUpdateAction =
                buildChangeSlugUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(changeSlugUpdateAction).isNotNull();
        assertThat(changeSlugUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) changeSlugUpdateAction).getSlug()).isEqualTo(LocalizedString.of(LOCALE, "newSlug"));
    }

    @Test
    public void buildChangeSlugUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getSlug()).thenReturn(null);

        final UpdateAction<Category> changeSlugUpdateAction =
                buildChangeSlugUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(changeSlugUpdateAction).isNotNull();
        assertThat(changeSlugUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) changeSlugUpdateAction).getSlug()).isNull();
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

        final UpdateAction<Category> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft,
                        mock(CategorySyncOptions.class)).orElse(null);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) setDescriptionUpdateAction).getDescription())
                .isEqualTo(LocalizedString.of(LOCALE, "newDescription"));
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameDescription = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameDescription.getDescription())
                .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_DESCRIPTION));

        final Optional<UpdateAction<Category>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameDescription,
                        mock(CategorySyncOptions.class));

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithNullValues_ShouldNotBuildUpdateActionAndCallCallback() {
        when(MOCK_OLD_CATEGORY.getId()).thenReturn("oldCatId");
        final CategoryDraft newCategory = mock(CategoryDraft.class);
        when(newCategory.getDescription()).thenReturn(null);

        final ArrayList<Object> callBackResponse = new ArrayList<>();
        final Consumer<String> updateActionWarningCallBack = callBackResponse::add;

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(mock(CtpClient.class))
                .setWarningCallBack(updateActionWarningCallBack)
                .build();

        final Optional<UpdateAction<Category>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategory,
                        categorySyncOptions);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction).isNotPresent();
        assertThat(callBackResponse).hasSize(1);
        assertThat(callBackResponse.get(0)).isEqualTo("Cannot unset 'description' field of category with id 'oldCatId'.");
    }

    @Test
    public void buildChangeParentUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getParent()).thenReturn(Reference.of("Category", "2"));

        final UpdateAction<Category> changeParentUpdateAction =
                buildChangeParentUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft,
                        mock(CategorySyncOptions.class)).orElse(null);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction.getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent) changeParentUpdateAction).getParent())
                .isEqualTo(Reference.of("Category", "2"));
    }

    @Test
    public void buildChangeParentUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameParent = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameParent.getParent())
                .thenReturn(Reference.of(MOCK_CATEGORY_REFERENCE_TYPE, MOCK_OLD_CATEGORY_PARENT_ID));

        final Optional<UpdateAction<Category>> changeParentUpdateAction =
                buildChangeParentUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameParent,
                        mock(CategorySyncOptions.class));

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeParentnUpdateAction_WithNullValues_ShouldNotBuildUpdateActionAndCallCallback() {
        when(MOCK_OLD_CATEGORY.getId()).thenReturn("oldCatId");
        final CategoryDraft newCategory = mock(CategoryDraft.class);
        when(newCategory.getParent()).thenReturn(null);

        final ArrayList<Object> callBackResponse = new ArrayList<>();
        final Consumer<String> updateActionWarningCallBack = callBackResponse::add;

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(mock(CtpClient.class))
                .setWarningCallBack(updateActionWarningCallBack)
                .build();

        final Optional<UpdateAction<Category>> changeParentUpdateAction =
                buildChangeParentUpdateAction(MOCK_OLD_CATEGORY, newCategory,
                        categorySyncOptions);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction).isNotPresent();
        assertThat(callBackResponse).hasSize(1);
        assertThat(callBackResponse.get(0)).isEqualTo("Cannot unset 'parent' field of category with id 'oldCatId'.");
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getOrderHint()).thenReturn("099");

        final UpdateAction<Category> changeOrderHintUpdateAction =
                buildChangeOrderHintUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft,
                        mock(CategorySyncOptions.class)).orElse(null);

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction.getAction()).isEqualTo("changeOrderHint");
        assertThat(((ChangeOrderHint) changeOrderHintUpdateAction).getOrderHint())
                .isEqualTo("099");
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameOrderHint = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameOrderHint.getOrderHint()).thenReturn(MOCK_OLD_CATEGORY_ORDERHINT);

        final Optional<UpdateAction<Category>> changeOrderHintUpdateAction =
                buildChangeOrderHintUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameOrderHint,
                        mock(CategorySyncOptions.class));

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithNullValues_ShouldNotBuildUpdateActionAndCallCallback() {
        when(MOCK_OLD_CATEGORY.getId()).thenReturn("oldCatId");
        final CategoryDraft newCategory = mock(CategoryDraft.class);
        when(newCategory.getOrderHint()).thenReturn(null);

        final ArrayList<Object> callBackResponse = new ArrayList<>();
        final Consumer<String> updateActionWarningCallBack = callBackResponse::add;

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(mock(CtpClient.class))
                .setWarningCallBack(updateActionWarningCallBack)
                .build();

        final Optional<UpdateAction<Category>> changeOrderHintUpdateAction =
                buildChangeOrderHintUpdateAction(MOCK_OLD_CATEGORY, newCategory,
                        categorySyncOptions);

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction).isNotPresent();
        assertThat(callBackResponse).hasSize(1);
        assertThat(callBackResponse.get(0)).isEqualTo("Cannot unset 'orderHint' field of category with id 'oldCatId'.");
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaTitle()).thenReturn(LocalizedString.of(LOCALE, "newMetaTitle"));

        final UpdateAction<Category> setMetaTitleUpdateAction =
                buildSetMetaTitleUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) setMetaTitleUpdateAction).getMetaTitle())
                .isEqualTo(LocalizedString.of(LOCALE, "newMetaTitle"));
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaTitle()).thenReturn(null);

        final UpdateAction<Category> setMetaTitleUpdateAction =
                buildSetMetaTitleUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) setMetaTitleUpdateAction).getMetaTitle())
                .isEqualTo(null);
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

        final UpdateAction<Category> setMetaKeywordsUpdateAction =
                buildSetMetaKeywordsUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) setMetaKeywordsUpdateAction).getMetaKeywords())
                .isEqualTo(LocalizedString.of(LOCALE, "newMetaKW"));
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaKeywords()).thenReturn(null);

        final UpdateAction<Category> setMetaKeywordsUpdateAction =
                buildSetMetaKeywordsUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) setMetaKeywordsUpdateAction).getMetaKeywords()).isNull();
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

        final UpdateAction<Category> setMetaDescriptionUpdateAction =
                buildSetMetaDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) setMetaDescriptionUpdateAction).getMetaDescription())
                .isEqualTo(LocalizedString.of(LOCALE, "newMetaDesc"));
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaDescription()).thenReturn(null);

        final UpdateAction<Category> setMetaDescriptionUpdateAction =
                buildSetMetaDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) setMetaDescriptionUpdateAction).getMetaDescription()).isNull();
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
