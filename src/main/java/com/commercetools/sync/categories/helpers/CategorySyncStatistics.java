package com.commercetools.sync.categories.helpers;


import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

public class CategorySyncStatistics extends BaseSyncStatistics {

    /**
     * The following {@link Map} ({@code categoryKeysWithMissingParents}) represents categories with
     * missing parents.
     *
     * <ul>
     *     <li>key: key of the missing parent category</li>
     *     <li>value: a list of the parent's children category keys</li>
     * </ul>
     *
     * <p>The map is thread-safe (by instantiating it with {@link ConcurrentHashMap}) because it is accessed/modified in
     * a concurrent context, specifically when updating products in parallel in
     * {@link com.commercetools.sync.categories.CategorySync#updateCategory(Category, CategoryDraft, List)}.
     *
     */
    private Map<String, List<String>> categoryKeysWithMissingParents = new ConcurrentHashMap<>();

    public CategorySyncStatistics() {
        super();
    }

    /**
     * Builds a summary of the category sync statistics instance that looks like the following example:
     *
     * <p>"Summary: 2 categories were processed in total (0 created, 0 updated and 0 categories failed to sync)."
     *
     * @return a summary message of the category sync statistics instance.
     */
    @Override
    public String getReportMessage() {
        reportMessage = format("Summary: %s categories were processed in total "
                + "(%s created, %s updated, %s failed to sync and %s categories with a missing parent).",
            getProcessed(), getCreated(), getUpdated(), getFailed(), getNumberOfCategoriesWithMissingParents());
        return reportMessage;
    }

    /**
     * Returns the total number of categories with missing parents.
     *
     * @return the total number of categories with missing parents.
     */
    public int getNumberOfCategoriesWithMissingParents() {
        return categoryKeysWithMissingParents.values()
                                             .stream()
                                             .filter(Objects::nonNull)
                                             .mapToInt(List::size)
                                             .sum();
    }

    public Map<String, List<String>> getCategoryKeysWithMissingParents() {
        return Collections.unmodifiableMap(categoryKeysWithMissingParents);
    }

    public void setCategoryKeysWithMissingParents(@Nonnull final ConcurrentHashMap<String, List<String>>
                                                      categoryKeysWithMissingParents) {
        this.categoryKeysWithMissingParents = categoryKeysWithMissingParents;
    }

    @Nullable
    public List<String> getMissingParentCategoryChildrenKeys(@Nonnull final String missingParentCategoryKey) {
        return categoryKeysWithMissingParents.get(missingParentCategoryKey);
    }

    public void putMissingParentCategoryChildrenKeys(@Nonnull final String missingParentCategoryKey,
                                                     @Nonnull final List<String> childrenKeys) {
        categoryKeysWithMissingParents.put(missingParentCategoryKey, childrenKeys);
    }

    /**
     * Given a categoryKey {@code childCategoryKey} this method, checks in the {@code categoryKeysWithMissingParents}
     * if it exists as a child to a missing parent, and returns the key of that missing parent. Otherwise, it returns
     * null.
     * @param childCategoryKey key of the category to look if it has a missing parent.
     * @return the key of the parent category.
     */
    @Nonnull
    public Optional<String> getMissingParentKey(@Nonnull final String childCategoryKey) {
        return categoryKeysWithMissingParents.entrySet()
                                             .stream()
                                             .filter(missingParentEntry -> missingParentEntry.getValue().contains(
                                                 childCategoryKey))
                                             .findFirst()
                                             .map(Map.Entry::getKey);
    }

    /**
     * Given a child {@code categoryKey} this method removes its occurrences from the map
     * {@code categoryKeysWithMissingParents}.
     *
     * <p>NOTE: When all the children keys of a missing parent are removed, the value of the map entry will be
     * an empty list. i.e. the entry itself will not be removed. However, this could be investigated whether removing
     * the entry at all when the list is empty will affect the algorithm. TODO: GITHUB ISSUE#
     *
     * @param childCategoryKey the child category key to remove from {@code categoryKeysWithMissingParents}
     */
    public void removeChildCategoryKeyFromMissingParentsMap(@Nonnull final String childCategoryKey) {
        categoryKeysWithMissingParents.forEach((key, value) -> value.remove(childCategoryKey));
    }
}
