package com.commercetools.sync.categories.utils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.expansion.CategoryExpansionModel;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.AssetReferenceReplacementUtils.replaceAssetsReferencesIdsWithKeys;
import static com.commercetools.sync.commons.utils.CustomTypeReferenceReplacementUtils.replaceCustomTypeIdWithKeys;
import static com.commercetools.sync.commons.utils.SyncUtils.replaceReferenceIdWithKey;

public final class CategoryReferenceReplacementUtils {

    /**
     * Takes a list of Categories that are supposed to have their custom type, parent category and asset custom type
     * references expanded in order to be able to fetch the keys and replace the reference ids with the corresponding
     * keys and then return a new list of category drafts with their references containing keys instead of the ids.
     * Note that if the references are not expanded for a category, the reference ids will not be replaced with keys and
     * will still have their ids in place.
     *
     * @param categories the categories to replace their reference ids with keys
     * @return a list of category drafts with keys instead of ids for references.
     */
    @Nonnull
    public static List<CategoryDraft> replaceCategoriesReferenceIdsWithKeys(@Nonnull final List<Category> categories) {
        return categories
            .stream()
            .map(category -> {
                final CustomFieldsDraft customTypeWithKeysInReference = replaceCustomTypeIdWithKeys(category);
                @SuppressWarnings("ConstantConditions") // NPE checked in replaceReferenceIdWithKey
                final ResourceIdentifier<Category> parentWithKeyInReference = replaceReferenceIdWithKey(
                    category.getParent(), () -> Category.referenceOfId(category.getParent().getObj().getKey()));
                final List<AssetDraft> assetDraftsWithKeyInReference =
                    replaceAssetsReferencesIdsWithKeys(category.getAssets());

                return CategoryDraftBuilder.of(category)
                                           .custom(customTypeWithKeysInReference)
                                           .parent(parentWithKeyInReference)
                                           .assets(assetDraftsWithKeyInReference)
                                           .build();
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

    private CategoryReferenceReplacementUtils() {
    }
}
