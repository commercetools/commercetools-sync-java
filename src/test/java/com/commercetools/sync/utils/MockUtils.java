package com.commercetools.sync.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.LocalizedString;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MockUtils {
    private static BlockingSphereClient ctpClient;
    private static final long timeout = 30;

    public static CategorySyncOptions getMockCategorySyncOptions() {
        return new CategorySyncOptions("hesham-baywa-testing-86",
                "l8rhXwMsYlal3eOJ_5tRF-3N", "hIJrCsyca5MgFMZQtsVKY2ai04px3Vow");
    }

    public static List<CategoryDraft> getMockCategoryDrafts() {
        List<CategoryDraft> categoryDrafts = new ArrayList<>();
        CategoryDraft categoryDraft1 = getMockCategoryDraft("draft1", "slug1", Locale.GERMAN, "SH663881");
        CategoryDraft categoryDraft2 = getMockCategoryDraft("draft2", "slug2", Locale.GERMAN, "nothing");
        categoryDrafts.add(categoryDraft1);
        categoryDrafts.add(categoryDraft2);
        return categoryDrafts;
    }

    public static CategoryDraft getMockCategoryDraft(String name, String slug, Locale locale, String externalId) {
        CategoryDraft categoryDraft = CategoryDraftBuilder.of(
                LocalizedString.of(locale, name),
                LocalizedString.of(locale, slug))
                .externalId(externalId)
                .build();
        return categoryDraft;
    }
}
