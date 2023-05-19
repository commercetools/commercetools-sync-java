package com.commercetools.sync.sdk2.producttypes.utils;

import static com.commercetools.sync.sdk2.producttypes.utils.ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.AttributeNestedType;
import com.commercetools.api.models.product_type.AttributeSetType;
import com.commercetools.api.models.product_type.AttributeType;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.sync.sdk2.commons.exceptions.ReferenceReplacementException;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.services.impl.BaseTransformServiceImpl;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;

public final class ProductTypeTransformUtils {

  /**
   * Transforms productTypes by resolving the references and map them to ProductTypeDrafts.
   *
   * <p>This method replaces the ids on attribute references with keys and resolves(fetch key values
   * for the reference id's) non null and unexpanded references of the productType{@link
   * ProductType} by using cache.
   *
   * <p>If the reference ids are already cached, key values are pulled from the cache, otherwise it
   * executes the query to fetch the key value for the reference id's and store the idToKey value
   * pair in the cache for reuse.
   *
   * <p>Then maps the ProductType to ProductTypeDraft by performing reference resolution considering
   * idToKey value from the cache.
   *
   * @param client commercetools client.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @param productTypes the productTypes to replace the references and attributes id's with keys.
   * @return a new list which contains productTypeDrafts which have all their references and
   *     attributes references resolved and already replaced with keys.
   */
  @Nonnull
  public static CompletableFuture<List<ProductTypeDraft>> toProductTypeDrafts(
      @Nonnull final ProjectApiRoot client,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache,
      @Nonnull final List<ProductType> productTypes) {

    final ProductTypeTransformServiceImpl productTypeTransformService =
        new ProductTypeTransformServiceImpl(client, referenceIdToKeyCache);
    return productTypeTransformService.toProductTypeDrafts(productTypes);
  }

  private static class ProductTypeTransformServiceImpl extends BaseTransformServiceImpl {

    public ProductTypeTransformServiceImpl(
        @Nonnull final ProjectApiRoot ctpClient,
        @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
      super(ctpClient, referenceIdToKeyCache);
    }

    @Nonnull
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
          setOfTypeIds, GraphQlQueryResource.PRODUCT_TYPES);
    }

    private Set<String> collectNestedReferenceIds(@Nonnull List<ProductType> productTypes) {

      return productTypes.stream()
          .map(
              productType ->
                  productType.getAttributes().stream()
                      .filter(Objects::nonNull)
                      .map(AttributeDefinition::getType)
                      .map(this::getNestedAttributeId)
                      .filter(StringUtils::isNotBlank)
                      .collect(toList()))
          .flatMap(Collection::stream)
          .collect(toSet());
    }

    private String getNestedAttributeId(AttributeType attributeType) {
      if (attributeType instanceof AttributeNestedType) {
        final ProductTypeReference nestedTypeReference =
            ((AttributeNestedType) attributeType).getTypeReference();
        if (nestedTypeReference != null) {
          return nestedTypeReference.getId();
        }
      } else if (attributeType instanceof AttributeSetType) {
        final AttributeType elementType = ((AttributeSetType) attributeType).getElementType();
        if (elementType != null) {
          return getNestedAttributeId(elementType);
        }
      }
      return null;
    }
  }
}
