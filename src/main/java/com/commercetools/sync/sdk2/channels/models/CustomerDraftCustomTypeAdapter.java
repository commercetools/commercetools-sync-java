package com.commercetools.sync.sdk2.channels.models;

import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.sync.sdk2.commons.models.CustomDraft;
import com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils;
import javax.annotation.Nullable;

/**
 * Adapt Customer with {@link CustomDraft} interface to be used on {@link CustomUpdateActionUtils}
 */
public final class CustomerDraftCustomTypeAdapter implements CustomDraft {

  private final CustomerDraft customerDraft;

  private CustomerDraftCustomTypeAdapter(CustomerDraft customerDraft) {
    this.customerDraft = customerDraft;
  }

  /**
   * Get custom fields of the {@link CustomerDraft}
   *
   * @return the {@link CustomFieldsDraft}
   */
  @Nullable
  @Override
  public CustomFieldsDraft getCustom() {
    return this.customerDraft.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link
   * CustomerDraft}
   *
   * @param customerDraft the {@link CustomerDraft}
   * @return the {@link CustomerDraftCustomTypeAdapter}
   */
  public static CustomerDraftCustomTypeAdapter of(CustomerDraft customerDraft) {
    return new CustomerDraftCustomTypeAdapter(customerDraft);
  }
}
