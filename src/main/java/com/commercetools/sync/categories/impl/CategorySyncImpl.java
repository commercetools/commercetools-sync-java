package com.commercetools.sync.categories.impl;

import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.utils.CategorySyncUtils;
import com.commercetools.sync.commons.helpers.SyncError;
import com.commercetools.sync.commons.helpers.SyncResult;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.commercetools.sync.commons.utils.StatisticsUtils.getHorizontalLine;
import static java.lang.String.format;

public class CategorySyncImpl implements CategorySync {
    private int updatedCategories;
    private int createdCategories;
    private int failedCategories;
    private int processedCategories;
    private final Logger LOGGER = LoggerFactory.getLogger(CategorySyncImpl.class);

    private CategorySyncOptions options;
    private TypeService typeService;
    private CategoryService categoryService;

    public CategorySyncImpl(CategorySyncOptions options, TypeService typeService, CategoryService categoryService) {
        this.options = options;
        this.updatedCategories = 0;
        this.createdCategories = 0;
        this.failedCategories = 0;
        this.processedCategories = 0;
        this.typeService = typeService;
        this.categoryService = categoryService;
    }

    @Override
    public void syncCategories(@Nonnull List<Category> categories) {
    }

    // TODO: REFACTOR
    @Override
    public void syncCategoryDrafts(@Nonnull List<CategoryDraft> categoryDrafts) {
        processedCategories = categoryDrafts.size();
        LOGGER.info(format("About to sync %d category drafts into CTP project with key '%s'."
                , categoryDrafts.size(), options.getCtpProjectKey()));
        final SyncResult<Category> syncResult = SyncResult.emptyResult();


        for (int i = 0; i < categoryDrafts.size(); i++) { // TODO: HANDLE IF THERE IS A NULL OBJECT IN THE LIST!
            SyncResult<Category> currentSyncResult = SyncResult.emptyResult();
            CategoryDraft newCategoryDraft = categoryDrafts.get(i);
            String externalId = newCategoryDraft != null ? newCategoryDraft.getExternalId() : null;
            if (externalId != null) { // TODO NEED TO PARALLELISE!
                Category oldCategory = categoryService.fetchCategoryByExternalId(externalId);
                if (oldCategory == null) {
                    currentSyncResult = createCategory(newCategoryDraft);
                } else {
                    currentSyncResult = CategorySyncUtils.buildActions(oldCategory, newCategoryDraft, typeService);
                    final List<UpdateAction<Category>> updateActions = currentSyncResult.getUpdateActions();
                    if (!updateActions.isEmpty()) {
                        updateCategory(oldCategory, currentSyncResult);
                    }
                }
            }
            syncResult.mergeStatistics(currentSyncResult);
        }
        LOGGER.info(getSummary(syncResult));
    }

    @Nullable
    private SyncResult<Category> createCategory(@Nonnull final CategoryDraft newCategory) {
        SyncResult<Category> syncResult = SyncResult.emptyResult();
        try {
            categoryService.createCategory(newCategory);
            createdCategories++;
        } catch (Exception e) {
            syncResult = SyncResult.ofError(SyncError.of(null, format("Failed to create category with external id" +
                                    " '%s' in CTP project with key '%s",
                            newCategory.getExternalId(), options.getCtpProjectKey()), e));
            failedCategories++;
        }
        return syncResult;
    }

    @Nullable
    private SyncResult<Category> updateCategory(@Nonnull final Category category,
                                                @Nonnull final SyncResult<Category> syncResult) {
        try {
            categoryService.updateCategory(category, syncResult.getUpdateActions());
            updatedCategories++;
        } catch (Exception e) {
            syncResult.addError(
                    SyncError.of(category.getId(), format("Failed to update category with id" +
                                    " '%s' in CTP project with key '%s",
                            category.getId(), options.getCtpProjectKey()), e));
            failedCategories++;
        }
        return syncResult;
    }


    @Override
    public String getSummary(@Nonnull final SyncResult<Category> syncResult) {
        return format("Category Sync completed successfully!\n" +
                        "Sync Summary:\n%s(%s) categories were processed in total." +
                        "\n\t+ (%s) categories created." +
                        "\n\t+ (%s) categories updated." +
                        "\n\t+ (%s) categories failed to sync." +
                        "\n\n" +
                        "Detailed Report of Sync Statistics:\n" +
                        "%s\n%s",
                getHorizontalLine(15),
                processedCategories,
                createdCategories,
                updatedCategories,
                failedCategories,
                getHorizontalLine(35),
                syncResult.getResultStatistics());
    }

}
