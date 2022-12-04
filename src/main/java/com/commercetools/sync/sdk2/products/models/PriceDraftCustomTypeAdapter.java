package com.commercetools.sync.sdk2.products.models;

import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.sync.sdk2.commons.models.CustomDraft;
import com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Adapt Customer with {@link CustomDraft} interface to be used on {@link CustomUpdateActionUtils}
 */
public final class PriceDraftCustomTypeAdapter implements CustomDraft {

  private final PriceDraft priceDraft;

  private PriceDraftCustomTypeAdapter(PriceDraft priceDraft) {
    this.priceDraft = priceDraft;
  }

  /**
   * Get custom fields of the {@link PriceDraft}
   *
   * @return the {@link CustomFieldsDraft}
   */
  @Nullable
  @Override
  public CustomFieldsDraft getCustom() {
    return this.priceDraft.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link
   * PriceDraft}
   *
   * @param priceDraft the {@link PriceDraft}
   * @return the {@link PriceDraftCustomTypeAdapter}
   */
  public static PriceDraftCustomTypeAdapter of(PriceDraft priceDraft) {
    return new PriceDraftCustomTypeAdapter(priceDraft);
  }
}
