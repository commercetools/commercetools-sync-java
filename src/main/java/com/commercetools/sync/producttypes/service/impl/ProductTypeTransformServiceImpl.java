package com.commercetools.sync.producttypes.service.impl;

import static com.commercetools.sync.producttypes.utils.ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.sync.commons.exceptions.ReferenceReplacementException;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.producttypes.service.ProductTypeTransformService;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;

public class ProductTypeTransformServiceImpl extends BaseTransformServiceImpl
    implements ProductTypeTransformService {

  public ProductTypeTransformServiceImpl(
      @Nonnull final SphereClient ctpClient,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    super(ctpClient, referenceIdToKeyCache);
  }

  @Nonnull
  @Override
  public CompletableFuture<List<ProductTypeDraft>> toProductTypeDrafts(
      @Nonnull final List<ProductType> productTypes) throws ReferenceReplacementException {

    return loadNestedProductTypeReferenceKeys(productTypes)
        .thenApply(ignore -> mapToProductTypeDrafts(productTypes, referenceIdToKeyCache));
  }

  @Nonnull
  private CompletableFuture<Void> loadNestedProductTypeReferenceKeys(
      @Nonnull final List<ProductType> productTypes) {

    final Set<String> setOfTypeIds = new HashSet<>(collectNestedReferenceIds(productTypes));

    return super.fetchAndFillReferenceIdToKeyCache(
        setOfTypeIds, GraphQlQueryResources.PRODUCT_TYPES);
  }

  private Set<String> collectNestedReferenceIds(@Nonnull List<ProductType> productTypes) {

    return productTypes.stream()
        .map(
            productType ->
                productType.getAttributes().stream()
                    .filter(Objects::nonNull)
                    .map(AttributeDefinition::getAttributeType)
                    .map(this::getNestedAttributeId)
                    .filter(StringUtils::isNotBlank)
                    .collect(toList()))
        .flatMap(Collection::stream)
        .collect(toSet());
  }

  private String getNestedAttributeId(AttributeType attributeType) {
    if (attributeType instanceof NestedAttributeType) {
      final Reference<ProductType> NestedTypeReference =
          ((NestedAttributeType) attributeType).getTypeReference();
      if (NestedTypeReference != null) {
        return NestedTypeReference.getId();
      }
    } else if (attributeType instanceof SetAttributeType) {
      final AttributeType elementType = ((SetAttributeType) attributeType).getElementType();
      if (elementType != null) {
        return getNestedAttributeId(elementType);
      }
    }
    return null;
  }
}
