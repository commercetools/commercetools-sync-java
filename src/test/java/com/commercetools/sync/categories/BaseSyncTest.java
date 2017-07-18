package com.commercetools.sync.categories;

import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraft;
import static com.commercetools.sync.commons.MockUtils.getMockCategoryService;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BaseSyncTest {

    private List<String> errorCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Initializes instances of  {@link CategorySyncOptions} and {@link CategorySync} which will be used by some
     * of the unit test methods in this test class.
     */
    @Before
    public void setup() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
    }

    @Test
    public void sync_WithBatchSizeSet_ShouldCallSyncOnEachBatch() {
        final int batchSize = 1;
        CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
            .of(mock(SphereClient.class))
            .setErrorCallBack(
                (errorMessage, exception) -> {
                    errorCallBackMessages.add(errorMessage);
                    errorCallBackExceptions.add(exception);
                })
            .setBatchSize(batchSize)
            .build();

        final CategorySync categorySync =
            new CategorySync(categorySyncOptions, getMockTypeService(), getMockCategoryService());

        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        final int numberOfCategoryDrafts = 160;
        for (int i = 0; i < numberOfCategoryDrafts; i++) {
            categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key" + i, "parentKey",
                "customTypeId", new HashMap<>()));
        }

        final CategorySync mockCategorySync = spy(categorySync);

        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(categoryDrafts)
                                                                      .toCompletableFuture().join();

        int expectedNumberOfCalls = (int) (Math.ceil(numberOfCategoryDrafts / batchSize) + 1);
        verify(mockCategorySync, times(expectedNumberOfCalls)).syncBatches(any(), any());

        int expectedNumberOfCategoriesCreated = expectedNumberOfCalls - 1;
        assertThat(syncStatistics.getCreated()).isEqualTo(expectedNumberOfCategoriesCreated);
        assertThat(syncStatistics.getFailed()).isEqualTo(0);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(categoryDrafts.size());
        assertThat(syncStatistics.getReportMessage()).isEqualTo(format("Summary: %s categories were processed"
                + " in total (%s created, 0 updated and 0 categories failed to sync).", categoryDrafts.size(),
            expectedNumberOfCategoriesCreated));
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);

        // With Default batch size

        categorySyncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                        .setErrorCallBack((errorMessage, exception) -> {
                                                            errorCallBackMessages.add(errorMessage);
                                                            errorCallBackExceptions.add(exception);
                                                        })
                                                        .build();

        final CategorySync categorySyncWithDefaultBatchSize = new CategorySync(categorySyncOptions,
            getMockTypeService(), getMockCategoryService());
        final CategorySync mockCategorySyncWithDefaultBatchSize = spy(categorySyncWithDefaultBatchSize);

        final CategorySyncStatistics syncStatisticsWithDefaultBatchSize = mockCategorySyncWithDefaultBatchSize
            .sync(categoryDrafts)
            .toCompletableFuture().join();

        expectedNumberOfCalls =
            (int) (Math.ceil(numberOfCategoryDrafts / (double) CategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT) + 1);

        verify(mockCategorySyncWithDefaultBatchSize, times(expectedNumberOfCalls)).syncBatches(any(), any());


        expectedNumberOfCategoriesCreated = expectedNumberOfCalls - 1;
        final int expectedFailedCategories = categoryDrafts.size() - (expectedNumberOfCategoriesCreated);

        assertThat(syncStatisticsWithDefaultBatchSize.getCreated()).isEqualTo(expectedNumberOfCategoriesCreated);
        assertThat(syncStatisticsWithDefaultBatchSize.getFailed()).isEqualTo(expectedFailedCategories);
        assertThat(syncStatisticsWithDefaultBatchSize.getUpdated()).isEqualTo(0);
        assertThat(syncStatisticsWithDefaultBatchSize.getProcessed()).isEqualTo(categoryDrafts.size());
        assertThat(syncStatisticsWithDefaultBatchSize.getReportMessage())
            .isEqualTo(format("Summary: %s categories were processed in total (%s created, 0 updated and %s categories"
                    + " failed to sync).", categoryDrafts.size(), expectedNumberOfCategoriesCreated,
                expectedFailedCategories));
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }
}
