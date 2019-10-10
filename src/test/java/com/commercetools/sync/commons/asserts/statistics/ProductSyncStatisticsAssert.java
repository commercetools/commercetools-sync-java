package com.commercetools.sync.commons.asserts.statistics;

import com.commercetools.sync.products.helpers.ProductSyncStatistics;

import javax.annotation.Nullable;

public final class ProductSyncStatisticsAssert
    extends AbstractSyncStatisticsAssert<ProductSyncStatisticsAssert, ProductSyncStatistics> {

    ProductSyncStatisticsAssert(@Nullable final ProductSyncStatistics actual) {
        super(actual, ProductSyncStatisticsAssert.class);
    }

    /**
     * Verifies that the actual {@link ProductSyncStatistics} value has identical statistics counters as the ones
     * supplied.
     *
     * @param processed                            the number of processed products.
     * @param created                              the number of created products.
     * @param updated                              the number of updated products.
     * @param failed                               the number of failed products.
     * @param numberOfProductsWithMissingParents the number of products with missing parents.
     *
     * @return {@code this} assertion object.
     * @throws AssertionError if the actual value is {@code null}.
     * @throws AssertionError if the actual statistics do not match the supplied values.
     */
    public ProductSyncStatisticsAssert hasValues(final int processed, final int created,
                                                  final int updated, final int failed,
                                                  final int numberOfProductsWithMissingParents) {
        super.hasValues(processed, created, updated, failed);
        org.assertj.core.api.Assertions.assertThat(
            actual.getNumberOfProductsWithMissingParents()).isEqualTo(numberOfProductsWithMissingParents);
        return myself;
    }
}
