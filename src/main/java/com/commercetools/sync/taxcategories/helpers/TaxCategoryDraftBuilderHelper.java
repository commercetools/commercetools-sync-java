package com.commercetools.sync.taxcategories.helpers;

import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class TaxCategoryDraftBuilderHelper {

    /**
     * Convenient method to create the {@link TaxCategoryDraft} instance out of the {@link TaxCategory} instance.
     *
     * @param taxCategory the taxCategory to be converted
     * @return an instance of the taxCategory draft
     */
    public static TaxCategoryDraft of(@Nonnull final TaxCategory taxCategory) {
        final List<TaxRateDraft> taxRateDrafts = taxCategory.getTaxRates().stream()
            .map(taxRate -> TaxRateDraftBuilder.of(taxRate).build())
            .collect(Collectors.toList());
        return TaxCategoryDraftBuilder.of(taxCategory.getName(), taxRateDrafts, taxCategory.getDescription())
            .key(taxCategory.getKey())
            .build();
    }

}
