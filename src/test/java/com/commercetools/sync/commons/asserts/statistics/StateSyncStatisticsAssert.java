package com.commercetools.sync.commons.asserts.statistics;

import com.commercetools.sync.states.helpers.StateSyncStatistics;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

public final class StateSyncStatisticsAssert
    extends AbstractSyncStatisticsAssert<StateSyncStatisticsAssert, StateSyncStatistics> {

    StateSyncStatisticsAssert(@Nullable final StateSyncStatistics actual) {
        super(actual, StateSyncStatisticsAssert.class);
    }

    /**
     * Verifies that the actual {@link StateSyncStatistics} value has identical statistics counters as the ones
     * supplied.
     *
     * @param processed                            the number of processed states.
     * @param created                              the number of created states.
     * @param updated                              the number of updated states.
     * @param failed                               the number of failed states.
     * @param numberOfStatesWithMissingParents the number of states with missing parents.
     *
     * @return {@code this} assertion object.
     * @throws AssertionError if the actual value is {@code null}.
     * @throws AssertionError if the actual statistics do not match the supplied values.
     */
    public StateSyncStatisticsAssert hasValues(final int processed, final int created,
                                               final int updated, final int failed,
                                               final int numberOfStatesWithMissingParents) {
        super.hasValues(processed, created, updated, failed);
        assertThat(actual.getNumberOfStatesWithMissingParents()).isEqualTo(numberOfStatesWithMissingParents);
        return myself;
    }
}
