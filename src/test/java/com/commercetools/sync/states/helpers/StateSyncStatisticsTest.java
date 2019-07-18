package com.commercetools.sync.states.helpers;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class StateSyncStatisticsTest {

    @Test
    void getReportMessage_WithRandomStats_ShouldGetCorrectMessage() {
        Random random = new Random();

        int created = random.nextInt();
        int updated = random.nextInt();
        int failed = random.nextInt();
        int processed = created + updated + failed;

        StateSyncStatistics stateSyncStatistics = new StateSyncStatistics();
        stateSyncStatistics.incrementCreated(created);
        stateSyncStatistics.incrementUpdated(updated);
        stateSyncStatistics.incrementFailed(failed);
        stateSyncStatistics.incrementProcessed(processed);

        assertThat(stateSyncStatistics.getReportMessage())
            .isEqualTo("Summary: %s states were processed in total (%s created, "
                + "%s updated and %s failed to sync).", processed, created, updated, failed);
    }

}
