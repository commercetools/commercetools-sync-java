package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.commands.TaxCategoryCreateCommand;
import io.sphere.sdk.taxcategories.commands.TaxCategoryUpdateCommand;
import io.sphere.sdk.taxcategories.expansion.TaxCategoryExpansionModel;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQueryModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Collections.singleton;

public final class TaxCategoryServiceImpl extends BaseServiceImpl<TaxCategoryDraft, TaxCategory, BaseSyncOptions,
    TaxCategoryQuery, TaxCategoryQueryModel, TaxCategoryExpansionModel<TaxCategory>>
    implements TaxCategoryService {

    public TaxCategoryServiceImpl(@Nonnull final TaxCategorySyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedTaxCategoryId(@Nullable final String key) {
        return fetchCachedResourceId(key);
    }

    @Nonnull
    @Override
    CompletionStage<Optional<String>> fetchAndCache(@Nonnull final String key) {
        return fetchAndCache(key, TaxCategoryQuery::of, TaxCategory::getKey, "TaxCategory");
    }

    @Nonnull
    @Override
    public CompletionStage<Set<TaxCategory>> fetchMatchingTaxCategoriesByKeys(
        @Nonnull final Set<String> taxCategoryNames) {
        return fetchMatchingResources(
            taxCategoryNames,
            () -> TaxCategoryQuery.of().withPredicates(
                buildResourceQueryPredicate(taxCategoryNames, "key")),
            TaxCategory::getKey);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<TaxCategory>> fetchTaxCategory(@Nullable final String name) {
        return fetchResource(name,
            () -> TaxCategoryQuery.of().withPredicates(
                buildResourceQueryPredicate(singleton(name), "key")),
            TaxCategory::getKey);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<TaxCategory>> createTaxCategory(@Nonnull final TaxCategoryDraft stateDraft) {
        return createResource(stateDraft, TaxCategoryDraft::getKey, TaxCategoryCreateCommand::of);
    }

    @Nonnull
    @Override
    public CompletionStage<TaxCategory> updateTaxCategory(
        @Nonnull final TaxCategory state,
        @Nonnull final List<UpdateAction<TaxCategory>> updateActions) {
        return updateResource(state, TaxCategoryUpdateCommand::of, updateActions);
    }

}
