package com.commercetools.sync.commons.asserts.statistics;

import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import javax.annotation.Nullable;

public final class CategorySyncStatisticsAssert
    extends AbstractSyncStatisticsAssert<CategorySyncStatisticsAssert, CategorySyncStatistics> {

  CategorySyncStatisticsAssert(@Nullable final CategorySyncStatistics actual) {
    super(actual, CategorySyncStatisticsAssert.class);
  }

  /**
   * Verifies that the actual {@link CategorySyncStatistics} value has identical statistics counters
   * as the ones supplied.
   *
   * @param processed the number of processed categories.
   * @param created the number of created categories.
   * @param updated the number of updated categories.
   * @param failed the number of failed categories.
   * @param numberOfCategoriesWithMissingParents the number of categories with missing parents.
   * @return {@code this} assertion object.
   * @throws AssertionError if the actual value is {@code null}.
   * @throws AssertionError if the actual statistics do not match the supplied values.
   */
  public CategorySyncStatisticsAssert hasValues(
      final int processed,
      final int created,
      final int updated,
      final int failed,
      final int numberOfCategoriesWithMissingParents) {
    super.hasValues(processed, created, updated, failed);
    org.assertj.core.api.Assertions.assertThat(actual.getNumberOfCategoriesWithMissingParents())
        .isEqualTo(numberOfCategoriesWithMissingParents);
    return myself;
  }
}
