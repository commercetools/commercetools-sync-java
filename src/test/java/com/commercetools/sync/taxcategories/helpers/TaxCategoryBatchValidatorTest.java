package com.commercetools.sync.taxcategories.helpers;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.tax_category.TaxCategoryDraft;
import com.commercetools.api.models.tax_category.TaxCategoryDraftBuilder;
import com.commercetools.api.models.tax_category.TaxRateDraft;
import com.commercetools.api.models.tax_category.TaxRateDraftBuilder;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptionsBuilder;
import com.neovisionaries.i18n.CountryCode;
import java.util.*;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaxCategoryBatchValidatorTest {
  private List<String> errorCallBackMessages;
  private TaxCategorySyncOptions syncOptions;
  private TaxCategorySyncStatistics syncStatistics;

  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);
    syncOptions =
        TaxCategorySyncOptionsBuilder.of(ctpClient)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                })
            .build();
    syncStatistics = new TaxCategorySyncStatistics();
  }

  @Test
  void validateAndCollectReferencedKeys_WithEmptyDraft_ShouldHaveEmptyResult() {
    final Set<TaxCategoryDraft> validDrafts = getValidDrafts(emptyList());

    assertThat(validDrafts).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithNullTaxCategoryDraft_ShouldHaveValidationErrorAndEmptyResult() {
    final Set<TaxCategoryDraft> validDrafts = getValidDrafts(Collections.singletonList(null));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(TaxCategoryBatchValidator.TAX_CATEGORY_DRAFT_IS_NULL);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithTaxCategoryDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
    final TaxCategoryDraft taxCategoryDraft = mock(TaxCategoryDraft.class);
    final Set<TaxCategoryDraft> validDrafts =
        getValidDrafts(Collections.singletonList(taxCategoryDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                TaxCategoryBatchValidator.TAX_CATEGORY_DRAFT_KEY_NOT_SET,
                taxCategoryDraft.getName()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithTaxCategoryDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
    final TaxCategoryDraft taxCategoryDraft = mock(TaxCategoryDraft.class);
    when(taxCategoryDraft.getKey()).thenReturn(EMPTY);
    final Set<TaxCategoryDraft> validDrafts =
        getValidDrafts(Collections.singletonList(taxCategoryDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                TaxCategoryBatchValidator.TAX_CATEGORY_DRAFT_KEY_NOT_SET,
                taxCategoryDraft.getName()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void validateAndCollectReferencedKeys_WithMixOfValidAndInvalidDrafts_ShouldValidateCorrectly() {
    final TaxCategoryDraft validDraft =
        TaxCategoryDraftBuilder.of()
            .name("foo")
            .rates(
                TaxRateDraftBuilder.of()
                    .name("foo")
                    .amount(2.0)
                    .includedInPrice(false)
                    .country(CountryCode.FR.getAlpha2())
                    .state("PARIS")
                    .build())
            .description("desc")
            .key("foo")
            .build();

    final TaxCategoryDraft withEmptyKey =
        TaxCategoryDraftBuilder.of().name("foo").rates(emptyList()).key("").build();

    final TaxCategoryDraft withNullKey =
        TaxCategoryDraftBuilder.of().name("foo").rates(emptyList()).key(null).build();

    final TaxRateDraft taxRateGermanyBerlin =
        TaxRateDraftBuilder.of()
            .name("foo")
            .amount(2.0)
            .includedInPrice(false)
            .country(CountryCode.DE.getAlpha2())
            .state("BERLIN")
            .build();

    final TaxCategoryDraft withDuplicatedState =
        TaxCategoryDraftBuilder.of()
            .name("foo")
            .rates(
                TaxRateDraftBuilder.of()
                    .name("foo")
                    .amount(2.0)
                    .includedInPrice(false)
                    .country(CountryCode.FR.getAlpha2())
                    .state("LYON")
                    .build(),
                TaxRateDraftBuilder.of()
                    .name("foo")
                    .amount(2.0)
                    .includedInPrice(false)
                    .country(CountryCode.FR.getAlpha2())
                    .state("PARIS")
                    .build(),
                taxRateGermanyBerlin,
                taxRateGermanyBerlin)
            .description("desc")
            .key("duplicatedState")
            .build();

    final TaxRateDraft taxRateFranceBerlin = TaxRateDraft.deepCopy(taxRateGermanyBerlin);
    taxRateFranceBerlin.setCountry(CountryCode.ES.getAlpha2());
    final TaxCategoryDraft withSameStateButDifferenCountry =
        TaxCategoryDraftBuilder.of()
            .name("foo")
            .rates(taxRateGermanyBerlin, taxRateFranceBerlin)
            .description("desc")
            .key("sameStateDifferentCountry")
            .build();

    final TaxRateDraft taxRateFrance =
        TaxRateDraftBuilder.of()
            .name("foo")
            .amount(2.0)
            .includedInPrice(false)
            .country(CountryCode.FR.getAlpha2())
            .build();

    final TaxCategoryDraft withDuplicatedCountry =
        TaxCategoryDraftBuilder.of()
            .name("foo")
            .rates(taxRateFrance, taxRateFrance)
            .description("desc")
            .key("duplicatedCountry")
            .build();

    final TaxRateDraft taxRateMissingCountry = TaxRateDraft.deepCopy(taxRateFrance);
    taxRateMissingCountry.setCountry(null);
    final TaxCategoryDraft withMissingCountry =
        TaxCategoryDraftBuilder.of()
            .name("foo")
            .rates(taxRateFrance)
            .description("desc")
            .key("missingCountry")
            .build();

    final TaxCategoryBatchValidator batchValidator =
        new TaxCategoryBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<TaxCategoryDraft>, Set<String>> pair =
        batchValidator.validateAndCollectReferencedKeys(
            Arrays.asList(
                null,
                validDraft,
                withEmptyKey,
                withNullKey,
                withDuplicatedState,
                withDuplicatedCountry,
                withMissingCountry,
                withSameStateButDifferenCountry));

    assertThat(pair.getLeft())
        .containsExactlyInAnyOrder(validDraft, withMissingCountry, withSameStateButDifferenCountry);
    assertThat(pair.getRight())
        .containsExactlyInAnyOrder("foo", "missingCountry", "sameStateDifferentCountry");

    assertThat(errorCallBackMessages).hasSize(5);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(TaxCategoryBatchValidator.TAX_CATEGORY_DRAFT_IS_NULL);
    assertThat(errorCallBackMessages.get(1))
        .isEqualTo(String.format(TaxCategoryBatchValidator.TAX_CATEGORY_DRAFT_KEY_NOT_SET, "foo"));
    assertThat(errorCallBackMessages.get(2))
        .isEqualTo(String.format(TaxCategoryBatchValidator.TAX_CATEGORY_DRAFT_KEY_NOT_SET, "foo"));
    assertThat(errorCallBackMessages.get(3))
        .isEqualTo(
            String.format(
                TaxCategoryBatchValidator.TAX_CATEGORY_DUPLICATED_COUNTRY_AND_STATE,
                CountryCode.DE,
                "BERLIN"));
    assertThat(errorCallBackMessages.get(4))
        .isEqualTo(
            String.format(
                TaxCategoryBatchValidator.TAX_CATEGORY_DUPLICATED_COUNTRY, CountryCode.FR));
  }

  @Nonnull
  private Set<TaxCategoryDraft> getValidDrafts(
      @Nonnull final List<TaxCategoryDraft> taxCategoryDrafts) {
    final TaxCategoryBatchValidator batchValidator =
        new TaxCategoryBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<TaxCategoryDraft>, Set<String>> pair =
        batchValidator.validateAndCollectReferencedKeys(taxCategoryDrafts);
    return pair.getLeft();
  }
}
