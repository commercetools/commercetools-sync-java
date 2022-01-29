package com.commercetools.sync.sdk2.customers.helpers;

import com.commercetools.sync.sdk2.commons.helpers.BaseSyncStatistics;

public class CustomerSyncStatistics extends BaseSyncStatistics {

  /**
   * Builds a summary of the customer sync statistics instance that looks like the following
   * example:
   *
   * <p>"Summary: 2 customers have been processed in total (0 created, 0 updated and 0 failed to
   * sync)."
   *
   * @return a summary message of the customer sync statistics instance.
   */
  @Override
  public String getReportMessage() {
    return getDefaultReportMessageForResource("customers");
  }
}
