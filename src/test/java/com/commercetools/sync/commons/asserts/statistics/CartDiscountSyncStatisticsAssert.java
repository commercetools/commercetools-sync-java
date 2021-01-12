package com.commercetools.sync.commons.asserts.statistics;

import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import javax.annotation.Nullable;

public final class CartDiscountSyncStatisticsAssert
    extends AbstractSyncStatisticsAssert<
        CartDiscountSyncStatisticsAssert, CartDiscountSyncStatistics> {

  CartDiscountSyncStatisticsAssert(@Nullable final CartDiscountSyncStatistics actual) {
    super(actual, CartDiscountSyncStatisticsAssert.class);
  }
}
