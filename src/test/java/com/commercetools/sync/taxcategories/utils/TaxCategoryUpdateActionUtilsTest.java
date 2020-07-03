package com.commercetools.sync.taxcategories.utils;

import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptionsBuilder;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.SubRate;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.TaxRate;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import io.sphere.sdk.taxcategories.commands.updateactions.AddTaxRate;
import io.sphere.sdk.taxcategories.commands.updateactions.ChangeName;
import io.sphere.sdk.taxcategories.commands.updateactions.RemoveTaxRate;
import io.sphere.sdk.taxcategories.commands.updateactions.ReplaceTaxRate;
import io.sphere.sdk.taxcategories.commands.updateactions.SetDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.commercetools.sync.taxcategories.utils.TaxCategoryUpdateActionUtils.buildChangeNameAction;
import static com.commercetools.sync.taxcategories.utils.TaxCategoryUpdateActionUtils.buildSetDescriptionAction;
import static com.commercetools.sync.taxcategories.utils.TaxCategoryUpdateActionUtils.buildTaxRateUpdateActions;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaxCategoryUpdateActionUtilsTest {

    private TaxCategory taxCategory;
    private TaxCategoryDraft newSameTaxCategoryDraft;
    private TaxCategoryDraft newDifferentTaxCategoryDraft;

    @BeforeEach
    void setup() {
        final String name = "test name";
        final String key = "test key";
        final String description = "test description";

        taxCategory = mock(TaxCategory.class);
        when(taxCategory.getName()).thenReturn(name);
        when(taxCategory.getKey()).thenReturn(key);
        when(taxCategory.getDescription()).thenReturn(description);

        final String taxRateName1 = "taxRateName1";
        final String taxRateName2 = "taxRateName2";

        final TaxRate taxRate1 = mock(TaxRate.class);
        when(taxRate1.getName()).thenReturn(taxRateName1);
        when(taxRate1.getId()).thenReturn("taxRateId1");
        when(taxRate1.getAmount()).thenReturn(1.0);
        when(taxRate1.getCountry()).thenReturn(CountryCode.DE);
        when(taxRate1.isIncludedInPrice()).thenReturn(false);
        final TaxRate taxRate2 = mock(TaxRate.class);
        when(taxRate2.getName()).thenReturn(taxRateName2);
        when(taxRate2.getId()).thenReturn("taxRateId2");
        when(taxRate2.getAmount()).thenReturn(2.0);
        when(taxRate2.getCountry()).thenReturn(CountryCode.US);
        when(taxRate2.isIncludedInPrice()).thenReturn(false);

        final List<TaxRate> oldTaxRates = asList(taxRate1, taxRate2);

        when(taxCategory.getTaxRates()).thenReturn(oldTaxRates);

        newSameTaxCategoryDraft = TaxCategoryDraftBuilder.of(name, emptyList(), description).key(key).build();

        newDifferentTaxCategoryDraft = TaxCategoryDraftBuilder.of("changedName", emptyList(), "desc")
            .key(key)
            .build();
    }

    @Test
    void buildChangeNameAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<TaxCategory>> result = buildChangeNameAction(taxCategory,
            newDifferentTaxCategoryDraft);

        assertThat(result).contains(ChangeName.of(newDifferentTaxCategoryDraft.getName()));
    }

    @Test
    void buildChangeNameAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<TaxCategory>> result = buildChangeNameAction(taxCategory, newSameTaxCategoryDraft);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetDescriptionAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<TaxCategory>> result = buildSetDescriptionAction(taxCategory,
            newDifferentTaxCategoryDraft);

        assertThat(result).contains(SetDescription.of(newDifferentTaxCategoryDraft.getDescription()));
    }

    @Test
    void buildSetDescriptionAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<TaxCategory>> result = buildSetDescriptionAction(taxCategory,
            newSameTaxCategoryDraft);

        assertThat(result).isEmpty();
    }

    @Test
    void buildRatesUpdateActions_WithDuplicatedCountryCodes_ShouldNotBuildActionAndTriggerErrorCallback() {
        final String name = "DuplicatedName";
        newDifferentTaxCategoryDraft = TaxCategoryDraftBuilder.of(name, asList(
            // replace
            TaxRateDraftBuilder.of(name, 2.0, false, CountryCode.DE).build(),
            TaxRateDraftBuilder.of(name, 3.0, false, CountryCode.DE).build()
        ), "desc").build();

        final AtomicReference<String> callback = new AtomicReference<>(null);

        final TaxCategorySyncOptions syncOptions = TaxCategorySyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((errorMsg, exception) -> callback.set(errorMsg))
            .build();

        final List<UpdateAction<TaxCategory>> result = buildTaxRateUpdateActions(taxCategory,
            newDifferentTaxCategoryDraft, syncOptions);

        assertThat(result).isEmpty();
        assertThat(callback.get())
            .contains(format("Tax rates drafts have duplicated country codes. Duplicated tax rate "
                    + "country code: '%s'. Tax rates country codes are expected to be unique "
                    + "inside their tax category.", CountryCode.DE
                ));
    }

    @Test
    void buildRatesUpdateActions_OnlyWithNewRate_ShouldBuildOnlyAddTaxRateAction() {
        TaxCategory taxCategory = mock(TaxCategory.class);
        when(taxCategory.getKey()).thenReturn("tax-category-key");

        TaxRateDraft taxRateDraft = TaxRateDraftBuilder
            .of("%5 DE", 0.05, false, CountryCode.DE)
            .build();

        TaxCategoryDraft taxCategoryDraft = TaxCategoryDraftBuilder
            .of(null, singletonList(taxRateDraft), null)
            .key("tax-category-key")
            .build();

        final List<UpdateAction<TaxCategory>> result =
            buildTaxRateUpdateActions(taxCategory, taxCategoryDraft, mock(TaxCategorySyncOptions.class));

        assertThat(result).isEqualTo(singletonList(AddTaxRate.of(taxRateDraft)));
    }

    @Test
    void buildRatesUpdateActions_OnlyUpdatedTaxRate_ShouldBuildOnlyReplaceTaxRateAction() {
        TaxCategory taxCategory = mock(TaxCategory.class);
        when(taxCategory.getKey()).thenReturn("tax-category-key");
        when(taxCategory.getName()).thenReturn("name");
        when(taxCategory.getDescription()).thenReturn("desc");

        final TaxRate taxRate = mock(TaxRate.class);
        when(taxRate.getName()).thenReturn("19% DE");
        when(taxRate.getId()).thenReturn("taxRate-1");
        when(taxRate.getAmount()).thenReturn(0.19);
        when(taxRate.getCountry()).thenReturn(CountryCode.DE);
        when(taxRate.isIncludedInPrice()).thenReturn(false);

        when(taxCategory.getTaxRates()).thenReturn(singletonList(taxRate));

        TaxRateDraft taxRateDraft = TaxRateDraftBuilder
            .of("%16 DE", 0.16, false, CountryCode.DE)
            .build();

        TaxCategoryDraft taxCategoryDraft = TaxCategoryDraftBuilder
            .of("name", singletonList(taxRateDraft), "desc")
            .key("tax-category-key")
            .build();

        final List<UpdateAction<TaxCategory>> result =
            buildTaxRateUpdateActions(taxCategory, taxCategoryDraft, mock(TaxCategorySyncOptions.class));

        assertThat(result).isEqualTo(singletonList(ReplaceTaxRate.of("taxRate-1", taxRateDraft)));
    }

    @Test
    void buildRatesUpdateActions_withoutNewTaxRate_ShouldBuildOnlyRemoveTaxRateAction() {
        TaxCategory taxCategory = mock(TaxCategory.class);
        when(taxCategory.getKey()).thenReturn("tax-category-key");
        when(taxCategory.getName()).thenReturn("name");
        when(taxCategory.getDescription()).thenReturn("desc");

        final TaxRate taxRate = mock(TaxRate.class);
        when(taxRate.getName()).thenReturn("19% DE");
        when(taxRate.getId()).thenReturn("taxRate-1");
        when(taxRate.getAmount()).thenReturn(0.19);
        when(taxRate.getCountry()).thenReturn(CountryCode.DE);
        when(taxRate.isIncludedInPrice()).thenReturn(false);

        when(taxCategory.getTaxRates()).thenReturn(singletonList(taxRate));

        TaxCategoryDraft taxCategoryDraft = TaxCategoryDraftBuilder
            .of("name", emptyList(), "desc")
            .key("tax-category-key")
            .build();

        final List<UpdateAction<TaxCategory>> result =
            buildTaxRateUpdateActions(taxCategory, taxCategoryDraft, mock(TaxCategorySyncOptions.class));

        assertThat(result).isEqualTo(singletonList(RemoveTaxRate.of("taxRate-1")));
    }

    @Test
    void buildRatesUpdateActions_withDifferentTaxRate_ShouldBuildMixedActions() {
        TaxCategory taxCategory = mock(TaxCategory.class);
        when(taxCategory.getKey()).thenReturn("tax-category-key");
        when(taxCategory.getName()).thenReturn("name");
        when(taxCategory.getDescription()).thenReturn("desc");

        final TaxRate taxRate1 = mock(TaxRate.class);
        when(taxRate1.getName()).thenReturn("11% US");
        when(taxRate1.getId()).thenReturn("taxRate-1");
        when(taxRate1.getAmount()).thenReturn(0.11);
        when(taxRate1.getCountry()).thenReturn(CountryCode.US);
        when(taxRate1.isIncludedInPrice()).thenReturn(false);

        final TaxRate taxRate2 = mock(TaxRate.class);
        when(taxRate2.getName()).thenReturn("8% DE");
        when(taxRate2.getId()).thenReturn("taxRate-2");
        when(taxRate2.getAmount()).thenReturn(0.08);
        when(taxRate2.getCountry()).thenReturn(CountryCode.AT);
        when(taxRate2.isIncludedInPrice()).thenReturn(false);

        final TaxRate taxRate3 = mock(TaxRate.class);
        when(taxRate3.getName()).thenReturn("21% ES");
        when(taxRate3.getId()).thenReturn("taxRate-3");
        when(taxRate3.getAmount()).thenReturn(0.21);
        when(taxRate3.getCountry()).thenReturn(CountryCode.ES);
        when(taxRate3.isIncludedInPrice()).thenReturn(false);

        when(taxCategory.getTaxRates()).thenReturn(asList(taxRate1, taxRate2, taxRate3));

        //Update: Price is included.
        TaxRateDraft taxRateDraft1 = TaxRateDraftBuilder
            .of("11% US", 0.11, true, CountryCode.US)
            .build();

        // taxRate-2 is removed.
        // new rate is added.
        TaxRateDraft taxRateDraft4 = TaxRateDraftBuilder
            .of("15% FR", 0.15, false, CountryCode.FR)
            .build();

        // same
        TaxRateDraft taxRateDraft3 = TaxRateDraftBuilder
            .of("21% ES", 0.21, false, CountryCode.ES)
            .build();

        TaxCategoryDraft taxCategoryDraft = TaxCategoryDraftBuilder
            .of("name", asList(taxRateDraft1, taxRateDraft4, taxRateDraft3), "desc")
            .key("tax-category-key")
            .build();


        final List<UpdateAction<TaxCategory>> result =
            buildTaxRateUpdateActions(taxCategory, taxCategoryDraft, mock(TaxCategorySyncOptions.class));

        assertThat(result).isEqualTo(asList(
            ReplaceTaxRate.of("taxRate-1", taxRateDraft1),
            RemoveTaxRate.of("taxRate-2"),
            AddTaxRate.of(taxRateDraft4)));
    }

    @Test
    void buildTaxRatesUpdateActions_WithOnlyDifferentSubRates_ShouldReturnOnlyReplaceAction() {

        TaxCategory taxCategory = mock(TaxCategory.class);
        when(taxCategory.getKey()).thenReturn("tax-category-key");
        when(taxCategory.getName()).thenReturn("name");
        when(taxCategory.getDescription()).thenReturn("desc");

        final TaxRate taxRate1 = mock(TaxRate.class);
        when(taxRate1.getName()).thenReturn("11% US");
        when(taxRate1.getState()).thenReturn("state");
        when(taxRate1.getId()).thenReturn("taxRate-1");
        when(taxRate1.getAmount()).thenReturn(0.11);
        when(taxRate1.getCountry()).thenReturn(CountryCode.US);
        when(taxRate1.isIncludedInPrice()).thenReturn(false);

        final SubRate oldSubRate1 = SubRate.of("subRate-1", 0.07);
        final SubRate oldSubRate2 = SubRate.of("subRate-2", 0.04);

        when(taxRate1.getSubRates()).thenReturn(asList(oldSubRate1, oldSubRate2));
        when(taxCategory.getTaxRates()).thenReturn(singletonList(taxRate1));

        final SubRate subRate1 = SubRate.of("subRate-1", 0.06);
        final SubRate subRate2 = SubRate.of("subRate-2", 0.05);

        TaxRateDraft taxRateDraft = TaxRateDraftBuilder
            .of("11% US", 0.11, false, CountryCode.US)
            .state("state")
            .subRates(asList(subRate1, subRate2))
            .build();

        TaxCategoryDraft taxCategoryDraft = TaxCategoryDraftBuilder
            .of("name", singletonList(taxRateDraft), "desc")
            .key("tax-category-key")
            .build();

        final List<UpdateAction<TaxCategory>> result =
            buildTaxRateUpdateActions(taxCategory, taxCategoryDraft, mock(TaxCategorySyncOptions.class));

        assertThat(result).isEqualTo(singletonList(ReplaceTaxRate.of("taxRate-1", taxRateDraft)));
    }

    @Test
    void buildTaxRatesUpdateActions_WithMoreSubRates_ShouldReturnOnlyReplaceAction() {

        TaxCategory taxCategory = mock(TaxCategory.class);
        when(taxCategory.getKey()).thenReturn("tax-category-key");
        when(taxCategory.getName()).thenReturn("name");
        when(taxCategory.getDescription()).thenReturn("desc");

        final TaxRate taxRate1 = mock(TaxRate.class);
        when(taxRate1.getName()).thenReturn("11% US");
        when(taxRate1.getState()).thenReturn("state");
        when(taxRate1.getId()).thenReturn("taxRate-1");
        when(taxRate1.getAmount()).thenReturn(0.11);
        when(taxRate1.getCountry()).thenReturn(CountryCode.US);
        when(taxRate1.isIncludedInPrice()).thenReturn(false);

        final SubRate oldSubRate1 = SubRate.of("subRate-1", 0.11);

        when(taxRate1.getSubRates()).thenReturn(singletonList(oldSubRate1));
        when(taxCategory.getTaxRates()).thenReturn(singletonList(taxRate1));

        final SubRate subRate1 = SubRate.of("subRate-1", 0.06);
        final SubRate subRate2 = SubRate.of("subRate-2", 0.05);

        TaxRateDraft taxRateDraft = TaxRateDraftBuilder
            .of("11% US", 0.11, false, CountryCode.US)
            .state("state")
            .subRates(asList(subRate1, subRate2))
            .build();

        TaxCategoryDraft taxCategoryDraft = TaxCategoryDraftBuilder
            .of("name", singletonList(taxRateDraft), "desc")
            .key("tax-category-key")
            .build();

        final List<UpdateAction<TaxCategory>> result =
            buildTaxRateUpdateActions(taxCategory, taxCategoryDraft, mock(TaxCategorySyncOptions.class));

        assertThat(result).isEqualTo(singletonList(ReplaceTaxRate.of("taxRate-1", taxRateDraft)));
    }
}
