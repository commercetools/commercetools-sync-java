package com.commercetools.sync.categories;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryUpdateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.queries.PagedQueryResult;
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
    @Nonnull
    private CategorySyncOptions options;

    public CategorySyncImpl(CategorySyncOptions options) {
        this.options = options;
        this.updatedCategories = 0;
        this.createdCategories = 0;
        this.failedCategories = 0;
        this.processedCategories = 0;
    }

    // TODO: NEED TO DELETE CATEGORY IF IT DOESN'T EXIST IN CATEGORYDRAFTS
    // TODO: MAYBE BETTER TO EXPECT A MAP INSTEAD OF CATEGORY DRAFTS
    // TODO: REFACTOR
    @Override
    public void syncCategoryDrafts(@Nonnull List<CategoryDraft> categoryDrafts) {
        processedCategories = categoryDrafts.size();
        LOGGER.info(format("About to sync %d category drafts into CTP project with key '%s'."
                , categoryDrafts.size(), options.getCtpProjectKey()));
        for (int i = 0; i < categoryDrafts.size(); i++) {
            CategoryDraft newCategoryDraft = categoryDrafts.get(i);
            if (newCategoryDraft != null && newCategoryDraft.getExternalId()!=null) { // TODO CHECK THIS!
                CategoryQuery categoryQuery = CategoryQuery.of().byExternalId(newCategoryDraft.getExternalId());
                final PagedQueryResult<Category> pagedQueryResult = options.getCTPclient().executeBlocking(categoryQuery);
                Category existingCategory = pagedQueryResult.head().orElse(null);
                if (existingCategory == null) {
                    createCategory(newCategoryDraft);
                } else {
                    List<UpdateAction<Category>> updateActions = CategorySyncUtils.buildActions(existingCategory, newCategoryDraft);
                    if (!updateActions.isEmpty()) {
                        updateCategory(existingCategory, updateActions);
                    }

                }
            }
        }
        LOGGER.info(getSummary());
    }

    @Nullable
    private Category updateCategory(@Nonnull final Category category, List<UpdateAction<Category>> updateActions) {
        Category updatedCategory = null;
        try {
            final CategoryUpdateCommand categoryUpdateCommand = CategoryUpdateCommand.of(category, updateActions);
            updatedCategory = options.getCTPclient().executeBlocking(categoryUpdateCommand);
            updatedCategories++;
        } catch (Exception e) {
            LOGGER.error(format("Failed to update category with id" +
                            " '%s' in CTP project with key '%s",
                    category.getId(), options.getCtpProjectKey()), e);
            failedCategories++;
        }
        return updatedCategory;
    }

    @Nullable
    private Category createCategory(@Nonnull final CategoryDraft newCategory) {
        Category category = null;
        try {
            final CategoryCreateCommand categoryCreateCommand = CategoryCreateCommand.of(newCategory);
            category = options.getCTPclient().executeBlocking(categoryCreateCommand);
            createdCategories++;
        } catch (Exception e) {
            LOGGER.error(format("Failed to create category with external id" +
                            " '%s' in CTP project with key '%s",
                    newCategory.getExternalId(), options.getCtpProjectKey()), e);
            failedCategories++;
        }
        return category;
    }

    @Override
    public void syncCategories(@Nonnull List<Category> categories) {
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
