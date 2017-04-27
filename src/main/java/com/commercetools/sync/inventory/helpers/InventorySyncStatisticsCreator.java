package com.commercetools.sync.inventory.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.commons.helpers.BaseSyncStatisticsCreator;

public final class InventorySyncStatisticsCreator extends BaseSyncStatisticsCreator {

    private int unprocessedDueToEmptySku = 0;

    /**
     * Increments the total number of resources unprocessed due to empty sku.
     */
    public void incrementUnprocessedDueToEmptySku() {
        this.unprocessedDueToEmptySku++;
    }

    /**
     *
     * @return new instance of {@link InventorySyncStatistics} fulfilled with data from {@code this}
     * {@link InventorySyncStatisticsCreator}
     */
    @Override
    public InventorySyncStatistics create() {
        BaseSyncStatistics baseSyncStatistics = super.create();
        return new InventorySyncStatistics(baseSyncStatistics, unprocessedDueToEmptySku);
    }
}
