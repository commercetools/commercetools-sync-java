package com.commercetools.sync.inventories.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventorySyncStatisticsTest {

  private InventorySyncStatistics inventorySyncStatistics;

  @BeforeEach
  void setup() {
    inventorySyncStatistics = new InventorySyncStatistics();
  }

  @Test
  void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
    inventorySyncStatistics.incrementCreated(1);
    inventorySyncStatistics.incrementFailed(1);
    inventorySyncStatistics.incrementUpdated(1);
    inventorySyncStatistics.incrementProcessed(3);

    assertThat(inventorySyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 3 inventory entries were processed in total "
                + "(1 created, 1 updated and 1 failed to sync).");
  }

  @Test
  void getSyncStatisticsClassName_ShouldReturnCorrectClassName() {
    assertThat(inventorySyncStatistics.getSyncStatisticsClassName())
        .isEqualTo("com.commercetools.sync.inventories.helpers.InventorySyncStatistics");
  }
}
