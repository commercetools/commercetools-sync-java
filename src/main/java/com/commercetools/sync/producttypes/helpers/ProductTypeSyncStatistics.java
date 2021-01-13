package com.commercetools.sync.producttypes.helpers;

import static java.lang.String.format;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.producttypes.ProductTypeSync;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

public class ProductTypeSyncStatistics extends BaseSyncStatistics {

  /**
   * The following {@link ConcurrentHashMap} ({@code missingNestedProductTypes}) keeps track of the
   * keys of missing product types, the keys of the product types which are referencing those
   * missing product types and a list of attribute definitions which contains those references.
   *
   * <ul>
   *   <li>key: key of the missing product type.
   *   <li>value: a map of which consists of:
   *       <ul>
   *         <li>key: key of the product type referencing the missing product type.
   *         <li>value: a set of the attribute definition drafts which contains the reference to the
   *             missing product type.
   *       </ul>
   * </ul>
   *
   * <p>The map is thread-safe (by instantiating it with {@link ConcurrentHashMap}) because it is
   * accessed/modified in a concurrent context, specifically when syncing product types in parallel
   * in {@link
   * ProductTypeSync#removeMissingReferenceAttributeAndUpdateMissingParentMap(ProductTypeDraft,
   * Map)}, {@link ProductTypeSync#updateProductType(ProductType, List)} and {@link
   * ProductTypeSync#buildToBeUpdatedMap()}
   */
  private ConcurrentHashMap<
          String,
          ConcurrentHashMap<
              String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
      missingNestedProductTypes = new ConcurrentHashMap<>();

  /** Only used for testing. */
  ProductTypeSyncStatistics(
      @Nonnull
          final ConcurrentHashMap<
                  String,
                  ConcurrentHashMap<
                      String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
              missingNestedProductTypes) {
    this.missingNestedProductTypes = missingNestedProductTypes;
  }

  public ProductTypeSyncStatistics() {}

  /**
   * Builds a summary of the product type sync statistics instance that looks like the following
   * example:
   *
   * <p>"Summary: 2 product types were processed in total (0 created, 0 updated, 0 failed to sync
   * and 0 product types with at least one NestedType or a Set of NestedType attribute definition(s)
   * referencing a missing productType)."
   *
   * @return a summary message of the product types sync statistics instance.
   */
  @Override
  public String getReportMessage() {
    return format(
        "Summary: %s product types were processed in total (%s created, %s updated, %s failed to sync"
            + " and %s product types with at least one NestedType or a Set of NestedType attribute definition(s)"
            + " referencing a missing product type).",
        getProcessed(),
        getCreated(),
        getUpdated(),
        getFailed(),
        getNumberOfProductTypesWithMissingNestedProductTypes());
  }

  /**
   * Returns the total number of product types with at least one NestedType or a Set of NestedType
   * attribute definition(s) referencing a missing productType.
   *
   * @return the total number of product types with at least one NestedType or a Set of NestedType
   *     attribute definition(s) referencing a missing productType.
   */
  public int getNumberOfProductTypesWithMissingNestedProductTypes() {
    final Set<String> productTypesWithMissingReferences = new HashSet<>();

    missingNestedProductTypes.values().stream()
        .map(ConcurrentHashMap::keySet)
        .flatMap(Collection::stream)
        .forEach(productTypesWithMissingReferences::add);

    return productTypesWithMissingReferences.size();
  }

  /**
   * @return an unmodifiable {@link ConcurrentHashMap} ({@code missingNestedProductTypes}) which
   *     keeps track of the keys of missing product types, the keys of the product types which are
   *     referencing those missing product types and a list of attribute definitions which contains
   *     those references.
   *     <ul>
   *       <li>key: key of the missing product type
   *       <li>value: a map of which consists of:
   *           <ul>
   *             <li>key: key of the product type referencing the missing product type.
   *             <li>value: a set of the attribute definition drafts which contains the reference to
   *                 the missing product type.
   *           </ul>
   *     </ul>
   */
  public Map<
          String,
          ConcurrentHashMap<
              String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
      getProductTypeKeysWithMissingParents() {
    return Collections.unmodifiableMap(missingNestedProductTypes);
  }

