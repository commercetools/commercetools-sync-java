package com.commercetools.sync.sdk2.products.models;

import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.sync.sdk2.commons.models.Custom;
import com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils;
import org.jetbrains.annotations.Nullable;

/** Adapt Customer with {@link Custom} interface to be used on {@link CustomUpdateActionUtils} */
public final class PriceCustomTypeAdapter implements Custom {

  private final Price price;

  private PriceCustomTypeAdapter(Price price) {
    this.price = price;
  }

  /**
   * Get Id of the {@link Price}
   *
   * @return the {@link Price#getId()}
   */
  @Override
  public String getId() {
    return this.price.getId();
  }

  /**
   * Get typeId of the {@link Price} see: https://docs.commercetools.com/api/types#referencetype
   *
   * @return the typeId "price"
   */
  @Override
  public String getTypeId() {
    return "product-price";
  }

  /**
   * Get custom fields of the {@link Price}
   *
   * @return the {@link CustomFields}
   */
  @Nullable
  @Override
  public CustomFields getCustom() {
    return this.price.getCustom();
  }

  /**
   * Get Key of the {@link Price}
   *
   * @return the {@link Price#getKey()}
   */
  public String getKey() {
    return this.price.getKey();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link Price}
   *
   * @param price the {@link Price}
   * @return the {@link PriceCustomTypeAdapter}
   */
  public static PriceCustomTypeAdapter of(Price price) {
    return new PriceCustomTypeAdapter(price);
  }
}
