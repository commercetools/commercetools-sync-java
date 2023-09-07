package com.commercetools.sync.customers.models;

import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.sync.commons.models.Custom;
import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import javax.annotation.Nullable;

/** Adapt Customer with {@link Custom} interface to be used on {@link CustomUpdateActionUtils} */
public final class CustomerCustomTypeAdapter implements Custom {

  private final Customer customer;

  private CustomerCustomTypeAdapter(Customer customer) {
    this.customer = customer;
  }

  /**
   * Get Id of the {@link Customer}
   *
   * @return the {@link Customer#getId()}
   */
  @Override
  public String getId() {
    return this.customer.getId();
  }

  /**
   * Get typeId of the {@link Customer} see: https://docs.commercetools.com/api/types#referencetype
   *
   * @return the typeId "customer"
   */
  @Override
  public String getTypeId() {
    return "customer";
  }

  /**
   * Get custom fields of the {@link Customer}
   *
   * @return the {@link CustomFields}
   */
  @Nullable
  @Override
  public CustomFields getCustom() {
    return this.customer.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link
   * Customer}
   *
   * @param customer the {@link Customer}
   * @return the {@link CustomerCustomTypeAdapter}
   */
  public static CustomerCustomTypeAdapter of(Customer customer) {
    return new CustomerCustomTypeAdapter(customer);
  }
}
