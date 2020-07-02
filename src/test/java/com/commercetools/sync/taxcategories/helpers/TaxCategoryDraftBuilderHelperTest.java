package com.commercetools.sync.taxcategories.helpers;

import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class TaxCategoryDraftBuilderHelperTest {

    @Test
    void of_WithTaxCategory_ShouldBuildTaxCategoryDraft() {
        TaxCategory resource = SphereJsonUtils.readObjectFromResource("tax-category.json", TaxCategory.class);
        // convertion because of types
        List<TaxRateDraft> resourcesDrafts = resource.getTaxRates().stream()
            .map(taxRate -> TaxRateDraftBuilder.of(taxRate).build()).collect(Collectors.toList());

        TaxCategoryDraft draft = TaxCategoryDraftBuilderHelper.of(resource);

        assertAll(
            () -> assertThat(draft.getKey()).isEqualTo(resource.getKey()),
            () -> assertThat(draft.getName()).isEqualTo(resource.getName()),
            () -> assertThat(draft.getDescription()).isEqualTo(resource.getDescription()),
            () -> assertThat(draft.getTaxRates()).isEqualTo(resourcesDrafts)
        );
    }

}
