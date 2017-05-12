package com.commercetools.sync.categories;

import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.categories.utils.CategorySyncUtils;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static com.commercetools.sync.commons.enums.Error.CTP_CATEGORY_CREATE_FAILED;
import static com.commercetools.sync.commons.enums.Error.CTP_CATEGORY_UPDATE_FAILED;
import static com.commercetools.sync.commons.enums.Error.CTP_CATEGORY_FETCH_FAILED;
import static com.commercetools.sync.commons.enums.Error.CTP_CATEGORY_SYNC_FAILED;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class CategorySync extends BaseSync<CategoryDraft, Category, CategorySyncStatistics, CategorySyncOptions> {
    private final TypeService typeService;
    private final CategoryService categoryService;

    /**
     * Takes a {@link CategorySyncOptions} instance to instantiate a new {@link CategorySync} instance that could be
     * used to sync categories or category drafts with the given categories in the CTP project specified in the
     * injected {@link CategorySyncOptions} instance.
     *
     * @param syncOptions the container of all the options of the sync process including the CTP project client and/or
     *                    configuration and other sync-specific options.
     */
    public CategorySync(@Nonnull final CategorySyncOptions syncOptions) {
        this(syncOptions,
            new TypeServiceImpl(syncOptions.getCtpClient().getClient()),
            new CategoryServiceImpl(syncOptions.getCtpClient().getClient()));
    }

    /**
     * Takes a {@link CategorySyncOptions}, a {@link TypeService} and {@link CategoryService} instances to instantiate
     * a new {@link CategorySync} instance that could be used to sync categories or category drafts with the given
     * categories in the CTP project specified in the injected {@link CategorySyncOptions} instance.
     *
     * <p>NOTE: This constructor is mainly to be used for tests where the services can be mocked and passed to.
     *
     * @param syncOptions     the container of all the options of the sync process including the CTP project
     *                        client and/or configuration and other sync-specific options.
     * @param typeService     the type service which is responsible for fetching/caching the Types from the CTP project.
     * @param categoryService the category service which is responsible for fetching, creating and updating categories
     *                        from and to the CTP project.
     */
    CategorySync(@Nonnull final CategorySyncOptions syncOptions,
                 @Nonnull final TypeService typeService,
                 @Nonnull final CategoryService categoryService) {
        super(new CategorySyncStatistics(), syncOptions);
        this.typeService = typeService;
        this.categoryService = categoryService;
    }

    /**
     * Traverses a {@link List} of {@link CategoryDraft} objects and tries to fetch a category, from the CTP
     * project with the configuration stored in the {@code syncOptions} instance of this class, using the external id.
     * If a category exists, this category is synced to be the same as the new category draft in this list. If no
     * category exist with such external id, a new category, identical to this new category draft  is created.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried out
     * successfully or not. If an exception was thrown on executing the request to CTP, the optional error callback
     * specified in the {@code syncOptions} is called.
     *
     * @param categoryDrafts the list of new category drafts to sync to the CTP project.
     * @return an instance of {@link CompletionStage&lt;U&gt;} which contains as a result an instance of
     *      {@link CategorySyncStatistics} representing the {@code statistics} instance attribute of
     *      {@link this} {@link CategorySync}.
     */
    @Override
    protected CompletionStage<CategorySyncStatistics> processDrafts(@Nonnull final List<CategoryDraft> categoryDrafts) {
        for (CategoryDraft categoryDraft : categoryDrafts) {
            if (categoryDraft != null) {
                this.statistics.incrementProcessed();
                final String externalId = categoryDraft.getExternalId();
                if (isNotBlank(externalId)) {
                    createOrUpdateCategory(categoryDraft);
                } else {
                    handleSyncFailure(format("CategoryDraft with name: %s doesn't have an externalId.",
                        categoryDraft.getName().toString()), null);
                }
            }
        }
        return CompletableFuture.completedFuture(this.statistics);
    }

    @Override
    protected CompletionStage<CategorySyncStatistics> process(@Nonnull final List<Category> resources) {
        //TODO: SEE GITHUB ISSUE#12
        return CompletableFuture.completedFuture(this.statistics);
    }

    /**
     * Given a category draft {@link CategoryDraft} with an externalId, this method blocks execution to try to fetch the
     * existing category from CTP project stored in the {@code syncOptions} instance of this class. If successful, It
     * either blocks to create a new category, if none exist with the same external id, or blocks to update the
     * existing category.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried out
     * successfully or not. If an exception was thrown on executing the request to CTP, the optional error callback
     * specified in the {@code syncOptions} is called.
     *
     * @param categoryDraft the category draft where we get the new data.
     */
    private void createOrUpdateCategory(@Nonnull final CategoryDraft categoryDraft) {
        final String externalId = categoryDraft.getExternalId();
        try {
            this.categoryService.fetchCategoryByExternalId(externalId)
                                .thenCompose(oldCategoryOptional -> {
                                    if (oldCategoryOptional.isPresent()) {
                                        return syncCategories(oldCategoryOptional.get(), categoryDraft);
                                    } else {
                                        return createCategory(categoryDraft);
                                    }
                                })
                                .exceptionally(sphereException -> {
                                    handleSyncFailure(format(CTP_CATEGORY_FETCH_FAILED.getDescription(), externalId,
                                        this.syncOptions.getCtpClient().getClientConfig().getProjectKey(),
                                        sphereException.getMessage()), sphereException);
                                    return null;
                                })
                                .toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException exception) {
            handleSyncFailure(format(CTP_CATEGORY_SYNC_FAILED.getDescription(), externalId,
                this.syncOptions.getCtpClient().getClientConfig().getProjectKey(), exception.getMessage()), exception);
        }
    }

    /**
     * Given a {@link CategoryDraft}, issues a request to the CTP project defined by the client configuration stored in
     * the {@code syncOptions} instance of this class to create a category with the same fields as this category draft.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP, the optional error callback
     * specified in the {@code syncOptions} is called.
     *
     * @param categoryDraft the category draft to create the category from.
     * @return a future monad which can contain an empty result.
     */
    private CompletionStage<Void> createCategory(@Nonnull final CategoryDraft categoryDraft) {
        return this.categoryService.createCategory(categoryDraft)
                                   .thenAccept(createdCategory -> this.statistics.incrementCreated())
                                   .exceptionally(sphereException -> {
                                       handleSyncFailure(format(CTP_CATEGORY_CREATE_FAILED.getDescription(),
                                           categoryDraft.getExternalId(),
                                           this.syncOptions.getCtpClient().getClientConfig().getProjectKey(),
                                           sphereException.getMessage()), sphereException);
                                       return null;
                                   });
    }

    /**
     * Given an existing {@link Category} and a new {@link CategoryDraft}, this method calculates all the update actions
     * required to synchronize the existing category to be the same as the new one. If there are update actions found, a
     * request is made to CTP to update the existing category, otherwise it doesn't issue a request.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new data.
     * @return a future monad which can contain an empty result.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<Void> syncCategories(@Nonnull final Category oldCategory,
                                                 @Nonnull final CategoryDraft newCategory) {
        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildActions(oldCategory, newCategory, this.syncOptions, this.typeService);
        if (!updateActions.isEmpty()) {
            return updateCategory(oldCategory, updateActions);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Given a {@link Category} and a {@link List} of {@link UpdateAction} elements, this method issues a request to
     * the CTP project defined by the client configuration stored in the {@code syncOptions} instance
     * of this class to update the specified category with this list of update actions.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP,
     * the optional error callback specified in the {@code syncOptions} is called.
     *
     * @param category      the category to update.
     * @param updateActions the list of update actions to update the category with.
     * @return a future monad which can contain an empty result.
     */
    private CompletionStage<Void> updateCategory(@Nonnull final Category category,
                                                 @Nonnull final List<UpdateAction<Category>> updateActions) {
        return this.categoryService.updateCategory(category, updateActions)
                                   .thenAccept(updatedCategory -> this.statistics.incrementUpdated())
                                   .exceptionally(sphereException -> {
                                       handleSyncFailure(format(CTP_CATEGORY_UPDATE_FAILED.getDescription(),
                                           category.getId(),
                                           this.syncOptions.getCtpClient().getClientConfig().getProjectKey(),
                                           sphereException.getMessage()), sphereException);
                                       return null;
                                   });
    }

    /**
     * Given a reason message as {@link String} and {@link Throwable} exception, this method calls the optional error
     * callback specified in the {@code syncOptions} and updates the {@code statistics} instance by incrementing the
     * total number of failed resources to sync.
     *
     * @param reason    the reason of failure.
     * @param exception the exception that occurred, if any.
     */
    private void handleSyncFailure(@Nonnull final String reason, @Nullable final Throwable exception) {
        this.syncOptions.applyErrorCallback(reason, exception);
        this.statistics.incrementFailed();
    }
}
