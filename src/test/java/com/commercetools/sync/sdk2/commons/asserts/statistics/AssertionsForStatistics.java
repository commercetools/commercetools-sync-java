package com.commercetools.sync.sdk2.commons.asserts.statistics;

import com.commercetools.sync.sdk2.cartdiscounts.helpers.CartDiscountSyncStatistics;
import com.commercetools.sync.sdk2.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.sdk2.customers.helpers.CustomerSyncStatistics;
import com.commercetools.sync.sdk2.customobjects.helpers.CustomObjectSyncStatistics;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.sdk2.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.sdk2.states.helpers.StateSyncStatistics;
import com.commercetools.sync.sdk2.types.helpers.TypeSyncStatistics;
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

  /**
   * Create assertion for {@link CategorySyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static CategorySyncStatisticsAssert assertThat(
      @Nullable final CategorySyncStatistics statistics) {
    return new CategorySyncStatisticsAssert(statistics);
  }

  /**
   * Create assertion for {@link CustomObjectSyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static CustomObjectSyncStatisticsAssert assertThat(
      @Nullable final CustomObjectSyncStatistics statistics) {
    return new CustomObjectSyncStatisticsAssert(statistics);
  }

  /**
   * Create assertion for {@link ProductTypeSyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static ProductTypeSyncStatisticsAssert assertThat(
      @Nullable final ProductTypeSyncStatistics statistics) {
    return new ProductTypeSyncStatisticsAssert(statistics);
  }

  /**
   * Create assertion for {@link TypeSyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static TypeSyncStatisticsAssert assertThat(@Nullable final TypeSyncStatistics statistics) {
    return new TypeSyncStatisticsAssert(statistics);
  }

  @Nonnull
  public static CartDiscountSyncStatisticsAssert assertThat(
      @Nullable final CartDiscountSyncStatistics statistics) {
    return new CartDiscountSyncStatisticsAssert(statistics);
  }

  /**
   * Create assertion for {@link StateSyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static StateSyncStatisticsAssert assertThat(
      @Nullable final StateSyncStatistics statistics) {
    return new StateSyncStatisticsAssert(statistics);
  }
}
