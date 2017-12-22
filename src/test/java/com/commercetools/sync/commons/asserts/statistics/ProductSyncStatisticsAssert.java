package com.commercetools.sync.commons.asserts.syncStatistic;

import com.commercetools.sync.products.helpers.ProductSyncStatistics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ProductSyncStatisticsAssert
    extends AbstractSyncStatisticsAssert<ProductSyncStatisticsAssert, ProductSyncStatistics> {

    private ProductSyncStatisticsAssert(@Nullable final ProductSyncStatistics actual) {
        super(actual, ProductSyncStatisticsAssert.class);
    }

    /**
     * Create assertion for {@link ProductSyncStatistics}.
     *
     * @param statistics the actual value.
     * @return the created assertion object.
     */
    @Nonnull
    public static ProductSyncStatisticsAssert assertThat(@Nullable final ProductSyncStatistics statistics) {
        return new ProductSyncStatisticsAssert(statistics);
    }
}
