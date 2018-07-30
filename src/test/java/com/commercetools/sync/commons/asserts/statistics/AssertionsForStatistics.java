package com.commercetools.sync.commons.asserts.statistics;

import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class AssertionsForStatistics {
    private AssertionsForStatistics() {
    }

    /**
     * Create assertion for {@link CategorySyncStatistics}.
     *
     * @param statistics the actual value.
     * @return the created assertion object.
     */
    @Nonnull
    public static CategorySyncStatisticsAssert assertThat(@Nullable final CategorySyncStatistics statistics) {
        return new CategorySyncStatisticsAssert(statistics);
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

    /**
     * Create assertion for {@link InventorySyncStatistics}.
     *
     * @param statistics the actual value.
     * @return the created assertion object.
     */
    @Nonnull
    public static InventorySyncStatisticsAssert assertThat(@Nullable final InventorySyncStatistics statistics) {
        return new InventorySyncStatisticsAssert(statistics);
    }

    /**
     * Create assertion for {@link ProductTypeSyncStatistics}.
     *
     * @param statistics the actual value.
     * @return the created assertion object.
     */
    @Nonnull
    public static ProductTypeSyncStatisticsAssert assertThat(@Nullable final ProductTypeSyncStatistics statistics) {
        return new ProductTypeSyncStatisticsAssert(statistics);
    }
}
