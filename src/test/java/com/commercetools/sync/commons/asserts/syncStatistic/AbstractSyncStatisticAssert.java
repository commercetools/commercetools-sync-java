package com.commercetools.sync.commons.asserts.syncStatistic;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import org.assertj.core.api.AbstractAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractSyncStatisticAssert<S extends AbstractSyncStatisticAssert<S, A>, A extends BaseSyncStatistics>
    extends AbstractAssert<S, A> {

    public AbstractSyncStatisticAssert(final A actual, final Class<S> selfType) {
        super(actual, selfType);
    }

    public S hasValues(final int processed, final int created, final int updated, final int failed) {
        assertThat(actual).isNotNull();
        assertThat(actual.getProcessed()).isEqualTo(processed);
        assertThat(actual.getCreated()).isEqualTo(created);
        assertThat(actual.getUpdated()).isEqualTo(updated);
        assertThat(actual.getFailed()).isEqualTo(failed);
        return myself;
    }


}
