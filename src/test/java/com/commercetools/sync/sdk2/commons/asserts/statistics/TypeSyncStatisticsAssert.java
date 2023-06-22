package com.commercetools.sync.sdk2.commons.asserts.statistics;

import com.commercetools.sync.sdk2.types.helpers.TypeSyncStatistics;
import javax.annotation.Nullable;

public final class TypeSyncStatisticsAssert
    extends AbstractSyncStatisticsAssert<TypeSyncStatisticsAssert, TypeSyncStatistics> {

  TypeSyncStatisticsAssert(@Nullable final TypeSyncStatistics actual) {
    super(actual, TypeSyncStatisticsAssert.class);
  }
}
