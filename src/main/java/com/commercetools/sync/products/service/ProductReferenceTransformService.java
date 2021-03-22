package com.commercetools.sync.products.service;

import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public interface ProductReferenceTransformService {

  /**
   * Transforms products by resolving the references and map them to ProductDrafts.
   *
   * <p>This method replaces the ids on attribute references with keys and resolves(fetch key values
   * for the reference id's) non null and unexpanded references of the product{@link Product} by
   * using cache.
   *
   * <p>If the reference id's are already cached, key values are fetched from the cache else it
   * executes the query to fetch the key value for the reference id's and store the idToKey value
   * pair in the cache for reuse.
   *
   * <p>Then maps the Product to ProductDraft by performing reference resolution considering idToKey
   * value from the cache.
   *
   * @param products the products to replace the references and attributes id's with keys.
   * @return a new list which contains productDrafts which have all their references and attributes
   *     references resolved and already replaced with keys.
   */
  @Nonnull
  CompletableFuture<List<ProductDraft>> transformProductReferences(@Nonnull List<Product> products);
}
