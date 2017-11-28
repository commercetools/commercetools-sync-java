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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.commercetools.sync.services.ServiceUtils.applyCallbackAndCreate;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Implementation of CategoryService interface.
 * TODO: USE graphQL to get only keys. GITHUB ISSUE#84
 */
public final class CategoryServiceImpl implements CategoryService {
    private final CategorySyncOptions syncOptions;
    private boolean isCached = false;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();
    private static final String CREATE_FAILED = "Failed to create CategoryDraft with key: '%s'. Reason: %s";
    private static final String FETCH_FAILED = "Failed to fetch Categories with keys: '%s'. Reason: %s";
    private static final String CATEGORY_KEY_NOT_SET = "Category with id: '%s' has no key set. Keys are required for "
        + "category matching.";

    public CategoryServiceImpl(@Nonnull final CategorySyncOptions syncOptions) {
        this.syncOptions = syncOptions;
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
        final List<CompletableFuture<Optional<Category>>> futureCreations = categoryDrafts.stream()
                                                                        .map(this::createCategory)
                                                                        .map(CompletionStage::toCompletableFuture)
                                                                        .collect(Collectors.toList());
        return CompletableFuture.allOf(futureCreations.toArray(new CompletableFuture[futureCreations.size()]))
                                .thenApply(result -> futureCreations.stream()
                                    .map(CompletionStage::toCompletableFuture)
                                    .map(CompletableFuture::join)
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toSet()));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedCategoryId(@Nonnull final String key) {
        if (isCached) {
            return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
        }
        return cacheAndFetch(key);
    }

    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key) {
        return cacheKeysToIds()
            .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Category>> createCategory(@Nonnull final CategoryDraft categoryDraft) {
        return applyCallbackAndCreate(categoryDraft, syncOptions, CategoryCreateCommand::of,
                this::handleCategoryCreation);
    }

    private Optional<Category> handleCategoryCreation(
            @Nonnull final CategoryDraft draft,
            @Nullable final Category createdCategory,
            @Nullable final Throwable sphereException) {

        final Supplier<Optional<Category>> onFailure = () -> {
            syncOptions.applyErrorCallback(format(CREATE_FAILED, draft.getKey(), sphereException), sphereException);
            return Optional.empty();
        };

        @SuppressWarnings("ConstantConditions") //createdCategory is only invoked if not null.
        final Supplier<Optional<Category>> onSuccess = () -> {
            keyToIdCache.put(createdCategory.getKey(), createdCategory.getId());
            return Optional.of(createdCategory);
        };

        return createdCategory != null ? onSuccess.get() : onFailure.get();
    }


    @Nonnull
    @Override
    public CompletionStage<Category> updateCategory(@Nonnull final Category category,
                                                              @Nonnull final List<UpdateAction<Category>>
                                                                  updateActions) {
        final CategoryUpdateCommand categoryUpdateCommand = CategoryUpdateCommand.of(category, updateActions);
        return syncOptions.getCtpClient().execute(categoryUpdateCommand);
    }
}
