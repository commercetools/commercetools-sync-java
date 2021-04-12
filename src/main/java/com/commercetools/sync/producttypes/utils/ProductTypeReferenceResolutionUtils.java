package com.commercetools.sync.producttypes.utils;

import static java.util.stream.Collectors.toList;

import com.commercetools.sync.commons.exceptions.ReferenceReplacementException;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeType;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class ProductTypeReferenceResolutionUtils {

  /**
   * Returns an {@link List}&lt;{@link ProductTypeDraft}&gt; consisting of the results of applying
   * the mapping from {@link ProductType} to {@link ProductTypeDraft} with considering reference
   * resolution.
   *
   * <table>
   *   <caption>Mapping of Reference fields for the reference resolution</caption>
   *   <thead>
   *     <tr>
   *       <th>Reference field</th>
   *       <th>from</th>
   *       <th>to</th>
   *     </tr>
   *   </thead>
   *   <tbody>
   *     <tr>
   *        <td>productType references (in case it has NestedType or set of NestedType)</td>
   *        <td>{@link Set}&lt;{@link Reference}&lt;{@link ProductType}&gt;&gt;</td>
   *        <td>{@link Set}&lt;{@link Reference}&lt;{@link ProductType}&gt;&gt; (with key replaced with id field)</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b>If some references are not expanded for an attributeDefinition of a productType,
   * the method will throw a {@link ReferenceReplacementException} containing the root causes of the
   * exceptions that occurred in any of the supplied {@code productTypes}.
   *
   * @param productTypes the product types with expanded references.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link List} of {@link ProductTypeDraft} built from the supplied {@link List} of
   *     {@link ProductType}.
   */
  @Nonnull
  public static List<ProductTypeDraft> mapToProductTypeDrafts(
      @Nonnull final List<ProductType> productTypes,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    return productTypes.stream()
        .filter(Objects::nonNull)
        .map(
            productType -> {
              final List<AttributeDefinitionDraft> referenceReplacedAttributeDefinitions =
                  replaceAttributeDefinitionsReferenceIdsWithKeys(
                      productType, referenceIdToKeyCache);

              return ProductTypeDraftBuilder.of(productType)
                  .attributes(referenceReplacedAttributeDefinitions)
                  .build();
            })
        .collect(toList());
  }

  @Nonnull
  private static List<AttributeDefinitionDraft> replaceAttributeDefinitionsReferenceIdsWithKeys(
      @Nonnull final ProductType productType,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    return productType.getAttributes().stream()
        .map(
            attributeDefinition -> {
              final AttributeType attributeType = attributeDefinition.getAttributeType();
              final AttributeType referenceReplacedType =
                  replaceIdWithKeyForProductTypeReference(attributeType, referenceIdToKeyCache);
              return AttributeDefinitionDraftBuilder.of(attributeDefinition)
                  .attributeType(referenceReplacedType)
                  .build();
            })
        .filter(Objects::nonNull)
        .collect(toList());
  }

  @Nonnull
  private static AttributeType replaceIdWithKeyForProductTypeReference(
      @Nonnull final AttributeType attributeType,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    if (attributeType instanceof NestedAttributeType) {

      final Reference<ProductType> referenceReplacedNestedType =
          replaceIdWithKeyForProductTypeReference(
              (NestedAttributeType) attributeType, referenceIdToKeyCache);
      return NestedAttributeType.of(referenceReplacedNestedType);

    } else if (attributeType instanceof SetAttributeType) {

      final SetAttributeType setAttributeType = (SetAttributeType) attributeType;
      final AttributeType elementType = setAttributeType.getElementType();
      final AttributeType referenceReplacedElementType =
          replaceIdWithKeyForProductTypeReference(elementType, referenceIdToKeyCache);
      return SetAttributeType.of(referenceReplacedElementType);
    }

    return attributeType;
  }

  @Nonnull
  private static Reference<ProductType> replaceIdWithKeyForProductTypeReference(
      @Nonnull final NestedAttributeType nestedAttributeType,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    final Reference<ProductType> productTypeReference = nestedAttributeType.getTypeReference();
    final String productTypeId = productTypeReference.getId();
    final String productTypeKey = referenceIdToKeyCache.get(productTypeId);
    if (null != productTypeKey) {
      return ProductType.referenceOfId(productTypeKey);
    }
    return productTypeReference;
  }

  private ProductTypeReferenceResolutionUtils() {}
}
