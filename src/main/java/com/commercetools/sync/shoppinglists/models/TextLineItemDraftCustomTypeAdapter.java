package com.commercetools.sync.shoppinglists.models;

import com.commercetools.api.models.shopping_list.TextLineItemDraft;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.sync.commons.models.CustomDraft;
import javax.annotation.Nullable;

/**
 * Adapt TextLineItemDraft with {@link com.commercetools.sync.commons.models.CustomDraft} interface
 * to be used on {@link com.commercetools.sync.commons.utils.CustomUpdateActionUtils}
 */
public final class TextLineItemDraftCustomTypeAdapter implements CustomDraft {

  private final TextLineItemDraft textLineItemDraft;

  private TextLineItemDraftCustomTypeAdapter(TextLineItemDraft textLineItemDraft) {
    this.textLineItemDraft = textLineItemDraft;
  }

  /**
   * Get custom fields of the {@link com.commercetools.api.models.shopping_list.TextLineItemDraft}
   *
   * @return the {@link com.commercetools.api.models.type.CustomFieldsDraft}
   */
  @Nullable
  @Override
  public CustomFieldsDraft getCustom() {
    return this.textLineItemDraft.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link
   * com.commercetools.api.models.shopping_list.TextLineItemDraft}
   *
   * @param textLineItemDraft the {@link
   *     com.commercetools.api.models.shopping_list.TextLineItemDraft}
   * @return the {@link TextLineItemDraftCustomTypeAdapter}
   */
  public static TextLineItemDraftCustomTypeAdapter of(TextLineItemDraft textLineItemDraft) {
    return new TextLineItemDraftCustomTypeAdapter(textLineItemDraft);
  }
}
