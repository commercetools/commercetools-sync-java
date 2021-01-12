package com.commercetools.sync.taxcategories.helpers;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class TaxCategoryBatchValidator
    extends BaseBatchValidator<
        TaxCategoryDraft, TaxCategorySyncOptions, TaxCategorySyncStatistics> {

  static final String TAX_CATEGORY_DRAFT_KEY_NOT_SET =
      "TaxCategoryDraft with name: %s doesn't have a key. "
          + "Please make sure all tax category drafts have keys.";
  static final String TAX_CATEGORY_DRAFT_IS_NULL = "TaxCategoryDraft is null.";
  static final String TAX_CATEGORY_DUPLICATED_COUNTRY =
      "Tax rate drafts have duplicated country "
          + "codes. Duplicated tax rate country code: '%s'. Tax rate country codes and "
          + "states are expected to be unique inside their tax category.";
  static final String TAX_CATEGORY_DUPLICATED_COUNTRY_AND_STATE =
      "Tax rate drafts have duplicated country "
          + "codes and states. Duplicated tax rate country code: '%s'. state : '%s'. Tax rate country codes and "
          + "states are expected to be unique inside their tax category.";

  public TaxCategoryBatchValidator(
      @Nonnull final TaxCategorySyncOptions syncOptions,
      @Nonnull final TaxCategorySyncStatistics syncStatistics) {
    super(syncOptions, syncStatistics);
  }

  /**
   * Given the {@link List}&lt;{@link TaxCategoryDraft}&gt; of drafts this method attempts to
   * validate drafts and return an {@link ImmutablePair}&lt;{@link Set}&lt;{@link
   * TaxCategoryDraft}&gt;,{@link Set}&lt; {@link String}&gt;&gt; which contains the {@link Set} of
   * valid drafts and valid tax category keys.
   *
   * <p>A valid tax category draft is one which satisfies the following conditions:
   *
   * <ol>
   *   <li>It is not null
   *   <li>It has a key which is not blank (null/empty)
   *   <li>Tax rates have not duplicated country and state.
   * </ol>
   *
   * @param taxCategoryDrafts the tax category drafts to validate and collect valid tax category
   *     keys.
   * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@link TaxCategoryDraft}&gt;, {@link
   *     Set}&lt;{@link String}&gt;&gt; which contains the {@link Set} of valid drafts and valid tax
   *     category keys.
   */
  @Override
  public ImmutablePair<Set<TaxCategoryDraft>, Set<String>> validateAndCollectReferencedKeys(
      @Nonnull final List<TaxCategoryDraft> taxCategoryDrafts) {

    final Set<TaxCategoryDraft> validDrafts =
        taxCategoryDrafts.stream().filter(this::isValidTaxCategoryDraft).collect(toSet());

    final Set<String> validKeys =
        validDrafts.stream().map(TaxCategoryDraft::getKey).collect(toSet());

    return ImmutablePair.of(validDrafts, validKeys);
  }

  private boolean isValidTaxCategoryDraft(@Nullable final TaxCategoryDraft taxCategoryDraft) {

    if (taxCategoryDraft == null) {
      handleError(TAX_CATEGORY_DRAFT_IS_NULL);
    } else if (isBlank(taxCategoryDraft.getKey())) {
      handleError(format(TAX_CATEGORY_DRAFT_KEY_NOT_SET, taxCategoryDraft.getName()));
    } else if (taxCategoryDraft.getTaxRates() != null
        && !taxCategoryDraft.getTaxRates().isEmpty()) {
      return validateIfDuplicateCountryAndState(taxCategoryDraft.getTaxRates());
    } else {
      return true;
    }

    return false;
  }

  private boolean validateIfDuplicateCountryAndState(final List<TaxRateDraft> taxRateDrafts) {
    /*
    For TaxRates uniqueness could be ensured by country code and states.
    So in tax category sync are using country code and states for matching.

    Representation of the commercetools platform error when country code is duplicated,
        {
            "statusCode": 400,
            "message": "A duplicate value '{\"country\":\"DE\"}' exists for field 'country'.",
            "errors": [
                {
                    "code": "DuplicateField",
                    ....
            ]
        }
    */
    Map<String, Map<String, Long>> map =
        taxRateDrafts.stream()
            .collect(
                Collectors.groupingBy(
                    draft -> Objects.toString(draft.getCountry(), ""),
                    Collectors.groupingBy(
                        draft -> Objects.toString(draft.getState(), ""), Collectors.counting())));

    for (Map.Entry<String, Map<String, Long>> countryEntry : map.entrySet()) {
      for (Map.Entry<String, Long> stateEntry : countryEntry.getValue().entrySet()) {
        if (stateEntry.getValue() > 1L) {
          String errorMessage =
              StringUtils.isBlank(stateEntry.getKey())
                  ? format(TAX_CATEGORY_DUPLICATED_COUNTRY, countryEntry.getKey())
                  : format(
                      TAX_CATEGORY_DUPLICATED_COUNTRY_AND_STATE,
                      countryEntry.getKey(),
                      stateEntry.getKey());
          handleError(new SyncException(errorMessage));
          return false;
        }
      }
    }

    return true;
  }
}
