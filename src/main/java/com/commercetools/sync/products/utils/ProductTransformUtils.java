package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.products.service.ProductTransformService;
import com.commercetools.sync.products.service.impl.ProductTransformServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public final class ProductTransformUtils {

  /**
   * Transforms products by resolving the references and map them to ProductDrafts.
   *
   * <p>This method replaces the ids on attribute references with keys and resolves(fetch key values
   * for the reference id's) non null and unexpanded references of the product{@link Product} by
   * using cache.
   *
   * <p>If the reference ids are already cached, key values are pulled from the cache, otherwise it
   * executes the query to fetch the key value for the reference id's and store the idToKey value
   * pair in the cache for reuse.
   *
   * <p>Then maps the Product to ProductDraft by performing reference resolution considering idToKey
   * value from the cache.
   *
   * @param client commercetools client.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @param products the products to replace the references and attributes id's with keys.
   * @return a new list which contains productDrafts which have all their references and attributes
   *     references resolved and already replaced with keys.
   *     <p>TODO: Move the implementation from service class to this util class.
   */
  @Nonnull
  public static CompletableFuture<List<ProductDraft>> toProductDrafts(
      @Nonnull final SphereClient client,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache,
      @Nonnull final List<ProductProjection> products) {

    final ProductTransformService productTransformService =
        new ProductTransformServiceImpl(client, referenceIdToKeyCache);
    return productTransformService.toProductDrafts(products);
  }
}
