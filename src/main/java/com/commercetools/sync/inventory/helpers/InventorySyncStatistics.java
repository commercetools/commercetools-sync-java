package com.commercetools.sync.inventory.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import javax.annotation.Nonnull;

import static java.lang.String.format;

/**
 * Statistics of inventory synchronisation process. Class is immutable.
 */
public class InventorySyncStatistics extends BaseSyncStatistics {

    private int unprocessedDueToEmptySku;

    InventorySyncStatistics(@Nonnull BaseSyncStatistics baseSyncStatistics, int unprocessedDueToEmptySku) {
        super(baseSyncStatistics);
        this.unprocessedDueToEmptySku = unprocessedDueToEmptySku;
    }

    /**
     * Returns new {@link InventorySyncStatistics} instance that contains statistics values summed up from
     * {@code statistics1} and {@code statistics2}
     *
     * @param statistics1 first element
     * @param statistics2 second element
     * @return new {@link InventorySyncStatistics} instance that contains summed up statistics
     */
    public static InventorySyncStatistics merge(@Nonnull final InventorySyncStatistics statistics1,
                                                @Nonnull final InventorySyncStatistics statistics2) {
        return new InventorySyncStatistics(BaseSyncStatistics.merge(statistics1, statistics2),
                statistics1.getUnprocessedDueToEmptySku() + statistics2.getUnprocessedDueToEmptySku());
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
