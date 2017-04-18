package com.commercetools.sync.categories.impl;

import com.commercetools.sync.categories.CategoryStatistics;
import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.utils.CategorySyncUtils;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.commercetools.sync.commons.helpers.BaseStatistics.getStatisticsAsJSONString;
import static java.lang.String.format;

public class CategorySyncImpl implements CategorySync {
    private final Logger LOGGER = LoggerFactory.getLogger(CategorySyncImpl.class);

    private CategorySyncOptions options;
    private CategoryStatistics statistics;

    public CategorySyncImpl(@Nonnull final CategorySyncOptions options) {
        this.options = options;
    }

    @Override
    public void syncCategories(@Nonnull final List<Category> categories) {
    }

    // TODO: REFACTOR
    @Override
    public void syncCategoryDrafts(@Nonnull final List<CategoryDraft> categoryDrafts) {
        statistics = new CategoryStatistics();
        LOGGER.info(format("About to sync %d category drafts into CTP project with key '%s'."
                , categoryDrafts.size(), options.getClientConfig().getProjectKey()));
        for (int i = 0; i < categoryDrafts.size(); i++) {
            statistics.incrementProcessed(); // Need to take care about null values.
            final CategoryDraft newCategoryDraft = categoryDrafts.get(i);
            final String externalId = newCategoryDraft != null ? newCategoryDraft.getExternalId() : null;
            if (externalId != null) { // TODO NEED TO PARALLELISE!
                Category oldCategory = options.getCategoryService().fetchCategoryByExternalId(externalId);
                if (oldCategory == null) {
                    createCategory(newCategoryDraft);
                } else {
                    final List<UpdateAction<Category>> updateActions =
                            CategorySyncUtils.buildActions(oldCategory, newCategoryDraft, options);
                    if (!updateActions.isEmpty()) {
                        updateCategory(oldCategory, updateActions);
                    }
                }
            }
        }
        statistics.calculateProcessingTime();
        LOGGER.info(getSummary());
    }

    @Nullable
    private Category createCategory(@Nonnull final CategoryDraft newCategory) {
        Category category = null;
        try {
            category = options.getCategoryService().createCategory(newCategory);
            statistics.incrementCreated();
        } catch (Exception e) {
            LOGGER.error(format("Failed to create category with external id" +
                            " '%s' in CTP project with key '%s",
                    newCategory.getExternalId(), options.getClientConfig().getProjectKey()), e);
            statistics.incrementFailed();
        }
        return category;
    }

    @Nullable
    private Category updateCategory(@Nonnull final Category category, List<UpdateAction<Category>> updateActions) {
        Category updatedCategory = null;
        try {
            updatedCategory = options.getCategoryService().updateCategory(category, updateActions);
            statistics.incrementUpdated();
        } catch (Exception e) {
            LOGGER.error(format("Failed to update category with id" +
                            " '%s' in CTP project with key '%s",
                    category.getId(), options.getClientConfig().getProjectKey()), e);
            statistics.incrementFailed();
        }
        return updatedCategory;
    }


    @Override
    public String getSummary() {
        return getStatisticsAsJSONString(statistics);
    }

}
