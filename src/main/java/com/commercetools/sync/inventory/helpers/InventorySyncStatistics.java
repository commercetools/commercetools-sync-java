package com.commercetools.sync.inventory.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import static java.lang.String.format;

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

    /**
     * Builds a summary of the inventory sync statistics instance that looks like the following example:
     * <pre>
     *     Summary of inventory synchronisation:
     *     2 inventory entries weren't processed due to empty sku.
     *     25 inventory entries were processed in total (9 created, 5 updated, 2 failed to sync).
     * </pre>
     * <strong>Note:</strong> second line is present only when there are any unprocessed entries.
     *
     * @return summary message
     */
    @Override
    public String getReportMessage() {
        String report = "\nSummary of inventory synchronisation:\n";
        if (unprocessedDueToEmptySku > 0) {
            report += format("%d inventory entries weren't processed due to empty sku\n", unprocessedDueToEmptySku);
        }
        report += format("%d inventory entries were processed in total (%d created, %d updated, %d failed to sync\n\n",
                getProcessed(), getCreated(), getUpdated(), getFailed());
        return report;
    }
}
