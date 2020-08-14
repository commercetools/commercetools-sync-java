package com.commercetools.sync.taxcategories.utils;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxRate;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import io.sphere.sdk.taxcategories.commands.updateactions.AddTaxRate;
import io.sphere.sdk.taxcategories.commands.updateactions.RemoveTaxRate;
import io.sphere.sdk.taxcategories.commands.updateactions.ReplaceTaxRate;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
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
     * <p>Notes: The method will ignore/filter out:
     * <ul>
     *     <li>
     *       the {@code null} tax rates drafts from the passed {@code newTaxRateDrafts}.
     *     </li>
     *     <li>
     *       the duplicated (has same country code and state) tax rates drafts from the passed {@code newTaxRateDrafts}
     *       and will create update action only for the first tax rate draft in the {@code newTaxRateDrafts}.
     *     </li>
     * </ul>
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
     */
    @Nonnull
    private static List<UpdateAction<TaxCategory>> buildUpdateActions(
        @Nonnull final List<TaxRate> oldTaxRates,
        @Nonnull final List<TaxRateDraft> newTaxRatesDrafts) {

        List<TaxRateDraft> newTaxRateDraftsCopy = new ArrayList<>(newTaxRatesDrafts);

        final List<UpdateAction<TaxCategory>> updateActions =
            buildRemoveOrReplaceTaxRateUpdateActions(
                oldTaxRates,
                newTaxRateDraftsCopy
            );
        updateActions.addAll(
            buildAddTaxRateUpdateActions(
                oldTaxRates,
                newTaxRateDraftsCopy
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
     */
    @Nonnull
    private static List<UpdateAction<TaxCategory>> buildRemoveOrReplaceTaxRateUpdateActions(
        @Nonnull final List<TaxRate> oldTaxRates,
        @Nonnull final List<TaxRateDraft> newTaxRatesDrafts) {

        return oldTaxRates
            .stream()
            .map(oldTaxRate -> newTaxRatesDrafts
                .stream()
                .filter(taxRateDraft -> Objects.equals(oldTaxRate.getCountry(), taxRateDraft.getCountry()))
                .filter(taxRateDraft -> oldTaxRate.getState() == null
                    || (oldTaxRate.getState() != null && taxRateDraft.getState() == null)
                    || Objects.equals(oldTaxRate.getState(), taxRateDraft.getState()))
                .findFirst()
                .map(matchedTaxRateDraft ->  {
                    if (!hasSameFields(oldTaxRate, matchedTaxRateDraft)) {
                        newTaxRatesDrafts.remove(matchedTaxRateDraft);
                        return singletonList(ReplaceTaxRate.of(oldTaxRate.getId(), matchedTaxRateDraft));
                    } else {
                        return new ArrayList<UpdateAction<TaxCategory>>();
                    }
                })
                .orElseGet(() -> singletonList(RemoveTaxRate.of(oldTaxRate.getId()))))
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

        final Map<String, TaxRate> taxRateDraftMap = oldTaxRates
            .stream()
            .collect(toMap(taxRate -> getTaxRateDraftMapKey(taxRate.getCountry(), taxRate.getState()),
                taxRateDraft -> taxRateDraft));

        return newTaxRateDrafts
            .stream()
            .filter(taxRateDraft -> taxRateDraft.getCountry() != null
                && !taxRateDraftMap.containsKey(
                    getTaxRateDraftMapKey(taxRateDraft.getCountry(), taxRateDraft.getState())))
            .map(AddTaxRate::of)
            .collect(toList());
    }

    @Nonnull
    private static String getTaxRateDraftMapKey(final CountryCode countryCode, final String state) {
        return StringUtils.isEmpty(state)
            ? countryCode.toString()
            : String.format("%s_%s", countryCode, state);
    }

    private static boolean hasSameFields(@Nonnull final TaxRate oldTaxRate, @Nonnull final TaxRateDraft newTaxRate) {
        return TaxRateDraftBuilder.of(oldTaxRate).build().equals(newTaxRate);
    }
}
