package com.commercetools.sync.integration.services;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.services.impl.TaxCategoryServiceImpl;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.commands.TaxCategoryCreateCommand;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Optional;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.createTaxCategory;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.createTaxRateDraft;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.deleteTaxCategories;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class TaxCategoryServiceIT {
    private TaxCategoryService taxCategoryService;
    private TaxCategory oldTaxCategory;
    private ArrayList<String> warnings;

    /**
     * Deletes tax categories from the target CTP projects, then it populates target CTP project with test data.
     */
    @Before
    public void setup() {
        deleteTaxCategories(CTP_TARGET_CLIENT);
        warnings = new ArrayList<>();
        oldTaxCategory = createTaxCategory(CTP_TARGET_CLIENT);
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                               .warningCallBack(warnings::add)
                                                                               .build();
        taxCategoryService = new TaxCategoryServiceImpl(productSyncOptions);
    }

    /**
     * Cleans up the target and source test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteTaxCategories(CTP_TARGET_CLIENT);
    }

    @Test
    public void fetchCachedTaxCategoryId_WithNonExistingTaxCategory_ShouldNotFetchATaxCategory() {
        final Optional<String> taxCategoryId = taxCategoryService.fetchCachedTaxCategoryId("non-existing-key")
                                                                 .toCompletableFuture()
                                                                 .join();
        assertThat(taxCategoryId).isEmpty();
        assertThat(warnings).isEmpty();
    }

    @Test
    public void fetchCachedTaxCategoryId_WithExistingTaxCategory_ShouldFetchProductTypeAndCache() {
        final Optional<String> taxCategoryId = taxCategoryService.fetchCachedTaxCategoryId(oldTaxCategory.getKey())
                                                                 .toCompletableFuture()
                                                                 .join();
        assertThat(taxCategoryId).isNotEmpty();
        assertThat(warnings).isEmpty();
    }

    @Test
    public void fetchCachedTaxCategoryId_OnSecondTime_ShouldNotFindProductTypeInCache() {
        // Fetch any key to populate cache
        taxCategoryService.fetchCachedTaxCategoryId("anyKey").toCompletableFuture().join();

        // Create new taxCategory
        final String newTaxCategoryKey = "new_tax_category_key";
        final TaxCategoryDraft taxCategoryDraft = TaxCategoryDraftBuilder
            .of("newTaxCategory", singletonList(createTaxRateDraft()), oldTaxCategory.getDescription())
            .key(newTaxCategoryKey)
            .build();
        executeBlocking(CTP_TARGET_CLIENT.execute(TaxCategoryCreateCommand.of(taxCategoryDraft)));


        final Optional<String> taxCategoryId = taxCategoryService.fetchCachedTaxCategoryId(newTaxCategoryKey)
                                                                 .toCompletableFuture()
                                                                 .join();

        assertThat(taxCategoryId).isEmpty();
        assertThat(warnings).isEmpty();
    }

    @Test
    public void fetchCachedTaxCategoryId_WithTaxCategoryExistingWithNoKey_ShouldTriggerWarningCallback() {
        // Create new taxCategory without key
        final TaxCategoryDraft taxCategoryDraft = TaxCategoryDraftBuilder
            .of("newTaxCategory", singletonList(createTaxRateDraft()), oldTaxCategory.getDescription())
            .build();
        final TaxCategory newTaxCategory = executeBlocking(
            CTP_TARGET_CLIENT.execute(TaxCategoryCreateCommand.of(taxCategoryDraft)));

        final Optional<String> taxCategoryId = taxCategoryService.fetchCachedTaxCategoryId(oldTaxCategory.getKey())
                                                                 .toCompletableFuture()
                                                                 .join();

        assertThat(taxCategoryId).isNotEmpty();
        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0)).isEqualTo(format("TaxCategory with id: '%s' has no key"
            + " set. Keys are required for taxCategory matching.", newTaxCategory.getId()));
    }

    @Test
    public void fetchCachedTaxCategoryId_WithWithNullKey_ShouldReturnFutureWithEmptyOptional() {
        final Optional<String> taxCategoryId = taxCategoryService.fetchCachedTaxCategoryId(null)
                                                                 .toCompletableFuture()
                                                                 .join();
        assertThat(taxCategoryId).isEmpty();
        assertThat(warnings).isEmpty();
    }
}
