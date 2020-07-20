package com.commercetools.sync.taxcategories.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateCountryCodeAndStateException;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.SubRate;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxRate;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.commands.updateactions.AddTaxRate;
import io.sphere.sdk.taxcategories.commands.updateactions.RemoveTaxRate;
import io.sphere.sdk.taxcategories.commands.updateactions.ReplaceTaxRate;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * This class is only meant for the internal use of the commercetools-sync-java library.
 */
final class TaxRatesUpdateActionUtils {

    private TaxRatesUpdateActionUtils() {
    }

    /**
     * Compares a list of {@link TaxRate}s with a list of {@link TaxRateDraft}s to
     * returns a {@link List} of {@link UpdateAction}&lt;{@link TaxCategory}&gt;. If both lists have identical
     * TaxRates, then no update actions are needed and hence an empty {@link List} is returned.
     *
     * <p>If the list of new {@link TaxRateDraft}s is empty, then remove actions are built for
     * every existing tax rate in the {@code oldTaxRates} list.
     *
     * <p>Note: The method will ignore/filter out {@code null} tax rates drafts from the passed
     * {@code newTaxRateDrafts}.</p>
     *
     * @param oldTaxRates       the old list of tax rates.
     * @param newTaxRatesDrafts the new list of tax rates drafts.
     * @return a list of tax rates update actions if the list of tax rates are not identical.
     *         Otherwise, if the tax rates are identical, an empty list is returned.

     */
    @Nonnull
    static List<UpdateAction<TaxCategory>> buildTaxRatesUpdateActions(
        @Nonnull final List<TaxRate> oldTaxRates, final List<TaxRateDraft> newTaxRatesDrafts) {

        if (newTaxRatesDrafts != null && !newTaxRatesDrafts.isEmpty()) {
            return buildUpdateActions(
                oldTaxRates,
                newTaxRatesDrafts.stream().filter(Objects::nonNull).collect(toList())
            );
        } else {
            return oldTaxRates
                .stream()
                .map(TaxRate::getId)
                .filter(Objects::nonNull)
                .map(RemoveTaxRate::of)
                .collect(Collectors.toList());
        }
    }

    /**
     * Compares a list of {@link TaxRate}s with a list of {@link TaxRateDraft}s.
     * The method serves as an implementation for tax rates syncing. The method takes in functions
     * for building the required update actions (AddTaxRate, ReplaceTaxRate, RemoveTaxRate) for the required
     * resource.
     *
     * @param oldTaxRates       the old list of tax rates.
     * @param newTaxRatesDrafts the new list of tax rates drafts.
     * @return a list of tax rates update actions if the list of tax rates is not identical.
     *         Otherwise, if the tax rates are identical, an empty list is returned.
     * @throws BuildUpdateActionException in case there are runtime exception occurs.
     */
    @Nonnull
    private static List<UpdateAction<TaxCategory>> buildUpdateActions(
        @Nonnull final List<TaxRate> oldTaxRates,
        @Nonnull final List<TaxRateDraft> newTaxRatesDrafts) {


        final List<UpdateAction<TaxCategory>> updateActions =
            buildRemoveOrReplaceTaxRateUpdateActions(
                oldTaxRates,
                newTaxRatesDrafts
            );
        updateActions.addAll(
            buildAddTaxRateUpdateActions(
                oldTaxRates,
                newTaxRatesDrafts
            )
        );
        return updateActions;
    }

