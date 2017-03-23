package com.commercetools.sync.categories;


import com.commercetools.sync.categories.impl.CategorySyncImpl;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import io.sphere.sdk.categories.CategoryDraft;

import java.util.List;

import static com.commercetools.sync.commons.MockUtils.getMockCategoryDrafts;
import static com.commercetools.sync.commons.MockUtils.getMockCategorySyncOptions;

public class CategorySyncImplTest {

    private final static List<CategoryDraft> CATEGORY_DRAFTS = getMockCategoryDrafts();
    private final static CategorySyncOptions CATEGORY_SYNC_OPTIONS = getMockCategorySyncOptions();

    // TODO: TEMP! REMOVE.
    public void testSyncCategoryDrafts() {
        TypeService typeService = new TypeServiceImpl(CATEGORY_SYNC_OPTIONS.getCTPclient());
        CategoryService categoryService = new CategoryServiceImpl(CATEGORY_SYNC_OPTIONS.getCTPclient());
        CategorySyncImpl categorySync = new CategorySyncImpl(CATEGORY_SYNC_OPTIONS, typeService, categoryService);
        categorySync.syncCategoryDrafts(CATEGORY_DRAFTS);
    }


}
