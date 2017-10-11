package com.commercetools.sync.categories.utils;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.ChangeName;
import io.sphere.sdk.categories.commands.updateactions.ChangeOrderHint;
import io.sphere.sdk.categories.commands.updateactions.ChangeParent;
import io.sphere.sdk.categories.commands.updateactions.ChangeSlug;
import io.sphere.sdk.categories.commands.updateactions.SetDescription;
import io.sphere.sdk.categories.commands.updateactions.SetExternalId;
import io.sphere.sdk.categories.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.categories.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.categories.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategory;
import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraft;
import static com.commercetools.sync.categories.utils.CategorySyncUtils.replaceCategoriesReferenceIdsWithKeys;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategorySyncUtilsTest {
    private Category mockOldCategory;
    private CategorySyncOptions categorySyncOptions;
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private static final Locale LOCALE = Locale.GERMAN;
    private static final String CATEGORY_PARENT_ID = "1";
    private static final String CATEGORY_NAME = "categoryName";
    private static final String CATEGORY_SLUG = "categorySlug";
    private static final String CATEGORY_KEY = "categoryKey";
    private static final String CATEGORY_EXTERNAL_ID = "externalId";
    private static final String CATEGORY_DESC = "categoryDesc";
    private static final String CATEGORY_META_DESC = "categoryMetaDesc";
    private static final String CATEGORY_META_TITLE = "categoryMetaTitle";
    private static final String CATEGORY_KEYWORDS = "categoryKeywords";
    private static final String CATEGORY_ORDER_HINT = "123";

    /**
     * Initializes an instance of {@link CategorySyncOptions} and {@link Category}.
     */
    @Before
    public void setup() {
        categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                        .build();

        mockOldCategory = getMockCategory(LOCALE,
            CATEGORY_NAME,
            CATEGORY_SLUG,
            CATEGORY_KEY,
            CATEGORY_EXTERNAL_ID,
            CATEGORY_DESC,
            CATEGORY_META_DESC,
            CATEGORY_META_TITLE,
            CATEGORY_KEYWORDS,
            CATEGORY_ORDER_HINT,
            CATEGORY_PARENT_ID);
    }

    @Test
    public void buildActions_FromDraftsWithDifferentNameValues_ShouldBuildUpdateActions() {
        final CategoryDraft newCategoryDraft = getMockCategoryDraft(LOCALE,
            "differentName",
            CATEGORY_SLUG,
            CATEGORY_KEY,
            CATEGORY_EXTERNAL_ID,
            CATEGORY_DESC,
            CATEGORY_META_DESC,
            CATEGORY_META_TITLE,
            CATEGORY_KEYWORDS,
            CATEGORY_ORDER_HINT,
            CATEGORY_PARENT_ID);

        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildActions(mockOldCategory, newCategoryDraft, categorySyncOptions);
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);

        final UpdateAction<Category> updateAction = updateActions.get(0);
        assertThat(updateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) updateAction).getName()).isEqualTo(LocalizedString.of(LOCALE, "differentName"));
    }

    @Test
    public void buildActions_FromDraftsWithMultipleDifferentValues_ShouldBuildUpdateActions() {
        final CategoryDraft newCategoryDraft = getMockCategoryDraft(LOCALE,
            "differentName",
            "differentSlug",
            CATEGORY_KEY,
            "differentExternalId",
            "differentDescription",
            "differentMetaDescription",
            "differentMetaTitle",
            "differentMetaKeywords",
            "differentOrderHint",
            "differentParentId");

        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildActions(mockOldCategory, newCategoryDraft, categorySyncOptions);
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(9);

        final UpdateAction<Category> nameUpdateAction = updateActions.get(0);
        assertThat(nameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) nameUpdateAction).getName()).isEqualTo(LocalizedString.of(LOCALE, "differentName"));

        final UpdateAction<Category> slugUpdateAction = updateActions.get(1);
        assertThat(slugUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) slugUpdateAction).getSlug()).isEqualTo(LocalizedString.of(LOCALE, "differentSlug"));

        final UpdateAction<Category> setExternalIdUpdateAction = updateActions.get(2);
        assertThat(setExternalIdUpdateAction.getAction()).isEqualTo("setExternalId");
        assertThat(((SetExternalId) setExternalIdUpdateAction).getExternalId()).isEqualTo("differentExternalId");

        final UpdateAction<Category> descriptionUpdateAction = updateActions.get(3);
        assertThat(descriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) descriptionUpdateAction).getDescription())
            .isEqualTo(LocalizedString.of(LOCALE, "differentDescription"));

        final UpdateAction<Category> parentUpdateAction = updateActions.get(4);
        assertThat(parentUpdateAction.getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent) parentUpdateAction).getParent().getId()).isEqualTo("differentParentId");

        final UpdateAction<Category> orderHintUpdateAction = updateActions.get(5);
        assertThat(orderHintUpdateAction.getAction()).isEqualTo("changeOrderHint");
        assertThat(((ChangeOrderHint) orderHintUpdateAction).getOrderHint()).isEqualTo("differentOrderHint");

        final UpdateAction<Category> metaTitleUpdateAction = updateActions.get(6);
        assertThat(metaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) metaTitleUpdateAction).getMetaTitle())
            .isEqualTo(LocalizedString.of(LOCALE, "differentMetaTitle"));

        final UpdateAction<Category> metaDescriptionUpdateAction = updateActions.get(7);
        assertThat(metaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) metaDescriptionUpdateAction).getMetaDescription())
            .isEqualTo(LocalizedString.of(LOCALE, "differentMetaDescription"));

        final UpdateAction<Category> metaKeywordsUpdateAction = updateActions.get(8);
        assertThat(metaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) metaKeywordsUpdateAction).getMetaKeywords())
            .isEqualTo(LocalizedString.of(LOCALE, "differentMetaKeywords"));
    }

    @Test
    public void buildActions_FromDraftsWithMultipleDifferentValuesWithFilterFunction_ShouldBuildFilteredActions() {
        final CategoryDraft newCategoryDraft = getMockCategoryDraft(LOCALE,
            "differentName",
            "differentSlug",
            CATEGORY_KEY,
            "differentExternalId",
            "differentDescription",
            "differentMetaDescription",
            "differentMetaTitle",
            "differentMetaKeywords",
            "differentOrderHint",
            "differentParentId");

        final Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>>
            reverseOrderFilter = (unfilteredList) -> {
                Collections.reverse(unfilteredList);
                return unfilteredList;
            };

        categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                        .setUpdateActionsFilter(reverseOrderFilter)
                                                        .build();

        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildActions(mockOldCategory, newCategoryDraft, categorySyncOptions);
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(9);

        final UpdateAction<Category> metaKeywordsUpdateAction = updateActions.get(0);
        assertThat(metaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) metaKeywordsUpdateAction).getMetaKeywords())
            .isEqualTo(LocalizedString.of(LOCALE, "differentMetaKeywords"));

        final UpdateAction<Category> metaDescriptionUpdateAction = updateActions.get(1);
        assertThat(metaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) metaDescriptionUpdateAction).getMetaDescription())
            .isEqualTo(LocalizedString.of(LOCALE, "differentMetaDescription"));

        final UpdateAction<Category> metaTitleUpdateAction = updateActions.get(2);
        assertThat(metaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) metaTitleUpdateAction).getMetaTitle())
            .isEqualTo(LocalizedString.of(LOCALE, "differentMetaTitle"));

        final UpdateAction<Category> orderHintUpdateAction = updateActions.get(3);
        assertThat(orderHintUpdateAction.getAction()).isEqualTo("changeOrderHint");
        assertThat(((ChangeOrderHint) orderHintUpdateAction).getOrderHint()).isEqualTo("differentOrderHint");

        final UpdateAction<Category> parentUpdateAction = updateActions.get(4);
        assertThat(parentUpdateAction.getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent) parentUpdateAction).getParent().getId()).isEqualTo("differentParentId");

        final UpdateAction<Category> descriptionUpdateAction = updateActions.get(5);
        assertThat(descriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) descriptionUpdateAction).getDescription())
            .isEqualTo(LocalizedString.of(LOCALE, "differentDescription"));

        final UpdateAction<Category> setExternalIdUpdateAction = updateActions.get(6);
        assertThat(setExternalIdUpdateAction.getAction()).isEqualTo("setExternalId");
        assertThat(((SetExternalId) setExternalIdUpdateAction).getExternalId()).isEqualTo("differentExternalId");

        final UpdateAction<Category> slugUpdateAction = updateActions.get(7);
        assertThat(slugUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) slugUpdateAction).getSlug()).isEqualTo(LocalizedString.of(LOCALE, "differentSlug"));

        final UpdateAction<Category> nameUpdateAction = updateActions.get(8);
        assertThat(nameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) nameUpdateAction).getName()).isEqualTo(LocalizedString.of(LOCALE, "differentName"));
    }

    @Test
    public void buildCoreActions_FromDraftsWithDifferentNameValues_ShouldBuildUpdateActions() {
        final CategoryDraft newCategoryDraft = getMockCategoryDraft(LOCALE,
            "differentName",
            CATEGORY_SLUG,
            CATEGORY_KEY,
            CATEGORY_EXTERNAL_ID,
            CATEGORY_DESC,
            CATEGORY_META_DESC,
            CATEGORY_META_TITLE,
            CATEGORY_KEYWORDS,
            CATEGORY_ORDER_HINT,
            CATEGORY_PARENT_ID);

        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildCoreActions(mockOldCategory, newCategoryDraft, categorySyncOptions);
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);

        final UpdateAction<Category> updateAction = updateActions.get(0);
        assertThat(updateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) updateAction).getName()).isEqualTo(LocalizedString.of(LOCALE, "differentName"));
    }

    @Test
    public void buildCoreActions_FromDraftsWithMultipleDifferentValues_ShouldBuildUpdateActions() {
        final CategoryDraft newCategoryDraft = getMockCategoryDraft(LOCALE,
            "differentName",
            "differentSlug",
            CATEGORY_KEY,
            "differentExternalId",
            "differentDescription",
            "differentMetaDescription",
            "differentMetaTitle",
            "differentMetaKeywords",
            "differentOrderHint",
            "differentParentId");

        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildCoreActions(mockOldCategory, newCategoryDraft, categorySyncOptions);
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(9);

        final UpdateAction<Category> nameUpdateAction = updateActions.get(0);
        assertThat(nameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) nameUpdateAction).getName()).isEqualTo(LocalizedString.of(LOCALE, "differentName"));

        final UpdateAction<Category> slugUpdateAction = updateActions.get(1);
        assertThat(slugUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) slugUpdateAction).getSlug()).isEqualTo(LocalizedString.of(LOCALE, "differentSlug"));

        final UpdateAction<Category> setExternalIdUpdateAction = updateActions.get(2);
        assertThat(setExternalIdUpdateAction.getAction()).isEqualTo("setExternalId");
        assertThat(((SetExternalId) setExternalIdUpdateAction).getExternalId()).isEqualTo("differentExternalId");

        final UpdateAction<Category> descriptionUpdateAction = updateActions.get(3);
        assertThat(descriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) descriptionUpdateAction).getDescription())
            .isEqualTo(LocalizedString.of(LOCALE, "differentDescription"));

        final UpdateAction<Category> parentUpdateAction = updateActions.get(4);
        assertThat(parentUpdateAction.getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent) parentUpdateAction).getParent().getId()).isEqualTo("differentParentId");

        final UpdateAction<Category> orderHintUpdateAction = updateActions.get(5);
        assertThat(orderHintUpdateAction.getAction()).isEqualTo("changeOrderHint");
        assertThat(((ChangeOrderHint) orderHintUpdateAction).getOrderHint()).isEqualTo("differentOrderHint");

        final UpdateAction<Category> metaTitleUpdateAction = updateActions.get(6);
        assertThat(metaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) metaTitleUpdateAction).getMetaTitle())
            .isEqualTo(LocalizedString.of(LOCALE, "differentMetaTitle"));

        final UpdateAction<Category> metaDescriptionUpdateAction = updateActions.get(7);
        assertThat(metaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) metaDescriptionUpdateAction).getMetaDescription())
            .isEqualTo(LocalizedString.of(LOCALE, "differentMetaDescription"));

        final UpdateAction<Category> metaKeywordsUpdateAction = updateActions.get(8);
        assertThat(metaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) metaKeywordsUpdateAction).getMetaKeywords())
            .isEqualTo(LocalizedString.of(LOCALE, "differentMetaKeywords"));
    }

    @Test
    public void buildCoreActions_FromDraftsWithSameValues_ShouldNotBuildUpdateActions() {
        final CategoryDraft newCategoryDraft = getMockCategoryDraft(LOCALE,
            CATEGORY_NAME,
            CATEGORY_SLUG,
            CATEGORY_KEY,
            CATEGORY_EXTERNAL_ID,
            CATEGORY_DESC,
            CATEGORY_META_DESC,
            CATEGORY_META_TITLE,
            CATEGORY_KEYWORDS,
            CATEGORY_ORDER_HINT,
            CATEGORY_PARENT_ID);

        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildCoreActions(mockOldCategory, newCategoryDraft, categorySyncOptions);
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(0);
    }

    @Test
    public void
    replaceCategoryReferenceIdsWithKeys_WithAllExpandedCategoryReferences_ShouldReturnReferencesWithReplacedKeys() {
        final String parentId = UUID.randomUUID().toString();
        final String customTypeId = UUID.randomUUID().toString();
        final String customTypeKey = "customTypeKey";
        final Type mockCustomType = mock(Type.class);
        when(mockCustomType.getId()).thenReturn(customTypeId);
        when(mockCustomType.getKey()).thenReturn(customTypeKey);

        final List<Category> mockCategories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final Category mockCategory = mock(Category.class);

            // Simulate reference expansion
            final Category mockParent = mock(Category.class);
            when(mockParent.getId()).thenReturn(parentId);
            when(mockParent.getKey()).thenReturn("parentKey" + i);
            final Reference<Category> parentReference = Reference.ofResourceTypeIdAndObj(UUID.randomUUID().toString(),
                mockParent);
            when(mockCategory.getParent()).thenReturn(parentReference);

            final CustomFields mockCustomFields = mock(CustomFields.class);
            final Reference<Type> typeReference = Reference.ofResourceTypeIdAndObj("resourceTypeId",
                mockCustomType);
            when(mockCustomFields.getType()).thenReturn(typeReference);
            when(mockCategory.getCustom()).thenReturn(mockCustomFields);

            mockCategories.add(mockCategory);
        }

        for (final Category category : mockCategories) {
            assertThat(category.getParent().getId()).isEqualTo(parentId);
            assertThat(category.getCustom().getType().getId()).isEqualTo(customTypeId);
        }
        final List<CategoryDraft> referenceReplacedDrafts = replaceCategoriesReferenceIdsWithKeys(mockCategories);

        for (int i = 0; i < referenceReplacedDrafts.size(); i++) {
            assertThat(referenceReplacedDrafts.get(i).getParent().getId()).isEqualTo("parentKey" + i);
            assertThat(referenceReplacedDrafts.get(i).getCustom().getType().getId()).isEqualTo(customTypeKey);
        }
    }

    @Test
    public void
    replaceCategoryReferenceIdsWithKeys_WithNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
        final String parentId = UUID.randomUUID().toString();
        final String customTypeId = UUID.randomUUID().toString();

        final List<Category> mockCategories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final Category mockCategory = mock(Category.class);

            // Simulate no reference expansion
            final Reference<Category> parentReference = Reference.ofResourceTypeIdAndId(UUID.randomUUID().toString(),
                parentId);
            when(mockCategory.getParent()).thenReturn(parentReference);


            final CustomFields mockCustomFields = mock(CustomFields.class);
            final Reference<Type> typeReference = Reference.ofResourceTypeIdAndId("resourceTypeId",
                customTypeId);
            when(mockCustomFields.getType()).thenReturn(typeReference);
            when(mockCategory.getCustom()).thenReturn(mockCustomFields);

            mockCategories.add(mockCategory);
        }

        for (final Category category : mockCategories) {
            assertThat(category.getParent().getId()).isEqualTo(parentId);
            assertThat(category.getCustom().getType().getId()).isEqualTo(customTypeId);
        }
        final List<CategoryDraft> referenceReplacedDrafts = replaceCategoriesReferenceIdsWithKeys(mockCategories);

        for (CategoryDraft referenceReplacedDraft : referenceReplacedDrafts) {
            assertThat(referenceReplacedDraft.getParent().getId()).isEqualTo(parentId);
            assertThat(referenceReplacedDraft.getCustom().getType().getId()).isEqualTo(customTypeId);
        }
    }
}
