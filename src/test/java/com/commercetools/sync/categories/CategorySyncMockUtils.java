package com.commercetools.sync.categories;


import com.commercetools.sync.categories.helpers.CategorySyncOptions;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.services.CategoryService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.commons.MockUtils.getMockCustomFieldsDraft;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategorySyncMockUtils {
    public static Category getMockCategory(@Nonnull final Locale locale,
                                           @Nonnull final String name,
                                           @Nonnull final String slug,
                                           @Nonnull final String externalId,
                                           @Nonnull final String description,
                                           @Nonnull final String metaDescription,
                                           @Nonnull final String metaTitle,
                                           @Nonnull final String metaKeywords,
                                           @Nonnull final String orderHint,
                                           @Nonnull final String parentId) {
        final Category oldCategory = mock(Category.class);
        when(oldCategory.getName()).thenReturn(LocalizedString.of(locale, name));
        when(oldCategory.getSlug()).thenReturn(LocalizedString.of(locale, slug));
        when(oldCategory.getExternalId()).thenReturn(externalId);
        when(oldCategory.getDescription()).thenReturn(LocalizedString.of(locale, description));
        when(oldCategory.getMetaDescription()).thenReturn(LocalizedString.of(locale, metaDescription));
        when(oldCategory.getMetaTitle()).thenReturn(LocalizedString.of(locale, metaTitle));
        when(oldCategory.getMetaKeywords()).thenReturn(LocalizedString.of(locale, metaKeywords));
        when(oldCategory.getOrderHint()).thenReturn(orderHint);
        when(oldCategory.getParent()).thenReturn(Reference.of("type", parentId));
        return oldCategory;
    }

    public static CategoryDraft getMockCategoryDraft(@Nonnull final Locale locale,
                                                     @Nonnull final String name,
                                                     @Nonnull final String slug,
                                                     @Nonnull final String externalId,
                                                     @Nonnull final String description,
                                                     @Nonnull final String metaDescription,
                                                     @Nonnull final String metaTitle,
                                                     @Nonnull final String metaKeywords,
                                                     @Nonnull final String orderHint,
                                                     @Nonnull final String parentId) {
        final CategoryDraft categoryDraft = mock(CategoryDraft.class);
        when(categoryDraft.getName()).thenReturn(LocalizedString.of(locale, name));
        when(categoryDraft.getSlug()).thenReturn(LocalizedString.of(locale, slug));
        when(categoryDraft.getExternalId()).thenReturn(externalId);
        when(categoryDraft.getDescription()).thenReturn(LocalizedString.of(locale, description));
        when(categoryDraft.getMetaDescription()).thenReturn(LocalizedString.of(locale, metaDescription));
        when(categoryDraft.getMetaTitle()).thenReturn(LocalizedString.of(locale, metaTitle));
        when(categoryDraft.getMetaKeywords()).thenReturn(LocalizedString.of(locale, metaKeywords));
        when(categoryDraft.getOrderHint()).thenReturn(orderHint);
        when(categoryDraft.getParent()).thenReturn(Reference.of("type", parentId));
        return categoryDraft;
    }

    public static List<CategoryDraft> getMockCategoryDrafts() {
        final List<CategoryDraft> categoryDrafts = new ArrayList<>();
        CategoryDraft categoryDraft1 = getMockCategoryDraft(Locale.GERMAN, "draft1", "slug1", "SH663881");
        CategoryDraft categoryDraft2 = getMockCategoryDraft(Locale.GERMAN, "draft2", "slug2", "SH604972");
        categoryDrafts.add(categoryDraft1);
        categoryDrafts.add(categoryDraft2);
        return categoryDrafts;
    }

    public static CategoryDraft getMockCategoryDraft(@Nonnull final Locale locale,
                                                     @Nonnull final String name,
                                                     @Nonnull final String slug,
                                                     @Nullable final String externalId) {
        final CategoryDraft mockCategoryDraft = mock(CategoryDraft.class);
        when(mockCategoryDraft.getName()).thenReturn(LocalizedString.of(locale, name));
        when(mockCategoryDraft.getSlug()).thenReturn(LocalizedString.of(locale, slug));
        when(mockCategoryDraft.getExternalId()).thenReturn(externalId);
        when(mockCategoryDraft.getCustom()).thenReturn(getMockCustomFieldsDraft());
        return mockCategoryDraft;
    }

    public static CategorySync getMockCategorySync() {
        final CategorySyncOptions categorySyncOptions = getMockCategorySyncOptions();
        final CategorySyncStatistics categorySyncStatistics = new CategorySyncStatistics();

        final CategorySync categorySync = mock(CategorySync.class);
        when(categorySync.getSyncOptions()).thenReturn(categorySyncOptions);
        when(categorySync.getStatistics()).thenReturn(categorySyncStatistics);
        doCallRealMethod().when(categorySync).syncDrafts(any());
        doCallRealMethod().when(categorySync).createOrUpdateCategory(any());
        doCallRealMethod().when(categorySync).updateCategory(any(), any());

        return categorySync;
    }

    public static CategorySyncOptions getMockCategorySyncOptions() {
        final CategoryService categoryService = getMockCategoryService();
        final SphereClientConfig sphereClientConfig = SphereClientConfig
                .of("testProjectKey", "testCliendId", "testClientSecret");

        final CategorySyncOptions categorySyncOptions = mock(CategorySyncOptions.class);
        when(categorySyncOptions.getCategoryService()).thenReturn(categoryService);
        when(categorySyncOptions.getClientConfig()).thenReturn(sphereClientConfig);

        return categorySyncOptions;
    }

    public static CategoryService getMockCategoryService() {
        final Category category = getMockCategory(Locale.ENGLISH,
                "name",
                "slug",
                "externalId",
                "description",
                "metaDescription",
                "metaTitle",
                "metaKeywords",
                "orderHint",
                "parentId");

        final CategoryService categoryService = mock(CategoryService.class);
        when(categoryService.fetchCategoryByExternalId(anyString())).thenReturn(category);
        when(categoryService.updateCategory(any(), any())).thenReturn(category);
        when(categoryService.createCategory(any())).thenReturn(category);

        return categoryService;
    }
}
