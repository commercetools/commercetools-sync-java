package com.commercetools.sync.customers.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerSyncStatisticsTest {
  private CustomerSyncStatistics customerSyncStatistics;

  @BeforeEach
  void setup() {
    customerSyncStatistics = new CustomerSyncStatistics();
  }

  @Test
  void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
    customerSyncStatistics.incrementCreated(1);
    customerSyncStatistics.incrementFailed(2);
    customerSyncStatistics.incrementUpdated(3);
    customerSyncStatistics.incrementProcessed(6);

    assertThat(customerSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 6 customers were processed in total "
                + "(1 created, 3 updated and 2 failed to sync).");
  }
}
