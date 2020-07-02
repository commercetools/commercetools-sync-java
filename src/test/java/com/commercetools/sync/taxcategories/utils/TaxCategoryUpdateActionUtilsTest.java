package com.commercetools.sync.taxcategories.utils;

import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptionsBuilder;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.TaxRate;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import io.sphere.sdk.taxcategories.commands.updateactions.AddTaxRate;
import io.sphere.sdk.taxcategories.commands.updateactions.ChangeName;
import io.sphere.sdk.taxcategories.commands.updateactions.RemoveTaxRate;
import io.sphere.sdk.taxcategories.commands.updateactions.ReplaceTaxRate;
import io.sphere.sdk.taxcategories.commands.updateactions.SetDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.commercetools.sync.taxcategories.utils.TaxCategoryUpdateActionUtils.buildChangeNameAction;
import static com.commercetools.sync.taxcategories.utils.TaxCategoryUpdateActionUtils.buildTaxRateUpdateActions;
import static com.commercetools.sync.taxcategories.utils.TaxCategoryUpdateActionUtils.buildSetDescriptionAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
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
        final CountryCode countryCode = CountryCode.DE;

        final TaxRate taxRate1 = mock(TaxRate.class);
        when(taxRate1.getName()).thenReturn(taxRateName1);
        when(taxRate1.getId()).thenReturn("taxRateId1");
        when(taxRate1.getAmount()).thenReturn(1.0);
        when(taxRate1.getCountry()).thenReturn(countryCode);
        when(taxRate1.isIncludedInPrice()).thenReturn(false);
        final TaxRate taxRate2 = mock(TaxRate.class);
        when(taxRate2.getName()).thenReturn(taxRateName2);
        when(taxRate2.getId()).thenReturn("taxRateId2");
        when(taxRate2.getAmount()).thenReturn(2.0);
        when(taxRate2.getCountry()).thenReturn(countryCode);
        when(taxRate2.isIncludedInPrice()).thenReturn(false);

        final List<TaxRate> oldTaxRates = Arrays.asList(taxRate1, taxRate2);

        when(taxCategory.getTaxRates()).thenReturn(oldTaxRates);

        newSameTaxCategoryDraft = TaxCategoryDraftBuilder.of(name, Arrays.asList(
            TaxRateDraftBuilder
                .of(taxRateName1, 1.0, false, countryCode)
                .build(),
            TaxRateDraftBuilder
                .of(taxRateName2, 2.0, false, countryCode)
                .build()
        ), description).key(key).build();

        newDifferentTaxCategoryDraft = TaxCategoryDraftBuilder.of("changedName", Arrays.asList(
            // replace
            TaxRateDraftBuilder
                .of(taxRateName1, 2.0, false, countryCode)
                .build(),
            // remove - taxRateName2
            // add
            TaxRateDraftBuilder
                .of("taxRateName3", 3.0, false, countryCode)
                .build()
        ), "desc")
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
    void buildRatesUpdateActions_WithSameValues_ShouldNotBuildAction() {
        final List<UpdateAction<TaxCategory>> result = buildTaxRateUpdateActions(taxCategory, newSameTaxCategoryDraft,
            mock(TaxCategorySyncOptions.class));

        assertThat(result).isEmpty();
    }

    @Test
    void buildRatesUpdateActions_WithDifferentValues_ShouldReturnAction() {
        final List<UpdateAction<TaxCategory>> result = buildTaxRateUpdateActions(taxCategory,
            newDifferentTaxCategoryDraft, mock(TaxCategorySyncOptions.class));

        assertAll(
            () -> assertThat(result)
                .contains(AddTaxRate.of(newDifferentTaxCategoryDraft.getTaxRates().get(1))),
            () -> assertThat(result)
                .contains(ReplaceTaxRate.of("taxRateId1", newDifferentTaxCategoryDraft.getTaxRates().get(0))),
            () -> assertThat(result).contains(RemoveTaxRate.of("taxRateId2"))
        );
    }

    @Test
    void buildRatesUpdateActions_WithDuplicatedValues_ShouldNotBuildActionAndTriggerErrorCallback() {
        final String name = "DuplicatedName";
        newDifferentTaxCategoryDraft = TaxCategoryDraftBuilder.of(name, Arrays.asList(
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

        assertAll(
            () -> assertThat(result).isEmpty(),
            () -> assertThat(callback.get()).contains(name)
        );
    }

}
