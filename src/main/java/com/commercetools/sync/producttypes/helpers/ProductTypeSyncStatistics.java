package com.commercetools.sync.producttypes.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.producttypes.ProductTypeSync;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

public class ProductTypeSyncStatistics extends BaseSyncStatistics {

    /**
     * The following {@link ConcurrentHashMap} ({@code missingNestedProductTypes}) keeps track of the keys of missing
     * product types, the keys of the product types which are referencing those missing product types and a list
     * of attribute definitions which contains those references.
     *
     * <ul>
     * <li>key: key of the missing product type</li>
     * <li>value: a map of which consists of:
     *      <ul>
     *          <li>key: key of the product type referencing the missing product type.</li>
     *          <li>value: a set of the attribute definition drafts which contains the reference
     *          to the missing product type.</li>
     *      </ul>
     * </li>
     * </ul>
     *
     * <p>The map is thread-safe (by instantiating it with {@link ConcurrentHashMap}) because it is accessed/modified in
     * a concurrent context, specifically when syncing product types in parallel in
     * {@link ProductTypeSync#removeMissingReferenceAttributeAndUpdateMissingParentMap(ProductTypeDraft, Map)},
     * {@link ProductTypeSync#updateProductType(ProductType, List)} and {@link ProductTypeSync#buildToBeUpdatedMap()}
     */
    private
    ConcurrentHashMap<String, // -> Key of missing nested productType
        ConcurrentHashMap<String, // -> Key of actual productType that is referencing the nested productType
            ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>// -> The actions to apply to on the
            // referencing product type to resolve it's referenced nested product types when they are available.
            >
        >
        missingNestedProductTypes = new ConcurrentHashMap<>();

    ProductTypeSyncStatistics (
        @Nonnull final ConcurrentHashMap<String,
            ConcurrentHashMap<String,
                ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>> missingNestedProductTypes) {
        this.missingNestedProductTypes = missingNestedProductTypes;
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

        missingNestedProductTypes.values()
                                 .stream()
                                 .map(ConcurrentHashMap::keySet)
                                 .flatMap(Collection::stream)
                                 .forEach(productTypesWithMissingReferences::add);

        return productTypesWithMissingReferences.size();
    }

    public Map<String, ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
    getProductTypeKeysWithMissingParents() {
        return Collections.unmodifiableMap(missingNestedProductTypes);
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


        final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
            productTypesWaitingForMissingReference = missingNestedProductTypes.get(missingReferencedProductTypeKey);

        if (productTypesWaitingForMissingReference != null) {
            final ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean> attributeDefinitionDrafts =
                productTypesWaitingForMissingReference.get(productTypeDraftKey);

            if (attributeDefinitionDrafts != null) {
                attributeDefinitionDrafts.add(attributeDefinitionDraft);
            } else {
                final ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean> newAttributeDefinitions =
                    ConcurrentHashMap.newKeySet();
                newAttributeDefinitions.add(attributeDefinitionDraft);
                productTypesWaitingForMissingReference.put(productTypeDraftKey, newAttributeDefinitions);
            }

        } else {
            final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
                newProductTypesWaitingForMissingReference = new ConcurrentHashMap<>();

            final ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean> newAttributeDefinitions =
                ConcurrentHashMap.newKeySet();
            newAttributeDefinitions.add(attributeDefinitionDraft);
            newProductTypesWaitingForMissingReference.put(productTypeDraftKey, newAttributeDefinitions);

            missingNestedProductTypes.put(
                missingReferencedProductTypeKey,
                newProductTypesWaitingForMissingReference);
        }
    }

    public void removeProductTypeWaitingToBeResolvedKey(@Nonnull final String key) {

        final Iterator<ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
            productTypesIterator = missingNestedProductTypes.values().iterator();

        while (productTypesIterator.hasNext()) {
            final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
                productTypeActionsEntry = productTypesIterator.next();

            productTypeActionsEntry.remove(key);

            if (productTypeActionsEntry.isEmpty()) {
                productTypesIterator.remove();
            }
        }
    }
}
