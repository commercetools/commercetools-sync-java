package com.commercetools.sync.categories;

import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.categories.utils.CategorySyncUtils;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static com.commercetools.sync.commons.helpers.ReferenceResolver.resolveReferences;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class CategorySync extends BaseSync<CategoryDraft, CategorySyncStatistics, CategorySyncOptions> {
    private static final String CTP_CATEGORY_UPDATE_FAILED = "Failed to update category with externalId:'%s'."
        + " Reason: %s";
    private static final String CTP_CATEGORY_FETCH_FAILED = "Failed to fetch category with externalId:'%s'."
        + " Reason: %s";
    private static final String CTP_CATEGORY_CREATE_FAILED = "Failed to create category with externalId:'%s'."
        + " Reason: %s";
    private static final String CTP_CATEGORY_SYNC_FAILED = "Failed to sync category with externalId:'%s'."
        + " Reason: %s";
    private static final String CATEGORY_DRAFT_EXTERNAL_ID_NOT_SET = "CategoryDraft with name: %s doesn't have an"
        + " externalId.";
    private static final String CATEGORY_DRAFT_IS_NULL = "CategoryDraft is null.";
    private static final String CATEGORY_DRAFT_REFERENCE_RESOLUTION_FAILED = "Failed to resolve reference on "
        + "CategoryDraft with externalId:'%s'. Reason: %s";

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
            new TypeServiceImpl(syncOptions.getCtpClient()),
            new CategoryServiceImpl(syncOptions.getCtpClient()));
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
     * category exist with such external id, a new category, identical to this new category draft is created.
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
    protected CompletionStage<CategorySyncStatistics> process(@Nonnull final List<CategoryDraft> categoryDrafts) {
        for (CategoryDraft categoryDraft : categoryDrafts) {
            if (categoryDraft != null) {
                final String externalId = categoryDraft.getExternalId();
                if (isNotBlank(externalId)) {
                    createOrUpdateCategory(categoryDraft);
                } else {
                    final String errorMessage = format(CATEGORY_DRAFT_EXTERNAL_ID_NOT_SET, categoryDraft.getName());
                    handleError(errorMessage, null);
                }
            } else {
                handleError(CATEGORY_DRAFT_IS_NULL, null);
            }
            statistics.incrementProcessed();
        }
        return CompletableFuture.completedFuture(statistics);
    }

    /**
     * Given a category draft {@link CategoryDraft} with an externalId, this method blocks execution to try to fetch the
     * existing category from CTP project stored in the {@code syncOptions} instance of this class. If successful, it
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
            categoryService.fetchCategoryByExternalId(externalId)
                           .thenCompose(fetchedCategoryOptional ->
                               fetchedCategoryOptional
                                   .map(category -> syncCategories(category, categoryDraft))
                                   .orElseGet(() -> createCategory(categoryDraft)))
                           .exceptionally(exception -> {
                               final String errorMessage = format(CTP_CATEGORY_FETCH_FAILED,
                                   categoryDraft.getExternalId(), exception.getMessage());
                               handleError(errorMessage, exception);
                               return null;
                           })
                           .toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException exception) {
            final String errorMessage = format(CTP_CATEGORY_SYNC_FAILED, categoryDraft.getExternalId(),
                exception.getMessage());
            handleError(errorMessage, exception);
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
        return categoryService.createCategory(categoryDraft)
                              .thenAccept(createdCategory -> statistics.incrementCreated())
                              .exceptionally(exception -> {
                                  final String errorMessage = format(CTP_CATEGORY_CREATE_FAILED,
                                      categoryDraft.getExternalId(), exception.getMessage());
                                  handleError(errorMessage, exception);
                                  return null;
                              });
    }

    /**
     * Given an existing {@link Category} and a new {@link CategoryDraft}, first resolves all references on the category
     * draft, then it calculates all the update actions required to synchronize the existing category to be the same as
     * the new one. If there are update actions found, a request is made to CTP to update the existing category,
     * otherwise it doesn't issue a request.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new data.
     * @return a future monad which can contain an empty result.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<Void> syncCategories(@Nonnull final Category oldCategory,
                                                 @Nonnull final CategoryDraft newCategory) {
        try {
            final CategoryDraft resolvedReferencesCategoryDraft =
                resolveReferences(newCategory, typeService, categoryService, syncOptions);
            final List<UpdateAction<Category>> updateActions =
                CategorySyncUtils.buildActions(oldCategory, resolvedReferencesCategoryDraft, syncOptions);
            if (!updateActions.isEmpty()) {
                return updateCategory(oldCategory, updateActions);
            }
        } catch (@Nonnull final ReferenceResolutionException exception) {
            final String errorMessage = format(CATEGORY_DRAFT_REFERENCE_RESOLUTION_FAILED, newCategory.getExternalId(),
                exception.getMessage());
            handleError(errorMessage, exception);
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
        return categoryService.updateCategory(category, updateActions)
                              .thenAccept(updatedCategory -> statistics.incrementUpdated())
                              .exceptionally(exception -> {
                                  final String errorMessage = format(CTP_CATEGORY_UPDATE_FAILED,
                                      category.getExternalId(), exception.getMessage());
                                  handleError(errorMessage, exception);
                                  return null;
                              });
    }

    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed categories to sync.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     * @param exception    The exception that called caused the failure, if any.
     */
    private void handleError(@Nonnull final String errorMessage, @Nullable final Throwable exception) {
        syncOptions.applyErrorCallback(errorMessage, exception);
        statistics.incrementFailed();
    }
}
