package com.commercetools.sync.sdk2.commons.statistics;

import com.commercetools.sync.sdk2.customers.helpers.CustomerSyncStatistics;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class AssertionsForStatistics {
  private AssertionsForStatistics() {}

  /**
   * Create assertion for {@link CustomerSyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static CustomerSyncStatisticsAssert assertThat(
      @Nullable final CustomerSyncStatistics statistics) {
    return new CustomerSyncStatisticsAssert(statistics);
  }
}
