package com.commercetools.sync.commons.asserts.statistics;

import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;

import javax.annotation.Nullable;

public final class ShoppingListSyncStatisticsAssert extends
    AbstractSyncStatisticsAssert<ShoppingListSyncStatisticsAssert, ShoppingListSyncStatistics> {

    ShoppingListSyncStatisticsAssert(@Nullable final ShoppingListSyncStatistics actual) {
        super(actual, ShoppingListSyncStatisticsAssert.class);
    }
}
