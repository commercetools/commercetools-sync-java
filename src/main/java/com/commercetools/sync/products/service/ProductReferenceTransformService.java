package com.commercetools.sync.products.service;

import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public interface ProductReferenceTransformService {

  /**
   * Transforms the products by resolving the references and map them to ProductDrafts. This method
   * checks if the reference id's are already cached, else it executes the query to fetch the key
   * value for the reference Id's and store the idToKey value pair in the cache.
   * Also maps the Product to ProductDraft by performing reference resolution considering idToKey 
   * value from the cache.
   *
   * @param products the products to replace the references and attributes id's with keys.
   * @return a new list which contains productDrafts which have all their references and attributes
   *     references resolved and already replaced with keys.
   */
  @Nonnull
  CompletableFuture<List<ProductDraft>> transformProductReferences(@Nonnull List<Product> products);
}
