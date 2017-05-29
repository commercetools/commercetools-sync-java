package com.commercetools.sync.categories.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatisticsBuilder;

public final class CategorySyncStatisticsBuilder extends BaseSyncStatisticsBuilder<CategorySyncStatisticsBuilder,
    CategorySyncStatistics> {

    /**
     * Returns new instance of {@link CategorySyncStatistics}, enriched with all attributes provided to {@code this}
     * builder.
     *
     * @return new instance of {@link CategorySyncStatistics}
     */
    @Override
    public CategorySyncStatistics build() {
        return new CategorySyncStatistics(processingTimeInMillis, created, updated, upToDate, failed);
    }

    /**
     * Returns {@code this} instance of {@link CategorySyncStatisticsBuilder}.
     *
     * <p><strong>Inherited doc:</strong><br/>{@inheritDoc}
     */
    @Override
    public CategorySyncStatisticsBuilder getThis() {
        return this;
    }
}
