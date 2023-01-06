package com.commercetools.sync.sdk2.services.impl;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.collectionOfFuturesToFutureOfCollection;
import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.client.ByProjectKeyCategoriesGet;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryPagedQueryResponse;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.api.models.category.CategoryUpdateBuilder;
import com.commercetools.sync.commons.utils.ChunkUtils;
import com.commercetools.sync.sdk2.categories.CategorySyncOptions;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.services.CategoryService;
import io.sphere.sdk.client.NotFoundException;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.ApiMethod;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.text.StringEscapeUtils;

/** Implementation of CategoryService interface. */
public final class CategoryServiceImpl
    extends BaseService<CategorySyncOptions, Category, ByProjectKeyCategoriesGet>
    implements CategoryService {

  public CategoryServiceImpl(@Nonnull final CategorySyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> categoryKeys) {
    return super.cacheKeysToIds(categoryKeys, GraphQlQueryResource.CATEGORIES);
  }

  @Nonnull
  @Override
  public CompletionStage<Set<Category>> fetchMatchingCategoriesByKeys(
      @Nonnull final Set<String> categoryKeys) {
    if (categoryKeys.isEmpty()) {
      return CompletableFuture.completedFuture(Collections.emptySet());
    }

    final List<List<String>> chunkedKeys = ChunkUtils.chunk(categoryKeys, CHUNK_SIZE);

    final List<ByProjectKeyCategoriesGet> fetchByKeysRequests =
        chunkedKeys.stream()
            .map(
                keys ->
                    keys.stream()
                        .filter(key -> !isBlank(key))
                        .map(StringEscapeUtils::escapeJava)
                        .map(s -> "\"" + s + "\"")
                        .collect(Collectors.joining(", ")))
            .map(commaSeparatedKeys -> format("key in (%s)", commaSeparatedKeys))
            .map(
                whereQuery ->
                    syncOptions
                        .getCtpClient()
                        .categories()
                        .get()
                        .addWhere(whereQuery)
                        .withLimit(CHUNK_SIZE)
                        .withWithTotal(false))
            .collect(toList());

    // todo: what happens on error ?
    return collectionOfFuturesToFutureOfCollection(
            fetchByKeysRequests.stream().map(ApiMethod::execute).collect(Collectors.toList()),
            Collectors.toList())
        .thenApply(
            pagedCategoriesResponses ->
                pagedCategoriesResponses.stream()
                    .map(ApiHttpResponse::getBody)
                    .map(CategoryPagedQueryResponse::getResults)
                    .flatMap(Collection::stream)
                    .peek(category -> keyToIdCache.put(category.getKey(), category.getId()))
                    .collect(Collectors.toSet()));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Category>> fetchCategory(@Nullable final String key) {
    if (isBlank(key)) {
      return CompletableFuture.completedFuture(null);
    }

    return syncOptions
        .getCtpClient()
        .categories()
        .withKey(key)
        .get()
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .thenApply(
            category -> {
              keyToIdCache.put(category.getKey(), category.getId());
              return Optional.of(category);
            })
        .exceptionally(
            throwable -> {
              if (throwable.getCause() instanceof NotFoundException) {
                return Optional.empty();
              }
              // todo - to check with the team: what is the best way to handle this ?
              syncOptions.applyErrorCallback(new SyncException(throwable));
              return Optional.empty();
            });
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedCategoryId(@Nonnull final String key) {
    ByProjectKeyCategoriesGet query =
        syncOptions
            .getCtpClient()
            .categories()
            .get()
            .withWhere("key in :keys")
            .withPredicateVar("keys", Collections.singletonList(key));

    return this.fetchCachedResourceId(key, query);
  }

  @Nonnull
  CompletionStage<Optional<String>> fetchCachedResourceId(
      @Nullable final String key, @Nonnull final ByProjectKeyCategoriesGet query) {
    return fetchCachedResourceId(key, resource -> resource.getKey(), query);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Category>> createCategory(
      @Nonnull final CategoryDraft categoryDraft) {
    final String draftKey = categoryDraft.getKey();

    if (isBlank(draftKey)) {
      syncOptions.applyErrorCallback(
          new SyncException(format(CREATE_FAILED, draftKey, "Draft key is blank!")),
          null,
          categoryDraft,
          null);
      return CompletableFuture.completedFuture(Optional.empty());
    } else {
      return syncOptions
          .getCtpClient()
          .categories()
          .post(categoryDraft)
          .execute()
          .handle(
              ((resource, exception) -> {
                if (exception == null && resource.getBody() != null) {
                  keyToIdCache.put(draftKey, resource.getBody().getId());
                  return Optional.of(resource.getBody());
                } else if (exception != null) {
                  syncOptions.applyErrorCallback(
                      new SyncException(
                          format(CREATE_FAILED, draftKey, exception.getMessage()), exception),
                      null,
                      categoryDraft,
                      null);
                  return Optional.empty();
                } else {
                  return Optional.empty();
                }
              }));
    }
  }

  @Nonnull
  @Override
  public CompletionStage<Category> updateCategory(
      @Nonnull final Category category, @Nonnull final List<CategoryUpdateAction> updateActions) {
    final List<List<CategoryUpdateAction>> actionBatches =
        batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);

    CompletionStage<ApiHttpResponse<Category>> resultStage =
        CompletableFuture.completedFuture(new ApiHttpResponse<>(200, null, category));

    for (final List<CategoryUpdateAction> batch : actionBatches) {
      resultStage =
          resultStage
              .thenApply(ApiHttpResponse::getBody)
              .thenCompose(
                  updatedCategory ->
                      syncOptions
                          .getCtpClient()
                          .categories()
                          .withId(updatedCategory.getId())
                          .post(
                              CategoryUpdateBuilder.of()
                                  .actions(batch)
                                  .version(updatedCategory.getVersion())
                                  .build())
                          .execute());
    }

    return resultStage.thenApply(ApiHttpResponse::getBody);
  }
}
