package com.commercetools.sync.sdk2.services.impl;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;

import com.commercetools.api.client.ByProjectKeyTaxCategoriesGet;
import com.commercetools.api.client.ByProjectKeyTaxCategoriesKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyTaxCategoriesPost;
import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.api.models.tax_category.TaxCategoryDraft;
import com.commercetools.api.models.tax_category.TaxCategoryPagedQueryResponse;
import com.commercetools.api.models.tax_category.TaxCategoryUpdateAction;
import com.commercetools.api.models.tax_category.TaxCategoryUpdateBuilder;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.services.TaxCategoryService;
import com.commercetools.sync.sdk2.taxcategories.TaxCategorySyncOptions;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TaxCategoryServiceImpl
    extends BaseServiceWithKey<
        TaxCategorySyncOptions,
        TaxCategory,
        TaxCategoryDraft,
        ByProjectKeyTaxCategoriesGet,
        TaxCategoryPagedQueryResponse,
        ByProjectKeyTaxCategoriesKeyByKeyGet,
        TaxCategory,
        ByProjectKeyTaxCategoriesPost>
    implements TaxCategoryService {

  public TaxCategoryServiceImpl(@Nonnull final TaxCategorySyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> taxCategoryKeys) {
    return super.cacheKeysToIdsUsingGraphQl(taxCategoryKeys, GraphQlQueryResource.TAX_CATEGORIES);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedTaxCategoryId(@Nullable final String key) {
    if (key == null) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    final ByProjectKeyTaxCategoriesGet query =
        syncOptions
            .getCtpClient()
            .taxCategories()
            .get()
            .withWhere("key in :keys")
            .withPredicateVar("keys", Collections.singletonList(key));

    return super.fetchCachedResourceId(key, query);
  }

  @Nonnull
  @Override
  public CompletionStage<Set<TaxCategory>> fetchMatchingTaxCategoriesByKeys(
      @Nonnull final Set<String> keys) {
    return super.fetchMatchingResources(
        keys,
        TaxCategory::getKey,
        (keysNotCached) ->
            syncOptions
                .getCtpClient()
                .taxCategories()
                .get()
                .withWhere("key in :keys")
                .withPredicateVar("keys", keysNotCached));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<TaxCategory>> fetchTaxCategory(@Nullable final String key) {
    return super.fetchResource(key, syncOptions.getCtpClient().taxCategories().withKey(key).get());
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<TaxCategory>> createTaxCategory(
      @Nonnull final TaxCategoryDraft taxCategoryDraft) {
    return super.createResource(
        taxCategoryDraft,
        TaxCategoryDraft::getKey,
        TaxCategory::getId,
        Function.identity(),
        syncOptions.getCtpClient().taxCategories().post(taxCategoryDraft));
  }

  @Nonnull
  @Override
  public CompletionStage<TaxCategory> updateTaxCategory(
      @Nonnull final TaxCategory taxCategory,
      @Nonnull final List<TaxCategoryUpdateAction> updateActions) {

    final List<List<TaxCategoryUpdateAction>> actionBatches =
        batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);

    CompletionStage<ApiHttpResponse<TaxCategory>> resultStage =
        CompletableFuture.completedFuture(new ApiHttpResponse<>(200, null, taxCategory));

    for (final List<TaxCategoryUpdateAction> batch : actionBatches) {
      resultStage =
          resultStage
              .thenApply(ApiHttpResponse::getBody)
              .thenCompose(
                  updatedTaxCategory ->
                      syncOptions
                          .getCtpClient()
                          .taxCategories()
                          .withId(updatedTaxCategory.getId())
                          .post(
                              TaxCategoryUpdateBuilder.of()
                                  .actions(batch)
                                  .version(updatedTaxCategory.getVersion())
                                  .build())
                          .execute());
    }

    return resultStage.thenApply(ApiHttpResponse::getBody);
  }
}
