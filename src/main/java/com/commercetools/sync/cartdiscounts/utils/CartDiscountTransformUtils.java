package com.commercetools.sync.cartdiscounts.utils;

import static java.util.stream.Collectors.toSet;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.sync.commons.models.GraphQlQueryResource;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.services.impl.BaseTransformServiceImpl;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public final class CartDiscountTransformUtils {

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
   * @param client commercetools client.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @param cartDiscounts the cartDiscounts to resolve the references.
   * @return a new list which contains cartDiscountDrafts which have all their references resolved.
   */
  @Nonnull
  public static CompletableFuture<List<CartDiscountDraft>> toCartDiscountDrafts(
      @Nonnull final ProjectApiRoot client,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache,
      @Nonnull final List<CartDiscount> cartDiscounts) {

    final CartDiscountTransformServiceImpl cartDiscountTransformService =
        new CartDiscountTransformServiceImpl(client, referenceIdToKeyCache);
    return cartDiscountTransformService.toCartDiscountDrafts(cartDiscounts);
  }

  private static class CartDiscountTransformServiceImpl extends BaseTransformServiceImpl {

    public CartDiscountTransformServiceImpl(
        @Nonnull final ProjectApiRoot ctpClient,
        @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
      super(ctpClient, referenceIdToKeyCache);
    }

    @Nonnull
    public CompletableFuture<List<CartDiscountDraft>> toCartDiscountDrafts(
        @Nonnull final List<CartDiscount> cartDiscounts) {

      return transformCustomTypeReference(cartDiscounts)
          .thenApply(
              ignore ->
                  CartDiscountReferenceResolutionUtils.mapToCartDiscountDrafts(
                      cartDiscounts, referenceIdToKeyCache));
    }

    @Nonnull
    private CompletableFuture<Void> transformCustomTypeReference(
        @Nonnull final List<CartDiscount> cartDiscounts) {

      final Set<String> setOfTypeIds =
          cartDiscounts.stream()
              .map(CartDiscount::getCustom)
              .filter(Objects::nonNull)
              .map(customFields -> customFields.getType().getId())
              .collect(toSet());

      return fetchAndFillReferenceIdToKeyCache(setOfTypeIds, GraphQlQueryResource.TYPES);
    }
  }
}
