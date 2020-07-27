package com.commercetools.sync.services.impl;

import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.commands.TaxCategoryCreateCommand;
import io.sphere.sdk.taxcategories.commands.TaxCategoryUpdateCommand;
import io.sphere.sdk.taxcategories.expansion.TaxCategoryExpansionModel;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQueryBuilder;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQueryModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Collections.singleton;

public final class TaxCategoryServiceImpl
    extends BaseServiceWithKey<TaxCategoryDraft, TaxCategory, TaxCategorySyncOptions, TaxCategoryQuery,
    TaxCategoryQueryModel, TaxCategoryExpansionModel<TaxCategory>> implements TaxCategoryService {

    public TaxCategoryServiceImpl(@Nonnull final TaxCategorySyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedTaxCategoryId(@Nullable final String key) {
        return fetchCachedResourceId(
            key,
            () -> TaxCategoryQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().isIn(singleton(key)))
                .build());
    }

    @Nonnull
    @Override
    public CompletionStage<Set<TaxCategory>> fetchMatchingTaxCategoriesByKeys(@Nonnull final Set<String> keys) {
        return fetchMatchingResources(keys,
            () -> TaxCategoryQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().isIn(keys))
                .build());
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<TaxCategory>> fetchTaxCategory(@Nullable final String key) {
        return fetchResource(key,
            () -> TaxCategoryQueryBuilder.of().plusPredicates(queryModel -> queryModel.key().is(key)).build());
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<TaxCategory>> createTaxCategory(@Nonnull final TaxCategoryDraft taxCategoryDraft) {
        return createResource(taxCategoryDraft, TaxCategoryCreateCommand::of);
    }

    @Nonnull
    @Override
    public CompletionStage<TaxCategory> updateTaxCategory(
        @Nonnull final TaxCategory taxCategory,
        @Nonnull final List<UpdateAction<TaxCategory>> updateActions) {
        return updateResource(taxCategory, TaxCategoryUpdateCommand::of, updateActions);
    }
}
