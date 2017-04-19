package com.commercetools.sync.categories;


import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.products.ProductVariantDraft;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Locale;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraft;
import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategorySync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategorySyncTest {
    @Test
    public void syncDrafts_WithEmptyListOfDrafts_ShouldNotProcessAnyCategories() {
        final CategorySync categorySync = getMockCategorySync();
        categorySync.syncDrafts(new ArrayList<>());

        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo("Summary: 0 categories were processed in total " +
                "(0 created, 0 updated and 0 categories failed to sync).");
        assertThat(categorySync.getSummary()).contains("Summary: 0 categories were processed in total " +
                "(0 created, 0 updated and 0 categories failed to sync).");
    }

    @Test
    public void syncDrafts_WithAWrongListOfDrafts_ShouldFailSync() {
        final CategorySync categorySync = getMockCategorySync();
        final ArrayList<ProductVariantDraft> drafts = new ArrayList<>();
        drafts.add(mock(ProductVariantDraft.class));

        categorySync.syncDrafts(drafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo("Summary: 0 categories were processed in total " +
                "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(categorySync.getSummary()).contains("Summary: 0 categories were processed in total " +
                "(0 created, 0 updated and 1 categories failed to sync).");
    }

    @Test
    public void syncDrafts_WithANullDraft_ShouldSkipIt() {
        final CategorySync categorySync = getMockCategorySync();
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(null);

        categorySync.syncDrafts(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo("Summary: 0 categories were processed in total " +
                "(0 created, 0 updated and 0 categories failed to sync).");
        assertThat(categorySync.getSummary()).contains("Summary: 0 categories were processed in total " +
                "(0 created, 0 updated and 0 categories failed to sync).");
    }

    @Test
    public void syncDrafts_WithADraftWithNoSetExternalID_ShouldFailSync() {
        final CategorySync categorySync = getMockCategorySync();
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH,
                "noExternalIdDraft",
                "no-external-id-draft",
                null));

        categorySync.syncDrafts(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo("Summary: 1 categories were processed in total " +
                "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(categorySync.getSummary()).contains("Summary: 1 categories were processed in total " +
                "(0 created, 0 updated and 1 categories failed to sync).");
    }

    @Test
    public void syncDrafts_WithNoExistingCategory_ShouldCreateCategory() {
        final CategorySync categorySync = getMockCategorySync();
        when(categorySync.getSyncOptions().getCategoryService().fetchCategoryByExternalId(anyString())).thenReturn(null);


        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH,
                "name",
                "slug",
                "newExternalId"));


        categorySync.syncDrafts(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo("Summary: 1 categories were processed in total " +
                "(1 created, 0 updated and 0 categories failed to sync).");
        assertThat(categorySync.getSummary()).contains("Summary: 1 categories were processed in total " +
                "(1 created, 0 updated and 0 categories failed to sync).");
    }

    @Test
    public void syncDrafts_WithExistingCategory_ShouldUpdateCategory() {
        final CategorySync categorySync = getMockCategorySync();
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH,
                "name",
                "slug",
                "externalId"));


        categorySync.syncDrafts(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo("Summary: 1 categories were processed in total " +
                "(0 created, 1 updated and 0 categories failed to sync).");
        assertThat(categorySync.getSummary()).contains("Summary: 1 categories were processed in total " +
                "(0 created, 1 updated and 0 categories failed to sync).");
    }


}
