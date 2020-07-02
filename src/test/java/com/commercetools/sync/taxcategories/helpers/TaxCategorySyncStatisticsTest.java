package com.commercetools.sync.taxcategories.helpers;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class TaxCategorySyncStatisticsTest {

    @Test
    void getReportMessage_WithRandomStats_ShouldGetCorrectMessage() {
        Random random = new Random();

        int created = random.nextInt();
        int updated = random.nextInt();
        int failed = random.nextInt();
        int processed = created + updated + failed;

        TaxCategorySyncStatistics statistics = new TaxCategorySyncStatistics();
        statistics.incrementCreated(created);
        statistics.incrementUpdated(updated);
        statistics.incrementFailed(failed);
        statistics.incrementProcessed(processed);

        assertThat(statistics.getReportMessage()).isEqualTo("Summary: %s tax categories were processed in "
            + "total (%s created, %s updated and %s failed to sync).", processed, created, updated, failed);
    }

}