  /**
   * Adds a new entry for a {@code referencingAttributeDefinitionDraft} that is pointed to by a
   * {@code referencingProductTypeKey} which is pointed to by a {@code missingNestedProductTypeKey}.
   *
   * <p>If any of the inner sets/maps is not existing (null), this method will create a new set/map
   * with only this new entry.
   *
   * <p>Important: This method is meant to be used only for internal use of the library and should
   * not be used by externally.
   *
   * @param missingNestedProductTypeKey the key of the missing nested product type.
   * @param referencingProductTypeKey the key of the referencing product type.
   * @param referencingAttributeDefinitionDraft the referencing attribute definition draft.
   */
  public void putMissingNestedProductType(
      @Nonnull final String missingNestedProductTypeKey,
      @Nonnull final String referencingProductTypeKey,
      @Nonnull final AttributeDefinitionDraft referencingAttributeDefinitionDraft) {

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        existingReferencingProductTypes =
            missingNestedProductTypes.get(missingNestedProductTypeKey);

    if (existingReferencingProductTypes != null) {
      final ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>
          existingAttributeDefinitionDrafts =
              existingReferencingProductTypes.get(referencingProductTypeKey);

      if (existingAttributeDefinitionDrafts != null) {
        existingAttributeDefinitionDrafts.add(referencingAttributeDefinitionDraft);
      } else {
        existingReferencingProductTypes.put(
            referencingProductTypeKey, asSet(referencingAttributeDefinitionDraft));
      }
    } else {
      missingNestedProductTypes.put(
          missingNestedProductTypeKey,
          asMap(referencingProductTypeKey, referencingAttributeDefinitionDraft));
    }
  }

  @Nonnull
  private ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
      asMap(
          @Nonnull final String referencingProductTypeKey,
          @Nonnull final AttributeDefinitionDraft referencingAttributeDefinitionDraft) {

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        newReferencingProductTypes = new ConcurrentHashMap<>();
    newReferencingProductTypes.put(
        referencingProductTypeKey, asSet(referencingAttributeDefinitionDraft));
    return newReferencingProductTypes;
  }

  @Nonnull
  private ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean> asSet(
      @Nonnull final AttributeDefinitionDraft referencingAttributeDefinitionDraft) {

    final ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean> newAttributeDefinitions =
        ConcurrentHashMap.newKeySet();
    newAttributeDefinitions.add(referencingAttributeDefinitionDraft);
    return newAttributeDefinitions;
  }

  /**
   * Removes all occurrences of the referencing product type key from {@link
   * #missingNestedProductTypes}. If there are no referencing product types for any missing nested
   * product type, the whole entry for this missing nested product type will be removed from {@link
   * #missingNestedProductTypes}.
   *
   * <p>Important: This method is meant to be used only for internal use of the library and should
   * not be used by externally.
   *
   * @param referencingProductTypeKey the key that should be removed from {@link
   *     #missingNestedProductTypes}.
   */
  public void removeReferencingProductTypeKey(@Nonnull final String referencingProductTypeKey) {

    final Iterator<
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        referencingProductTypesIterator = missingNestedProductTypes.values().iterator();

    while (referencingProductTypesIterator.hasNext()) {
      final ConcurrentHashMap<
              String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
          referencingProductTypes = referencingProductTypesIterator.next();

      referencingProductTypes.remove(referencingProductTypeKey);

      // If there are no referencing product types for this missing nested product type,
      // remove referencing product types map.
      if (referencingProductTypes.isEmpty()) {
        referencingProductTypesIterator.remove();
      }
    }
  }
}
