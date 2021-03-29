package com.commercetools.sync.producttypes.service.impl;

import static com.commercetools.sync.producttypes.utils.ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.sync.commons.exceptions.ReferenceReplacementException;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.producttypes.service.ProductTypeReferenceTransformService;
import com.commercetools.sync.services.impl.BaseTransformServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeType;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;

public class ProductTypeReferenceTransformServiceImpl extends BaseTransformServiceImpl
    implements ProductTypeReferenceTransformService {

  public ProductTypeReferenceTransformServiceImpl(
      @Nonnull final SphereClient ctpClient,
      @Nonnull final Map<String, String> referenceIdToKeyCache) {
    super(ctpClient, referenceIdToKeyCache);
  }

  @Nonnull
  @Override
  public CompletableFuture<List<ProductTypeDraft>> transformProductTypeReferences(
      @Nonnull final List<ProductType> productTypes) throws ReferenceReplacementException {

    return transformNestedProductTypeReference(productTypes)
        .thenApply(ignore -> mapToProductTypeDrafts(productTypes, referenceIdToKeyCache));
  }

  @Nonnull
  private CompletableFuture<Void> transformNestedProductTypeReference(
      @Nonnull final List<ProductType> productTypes) {

    final Set<String> setOfTypeIds = new HashSet<>();
    setOfTypeIds.addAll(collectNestedProductTypeReferenceIds(productTypes));

    return fetchAndFillReferenceIdToKeyCache(setOfTypeIds, GraphQlQueryResources.PRODUCT_TYPES);
  }

  private Set<String> collectNestedProductTypeReferenceIds(
      @Nonnull List<ProductType> productTypes) {

    return productTypes.stream()
        .map(
            productType ->
                productType.getAttributes().stream()
                    .filter(Objects::nonNull)
                    .map(AttributeDefinition::getAttributeType)
                    .map(attributeType -> getNestedAttributeIds(attributeType))
                    .filter(id -> StringUtils.isNotBlank(id))
                    .collect(toList()))
        .flatMap(Collection::stream)
        .collect(toSet());
  }

  private String getNestedAttributeIds(AttributeType attributeType) {
    if (attributeType instanceof NestedAttributeType) {
      final Reference<ProductType> referenceReplacedNestedType =
          ((NestedAttributeType) attributeType).getTypeReference();
      if (referenceReplacedNestedType != null) {
        return referenceReplacedNestedType.getId();
      }
    } else if (attributeType instanceof SetAttributeType) {
      final AttributeType elementType = ((SetAttributeType) attributeType).getElementType();
      if (elementType != null) {
        getNestedAttributeIds(elementType);
      }
    }
    return null;
  }
}
