package com.commercetools.sync.cartdiscounts.utils;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;

import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.Type;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class CartDiscountReferenceResolutionUtils {

  /**
   * Returns an {@link List}&lt;{@link CartDiscountDraft}&gt; consisting of the results of applying
   * the mapping from {@link CartDiscount} to {@link CartDiscountDraft} with considering reference
   * resolution.
   *
   * <table>
   *   <caption>Mapping of Reference fields for the reference resolution</caption>
   *   <thead>
   *     <tr>
   *       <th>Reference field</th>
   *       <th>from</th>
   *       <th>to</th>
   *     </tr>
   *   </thead>
   *   <tbody>
   *     <tr>
   *        <td>custom.type</td>
   *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The {@link Type} reference should contain Id in the map(cache) with a key
   * value. Any reference, which have its id in place and not replaced by the key, it would not be
   * found in the map. In this case, this reference will be considered as existing resources on the
   * target commercetools project and the library will issues an update/create API request without
   * reference resolution.
   *
   * @param cartDiscounts the cart discounts without expansion of references.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link List} of {@link CartDiscountDraft} built from the supplied {@link List} of
   *     {@link CartDiscount}.
   */
  @Nonnull
  public static List<CartDiscountDraft> mapToCartDiscountDrafts(
      @Nonnull final List<CartDiscount> cartDiscounts,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return cartDiscounts.stream()
        .map(cartDiscount -> mapToCartDiscountDraft(cartDiscount, referenceIdToKeyCache))
        .collect(Collectors.toList());
  }

  @Nonnull
  private static CartDiscountDraft mapToCartDiscountDraft(
      @Nonnull final CartDiscount cartDiscount,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return CartDiscountDraftBuilder.of(
            cartDiscount.getCartPredicate(),
            cartDiscount.getName(),
            cartDiscount.isRequiringDiscountCode(),
            cartDiscount.getSortOrder(),
            cartDiscount.getTarget(),
            cartDiscount.getValue())
        .key(cartDiscount.getKey())
        .description(cartDiscount.getDescription())
        .active(cartDiscount.isActive())
        .validFrom(cartDiscount.getValidFrom())
        .validUntil(cartDiscount.getValidUntil())
        .stackingMode(cartDiscount.getStackingMode())
        .custom(mapToCustomFieldsDraft(cartDiscount, referenceIdToKeyCache))
        .build();
  }

  private CartDiscountReferenceResolutionUtils() {}
}
