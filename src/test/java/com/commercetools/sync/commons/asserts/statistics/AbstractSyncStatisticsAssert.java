package com.commercetools.sync.commons.asserts.syncStatistic;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import org.assertj.core.api.AbstractAssert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractSyncStatisticsAssert<S extends AbstractSyncStatisticsAssert<S, A>, A extends BaseSyncStatistics>
    extends AbstractAssert<S, A> {

    AbstractSyncStatisticsAssert(@Nullable final A actual, @Nonnull final Class<S> selfType) {
        super(actual, selfType);
    }

    /**
     * Verifies that the actual {@link BaseSyncStatistics} value has identical statistics counters as the ones
     * supplied.
     *
     * @param processed                            the number of processed resources.
     * @param created                              the number of created resources.
     * @param updated                              the number of updated resources.
     * @param failed                               the number of failed resources.
     *
     * @return {@code this} assertion object.
     * @throws AssertionError if the actual value is {@code null}.
     * @throws AssertionError if the actual statistics do not match the supplied values.
     */
    public S hasValues(final int processed, final int created, final int updated, final int failed) {
        assertThat(actual).isNotNull();
        assertThat(actual.getProcessed()).isEqualTo(processed);
        assertThat(actual.getCreated()).isEqualTo(created);
        assertThat(actual.getUpdated()).isEqualTo(updated);
        assertThat(actual.getFailed()).isEqualTo(failed);
        return myself;
    }
}