    /**
     * Checks if there are any tax rates which are not existing in the {@code newTaxRatesDrafts}.
     * If there are, then "remove" tax rate update actions are built.
     * Otherwise, if the tax rate still exists in the new draft, then compare the tax rate
     * fields (amount, country, state, etc..), and add the computed actions to the list of update actions.
     *
     * @param oldTaxRates       the list of old {@link TaxRate}s.
     * @param newTaxRatesDrafts the list of new {@link TaxRateDraft}s.
     * @return a list of tax rate update actions if there are tax rates that are not existing
     *         in the new draft. If the tax rate still exists in the new draft, then compare the fields, and add
     *         the computed actions to the list of update actions.
     *         Otherwise, if the tax rates are identical, an empty list is returned.
     * @throws DuplicateCountryCodeAndStateException in case there are tax rates drafts with duplicate country codes.
     */
    @Nonnull
    private static List<UpdateAction<TaxCategory>> buildRemoveOrReplaceTaxRateUpdateActions(
        @Nonnull final List<TaxRate> oldTaxRates,
        @Nonnull final List<TaxRateDraft> newTaxRatesDrafts) {

        /*
        For TaxRates only unique field is country code. So we are using country code for matching.
        Representation of CTP error,
            {
                "statusCode": 400,
                "message": "A duplicate value '{\"country\":\"DE\"}' exists for field 'country'.",
                "errors": [
                    {
                        "code": "DuplicateField",
                        ....
                ]
            }
        * */
        final Map<String, TaxRateDraft> taxRateDraftCountryStateMap = newTaxRatesDrafts
                .stream()
                .collect(
                    toMap(taxRateDraft -> taxRateDraft.getCountry() + "_" + taxRateDraft.getState(),
                        taxRateDraft -> taxRateDraft));

        return oldTaxRates
            .stream()
            .map(oldTaxRate -> {
                final CountryCode oldTaxRateCountryCode = oldTaxRate.getCountry();
                final String oldTaxRateState = oldTaxRate.getState();
                final TaxRateDraft matchingNewTaxRateDraft = taxRateDraftCountryStateMap
                        .get(oldTaxRateCountryCode + "_" + oldTaxRateState);
                return ofNullable(matchingNewTaxRateDraft)
                    .map(taxRateDraft -> {
                        if (!hasSameFields(oldTaxRate, taxRateDraft)) {
                            return singletonList(ReplaceTaxRate.of(oldTaxRate.getId(), taxRateDraft));
                        } else {
                            return new ArrayList<UpdateAction<TaxCategory>>();
                        }
                    })
                    .orElseGet(() -> singletonList(RemoveTaxRate.of(oldTaxRate.getId())));
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    /**
     * Checks if there are any new tax rate drafts which are not existing in the {@code oldTaxRates}.
     * If there are, then "add" tax rate update actions are built.
     * Otherwise, if there are no new tax rates, then an empty list is returned.
     *
     * @param oldTaxRates      the list of old {@link TaxRate}s.
     * @param newTaxRateDrafts the list of new {@link TaxRateDraft}s.
     * @return a list of tax rate update actions if there are new tax rate that should be added.
     *         Otherwise, if the tax rates are identical, an empty list is returned.
     */
    @Nonnull
    private static List<UpdateAction<TaxCategory>> buildAddTaxRateUpdateActions(
        @Nonnull final List<TaxRate> oldTaxRates,
        @Nonnull final List<TaxRateDraft> newTaxRateDrafts) {

        final Map<CountryCode, TaxRate> countryCodeTaxRateMap = oldTaxRates
            .stream()
            .collect(toMap(TaxRate::getCountry, Function.identity()));

        return newTaxRateDrafts
            .stream()
            .filter(taxRateDraft -> !countryCodeTaxRateMap.containsKey(taxRateDraft.getCountry()))
            .map(AddTaxRate::of)
            .collect(Collectors.toList());
    }

    private static boolean hasSameFields(@Nonnull final TaxRate oldTaxRate, @Nonnull final TaxRateDraft newTaxRate) {
        return Objects.equals(oldTaxRate.getAmount(), newTaxRate.getAmount())
            && Objects.equals(oldTaxRate.getName(), newTaxRate.getName())
            && Objects.equals(oldTaxRate.getState(), newTaxRate.getState())
            && Objects.equals(oldTaxRate.isIncludedInPrice(), newTaxRate.isIncludedInPrice())
            && sameSubRates(oldTaxRate.getSubRates().stream().filter(Objects::nonNull).collect(toList()),
            newTaxRate.getSubRates().stream().filter(Objects::nonNull).collect(toList()));
    }

    private static boolean sameSubRates(final List<SubRate> oldSubRates, final List<SubRate> newSubRates) {
        if (Objects.nonNull(oldSubRates) && Objects.nonNull(newSubRates)) {
            if (oldSubRates.size() == newSubRates.size()) {
                return newSubRates.stream().allMatch(newSubRateItem -> {
                    return oldSubRates.stream().anyMatch(oldSubRateItem -> {
                        return Objects.toString(newSubRateItem.getName(), "").trim()
                                    .equals(Objects.toString(oldSubRateItem.getName(), "").trim())
                                     && (newSubRateItem.getAmount() == null ? 0 : newSubRateItem.getAmount())
                                     == (oldSubRateItem.getAmount() == null ? 0 : oldSubRateItem.getAmount());
                    });
                });
            }
        }
        return false;
    }
}
