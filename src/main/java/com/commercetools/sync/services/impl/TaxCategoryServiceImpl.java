package com.commercetools.sync.services.impl;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.TaxCategoryService;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.expansion.TaxCategoryExpansionModel;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQueryBuilder;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQueryModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static java.util.Collections.singleton;

public final class TaxCategoryServiceImpl
    extends BaseServiceWithKey<TaxCategoryDraft, TaxCategory, ProductSyncOptions, TaxCategoryQuery,
    TaxCategoryQueryModel, TaxCategoryExpansionModel<TaxCategory>> implements TaxCategoryService {

    public TaxCategoryServiceImpl(@Nonnull final ProductSyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedTaxCategoryId(@Nullable final String key) {
        return fetchCachedResourceId(key,
            () -> TaxCategoryQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().isIn(singleton(key)))
                .build());
    }
}
