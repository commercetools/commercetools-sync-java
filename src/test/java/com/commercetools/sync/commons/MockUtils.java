package com.commercetools.sync.commons;

import com.commercetools.sync.categories.CategorySyncOptions;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.LocalizedString;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockUtils {
    private static BlockingSphereClient ctpClient;
    private static final long timeout = 30;

    public static CategorySyncOptions getMockCategorySyncOptions() {
        return new CategorySyncOptions("hesham-baywa-testing-86",
                "l8rhXwMsYlal3eOJ_5tRF-3N", "hIJrCsyca5MgFMZQtsVKY2ai04px3Vow");
    }

    public static List<CategoryDraft> getMockCategoryDrafts() {
        List<CategoryDraft> categoryDrafts = new ArrayList<>();
        CategoryDraft categoryDraft1 = getMockCategoryDraft(Locale.GERMAN,"draft1", "slug1", "SH663881");
        CategoryDraft categoryDraft2 = getMockCategoryDraft(Locale.GERMAN,"draft2", "slug2", "nothing");
        categoryDrafts.add(categoryDraft1);
        categoryDrafts.add(categoryDraft2);
        return categoryDrafts;
    }

    public static CategoryDraft getMockCategoryDraft(@Nonnull final Locale locale,
                                                     @Nonnull final String name,
                                                     @Nonnull final String slug,
                                                     @Nonnull final String externalId) {
        CategoryDraft mockCategoryDraft = mock(CategoryDraft.class);
        when(mockCategoryDraft.getName()).thenReturn(LocalizedString.of(locale, name));
        when(mockCategoryDraft.getSlug()).thenReturn(LocalizedString.of(locale, slug));
        when(mockCategoryDraft.getExternalId()).thenReturn(externalId);
        return mockCategoryDraft;
    }
}
