package com.commercetools.sync.commons.utils;


import io.sphere.sdk.categories.CategoryDraft;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraft;
import static com.commercetools.sync.commons.utils.BaseSyncUtils.batchDrafts;
import static org.assertj.core.api.Assertions.assertThat;

public class BaseSyncUtilsTest {

    @Test
    public void batchCategories_WithValidSize_ShouldReturnCorrectBatches() {
        final int numberOfCategoryDrafts = 160;
        final int batchSize = 10;
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        for (int i = 0; i < numberOfCategoryDrafts; i++) {
            categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key" + i, "parentKey",
                "customTypeId", new HashMap<>()));
        }
        final List<List<CategoryDraft>> batches = batchDrafts(categoryDrafts, 10);
        assertThat(batches.size()).isEqualTo(numberOfCategoryDrafts / batchSize);
    }

    @Test
    public void batchCategories_WithEmptyListAndAnySize_ShouldReturnNoBatches() {
        final List<List<CategoryDraft>> batches = batchDrafts(new ArrayList<>(), 100);
        assertThat(batches.size()).isEqualTo(0);
    }

    @Test
    public void batchCategories_WithNegativeSize_ShouldReturnNoBatches() {
        final int numberOfCategoryDrafts = 160;
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        for (int i = 0; i < numberOfCategoryDrafts; i++) {
            categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key" + i, "parentKey",
                "customTypeId", new HashMap<>()));
        }
        final List<List<CategoryDraft>> batches = batchDrafts(categoryDrafts, -100);
        assertThat(batches.size()).isEqualTo(0);
    }
}
