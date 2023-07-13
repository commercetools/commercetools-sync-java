package com.commercetools.sync.sdk2.cartdiscounts.helpers;

import com.commercetools.sync.sdk2.commons.helpers.BaseSyncStatistics;

public final class CartDiscountSyncStatistics extends BaseSyncStatistics {

  /**
   * Builds a summary of the cart discount sync statistics instance that looks like the following
   * example:
   *
   * <p>"Summary: 2 cart discounts were processed in total (0 created, 0 updated and 0 failed to
   * sync)."
   *
   * @return a summary message of the cart discount sync statistics instance.
   */
  @Override
  public String getReportMessage() {
    return getDefaultReportMessageForResource("cart discounts");
  }
}
