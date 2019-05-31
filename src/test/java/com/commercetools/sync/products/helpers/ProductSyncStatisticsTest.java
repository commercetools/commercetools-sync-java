package com.commercetools.sync.products.helpers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSyncStatisticsTest {
    private ProductSyncStatistics productSyncStatistics;

    @BeforeEach
    void setup() {
        productSyncStatistics = new ProductSyncStatistics();
    }

    @Test
    void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
        productSyncStatistics.incrementCreated(1);
        productSyncStatistics.incrementFailed(1);
        productSyncStatistics.incrementUpdated(1);
        productSyncStatistics.incrementProcessed(3);

        assertThat(productSyncStatistics.getReportMessage()).isEqualTo("Summary: 3 products were processed in total "
            + "(1 created, 1 updated and 1 failed to sync).");
    }
}
