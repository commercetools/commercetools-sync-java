package com.commercetools.sync.taxcategories.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateNameException;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.SubRate;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxRate;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import io.sphere.sdk.taxcategories.commands.updateactions.AddTaxRate;
import io.sphere.sdk.taxcategories.commands.updateactions.RemoveTaxRate;
import io.sphere.sdk.taxcategories.commands.updateactions.ReplaceTaxRate;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaxRatesUpdateActionUtilsTest {

    private static final Random random = new Random();

    private static final String NAME = "Test old category";
    private static final String ID = RandomStringUtils.randomAlphabetic(15);
    private static final Double AMOUNT = random.nextDouble();
    private static final CountryCode COUNTRY_CODE = CountryCode.DE;
    private static final String STATE = RandomStringUtils.randomAlphabetic(15);
    private static final Boolean INCLUDED_IN_PRICE = random.nextBoolean();
    private List<TaxRate> oldTaxRates;

    private static final String SUB_RATE_NAME_1 = RandomStringUtils.randomAlphabetic(15);
    private static final Double SUB_RATE_AMOUNT_1 = random.nextDouble();

    private static final String SUB_RATE_NAME_2 = RandomStringUtils.randomAlphabetic(15);
    private static final Double SUB_RATE_AMOUNT_2 = random.nextDouble();

    @BeforeEach
    void setup() {
        TaxRate oldTaxRate = mock(TaxRate.class);
        when(oldTaxRate.getName()).thenReturn(NAME);
        when(oldTaxRate.getId()).thenReturn(ID);
        when(oldTaxRate.getAmount()).thenReturn(AMOUNT);
        when(oldTaxRate.getCountry()).thenReturn(COUNTRY_CODE);
        when(oldTaxRate.getState()).thenReturn(STATE);
        when(oldTaxRate.isIncludedInPrice()).thenReturn(INCLUDED_IN_PRICE);

        SubRate rate1 = SubRate.of(SUB_RATE_NAME_1, SUB_RATE_AMOUNT_1);
        SubRate rate2 = SubRate.of(SUB_RATE_NAME_2, SUB_RATE_AMOUNT_2);
        when(oldTaxRate.getSubRates()).thenReturn(Arrays.asList(rate1, rate2));

        oldTaxRates = singletonList(oldTaxRate);
    }

    @Test
    void buildTaxRatesUpdateActions_WithNewTaxRatesSetToNull_ShouldReturnOnlyRemovalActions()
        throws BuildUpdateActionException {
        List<UpdateAction<TaxCategory>> updateActions = TaxRatesUpdateActionUtils
            .buildTaxRatesUpdateActions(oldTaxRates, null);

        assertAll(
            () -> assertThat(updateActions).as("Update actions should not be empty").isNotEmpty(),
            () -> assertThat(updateActions).as("Update actions should contain only RemoveTaxRate")
                .allMatch(ac -> ac instanceof RemoveTaxRate),
            () -> assertThat(updateActions).as("Update actions should contain proper RemoveTaxRate")
                .contains(RemoveTaxRate.of(ID))
        );
    }

    @Test
    void buildTaxRatesUpdateActions_WithSameValues_ShouldNotBuildAction() throws BuildUpdateActionException {
        SubRate rate1 = SubRate.of(SUB_RATE_NAME_1, SUB_RATE_AMOUNT_1);
        SubRate rate2 = SubRate.of(SUB_RATE_NAME_2, SUB_RATE_AMOUNT_2);

        List<TaxRateDraft> newTaxRates = singletonList(
            TaxRateDraftBuilder.of(NAME, AMOUNT, INCLUDED_IN_PRICE, COUNTRY_CODE)
                .state(STATE)
                .subRates(Arrays.asList(rate2, rate1)).build());

        List<UpdateAction<TaxCategory>> updateActions = TaxRatesUpdateActionUtils
            .buildTaxRatesUpdateActions(oldTaxRates, newTaxRates);

        assertThat(updateActions).as("Update actions should be empty").isEmpty();
    }

    @Test
    void buildTaxRatesUpdateActions_WithDuplicatedValues_ShouldThrowDuplicateNameException() {
        List<TaxRateDraft> newTaxRates = Arrays.asList(
            TaxRateDraftBuilder.of(NAME, AMOUNT, INCLUDED_IN_PRICE, COUNTRY_CODE).build(),
            TaxRateDraftBuilder.of(NAME, AMOUNT, INCLUDED_IN_PRICE, COUNTRY_CODE).build());

        assertThatExceptionOfType(BuildUpdateActionException.class)
            .as("Should throw exception because duplicate name")
            .isThrownBy(() -> TaxRatesUpdateActionUtils
                .buildTaxRatesUpdateActions(oldTaxRates, newTaxRates))
            .withCauseInstanceOf(DuplicateNameException.class)
            .withMessageContaining(NAME);
    }

    @Test
    void buildTaxRatesUpdateActions_WithDifferentValues_ShouldReturnReplaceAction() throws BuildUpdateActionException {
        SubRate rate1 = SubRate.of(SUB_RATE_NAME_1, SUB_RATE_AMOUNT_1);
        SubRate rate2 = SubRate.of(SUB_RATE_NAME_2, random.nextDouble());

        List<TaxRateDraft> newTaxRates = singletonList(
            TaxRateDraftBuilder.of(NAME, AMOUNT, INCLUDED_IN_PRICE, COUNTRY_CODE)
                .state(STATE)
                .subRates(Arrays.asList(rate1, rate2)).build());

        List<UpdateAction<TaxCategory>> updateActions = TaxRatesUpdateActionUtils
            .buildTaxRatesUpdateActions(oldTaxRates, newTaxRates);

        assertAll(
            () -> assertThat(updateActions).as("Update actions should not be empty").isNotEmpty(),
            () -> assertThat(updateActions).as("Update actions should contain only ReplaceTaxRate")
                .allMatch(ac -> ac instanceof ReplaceTaxRate),
            () -> assertThat(updateActions).as("Update actions should contain proper ReplaceTaxRate")
                .contains(ReplaceTaxRate.of(ID, newTaxRates.get(0)))
        );
    }

    @Test
    void buildTaxRatesUpdateActions_WithNewValues_ShouldReturnAddAction() throws BuildUpdateActionException {
        List<TaxRateDraft> newTaxRates = singletonList(
            TaxRateDraftBuilder.of(NAME, AMOUNT, INCLUDED_IN_PRICE, COUNTRY_CODE).build());

        List<UpdateAction<TaxCategory>> updateActions = TaxRatesUpdateActionUtils
            .buildTaxRatesUpdateActions(emptyList(), newTaxRates);

        assertAll(
            () -> assertThat(updateActions).as("Update actions should not be empty").isNotEmpty(),
            () -> assertThat(updateActions).as("Update actions should contain only AddTaxRate")
                .allMatch(ac -> ac instanceof AddTaxRate),
            () -> assertThat(updateActions).as("Update actions should contain proper AddTaxRate")
                .contains(AddTaxRate.of(newTaxRates.get(0)))
        );
    }

}
