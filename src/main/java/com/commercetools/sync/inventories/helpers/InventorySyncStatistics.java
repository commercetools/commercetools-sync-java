package com.commercetools.sync.inventories.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import static java.lang.String.format;

public class InventorySyncStatistics extends BaseSyncStatistics {

    public InventorySyncStatistics() {
        super();
    }

    /**
     * Builds a summary of the inventory sync statistics instance that looks like the following example:
     * <pre>
     *     Summary: 25 inventory entries were processed in total (9 created, 5 updated, 2 failed to sync).
     * </pre>
     *
     * @return summary message
     */
    @Override
    public String getReportMessage() {
        reportMessage = format("Summary: %s inventory entries were processed in total "
                + "(%s created, %s updated and %s failed to sync).",
            getProcessed(), getCreated(), getUpdated(), getFailed());
        return reportMessage;
    }
}
