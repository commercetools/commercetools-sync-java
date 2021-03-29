package com.commercetools.sync.producttypes.service;

import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public interface ProductTypeReferenceTransformService {

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
   * @param productTypes the productTypes to replace the references and attributes id's with keys.
   * @return a new list which contains productTypeDrafts which have all their references and
   *     attributes references resolved and already replaced with keys.
   */
  @Nonnull
  CompletableFuture<List<ProductTypeDraft>> transformProductTypeReferences(
      @Nonnull List<ProductType> productTypes);
}
