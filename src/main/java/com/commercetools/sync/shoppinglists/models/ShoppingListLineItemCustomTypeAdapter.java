package com.commercetools.sync.shoppinglists.models;

import com.commercetools.api.models.shopping_list.ShoppingListLineItem;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.sync.commons.models.Custom;
import javax.annotation.Nullable;

/**
 * Adapt ShoppingListLineItem with {@link com.commercetools.sync.commons.models.Custom} interface to
 * be used on {@link com.commercetools.sync.commons.utils.CustomUpdateActionUtils}
 */
public final class ShoppingListLineItemCustomTypeAdapter implements Custom {

  private final ShoppingListLineItem shoppingListLineItem;

  private ShoppingListLineItemCustomTypeAdapter(ShoppingListLineItem shoppingListLineItem) {
    this.shoppingListLineItem = shoppingListLineItem;
  }

  /**
   * Get Id of the {@link ShoppingListLineItem}
   *
   * @return the {@link ShoppingListLineItem#getId()}
   */
  @Override
  public String getId() {
    return this.shoppingListLineItem.getId();
  }

  /**
   * Get typeId of the {@link ShoppingListLineItem} see:
   * https://docs.commercetools.com/api/types#referencetype
   *
   * @return the typeId "shopping-list"
   */
  @Override
  public String getTypeId() {
    return "shopping-list";
  }

  /**
   * Get custom fields of the {@link ShoppingListLineItem}
   *
   * @return the {@link com.commercetools.api.models.type.CustomFields}
   */
  @Nullable
  @Override
  public CustomFields getCustom() {
    return this.shoppingListLineItem.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link
   * ShoppingListLineItem}
   *
   * @param shoppingList the {@link ShoppingListLineItem}
   * @return the {@link ShoppingListLineItemCustomTypeAdapter}
   */
  public static ShoppingListLineItemCustomTypeAdapter of(ShoppingListLineItem shoppingList) {
    return new ShoppingListLineItemCustomTypeAdapter(shoppingList);
  }
}
