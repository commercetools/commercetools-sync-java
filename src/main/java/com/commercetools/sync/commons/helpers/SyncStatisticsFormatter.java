package com.commercetools.sync.commons.helpers;

import javax.annotation.Nonnull;

/**
 * Provides interface for different formatters of {@link BaseSyncStatistics}.
 */
public interface SyncStatisticsFormatter {

    String format(@Nonnull final BaseSyncStatistics statistics);
}
