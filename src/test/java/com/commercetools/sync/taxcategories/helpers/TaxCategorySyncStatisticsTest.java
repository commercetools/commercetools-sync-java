package com.commercetools.sync.taxcategories.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TaxCategorySyncStatisticsTest {

  static final Random random = new SecureRandom();

  @Test
  void getReportMessage_WithRandomStats_ShouldGetCorrectMessage() {
    int created = random.nextInt();
    int updated = random.nextInt();
    int failed = random.nextInt();
    int processed = created + updated + failed;

    TaxCategorySyncStatistics statistics = new TaxCategorySyncStatistics();
    statistics.incrementCreated(created);
    statistics.incrementUpdated(updated);
    statistics.incrementFailed(failed);
    statistics.incrementProcessed(processed);

    assertThat(statistics.getReportMessage())
        .isEqualTo(
            "Summary: %s tax categories were processed in "
                + "total (%s created, %s updated and %s failed to sync).",
            processed, created, updated, failed);
  }

  @Test
  void getSyncStatisticsClassName_ShouldReturnCorrectClassName() {
    assertThat(new TaxCategorySyncStatistics().getSyncStatisticsClassName())
        .isEqualTo("com.commercetools.sync.taxcategories.helpers.TaxCategorySyncStatistics");
  }
}
