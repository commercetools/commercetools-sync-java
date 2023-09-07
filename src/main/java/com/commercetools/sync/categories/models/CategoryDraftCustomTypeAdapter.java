package com.commercetools.sync.categories.models;

import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.sync.commons.models.CustomDraft;
import javax.annotation.Nullable;

/**
 * Adapt Category with {@link com.commercetools.sync.commons.models.CustomDraft} interface to be
 * used on {@link com.commercetools.sync.commons.utils.CustomUpdateActionUtils}
 */
public final class CategoryDraftCustomTypeAdapter implements CustomDraft {

  private final CategoryDraft categoryDraft;

  private CategoryDraftCustomTypeAdapter(CategoryDraft categoryDraft) {
    this.categoryDraft = categoryDraft;
  }

  /**
   * Get custom fields of the {@link CategoryDraft}
   *
   * @return the {@link com.commercetools.api.models.type.CustomFieldsDraft}
   */
  @Nullable
  @Override
  public CustomFieldsDraft getCustom() {
    return this.categoryDraft.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link
   * CategoryDraft}
   *
   * @param categoryDraft the {@link CategoryDraft}
   * @return the {@link CategoryDraftCustomTypeAdapter}
   */
  public static CategoryDraftCustomTypeAdapter of(CategoryDraft categoryDraft) {
    return new CategoryDraftCustomTypeAdapter(categoryDraft);
  }
}
