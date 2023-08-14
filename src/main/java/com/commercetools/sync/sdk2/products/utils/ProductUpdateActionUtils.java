package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.products.utils.ProductSyncUtils.TEMPORARY_MASTER_SKU_SUFFIX;
import static com.commercetools.sync.sdk2.commons.utils.CollectionUtils.*;
import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.areResourceIdentifiersEqual;
import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActionForReferences;
import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActions;
import static com.commercetools.sync.sdk2.products.ActionGroup.*;
import static com.commercetools.sync.sdk2.products.utils.FilterUtils.executeSupplierIfPassesFilter;
import static com.commercetools.sync.sdk2.products.utils.ProductVariantUpdateActionUtils.buildProductVariantAssetsUpdateActions;
import static com.commercetools.sync.sdk2.products.utils.ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions;
import static com.commercetools.sync.sdk2.products.utils.ProductVariantUpdateActionUtils.buildProductVariantImagesUpdateActions;
import static com.commercetools.sync.sdk2.products.utils.ProductVariantUpdateActionUtils.buildProductVariantPricesUpdateActions;
import static com.commercetools.sync.sdk2.products.utils.ProductVariantUpdateActionUtils.buildProductVariantSkuUpdateAction;
import static com.commercetools.sync.sdk2.products.utils.UnorderedCollectionSyncUtils.buildRemoveUpdateActions;
import static java.lang.String.format;
import static java.util.Collections.*;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.CategoryOrderHints;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductAddToCategoryAction;
import com.commercetools.api.models.product.ProductAddVariantAction;
import com.commercetools.api.models.product.ProductAddVariantActionBuilder;
import com.commercetools.api.models.product.ProductChangeMasterVariantAction;
import com.commercetools.api.models.product.ProductChangeNameAction;
import com.commercetools.api.models.product.ProductChangeSlugAction;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductPublishAction;
import com.commercetools.api.models.product.ProductRemoveFromCategoryAction;
import com.commercetools.api.models.product.ProductRemoveFromCategoryActionBuilder;
import com.commercetools.api.models.product.ProductRemoveVariantAction;
import com.commercetools.api.models.product.ProductSetAttributeInAllVariantsAction;
import com.commercetools.api.models.product.ProductSetAttributeInAllVariantsActionBuilder;
import com.commercetools.api.models.product.ProductSetCategoryOrderHintAction;
import com.commercetools.api.models.product.ProductSetDescriptionAction;
import com.commercetools.api.models.product.ProductSetMetaDescriptionAction;
import com.commercetools.api.models.product.ProductSetMetaKeywordsAction;
import com.commercetools.api.models.product.ProductSetMetaTitleAction;
import com.commercetools.api.models.product.ProductSetSearchKeywordsAction;
import com.commercetools.api.models.product.ProductSetSearchKeywordsActionBuilder;
import com.commercetools.api.models.product.ProductSetSkuActionBuilder;
import com.commercetools.api.models.product.ProductSetTaxCategoryAction;
import com.commercetools.api.models.product.ProductTransitionStateAction;
import com.commercetools.api.models.product.ProductUnpublishAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.SearchKeywords;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.products.ActionGroup;
import com.commercetools.sync.sdk2.products.AttributeMetaData;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.SyncFilter;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public final class ProductUpdateActionUtils {
  private static final String BLANK_VARIANT_KEY = "The variant key is blank.";
  static final String NULL_VARIANT = "The variant is null.";
  static final String BLANK_OLD_MASTER_VARIANT_KEY = "Old master variant key is blank.";
  static final String BLANK_NEW_MASTER_VARIANT_KEY = "New master variant null or has blank key.";
  static final String BLANK_NEW_MASTER_VARIANT_SKU = "New master variant has blank SKU.";

  /**
   * Compares the {@link LocalizedString} names of a {@link ProductDraft} and a {@link
   * ProductProjection}. It returns an {@link ProductChangeNameAction} as a result in an {@link
   * Optional}. If both the {@link ProductProjection} and the {@link ProductDraft} have the same
   * name, then no update action is needed and hence an empty {@link Optional} is returned.
   *
   * <p>NOTE: Comparison is done against the staged projection of the old product.
   *
   * @param oldProduct the productprojection which should be updated.
   * @param newProduct the product draft where we get the new name.
   * @return A filled optional with the update action or an empty optional if the names are
   *     identical.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildChangeNameUpdateAction(
      @Nonnull final ProductProjection oldProduct, @Nonnull final ProductDraft newProduct) {
    final LocalizedString newName = newProduct.getName();
    final LocalizedString oldName = oldProduct.getName();
    return buildUpdateAction(
        oldName,
        newName,
        () -> ProductChangeNameAction.builder().name(newName).staged(true).build());
  }

  /**
   * Compares the {@link LocalizedString} descriptions of a {@link ProductDraft} and a {@link
   * ProductProjection}. It returns an {@link ProductSetDescriptionAction} as a result in an {@link
   * Optional}. If both the {@link ProductProjection} and the {@link ProductDraft} have the same
   * description, then no update action is needed and hence an empty {@link Optional} is returned.
   *
   * <p>NOTE: Comparison is done against the staged projection of the old product.
   *
   * @param oldProduct the productprojection which should be updated.
   * @param newProduct the product draft where we get the new description.
   * @return A filled optional with the update action or an empty optional if the descriptions are
   *     identical.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildSetDescriptionUpdateAction(
      @Nonnull final ProductProjection oldProduct, @Nonnull final ProductDraft newProduct) {
    final LocalizedString newDescription = newProduct.getDescription();
    final LocalizedString oldDescription = oldProduct.getDescription();
    return buildUpdateAction(
        oldDescription,
        newDescription,
        () ->
            ProductSetDescriptionAction.builder().description(newDescription).staged(true).build());
  }

  /**
   * Compares the {@link LocalizedString} slugs of a {@link ProductDraft} and a {@link
   * ProductProjection}. It returns a {@link ProductChangeSlugAction} update action as a result in
   * an {@link Optional}. If both the {@link ProductProjection} and the {@link ProductDraft} have
   * the same slug, then no update action is needed and hence an empty {@link Optional} is returned.
   *
   * <p>NOTE: Comparison is done against the staged projection of the old product.
   *
   * @param oldProduct the productprojection which should be updated.
   * @param newProduct the product draft where we get the new slug.
   * @return A filled optional with the update action or an empty optional if the slugs are
   *     identical.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildChangeSlugUpdateAction(
      @Nonnull final ProductProjection oldProduct, @Nonnull final ProductDraft newProduct) {
    final LocalizedString newSlug = newProduct.getSlug();
    final LocalizedString oldSlug = oldProduct.getSlug();
    return buildUpdateAction(
        oldSlug,
        newSlug,
        () -> ProductChangeSlugAction.builder().slug(newSlug).staged(true).build());
  }

  /**
   * Compares the {@link List} of {@link CategoryReference}s of a {@link ProductDraft} and a {@link
   * ProductProjection}. It returns a {@link List} of {@link ProductAddToCategoryAction} update
   * actions as a result, if the old product needs to be added to a category to have the same set of
   * categories as the new product. If both the {@link ProductProjection} and the {@link
   * ProductDraft} have the same set of categories, then no update actions are needed and hence an
   * empty {@link List} is returned.
   *
   * <p>NOTE: Comparison is done against the staged projection of the old product.
   *
   * @param oldProduct the productprojection which should be updated.
   * @param newProduct the product draft where we get the new slug.
   * @return A list containing the update actions or an empty list if the category sets are
   *     identical.
   */
  @Nonnull
  public static List<ProductUpdateAction> buildAddToCategoryUpdateActions(
      @Nonnull final ProductProjection oldProduct, @Nonnull final ProductDraft newProduct) {
    final List<CategoryResourceIdentifier> newCategories = newProduct.getCategories();
    final List<CategoryReference> oldCategories = oldProduct.getCategories();
    return buildUpdateActions(
        oldCategories,
        newCategories,
        () -> {
          final List<ProductUpdateAction> updateActions = new ArrayList<>();
          final List<CategoryResourceIdentifier> newCategoriesResourceIdentifiers =
              filterCollection(
                      newCategories,
                      newCategoryReference ->
                          oldCategories.stream()
                              .noneMatch(
                                  oldResourceReference ->
                                      areResourceIdentifiersEqual(
                                          oldResourceReference, newCategoryReference)))
                  .collect(toList());
          newCategoriesResourceIdentifiers.forEach(
              categoryResourceIdentifier ->
                  updateActions.add(
                      ProductAddToCategoryAction.builder()
                          .category(categoryResourceIdentifier)
                          .staged(true)
                          .build()));
          return updateActions;
        });
  }

  /**
   * Compares the {@link CategoryOrderHints} of a {@link ProductDraft} and a {@link
   * ProductProjection}. It returns a {@link ProductSetCategoryOrderHintAction} update action as a
   * result in an {@link List}. If both the {@link ProductProjection} and the {@link ProductDraft}
   * have the same categoryOrderHints, then no update actions are needed and hence an empty {@link
   * List} is returned.
   *
   * <p>NOTE: Comparison is done against the staged projection of the old product.
   *
   * <p>{@link ProductProjection} which should be updated.
   *
   * @param oldProduct the productprojection which should be updated.
   * @param newProduct the product draft where we get the new categoryOrderHints.
   * @return A list containing the update actions or an empty list if the categoryOrderHints are
   *     identical.
   */
  @Nonnull
  public static List<ProductUpdateAction> buildSetCategoryOrderHintUpdateActions(
      @Nonnull final ProductProjection oldProduct, @Nonnull final ProductDraft newProduct) {
    final CategoryOrderHints newCategoryOrderHints = newProduct.getCategoryOrderHints();
    final CategoryOrderHints oldCategoryOrderHints = oldProduct.getCategoryOrderHints();
    return buildUpdateActions(
        oldCategoryOrderHints,
        newCategoryOrderHints,
        () -> {
          final Set<String> newCategoryIds =
              newProduct.getCategories() == null
                  ? Collections.emptySet()
                  : newProduct.getCategories().stream()
                      .map(CategoryResourceIdentifier::getId)
                      .collect(toSet());

          final List<ProductUpdateAction> updateActions = new ArrayList<>();

          final Map<String, String> newMap =
              nonNull(newCategoryOrderHints) && nonNull(newCategoryOrderHints.values())
                  ? newCategoryOrderHints.values()
                  : emptyMap();
          final Map<String, String> oldMap =
              nonNull(oldCategoryOrderHints) && nonNull(oldCategoryOrderHints.values())
                  ? oldCategoryOrderHints.values()
                  : emptyMap();

          // remove category hints present in old product if they are absent in draft but only if
          // product
          // is or will be assigned to given category
          oldMap.forEach(
              (categoryId, value) -> {
                if (!newMap.containsKey(categoryId) && newCategoryIds.contains(categoryId)) {
                  updateActions.add(
                      ProductSetCategoryOrderHintAction.builder()
                          .categoryId(categoryId)
                          .orderHint(null)
                          .staged(true)
                          .build());
                }
              });

          // add category hints present in draft if they are absent or changed in old product
          newMap.forEach(
              (categoryId, value) -> {
                if (!oldMap.containsKey(categoryId)
                    || !Objects.equals(oldMap.get(categoryId), value)) {
                  updateActions.add(
                      ProductSetCategoryOrderHintAction.builder()
                          .orderHint(value)
                          .categoryId(categoryId)
                          .staged(true)
                          .build());
                }
              });

          return updateActions;
        });
  }

  /**
   * Compares the {@link List} of {@link CategoryResourceIdentifier} {@link CategoryReference}s of a
   * {@link ProductDraft} and a {@link ProductProjection}. It returns a {@link List} of {@link
   * ProductRemoveFromCategoryAction} update actions as a result, if the old product needs to be
   * removed from a category to have the same set of categories as the new product. If both the
   * {@link ProductProjection} and the {@link ProductDraft} have the same set of categories, then no
   * update actions are needed and hence an empty {@link List} is returned.
   *
   * <p>NOTE: Comparison is done against the staged projection of the old product.
   *
   * @param oldProduct the productprojection which should be updated.
   * @param newProduct the product draft where we get the new slug.
   * @return A list containing the update actions or an empty list if the category sets are
   *     identical.
   */
  @Nonnull
  public static List<ProductUpdateAction> buildRemoveFromCategoryUpdateActions(
      @Nonnull final ProductProjection oldProduct, @Nonnull final ProductDraft newProduct) {
    final List<CategoryResourceIdentifier> newCategories = newProduct.getCategories();
    final List<CategoryReference> oldCategories = oldProduct.getCategories();
    return buildUpdateActions(
        oldCategories,
        newCategories,
        () -> {
          final List<ProductUpdateAction> updateActions = new ArrayList<>();
          filterCollection(
                  oldCategories,
                  oldCategoryReference ->
                      newCategories.stream()
                          .noneMatch(
                              newResourceIdentifier ->
                                  areResourceIdentifiersEqual(
                                      oldCategoryReference, newResourceIdentifier)))
              .forEach(
                  categoryReference ->
                      updateActions.add(
                          ProductRemoveFromCategoryActionBuilder.of()
                              .category(
                                  CategoryResourceIdentifierBuilder.of()
                                      .id(categoryReference.getId())
                                      .build())
                              .staged(true)
                              .build()));
          return updateActions;
        });
  }

  /**
   * Compares the {@link SearchKeywords} of a {@link ProductDraft} and a {@link ProductProjection}.
   * It returns a {@link ProductSetSearchKeywordsAction} update action as a result in an {@link
   * Optional}. If both the {@link ProductProjection} and the {@link ProductDraft} have the same
   * search keywords, then no update action is needed and hence an empty {@link Optional} is
   * returned.
   *
   * <p>NOTE: Comparison is done against the staged projection of the old product.
   *
   * @param oldProduct the productprojection which should be updated.
   * @param newProduct the product draft where we get the new search keywords.
   * @return A filled optional with the update action or an empty optional if the search keywords
   *     are identical.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildSetSearchKeywordsUpdateAction(
      @Nonnull final ProductProjection oldProduct, @Nonnull final ProductDraft newProduct) {
    final SearchKeywords newSearchKeywords = newProduct.getSearchKeywords();
    final SearchKeywords oldSearchKeywords = oldProduct.getSearchKeywords();
    if (newSearchKeywords == null) {
      return Optional.empty();
    } else {
      return buildUpdateAction(
          oldSearchKeywords,
          newSearchKeywords,
          () ->
              ProductSetSearchKeywordsActionBuilder.of()
                  .searchKeywords(newSearchKeywords)
                  .staged(true)
                  .build());
    }
  }

  /**
   * Compares the {@link LocalizedString} meta descriptions of a {@link ProductDraft} and a {@link
   * Product}. It returns a {@link ProductSetMetaDescriptionAction} update action as a result in an
   * {@link Optional}. If both the {@link ProductProjection} and the {@link ProductDraft} have the
   * same meta description, then no update action is needed and hence an empty {@link Optional} is
   * returned.
   *
   * <p>NOTE: Comparison is done against the staged projection of the old product.
   *
   * @param oldProduct the productprojection which should be updated.
   * @param newProduct the product draft where we get the new meta description.
   * @return A filled optional with the update action or an empty optional if the meta descriptions
   *     are identical.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildSetMetaDescriptionUpdateAction(
      @Nonnull final ProductProjection oldProduct, @Nonnull final ProductDraft newProduct) {
    final LocalizedString newMetaDescription = newProduct.getMetaDescription();
    final LocalizedString oldMetaDescription = oldProduct.getMetaDescription();
    return buildUpdateAction(
        oldMetaDescription,
        newMetaDescription,
        () ->
            ProductSetMetaDescriptionAction.builder().metaDescription(newMetaDescription).build());
  }

  /**
   * Compares the {@link LocalizedString} meta keywordss of a {@link ProductDraft} and a {@link
   * Product}. It returns a {@link ProductSetMetaKeywordsAction} update action as a result in an
   * {@link Optional}. If both the {@link ProductProjection} and the {@link ProductDraft} have the
   * same meta keywords, then no update action is needed and hence an empty {@link Optional} is
   * returned.
   *
   * <p>NOTE: Comparison is done against the staged projection of the old product.
   *
   * @param oldProduct the productProjection which should be updated.
   * @param newProduct the product draft where we get the new meta keywords.
   * @return A filled optional with the update action or an empty optional if the meta keywords are
   *     identical.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildSetMetaKeywordsUpdateAction(
      @Nonnull final ProductProjection oldProduct, @Nonnull final ProductDraft newProduct) {
    final LocalizedString newMetaKeywords = newProduct.getMetaKeywords();
    final LocalizedString oldMetaKeywords = oldProduct.getMetaKeywords();
    return buildUpdateAction(
        oldMetaKeywords,
        newMetaKeywords,
        () -> ProductSetMetaKeywordsAction.builder().metaKeywords(newMetaKeywords).build());
  }

  /**
   * Compares the {@link LocalizedString} meta titles of a {@link ProductDraft} and a {@link
   * Product}. It returns a {@link ProductSetMetaTitleAction} update action as a result in an {@link
   * Optional}. If both the {@link ProductProjection} and the {@link ProductDraft} have the same
   * meta title, then no update action is needed and hence an empty {@link Optional} is returned.
   *
   * <p>NOTE: Comparison is done against the staged projection of the old product.
   *
   * @param oldProduct the productprojection which should be updated.
   * @param newProduct the product draft where we get the new meta title.
   * @return A filled optional with the update action or an empty optional if the meta titles are
   *     identical.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildSetMetaTitleUpdateAction(
      @Nonnull final ProductProjection oldProduct, @Nonnull final ProductDraft newProduct) {
    final LocalizedString newMetaTitle = newProduct.getMetaTitle();
    final LocalizedString oldMetaTitle = oldProduct.getMetaTitle();
    return buildUpdateAction(
        oldMetaTitle,
        newMetaTitle,
        () -> ProductSetMetaTitleAction.builder().metaTitle(newMetaTitle).build());
  }

  /**
   * Compares the variants (including the master variants) of a {@link ProductDraft} and a {@link
   * Product}. It returns a {@link List} of variant related update actions. For example:
   *
   * <ul>
   *   <li>{@link ProductAddVariantAction}
   *   <li>{@link ProductRemoveVariantAction}
   *   <li>{@link ProductChangeMasterVariantAction}
   *   <li>{@link com.commercetools.api.models.product.ProductSetAttributeAction}
   *   <li>{@link ProductSetAttributeInAllVariantsAction}
   *   <li>{@link com.commercetools.api.models.product.ProductSetSkuAction}
   *   <li>{@link com.commercetools.api.models.product.ProductAddExternalImageAction}
   *   <li>{@link com.commercetools.api.models.product.ProductRemoveImageAction}
   *   <li>{@link com.commercetools.api.models.product.ProductAddPriceAction}
   *   <li>... and more variant level update actions.
   * </ul>
   *
   * If both the {@link ProductProjection} and the {@link ProductDraft} have identical variants,
   * then no update actions are needed and hence an empty {@link List} is returned.
   *
   * <p>NOTE: Comparison is done against the staged projection of the old product.
   *
   * <p>{@link ProductProjection} which should be updated.
   *
   * @param oldProduct the productprojection which should be updated.
   * @param newProduct the product draft where we get the new meta title.
   * @param syncOptions the sync options wrapper which contains options related to the sync process
   *     supplied by the user. For example, custom callbacks to call in case of warnings or errors
   *     occurring on the build update action process. And other options (See {@link
   *     ProductSyncOptions} for more info).
   * @param attributesMetaData a map of attribute name -&gt; {@link AttributeMetaData}; which
   *     defines attribute information: its name and whether it has the constraint "SameForAll" or
   *     not.
   * @return A list of product variant-specific update actions.
   */
  @Nonnull
  public static List<ProductUpdateAction> buildVariantsUpdateActions(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final ProductDraft newProduct,
      @Nonnull final ProductSyncOptions syncOptions,
      @Nonnull final Map<String, AttributeMetaData> attributesMetaData) {

    if (haveInvalidMasterVariants(oldProduct, newProduct, syncOptions)) {
      return emptyList();
    }

    final ProductVariant oldMasterVariant = oldProduct.getMasterVariant();

    final List<ProductVariant> oldProductVariantsWithoutMaster = oldProduct.getVariants();

    final Map<String, ProductVariant> oldProductVariantsNoMaster =
        collectionToMap(oldProductVariantsWithoutMaster, ProductVariant::getKey);

    final Map<String, ProductVariant> oldProductVariantsWithMaster =
        new HashMap<>(oldProductVariantsNoMaster);
    oldProductVariantsWithMaster.put(oldMasterVariant.getKey(), oldMasterVariant);

    final List<ProductVariantDraft> newAllProductVariants =
        new ArrayList<>(
            newProduct.getVariants() == null ? Collections.emptyList() : newProduct.getVariants());
    final ProductVariantDraft newMasterVariant = newProduct.getMasterVariant();
    newAllProductVariants.add(newMasterVariant);

    // Remove missing variants, but keep master variant (MV can't be removed)
    final List<ProductUpdateAction> updateActions =
        buildRemoveUpdateActions(
            oldProductVariantsWithoutMaster,
            newAllProductVariants,
            ProductVariant::getKey,
            ProductVariantDraft::getKey,
            variant ->
                ProductRemoveVariantAction.builder().id(variant.getId()).staged(true).build());

    emptyIfNull(newAllProductVariants)
        .forEach(
            newProductVariant -> {
              if (newProductVariant == null) {
                handleBuildVariantsUpdateActionsError(oldProduct, NULL_VARIANT, syncOptions);
              } else {
                final String newProductVariantKey = newProductVariant.getKey();
                if (isBlank(newProductVariantKey)) {
                  handleBuildVariantsUpdateActionsError(oldProduct, BLANK_VARIANT_KEY, syncOptions);
                } else {
                  final ProductVariant matchingOldVariant =
                      oldProductVariantsWithMaster.get(newProductVariantKey);
                  final List<ProductUpdateAction> updateOrAddVariant =
                      ofNullable(matchingOldVariant)
                          .map(
                              oldVariant ->
                                  collectAllVariantUpdateActions(
                                      getSameForAllUpdateActions(updateActions),
                                      oldProduct,
                                      newProduct,
                                      oldVariant,
                                      newProductVariant,
                                      attributesMetaData,
                                      syncOptions))
                          .orElseGet(
                              () ->
                                  singletonList(
                                      buildAddVariantUpdateActionFromDraft(newProductVariant)));
                  updateActions.addAll(updateOrAddVariant);
                }
              }
            });

    updateActions.addAll(buildChangeMasterVariantUpdateAction(oldProduct, newProduct, syncOptions));
    if (newMasterVariant != null
        && hasAddVariantUpdateAction(updateActions)
        && hasChangeMasterVariantUpdateAction(updateActions)) {

      // Following update actions helps with the changing of master variants.
      // The details are described here:
      // https://github.com/commercetools/commercetools-project-sync/issues/447
      final String oldMasterVariantSku = oldMasterVariant.getSku();
      final boolean hasConflictingAddVariant =
          hasConflictingAddVariantUpdateAction(updateActions, oldMasterVariantSku);

      if (hasConflictingAddVariant) {
        updateActions.add(
            ProductSetSkuActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .sku(oldMasterVariant.getSku() + TEMPORARY_MASTER_SKU_SUFFIX)
                .build());
      }

      updateActions.addAll(
          buildSetAttributeInAllVariantsUpdateAction(
              attributesMetaData, oldProduct.getMasterVariant(), newMasterVariant));
    }
    return updateActions;
  }

  private static boolean hasConflictingAddVariantUpdateAction(
      final List<ProductUpdateAction> updateActions, final String oldMasterVariantSku) {
    return updateActions.stream()
        .anyMatch(
            productUpdateAction ->
                productUpdateAction instanceof ProductAddVariantAction
                    && ((ProductAddVariantAction) productUpdateAction).getSku() != null
                    && ((ProductAddVariantAction) productUpdateAction)
                        .getSku()
                        .equals(oldMasterVariantSku));
  }

  private static List<ProductUpdateAction> getSameForAllUpdateActions(
      final List<ProductUpdateAction> updateActions) {
    return emptyIfNull(updateActions).stream()
        .filter(
            productUpdateAction ->
                productUpdateAction instanceof ProductSetAttributeInAllVariantsAction)
        .collect(toList());
  }

  /**
   * Returns a list containing all the variants (including the master variant) of the supplied
   * {@link ProductDraft}.
   *
   * @param productDraft the product draft that has the variants and master variant that should be
   *     returned.
   * @return a list containing all the variants (including the master variant) of the supplied
   *     {@link ProductDraft}.
   */
  @Nonnull
  public static List<ProductVariantDraft> getAllVariants(@Nonnull final ProductDraft productDraft) {
    final List<ProductVariantDraft> allVariants = new ArrayList<>();
    if (productDraft.getMasterVariant() != null) {
      allVariants.add(productDraft.getMasterVariant());
      if (productDraft.getVariants() != null && productDraft.getVariants().size() > 0) {
        allVariants.addAll(productDraft.getVariants());
      }
    }
    return allVariants;
  }

  private static boolean hasDuplicateSameForAllAction(
      final List<ProductUpdateAction> sameForAllUpdateActions,
      final ProductUpdateAction collectedUpdateAction) {

    return !(collectedUpdateAction instanceof ProductSetAttributeInAllVariantsAction)
        || isSameForAllActionNew(sameForAllUpdateActions, collectedUpdateAction);
  }

  private static boolean isSameForAllActionNew(
      final List<ProductUpdateAction> sameForAllUpdateActions,
      final ProductUpdateAction productUpdateAction) {

    return sameForAllUpdateActions.stream()
        .noneMatch(
            previouslyAddedAction ->
                previouslyAddedAction instanceof ProductSetAttributeInAllVariantsAction
                    && previouslyAddedAction.getAction().equals(productUpdateAction.getAction()));
  }

  @Nonnull
  private static List<ProductUpdateAction> collectAllVariantUpdateActions(
      @Nonnull final List<ProductUpdateAction> sameForAllUpdateActions,
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final ProductDraft newProduct,
      @Nonnull final ProductVariant oldProductVariant,
      @Nonnull final ProductVariantDraft newProductVariant,
      @Nonnull final Map<String, AttributeMetaData> attributesMetaData,
      @Nonnull final ProductSyncOptions syncOptions) {

    final ArrayList<ProductUpdateAction> updateActions = new ArrayList<>();
    final SyncFilter syncFilter = syncOptions.getSyncFilter();

    updateActions.addAll(
        buildActionsIfPassesFilter(
            syncFilter,
            ATTRIBUTES,
            () ->
                emptyIfNull(
                        buildProductVariantAttributesUpdateActions(
                            oldProduct,
                            newProduct,
                            oldProductVariant,
                            newProductVariant,
                            attributesMetaData,
                            syncOptions))
                    .stream()
                    .filter(
                        collectedUpdateAction ->
                            hasDuplicateSameForAllAction(
                                sameForAllUpdateActions, collectedUpdateAction))
                    .collect(Collectors.toList())));

    updateActions.addAll(
        buildActionsIfPassesFilter(
            syncFilter,
            IMAGES,
            () -> buildProductVariantImagesUpdateActions(oldProductVariant, newProductVariant)));

    updateActions.addAll(
        buildActionsIfPassesFilter(
            syncFilter,
            PRICES,
            () ->
                buildProductVariantPricesUpdateActions(
                    oldProduct, newProduct, oldProductVariant, newProductVariant, syncOptions)));

    updateActions.addAll(
        buildActionsIfPassesFilter(
            syncFilter,
            ASSETS,
            () ->
                buildProductVariantAssetsUpdateActions(
                    oldProduct, newProduct, oldProductVariant, newProductVariant, syncOptions)));

    buildActionIfPassesFilter(
            syncFilter,
            SKU,
            () -> buildProductVariantSkuUpdateAction(oldProductVariant, newProductVariant))
        .ifPresent(updateActions::add);

    return updateActions;
  }

  /**
   * Compares the 'published' field of a {@link ProductDraft} and a {@link ProductProjection} with
   * the new update actions and hasStagedChanges of the old product. Accordingly it returns a {@link
   * ProductPublishAction} or {@link ProductUnpublishAction} update action as a result in an {@link
   * Optional}. Check the calculation table below for all different combinations named as states.
   *
   * <table>
   * <caption>Mapping of product publish/unpublish update action calculation</caption>
   * <thead>
   * <tr>
   * <th>State</th>
   * <th>New draft publish</th>
   * <th>Old product publish</th>
   * <th>New update actions</th>
   * <th>Old product hasStagedChanges</th>
   * <th>Action</th>
   * </tr>
   * </thead>
   * <tbody>
   * <tr>
   * <td>State 1</td>
   * <td>false</td>
   * <td>false</td>
   * <td>false</td>
   * <td>false</td>
   * <td>-</td>
   * </tr>
   * <tr>
   * <td>State 2</td>
   * <td>false</td>
   * <td>false</td>
   * <td>false</td>
   * <td>true</td>
   * <td>-</td>
   * </tr>
   * <tr>
   * <td>State 3</td>
   * <td>false</td>
   * <td>false</td>
   * <td>true</td>
   * <td>false</td>
   * <td>-</td>
   * </tr>
   * <tr>
   * <td>State 4</td>
   * <td>false</td>
   * <td>false</td>
   * <td>true</td>
   * <td>true</td>
   * <td>-</td>
   * </tr>
   * <tr>
   * <td>State 5</td>
   * <td>false</td>
   * <td>true</td>
   * <td>false</td>
   * <td>false</td>
   * <td>unpublish</td>
   * </tr>
   * <tr>
   * <td>State 6</td>
   * <td>false</td>
   * <td>true</td>
   * <td>false</td>
   * <td>true</td>
   * <td>unpublish</td>
   * </tr>
   * <tr>
   * <td>State 7</td>
   * <td>false</td>
   * <td>true</td>
   * <td>true</td>
   * <td>false</td>
   * <td>unpublish</td>
   * </tr>
   * <tr>
   * <td>State 8</td>
   * <td>false</td>
   * <td>true</td>
   * <td>true</td>
   * <td>true</td>
   * <td>unpublish</td>
   * </tr>
   * <tr>
   * <td>State 9</td>
   * <td>true</td>
   * <td>false</td>
   * <td>false</td>
   * <td>false</td>
   * <td>publish</td>
   * </tr>
   * <tr>
   * <td>State 10</td>
   * <td>true</td>
   * <td>false</td>
   * <td>false</td>
   * <td>true</td>
   * <td>publish</td>
   * </tr>
   * <tr>
   * <td>State 11</td>
   * <td>true</td>
   * <td>false</td>
   * <td>true</td>
   * <td>false</td>
   * <td>publish</td>
   * </tr>
   * <tr>
   * <td>State 12</td>
   * <td>true</td>
   * <td>false</td>
   * <td>true</td>
   * <td>true</td>
   * <td>publish</td>
   * </tr>
   * <tr>
   * <td>State 13</td>
   * <td>true</td>
   * <td>true</td>
   * <td>false</td>
   * <td>false</td>
   * <td>-</td>
   * </tr>
   * <tr>
   * <td>State 14</td>
   * <td>true</td>
   * <td>true</td>
   * <td>false</td>
   * <td>true</td>
   * <td>publish</td>
   * </tr>
   * <tr>
   * <td>State 15</td>
   * <td>true</td>
   * <td>true</td>
   * <td>true</td>
   * <td>false</td>
   * <td>publish</td>
   * </tr>
   * <tr>
   * <td>State 16</td>
   * <td>true</td>
   * <td>true</td>
   * <td>true</td>
   * <td>true</td>
   * <td>publish</td>
   * </tr>
   * </tbody>
   * </table>
   *
   * <p>NOTE: Comparison is done against the staged projection of the old product. If the new
   * product's 'published' field is null, then the default false value is assumed.
   *
   * @param oldProduct the productprojection which should be updated.
   * @param newProduct the product draft where we get the new published field value.
   * @param hasNewUpdateActions the product draft has other update actions set.
   * @return A filled optional with the update action or an empty optional if the flag values are
   *     identical.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildPublishOrUnpublishUpdateAction(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final ProductDraft newProduct,
      final boolean hasNewUpdateActions) {

    final boolean isNewProductPublished = toBoolean(newProduct.getPublish());
    final boolean isOldProductPublished = toBoolean(oldProduct.getPublished());

    if (isNewProductPublished) {
      if (isOldProductPublished && (hasNewUpdateActions || oldProduct.getHasStagedChanges())) {
        // covers the state 14, state 15 and state 16.
        return Optional.of(ProductPublishAction.of());
      }
      return buildUpdateAction(isOldProductPublished, true, ProductPublishAction::of);
    }
    return buildUpdateAction(isOldProductPublished, false, ProductUnpublishAction::of);
  }

  /**
   * Create update action, if {@code newProduct} has {@code #masterVariant#key} different than
   * {@code oldProduct} staged {@code #masterVariant#key}.
   *
   * <p>If update action is created - it is created of {@link ProductVariantDraft
   * newProduct.getMasterVariant().getSku()}
   *
   * <p>If old master variant is missing in the new variants list - add {@link
   * ProductRemoveVariantAction} action at the end.
   *
   * @param oldProduct old productprojections with variants
   * @param newProduct new product draft with variants <b>with resolved references prices
   *     references</b>
   * @param syncOptions the sync options wrapper which contains options related to the sync process
   * @return a list of maximum two elements: {@link ProductChangeMasterVariantAction} if the keys
   *     are different, optionally followed by {@link ProductRemoveVariantAction} if the changed
   *     variant does not exist in the new variants list.
   */
  @Nonnull
  public static List<ProductUpdateAction> buildChangeMasterVariantUpdateAction(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final ProductDraft newProduct,
      @Nonnull final ProductSyncOptions syncOptions) {
    final String newKey = newProduct.getMasterVariant().getKey();
    final String oldKey = oldProduct.getMasterVariant().getKey();

    if (haveInvalidMasterVariants(oldProduct, newProduct, syncOptions)) {
      return emptyList();
    }

    return buildUpdateActions(
        newKey,
        oldKey,
        // it might be that the new master variant is from new added variants, so CTP variantId is
        // not set yet,
        // thus we can't use ChangeMasterVariant.ofVariantId(),
        // but it could be re-factored as soon as ChangeMasterVariant.ofKey() happens in the SDK
        () -> {
          final String newSku = newProduct.getMasterVariant().getSku();
          if (isBlank(newSku)) {
            handleBuildVariantsUpdateActionsError(
                oldProduct, BLANK_NEW_MASTER_VARIANT_SKU, syncOptions);
            return emptyList();
          }

          final List<ProductUpdateAction> updateActions = new ArrayList<>(2);
          updateActions.add(
              ProductChangeMasterVariantAction.builder().sku(newSku).staged(true).build());

          // verify whether the old master variant should be removed:
          // if the new variant list doesn't contain the old master variant key.
          // Since we can't remove a variant, if it is master, we have to change MV first, and then
          // remove it
          // (if it does not exist in the new variants list).
          // We don't need to include new master variant to the iteration stream iteration,
          // because this body is called only if newKey != oldKey
          if (newProduct.getVariants() == null
              || newProduct.getVariants().stream().allMatch(Objects::isNull)
              || newProduct.getVariants().stream()
                  .noneMatch(variant -> Objects.equals(variant.getKey(), oldKey))) {
            updateActions.add(
                ProductRemoveVariantAction.builder()
                    .id(oldProduct.getMasterVariant().getId())
                    .build());
          }
          return updateActions;
        });
  }

  private static List<ProductUpdateAction> buildSetAttributeInAllVariantsUpdateAction(
      @Nonnull final Map<String, AttributeMetaData> attributesMetaData,
      @Nonnull final ProductVariant oldMasterVariant,
      @Nonnull final ProductVariantDraft newMasterVariant) {
    final List<ProductUpdateAction> updateActions = new ArrayList<>();
    final List<Attribute> attributes = newMasterVariant.getAttributes();
    if (attributes != null) {
      attributes.forEach(
          attributeDraft -> {
            final AttributeMetaData attributeMetaData =
                attributesMetaData.get(attributeDraft.getName());
            final Boolean isAttributesEqual =
                oldMasterVariant.getAttributes().stream()
                    .filter(oldAttribute -> oldAttribute.getName().equals(attributeDraft.getName()))
                    .findAny()
                    .map(attribute -> attribute.getValue().equals(attributeDraft.getValue()))
                    .orElse(false);
            if (attributeMetaData.isSameForAll() && !isAttributesEqual) {
              updateActions.add(
                  0,
                  ProductSetAttributeInAllVariantsActionBuilder.of()
                      .name(attributeDraft.getName())
                      .value(attributeDraft.getValue())
                      .build());
            }
          });
    }

    return updateActions;
  }

  private static boolean hasChangeMasterVariantUpdateAction(
      final List<ProductUpdateAction> updateActions) {
    return updateActions.stream()
        .anyMatch(updateAction -> updateAction instanceof ProductChangeMasterVariantAction);
  }

  private static boolean hasAddVariantUpdateAction(final List<ProductUpdateAction> updateActions) {
    return updateActions.stream()
        .anyMatch(updateAction -> updateAction instanceof ProductAddVariantAction);
  }

  /**
   * Compares the {@link com.commercetools.api.models.tax_category.TaxCategory} references of an old
   * {@link Product} and new {@link ProductDraft}. If they are different - return {@link
   * ProductSetTaxCategoryAction} update action.
   *
   * <p>If the old value is set, but the new one is empty - the command will unset the tax category.
   *
   * <p>{@link ProductProjection} which should be updated.
   *
   * @param oldProduct the productprojection which should be updated.
   * @param newProduct the product draft with new {@link
   *     com.commercetools.api.models.tax_category.TaxCategory} reference.
   * @return An optional with {@link ProductUpdateAction} update action.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildSetTaxCategoryUpdateAction(
      @Nonnull final ProductProjection oldProduct, @Nonnull final ProductDraft newProduct) {
    return buildUpdateActionForReferences(
        oldProduct.getTaxCategory(),
        newProduct.getTaxCategory(),
        () ->
            ProductSetTaxCategoryAction.builder().taxCategory(newProduct.getTaxCategory()).build());
  }

  /**
   * Compares the {@link State} references of an old {@link ProductProjection} and new {@link
   * ProductDraft}. If they are different - return {@link ProductTransitionStateAction} update
   * action.
   *
   * <p>If the old value is set, but the new one is empty - return empty object, because unset
   * transition state is not possible.
   *
   * <p><b>Note:</b> the transition state action is called with <i>force == true</i>, i.e. the
   * platform won't verify transition
   *
   * @param oldProduct the productprojection which should be updated.
   * @param newProduct the product draft with new {@link State} reference.
   * @return An optional with {@link ProductTransitionStateAction} update action.
   */
  @Nonnull
  public static Optional<ProductUpdateAction> buildTransitionStateUpdateAction(
      @Nonnull final ProductProjection oldProduct, @Nonnull final ProductDraft newProduct) {

    return ofNullable(
        newProduct.getState() != null
                && !Objects.equals(
                    Optional.ofNullable(oldProduct.getState())
                        .map(
                            stateReference ->
                                StateResourceIdentifier.builder()
                                    .id(stateReference.getId())
                                    .build())
                        .orElse(null),
                    newProduct.getState())
            ? ProductTransitionStateAction.builder().state(newProduct.getState()).build()
            : null);
  }

  /**
   * Factory method to create {@link ProductAddVariantAction} action from {@link
   * ProductVariantDraft} instance.
   *
   * <p>The {@link ProductAddVariantAction} will include:
   *
   * <ul>
   *   <li>sku
   *   <li>keys
   *   <li>attributes
   *   <li>prices
   *   <li>images
   *   <li>assets
   * </ul>
   *
   * @param draft {@link ProductVariantDraft} which to add.
   * @return an {@link ProductAddVariantAction} update action with properties from {@code draft}.
   */
  @Nonnull
  static ProductUpdateAction buildAddVariantUpdateActionFromDraft(
      @Nonnull final ProductVariantDraft draft) {

    return ProductAddVariantActionBuilder.of()
        .prices(draft.getPrices())
        .sku(draft.getSku())
        .attributes(draft.getAttributes())
        .staged(true)
        .key(draft.getKey())
        .images(draft.getImages())
        .assets(draft.getAssets())
        .build();
  }

  /**
   * Util that checks if the supplied {@link ActionGroup} passes the {@link SyncFilter}, then it
   * returns the result of the {@code updateActionSupplier} execution, otherwise it returns an empty
   * {@link Optional}.
   *
   * @param syncFilter the sync filter to check the {@code actionGroup} against.
   * @param actionGroup the action group to check against the filter.
   * @param updateActionSupplier the supplier to execute if the {@code actionGroup} passes the
   *     filter.
   * @param <T> the type of the content of the returned {@link Optional}.
   * @return the result of the {@code updateActionSupplier} execution if the {@code actionGroup}
   *     passes the {@code syncFilter}, otherwise it returns an empty {@link Optional}.
   */
  @Nonnull
  static <T> Optional<T> buildActionIfPassesFilter(
      @Nonnull final SyncFilter syncFilter,
      @Nonnull final ActionGroup actionGroup,
      @Nonnull final Supplier<Optional<T>> updateActionSupplier) {
    return executeSupplierIfPassesFilter(
        syncFilter, actionGroup, updateActionSupplier, Optional::empty);
  }

  /**
   * Util that checks if the supplied {@link ActionGroup} passes the {@link SyncFilter}, then it
   * returns the result of the {@code updateActionSupplier} execution, otherwise it returns an empty
   * {@link List}.
   *
   * @param syncFilter the sync filter to check the {@code actionGroup} against.
   * @param actionGroup the action group to check against the filter.
   * @param updateActionSupplier the supplier to execute if the {@code actionGroup} passes the
   *     filter.
   * @param <T> the type of the content of the returned {@link List}.
   * @return the result of the {@code updateActionSupplier} execution if the {@code actionGroup}
   *     passes the {@code syncFilter}, otherwise it returns an empty {@link List}.
   */
  @Nonnull
  static <T> List<T> buildActionsIfPassesFilter(
      @Nonnull final SyncFilter syncFilter,
      @Nonnull final ActionGroup actionGroup,
      @Nonnull final Supplier<List<T>> updateActionSupplier) {
    return executeSupplierIfPassesFilter(
        syncFilter, actionGroup, updateActionSupplier, Collections::emptyList);
  }

  /**
   * Validate both old and new product have master variant with significant key.
   *
   * <p>If at least on of the master variants key not found - the error is reported to {@code
   * syncOptions} and <b>true</b> is returned.
   *
   * @param oldProduct old productprojection to verify
   * @param newProduct new product to verify
   * @param syncOptions {@link BaseSyncOptions#applyErrorCallback(String) applyErrorCallback} holder
   * @return <b>true</b> if at least one of the products have invalid (null/blank) master variant or
   *     key.
   */
  private static boolean haveInvalidMasterVariants(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final ProductDraft newProduct,
      @Nonnull final ProductSyncOptions syncOptions) {
    boolean hasError = false;

    final ProductVariant oldMasterVariant = oldProduct.getMasterVariant();
    if (isBlank(oldMasterVariant.getKey())) {
      handleBuildVariantsUpdateActionsError(oldProduct, BLANK_OLD_MASTER_VARIANT_KEY, syncOptions);
      hasError = true;
    }

    final ProductVariantDraft newMasterVariant = newProduct.getMasterVariant();
    if (newMasterVariant == null || isBlank(newMasterVariant.getKey())) {
      handleBuildVariantsUpdateActionsError(oldProduct, BLANK_NEW_MASTER_VARIANT_KEY, syncOptions);
      hasError = true;
    }

    return hasError;
  }

  /**
   * Apply error message to the {@code syncOptions}, reporting the product key and {@code reason}
   *
   * @param oldProduct product which has sync error
   * @param reason reason to specify in the error message.
   * @param syncOptions {@link BaseSyncOptions#applyErrorCallback(SyncException, Object, Object,
   *     List)} holder
   */
  private static void handleBuildVariantsUpdateActionsError(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final String reason,
      @Nonnull final ProductSyncOptions syncOptions) {
    syncOptions.applyErrorCallback(
        new SyncException(
            format(
                "Failed to build variants update actions on the product with key '%s'. "
                    + "Reason: %s",
                oldProduct.getKey(), reason)),
        oldProduct,
        null,
        null);
  }

  private ProductUpdateActionUtils() {}
}
