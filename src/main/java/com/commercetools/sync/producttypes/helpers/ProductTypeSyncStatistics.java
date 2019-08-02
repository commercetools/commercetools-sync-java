package com.commercetools.sync.producttypes.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.producttypes.ProductTypeSync;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.producttypes.ProductType;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

public class ProductTypeSyncStatistics extends BaseSyncStatistics {

    /**
     * The following {@link Map} ({@code productTypeKeysWithMissingParents}) represents productTypes with
     * missing parents.
     *
     * <ul>
     * <li>key: key of the missing parent productType</li>
     * <li>value: a set of the parent's children productType keys</li>
     * </ul>
     *
     * <p>The map is thread-safe (by instantiating it with {@link ConcurrentHashMap}) because it is accessed/modified in
     * a concurrent context, specifically when updating products in parallel in
     * {@link ProductTypeSync#updateProductType(ProductType, ProductTypeDraft, List)} .
     */
    private ConcurrentHashMap<String, Set<Pair<String, UpdateAction<ProductType>>>> productTypeKeysWithMissingParents =
        new ConcurrentHashMap<>();

    /**
     * Builds a summary of the product type sync statistics instance that looks like the following example:
     *
     * <p>"Summary: 2 product types were processed in total (0 created, 0 updated and 0 failed to sync)."
     *
     * @return a summary message of the product types sync statistics instance.
     */
    @Override
    public String getReportMessage() {
        reportMessage = format(
            "Summary: %s product types were processed in total (%s created, %s updated, %s failed to sync"
                + "and %s productTypes with a missing parent).",
            getProcessed(), getCreated(), getUpdated(), getFailed(), getNumberOfProductTypesWithMissingParents());

        return reportMessage;
    }

    /**
     * Returns the total number of categories with missing parents.
     *
     * @return the total number of categories with missing parents.
     */
    public int getNumberOfProductTypesWithMissingParents() {
        return productTypeKeysWithMissingParents.values()
                                                .stream()
                                                .mapToInt(Set::size)
                                                .sum();
    }

    public Map<String, Set<Pair<String, UpdateAction<ProductType>>>> getProductTypeKeysWithMissingParents() {
        return Collections.unmodifiableMap(productTypeKeysWithMissingParents);
    }

    /**
     * This method checks if there is an entry with the key of the {@code missingParentCategoryKey} in the
     * {@code categoryKeysWithMissingParents}, if there isn't it creates a new entry with this parent key and as a value
     * a new set containing the {@code childKey}. Otherwise, if there is already, it just adds the
     * {@code categoryKey} to the existing set.
     *
     * @param missingParentKey the key of the missing parent.
     * @param childPair                 the key of the category with a missing parent.
     */
    public void putMissingParentChildKey(@Nonnull final String missingParentKey,
                                         @Nonnull final Pair<String, UpdateAction<ProductType>> childPair) {

        final Set<Pair<String, UpdateAction<ProductType>>> missingParentChildrenActions =
            productTypeKeysWithMissingParents.get(missingParentKey);

        if (missingParentChildrenActions != null) {
            missingParentChildrenActions.add(childPair);
        } else {
            final Set<Pair<String, UpdateAction<ProductType>>> newChildKeys = new HashSet<>();
            newChildKeys.add(childPair);
            productTypeKeysWithMissingParents.put(missingParentKey, newChildKeys);
        }
    }

    public void removeMissingParentKeys(@Nonnull final Set<String> keys) {
        keys.forEach(key -> productTypeKeysWithMissingParents.remove(key));
    }
}
