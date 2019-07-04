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
import static com.commercetools.sync.taxcategories.utils.TaxCategoryUpdateActionUtils.buildRatesUpdateActions;
import static com.commercetools.sync.taxcategories.utils.TaxCategoryUpdateActionUtils.buildSetDescriptionAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaxCategoryUpdateActionUtilsTest {

    private TaxCategory old;
    private TaxCategoryDraft newSame;
    private TaxCategoryDraft newDifferent;

    @BeforeEach
    void setup() {
        final String name = "test name";
        final String key = "test key";
        final String description = "test description";

        old = mock(TaxCategory.class);
        when(old.getName()).thenReturn(name);
        when(old.getKey()).thenReturn(key);
        when(old.getDescription()).thenReturn(description);

        final String taxRateName1 = "taxRateName1";
        final String taxRateName2 = "taxRateName2";
        final CountryCode countryCode = CountryCode.DE;

        TaxRate taxRate1 = mock(TaxRate.class);
        when(taxRate1.getName()).thenReturn(taxRateName1);
        when(taxRate1.getId()).thenReturn("taxRateId1");
        when(taxRate1.getAmount()).thenReturn(1.0);
        when(taxRate1.getCountry()).thenReturn(countryCode);
        when(taxRate1.isIncludedInPrice()).thenReturn(false);
        TaxRate taxRate2 = mock(TaxRate.class);
        when(taxRate2.getName()).thenReturn(taxRateName2);
        when(taxRate2.getId()).thenReturn("taxRateId2");
        when(taxRate2.getAmount()).thenReturn(2.0);
        when(taxRate2.getCountry()).thenReturn(countryCode);
        when(taxRate2.isIncludedInPrice()).thenReturn(false);

        List<TaxRate> oldTaxRates = Arrays.asList(taxRate1, taxRate2);

        when(old.getTaxRates()).thenReturn(oldTaxRates);

        newSame = TaxCategoryDraftBuilder.of(name, Arrays.asList(
            TaxRateDraftBuilder
                .of(taxRateName1, 1.0, false, countryCode)
                .build(),
            TaxRateDraftBuilder
                .of(taxRateName2, 2.0, false, countryCode)
                .build()
        ), description).key(key).build();

        newDifferent = TaxCategoryDraftBuilder.of("changedName", Arrays.asList(
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
        Optional<UpdateAction<TaxCategory>> result = buildChangeNameAction(old, newDifferent);

        assertAll(
            () -> assertThat(result).as("Should contain action of `ChangeName`")
                .containsInstanceOf(ChangeName.class),
            () -> assertThat(result).as("Should change to proper value")
                .contains(ChangeName.of(newDifferent.getName()))
        );
    }

    @Test
    void buildChangeNameAction_WithSameValues_ShouldReturnEmptyOptional() {
        Optional<UpdateAction<TaxCategory>> result = buildChangeNameAction(old, newSame);

        assertThat(result).as("There should be no action created").isEmpty();
    }

    @Test
    void buildSetDescriptionAction_WithDifferentValues_ShouldReturnAction() {
        Optional<UpdateAction<TaxCategory>> result = buildSetDescriptionAction(old, newDifferent);

        assertAll(
            () -> assertThat(result).as("Should contain action of `SetDescription`")
                .containsInstanceOf(SetDescription.class),
            () -> assertThat(result).as("Should change to proper value")
                .contains(SetDescription.of(newDifferent.getDescription()))
        );
    }

    @Test
    void buildSetDescriptionAction_WithSameValues_ShouldReturnEmptyOptional() {
        Optional<UpdateAction<TaxCategory>> result = buildSetDescriptionAction(old, newSame);

        assertThat(result).as("There should be no action created").isEmpty();
    }

    @Test
    void buildRatesUpdateActions_WithSameValues_ShouldNotBuildAction() {
        List<UpdateAction<TaxCategory>> result = buildRatesUpdateActions(old, newSame,
            mock(TaxCategorySyncOptions.class));

        assertThat(result).as("There should be no actions created").isEmpty();
    }

    @Test
    void buildRatesUpdateActions_WithDifferentValues_ShouldReturnAction() {
        List<UpdateAction<TaxCategory>> result = buildRatesUpdateActions(old, newDifferent,
            mock(TaxCategorySyncOptions.class));

        assertAll(
            () -> assertThat(result).as("Should add new role")
                .contains(AddTaxRate.of(newDifferent.getTaxRates().get(1))),
            () -> assertThat(result).as("Should replace old role")
                .contains(ReplaceTaxRate.of("taxRateId1", newDifferent.getTaxRates().get(0))),
            () -> assertThat(result).as("Should remove old role")
                .contains(RemoveTaxRate.of("taxRateId2"))
        );
    }

    @Test
    void buildRatesUpdateActions_WithDuplicatedValues_ShouldNotBuildActionAndTriggerErrorCallback() {
        String name = "DuplicatedName";
        newDifferent = TaxCategoryDraftBuilder.of(name, Arrays.asList(
            // replace
            TaxRateDraftBuilder.of(name, 2.0, false, CountryCode.DE).build(),
            TaxRateDraftBuilder.of(name, 3.0, false, CountryCode.DE).build()
        ), "desc").build();

        AtomicReference<String> callback = new AtomicReference<>(null);

        TaxCategorySyncOptions syncOptions = TaxCategorySyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((errorMsg, exception) -> callback.set(errorMsg))
            .build();

        List<UpdateAction<TaxCategory>> result = buildRatesUpdateActions(old, newDifferent, syncOptions);

        assertAll(
            () -> assertThat(result).as("There should be no actions created").isEmpty(),
            () -> assertThat(callback.get()).as("Error callback should be called").isNotNull(),
            () -> assertThat(callback.get()).as("Error message should contain faulty name").contains(name)
        );
    }

}
