package com.commercetools.sync.sdk2.commons.statistics;

import com.commercetools.sync.sdk2.customers.helpers.CustomerSyncStatistics;
import javax.annotation.Nullable;

public final class CustomerSyncStatisticsAssert
    extends AbstractSyncStatisticsAssert<CustomerSyncStatisticsAssert, CustomerSyncStatistics> {

  CustomerSyncStatisticsAssert(@Nullable final CustomerSyncStatistics actual) {
    super(actual, CustomerSyncStatisticsAssert.class);
  }
}
