package com.commercetools.sync.categories;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryUpdateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.queries.TypeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public void syncCategories(@Nonnull List<Category> categories) {
    }

    // TODO: REFACTOR
    @Override
    public void syncCategoryDrafts(@Nonnull List<CategoryDraft> categoryDrafts) {
        processedCategories = categoryDrafts.size();
        LOGGER.info(format("About to sync %d category drafts into CTP project with key '%s'."
                , categoryDrafts.size(), options.getCtpProjectKey()));
        // cache types
        Map<String, String> typeKeyMap = getCustomTypeKeyMap();

        for (int i = 0; i < categoryDrafts.size(); i++) {
            CategoryDraft newCategoryDraft = categoryDrafts.get(i);
            if (newCategoryDraft != null && newCategoryDraft.getExternalId()!=null) { // TODO CHECK THIS!
                CategoryQuery categoryQuery = CategoryQuery.of()
                        .byExternalId(newCategoryDraft.getExternalId())
                        .plusExpansionPaths(ExpansionPath.of("custom.type")); //TODO: REMOVE REFERENCE EXPANSION FOR EFFICIENCY AND CACHE CUSTOM TYPES IN ADVANCE
                final PagedQueryResult<Category> pagedQueryResult = options.getCTPclient().executeBlocking(categoryQuery); // TODO: HANDLE PAGINATION
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

    /**
     * Cache a map of Types internal id -> key
     * // TODO: USE graphQL to get only keys
     * // TODO: UNIT TEST
     * // TODO: JAVA DOC
     */
    @Nonnull
    private Map<String, String> getCustomTypeKeyMap() {
        Map<String, String> typeKeyMap = new HashMap<>();
        try {
            final PagedQueryResult<Type> pagedQueryResult = options.getCTPclient().executeBlocking(TypeQuery.of());
            pagedQueryResult.getResults()
                    .forEach(type -> typeKeyMap.put(type.getId(), type.getKey()));
        } catch (Exception e) {
            LOGGER.error(format("Failed to fetch Types" +
                            "from CTP project with key '%s", options.getCtpProjectKey()), e);
        }
        return typeKeyMap;
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
