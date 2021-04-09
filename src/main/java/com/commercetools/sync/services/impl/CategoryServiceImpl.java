package com.commercetools.sync.services.impl;

import static java.util.Collections.singleton;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.services.CategoryService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryUpdateCommand;
import io.sphere.sdk.categories.expansion.CategoryExpansionModel;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.categories.queries.CategoryQueryBuilder;
import io.sphere.sdk.categories.queries.CategoryQueryModel;
import io.sphere.sdk.commands.UpdateAction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Implementation of CategoryService interface. */
public final class CategoryServiceImpl
    extends BaseServiceWithKey<
        CategoryDraft,
        Category,
        Category,
        CategorySyncOptions,
        CategoryQuery,
        CategoryQueryModel,
        CategoryExpansionModel<Category>>
    implements CategoryService {

  public CategoryServiceImpl(@Nonnull final CategorySyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> categoryKeys) {
    return cacheKeysToIds(
        categoryKeys,
        keysNotCached ->
            new ResourceKeyIdGraphQlRequest(keysNotCached, GraphQlQueryResources.CATEGORIES));
  }

  @Nonnull
  @Override
  public CompletionStage<Set<Category>> fetchMatchingCategoriesByKeys(
      @Nonnull final Set<String> categoryKeys) {

    return fetchMatchingResources(
        categoryKeys,
        (keysNotCached) ->
            CategoryQuery.of()
                .plusPredicates(
                    categoryQueryModel -> categoryQueryModel.key().isIn(keysNotCached)));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Category>> fetchCategory(@Nullable final String key) {

    return fetchResource(
        key,
        () ->
            CategoryQuery.of()
                .plusPredicates(categoryQueryModel -> categoryQueryModel.key().is(key)));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedCategoryId(@Nonnull final String key) {

    return fetchCachedResourceId(
        key,
        () ->
            CategoryQueryBuilder.of()
                .plusPredicates(queryModel -> queryModel.key().isIn(singleton(key)))
                .build());
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Category>> createCategory(
      @Nonnull final CategoryDraft categoryDraft) {
    return createResource(categoryDraft, CategoryCreateCommand::of);
  }

  @Nonnull
  @Override
  public CompletionStage<Category> updateCategory(
      @Nonnull final Category category, @Nonnull final List<UpdateAction<Category>> updateActions) {
    return updateResource(category, CategoryUpdateCommand::of, updateActions);
  }
}
