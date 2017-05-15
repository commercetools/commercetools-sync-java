package com.commercetools.sync.inventories.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import static java.lang.String.format;

/**
 * Statistics of inventory synchronisation process.
 */
public class InventorySyncStatistics extends BaseSyncStatistics {

    public InventorySyncStatistics() {
        super();
    }

    /**
     * Builds a summary of the inventory sync statistics instance that looks like the following example:
     * <pre>
     *     Summary of inventory synchronisation:
     *     25 inventory entries were processed in total (9 created, 5 updated, 2 failed to sync).
     * </pre>
     * <strong>Note:</strong> second line is present only when there are any unprocessed entries.
     *
     * @return summary message
     */
    @Override
    public String getReportMessage() {
        return format("%nSummary of inventory synchronisation:%n%d inventory entries were processed in total "
                        + "(%d created, %d updated, %d failed to sync)%n",
                getProcessed(), getCreated(), getUpdated(), getFailed());
    }
}
