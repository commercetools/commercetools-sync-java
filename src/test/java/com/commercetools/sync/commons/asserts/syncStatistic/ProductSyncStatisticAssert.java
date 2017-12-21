package com.commercetools.sync.commons.asserts.syncStatistic;

import com.commercetools.sync.products.helpers.ProductSyncStatistics;

public class ProductSyncStatisticAssert
    extends AbstractSyncStatisticAssert<ProductSyncStatisticAssert, ProductSyncStatistics> {

    public ProductSyncStatisticAssert(final ProductSyncStatistics actual) {
        super(actual, ProductSyncStatisticAssert.class);
    }

    public static ProductSyncStatisticAssert assertThatStatistic(final ProductSyncStatistics actual) {
        return new ProductSyncStatisticAssert(actual);
    }

}
