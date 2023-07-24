package com.commercetools.sync.sdk2.shoppinglists.helpers;

import com.commercetools.sync.sdk2.commons.helpers.BaseSyncStatistics;

public final class ShoppingListSyncStatistics extends BaseSyncStatistics {

  /**
   * Builds a summary of the shopping list sync statistics instance that looks like the following
   * example:
   *
   * <p>"Summary: 2 shopping lists were processed in total (0 created, 0 updated and 0 failed to
   * sync)."
   *
   * @return a summary message of the shopping list sync statistics instance.
   */
  @Override
  public String getReportMessage() {
    return getDefaultReportMessageForResource("shopping lists");
  }
}
