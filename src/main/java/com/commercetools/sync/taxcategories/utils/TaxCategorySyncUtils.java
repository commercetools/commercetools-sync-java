package com.commercetools.sync.taxcategories.utils;

import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.taxcategories.utils.TaxCategoryUpdateActionUtils.*;

import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.api.models.tax_category.TaxCategoryDraft;
import com.commercetools.api.models.tax_category.TaxCategoryUpdateAction;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

public final class TaxCategorySyncUtils {

  private TaxCategorySyncUtils() {}

  /**
   * Compares all the fields of a {@link TaxCategory} and a {@link TaxCategoryDraft}. It returns a
   * {@link List} of {@link TaxCategoryUpdateAction} as a result. If no update action is needed, for
   * example in case where both the {@link TaxCategory} and the {@link TaxCategoryDraft} have the
   * same fields, an empty {@link List} is returned.
   *
   * @param oldTaxCategory the {@link TaxCategory} which should be updated.
   * @param newTaxCategory the {@link TaxCategoryDraft} where we get the new data.
   * @return A list of tax category-specific update actions.
   */
  @Nonnull
  public static List<TaxCategoryUpdateAction> buildActions(
      @Nonnull final TaxCategory oldTaxCategory, @Nonnull final TaxCategoryDraft newTaxCategory) {

    final List<TaxCategoryUpdateAction> updateActions =
        new ArrayList<>(
            filterEmptyOptionals(
                buildChangeNameAction(oldTaxCategory, newTaxCategory),
                buildSetDescriptionAction(oldTaxCategory, newTaxCategory)));

    updateActions.addAll(buildTaxRateUpdateActions(oldTaxCategory, newTaxCategory));

    return updateActions;
  }
}
