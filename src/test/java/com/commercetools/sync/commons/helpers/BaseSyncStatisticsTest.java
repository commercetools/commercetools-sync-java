package com.commercetools.sync.commons.helpers;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.customers.helpers.CustomerSyncStatistics;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BaseSyncStatisticsTest {
  private BaseSyncStatistics baseSyncStatistics;

  @BeforeEach
  void setup() {
    baseSyncStatistics = new CustomerSyncStatistics();
  }

  @Test
  void getUpdated_WithNoUpdated_ShouldReturnZero() {
    assertThat(baseSyncStatistics.getUpdated()).hasValue(0);
  }

  @Test
  void incrementUpdated_WithNoSpecifiedTimes_ShouldIncrementUpdatedValue() {
    baseSyncStatistics.incrementUpdated();
    assertThat(baseSyncStatistics.getUpdated()).hasValue(1);
  }

  @Test
  void incrementUpdated_WithSpecifiedTimes_ShouldIncrementUpdatedValue() {
    baseSyncStatistics.incrementUpdated(5);
    assertThat(baseSyncStatistics.getUpdated()).hasValue(5);
  }

  @Test
  void getCreated_WithNoCreated_ShouldReturnZero() {
    assertThat(baseSyncStatistics.getCreated()).hasValue(0);
  }

  @Test
  void incrementCreated_WithNoSpecifiedTimes_ShouldIncrementCreatedValue() {
    baseSyncStatistics.incrementCreated();
    assertThat(baseSyncStatistics.getCreated()).hasValue(1);
  }

  @Test
  void incrementCreated_WithSpecifiedTimes_ShouldIncrementCreatedValue() {
    baseSyncStatistics.incrementCreated(2);
    assertThat(baseSyncStatistics.getCreated()).hasValue(2);
  }

  @Test
  void getProcessed_WithNoProcessed_ShouldReturnZero() {
    assertThat(baseSyncStatistics.getProcessed()).hasValue(0);
  }

  @Test
  void incrementProcessed_WithNoSpecifiedTimes_ShouldIncrementProcessedValue() {
    baseSyncStatistics.incrementProcessed();
    assertThat(baseSyncStatistics.getProcessed()).hasValue(1);
  }

  @Test
  void incrementProcessed_WithSpecifiedTimes_ShouldIncrementProcessedValue() {
    baseSyncStatistics.incrementProcessed(2);
    assertThat(baseSyncStatistics.getProcessed()).hasValue(2);
  }

  @Test
  void getFailed_WithNoFailed_ShouldReturnZero() {
    assertThat(baseSyncStatistics.getFailed()).hasValue(0);
  }

  @Test
  void incrementFailed_WithNoSpecifiedTimes_ShouldIncrementFailedValue() {
    baseSyncStatistics.incrementFailed();
    assertThat(baseSyncStatistics.getFailed()).hasValue(1);
  }

  @Test
  void incrementFailed_WithSpecifiedTimes_ShouldIncrementFailedValue() {
    baseSyncStatistics.incrementFailed(3);
    assertThat(baseSyncStatistics.getFailed()).hasValue(3);
  }

  @Test
  void calculateProcessingTime_ShouldSetProcessingTimeInAllUnitsAndHumanReadableString()
      throws InterruptedException {
    assertThat(baseSyncStatistics.getLatestBatchProcessingTimeInMillis()).isEqualTo(0);
    assertThat(baseSyncStatistics.getLatestBatchHumanReadableProcessingTime()).isEqualTo("");

    final int waitingTimeInMillis = 100;
    Thread.sleep(waitingTimeInMillis);
    baseSyncStatistics.calculateProcessingTime();

    assertThat(baseSyncStatistics.getLatestBatchProcessingTimeInDays()).isGreaterThanOrEqualTo(0);
    assertThat(baseSyncStatistics.getLatestBatchProcessingTimeInHours()).isGreaterThanOrEqualTo(0);
    assertThat(baseSyncStatistics.getLatestBatchProcessingTimeInMinutes())
        .isGreaterThanOrEqualTo(0);
    assertThat(baseSyncStatistics.getLatestBatchProcessingTimeInSeconds())
        .isGreaterThanOrEqualTo(waitingTimeInMillis / 1000);
    assertThat(baseSyncStatistics.getLatestBatchProcessingTimeInMillis())
        .isGreaterThanOrEqualTo(waitingTimeInMillis);

    final long remainingMillis =
        baseSyncStatistics.getLatestBatchProcessingTimeInMillis()
            - TimeUnit.SECONDS.toMillis(baseSyncStatistics.getLatestBatchProcessingTimeInSeconds());
    assertThat(baseSyncStatistics.getLatestBatchHumanReadableProcessingTime())
        .contains(format(", %dms", remainingMillis));
  }

  @Test
  void getDefaultReportMessageForResource_withResourceString_ShouldBuildCorrectSummary() {
    String message = baseSyncStatistics.getDefaultReportMessageForResource("resources");
    assertThat(message)
        .isEqualTo(
            "Summary: 0 resources were processed in total (0 created, 0 updated and 0 "
                + "failed to sync).");
  }

  @Test
  void twoSyncsStatistics_withSameProperties_ShouldBeEqual() {
    final CustomerSyncStatistics customerSyncStatistics = new CustomerSyncStatistics();
    final CustomerSyncStatistics customerSyncStatisticsIdentical = new CustomerSyncStatistics();

    assertThat(customerSyncStatistics).isEqualTo(customerSyncStatisticsIdentical);
  }
}
