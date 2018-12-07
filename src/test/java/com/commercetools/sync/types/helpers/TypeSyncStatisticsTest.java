package com.commercetools.sync.types.helpers;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class TypeSyncStatisticsTest {
    private TypeSyncStatistics typeSyncStatistics;

    @Before
    public void setup() {
        typeSyncStatistics = new TypeSyncStatistics();
    }

    @Test
    public void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
        typeSyncStatistics.incrementCreated(1);
        typeSyncStatistics.incrementFailed(2);
        typeSyncStatistics.incrementUpdated(3);
        typeSyncStatistics.incrementProcessed(6);

        assertThat(typeSyncStatistics.getReportMessage())
            .isEqualTo("Summary: 6 types were processed in total "
                + "(1 created, 3 updated and 2 failed to sync).");
    }
}
