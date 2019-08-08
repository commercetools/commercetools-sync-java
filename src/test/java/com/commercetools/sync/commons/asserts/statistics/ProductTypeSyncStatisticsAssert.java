package com.commercetools.sync.commons.asserts.statistics;

import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;

import javax.annotation.Nullable;

public final class ProductTypeSyncStatisticsAssert extends
    AbstractSyncStatisticsAssert<ProductTypeSyncStatisticsAssert, ProductTypeSyncStatistics> {

    ProductTypeSyncStatisticsAssert(@Nullable final ProductTypeSyncStatistics actual) {
        super(actual, ProductTypeSyncStatisticsAssert.class);
    }

    /**
     * Verifies that the actual {@link ProductTypeSyncStatistics} value has identical statistics counters as the ones
     * supplied.
     *
     * @param processed                                       the number of processed productTypes.
     * @param created                                         the number of created productTypes.
     * @param updated                                         the number of updated productTypes.
     * @param failed                                          the number of failed productTypes.
     * @param numberOfProductTypesWithMissingNestedReferences the number of productTypes with missing nestedType
     *                                                        references.
     * @return {@code this} assertion object.
     * @throws AssertionError if the actual value is {@code null}.
     * @throws AssertionError if the actual statistics do not match the supplied values.
     */
    public ProductTypeSyncStatisticsAssert hasValues(final int processed, final int created,
                                                     final int updated, final int failed,
                                                     final int numberOfProductTypesWithMissingNestedReferences) {
        super.hasValues(processed, created, updated, failed);
        org.assertj.core.api.Assertions.assertThat(
            actual.getNumberOfProductTypesWithMissingNestedProductTypes())
                                       .isEqualTo(numberOfProductTypesWithMissingNestedReferences);
        return myself;
    }
}
