package com.commercetools.sync.products.service;

import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public interface ProductReferenceTransformService {

  /**
   * Replaces the ids on attribute references with keys. If a product has at least one irresolvable
   * reference, it will be filtered out and not returned in the new list.
   *
   * <p>Note: this method mutates the products passed by changing the reference keys with ids.
   *
   * @param products the products to replace the reference attributes ids with keys on.
   * @return a new list which contains only products which have all their attributes references
   *     resolvable and already replaced with keys.
   */
  @Nonnull
  CompletionStage<List<Product>> replaceAttributeReferenceIdsWithKeys(
      @Nonnull final List<Product> products);

  /**
   * Transforms the products by resolving the references using cache. This method will collect all
   * the unresolved references and executes the query to fetch the key value for the references Id's
   * and store the idToKey value pair in the cache. Also maps the Product to ProductDraft by
   * performing reference resolution considering idToKey value from the cache.
   *
   * @param products the products to replace the reference attributes ids with keys on.
   * @return a new list which contains only products which have all their attributes references
   *     resolvable and already replaced with keys.
   */
  @Nonnull
  CompletableFuture<List<ProductDraft>> transformProductReferencesAndMapToProductDrafts(
      @Nonnull List<Product> products);
}
