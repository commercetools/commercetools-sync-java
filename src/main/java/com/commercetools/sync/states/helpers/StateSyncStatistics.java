package com.commercetools.sync.states.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import static java.lang.String.format;

/**
 * States sync statistics.
 * Keeps track of processed, created, updated and failed states through whole sync process.
 */
public final class StateSyncStatistics extends BaseSyncStatistics {

    /**
     * Builds a summary of the state sync statistics instance that looks like the following example:
     *
     * <p>"Summary: 2 states were processed in total (0 created, 0 updated and 0 failed to sync)."
     *
     * @return a summary message of the states sync statistics instance.
     */
    @Override
    public String getReportMessage() {
        reportMessage = format(
            "Summary: %s states were processed in total (%s created, %s updated and %s failed to sync).",
            getProcessed(), getCreated(), getUpdated(), getFailed());
        return reportMessage;
    }

}
