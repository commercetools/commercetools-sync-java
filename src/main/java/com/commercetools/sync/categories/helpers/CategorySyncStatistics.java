package com.commercetools.sync.categories.helpers;


import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import static java.lang.String.format;

public class CategorySyncStatistics extends BaseSyncStatistics {
    public CategorySyncStatistics() {
        super();
    }

    /**
     * Builds a summary of the category sync statistics instance that looks like the following example:
     *
     * <p>"Summary: 2 categories were processed in total (0 created, 0 updated and 0 categories failed to sync)."
     *
     * @return a summary message of the category sync statistics instance.
     */
    @Override
    public String getReportMessage() {
        return format("Summary: %s categories were processed in total "
            + "(%s created, %s updated and %s categories failed to sync).",
          getProcessed(), getCreated(), getUpdated(), getFailed());
    }
}
