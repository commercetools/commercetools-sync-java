package com.commercetools.sync.inventories.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatisticsBuilder;

public final class InventorySyncStatisticsBuilder extends BaseSyncStatisticsBuilder<InventorySyncStatisticsBuilder,
    InventorySyncStatistics> {

    /**
     * Returns new instance of {@link InventorySyncStatistics}, enriched with all attributes provided to {@code this}
     * builder.
     *
     * @return new instance of {@link InventorySyncStatistics}
     */
    @Override
    public InventorySyncStatistics build() {
        return new InventorySyncStatistics(processingTimeInMillis, created, updated, upToDate, failed);
    }

    /**
     * Returns {@code this} instance of {@link InventorySyncStatisticsBuilder}.
     *
     * <p><strong>Inherited doc:</strong><br/>{@inheritDoc}
     */
    @Override
    public InventorySyncStatisticsBuilder getThis() {
        return this;
    }
}
