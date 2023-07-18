package com.commercetools.sync.sdk2.commons.asserts.statistics;

import com.commercetools.sync.sdk2.taxcategories.helpers.TaxCategorySyncStatistics;
import javax.annotation.Nullable;

public final class TaxCategorySyncStatisticsAssert
    extends AbstractSyncStatisticsAssert<
        TaxCategorySyncStatisticsAssert, TaxCategorySyncStatistics> {

  TaxCategorySyncStatisticsAssert(@Nullable final TaxCategorySyncStatistics actual) {
    super(actual, TaxCategorySyncStatisticsAssert.class);
  }
}
