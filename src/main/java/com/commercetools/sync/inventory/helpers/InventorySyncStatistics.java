package com.commercetools.sync.inventory.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

/**
 * Statistics of inventory synchronisation process
 */
public class InventorySyncStatistics extends BaseSyncStatistics {

    private int unprocessedDueToEmptySku;

    public InventorySyncStatistics() {
        super();
    }

    /**
     * Get the total number of resources that weren't processed because they had empty sku.
     *
     * @return total number of resources unprocessed due to empty sku.
     */
    public int getUnprocessedDueToEmptySku() {
        return unprocessedDueToEmptySku;
    }

    /**
     * Increment the total number of resources that weren't processed because they had empty sku.
     */
    public void incrementUnprocessedDueToEmptySku() {
        this.unprocessedDueToEmptySku++;
    }
}
