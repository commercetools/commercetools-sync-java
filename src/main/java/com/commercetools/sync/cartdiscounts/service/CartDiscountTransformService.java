package com.commercetools.sync.cartdiscounts.service;

import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public interface CartDiscountTransformService {

  /**
   * Transforms cartDiscounts by resolving the references and map them to CartDiscountDrafts.
   *
   * <p>This method resolves(fetch key values for the reference id's) non null and unexpanded
   * references of the cartDiscount{@link CartDiscount} by using cache.
   *
   * <p>If the reference ids are already cached, key values are pulled from the cache, otherwise it
   * executes the query to fetch the key value for the reference id's and store the idToKey value
   * pair in the cache for reuse.
   *
   * <p>Then maps the CartDiscount to CartDiscountDraft by performing reference resolution
   * considering idToKey value from the cache.
   *
   * @param cartDiscounts the cartDiscounts to resolve the references.
   * @return a new list which contains cartDiscountDrafts which have all their references resolved.
   */
  @Nonnull
  CompletableFuture<List<CartDiscountDraft>> toCartDiscountDrafts(
      @Nonnull List<CartDiscount> cartDiscounts);
}
