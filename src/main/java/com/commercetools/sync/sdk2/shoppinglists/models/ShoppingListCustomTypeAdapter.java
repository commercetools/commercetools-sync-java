package com.commercetools.sync.sdk2.shoppinglists.models;

import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.sync.sdk2.commons.models.Custom;
import javax.annotation.Nullable;

/**
 * Adapt ShoppingList with {@link com.commercetools.sync.sdk2.commons.models.Custom} interface to be
 * used on {@link com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils}
 */
public final class ShoppingListCustomTypeAdapter implements Custom {

  private final ShoppingList shoppingList;

  private ShoppingListCustomTypeAdapter(ShoppingList shoppingList) {
    this.shoppingList = shoppingList;
  }

  /**
   * Get Id of the {@link com.commercetools.api.models.shopping_list.ShoppingList}
   *
   * @return the {@link com.commercetools.api.models.shopping_list.ShoppingList#getId()}
   */
  @Override
  public String getId() {
    return this.shoppingList.getId();
  }

  /**
   * Get typeId of the {@link com.commercetools.api.models.shopping_list.ShoppingList} see:
   * https://docs.commercetools.com/api/types#referencetype
   *
   * @return the typeId "shopping-list"
   */
  @Override
  public String getTypeId() {
    return "shopping-list";
  }

  /**
   * Get custom fields of the {@link com.commercetools.api.models.shopping_list.ShoppingList}
   *
   * @return the {@link com.commercetools.api.models.type.CustomFields}
   */
  @Nullable
  @Override
  public CustomFields getCustom() {
    return this.shoppingList.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link
   * com.commercetools.api.models.shopping_list.ShoppingList}
   *
   * @param shoppingList the {@link com.commercetools.api.models.shopping_list.ShoppingList}
   * @return the {@link
   *     com.commercetools.sync.sdk2.shoppinglists.models.ShoppingListCustomTypeAdapter}
   */
  public static ShoppingListCustomTypeAdapter of(ShoppingList shoppingList) {
    return new ShoppingListCustomTypeAdapter(shoppingList);
  }
}
