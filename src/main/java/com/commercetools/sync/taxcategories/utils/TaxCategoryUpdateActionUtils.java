package com.commercetools.sync.taxcategories.utils;

import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.commands.updateactions.ChangeName;
import io.sphere.sdk.taxcategories.commands.updateactions.SetDescription;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.taxcategories.utils.TaxRatesUpdateActionUtils.buildTaxRatesUpdateActions;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

public final class TaxCategoryUpdateActionUtils {

    private TaxCategoryUpdateActionUtils() {
    }

    /**
     * Compares the {@code name} values of a {@link TaxCategory} and a {@link TaxCategoryDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeName"}
     * {@link UpdateAction}. If both {@link TaxCategory} and {@link TaxCategoryDraft} have the same
     * {@code name} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldTaxCategory the tax category that should be updated.
     * @param newTaxCategory the tax category draft which contains the new name.
     * @return optional containing update action or empty optional if names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<TaxCategory>> buildChangeNameAction(
        @Nonnull final TaxCategory oldTaxCategory,
        @Nonnull final TaxCategoryDraft newTaxCategory) {

        return buildUpdateAction(oldTaxCategory.getName(), newTaxCategory.getName(),
            () -> ChangeName.of(newTaxCategory.getName()));
    }

    /**
     * Compares the {@code description} values of a {@link TaxCategory} and a {@link TaxCategoryDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setDescription"}
     * {@link UpdateAction}. If both {@link TaxCategory} and {@link TaxCategoryDraft} have the same
     * {@code description} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldTaxCategory the tax category that should be updated.
     * @param newTaxCategory the tax category draft which contains the new description.
     * @return optional containing update action or empty optional if descriptions are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<TaxCategory>> buildSetDescriptionAction(
        @Nonnull final TaxCategory oldTaxCategory,
        @Nonnull final TaxCategoryDraft newTaxCategory) {

        return buildUpdateAction(oldTaxCategory.getDescription(), newTaxCategory.getDescription(),
            () -> SetDescription.of(newTaxCategory.getDescription()));
    }

    /**
     * Compares the tax rates of a {@link TaxCategory} and a {@link TaxCategoryDraft} and returns a list of
     * {@link UpdateAction}&lt;{@link TaxCategory}&gt; as a result. If both the {@link TaxCategory} and
     * the {@link TaxCategoryDraft} have identical tax rates, then no update action is needed and hence an empty
     * {@link List} is returned. In case, the new tax category draft has a list of attributes in which a duplicate name
     * exists, the error callback is triggered and an empty list is returned.
     *
     * @param oldTaxCategory the tax category which should be updated.
     * @param newTaxCategory the tax category draft where we get the key.
     * @param syncOptions    responsible for supplying the sync options to the sync utility method.
     *                       It is used for triggering the error callback within the utility, in case of
     *                       errors.
     * @return A list with the update actions or an empty list if the tax rates are identical.
     */
    @Nonnull
    public static List<UpdateAction<TaxCategory>> buildTaxRateUpdateActions(
        @Nonnull final TaxCategory oldTaxCategory,
        @Nonnull final TaxCategoryDraft newTaxCategory,
        @Nonnull final TaxCategorySyncOptions syncOptions) {

        try {
            return buildTaxRatesUpdateActions(
                oldTaxCategory.getTaxRates(),
                newTaxCategory.getTaxRates()
            );
        } catch (final BuildUpdateActionException exception) {
            syncOptions.applyErrorCallback(format("Failed to build update actions for the tax rates "
                    + "of the tax category with the key '%s'. Reason: %s", oldTaxCategory.getKey(), exception),
                exception);
            return emptyList();
        }
    }

}
