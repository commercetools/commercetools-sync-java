package com.commercetools.sync.taxcategories.utils;

import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.commands.UpdateAction;
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
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.commercetools.sync.taxcategories.utils.TaxCategorySyncUtils.buildActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaxCategorySyncUtilsTest {

    private static String KEY = "key";

    @Test
    void buildActions_WithSameValues_ShouldNotBuildUpdateActions() {
        final String name = "test name";
        final String description = "test description";

        final TaxCategory taxCategory = mock(TaxCategory.class);
        when(taxCategory.getName()).thenReturn(name);
        when(taxCategory.getKey()).thenReturn(KEY);
        when(taxCategory.getDescription()).thenReturn(description);

        final TaxCategoryDraft taxCategoryDraft = TaxCategoryDraftBuilder.of(name, Collections.emptyList(), description)
            .key(KEY)
            .build();

        final List<UpdateAction<TaxCategory>> result = buildActions(taxCategory, taxCategoryDraft,
            mock(TaxCategorySyncOptions.class));

        assertThat(result).isEmpty();
    }

    @Test
    void buildActions_WithDifferentValues_ShouldBuildAllUpdateActions() {
        final TaxCategory taxCategory = mock(TaxCategory.class);
        when(taxCategory.getName()).thenReturn("name");
        when(taxCategory.getKey()).thenReturn(KEY);
        when(taxCategory.getDescription()).thenReturn("description");

        final String removeId = "removeMe";
        final String replaceId = "replaceMe";
        final CountryCode countryCode = CountryCode.DE;

        final TaxRate toBeRemovedTaxRate = mock(TaxRate.class);
        when(toBeRemovedTaxRate.getId()).thenReturn(removeId);
        when(toBeRemovedTaxRate.getName()).thenReturn(removeId);
        final TaxRate toBeReplacedTaxRate = mock(TaxRate.class);
        when(toBeReplacedTaxRate.getId()).thenReturn(replaceId);
        when(toBeReplacedTaxRate.getName()).thenReturn(replaceId);

        when(taxCategory.getTaxRates()).thenReturn(Arrays.asList(toBeRemovedTaxRate, toBeReplacedTaxRate));

        final TaxRateDraft replaceTaxRateDraft = TaxRateDraftBuilder
            .of(replaceId, 10, true, countryCode)
            .build();
        final TaxRateDraft addTaxRateDraft = TaxRateDraftBuilder
            .of("addMe", 1, true, countryCode)
            .build();

        final TaxCategoryDraft taxCategoryDraft = TaxCategoryDraftBuilder
            .of("different name", Arrays.asList(replaceTaxRateDraft, addTaxRateDraft), "different description")
            .key(KEY)
            .build();

        final List<UpdateAction<TaxCategory>> result = buildActions(taxCategory, taxCategoryDraft,
            mock(TaxCategorySyncOptions.class));

        assertAll(
            () -> assertThat(result).contains(ChangeName.of(taxCategoryDraft.getName())),
            () -> assertThat(result).contains(SetDescription.of(taxCategoryDraft.getDescription())),
            () -> assertThat(result).contains(ReplaceTaxRate.of(replaceId, replaceTaxRateDraft)),
            () -> assertThat(result).contains(RemoveTaxRate.of(removeId)),
            () -> assertThat(result).contains(AddTaxRate.of(addTaxRateDraft))
        );
    }

}
