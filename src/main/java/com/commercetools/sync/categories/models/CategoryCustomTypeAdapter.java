package com.commercetools.sync.categories.models;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.sync.commons.models.Custom;
import javax.annotation.Nullable;

/**
 * Adapt Category with {@link com.commercetools.sync.commons.models.Custom} interface to be used on
 * {@link com.commercetools.sync.commons.utils.CustomUpdateActionUtils}
 */
public final class CategoryCustomTypeAdapter implements Custom {

  private final Category category;

  private CategoryCustomTypeAdapter(Category category) {
    this.category = category;
  }

  /**
   * Get Id of the {@link com.commercetools.api.models.category.Category}
   *
   * @return the {@link com.commercetools.api.models.category.Category#getId()}
   */
  @Override
  public String getId() {
    return this.category.getId();
  }

  /**
   * Get typeId of the {@link com.commercetools.api.models.category.Category} see:
   * https://docs.commercetools.com/api/types#referencetype
   *
   * @return the typeId "category"
   */
  @Override
  public String getTypeId() {
    return "category";
  }

  /**
   * Get custom fields of the {@link com.commercetools.api.models.category.Category}
   *
   * @return the {@link com.commercetools.api.models.type.CustomFields}
   */
  @Nullable
  @Override
  public CustomFields getCustom() {
    return this.category.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link
   * com.commercetools.api.models.category.Category}
   *
   * @param category the {@link com.commercetools.api.models.category.Category}
   * @return the {@link CategoryCustomTypeAdapter}
   */
  public static CategoryCustomTypeAdapter of(Category category) {
    return new CategoryCustomTypeAdapter(category);
  }
}
