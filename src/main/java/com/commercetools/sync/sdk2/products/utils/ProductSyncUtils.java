package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.commons.utils.CollectionUtils.emptyIfNull;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildActionIfPassesFilter;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildActionsIfPassesFilter;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildAddToCategoryUpdateActions;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildChangeSlugUpdateAction;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildPublishOrUnpublishUpdateAction;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildRemoveFromCategoryUpdateActions;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildSetCategoryOrderHintUpdateActions;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildSetDescriptionUpdateAction;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildSetMetaDescriptionUpdateAction;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildSetMetaKeywordsUpdateAction;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildSetMetaTitleUpdateAction;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildSetSearchKeywordsUpdateAction;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildSetTaxCategoryUpdateAction;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildTransitionStateUpdateAction;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildVariantsUpdateActions;

import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductRemoveVariantAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.sync.products.ActionGroup;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.SyncFilter;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public final class ProductSyncUtils {

  private static final String REMOVE_VARIANT_ACTION_NAME = "removeVariant";
  private static final String SET_ATTRIBUTE_IN_ALL_VARIANTS_ACTION_NAME =
      "setAttributeInAllVariants";
  private static final String ADD_VARIANT_ACTION_NAME = "addVariant";
  private static final String CHANGE_MASTER_VARIANT_ACTION_NAME = "changeMasterVariant";

  /**
   * Compares all the fields (including the variants see {@link
   * ProductUpdateActionUtils#buildVariantsUpdateActions(ProductProjection, ProductDraft,
   * ProductSyncOptions, Map)}) of a {@link ProductProjection} and a {@link ProductDraft}, given
   * that each of these fields pass the specified {@link SyncFilter}. It returns a {@link List} of
   * {@link ProductUpdateAction}&lt;{@link Product}&gt; as a result. If no update action is needed,
   * for example in case where both the {@link ProductProjection} and the {@link ProductDraft} have
   * the same names, an empty {@link List} is returned. Then it applies a specified filter function
   * in the {@link ProductSyncOptions} instance on the resultant list and returns this result.
   *
   * @param oldProduct the productprojection which should be updated.
   * @param newProduct the product draft where we get the new data.
   * @param syncOptions the sync options wrapper which contains options related to the sync process
   *     supplied by the user. For example, custom callbacks to call in case of warnings or errors
   *     occurring on the build update action process. And other options (See {@link
   *     ProductSyncOptions} for more info).
   * @param attributesMetaData a map of attribute name -&gt; {@link AttributeMetaData}; which
   *     defines each attribute's information: its name and whether it has the constraint
   *     "SameForAll" or not.
   * @return A list of product-specific update actions.
   */
  @Nonnull
  public static List<ProductUpdateAction> buildActions(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final ProductDraft newProduct,
      @Nonnull final ProductSyncOptions syncOptions,
      @Nonnull final Map<String, AttributeMetaData> attributesMetaData) {

    final SyncFilter syncFilter = syncOptions.getSyncFilter();

    final List<ProductUpdateAction> updateActions =
        new ArrayList<>(
            filterEmptyOptionals(
                buildActionIfPassesFilter(
                    syncFilter,
                    ActionGroup.NAME,
                    () -> buildChangeNameUpdateAction(oldProduct, newProduct)),
                buildActionIfPassesFilter(
                    syncFilter,
                    ActionGroup.DESCRIPTION,
                    () -> buildSetDescriptionUpdateAction(oldProduct, newProduct)),
                buildActionIfPassesFilter(
                    syncFilter,
                    ActionGroup.SLUG,
                    () -> buildChangeSlugUpdateAction(oldProduct, newProduct)),
                buildActionIfPassesFilter(
                    syncFilter,
                    ActionGroup.SEARCHKEYWORDS,
                    () -> buildSetSearchKeywordsUpdateAction(oldProduct, newProduct)),
                buildActionIfPassesFilter(
                    syncFilter,
                    ActionGroup.METATITLE,
                    () -> buildSetMetaTitleUpdateAction(oldProduct, newProduct)),
                buildActionIfPassesFilter(
                    syncFilter,
                    ActionGroup.METADESCRIPTION,
                    () -> buildSetMetaDescriptionUpdateAction(oldProduct, newProduct)),
                buildActionIfPassesFilter(
                    syncFilter,
                    ActionGroup.METAKEYWORDS,
                    () -> buildSetMetaKeywordsUpdateAction(oldProduct, newProduct)),
                buildActionIfPassesFilter(
                    syncFilter,
                    ActionGroup.TAXCATEGORY,
                    () -> buildSetTaxCategoryUpdateAction(oldProduct, newProduct)),
                buildActionIfPassesFilter(
                    syncFilter,
                    ActionGroup.STATE,
                    () -> buildTransitionStateUpdateAction(oldProduct, newProduct))));

    final List<ProductUpdateAction> productCategoryUpdateActions =
        buildActionsIfPassesFilter(
            syncFilter, ActionGroup.CATEGORIES, () -> buildCategoryActions(oldProduct, newProduct));
    updateActions.addAll(productCategoryUpdateActions);

    updateActions.addAll(
        buildVariantsUpdateActions(oldProduct, newProduct, syncOptions, attributesMetaData));

    // lastly publish/unpublish product
    final boolean hasNewUpdateActions = updateActions.size() > 0;
    buildPublishOrUnpublishUpdateAction(oldProduct, newProduct, hasNewUpdateActions)
        .ifPresent(updateActions::add);

    return prioritizeUpdateActions(updateActions, oldProduct.getMasterVariant().getId());
  }

  /**
   * @param updateActions All generated update actions for all variants
   * @param oldMasterVariantId The masterVariant of the old product fetched from Target
   * @return An ordered list of UpdateActions as follows: 1 removeVariant actions except master 2
   *     sameForAll Actions before executing addVariant to avoid possible errors 3 addVariant
   *     actions 4 changeMasterVariant if any 5 removeVariant for old master 6 the rest comes in
   */
  private static List<ProductUpdateAction> prioritizeUpdateActions(
      final List<ProductUpdateAction> updateActions, final Long oldMasterVariantId) {

    final ProductRemoveVariantAction removeMasterVariantUpdateAction =
        ProductRemoveVariantAction.builder().id(oldMasterVariantId).build();

    final List<ProductUpdateAction> removeVariantUpdateActionsNoMaster =
        getActionsByActionName(
            updateActions,
            action ->
                action.getAction().equals(REMOVE_VARIANT_ACTION_NAME)
                    && !action.equals(removeMasterVariantUpdateAction));

    final List<ProductUpdateAction> sameForAllUpdateActions =
        getActionsByActionName(
            updateActions,
            action -> action.getAction().equals(SET_ATTRIBUTE_IN_ALL_VARIANTS_ACTION_NAME));

    final List<ProductUpdateAction> addVariantUpdateActions =
        getActionsByActionName(
            updateActions, action -> action.getAction().equals(ADD_VARIANT_ACTION_NAME));

    final List<ProductUpdateAction> changeMasterUpdateActions =
        getActionsByActionName(
            updateActions, action -> action.getAction().equals(CHANGE_MASTER_VARIANT_ACTION_NAME));

    final List<ProductUpdateAction> removeOldMasterVariantUpdateAction =
        getActionsByActionName(
            updateActions,
            action ->
                action.getAction().equals(REMOVE_VARIANT_ACTION_NAME)
                    && action.equals(removeMasterVariantUpdateAction));

    final List<ProductUpdateAction> updateActionList =
        new ArrayList<>(removeVariantUpdateActionsNoMaster);
    updateActionList.addAll(sameForAllUpdateActions);
    updateActionList.addAll(addVariantUpdateActions);
    updateActionList.addAll(changeMasterUpdateActions);
    updateActionList.addAll(removeOldMasterVariantUpdateAction);
    updateActionList.addAll(updateActions);

    return updateActionList;
  }

  private static List<ProductUpdateAction> getActionsByActionName(
      final List<ProductUpdateAction> updateActions,
      final Predicate<ProductUpdateAction> updateActionPredicate) {

    final List<ProductUpdateAction> filteredUpdateActions =
        emptyIfNull(updateActions).stream()
            .filter(updateActionPredicate)
            .collect(Collectors.toList());
    updateActions.removeAll(filteredUpdateActions);

    return filteredUpdateActions;
  }

  /**
   * Compares the categories of a {@link ProductProjection} and a {@link ProductDraft}. It returns a
   * {@link List} of {@link ProductUpdateAction}&lt;{@link Product}&gt; as a result. If no update
   * action is needed, for example in case where both the {@link ProductProjection} and the {@link
   * ProductDraft} have the identical categories, an empty {@link List} is returned.
   *
   * @param oldProduct the productprojection which should be updated.
   * @param newProduct the product draft where we get the new data.
   * @return A list of product category-related update actions.
   */
  @Nonnull
  public static List<ProductUpdateAction> buildCategoryActions(
      @Nonnull final ProductProjection oldProduct, @Nonnull final ProductDraft newProduct) {
    final List<ProductUpdateAction> updateActions = new ArrayList<>();
    updateActions.addAll(buildAddToCategoryUpdateActions(oldProduct, newProduct));
    updateActions.addAll(buildSetCategoryOrderHintUpdateActions(oldProduct, newProduct));
    updateActions.addAll(buildRemoveFromCategoryUpdateActions(oldProduct, newProduct));
    return updateActions;
  }

  private ProductSyncUtils() {}
}
