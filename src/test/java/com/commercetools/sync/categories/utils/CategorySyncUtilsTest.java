package com.commercetools.sync.categories.utils;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.MockUtils;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.*;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CategorySyncUtilsTest {
    private static Category MOCK_OLD_CATEGORY;
    private final static Locale LOCALE = Locale.GERMAN;
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
        MOCK_OLD_CATEGORY = MockUtils.getMockCategory(LOCALE,
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
    public void buildCoreActions_FromDraftsWithDifferentNameValues_ShouldBuildUpdateActions() {
        final CategoryDraft newCategoryDraft = MockUtils.getMockCategoryDraft(LOCALE,
                "differentName",
                MOCK_OLD_CATEGORY_SLUG,
                MOCK_OLD_CATEGORY_EXTERNAL_ID,
                MOCK_OLD_CATEGORY_DESCRIPTION,
                MOCK_OLD_CATEGORY_META_DESCRIPTION,
                MOCK_OLD_CATEGORY_META_TITLE,
                MOCK_OLD_CATEGORY_META_KEYWORDS,
                MOCK_OLD_CATEGORY_ORDERHINT,
                MOCK_OLD_CATEGORY_PARENT_ID);

        final List<UpdateAction<Category>> updateActions =
                CategorySyncUtils.buildCoreActions(MOCK_OLD_CATEGORY, newCategoryDraft, mock(CategorySyncOptions.class));
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);

        final UpdateAction<Category> updateAction = updateActions.get(0);
        assertThat(updateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) updateAction).getName()).isEqualTo(LocalizedString.of(LOCALE, "differentName"));
    }

    @Test
    public void buildCoreActions_FromDraftsWithMultipleDifferentValues_ShouldBuildUpdateActions() {
        final CategoryDraft newCategoryDraft = MockUtils.getMockCategoryDraft(LOCALE,
                "differentName",
                "differentSlug",
                MOCK_OLD_CATEGORY_EXTERNAL_ID,
                "differentDescription",
                "differentMetaDescription",
                "differentMetaTitle",
                "differentMetaKeywords",
                "differentOrderHint",
                "differentParentId");

        final List<UpdateAction<Category>> updateActions =
                CategorySyncUtils.buildCoreActions(MOCK_OLD_CATEGORY, newCategoryDraft, mock(CategorySyncOptions.class));
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(8);

        final UpdateAction<Category> nameUpdateAction = updateActions.get(0);
        assertThat(nameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) nameUpdateAction).getName()).isEqualTo(LocalizedString.of(LOCALE, "differentName"));

        final UpdateAction<Category> slugUpdateAction = updateActions.get(1);
        assertThat(slugUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) slugUpdateAction).getSlug()).isEqualTo(LocalizedString.of(LOCALE, "differentSlug"));

        final UpdateAction<Category> descriptionUpdateAction = updateActions.get(2);
        assertThat(descriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) descriptionUpdateAction).getDescription())
                .isEqualTo(LocalizedString.of(LOCALE, "differentDescription"));

        final UpdateAction<Category> parentUpdateAction = updateActions.get(3);
        assertThat(parentUpdateAction.getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent) parentUpdateAction).getParent().getId()).isEqualTo("differentParentId");

        final UpdateAction<Category> orderHintUpdateAction = updateActions.get(4);
        assertThat(orderHintUpdateAction.getAction()).isEqualTo("changeOrderHint");
        assertThat(((ChangeOrderHint) orderHintUpdateAction).getOrderHint()).isEqualTo("differentOrderHint");

        final UpdateAction<Category> metaTitleUpdateAction = updateActions.get(5);
        assertThat(metaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) metaTitleUpdateAction).getMetaTitle())
                .isEqualTo(LocalizedString.of(LOCALE, "differentMetaTitle"));

        final UpdateAction<Category> metaDescriptionUpdateAction = updateActions.get(6);
        assertThat(metaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) metaDescriptionUpdateAction).getMetaDescription())
                .isEqualTo(LocalizedString.of(LOCALE, "differentMetaDescription"));

        final UpdateAction<Category> metaKeywordsUpdateAction = updateActions.get(7);
        assertThat(metaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) metaKeywordsUpdateAction).getMetaKeywords())
                .isEqualTo(LocalizedString.of(LOCALE, "differentMetaKeywords"));
    }

    @Test
    public void buildCoreActions_FromDraftsWithSameValues_ShouldNotBuildUpdateActions() {
        final CategoryDraft newCategoryDraft = MockUtils.getMockCategoryDraft(LOCALE,
                MOCK_OLD_CATEGORY_NAME,
                MOCK_OLD_CATEGORY_SLUG,
                MOCK_OLD_CATEGORY_EXTERNAL_ID,
                MOCK_OLD_CATEGORY_DESCRIPTION,
                MOCK_OLD_CATEGORY_META_DESCRIPTION,
                MOCK_OLD_CATEGORY_META_TITLE,
                MOCK_OLD_CATEGORY_META_KEYWORDS,
                MOCK_OLD_CATEGORY_ORDERHINT,
                MOCK_OLD_CATEGORY_PARENT_ID);

        final List<UpdateAction<Category>> updateActions =
                CategorySyncUtils.buildCoreActions(MOCK_OLD_CATEGORY, newCategoryDraft, mock(CategorySyncOptions.class));
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(0);
    }
}
