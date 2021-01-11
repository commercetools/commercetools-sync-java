package com.commercetools.sync.customobjects.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

public class CustomObjectSyncStatistics extends BaseSyncStatistics {
  /**
   * Builds a summary of the custom object sync statistics instance that looks like the following
   * example:
   *
   * <p>"Summary: 2 custom objects were processed in total (0 created, 0 updated and 0 failed to
   * sync)."
   *
   * @return a summary message of the custom objects sync statistics instance.
   */
  @Override
  public String getReportMessage() {
    return getDefaultReportMessageForResource("custom objects");
  }
}
