package com.commercetools.sync.sdk2.commons.asserts.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.sdk2.commons.helpers.BaseSyncStatistics;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.assertj.core.api.AbstractAssert;

class AbstractSyncStatisticsAssert<
        SyncStatisticsAssert extends
            AbstractSyncStatisticsAssert<SyncStatisticsAssert, SyncStatistics>,
        SyncStatistics extends BaseSyncStatistics>
    extends AbstractAssert<SyncStatisticsAssert, SyncStatistics> {

  AbstractSyncStatisticsAssert(
      @Nullable final SyncStatistics actual, @Nonnull final Class<SyncStatisticsAssert> selfType) {
    super(actual, selfType);
  }

  /**
   * Verifies that the actual {@link BaseSyncStatistics} value has identical statistics counters as
   * the ones supplied.
   *
   * @param processed the number of processed resources.
   * @param created the number of created resources.
   * @param updated the number of updated resources.
   * @param failed the number of failed resources.
   * @return {@code this} assertion object.
   * @throws AssertionError if the actual value is {@code null}.
   * @throws AssertionError if the actual statistics do not match the supplied values.
   */
  public SyncStatisticsAssert hasValues(
      final int processed, final int created, final int updated, final int failed) {
    assertThat(actual).isNotNull();
    assertThat(actual.getProcessed()).hasValue(processed);
    assertThat(actual.getCreated()).hasValue(created);
    assertThat(actual.getUpdated()).hasValue(updated);
    assertThat(actual.getFailed()).hasValue(failed);
    return myself;
  }
}
