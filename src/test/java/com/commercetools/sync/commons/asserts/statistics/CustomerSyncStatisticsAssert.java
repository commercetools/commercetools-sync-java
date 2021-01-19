package com.commercetools.sync.commons.asserts.statistics;

import com.commercetools.sync.customers.helpers.CustomerSyncStatistics;
import javax.annotation.Nullable;

public final class CustomerSyncStatisticsAssert
    extends AbstractSyncStatisticsAssert<CustomerSyncStatisticsAssert, CustomerSyncStatistics> {

  CustomerSyncStatisticsAssert(@Nullable final CustomerSyncStatistics actual) {
    super(actual, CustomerSyncStatisticsAssert.class);
  }
}
