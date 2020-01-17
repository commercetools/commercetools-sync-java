package com.commercetools.sync.integration.services.impl;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.services.impl.TaxCategoryServiceImpl;
import io.sphere.sdk.taxcategories.TaxCategory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.createTaxCategory;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.deleteTaxCategories;
import static org.assertj.core.api.Assertions.assertThat;

class TaxCategoryServiceImplIT {
    private TaxCategoryService taxCategoryService;
    private TaxCategory oldTaxCategory;
    private ArrayList<String> warnings;

    /**
     * Deletes tax categories from the target CTP projects, then it populates target CTP project with test data.
     */
    @BeforeEach
    void setup() {
        deleteTaxCategories(CTP_TARGET_CLIENT);
        warnings = new ArrayList<>();
        oldTaxCategory = createTaxCategory(CTP_TARGET_CLIENT);
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                               .warningCallback(warnings::add)
                                                                               .build();
        taxCategoryService = new TaxCategoryServiceImpl(productSyncOptions);
    }

    /**
     * Cleans up the target and source test data that were built in this test class.
     */
    @AfterAll
    static void tearDown() {
        deleteTaxCategories(CTP_TARGET_CLIENT);
    }

    @Test
    void fetchCachedTaxCategoryId_WithNonExistingTaxCategory_ShouldNotFetchATaxCategory() {
        final Optional<String> taxCategoryId = taxCategoryService.fetchCachedTaxCategoryId("non-existing-key")
                                                                 .toCompletableFuture()
                                                                 .join();
        assertThat(taxCategoryId).isEmpty();
        assertThat(warnings).isEmpty();
    }

    @Test
    void fetchCachedTaxCategoryId_WithExistingTaxCategory_ShouldFetchProductTypeAndCache() {
        final Optional<String> taxCategoryId = taxCategoryService.fetchCachedTaxCategoryId(oldTaxCategory.getKey())
                                                                 .toCompletableFuture()
                                                                 .join();
        assertThat(taxCategoryId).isNotEmpty();
        assertThat(warnings).isEmpty();
    }

    @Test
    void fetchCachedTaxCategoryId_WithWithNullKey_ShouldReturnFutureWithEmptyOptional() {
        final Optional<String> taxCategoryId = taxCategoryService.fetchCachedTaxCategoryId(null)
                                                                 .toCompletableFuture()
                                                                 .join();
        assertThat(taxCategoryId).isEmpty();
        assertThat(warnings).isEmpty();
    }
}
