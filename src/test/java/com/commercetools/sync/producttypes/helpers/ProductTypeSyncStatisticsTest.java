package com.commercetools.sync.producttypes.helpers;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductTypeSyncStatisticsTest {
    private ProductTypeSyncStatistics productTypeSyncStatistics;

    @Before
    public void setup() {
        productTypeSyncStatistics = new ProductTypeSyncStatistics();
    }

    @Test
    public void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
        productTypeSyncStatistics.incrementCreated(1);
        productTypeSyncStatistics.incrementFailed(1);
        productTypeSyncStatistics.incrementUpdated(1);
        productTypeSyncStatistics.incrementProcessed(3);

        assertThat(productTypeSyncStatistics.getReportMessage())
            .isEqualTo("Summary: 3 product types were processed in total "
                + "(1 created, 1 updated and 1 failed to sync).");
    }
}
