package com.commercetools.sync.categories;


import com.commercetools.sync.commons.helpers.BaseStatistics;

import static java.lang.String.format;

public class CategoryStatistics extends BaseStatistics {
    public CategoryStatistics() {
    }

    public String getReportMessage() {
        return format("Summary: %s categories were processed in total " +
                        "(%s created, %s updated and %s categories failed to sync).",
                getProcessed(), getCreated(), getUpdated(), getFailed());
    }
}
