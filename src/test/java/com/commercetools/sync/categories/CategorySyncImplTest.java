package com.commercetools.sync.categories;


import io.sphere.sdk.categories.CategoryDraft;
import org.junit.Test;

import java.util.List;

import static com.commercetools.sync.commons.MockUtils.getMockCategoryDrafts;
import static com.commercetools.sync.commons.MockUtils.getMockCategorySyncOptions;

public class CategorySyncImplTest {

    private final static List<CategoryDraft> CATEGORY_DRAFTS = getMockCategoryDrafts();
    private final static CategorySyncOptions CATEGORY_SYNC_OPTIONS = getMockCategorySyncOptions();

    @Test
    public void testSyncCategoryDrafts() {
        CategorySyncImpl categorySync = new CategorySyncImpl(CATEGORY_SYNC_OPTIONS);
        categorySync.syncCategoryDrafts(CATEGORY_DRAFTS);
    }


}
