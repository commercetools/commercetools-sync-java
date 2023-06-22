package com.commercetools.sync.sdk2.types.helpers;

import com.commercetools.sync.sdk2.commons.helpers.BaseSyncStatistics;

public class TypeSyncStatistics extends BaseSyncStatistics {
  /**
   * Builds a summary of the type sync statistics instance that looks like the following example:
   *
   * <p>"Summary: 2 types were processed in total (0 created, 0 updated and 0 failed to sync)."
   *
   * @return a summary message of the types sync statistics instance.
   */
  @Override
  public String getReportMessage() {
    return getDefaultReportMessageForResource("types");
  }
}
