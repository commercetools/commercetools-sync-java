package com.commercetools.sync.taxcategories.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateNameException;
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

import static java.lang.String.format;
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
     * @throws BuildUpdateActionException in case there are tax rates drafts with duplicate names or enums
     *                                    duplicate keys.
     */
    @Nonnull
    static List<UpdateAction<TaxCategory>> buildTaxRatesUpdateActions(
        @Nonnull final List<TaxRate> oldTaxRates,
        @Nonnull final List<TaxRateDraft> newTaxRatesDrafts)
        throws BuildUpdateActionException {

        if (!newTaxRatesDrafts.isEmpty()) {
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
     * @throws BuildUpdateActionException in case there are tax rates drafts with duplicate names.
     */
    @Nonnull
    private static List<UpdateAction<TaxCategory>> buildUpdateActions(
        @Nonnull final List<TaxRate> oldTaxRates,
        @Nonnull final List<TaxRateDraft> newTaxRatesDrafts)
        throws BuildUpdateActionException {

        try {
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
        } catch (final DuplicateNameException exception) {
            throw new BuildUpdateActionException(exception);
        }
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
     * @throws DuplicateNameException in case there are tax rates drafts with duplicate names.
     */
    @Nonnull
    private static List<UpdateAction<TaxCategory>> buildRemoveOrReplaceTaxRateUpdateActions(
        @Nonnull final List<TaxRate> oldTaxRates,
        @Nonnull final List<TaxRateDraft> newTaxRatesDrafts) {

        final Map<String, TaxRateDraft> newTaxRatesDraftsNameMap =
            newTaxRatesDrafts
                .stream().collect(
                toMap(TaxRateDraft::getName, taxRateDraft -> taxRateDraft,
                    (taxRateDraftA, taxRateDraftB) -> {
                        throw new DuplicateNameException(
                            format("Tax rates drafts have duplicated names. Duplicated tax rate "
                                    + "name: '%s'.  Tax rates names are expected to be unique "
                                    + "inside their tax category.",
                                taxRateDraftA.getName()));
                    }
                ));

        return oldTaxRates
            .stream()
            .map(oldTaxRate -> {
                final String oldTaxRateName = oldTaxRate.getName();
                final TaxRateDraft matchingNewTaxRateDraft = newTaxRatesDraftsNameMap.get(oldTaxRateName);
                return ofNullable(matchingNewTaxRateDraft)
                    .map(taxRateDraft -> {
                        if (!same(oldTaxRate, taxRateDraft)) {
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

        final Map<String, TaxRate> oldTaxRateNameMap = oldTaxRates.stream()
            .collect(toMap(TaxRate::getName, Function.identity()));

        return newTaxRateDrafts
            .stream()
            .filter(taxRateDraft -> !oldTaxRateNameMap.containsKey(taxRateDraft.getName()))
            .map(AddTaxRate::of)
            .collect(Collectors.toList());
    }

    private static boolean same(@Nonnull final TaxRate oldTaxRate, @Nonnull final TaxRateDraft newTaxRate) {
        return Objects.equals(oldTaxRate.getAmount(), newTaxRate.getAmount())
            && Objects.equals(oldTaxRate.getCountry(), newTaxRate.getCountry())
            && Objects.equals(oldTaxRate.getState(), newTaxRate.getState())
            && Objects.equals(oldTaxRate.isIncludedInPrice(), newTaxRate.isIncludedInPrice())
            && sameSubRates(oldTaxRate.getSubRates().stream().filter(Objects::nonNull).collect(toList()),
            newTaxRate.getSubRates().stream().filter(Objects::nonNull).collect(toList()));
    }

    private static boolean sameSubRates(final List<SubRate> oldSubRates, final List<SubRate> newSubRates) {
        boolean same = false;

        if (Objects.nonNull(oldSubRates) && Objects.nonNull(newSubRates)) {
            if (oldSubRates.size() == newSubRates.size()) {
                Map<String, SubRate> osr = oldSubRates.stream().collect(toMap(SubRate::getName, subRate -> subRate));
                Map<String, SubRate> nsr = newSubRates.stream().collect(toMap(SubRate::getName, subRate -> subRate));

                same = true;
                for (final Map.Entry<String, SubRate> entry : osr.entrySet()) {
                    if (!Objects.equals(entry.getValue(), nsr.get(entry.getKey()))) {
                        same = false;
                        break;
                    }
                }
            }
        }

        return same;
    }

}
