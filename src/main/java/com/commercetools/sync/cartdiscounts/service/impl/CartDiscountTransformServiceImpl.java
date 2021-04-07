package com.commercetools.sync.cartdiscounts.service.impl;

import static com.commercetools.sync.cartdiscounts.utils.CartDiscountReferenceResolutionUtils.mapToCartDiscountDrafts;
import static java.util.stream.Collectors.toSet;

import com.commercetools.sync.cartdiscounts.service.CartDiscountTransformService;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.services.impl.BaseTransformServiceImpl;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class CartDiscountTransformServiceImpl extends BaseTransformServiceImpl
    implements CartDiscountTransformService {

  public CartDiscountTransformServiceImpl(
      @Nonnull final SphereClient ctpClient,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    super(ctpClient, referenceIdToKeyCache);
  }

  @Nonnull
  @Override
  public CompletableFuture<List<CartDiscountDraft>> toCartDiscountDrafts(
      @Nonnull final List<CartDiscount> cartDiscounts) {

    return transformCustomTypeReference(cartDiscounts)
        .thenApply(ignore -> mapToCartDiscountDrafts(cartDiscounts, referenceIdToKeyCache));
  }

  @Nonnull
  private CompletableFuture<Void> transformCustomTypeReference(
      @Nonnull final List<CartDiscount> cartDiscounts) {

    final Set<String> setOfTypeIds =
        cartDiscounts.stream()
            .map(CartDiscount::getCustom)
            .filter(Objects::nonNull)
            .map(CustomFields::getType)
            .map(Reference::getId)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(setOfTypeIds, GraphQlQueryResources.TYPES);
  }
}
