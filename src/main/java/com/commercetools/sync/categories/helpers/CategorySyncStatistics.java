package com.commercetools.sync.categories.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import static java.lang.String.format;

public final class CategorySyncStatistics extends BaseSyncStatistics {

    CategorySyncStatistics(final long processingTimeInMillis, final int created, final int updated, final int upToDate,
                           final int failed) {
        super(processingTimeInMillis, created, updated, upToDate, failed);
    }

    /**
     * Builds a summary of the category sync statistics instance that looks like the following example:
     *
     * <p>"Summary: 10 categories were processed in total (4 created, 2 updated, 2 were up to date and 2 failed to
     * sync)."
     *
     * @return a summary message of the category sync statistics instance.
     */
    @Override
    public String getReportMessage() {
        return format(REPORT_MESSAGE_TEMPLATE, getProcessed(), "categories", getCreated(), getUpdated(), getUpToDate(),
            getFailed());
    }
}
