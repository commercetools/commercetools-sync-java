package com.commercetools.sync.sdk2.cartdiscounts.models;

import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.sync.sdk2.commons.models.Custom;
import javax.annotation.Nullable;

/**
 * Adapt CartDiscount with {@link com.commercetools.sync.sdk2.commons.models.Custom} interface to be
 * used on {@link com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils}
 */
public final class CartDiscountCustomTypeAdapter implements Custom {

  private final CartDiscount cartDiscount;

  private CartDiscountCustomTypeAdapter(CartDiscount cartDiscount) {
    this.cartDiscount = cartDiscount;
  }

  /**
   * Get Id of the {@link CartDiscount}
   *
   * @return the {@link CartDiscount#getId()}
   */
  @Override
  public String getId() {
    return this.cartDiscount.getId();
  }

  /**
   * Get typeId of the {@link CartDiscount} see:
   * https://docs.commercetools.com/api/types#referencetype
   *
   * @return the typeId "cartDiscount"
   */
  @Override
  public String getTypeId() {
    return "cartDiscount";
  }

  /**
   * Get custom fields of the {@link CartDiscount}
   *
   * @return the {@link com.commercetools.api.models.type.CustomFields}
   */
  @Nullable
  @Override
  public CustomFields getCustom() {
    return this.cartDiscount.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link
   * CartDiscount}
   *
   * @param cartDiscount the {@link CartDiscount}
   * @return the {@link CartDiscountCustomTypeAdapter}
   */
  public static CartDiscountCustomTypeAdapter of(CartDiscount cartDiscount) {
    return new CartDiscountCustomTypeAdapter(cartDiscount);
  }
}
