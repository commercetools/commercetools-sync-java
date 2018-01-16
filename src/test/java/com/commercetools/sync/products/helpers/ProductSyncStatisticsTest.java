package com.commercetools.sync.products.helpers;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductSyncStatisticsTest {
    private ProductSyncStatistics productSyncStatistics;

    @Before
    public void setup() {
        productSyncStatistics = new ProductSyncStatistics();
    }

    @Test
    public void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
        productSyncStatistics.incrementCreated(1);
        productSyncStatistics.incrementFailed(1);
        productSyncStatistics.incrementUpdated(1);
        productSyncStatistics.incrementProcessed(3);

        assertThat(productSyncStatistics.getReportMessage()).isEqualTo("Summary: 3 products were processed in total "
            + "(1 created, 1 updated and 1 failed to sync).");
    }
}
