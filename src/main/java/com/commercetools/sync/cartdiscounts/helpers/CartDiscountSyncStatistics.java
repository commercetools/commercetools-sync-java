package com.commercetools.sync.cartdiscounts.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import static java.lang.String.format;

public final class CartDiscountSyncStatistics extends BaseSyncStatistics {

    /**
     * Builds a summary of the cart discount sync statistics instance that looks like the following example:
     *
     * <p>"Summary: 2 cart discounts were processed in total (0 created, 0 updated and 0 failed to sync)."
     *
     * @return a summary message of the cart discount sync statistics instance.
     */
    @Override
    public String getReportMessage() {
        reportMessage = format(
            "Summary: %s cart discounts were processed in total (%s created, %s updated and %s failed to sync).",
            getProcessed(), getCreated(), getUpdated(), getFailed());

        return reportMessage;
    }
}
