package com.commercetools.sync.producttypes.service;

import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public interface ProductTypeTransformService {

  /**
   * Transforms productTypes by resolving the references and map them to ProductTypeDrafts.
   *
   * <p>This method replaces the ids on attribute references with keys. It resolves(fetch key value
   * for the reference id) the non-null unexpanded references in the productType{@link ProductType}
   * by using a cache.
   *
   * <p>If the reference ids are already cached, key values are pulled from the cache, otherwise it
   * executes the query to fetch the key value for the reference id and store in the cache.
   *
   * <p>Then maps the ProductType to ProductTypeDraft by performing reference resolution considering
   * idToKey value from the cache.
   *
   * @param productTypes the productTypes to replace the references and attributes ids with keys.
   * @return a new list which contains productTypeDrafts which have all their references and
   *     attributes references resolved and already replaced with keys.
   */
  @Nonnull
  CompletableFuture<List<ProductTypeDraft>> toProductTypeDrafts(
      @Nonnull List<ProductType> productTypes);
}
