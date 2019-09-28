package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

public class ProductSyncStatistics extends BaseSyncStatistics {

    /**
     * The following {@link Map} ({@code categoryKeysWithMissingParents}) represents categories with
     * missing parents.
     *
     * <ul>
     *     <li>key: key of the missing parent category</li>
     *     <li>value: a set of the parent's children category keys</li>
     * </ul>
     *
     * <p>The map is thread-safe (by instantiating it with {@link ConcurrentHashMap}) because it is accessed/modified in
     * a concurrent context, specifically when updating products in parallel in
     * {@link com.commercetools.sync.categories.CategorySync#updateCategory(Category, CategoryDraft, List)}.
     *
     */
    private ConcurrentHashMap<String, Set<String>> productKeysWithMissingParents = new ConcurrentHashMap<>();


    /**
     * Builds a summary of the product sync statistics instance that looks like the following example:
     *
     * <p>"Summary: 2 products were processed in total (0 created, 0 updated and 0 failed to sync)."
     *
     * @return a summary message of the product sync statistics instance.
     */
    @Override
    public String getReportMessage() {
        reportMessage = format("Summary: %s products were processed in total "
                + "(%s created, %s updated, %s failed to sync and %s products with a missing reference).",
            getProcessed(), getCreated(), getUpdated(), getFailed(), getNumberOfProductsWithMissingParents());
        return reportMessage;
    }

    /**
     * Returns the total number of categories with missing parents.
     *
     * @return the total number of categories with missing parents.
     */
    public int getNumberOfProductsWithMissingParents() {
        return productKeysWithMissingParents.values()
                                             .stream()
                                             .mapToInt(Set::size)
                                             .sum();
    }

    public Map<String, Set<String>> getProductKeysWithMissingParents() {
        return Collections.unmodifiableMap(productKeysWithMissingParents);
    }

    public void setProductKeysWithMissingParents(@Nonnull final ConcurrentHashMap<String, Set<String>>
                                                     productKeysWithMissingParents) {
        this.productKeysWithMissingParents = productKeysWithMissingParents;
    }

    /**
     * This method checks if there is an entry with the key of the {@code missingParentCategoryKey} in the
     * {@code categoryKeysWithMissingParents}, if there isn't it creates a new entry with this parent key and as a value
     * a new set containing the {@code childKey}. Otherwise, if there is already, it just adds the
     * {@code categoryKey} to the existing set.
     *
     * @param missingParentProductKey the key of the missing parent.
     * @param childKey                 the key of the category with a missing parent.
     */
    public void putMissingParentProductChildKey(@Nonnull final String missingParentProductKey,
                                                @Nonnull final String childKey) {
        final Set<String> missingParentProductChildrenKeys =
            productKeysWithMissingParents.get(missingParentProductKey);
        if (missingParentProductChildrenKeys != null) {
            missingParentProductChildrenKeys.add(childKey);
        } else {
            final Set<String> newChildProductKeys = new HashSet<>();
            newChildProductKeys.add(childKey);
            productKeysWithMissingParents.put(missingParentProductKey, newChildProductKeys);
        }
    }


    /**
     * Given a child {@code categoryKey} this method removes its occurrences from the map
     * {@code categoryKeysWithMissingParents}.
     *
     * <p>NOTE: When all the children keys of a missing parent are removed, the value of the map entry will be
     * an empty list. i.e. the entry itself will not be removed. However, this could be investigated whether removing
     * the entry at all when the list is empty will affect the algorithm. TODO: RELATED BUT NOT SAME AS GITHUB ISSUE#77
     *
     * @param childProductKey the child category key to remove from {@code categoryKeysWithMissingParents}
     */
    public void removeChildProductKeyFromMissingParentsMap(@Nonnull final String childProductKey) {
        productKeysWithMissingParents.forEach((key, value) -> value.remove(childProductKey));
    }

    @Nullable
    public Set<String> removeAndGetReferencingKeys(@Nonnull final String key) {
        return productKeysWithMissingParents.remove(key);
    }
}
