package com.commercetools.sync.categories;


import com.commercetools.sync.categories.impl.CategorySyncImpl;
import io.sphere.sdk.categories.CategoryDraft;

import java.util.List;

import static com.commercetools.sync.commons.MockUtils.getMockCategoryDrafts;
import static com.commercetools.sync.commons.MockUtils.getMockCategorySyncOptions;

public class CategorySyncImplTest {

    private final static List<CategoryDraft> CATEGORY_DRAFTS = getMockCategoryDrafts();
    private final static CategorySyncOptions CATEGORY_SYNC_OPTIONS = getMockCategorySyncOptions();

    // TODO: TEMP! REMOVE.
    public void testSyncCategoryDrafts() {
        CategorySyncImpl categorySync = new CategorySyncImpl(CATEGORY_SYNC_OPTIONS);
        categorySync.syncCategoryDrafts(CATEGORY_DRAFTS);
    }


}
