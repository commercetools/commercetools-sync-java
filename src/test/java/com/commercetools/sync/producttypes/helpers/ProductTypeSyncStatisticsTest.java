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
        productTypeSyncStatistics.incrementFailed(2);
        productTypeSyncStatistics.incrementUpdated(3);
        productTypeSyncStatistics.incrementProcessed(6);

        assertThat(productTypeSyncStatistics.getReportMessage())
            .isEqualTo("Summary: 6 product types were processed in total "
                + "(1 created, 3 updated and 2 failed to sync).");
    }
}
