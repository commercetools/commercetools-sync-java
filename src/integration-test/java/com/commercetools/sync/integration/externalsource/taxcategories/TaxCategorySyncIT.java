package com.commercetools.sync.integration.externalsource.taxcategories;

import com.commercetools.sync.taxcategories.TaxCategorySync;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptionsBuilder;
import com.commercetools.sync.taxcategories.helpers.TaxCategorySyncStatistics;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.taxcategories.SubRate;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.TaxRate;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import io.sphere.sdk.taxcategories.commands.TaxCategoryCreateCommand;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.deleteTaxCategories;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.getTaxCategoryByKey;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

class TaxCategorySyncIT {

    @BeforeEach
    void setup() {
        deleteTaxCategories(CTP_TARGET_CLIENT);

        final SubRate subRate1 = SubRate.of("subRate-1", 0.08);
        final SubRate subRate2 = SubRate.of("subRate-2", 0.11);

        final TaxRateDraft taxRateDraft = TaxRateDraftBuilder
            .of("%19 VAT DE", 0.19, false, CountryCode.DE)
            .subRates(asList(subRate1, subRate2))
            .build();

        final TaxCategoryDraft taxCategoryDraft = TaxCategoryDraftBuilder
            .of("tax-category-name", singletonList(taxRateDraft), "tax-category-description")
            .key("tax-category-key")
            .build();

        executeBlocking(CTP_TARGET_CLIENT.execute(TaxCategoryCreateCommand.of(taxCategoryDraft)));
    }

    @AfterAll
    static void tearDown() {
        deleteTaxCategories(CTP_TARGET_CLIENT);
    }

    @Test
    void sync_withNewTaxCategory_shouldCreateTaxCategory() {
        final SubRate subRate1 = SubRate.of("subRate-1", 0.05);
        final SubRate subRate2 = SubRate.of("subRate-2", 0.06);

        final TaxRateDraft taxRateDraft = TaxRateDraftBuilder
            .of("%11 US", 0.11, false, CountryCode.US)
            .subRates(asList(subRate1, subRate2))
            .build();

        final TaxCategoryDraft taxCategoryDraft = TaxCategoryDraftBuilder
            .of("tax-category-name-new", singletonList(taxRateDraft), "tax-category-description-new")
            .key("tax-category-key-new")
            .build();

        final TaxCategorySyncOptions taxCategorySyncOptions = TaxCategorySyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final TaxCategorySync taxCategorySync = new TaxCategorySync(taxCategorySyncOptions);

        // test
        final TaxCategorySyncStatistics taxCategorySyncStatistics = taxCategorySync
            .sync(singletonList(taxCategoryDraft))
            .toCompletableFuture()
            .join();

        assertThat(taxCategorySyncStatistics).hasValues(1, 1, 0, 0);
    }

    @Test
    void sync_WithUpdatedTaxCategory_ShouldUpdateTaxCategory() {
        // preparation
        final SubRate subRate1 = SubRate.of("subRate-1", 0.07);
        final SubRate subRate2 = SubRate.of("subRate-2", 0.09);

        final TaxRateDraft taxRateDraft = TaxRateDraftBuilder
            .of("%16 VAT", 0.16, true, CountryCode.DE)
            .subRates(asList(subRate1, subRate2))
            .build();

        final TaxCategoryDraft taxCategoryDraft = TaxCategoryDraftBuilder
            .of("tax-category-name-updated", singletonList(taxRateDraft), "tax-category-description-updated")
            .key("tax-category-key")
            .build();

        final TaxCategorySyncOptions taxCategorySyncOptions = TaxCategorySyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final TaxCategorySync taxCategorySync = new TaxCategorySync(taxCategorySyncOptions);

        // test
        final TaxCategorySyncStatistics taxCategorySyncStatistics = taxCategorySync
            .sync(singletonList(taxCategoryDraft))
            .toCompletableFuture()
            .join();

        // assertion
        assertThat(taxCategorySyncStatistics).hasValues(1, 0, 1, 0);

        final Optional<TaxCategory> oldTaxCategoryAfter = getTaxCategoryByKey(CTP_TARGET_CLIENT, "tax-category-key");

        Assertions.assertThat(oldTaxCategoryAfter).hasValueSatisfying(taxCategory -> {
            Assertions.assertThat(taxCategory.getName()).isEqualTo("tax-category-name-updated");
            Assertions.assertThat(taxCategory.getDescription()).isEqualTo("tax-category-description-updated");
            final TaxRate taxRate = taxCategory.getTaxRates().get(0);
            Assertions.assertThat(taxRate.getName()).isEqualTo("%16 VAT");
            Assertions.assertThat(taxRate.getAmount()).isEqualTo(0.16);
            Assertions.assertThat(taxRate.getCountry()).isEqualTo(CountryCode.DE);
            Assertions.assertThat(taxRate.isIncludedInPrice()).isEqualTo(true);
            Assertions.assertThat(taxRate.getSubRates()).isEqualTo(asList(subRate1, subRate2));
        });
    }

    @Test
    void sync_withEqualTaxCategory_shouldNotUpdateTaxCategory() {
        final SubRate subRate1 = SubRate.of("subRate-1", 0.08);
        final SubRate subRate2 = SubRate.of("subRate-2", 0.11);

        final TaxRateDraft taxRateDraft = TaxRateDraftBuilder
            .of("%19 VAT DE", 0.19, false, CountryCode.DE)
            .subRates(asList(subRate1, subRate2))
            .build();

        final TaxCategoryDraft taxCategoryDraft = TaxCategoryDraftBuilder
            .of("tax-category-name", singletonList(taxRateDraft), "tax-category-description")
            .key("tax-category-key")
            .build();

        final TaxCategorySyncOptions taxCategorySyncOptions = TaxCategorySyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final TaxCategorySync taxCategorySync = new TaxCategorySync(taxCategorySyncOptions);

        // test
        final TaxCategorySyncStatistics taxCategorySyncStatistics = taxCategorySync
            .sync(singletonList(taxCategoryDraft))
            .toCompletableFuture()
            .join();

        assertThat(taxCategorySyncStatistics).hasValues(1, 0, 0, 0);
    }
}
