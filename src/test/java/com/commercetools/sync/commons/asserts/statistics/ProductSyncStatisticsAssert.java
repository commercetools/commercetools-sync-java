package com.commercetools.sync.commons.asserts.statistics;

import com.commercetools.sync.products.helpers.ProductSyncStatistics;

import javax.annotation.Nullable;

public final class ProductSyncStatisticsAssert
    extends AbstractSyncStatisticsAssert<ProductSyncStatisticsAssert, ProductSyncStatistics> {

    ProductSyncStatisticsAssert(@Nullable final ProductSyncStatistics actual) {
        super(actual, ProductSyncStatisticsAssert.class);
    }
}
