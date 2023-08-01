package com.commercetools.sync.sdk2.taxcategories.utils;

import static com.commercetools.sync.sdk2.taxcategories.utils.TaxCategoryUpdateActionUtils.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.tax_category.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaxCategoryUpdateActionUtilsTest {

  private static final String OLD_NAME = "test name";
  private static final String OLD_KEY = "test key";
  private static final String OLD_DESCRIPTION = "test description";
  private static TaxCategory taxCategory;

  @BeforeEach
  void setup() {
    taxCategory = mock(TaxCategory.class);
    when(taxCategory.getName()).thenReturn(OLD_NAME);
    when(taxCategory.getKey()).thenReturn(OLD_KEY);
    when(taxCategory.getDescription()).thenReturn(OLD_DESCRIPTION);

    final String taxRateName1 = "taxRateName1";
    final String taxRateName2 = "taxRateName2";

    final TaxRate taxRate1 = mock(TaxRate.class);
    when(taxRate1.getName()).thenReturn(taxRateName1);
    when(taxRate1.getId()).thenReturn("taxRateId1");
    when(taxRate1.getAmount()).thenReturn(1.0);
    when(taxRate1.getCountry()).thenReturn("DE");
    when(taxRate1.getIncludedInPrice()).thenReturn(false);

    final TaxRate taxRate2 = mock(TaxRate.class);
    when(taxRate2.getName()).thenReturn(taxRateName2);
    when(taxRate2.getId()).thenReturn("taxRateId2");
    when(taxRate2.getAmount()).thenReturn(2.0);
    when(taxRate2.getCountry()).thenReturn("US");
    when(taxRate2.getIncludedInPrice()).thenReturn(false);

    final List<TaxRate> oldTaxRates = asList(taxRate1, taxRate2);

    when(taxCategory.getRates()).thenReturn(oldTaxRates);
  }

  @Test
  void buildChangeNameAction_WithDifferentValues_ShouldReturnAction() {
    final TaxCategoryDraft newDifferentTaxCategoryDraft =
        TaxCategoryDraftBuilder.of().name("newName").build();
    final Optional<TaxCategoryUpdateAction> result =
        buildChangeNameAction(taxCategory, newDifferentTaxCategoryDraft);

    assertThat(result)
        .contains(
            TaxCategoryChangeNameActionBuilder.of()
                .name(newDifferentTaxCategoryDraft.getName())
                .build());
  }

  @Test
  void buildChangeNameAction_WithSameValues_ShouldReturnEmptyOptional() {
    final TaxCategoryDraft newSameTaxCategoryDraft =
        TaxCategoryDraftBuilder.of().name(OLD_NAME).build();
    final Optional<TaxCategoryUpdateAction> result =
        buildChangeNameAction(taxCategory, newSameTaxCategoryDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetDescriptionAction_WithDifferentValues_ShouldReturnAction() {
    final TaxCategoryDraft newDifferentTaxCategoryDraft =
        TaxCategoryDraftBuilder.of().name(OLD_NAME).description("different description").build();
    final Optional<TaxCategoryUpdateAction> result =
        buildSetDescriptionAction(taxCategory, newDifferentTaxCategoryDraft);

    assertThat(result)
        .contains(
            TaxCategorySetDescriptionActionBuilder.of()
                .description(newDifferentTaxCategoryDraft.getDescription())
                .build());
  }

  @Test
  void buildSetDescriptionAction_WithSameValues_ShouldReturnEmptyOptional() {
    final TaxCategoryDraft newSameTaxCategoryDraft =
        TaxCategoryDraftBuilder.of().name(OLD_NAME).description(OLD_DESCRIPTION).build();
    final Optional<TaxCategoryUpdateAction> result =
        buildSetDescriptionAction(taxCategory, newSameTaxCategoryDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildRatesUpdateActions_OnlyWithNewRate_ShouldBuildOnlyAddTaxRateAction() {
    when(taxCategory.getRates()).thenReturn(emptyList());
    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("%5 DE")
            .amount(0.05)
            .includedInPrice(false)
            .country("DE")
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of().name(OLD_NAME).rates(taxRateDraft).key(OLD_KEY).build();

    final List<TaxCategoryUpdateAction> result =
        buildTaxRateUpdateActions(TaxCategoryUpdateActionUtilsTest.taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(
            singletonList(TaxCategoryAddTaxRateActionBuilder.of().taxRate(taxRateDraft).build()));
  }

  @Test
  void buildRatesUpdateActions_OnlyUpdatedTaxRate_ShouldBuildOnlyReplaceTaxRateAction() {
    TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate = mock(TaxRate.class);
    when(taxRate.getName()).thenReturn("19% DE");
    when(taxRate.getId()).thenReturn("taxRate-1");
    when(taxRate.getAmount()).thenReturn(0.19);
    when(taxRate.getCountry()).thenReturn("DE");
    when(taxRate.getIncludedInPrice()).thenReturn(false);

    when(taxCategory.getRates()).thenReturn(singletonList(taxRate));

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("%16 DE")
            .amount(0.16)
            .includedInPrice(false)
            .country("DE")
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("name")
            .rates(taxRateDraft)
            .description("desc")
            .key("tax-category-key")
            .build();

    final List<TaxCategoryUpdateAction> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(
            singletonList(
                TaxCategoryReplaceTaxRateActionBuilder.of()
                    .taxRateId("taxRate-1")
                    .taxRate(taxRateDraft)
                    .build()));
  }

  @Test
  void buildRatesUpdateActions_withoutNewTaxRate_ShouldBuildOnlyRemoveTaxRateAction() {
    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name(OLD_NAME)
            .rates(emptyList())
            .description(OLD_DESCRIPTION)
            .key(OLD_KEY)
            .build();

    final List<TaxCategoryUpdateAction> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(
            asList(
                TaxCategoryRemoveTaxRateActionBuilder.of().taxRateId("taxRateId1").build(),
                TaxCategoryRemoveTaxRateActionBuilder.of().taxRateId("taxRateId2").build()));
  }

  @Test
  void buildRatesUpdateActions_withDifferentTaxRate_ShouldBuildMixedActions() {
    final TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = mock(TaxRate.class);
    when(taxRate1.getName()).thenReturn("11% US");
    when(taxRate1.getId()).thenReturn("taxRate-1");
    when(taxRate1.getAmount()).thenReturn(0.11);
    when(taxRate1.getCountry()).thenReturn("US");
    when(taxRate1.getIncludedInPrice()).thenReturn(false);

    final TaxRate taxRate2 = mock(TaxRate.class);
    when(taxRate2.getName()).thenReturn("8% DE");
    when(taxRate2.getId()).thenReturn("taxRate-2");
    when(taxRate2.getAmount()).thenReturn(0.08);
    when(taxRate2.getCountry()).thenReturn("AT");
    when(taxRate2.getIncludedInPrice()).thenReturn(false);

    final TaxRate taxRate3 = mock(TaxRate.class);
    when(taxRate3.getName()).thenReturn("21% ES");
    when(taxRate3.getId()).thenReturn("taxRate-3");
    when(taxRate3.getAmount()).thenReturn(0.21);
    when(taxRate3.getCountry()).thenReturn("ES");
    when(taxRate3.getIncludedInPrice()).thenReturn(false);

    when(taxCategory.getRates()).thenReturn(asList(taxRate1, taxRate2, taxRate3));

    // Update: Price is included.
    final TaxRateDraft taxRateDraft1 =
        TaxRateDraftBuilder.of()
            .name("11% US")
            .amount(0.11)
            .includedInPrice(true)
            .country("US")
            .build();

    // taxRate-2 is removed.
    // new rate is added.
    final TaxRateDraft taxRateDraft4 =
        TaxRateDraftBuilder.of()
            .name("15% FR")
            .amount(0.15)
            .includedInPrice(false)
            .country("FR")
            .build();
    // same
    final TaxRateDraft taxRateDraft3 =
        TaxRateDraftBuilder.of()
            .name("21% ES")
            .amount(0.21)
            .includedInPrice(false)
            .country("ES")
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("name")
            .rates(taxRateDraft1, taxRateDraft4, taxRateDraft3)
            .description("desc")
            .key("tax-category-key")
            .build();

    final List<TaxCategoryUpdateAction> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(
            asList(
                TaxCategoryReplaceTaxRateActionBuilder.of()
                    .taxRateId("taxRate-1")
                    .taxRate(taxRateDraft1)
                    .build(),
                TaxCategoryRemoveTaxRateActionBuilder.of().taxRateId("taxRate-2").build(),
                TaxCategoryAddTaxRateActionBuilder.of().taxRate(taxRateDraft4).build()));
  }

  @Test
  void buildTaxRatesUpdateActions_WithOnlyDifferentSubRates_ShouldReturnOnlyReplaceAction() {

    final TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = mock(TaxRate.class);
    when(taxRate1.getName()).thenReturn("11% US");
    when(taxRate1.getState()).thenReturn("state");
    when(taxRate1.getId()).thenReturn("taxRate-1");
    when(taxRate1.getAmount()).thenReturn(0.11);
    when(taxRate1.getCountry()).thenReturn("US");
    when(taxRate1.getIncludedInPrice()).thenReturn(false);

    final SubRate oldSubRate1 = SubRateBuilder.of().name("subRate-1").amount(0.07).build();
    final SubRate oldSubRate2 = SubRateBuilder.of().name("subRate-2").amount(0.04).build();

    when(taxRate1.getSubRates()).thenReturn(asList(oldSubRate1, oldSubRate2));
    when(taxCategory.getRates()).thenReturn(singletonList(taxRate1));

    final SubRate subRate1 = SubRateBuilder.of().name("subRate-1").amount(0.06).build();
    final SubRate subRate2 = SubRateBuilder.of().name("subRate-2").amount(0.05).build();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("11% US")
            .amount(0.11)
            .includedInPrice(false)
            .country("US")
            .state("state")
            .subRates(asList(subRate1, subRate2))
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("name")
            .rates(taxRateDraft)
            .description("desc")
            .key("tax-category-key")
            .build();

    final List<TaxCategoryUpdateAction> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(
            singletonList(
                TaxCategoryReplaceTaxRateActionBuilder.of()
                    .taxRateId("taxRate-1")
                    .taxRate(taxRateDraft)
                    .build()));
  }

  @Test
  void buildTaxRatesUpdateActions_WithMoreSubRates_ShouldReturnOnlyReplaceAction() {

    final TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = buildTaxRate("state");
    when(taxCategory.getRates()).thenReturn(singletonList(taxRate1));

    final SubRate subRate1 = SubRateBuilder.of().name("subRate-1").amount(0.06).build();
    final SubRate subRate2 = SubRateBuilder.of().name("subRate-2").amount(0.05).build();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("11% US")
            .amount(0.11)
            .includedInPrice(false)
            .country("US")
            .state("state")
            .subRates(asList(subRate1, subRate2))
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("name")
            .rates(taxRateDraft)
            .description("desc")
            .key("tax-category-key")
            .build();

    final List<TaxCategoryUpdateAction> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(
            singletonList(
                TaxCategoryReplaceTaxRateActionBuilder.of()
                    .taxRateId("taxRate-1")
                    .taxRate(taxRateDraft)
                    .build()));
  }

  @Test
  void buildTaxRatesUpdateActions_WithSameTaxRateAndSubRates_ShouldNotBuildAnyAction() {

    final TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = buildTaxRate("state");
    when(taxCategory.getRates()).thenReturn(singletonList(taxRate1));

    final SubRate subRate1 = SubRateBuilder.of().name("subRate-1").amount(0.11).build();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("11% US")
            .amount(0.11)
            .includedInPrice(false)
            .country("US")
            .state("state")
            .subRates(subRate1)
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("name")
            .rates(taxRateDraft)
            .description("desc")
            .key("tax-category-key")
            .build();

    final List<TaxCategoryUpdateAction> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildTaxRatesUpdateActions_WithNullOldState_ShouldReturnOnlyReplaceAction() {

    final TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = buildTaxRate(null);
    when(taxCategory.getRates()).thenReturn(singletonList(taxRate1));

    final SubRate subRate1 = SubRateBuilder.of().name("subRate-1").amount(0.06).build();
    final SubRate subRate2 = SubRateBuilder.of().name("subRate-2").amount(0.05).build();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("11% US")
            .amount(0.11)
            .includedInPrice(false)
            .country("US")
            .state("state")
            .subRates(asList(subRate1, subRate2))
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("name")
            .rates(taxRateDraft)
            .description("desc")
            .key("tax-category-key")
            .build();

    final List<TaxCategoryUpdateAction> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(
            asList(
                TaxCategoryRemoveTaxRateActionBuilder.of().taxRateId("taxRate-1").build(),
                TaxCategoryAddTaxRateActionBuilder.of().taxRate(taxRateDraft).build()));
  }

  @Test
  void buildTaxRatesUpdateActions_WithRemovedState_ShouldReturnRemoveAndAddActions() {

    final TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = buildTaxRate("state");
    when(taxCategory.getRates()).thenReturn(singletonList(taxRate1));

    final SubRate subRate1 = SubRateBuilder.of().name("subRate-1").amount(0.05).build();
    final SubRate subRate2 = SubRateBuilder.of().name("subRate-2").amount(0.06).build();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("11% US")
            .amount(0.11)
            .includedInPrice(false)
            .country("US")
            .state(null)
            .subRates(asList(subRate1, subRate2))
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("name")
            .rates(taxRateDraft)
            .description("desc")
            .key("tax-category-key")
            .build();

    final List<TaxCategoryUpdateAction> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(
            asList(
                TaxCategoryRemoveTaxRateActionBuilder.of().taxRateId("taxRate-1").build(),
                TaxCategoryAddTaxRateActionBuilder.of().taxRate(taxRateDraft).build()));
  }

  @Test
  void WhenTaxRatesWithAndWithoutState_WithRemovedTaxRate_ShouldBuildAndReturnRemoveAction() {

    final TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = buildTaxRate("state");
    final TaxRate taxRate2 = buildTaxRate(null);
    when(taxCategory.getRates()).thenReturn(asList(taxRate1, taxRate2));

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name(taxRate2.getName())
            .amount(taxRate2.getAmount())
            .includedInPrice(taxRate2.getIncludedInPrice())
            .country(taxRate2.getCountry())
            .state(taxRate2.getState())
            .subRates(taxRate2.getSubRates())
            .build();

    // TaxCategoryDraft with only one taxRate
    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("name")
            .rates(taxRateDraft)
            .description("desc")
            .key("tax-category-key")
            .build();

    final List<TaxCategoryUpdateAction> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(
            asList(TaxCategoryRemoveTaxRateActionBuilder.of().taxRateId("taxRate-1").build()));
  }

  @Test
  void buildTaxRatesUpdateActions_WithRemovedCountryCode_ShouldReturnOnlyRemoveAction() {

    final TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = buildTaxRate("state");
    when(taxCategory.getRates()).thenReturn(singletonList(taxRate1));

    final SubRate subRate1 = SubRateBuilder.of().name("subRate-1").amount(0.06).build();
    final SubRate subRate2 = SubRateBuilder.of().name("subRate-2").amount(0.05).build();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("11% US")
            .amount(0.11)
            .includedInPrice(false)
            .country("US")
            .subRates(asList(subRate1, subRate2))
            .build();
    taxRateDraft.setCountry(null);

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("name")
            .rates(taxRateDraft)
            .description("desc")
            .key("tax-category-key")
            .build();

    final List<TaxCategoryUpdateAction> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(
            singletonList(
                TaxCategoryRemoveTaxRateActionBuilder.of().taxRateId("taxRate-1").build()));
  }

  @Test
  void buildTaxRatesUpdateActions_WithDuplicatedTaxRateDrafts_ShouldReturnOnlyFirstDraftInAction() {

    final TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = buildTaxRate("state");
    when(taxCategory.getRates()).thenReturn(singletonList(taxRate1));

    final SubRate subRate1 = SubRateBuilder.of().name("subRate-1").amount(0.06).build();
    final SubRate subRate2 = SubRateBuilder.of().name("subRate-2").amount(0.05).build();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("11% US")
            .amount(0.11)
            .includedInPrice(false)
            .country("US")
            .state("state")
            .subRates(asList(subRate1, subRate2))
            .build();

    final TaxRateDraft duplicatedCountryCodeAndState =
        TaxRateDraftBuilder.of()
            .name("12% US")
            .amount(0.12)
            .includedInPrice(false)
            .country("US")
            .state("state")
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("name")
            .rates(taxRateDraft, duplicatedCountryCodeAndState)
            .description("desc")
            .key("tax-category-key")
            .build();

    final List<TaxCategoryUpdateAction> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(
            singletonList(
                TaxCategoryReplaceTaxRateActionBuilder.of()
                    .taxRateId("taxRate-1")
                    .taxRate(taxRateDraft)
                    .build()));
  }

  @Test
  void
      buildTaxRatesUpdateActions_WithNewTaxRateDraftsWithSameCountryAndDifferentStates_ShouldCreateCorrectActions() {
    final TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = mock(TaxRate.class);
    when(taxRate1.getName()).thenReturn("11% US");
    when(taxRate1.getState()).thenReturn("state-1");
    when(taxRate1.getId()).thenReturn("taxRate-1");
    when(taxRate1.getAmount()).thenReturn(0.11);
    when(taxRate1.getCountry()).thenReturn("US");
    when(taxRate1.getIncludedInPrice()).thenReturn(false);

    final TaxRate taxRate2 = mock(TaxRate.class);
    when(taxRate2.getName()).thenReturn("12% US");
    when(taxRate2.getState()).thenReturn("state-2");
    when(taxRate2.getId()).thenReturn("taxRate-2");
    when(taxRate2.getAmount()).thenReturn(0.12);
    when(taxRate2.getCountry()).thenReturn("US");
    when(taxRate2.getIncludedInPrice()).thenReturn(false);

    final TaxRate taxRate4 = mock(TaxRate.class);
    when(taxRate4.getName()).thenReturn("14% US");
    when(taxRate4.getState()).thenReturn("state-4");
    when(taxRate4.getId()).thenReturn("taxRate-4");
    when(taxRate4.getAmount()).thenReturn(0.14);
    when(taxRate4.getCountry()).thenReturn("US");
    when(taxRate4.getIncludedInPrice()).thenReturn(false);

    when(taxCategory.getRates()).thenReturn(asList(taxRate1, taxRate2, taxRate4));

    final SubRate subRate3 = SubRateBuilder.of().name("subRate-3").amount(0.13).build();
    final TaxRateDraft taxRateDraft1 =
        TaxRateDraftBuilder.of()
            .name("11% US")
            .amount(0.11)
            .includedInPrice(false)
            .country("US")
            .state("state-1")
            .build();

    final TaxRateDraft taxRateDraft2 =
        TaxRateDraftBuilder.of()
            .name("12% US")
            .amount(0.12)
            .includedInPrice(false)
            .country("US")
            .state("state-2")
            .build();

    final TaxRateDraft newTaxRateDraft =
        TaxRateDraftBuilder.of()
            .name("13% US")
            .amount(0.13)
            .includedInPrice(false)
            .country("US")
            .state("state-3")
            .subRates(singletonList(subRate3))
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("name")
            .rates(taxRateDraft1, taxRateDraft2, newTaxRateDraft)
            .description("desc")
            .key("tax-category-key")
            .build();

    final List<TaxCategoryUpdateAction> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(
            asList(
                TaxCategoryRemoveTaxRateActionBuilder.of().taxRateId("taxRate-4").build(),
                TaxCategoryAddTaxRateActionBuilder.of().taxRate(newTaxRateDraft).build()));
  }

  private TaxRate buildTaxRate(String state) {
    final TaxRate taxRate = mock(TaxRate.class);
    when(taxRate.getName()).thenReturn("11% US");
    when(taxRate.getState()).thenReturn(state);
    when(taxRate.getId()).thenReturn("taxRate-1");
    when(taxRate.getAmount()).thenReturn(0.11);
    when(taxRate.getCountry()).thenReturn("US");
    when(taxRate.getIncludedInPrice()).thenReturn(false);

    final SubRate oldSubRate = SubRateBuilder.of().name("subRate-1").amount(0.11).build();

    when(taxRate.getSubRates()).thenReturn(singletonList(oldSubRate));
    return taxRate;
  }
}
