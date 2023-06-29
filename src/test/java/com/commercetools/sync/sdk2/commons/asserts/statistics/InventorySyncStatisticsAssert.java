package com.commercetools.sync.sdk2.commons.asserts.statistics;

import com.commercetools.sync.sdk2.inventories.helpers.InventorySyncStatistics;
import javax.annotation.Nullable;

public final class InventorySyncStatisticsAssert
    extends AbstractSyncStatisticsAssert<InventorySyncStatisticsAssert, InventorySyncStatistics> {

  InventorySyncStatisticsAssert(@Nullable final InventorySyncStatistics actual) {
    super(actual, InventorySyncStatisticsAssert.class);
  }
}
