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
import io.sphere.sdk.models.SphereException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class CategorySync extends BaseSync<CategoryDraft, Category, CategorySyncStatistics, CategorySyncOptions> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategorySync.class);
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
     * a new {@link CategorySync} instance that could be used to sync categories or category drafts with the given categories
     * in the CTP project specified in the injected {@link CategorySyncOptions} instance.
     * <p>
     * NOTE: This constructor is mainly to be used for tests where the services can be mocked and passed to.
     *
     * @param syncOptions     the container of all the options of the sync process including the CTP project client and/or
     *                        configuration and other sync-specific options.
     * @param typeService     the type service which is responsible for fetching/caching the Types from the CTP project.
     * @param categoryService the category service which is responsible for fetching, creating and updating categories
     *                        from and to the CTP project.
     */
    CategorySync(@Nonnull final CategorySyncOptions syncOptions,
                 @Nonnull final TypeService typeService,
                 @Nonnull final CategoryService categoryService) {
        super(new CategorySyncStatistics(),
                syncOptions,
                LOGGER);
        this.typeService = typeService;
        this.categoryService = categoryService;
    }

    /**
     * Traverses a {@link List} of {@link CategoryDraft} objects and tries to fetch a category, from the CTP project with
     * the configuration stored in the {@code syncOptions} instance of this class, using the external id. If a category exists,
     * this category is synced to be the same as the new category draft in this list. If no category exist with such external id,
     * a new category, identical to this new category draft is created.
     * <p>
     * The {@code statistics} instance is updated accordingly to whether the CTP request was carried out successfully or not.
     * If an exception was thrown on executing the request to CTP,
     * the optional error callback specified in the {@code syncOptions} is called.
     *
     * @param categoryDrafts the list of new category drafts to sync to the CTP project.
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
                    failSync(format("CategoryDraft with name: %s doesn't have an externalId.",
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
     * Given a category draft {@link CategoryDraft} with an externalId, this method tries to fetch the existing category
     * from CTP project stored in the {@code syncOptions} instance of this class. It then either creates a new category,
     * if none exist with the same external id, or update the existing category.
     * <p>
     * The {@code statistics} instance is updated accordingly to whether the CTP request was carried out successfully or not.
     * If an exception was thrown on executing the request to CTP,
     * the optional error callback specified in the {@code syncOptions} is called.
     *
     * @param categoryDraft the category draft where we get the new data.
     */
    private void createOrUpdateCategory(@Nonnull final CategoryDraft categoryDraft) {
        final String externalId = categoryDraft.getExternalId();
        try {
            final Category oldCategory = fetchOldCategoryByExternalId(externalId);
            if (oldCategory != null) {
                syncCategories(oldCategory, categoryDraft);
            } else {
                createCategory(categoryDraft);
            }
        } catch (SphereException e) {
            failSync(format("Failed to fetch category with external id" +
                            " '%s' in CTP project with key '%s",
                    externalId, this.syncOptions.getCtpClient().getClientConfig().getProjectKey()), e);
        }
    }

    /**
     * Given an {@code externalId} this method uses {@code this} instance's injected {@link CategoryService} to fetch
     * a Category with this {@code externalId} from CTP. The service returns a
     * {@link CompletionStage&lt;Optional&lt;Category&gt;&gt;}, which this method blocks the execution of the code to
     * complete this completion stage and return the resultant {@link Category} wrapped in the {@link Optional}, if one
     * exists. If no {@link Category} exists with such {@code externalId}, this method returns {@code null}.
     *
     * @param externalId the externalId by which a {@link Category} should be fetched from the CTP project.
     * @return {@link Category} with the {@code externalId} specified if exists, or {@code null} otherwise.
     */
    @Nullable
    private Category fetchOldCategoryByExternalId(@Nullable final String externalId) {
        final CompletionStage<Optional<Category>> oldCategoryOptionalStage =
            this.categoryService.fetchCategoryByExternalId(externalId);
        final Optional<Category> oldCategoryOptional = oldCategoryOptionalStage.toCompletableFuture().join();
        return oldCategoryOptional.orElse(null);
    }

    /**
     * Given a {@link CategoryDraft}, this method issues a blocking request to the CTP project defined by the client
     * configuration stored in the {@code syncOptions} instance of this class to create a category with the same fields as
     * this category draft.
     * <p>
     * The {@code statistics} instance is updated accordingly to whether the CTP request was carried out successfully or not.
     * If an exception was thrown on executing the request to CTP,
     * the optional error callback specified in the {@code syncOptions} is called.
     *
     * @param categoryDraft the category draft to create the category from.
     */
    private void createCategory(@Nonnull final CategoryDraft categoryDraft) {
        try {
            this.categoryService.createCategory(categoryDraft).toCompletableFuture().join();
            this.statistics.incrementCreated();
        } catch (SphereException e) {
            failSync(format("Failed to create category with external id" +
                            " '%s' in CTP project with key '%s",
                    categoryDraft.getExternalId(), this.syncOptions.getCtpClient().getClientConfig().getProjectKey()), e);
        }
    }

    /**
     * Given an existing {@link Category} and a new {@link CategoryDraft}, this method calculates all the update actions
     * required to synchronize the existing category to be the same as the new one. If there are update actions found, a
     * request is made to CTP to update the existing category, otherwise it doesn't issue a request.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new data.
     */
    private void syncCategories(@Nonnull final Category oldCategory,
                                @Nonnull final CategoryDraft newCategory) {
        final List<UpdateAction<Category>> updateActions =
                CategorySyncUtils.buildActions(oldCategory, newCategory, this.syncOptions, this.typeService);
        if (!updateActions.isEmpty()) {
            updateCategory(oldCategory, updateActions);
        }
    }

    /**
     * Given a {@link Category} and a {@link List} of {@link UpdateAction} elements, this method issues a blocking request
     * to the CTP project defined by the client configuration stored in the {@code syncOptions} instance of this class to
     * update the specified category with this list of update actions.
     * <p>
     * The {@code statistics} instance is updated accordingly to whether the CTP request was carried out successfully or not.
     * If an exception was thrown on executing the request to CTP,
     * the optional error callback specified in the {@code syncOptions} is called.
     *
     * @param category      the category to update.
     * @param updateActions the list of update actions to update the category with.
     */
    private void updateCategory(@Nonnull final Category category,
                                @Nonnull final List<UpdateAction<Category>> updateActions) {
        try {
            this.categoryService.updateCategory(category, updateActions).toCompletableFuture().join();
            this.statistics.incrementUpdated();
        } catch (SphereException e) {
            failSync(format("Failed to update category with id" +
                            " '%s' in CTP project with key '%s",
                    category.getId(), this.syncOptions.getCtpClient().getClientConfig().getProjectKey()), e);
        }
    }

    /**
     * Given a reason message as {@link String} and {@link Throwable} exception, this method calls the optional error
     * callback specified in the {@code syncOptions} and updates the {@code statistics} instance by incrementing the
     * total number of failed resources to sync.
     *
     * @param reason    the reason of failure.
     * @param exception the exception that occurred, if any.
     */
    private void failSync(@Nonnull final String reason, @Nullable final Throwable exception) {
        this.syncOptions.applyErrorCallback(reason, exception);
        this.statistics.incrementFailed();
    }
}
