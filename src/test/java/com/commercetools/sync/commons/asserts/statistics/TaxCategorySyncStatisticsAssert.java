package com.commercetools.sync.commons.asserts.statistics;

import com.commercetools.sync.taxcategories.helpers.TaxCategorySyncStatistics;

import javax.annotation.Nullable;

public final class TaxCategorySyncStatisticsAssert extends
    AbstractSyncStatisticsAssert<TaxCategorySyncStatisticsAssert, TaxCategorySyncStatistics> {

    TaxCategorySyncStatisticsAssert(@Nullable final TaxCategorySyncStatistics actual) {
        super(actual, TaxCategorySyncStatisticsAssert.class);
    }
}
