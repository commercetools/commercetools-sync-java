package com.commercetools.sync.sdk2.categories.utils;

import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActionForReferences;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryChangeNameActionBuilder;
import com.commercetools.api.models.category.CategoryChangeOrderHintActionBuilder;
import com.commercetools.api.models.category.CategoryChangeParentActionBuilder;
import com.commercetools.api.models.category.CategoryChangeSlugActionBuilder;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.category.CategorySetDescriptionActionBuilder;
import com.commercetools.api.models.category.CategorySetExternalIdActionBuilder;
import com.commercetools.api.models.category.CategorySetMetaDescriptionActionBuilder;
import com.commercetools.api.models.category.CategorySetMetaKeywordsActionBuilder;
import com.commercetools.api.models.category.CategorySetMetaTitleActionBuilder;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.sync.sdk2.categories.CategorySyncOptions;
import com.commercetools.sync.sdk2.categories.helpers.CategoryAssetActionFactory;
import com.commercetools.sync.sdk2.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.AssetsUpdateActionUtils;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

public final class CategoryUpdateActionUtils {
  private static final String CATEGORY_CHANGE_PARENT_EMPTY_PARENT =
      "Cannot unset 'parent' field of category with id '%s'.";
  private static final String CATEGORY_CHANGE_ORDER_HINT_EMPTY_ORDERHINT =
      "Cannot unset 'orderHint' field of category with id '%s'.";

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} names of a {@link
   * Category} and a {@link CategoryDraft} and returns an {@link CategoryUpdateAction} as a result
   * in an {@link java.util.Optional}. If both the {@link Category} and the {@link CategoryDraft}
   * have the same name, then no update action is needed and hence an empty {@link
   * java.util.Optional} is returned.
   *
   * @param oldCategory the category which should be updated.
   * @param newCategory the category draft where we get the new name.
   * @return A filled optional with the update action or an empty optional if the names are
   *     identical.
   */
  @Nonnull
  public static Optional<CategoryUpdateAction> buildChangeNameUpdateAction(
      @Nonnull final Category oldCategory, @Nonnull final CategoryDraft newCategory) {
    return buildUpdateAction(
        oldCategory.getName(),
        newCategory.getName(),
        () -> CategoryChangeNameActionBuilder.of().name(newCategory.getName()).build());
  }

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} slugs of a {@link
   * Category} and a {@link CategoryDraft} and returns an {@link CategoryUpdateAction} as a result
   * in an {@link java.util.Optional}. If both the {@link Category} and the {@link CategoryDraft}
   * have the same slug, then no update action is needed and hence an empty {@link
   * java.util.Optional} is returned.
   *
   * @param oldCategory the category which should be updated.
   * @param newCategory the category draft where we get the new slug.
   * @return A filled optional with the update action or an empty optional if the slugs are
   *     identical.
   */
  @Nonnull
  public static Optional<CategoryUpdateAction> buildChangeSlugUpdateAction(
      @Nonnull final Category oldCategory, @Nonnull final CategoryDraft newCategory) {
    return buildUpdateAction(
        oldCategory.getSlug(),
        newCategory.getSlug(),
        () -> CategoryChangeSlugActionBuilder.of().slug(newCategory.getSlug()).build());
  }

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} descriptions of a
   * {@link Category} and a {@link CategoryDraft} and returns an {@link CategoryUpdateAction} as a
   * result in an {@link java.util.Optional}. If both the {@link Category} and the {@link
   * CategoryDraft} have the same description, then no update action is needed and hence an empty
   * {@link java.util.Optional} is returned.
   *
   * @param oldCategory the category which should be updated.
   * @param newCategory the category draft where we get the new description.
   * @return A filled optional with the update action or an empty optional if the descriptions are
   *     identical.
   */
  @Nonnull
  public static Optional<CategoryUpdateAction> buildSetDescriptionUpdateAction(
      @Nonnull final Category oldCategory, @Nonnull final CategoryDraft newCategory) {
    return buildUpdateAction(
        oldCategory.getDescription(),
        newCategory.getDescription(),
        () ->
            CategorySetDescriptionActionBuilder.of()
                .description(newCategory.getDescription())
                .build());
  }

  /**
   * Compares the parents {@link CategoryReference} of a {@link Category} and a {@link
   * CategoryDraft} and returns an {@link io.sphere.sdk.commands.UpdateAction}&lt;{@link
   * Category}&gt; as a result in an {@link java.util.Optional}. If both the {@link Category} and
   * the {@link CategoryDraft} have the same parents, then no update action is needed and hence an
   * empty {@link java.util.Optional} is returned.
   *
   * <p>Note: If the parent {@link CategoryReference} of the new {@link CategoryDraft} is null, an
   * empty {@link java.util.Optional} is returned with no update actions and a custom callback
   * function, if set on the supplied {@link com.commercetools.sync.categories.CategorySyncOptions},
   * is called.
   *
   * @param oldCategory the category which should be updated.
   * @param newCategory the category draft where we get the new parent.
   * @param syncOptions the sync syncOptions with which a custom callback function is called in case
   *     the parent is null.
   * @return A filled optional with the update action or an empty optional if the parent references
   *     are identical.
   */
  @Nonnull
  public static Optional<CategoryUpdateAction> buildChangeParentUpdateAction(
      @Nonnull final Category oldCategory,
      @Nonnull final CategoryDraft newCategory,
      @Nonnull final CategorySyncOptions syncOptions) {

    final CategoryReference oldParent = oldCategory.getParent();
    final CategoryResourceIdentifier newParent = newCategory.getParent();
    if (newParent == null && oldParent != null) {
      syncOptions.applyWarningCallback(
          new SyncException(format(CATEGORY_CHANGE_PARENT_EMPTY_PARENT, oldCategory.getId())),
          oldCategory,
          newCategory);
      return Optional.empty();
    } else {
      // The newParent.getId() call below can not cause an NPE in this case, since if both newParent
      // and oldParent
      // are null, then the supplier will not be called at all. The remaining cases all involve the
      // newParent
      // being not null.
      return buildUpdateActionForReferences(
          oldParent,
          newParent,
          () ->
              CategoryChangeParentActionBuilder.of()
                  .parent(CategoryResourceIdentifierBuilder.of().id(newParent.getId()).build())
                  .build());
    }
  }

  /**
   * Compares the orderHint values of a {@link Category} and a {@link CategoryDraft} and returns an
   * {@link CategoryUpdateAction} as a result in an {@link java.util.Optional}. If both the {@link
   * Category} and the {@link CategoryDraft} have the same orderHint, then no update action is
   * needed and hence an empty {@link java.util.Optional} is returned.
   *
   * <p>Note: If the orderHint of the new {@link CategoryDraft} is null, an empty {@link
   * java.util.Optional} is returned with no update actions and a custom callback function, if set
   * on the supplied {@link CategorySyncOptions}, is called.
   *
   * @param oldCategory the category which should be updated.
   * @param newCategory the category draft where we get the new orderHint.
   * @param syncOptions the sync syncOptions with which a custom callback function is called in case
   *     the orderHint is null.
   * @return A filled optional with the update action or an empty optional if the orderHint values
   *     are identical.
   */
  @Nonnull
  public static Optional<CategoryUpdateAction> buildChangeOrderHintUpdateAction(
      @Nonnull final Category oldCategory,
      @Nonnull final CategoryDraft newCategory,
      @Nonnull final CategorySyncOptions syncOptions) {
    if (newCategory.getOrderHint() == null && oldCategory.getOrderHint() != null) {
      syncOptions.applyWarningCallback(
          new SyncException(
              format(CATEGORY_CHANGE_ORDER_HINT_EMPTY_ORDERHINT, oldCategory.getId())),
          oldCategory,
          newCategory);
      return Optional.empty();
    }
    return buildUpdateAction(
        oldCategory.getOrderHint(),
        newCategory.getOrderHint(),
        () ->
            CategoryChangeOrderHintActionBuilder.of()
                .orderHint(newCategory.getOrderHint())
                .build());
  }

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} meta title of a {@link
   * Category} and a {@link CategoryDraft} and returns an {@link CategoryUpdateAction} as a result
   * in an {@link java.util.Optional}. If both the {@link Category} and the {@link CategoryDraft}
   * have the same meta title, then no update action is needed and hence an empty {@link
   * java.util.Optional} is returned.
   *
   * @param oldCategory the category which should be updated.
   * @param newCategory the category draft where we get the new meta title.
   * @return A filled optional with the update action or an empty optional if the meta titles values
   *     are identical.
   */
  @Nonnull
  public static Optional<CategoryUpdateAction> buildSetMetaTitleUpdateAction(
      @Nonnull final Category oldCategory, @Nonnull final CategoryDraft newCategory) {
    return buildUpdateAction(
        oldCategory.getMetaTitle(),
        newCategory.getMetaTitle(),
        () -> CategorySetMetaTitleActionBuilder.of().metaTitle(newCategory.getMetaTitle()).build());
  }

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} meta keywords of a
   * {@link Category} and a {@link CategoryDraft} and returns an {@link CategoryUpdateAction} as a
   * result in an {@link java.util.Optional}. If both the {@link Category} and the {@link
   * CategoryDraft} have the same meta keywords, then no update action is needed and hence an empty
   * {@link java.util.Optional} is returned.
   *
   * @param oldCategory the category which should be updated.
   * @param newCategory the category draft where we get the new meta keywords.
   * @return A filled optional with the update action or an empty optional if the meta keywords
   *     values are identical.
   */
  @Nonnull
  public static Optional<CategoryUpdateAction> buildSetMetaKeywordsUpdateAction(
      @Nonnull final Category oldCategory, @Nonnull final CategoryDraft newCategory) {
    return buildUpdateAction(
        oldCategory.getMetaKeywords(),
        newCategory.getMetaKeywords(),
        () ->
            CategorySetMetaKeywordsActionBuilder.of()
                .metaKeywords(newCategory.getMetaKeywords())
                .build());
  }

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} meta description of a
   * {@link Category} and a {@link CategoryDraft} and returns an {@link CategoryUpdateAction} as a
   * result in an {@link java.util.Optional}. If both the {@link Category} and the {@link
   * CategoryDraft} have the same meta description, then no update action is needed and hence an
   * empty {@link java.util.Optional} is returned.
   *
   * @param oldCategory the category which should be updated.
   * @param newCategory the category draft where we get the new meta description.
   * @return A filled optional with the update action or an empty optional if the meta description
   *     values are identical.
   */
  @Nonnull
  public static Optional<CategoryUpdateAction> buildSetMetaDescriptionUpdateAction(
      @Nonnull final Category oldCategory, @Nonnull final CategoryDraft newCategory) {
    return buildUpdateAction(
        oldCategory.getMetaDescription(),
        newCategory.getMetaDescription(),
        () ->
            CategorySetMetaDescriptionActionBuilder.of()
                .metaDescription(newCategory.getMetaDescription())
                .build());
  }

  /**
   * Compares the externalId values of a {@link Category} and a {@link CategoryDraft} and returns an
   * {@link CategoryUpdateAction} as a result in an {@link java.util.Optional}. If both the {@link
   * Category} and the {@link CategoryDraft} have the same externalId, then no update action is
   * needed and hence an empty {@link java.util.Optional} is returned.
   *
   * @param oldCategory the category which should be updated.
   * @param newCategory the category draft where we get the new externalId.
   * @return A filled optional with the update action or an empty optional if the externalId values
   *     are identical.
   */
  @Nonnull
  public static Optional<CategoryUpdateAction> buildSetExternalIdUpdateAction(
      @Nonnull final Category oldCategory, @Nonnull final CategoryDraft newCategory) {
    return buildUpdateAction(
        oldCategory.getExternalId(),
        newCategory.getExternalId(),
        () ->
            CategorySetExternalIdActionBuilder.of()
                .externalId(newCategory.getExternalId())
                .build());
  }

  /**
   * Compares the assets of a {@link Category} and a {@link CategoryDraft} and returns a list of
   * {@link CategoryUpdateAction} as a result. If both the {@link Category} and the {@link
   * CategoryDraft} have the identical assets, then no update action is needed and hence an empty
   * {@link java.util.List} is returned. In case, the new category draft has a list of assets in
   * which a duplicate key exists, the error callback is triggered and an empty list is returned.
   *
   * @param oldCategory the category which should be updated.
   * @param newCategory the category draft where we get the new externalId.
   * @param syncOptions the sync options with which a custom callback function is called in case
   *     errors exists while building assets custom field/type actions.
   * @return A list with the update actions or an empty list if the assets are identical.
   */
  @Nonnull
  public static List<CategoryUpdateAction> buildAssetsUpdateActions(
      @Nonnull final Category oldCategory,
      @Nonnull final CategoryDraft newCategory,
      @Nonnull final CategorySyncOptions syncOptions) {

    try {
      return AssetsUpdateActionUtils.buildAssetsUpdateActions(
          newCategory,
          oldCategory.getAssets(),
          newCategory.getAssets(),
          new CategoryAssetActionFactory(syncOptions),
          syncOptions);
    } catch (final BuildUpdateActionException exception) {
      syncOptions.applyErrorCallback(
          new SyncException(
              format(
                  "Failed to build update actions for the assets "
                      + "of the category with the key '%s'.",
                  oldCategory.getKey()),
              exception),
          oldCategory,
          newCategory,
          null);
      return emptyList();
    }
  }

  private CategoryUpdateActionUtils() {}
}
