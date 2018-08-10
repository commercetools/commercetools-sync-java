package com.commercetools.sync.commons.asserts.statistics;

import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;

import javax.annotation.Nullable;

public final class ProductTypeSyncStatisticsAssert extends
    AbstractSyncStatisticsAssert<ProductTypeSyncStatisticsAssert, ProductTypeSyncStatistics> {

    ProductTypeSyncStatisticsAssert(@Nullable final ProductTypeSyncStatistics actual) {
        super(actual, ProductTypeSyncStatisticsAssert.class);
    }
}
