package com.commercetools.sync.commons.asserts.statistics;

import com.commercetools.sync.customobjects.helpers.CustomObjectSyncStatistics;
import javax.annotation.Nullable;

public final class CustomObjectSyncStatisticsAssert extends
    AbstractSyncStatisticsAssert<CustomObjectSyncStatisticsAssert, CustomObjectSyncStatistics> {

    CustomObjectSyncStatisticsAssert(@Nullable final CustomObjectSyncStatistics actual) {
        super(actual, CustomObjectSyncStatisticsAssert.class);
    }
}
