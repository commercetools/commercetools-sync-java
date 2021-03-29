package com.commercetools.sync.producttypes.utils;

import static java.util.stream.Collectors.toList;

import com.commercetools.sync.commons.exceptions.ReferenceReplacementException;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeType;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import java.util.List;
import java.util.Map;
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
   * @param referenceIdToKeyMap the map containing the cached id to key values.
   * @return a {@link List} of {@link ProductTypeDraft} built from the supplied {@link List} of
   *     {@link ProductType}.
   */
  @Nonnull
  public static List<ProductTypeDraft> mapToProductTypeDrafts(
      @Nonnull final List<ProductType> productTypes,
      @Nonnull final Map<String, String> referenceIdToKeyMap) {

    final List<ProductTypeDraft> referenceReplacedDrafts =
        productTypes.stream()
            .filter(Objects::nonNull)
            .map(
                productType -> {
                  final List<AttributeDefinitionDraft> referenceReplacedAttributeDefinitions;
                  referenceReplacedAttributeDefinitions =
                      replaceAttributeDefinitionsReferenceIdsWithKeys(
                          productType, referenceIdToKeyMap);

                  return ProductTypeDraftBuilder.of(productType)
                      .attributes(referenceReplacedAttributeDefinitions)
                      .build();
                })
            .filter(Objects::nonNull)
            .collect(toList());

    return referenceReplacedDrafts;
  }

  @Nonnull
  private static List<AttributeDefinitionDraft> replaceAttributeDefinitionsReferenceIdsWithKeys(
      @Nonnull final ProductType productType,
      @Nonnull final Map<String, String> referenceIdToKeyMap) {

    final List<AttributeDefinitionDraft> referenceReplacedAttributeDefinitions =
        productType.getAttributes().stream()
            .map(
                attributeDefinition -> {
                  final AttributeType attributeType = attributeDefinition.getAttributeType();
                  final AttributeType referenceReplacedType =
                      replaceProductTypeReferenceIdWithKey(attributeType, referenceIdToKeyMap);
                  return AttributeDefinitionDraftBuilder.of(attributeDefinition)
                      .attributeType(referenceReplacedType)
                      .build();
                })
            .filter(Objects::nonNull)
            .collect(toList());

    return referenceReplacedAttributeDefinitions;
  }

  @Nonnull
  private static AttributeType replaceProductTypeReferenceIdWithKey(
      @Nonnull final AttributeType attributeType,
      @Nonnull final Map<String, String> referenceIdToKeyMap) {

    if (attributeType instanceof NestedAttributeType) {

      final Reference<ProductType> referenceReplacedNestedType =
          replaceProductTypeReferenceIdWithKey(
              (NestedAttributeType) attributeType, referenceIdToKeyMap);
      return NestedAttributeType.of(referenceReplacedNestedType);

    } else if (attributeType instanceof SetAttributeType) {

      final SetAttributeType setAttributeType = (SetAttributeType) attributeType;
      final AttributeType elementType = setAttributeType.getElementType();
      final AttributeType referenceReplacedElementType =
          replaceProductTypeReferenceIdWithKey(elementType, referenceIdToKeyMap);
      return SetAttributeType.of(referenceReplacedElementType);
    }

    return attributeType;
  }

  @Nonnull
  private static Reference<ProductType> replaceProductTypeReferenceIdWithKey(
      @Nonnull final NestedAttributeType nestedAttributeType,
      @Nonnull final Map<String, String> referenceIdToKeyMap) {

    final Reference<ProductType> productTypeReference = nestedAttributeType.getTypeReference();
    final String productTypeId = productTypeReference.getId();
    final String productTypeKey = referenceIdToKeyMap.get(productTypeId);
    return ProductType.referenceOfId(productTypeKey);
  }

  /**
   * Builds a {@link ProductTypeQuery} for fetching products from a source CTP project without any
   * expansion to the {@link ProductType}
   *
   * @return the query for fetching products from the source CTP project without any expansion to
   *     the {@link ProductType}.
   */
  @Nonnull
  public static ProductTypeQuery buildProductTypeQuery() {
    return ProductTypeQuery.of();
  }

  private ProductTypeReferenceResolutionUtils() {}
}
