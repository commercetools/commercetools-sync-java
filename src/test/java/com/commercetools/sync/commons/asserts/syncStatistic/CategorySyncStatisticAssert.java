package com.commercetools.sync.commons.asserts.syncStatistic;

import com.commercetools.sync.categories.helpers.CategorySyncStatistics;

import static org.assertj.core.api.Assertions.assertThat;

public class CategorySyncStatisticAssert
    extends AbstractSyncStatisticAssert<CategorySyncStatisticAssert, CategorySyncStatistics> {

    public CategorySyncStatisticAssert(final CategorySyncStatistics actual) {
        super(actual, CategorySyncStatisticAssert.class);
    }

    public static CategorySyncStatisticAssert assertThatStatistic(final CategorySyncStatistics actual) {
        return new CategorySyncStatisticAssert(actual);
    }

    public CategorySyncStatisticAssert hasValues(final int processed, final int created,
                                                 final int updated, final int failed,
                                                 final int numberOfCategoriesWithMissingParents) {
        super.hasValues(processed, created, updated, failed);
        assertThat(actual.getNumberOfCategoriesWithMissingParents()).isEqualTo(numberOfCategoriesWithMissingParents);
        return myself;
    }
}
