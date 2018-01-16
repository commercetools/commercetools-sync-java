package com.commercetools.sync.commons.asserts.statistics;

import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;

import javax.annotation.Nullable;

public final class InventorySyncStatisticsAssert extends
    AbstractSyncStatisticsAssert<InventorySyncStatisticsAssert, InventorySyncStatistics> {

    InventorySyncStatisticsAssert(@Nullable final InventorySyncStatistics actual) {
        super(actual, InventorySyncStatisticsAssert.class);
    }
}
