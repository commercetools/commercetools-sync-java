package com.commercetools.sync.categories.utils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.expansion.CategoryExpansionModel;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.AssetReferenceReplacementUtils.replaceAssetsReferencesIdsWithKeys;
import static com.commercetools.sync.commons.utils.CustomTypeReferenceReplacementUtils.replaceCustomTypeIdWithKeys;
import static io.sphere.sdk.models.ResourceIdentifier.ofKey;

/**
 * Util class which provides utilities that can be used when syncing resources from a source commercetools project
 * to a target one.
 */
public final class CategoryReferenceReplacementUtils {

    private CategoryReferenceReplacementUtils() {
    }

    /**
     * Takes a list of Categories that are supposed to have their custom type and asset custom type
     * references expanded in order to be able to fetch the keys and replace the resource identifier ids with the
     * corresponding keys and then return a new list of category drafts with their resource identifiers containing keys
     * instead of the ids. Note that if the references are not expanded for a category, the resource identifier ids will
     * not be replaced with keys and will still have their ids in place.
     *
     * @param categories the categories to replace their resource identifier ids with keys
     * @return a list of category drafts with keys set on the resource identifiers.
     */
    @Nonnull
    public static List<CategoryDraft> replaceCategoriesReferenceIdsWithKeys(@Nonnull final List<Category> categories ) {

        Map<String, String> idToKeyMap = categories.stream()
            .collect(Collectors.toMap(Category::getId, Category::getKey));
          return categories
            .stream()
            .map(category -> {
                final CustomFieldsDraft customTypeWithKeysInReference = replaceCustomTypeIdWithKeys(category);
                @SuppressWarnings("ConstantConditions") // NPE checked in replaceReferenceIdWithKey
                final List<AssetDraft> assetDraftsWithKeyInReference =
                    replaceAssetsReferencesIdsWithKeys(category.getAssets());
                CategoryDraftBuilder builder = CategoryDraftBuilder.of(category)
                    .custom(customTypeWithKeysInReference)
                    .assets(assetDraftsWithKeyInReference);
                Reference<Category> parent = category.getParent();
                if (parent != null && parent.getObj() != null) {
                    builder = builder.parent(ofKey(parent.getObj().getKey()));
                }
                return builder.build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Builds a {@link CategoryQuery} for fetching categories from a source CTP project with all the needed references
     * expanded for the sync:
     * <ul>
     *     <li>Custom Type</li>
     *     <li>Assets Custom Types</li>
     *     <li>Parent Category</li>
     * </ul>
     *
     * <p>Note: Please only use this util if you desire to sync all the aforementioned references from
     * a source commercetools project. Otherwise, it is more efficient to build the query without expansions, if they
     * are not needed, to avoid unnecessarily bigger payloads fetched from the source project.
     *
     * @return the query for fetching categories from the source CTP project with all the aforementioned references
     *         expanded.
     */
    public static CategoryQuery buildCategoryQuery() {
        return CategoryQuery.of()
            .withLimit(QueryExecutionUtils.DEFAULT_PAGE_SIZE)
            .withExpansionPaths(ExpansionPath.of("custom.type"))
            .plusExpansionPaths(ExpansionPath.of("assets[*].custom.type"))
            .plusExpansionPaths(CategoryExpansionModel::parent);
    }
}
