package com.commercetools.sync.commons;

import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;

public final class AssertionUtils {
    private AssertionUtils() {
    }

    public static void assertStatistics(@Nonnull final BaseSyncStatistics syncStatistics,
                                        final int processed,
                                        final int created,
                                        final int updated,
                                        final int failed) {
        assertThat(syncStatistics.getProcessed()).isEqualTo(processed);
        assertThat(syncStatistics.getCreated()).isEqualTo(created);
        assertThat(syncStatistics.getUpdated()).isEqualTo(updated);
        assertThat(syncStatistics.getFailed()).isEqualTo(failed);
    }

    public static void assertStatistics(@Nonnull final CategorySyncStatistics syncStatistics,
                                        final int processed,
                                        final int created,
                                        final int updated,
                                        final int failed,
                                        final int numberOfCategoriesWithMissingParents) {
        assertThat(syncStatistics.getProcessed()).isEqualTo(processed);
        assertThat(syncStatistics.getCreated()).isEqualTo(created);
        assertThat(syncStatistics.getUpdated()).isEqualTo(updated);
        assertThat(syncStatistics.getFailed()).isEqualTo(failed);
        assertThat(syncStatistics.getNumberOfCategoriesWithMissingParents())
            .isEqualTo(numberOfCategoriesWithMissingParents);
    }

}
