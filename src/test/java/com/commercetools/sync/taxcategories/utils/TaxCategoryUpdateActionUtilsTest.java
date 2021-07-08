package com.commercetools.sync.taxcategories.utils;

import static com.commercetools.sync.taxcategories.utils.TaxCategoryUpdateActionUtils.buildChangeNameAction;
import static com.commercetools.sync.taxcategories.utils.TaxCategoryUpdateActionUtils.buildSetDescriptionAction;
import static com.commercetools.sync.taxcategories.utils.TaxCategoryUpdateActionUtils.buildTaxRateUpdateActions;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.SubRate;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.TaxRate;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import io.sphere.sdk.taxcategories.commands.updateactions.AddTaxRate;
import io.sphere.sdk.taxcategories.commands.updateactions.ChangeName;
import io.sphere.sdk.taxcategories.commands.updateactions.RemoveTaxRate;
import io.sphere.sdk.taxcategories.commands.updateactions.ReplaceTaxRate;
import io.sphere.sdk.taxcategories.commands.updateactions.SetDescription;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaxCategoryUpdateActionUtilsTest {

  private TaxCategory taxCategory;
  private TaxCategoryDraft newSameTaxCategoryDraft;
  private TaxCategoryDraft newDifferentTaxCategoryDraft;

  @BeforeEach
  void setup() {
    final String name = "test name";
    final String key = "test key";
    final String description = "test description";

    taxCategory = mock(TaxCategory.class);
    when(taxCategory.getName()).thenReturn(name);
    when(taxCategory.getKey()).thenReturn(key);
    when(taxCategory.getDescription()).thenReturn(description);

    final String taxRateName1 = "taxRateName1";
    final String taxRateName2 = "taxRateName2";

    final TaxRate taxRate1 = mock(TaxRate.class);
    when(taxRate1.getName()).thenReturn(taxRateName1);
    when(taxRate1.getId()).thenReturn("taxRateId1");
    when(taxRate1.getAmount()).thenReturn(1.0);
    when(taxRate1.getCountry()).thenReturn(CountryCode.DE);
    when(taxRate1.isIncludedInPrice()).thenReturn(false);
    final TaxRate taxRate2 = mock(TaxRate.class);
    when(taxRate2.getName()).thenReturn(taxRateName2);
    when(taxRate2.getId()).thenReturn("taxRateId2");
    when(taxRate2.getAmount()).thenReturn(2.0);
    when(taxRate2.getCountry()).thenReturn(CountryCode.US);
    when(taxRate2.isIncludedInPrice()).thenReturn(false);

    final List<TaxRate> oldTaxRates = asList(taxRate1, taxRate2);

    when(taxCategory.getTaxRates()).thenReturn(oldTaxRates);

    newSameTaxCategoryDraft =
        TaxCategoryDraftBuilder.of(name, emptyList(), description).key(key).build();

    newDifferentTaxCategoryDraft =
        TaxCategoryDraftBuilder.of("changedName", emptyList(), "desc").key(key).build();
  }

  @Test
  void buildChangeNameAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<UpdateAction<TaxCategory>> result =
        buildChangeNameAction(taxCategory, newDifferentTaxCategoryDraft);

    assertThat(result).contains(ChangeName.of(newDifferentTaxCategoryDraft.getName()));
  }

  @Test
  void buildChangeNameAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<UpdateAction<TaxCategory>> result =
        buildChangeNameAction(taxCategory, newSameTaxCategoryDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetDescriptionAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<UpdateAction<TaxCategory>> result =
        buildSetDescriptionAction(taxCategory, newDifferentTaxCategoryDraft);

    assertThat(result).contains(SetDescription.of(newDifferentTaxCategoryDraft.getDescription()));
  }

  @Test
  void buildSetDescriptionAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<UpdateAction<TaxCategory>> result =
        buildSetDescriptionAction(taxCategory, newSameTaxCategoryDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildRatesUpdateActions_OnlyWithNewRate_ShouldBuildOnlyAddTaxRateAction() {
    TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");

    TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("%5 DE", 0.05, false, CountryCode.DE).build();

    TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of(null, singletonList(taxRateDraft), null)
            .key("tax-category-key")
            .build();

    final List<UpdateAction<TaxCategory>> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result).isEqualTo(singletonList(AddTaxRate.of(taxRateDraft)));
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
    when(taxRate.getCountry()).thenReturn(CountryCode.DE);
    when(taxRate.isIncludedInPrice()).thenReturn(false);

    when(taxCategory.getTaxRates()).thenReturn(singletonList(taxRate));

    TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("%16 DE", 0.16, false, CountryCode.DE).build();

    TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of("name", singletonList(taxRateDraft), "desc")
            .key("tax-category-key")
            .build();

    final List<UpdateAction<TaxCategory>> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result).isEqualTo(singletonList(ReplaceTaxRate.of("taxRate-1", taxRateDraft)));
  }

  @Test
  void buildRatesUpdateActions_withoutNewTaxRate_ShouldBuildOnlyRemoveTaxRateAction() {
    TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate = mock(TaxRate.class);
    when(taxRate.getName()).thenReturn("19% DE");
    when(taxRate.getId()).thenReturn("taxRate-1");
    when(taxRate.getAmount()).thenReturn(0.19);
    when(taxRate.getCountry()).thenReturn(CountryCode.DE);
    when(taxRate.isIncludedInPrice()).thenReturn(false);

    when(taxCategory.getTaxRates()).thenReturn(singletonList(taxRate));

    TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of("name", emptyList(), "desc").key("tax-category-key").build();

    final List<UpdateAction<TaxCategory>> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result).isEqualTo(singletonList(RemoveTaxRate.of("taxRate-1")));
  }

  @Test
  void buildRatesUpdateActions_withDifferentTaxRate_ShouldBuildMixedActions() {
    TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = mock(TaxRate.class);
    when(taxRate1.getName()).thenReturn("11% US");
    when(taxRate1.getId()).thenReturn("taxRate-1");
    when(taxRate1.getAmount()).thenReturn(0.11);
    when(taxRate1.getCountry()).thenReturn(CountryCode.US);
    when(taxRate1.isIncludedInPrice()).thenReturn(false);

    final TaxRate taxRate2 = mock(TaxRate.class);
    when(taxRate2.getName()).thenReturn("8% DE");
    when(taxRate2.getId()).thenReturn("taxRate-2");
    when(taxRate2.getAmount()).thenReturn(0.08);
    when(taxRate2.getCountry()).thenReturn(CountryCode.AT);
    when(taxRate2.isIncludedInPrice()).thenReturn(false);

    final TaxRate taxRate3 = mock(TaxRate.class);
    when(taxRate3.getName()).thenReturn("21% ES");
    when(taxRate3.getId()).thenReturn("taxRate-3");
    when(taxRate3.getAmount()).thenReturn(0.21);
    when(taxRate3.getCountry()).thenReturn(CountryCode.ES);
    when(taxRate3.isIncludedInPrice()).thenReturn(false);

    when(taxCategory.getTaxRates()).thenReturn(asList(taxRate1, taxRate2, taxRate3));

    // Update: Price is included.
    TaxRateDraft taxRateDraft1 =
        TaxRateDraftBuilder.of("11% US", 0.11, true, CountryCode.US).build();

    // taxRate-2 is removed.
    // new rate is added.
    TaxRateDraft taxRateDraft4 =
        TaxRateDraftBuilder.of("15% FR", 0.15, false, CountryCode.FR).build();

    // same
    TaxRateDraft taxRateDraft3 =
        TaxRateDraftBuilder.of("21% ES", 0.21, false, CountryCode.ES).build();

    TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                "name", asList(taxRateDraft1, taxRateDraft4, taxRateDraft3), "desc")
            .key("tax-category-key")
            .build();

    final List<UpdateAction<TaxCategory>> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(
            asList(
                ReplaceTaxRate.of("taxRate-1", taxRateDraft1),
                RemoveTaxRate.of("taxRate-2"),
                AddTaxRate.of(taxRateDraft4)));
  }

  @Test
  void buildTaxRatesUpdateActions_WithOnlyDifferentSubRates_ShouldReturnOnlyReplaceAction() {

    TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = mock(TaxRate.class);
    when(taxRate1.getName()).thenReturn("11% US");
    when(taxRate1.getState()).thenReturn("state");
    when(taxRate1.getId()).thenReturn("taxRate-1");
    when(taxRate1.getAmount()).thenReturn(0.11);
    when(taxRate1.getCountry()).thenReturn(CountryCode.US);
    when(taxRate1.isIncludedInPrice()).thenReturn(false);

    final SubRate oldSubRate1 = SubRate.of("subRate-1", 0.07);
    final SubRate oldSubRate2 = SubRate.of("subRate-2", 0.04);

    when(taxRate1.getSubRates()).thenReturn(asList(oldSubRate1, oldSubRate2));
    when(taxCategory.getTaxRates()).thenReturn(singletonList(taxRate1));

    final SubRate subRate1 = SubRate.of("subRate-1", 0.06);
    final SubRate subRate2 = SubRate.of("subRate-2", 0.05);

    TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("11% US", 0.11, false, CountryCode.US)
            .state("state")
            .subRates(asList(subRate1, subRate2))
            .build();

    TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of("name", singletonList(taxRateDraft), "desc")
            .key("tax-category-key")
            .build();

    final List<UpdateAction<TaxCategory>> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result).isEqualTo(singletonList(ReplaceTaxRate.of("taxRate-1", taxRateDraft)));
  }

  @Test
  void buildTaxRatesUpdateActions_WithMoreSubRates_ShouldReturnOnlyReplaceAction() {

    TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = buildTaxRate("state");
    when(taxCategory.getTaxRates()).thenReturn(singletonList(taxRate1));

    final SubRate subRate1 = SubRate.of("subRate-1", 0.06);
    final SubRate subRate2 = SubRate.of("subRate-2", 0.05);

    TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("11% US", 0.11, false, CountryCode.US)
            .state("state")
            .subRates(asList(subRate1, subRate2))
            .build();

    TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of("name", singletonList(taxRateDraft), "desc")
            .key("tax-category-key")
            .build();

    final List<UpdateAction<TaxCategory>> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result).isEqualTo(singletonList(ReplaceTaxRate.of("taxRate-1", taxRateDraft)));
  }

  @Test
  void buildTaxRatesUpdateActions_WithSameTaxRateAndSubRates_ShouldNotBuildAnyAction() {

    TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = buildTaxRate("state");
    when(taxCategory.getTaxRates()).thenReturn(singletonList(taxRate1));

    final SubRate subRate1 = SubRate.of("subRate-1", 0.11);

    TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("11% US", 0.11, false, CountryCode.US)
            .state("state")
            .subRates(singletonList(subRate1))
            .build();

    TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of("name", singletonList(taxRateDraft), "desc")
            .key("tax-category-key")
            .build();

    final List<UpdateAction<TaxCategory>> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildTaxRatesUpdateActions_WithNullOldState_ShouldReturnOnlyReplaceAction() {

    TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = buildTaxRate(null);
    when(taxCategory.getTaxRates()).thenReturn(singletonList(taxRate1));

    final SubRate subRate1 = SubRate.of("subRate-1", 0.06);
    final SubRate subRate2 = SubRate.of("subRate-2", 0.05);

    TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("11% US", 0.11, false, CountryCode.US)
            .state("state")
            .subRates(asList(subRate1, subRate2))
            .build();

    TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of("name", singletonList(taxRateDraft), "desc")
            .key("tax-category-key")
            .build();

    final List<UpdateAction<TaxCategory>> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(asList(RemoveTaxRate.of("taxRate-1"), AddTaxRate.of(taxRateDraft)));
  }

  @Test
  void buildTaxRatesUpdateActions_WithRemovedState_ShouldReturnRemoveAndAddActions() {

    TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = buildTaxRate("state");
    when(taxCategory.getTaxRates()).thenReturn(singletonList(taxRate1));

    final SubRate subRate1 = SubRate.of("subRate-1", 0.06);
    final SubRate subRate2 = SubRate.of("subRate-2", 0.05);

    TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("11% US", 0.11, false, CountryCode.US)
            .state(null)
            .subRates(asList(subRate1, subRate2))
            .build();

    TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of("name", singletonList(taxRateDraft), "desc")
            .key("tax-category-key")
            .build();

    final List<UpdateAction<TaxCategory>> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(asList(RemoveTaxRate.of("taxRate-1"), AddTaxRate.of(taxRateDraft)));
  }

  @Test
  void WhenTaxRatesWithAndWithoutState_WithRemovedTaxRate_ShouldBuildAndReturnRemoveAction() {

    TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = buildTaxRate("state");
    final TaxRate taxRate2 = buildTaxRate(null);
    when(taxCategory.getTaxRates()).thenReturn(asList(taxRate1, taxRate2));

    TaxRateDraft taxRateDraft = TaxRateDraftBuilder.of(taxRate2).build();

    // TaxCategoryDraft with only one taxRate
    TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of("name", singletonList(taxRateDraft), "desc")
            .key("tax-category-key")
            .build();

    final List<UpdateAction<TaxCategory>> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result).isEqualTo(asList(RemoveTaxRate.of("taxRate-1")));
  }

  @Test
  void buildTaxRatesUpdateActions_WithRemovedCountryCode_ShouldReturnOnlyRemoveAction() {

    TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = buildTaxRate("state");
    when(taxCategory.getTaxRates()).thenReturn(singletonList(taxRate1));

    final SubRate subRate1 = SubRate.of("subRate-1", 0.06);
    final SubRate subRate2 = SubRate.of("subRate-2", 0.05);

    TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("11% US", 0.11, false, null)
            .state(null)
            .subRates(asList(subRate1, subRate2))
            .build();

    TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of("name", singletonList(taxRateDraft), "desc")
            .key("tax-category-key")
            .build();

    final List<UpdateAction<TaxCategory>> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result).isEqualTo(singletonList(RemoveTaxRate.of("taxRate-1")));
  }

  @Test
  void buildTaxRatesUpdateActions_WithDuplicatedTaxRateDrafts_ShouldReturnOnlyFirstDraftInAction() {

    TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = buildTaxRate("state");
    when(taxCategory.getTaxRates()).thenReturn(singletonList(taxRate1));

    final SubRate subRate1 = SubRate.of("subRate-1", 0.06);
    final SubRate subRate2 = SubRate.of("subRate-2", 0.05);

    TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("11% US", 0.11, false, CountryCode.US)
            .state("state")
            .subRates(asList(subRate1, subRate2))
            .build();

    TaxRateDraft duplicatedCountryCodeAndState =
        TaxRateDraftBuilder.of("12% US", 0.12, false, CountryCode.US).state("state").build();

    TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                "name", asList(taxRateDraft, duplicatedCountryCodeAndState), "desc")
            .key("tax-category-key")
            .build();

    final List<UpdateAction<TaxCategory>> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result).isEqualTo(singletonList(ReplaceTaxRate.of("taxRate-1", taxRateDraft)));
  }

  @Test
  void
      buildTaxRatesUpdateActions_WithNewTaxRateDraftsWithSameCountryAndDifferentStates_ShouldCreateCorrectActions() {
    TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getKey()).thenReturn("tax-category-key");
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getDescription()).thenReturn("desc");

    final TaxRate taxRate1 = mock(TaxRate.class);
    when(taxRate1.getName()).thenReturn("11% US");
    when(taxRate1.getState()).thenReturn("state-1");
    when(taxRate1.getId()).thenReturn("taxRate-1");
    when(taxRate1.getAmount()).thenReturn(0.11);
    when(taxRate1.getCountry()).thenReturn(CountryCode.US);
    when(taxRate1.isIncludedInPrice()).thenReturn(false);

    final TaxRate taxRate2 = mock(TaxRate.class);
    when(taxRate2.getName()).thenReturn("12% US");
    when(taxRate2.getState()).thenReturn("state-2");
    when(taxRate2.getId()).thenReturn("taxRate-2");
    when(taxRate2.getAmount()).thenReturn(0.12);
    when(taxRate2.getCountry()).thenReturn(CountryCode.US);
    when(taxRate2.isIncludedInPrice()).thenReturn(false);

    final TaxRate taxRate4 = mock(TaxRate.class);
    when(taxRate4.getName()).thenReturn("14% US");
    when(taxRate4.getState()).thenReturn("state-4");
    when(taxRate4.getId()).thenReturn("taxRate-4");
    when(taxRate4.getAmount()).thenReturn(0.14);
    when(taxRate4.getCountry()).thenReturn(CountryCode.US);
    when(taxRate4.isIncludedInPrice()).thenReturn(false);

    when(taxCategory.getTaxRates()).thenReturn(asList(taxRate1, taxRate2, taxRate4));

    final SubRate subRate3 = SubRate.of("subRate-3", 0.13);
    TaxRateDraft taxRateDraft1 =
        TaxRateDraftBuilder.of("11% US", 0.11, false, CountryCode.US).state("state-1").build();

    TaxRateDraft taxRateDraft2 =
        TaxRateDraftBuilder.of("12% US", 0.12, false, CountryCode.US).state("state-2").build();

    TaxRateDraft newTaxRateDraft =
        TaxRateDraftBuilder.of("13% US", 0.13, false, CountryCode.US)
            .state("state-3")
            .subRates(singletonList(subRate3))
            .build();

    TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                "name", asList(taxRateDraft1, taxRateDraft2, newTaxRateDraft), "desc")
            .key("tax-category-key")
            .build();

    final List<UpdateAction<TaxCategory>> result =
        buildTaxRateUpdateActions(taxCategory, taxCategoryDraft);

    assertThat(result)
        .isEqualTo(asList(RemoveTaxRate.of("taxRate-4"), AddTaxRate.of(newTaxRateDraft)));
  }

  private TaxRate buildTaxRate(String state) {
    final TaxRate taxRate = mock(TaxRate.class);
    when(taxRate.getName()).thenReturn("11% US");
    when(taxRate.getState()).thenReturn(state);
    when(taxRate.getId()).thenReturn("taxRate-1");
    when(taxRate.getAmount()).thenReturn(0.11);
    when(taxRate.getCountry()).thenReturn(CountryCode.US);
    when(taxRate.isIncludedInPrice()).thenReturn(false);

    final SubRate oldSubRate = SubRate.of("subRate-1", 0.11);

    when(taxRate.getSubRates()).thenReturn(singletonList(oldSubRate));
    return taxRate;
  }
}
