package com.commercetools.sync.sdk2.producttypes.utils;

import static com.commercetools.sync.sdk2.producttypes.helpers.ResourceToDraftConverters.toAttributeDefinitionDraftBuilder;
import static com.commercetools.sync.sdk2.producttypes.helpers.ResourceToDraftConverters.toProductTypeDraftBuilder;
import static java.util.stream.Collectors.toList;

import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeNestedType;
import com.commercetools.api.models.product_type.AttributeNestedTypeBuilder;
import com.commercetools.api.models.product_type.AttributeSetType;
import com.commercetools.api.models.product_type.AttributeSetTypeBuilder;
import com.commercetools.api.models.product_type.AttributeType;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.product_type.ProductTypeReferenceBuilder;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class ProductTypeReferenceResolutionUtils {

  /**
   * Returns an {@link java.util.List}&lt;{@link ProductTypeDraft}&gt; consisting of the results of
   * applying the mapping from {@link ProductType} to {@link ProductTypeDraft} with considering
   * reference resolution.
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
   *        <td>{@link java.util.Set}&lt;{@link com.commercetools.api.models.product_type.ProductTypeReference}&gt;</td>
   *        <td>{@link java.util.Set}&lt;{@link com.commercetools.api.models.product_type.ProductTypeReference}&gt; (with key replaced with id field)</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b>If some references are not expanded for an attributeDefinition of a productType,
   * the method will throw a {@link
   * com.commercetools.sync.sdk2.commons.exceptions.ReferenceReplacementException} containing the
   * root causes of the exceptions that occurred in any of the supplied {@code productTypes}.
   *
   * @param productTypes the product types with expanded references.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link java.util.List} of {@link ProductTypeDraft} built from the supplied {@link
   *     java.util.List} of {@link ProductType}.
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

              return toProductTypeDraftBuilder(productType)
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
              final AttributeType attributeType = attributeDefinition.getType();
              final AttributeType referenceReplacedType =
                  replaceIdWithKeyForProductTypeReference(attributeType, referenceIdToKeyCache);
              return toAttributeDefinitionDraftBuilder(attributeDefinition)
                  .type(referenceReplacedType)
                  .build();
            })
        .filter(Objects::nonNull)
        .collect(toList());
  }

  @Nonnull
  private static AttributeType replaceIdWithKeyForProductTypeReference(
      @Nonnull final AttributeType attributeType,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    if (attributeType instanceof AttributeNestedType) {

      final ProductTypeReference referenceReplacedNestedType =
          replaceIdWithKeyForProductTypeReference(
              (AttributeNestedType) attributeType, referenceIdToKeyCache);
      return AttributeNestedTypeBuilder.of().typeReference(referenceReplacedNestedType).build();

    } else if (attributeType instanceof AttributeSetType) {

      final AttributeSetType setAttributeType = (AttributeSetType) attributeType;
      final AttributeType elementType = setAttributeType.getElementType();
      final AttributeType referenceReplacedElementType =
          replaceIdWithKeyForProductTypeReference(elementType, referenceIdToKeyCache);
      return AttributeSetTypeBuilder.of().elementType(referenceReplacedElementType).build();
    }

    return attributeType;
  }

  @Nonnull
  private static ProductTypeReference replaceIdWithKeyForProductTypeReference(
      @Nonnull final AttributeNestedType nestedAttributeType,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    final ProductTypeReference productTypeReference = nestedAttributeType.getTypeReference();
    final String productTypeId = productTypeReference.getId();
    final String productTypeKey = referenceIdToKeyCache.get(productTypeId);
    if (null != productTypeKey) {
      return ProductTypeReferenceBuilder.of().id(productTypeKey).build();
    }
    return productTypeReference;
  }

  private ProductTypeReferenceResolutionUtils() {}
}
