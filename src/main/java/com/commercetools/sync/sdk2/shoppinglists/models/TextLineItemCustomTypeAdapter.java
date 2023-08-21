package com.commercetools.sync.sdk2.shoppinglists.models;

import com.commercetools.api.models.shopping_list.TextLineItem;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.sync.sdk2.commons.models.Custom;
import javax.annotation.Nullable;

/**
 * Adapt TextLineItem with {@link com.commercetools.sync.sdk2.commons.models.Custom} interface to be
 * used on {@link com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils}
 */
public final class TextLineItemCustomTypeAdapter implements Custom {

  private final TextLineItem textLineItem;

  private TextLineItemCustomTypeAdapter(TextLineItem textLineItem) {
    this.textLineItem = textLineItem;
  }

  /**
   * Get Id of the {@link com.commercetools.api.models.shopping_list.TextLineItem}
   *
   * @return the {@link com.commercetools.api.models.shopping_list.TextLineItem#getId()}
   */
  @Override
  public String getId() {
    return this.textLineItem.getId();
  }

  /**
   * Get typeId of the {@link com.commercetools.api.models.shopping_list.TextLineItem} see:
   * https://docs.commercetools.com/api/types#referencetype
   *
   * @return the typeId "shopping-list"
   */
  @Override
  public String getTypeId() {
    return "shopping-list";
  }

  /**
   * Get custom fields of the {@link com.commercetools.api.models.shopping_list.TextLineItem}
   *
   * @return the {@link com.commercetools.api.models.type.CustomFields}
   */
  @Nullable
  @Override
  public CustomFields getCustom() {
    return this.textLineItem.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link
   * com.commercetools.api.models.shopping_list.TextLineItem}
   *
   * @param textLineItem the {@link com.commercetools.api.models.shopping_list.TextLineItem}
   * @return the {@link TextLineItemCustomTypeAdapter}
   */
  public static TextLineItemCustomTypeAdapter of(TextLineItem textLineItem) {
    return new TextLineItemCustomTypeAdapter(textLineItem);
  }
}
