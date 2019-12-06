package com.commercetools.sync.types.helpers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class TypeSyncStatisticsTest {
    private TypeSyncStatistics typeSyncStatistics;

    @BeforeEach
    void setup() {
        typeSyncStatistics = new TypeSyncStatistics();
    }

    @Test
    void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
        typeSyncStatistics.incrementCreated(1);
        typeSyncStatistics.incrementFailed(2);
        typeSyncStatistics.incrementUpdated(3);
        typeSyncStatistics.incrementProcessed(6);

        assertThat(typeSyncStatistics.getReportMessage())
            .isEqualTo("Summary: 6 types were processed in total "
                + "(1 created, 3 updated and 2 failed to sync).");
    }
}
