package com.commercetools.sync.sdk2.categories.utils;

import static com.commercetools.sync.sdk2.categories.utils.CategoryUpdateActionUtils.*;
import static com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils.buildPrimaryResourceCustomUpdateActions;
import static com.commercetools.sync.sdk2.commons.utils.OptionalUtils.filterEmptyOptionals;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.sync.sdk2.categories.CategorySyncOptions;
import com.commercetools.sync.sdk2.categories.helpers.CategoryCustomActionBuilder;
import com.commercetools.sync.sdk2.categories.models.CategoryCustomTypeAdapter;
import com.commercetools.sync.sdk2.categories.models.CategoryDraftCustomTypeAdapter;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import java.util.List;
import javax.annotation.Nonnull;

public final class CategorySyncUtils {
  private static final CategoryCustomActionBuilder categoryCustomActionBuilder =
      new CategoryCustomActionBuilder();

  /**
   * Compares all the fields of a {@link Category} and a {@link CategoryDraft}. It returns a {@link
   * List} of {@link com.commercetools.api.models.category.CategoryUpdateAction} as a result. If no
   * update action is needed, for example in case where both the {@link Category} and the {@link
   * CategoryDraft} have the same parents, an empty {@link List} is returned.
   *
   * @param oldCategory the category which should be updated.
   * @param newCategory the category draft where we get the new data.
   * @param syncOptions the sync options wrapper which contains options related to the sync process
   *     supplied by the user. For example, custom callbacks to call in case of warnings or errors
   *     occurring on the build update action process. And other options (See {@link
   *     BaseSyncOptions} for more info.
   * @return A list of category-specific update actions.
   */
  @Nonnull
  public static List<CategoryUpdateAction> buildActions(
      @Nonnull final Category oldCategory,
      @Nonnull final CategoryDraft newCategory,
      @Nonnull final CategorySyncOptions syncOptions) {

    final List<CategoryUpdateAction> updateActions =
        filterEmptyOptionals(
            buildChangeNameUpdateAction(oldCategory, newCategory),
            buildChangeSlugUpdateAction(oldCategory, newCategory),
            buildSetExternalIdUpdateAction(oldCategory, newCategory),
            buildSetDescriptionUpdateAction(oldCategory, newCategory),
            buildChangeParentUpdateAction(oldCategory, newCategory, syncOptions),
            buildChangeOrderHintUpdateAction(oldCategory, newCategory, syncOptions),
            buildSetMetaTitleUpdateAction(oldCategory, newCategory),
            buildSetMetaDescriptionUpdateAction(oldCategory, newCategory),
            buildSetMetaKeywordsUpdateAction(oldCategory, newCategory));

    final List<CategoryUpdateAction> categoryCustomUpdateActions =
        buildPrimaryResourceCustomUpdateActions(
            CategoryCustomTypeAdapter.of(oldCategory),
            CategoryDraftCustomTypeAdapter.of(newCategory),
            categoryCustomActionBuilder,
            syncOptions);

    final List<CategoryUpdateAction> assetsUpdateActions =
        buildAssetsUpdateActions(oldCategory, newCategory, syncOptions);

    updateActions.addAll(categoryCustomUpdateActions);
    updateActions.addAll(assetsUpdateActions);

    return updateActions;
  }

  private CategorySyncUtils() {}
}
