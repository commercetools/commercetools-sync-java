package com.commercetools.sync.categories.impl;

import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncUtils;
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
        for (int i = 0; i < categoryDrafts.size(); i++) {
            CategoryDraft newCategoryDraft = categoryDrafts.get(i);
            String externalId = newCategoryDraft != null ? newCategoryDraft.getExternalId() : null;
            if (externalId != null) { // TODO NEED TO PARALLELISE!
                Category existingCategory = categoryService.fetchCategoryByExternalId(externalId);
                if (existingCategory == null) {
                    createCategory(newCategoryDraft);
                } else {
                    List<UpdateAction<Category>> updateActions = CategorySyncUtils.buildActions(existingCategory, newCategoryDraft, typeService);
                    if (!updateActions.isEmpty()) {
                        updateCategory(existingCategory, updateActions);
                    }
                }
            }
        }
        LOGGER.info(getSummary());
    }

    @Nullable
    private Category createCategory(@Nonnull final CategoryDraft newCategory) {
        Category category = null;
        try {
            category = categoryService.createCategory(newCategory);
            createdCategories++;
        } catch (Exception e) {
            LOGGER.error(format("Failed to create category with external id" +
                            " '%s' in CTP project with key '%s",
                    newCategory.getExternalId(), options.getCtpProjectKey()), e);
            failedCategories++;
        }
        return category;
    }

    @Nullable
    private Category updateCategory(@Nonnull final Category category, List<UpdateAction<Category>> updateActions) {
        Category updatedCategory = null;
        try {
            updatedCategory = categoryService.updateCategory(category, updateActions);
            updatedCategories++;
        } catch (Exception e) {
            LOGGER.error(format("Failed to update category with id" +
                            " '%s' in CTP project with key '%s",
                    category.getId(), options.getCtpProjectKey()), e);
            failedCategories++;
        }
        return updatedCategory;
    }


    @Override
    public String getSummary() {
        return format("Category Sync completed successfully!\n" +
                        "Summary: (%s) categories were processed in total." +
                        "\n\t+ (%s) categories created." +
                        "\n\t+ (%s) categories updated." +
                        "\n\t+ (%s) categories failed to sync.",
                processedCategories,
                createdCategories,
                updatedCategories,
                failedCategories);
    }

}
