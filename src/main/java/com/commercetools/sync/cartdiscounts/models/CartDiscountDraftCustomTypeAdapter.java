package com.commercetools.sync.cartdiscounts.models;

import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.sync.commons.models.CustomDraft;
import javax.annotation.Nullable;

/**
 * Adapt CartDiscountDraft with {@link com.commercetools.sync.commons.models.CustomDraft} interface
 * to be used on {@link com.commercetools.sync.commons.utils.CustomUpdateActionUtils}
 */
public final class CartDiscountDraftCustomTypeAdapter implements CustomDraft {

  private final CartDiscountDraft cartDiscountDraft;

  private CartDiscountDraftCustomTypeAdapter(CartDiscountDraft cartDiscountDraft) {
    this.cartDiscountDraft = cartDiscountDraft;
  }

  /**
   * Get custom fields of the {@link CartDiscountDraft}
   *
   * @return the {@link com.commercetools.api.models.type.CustomFieldsDraft}
   */
  @Nullable
  @Override
  public CustomFieldsDraft getCustom() {
    return this.cartDiscountDraft.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link
   * CartDiscountDraft}
   *
   * @param cartDiscountDraft the {@link CartDiscountDraft}
   * @return the {@link CartDiscountDraftCustomTypeAdapter}
   */
  public static CartDiscountDraftCustomTypeAdapter of(CartDiscountDraft cartDiscountDraft) {
    return new CartDiscountDraftCustomTypeAdapter(cartDiscountDraft);
  }
}
