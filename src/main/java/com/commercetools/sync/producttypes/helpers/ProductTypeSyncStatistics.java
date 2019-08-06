package com.commercetools.sync.producttypes.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.AddAttributeDefinition;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
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
     * a concurrent context, specifically when updating products in parallel in TODO: WHICH METHODS ACCESS IT IN PARALLEL.
     */
    private
    ConcurrentHashMap<String, // -> missing referenced/parent productType Key
        ConcurrentHashMap<String, // -> actual productType key that is referencing the parent productType key
            ConcurrentHashMap.KeySetView<UpdateAction<ProductType>, Boolean>// -> The actions to apply
            >
        >
        missingProductTypeReferences = new ConcurrentHashMap<>();

    ProductTypeSyncStatistics (@Nonnull final ConcurrentHashMap<String,
        ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<UpdateAction<ProductType>, Boolean>>>
                                   missingProductTypeReferences) {
        this.missingProductTypeReferences = missingProductTypeReferences;
    }

    public ProductTypeSyncStatistics() {
    }

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
                + " and %s product types with a missing referenced productType reference"
                + " in a NestedType or a Set of NestedType attribute definition).",
            getProcessed(), getCreated(), getUpdated(), getFailed(), getNumberOfProductTypesWithMissingParents());

        return reportMessage;
    }

    /**
     * Returns the total number of categories with missing parents.
     *
     * @return the total number of categories with missing parents.
     */
    public int getNumberOfProductTypesWithMissingParents() {
        //TODO: This is wrong. This gets the number of ATTRIBUTES with missing references not PRODUCT TYPES.

        final Set<String> productTypesWithMissingReferences = new HashSet<>();

        missingProductTypeReferences.values()
                                    .stream()
                                    .map(ConcurrentHashMap::keySet)
                                    .flatMap(Collection::stream)
                                    .forEach(productTypesWithMissingReferences::add);

        return productTypesWithMissingReferences.size();
    }

    public Map<String, ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<UpdateAction<ProductType>, Boolean>>>
    getProductTypeKeysWithMissingParents() {
        return Collections.unmodifiableMap(missingProductTypeReferences);
    }

    /**
     * This method checks if there is an entry with the key of the {@code missingParentCategoryKey} in the
     * {@code categoryKeysWithMissingParents}, if there isn't it creates a new entry with this parent key and as a value
     * a new set containing the {@code childKey}. Otherwise, if there is already, it just adds the
     * {@code categoryKey} to the existing set.
     *
     * @param missingReferencedProductTypeKey the key of the missing parent.
     * @param productTypeDraftKey                 the key of the category with a missing parent.
     */
    public void putMissingReferencedProductTypeKey(@Nonnull final String missingReferencedProductTypeKey,
                                                   @Nonnull final String productTypeDraftKey,
                                                   @Nonnull final AttributeDefinitionDraft attributeDefinitionDraft) {


        final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<UpdateAction<ProductType>, Boolean>>
            productTypesWaitingForMissingReference = missingProductTypeReferences.get(missingReferencedProductTypeKey);

        if (productTypesWaitingForMissingReference != null) {
            final ConcurrentHashMap.KeySetView<UpdateAction<ProductType>, Boolean> actions =
                productTypesWaitingForMissingReference.get(productTypeDraftKey);

            if (actions != null) {
                actions.add(AddAttributeDefinition.of(attributeDefinitionDraft));
            } else {
                final ConcurrentHashMap.KeySetView<UpdateAction<ProductType>, Boolean> newActions =
                    ConcurrentHashMap.newKeySet();
                newActions.add(AddAttributeDefinition.of(attributeDefinitionDraft));
                productTypesWaitingForMissingReference.put(productTypeDraftKey, newActions);
            }

        } else {
            final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<UpdateAction<ProductType>, Boolean>>
                newProductTypesWaitingForMissingReference = new ConcurrentHashMap<>();

            final ConcurrentHashMap.KeySetView<UpdateAction<ProductType>, Boolean> newActions =
                ConcurrentHashMap.newKeySet();
            newActions.add(AddAttributeDefinition.of(attributeDefinitionDraft));
            newProductTypesWaitingForMissingReference.put(productTypeDraftKey, newActions);

            missingProductTypeReferences.put(
                missingReferencedProductTypeKey,
                newProductTypesWaitingForMissingReference);
        }
    }

    public void removeProductTypeWaitingToBeResolvedKey(@Nonnull final String key) {

        final Iterator<ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<UpdateAction<ProductType>, Boolean>>>
            productTypesIterator = missingProductTypeReferences.values().iterator();

        while (productTypesIterator.hasNext()) {
            final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<UpdateAction<ProductType>, Boolean>>
                productTypeActionsEntry = productTypesIterator.next();

            productTypeActionsEntry.remove(key);

            if (productTypeActionsEntry.isEmpty()) {
                productTypesIterator.remove();
            }
        }
    }
}
