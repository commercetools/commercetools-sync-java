package com.commercetools.sync.taxcategories.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import static java.lang.String.format;

/**
 * Tax category sync statistics.
 * Keeps track of processed, created, updated and failed states through whole sync process.
 */
public final class TaxCategorySyncStatistics extends BaseSyncStatistics {

    /**
     * Builds a summary of the tax category sync statistics instance that looks like the following example:
     *
     * <p>"Summary: 2 tax categories were processed in total (0 created, 0 updated and 0 failed to sync)."
     *
     * @return a summary message of the tax category sync statistics instance.
     */
    @Override
    public String getReportMessage() {
        reportMessage = format(
            "Summary: %s tax categories were processed in total (%s created, %s updated and %s failed to sync).",
            getProcessed(), getCreated(), getUpdated(), getFailed());
        return reportMessage;
    }

}
