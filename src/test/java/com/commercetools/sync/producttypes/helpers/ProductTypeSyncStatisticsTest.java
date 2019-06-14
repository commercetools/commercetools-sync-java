package com.commercetools.sync.producttypes.helpers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTypeSyncStatisticsTest {
    private ProductTypeSyncStatistics productTypeSyncStatistics;

    @BeforeEach
    void setup() {
        productTypeSyncStatistics = new ProductTypeSyncStatistics();
    }

    @Test
    void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
        productTypeSyncStatistics.incrementCreated(1);
        productTypeSyncStatistics.incrementFailed(2);
        productTypeSyncStatistics.incrementUpdated(3);
        productTypeSyncStatistics.incrementProcessed(6);

        assertThat(productTypeSyncStatistics.getReportMessage())
            .isEqualTo("Summary: 6 product types were processed in total "
                + "(1 created, 3 updated and 2 failed to sync).");
    }
}
