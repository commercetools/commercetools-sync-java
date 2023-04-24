package com.commercetools.sync.sdk2.commons.asserts.statistics;

import com.commercetools.sync.sdk2.customers.helpers.CustomerSyncStatistics;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
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

  /**
   * Create assertion for {@link ProductSyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static ProductSyncStatisticsAssert assertThat(
      @Nullable final ProductSyncStatistics statistics) {
    return new ProductSyncStatisticsAssert(statistics);
  }
}