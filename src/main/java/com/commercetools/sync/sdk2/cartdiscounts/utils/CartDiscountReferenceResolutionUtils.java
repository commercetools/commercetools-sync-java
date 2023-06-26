package com.commercetools.sync.sdk2.cartdiscounts.utils;

import static com.commercetools.sync.sdk2.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;

import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountDraftBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountValue;
import com.commercetools.api.models.cart_discount.CartDiscountValueAbsolute;
import com.commercetools.api.models.cart_discount.CartDiscountValueAbsoluteDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueAbsoluteDraftBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountValueDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueFixed;
import com.commercetools.api.models.cart_discount.CartDiscountValueFixedDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueFixedDraftBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountValueGiftLineItem;
import com.commercetools.api.models.cart_discount.CartDiscountValueGiftLineItemDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueGiftLineItemDraftBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountValueRelative;
import com.commercetools.api.models.cart_discount.CartDiscountValueRelativeDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueRelativeDraftBuilder;
import com.commercetools.api.models.common.MoneyBuilder;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class CartDiscountReferenceResolutionUtils {

  /**
   * Returns an {@link java.util.List}&lt;{@link CartDiscountDraft}&gt; consisting of the results of
   * applying the mapping from {@link CartDiscount} to {@link CartDiscountDraft} with considering
   * reference resolution.
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
   *        <td>{@link com.commercetools.api.models.type.TypeReference}</td>
   *        <td>{@link com.commercetools.api.models.type.TypeResourceIdentifier}</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The {@link com.commercetools.api.models.type.Type} reference should contain Id
   * in the map(cache) with a key value. Any reference, which have its id in place and not replaced
   * by the key, it would not be found in the map. In this case, this reference will be considered
   * as existing resources on the target commercetools project and the library will issue an
   * update/create API request without reference resolution.
   *
   * @param cartDiscounts the cart discounts without expansion of references.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link java.util.List} of {@link CartDiscountDraft} built from the supplied {@link
   *     java.util.List} of {@link CartDiscount}.
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
    return CartDiscountDraftBuilder.of()
        .cartPredicate(cartDiscount.getCartPredicate())
        .name(cartDiscount.getName())
        .requiresDiscountCode(cartDiscount.getRequiresDiscountCode())
        .sortOrder(cartDiscount.getSortOrder())
        .target(cartDiscount.getTarget())
        .value(toCartDiscountValueDraft(cartDiscount.getValue()))
        .key(cartDiscount.getKey())
        .description(cartDiscount.getDescription())
        .isActive(cartDiscount.getIsActive())
        .validFrom(cartDiscount.getValidFrom())
        .validUntil(cartDiscount.getValidUntil())
        .stackingMode(cartDiscount.getStackingMode())
        .custom(mapToCustomFieldsDraft(cartDiscount, referenceIdToKeyCache))
        .build();
  }

  private static CartDiscountValueDraft toCartDiscountValueDraft(
      final CartDiscountValue cartDiscountValue) {
    if (cartDiscountValue instanceof CartDiscountValueAbsolute) {
      return cloneCartDiscountValueAbsoluteDraft((CartDiscountValueAbsolute) cartDiscountValue);
    }
    if (cartDiscountValue instanceof CartDiscountValueFixed) {
      return cloneCartDiscountValueFixedDraft((CartDiscountValueFixed) cartDiscountValue);
    }
    if (cartDiscountValue instanceof CartDiscountValueGiftLineItem) {
      return cloneCartDiscountValueGiftLineItemDraft(
          (CartDiscountValueGiftLineItem) cartDiscountValue);
    }
    if (cartDiscountValue instanceof CartDiscountValueRelative) {
      return cloneCartDiscountValueRelativeDraft((CartDiscountValueRelative) cartDiscountValue);
    }
    return null;
  }

  private static CartDiscountValueRelativeDraft cloneCartDiscountValueRelativeDraft(
      final CartDiscountValueRelative cartDiscountValue) {
    final CartDiscountValueRelative cartDiscountValueRelative = cartDiscountValue;
    return CartDiscountValueRelativeDraftBuilder.of()
        .permyriad(cartDiscountValueRelative.getPermyriad())
        .build();
  }

  private static CartDiscountValueGiftLineItemDraft cloneCartDiscountValueGiftLineItemDraft(
      final CartDiscountValueGiftLineItem cartDiscountValue) {
    final CartDiscountValueGiftLineItem cartDiscountValueGiftLineItem = cartDiscountValue;
    return CartDiscountValueGiftLineItemDraftBuilder.of()
        .distributionChannel(
            channelResourceIdentifierBuilder ->
                channelResourceIdentifierBuilder.id(
                    cartDiscountValueGiftLineItem.getDistributionChannel().getId()))
        .product(
            productResourceIdentifierBuilder ->
                productResourceIdentifierBuilder.id(
                    cartDiscountValueGiftLineItem.getProduct().getId()))
        .supplyChannel(
            channelResourceIdentifierBuilder ->
                channelResourceIdentifierBuilder.id(
                    cartDiscountValueGiftLineItem.getSupplyChannel().getId()))
        .variantId(cartDiscountValueGiftLineItem.getVariantId())
        .build();
  }

  private static CartDiscountValueFixedDraft cloneCartDiscountValueFixedDraft(
      final CartDiscountValueFixed cartDiscountValue) {
    final CartDiscountValueFixed cartDiscountValueFixed = cartDiscountValue;

    return CartDiscountValueFixedDraftBuilder.of()
        .money(
            cartDiscountValueFixed.getMoney().stream()
                .map(
                    centPrecisionMoney ->
                        MoneyBuilder.of()
                            .currencyCode(centPrecisionMoney.getCurrencyCode())
                            .centAmount(centPrecisionMoney.getCentAmount())
                            .build())
                .collect(Collectors.toList()))
        .build();
  }

  private static CartDiscountValueAbsoluteDraft cloneCartDiscountValueAbsoluteDraft(
      final CartDiscountValueAbsolute cartDiscountValue) {
    final CartDiscountValueAbsolute cartDiscountValueAbsolute = cartDiscountValue;
    return CartDiscountValueAbsoluteDraftBuilder.of()
        .money(
            cartDiscountValueAbsolute.getMoney().stream()
                .map(
                    centPrecisionMoney ->
                        MoneyBuilder.of()
                            .currencyCode(centPrecisionMoney.getCurrencyCode())
                            .centAmount(centPrecisionMoney.getCentAmount())
                            .build())
                .collect(Collectors.toList()))
        .build();
  }

  private CartDiscountReferenceResolutionUtils() {}
}
