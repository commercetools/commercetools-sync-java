package com.commercetools.sync.services.impl;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.services.CategoryService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryUpdateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.queries.PagedResult;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Implementation of CategoryService interface.
 * TODO: USE graphQL to get only keys. GITHUB ISSUE#84
 */
public final class CategoryServiceImpl extends BaseService<Category, CategoryDraft> implements CategoryService {
    private static final String FETCH_FAILED = "Failed to fetch Categories with keys: '%s'. Reason: %s";
    private static final String CATEGORY_KEY_NOT_SET = "Category with id: '%s' has no key set. Keys are required for "
        + "category matching.";

    public CategoryServiceImpl(@Nonnull final CategorySyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> cacheKeysToIds() {
        if (isCached) {
            return CompletableFuture.completedFuture(keyToIdCache);
        }

        final Consumer<List<Category>> categoryPageConsumer = categoriesPage ->
            categoriesPage.forEach(category -> {
                final String key = category.getKey();
                final String id = category.getId();
                if (StringUtils.isNotBlank(key)) {
                    keyToIdCache.put(key, id);
                } else {
                    syncOptions.applyWarningCallback(format(CATEGORY_KEY_NOT_SET, id));
                }
            });

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), CategoryQuery.of(), categoryPageConsumer)
                            .thenAccept(result -> isCached = true)
                            .thenApply(result -> keyToIdCache);
    }

    @Nonnull
    @Override
    public CompletionStage<Set<Category>> fetchMatchingCategoriesByKeys(@Nonnull final Set<String> categoryKeys) {
        if (categoryKeys.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        final Function<List<Category>, List<Category>> categoryPageCallBack = categoriesPage -> categoriesPage;
        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(),
            CategoryQuery.of().plusPredicates(categoryQueryModel -> categoryQueryModel.key().isIn(categoryKeys)),
            categoryPageCallBack)
                            .handle((fetchedCategories, sphereException) -> {
                                if (sphereException != null) {
                                    syncOptions
                                        .applyErrorCallback(format(FETCH_FAILED, categoryKeys, sphereException),
                                            sphereException);
                                    return Collections.emptySet();
                                }
                                return fetchedCategories.stream()
                                                        .flatMap(List::stream)
                                                        .collect(Collectors.toSet());
                            });
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Category>> fetchCategory(@Nullable final String key) {
        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return syncOptions.getCtpClient()
                .execute(CategoryQuery.of().plusPredicates(categoryQueryModel -> categoryQueryModel.key().is(key)))
                .thenApply(PagedResult::head)
                .exceptionally(sphereException -> {
                    syncOptions.applyErrorCallback(format(FETCH_FAILED, key, sphereException), sphereException);
                    return Optional.empty();
                });
    }

    @Nonnull
    @Override
    public CompletionStage<Set<Category>> createCategories(@Nonnull final Set<CategoryDraft> categoryDrafts) {
        return mapValuesToFutureOfCompletedValues(categoryDrafts, this::createCategory)
            .thenApply(results -> results.filter(Optional::isPresent).map(Optional::get))
            .thenApply(createdCategories -> createdCategories.collect(Collectors.toSet()));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedCategoryId(@Nonnull final String key) {
        if (isCached) {
            return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
        }
        return fetchAndCache(key);
    }

    private CompletionStage<Optional<String>> fetchAndCache(@Nonnull final String key) {
        return cacheKeysToIds()
            .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Category>> createCategory(@Nonnull final CategoryDraft categoryDraft) {
        return applyCallbackAndCreate(categoryDraft, categoryDraft.getKey(), CategoryCreateCommand::of);
    }

    @Nonnull
    @Override
    public CompletionStage<Category> updateCategory(@Nonnull final Category category,
                                                    @Nonnull final List<UpdateAction<Category>> updateActions) {
        return updateResource(category, CategoryUpdateCommand::of, updateActions);
    }
}
