package com.commercetools.sync.customobjects.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomObjectSyncStatisticsTest {
  private CustomObjectSyncStatistics customObjectSyncStatistics;

  @BeforeEach
  void setup() {
    customObjectSyncStatistics = new CustomObjectSyncStatistics();
  }

  @Test
  void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
    customObjectSyncStatistics.incrementCreated(1);
    customObjectSyncStatistics.incrementFailed(2);
    customObjectSyncStatistics.incrementUpdated(3);
    customObjectSyncStatistics.incrementProcessed(6);

    assertThat(customObjectSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 6 custom objects were processed in total "
                + "(1 created, 3 updated and 2 failed to sync).");
  }

  @Test
  void getSyncStatisticsClassName_ShouldReturnCorrectClassName() {
    assertThat(customObjectSyncStatistics.getSyncStatisticsClassName())
        .isEqualTo("com.commercetools.sync.customobjects.helpers.CustomObjectSyncStatistics");
  }
}
