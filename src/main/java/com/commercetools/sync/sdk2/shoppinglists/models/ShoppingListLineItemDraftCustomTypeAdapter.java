package com.commercetools.sync.sdk2.shoppinglists.models;

import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraft;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.sync.sdk2.commons.models.CustomDraft;
import javax.annotation.Nullable;

/**
 * Adapt ShoppingListLineItemDraft with {@link
 * com.commercetools.sync.sdk2.commons.models.CustomDraft} interface to be used on {@link
 * com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils}
 */
public final class ShoppingListLineItemDraftCustomTypeAdapter implements CustomDraft {

  private final ShoppingListLineItemDraft shoppingListLineItemDraft;

  private ShoppingListLineItemDraftCustomTypeAdapter(
      ShoppingListLineItemDraft shoppingListLineItemDraft) {
    this.shoppingListLineItemDraft = shoppingListLineItemDraft;
  }

  /**
   * Get custom fields of the {@link ShoppingListLineItemDraft}
   *
   * @return the {@link com.commercetools.api.models.type.CustomFieldsDraft}
   */
  @Nullable
  @Override
  public CustomFieldsDraft getCustom() {
    return this.shoppingListLineItemDraft.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link
   * ShoppingListLineItemDraft}
   *
   * @param shoppingListLineItemDraft the {@link ShoppingListLineItemDraft}
   * @return the {@link ShoppingListLineItemDraftCustomTypeAdapter}
   */
  public static ShoppingListLineItemDraftCustomTypeAdapter of(
      ShoppingListLineItemDraft shoppingListLineItemDraft) {
    return new ShoppingListLineItemDraftCustomTypeAdapter(shoppingListLineItemDraft);
  }
}
