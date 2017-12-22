package com.commercetools.sync.inventories.helpers;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InventorySyncStatisticsTest {

    private InventorySyncStatistics inventorySyncStatistics;

    @Before
    public void setup() {
        inventorySyncStatistics = new InventorySyncStatistics();
    }

    @Test
    public void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
        inventorySyncStatistics.incrementCreated(1);
        inventorySyncStatistics.incrementFailed(1);
        inventorySyncStatistics.incrementUpdated(1);
        inventorySyncStatistics.incrementProcessed(3);

        assertThat(inventorySyncStatistics.getReportMessage())
            .isEqualTo("Summary: 3 inventory entries were processed in total "
                + "(1 created, 1 updated and 1 failed to sync).");
    }
}
