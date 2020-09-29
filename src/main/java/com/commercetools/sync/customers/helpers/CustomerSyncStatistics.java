package com.commercetools.sync.customers.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import static java.lang.String.format;

public class CustomerSyncStatistics extends BaseSyncStatistics {

    public CustomerSyncStatistics() {
        super();
    }

    /**
     * Builds a summary of the customer sync statistics instance that looks like the following example:
     *
     * <p>"Summary: 2 customers have been processed in total (0 created, 0 updated and 0 failed to sync)."
     *
     * @return a summary message of the customer sync statistics instance.
     */
    @Override
    public String getReportMessage() {
        reportMessage = format(
            "Summary: %s customers have been processed in total (%s created, %s updated and %s failed to sync).",
            getProcessed(), getCreated(), getUpdated(), getFailed());
        return reportMessage;
    }
}
