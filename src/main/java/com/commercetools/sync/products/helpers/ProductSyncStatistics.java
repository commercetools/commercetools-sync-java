package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

public class ProductSyncStatistics extends BaseSyncStatistics {

    private ConcurrentHashMap<String, Set<String>> productKeysWithMissingParents = new ConcurrentHashMap<>();

    /**
     * Builds a summary of the product sync statistics instance that looks like the following example:
     *
     * <p>"Summary: 4 product(s) were processed in total (1 created, 1 updated, 1 failed to sync
     * and 1 product(s) with a missing reference(s))."
     *
     * @return a summary message of the product sync statistics instance.
     */
    @Override
    public String getReportMessage() {
        reportMessage = format("Summary: %s product(s) were processed in total "
                + "(%s created, %s updated, %s failed to sync and %s product(s) with missing reference(s)).",
            getProcessed(), getCreated(), getUpdated(), getFailed(), getNumberOfProductsWithMissingParents());
        return reportMessage;
    }

    /**
     * Returns the total number of categories with missing parents.
     *
     * @return the total number of categories with missing parents.
     */
    public int getNumberOfProductsWithMissingParents() {
        return productKeysWithMissingParents
            .values()
            .stream()
            .mapToInt(Set::size)
            .sum();
    }

    /**
     * This method checks if there is an entry with the key of the {@code missingParentCategoryKey} in the
     * {@code productKeysWithMissingParents}, if there isn't it creates a new entry with this parent key and as a value
     * a new set containing the {@code childKey}. Otherwise, if there is already, it just adds the
     * {@code childKey} to the existing set.
     *
     * @param parentKey the key of the missing parent.
     * @param childKey  the key of the product with a missing parent.
     */
    public void addMissingDependency(@Nonnull final String parentKey, @Nonnull final String childKey) {

        final Set<String> missingParentProductChildrenKeys =
            productKeysWithMissingParents.get(parentKey);
        if (missingParentProductChildrenKeys != null) {
            missingParentProductChildrenKeys.add(childKey);
        } else {
            final Set<String> newChildProductKeys = new HashSet<>();
            newChildProductKeys.add(childKey);
            productKeysWithMissingParents.put(parentKey, newChildProductKeys);
        }
    }

    @Nullable
    public Set<String> removeAndGetReferencingKeys(@Nonnull final String key) {
        return productKeysWithMissingParents.remove(key);
    }
}
