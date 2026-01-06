package com.commercetools.sync.shoppinglists.utils;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildPrimaryResourceCustomUpdateActions;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.shoppinglists.utils.LineItemUpdateActionUtils.buildLineItemsUpdateActions;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.*;
import static com.commercetools.sync.shoppinglists.utils.TextLineItemUpdateActionUtils.buildTextLineItemsUpdateActions;

import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.models.ShoppingListCustomTypeAdapter;
import java.util.List;
import javax.annotation.Nonnull;

public final class ShoppingListSyncUtils {

  private static final ShoppingListCustomActionBuilder shoppingListCustomActionBuilder =
      ShoppingListCustomActionBuilder.of();

  /**
   * Compares all the fields of a {@link ShoppingList} and a {@link ShoppingListDraft}. It returns a
   * {@link java.util.List} of {@link ShoppingListUpdateAction} as a result. If no update action is
   * needed, for example in case where both the {@link ShoppingListDraft} and the {@link
   * ShoppingList} have the same fields, an empty {@link java.util.List} is returned.
   *
   * @param oldShoppingList the shopping list which should be updated.
   * @param newShoppingList the shopping list draft where we get the new data.
   * @param syncOptions the sync options wrapper which contains options related to the sync process
   *     supplied by the user. For example, custom callbacks to call in case of warnings or errors
   *     occurring on the build update action process. And other options (See {@link
   *     ShoppingListSyncOptions} for more info.
   * @return A list of shopping list specific update actions.
   */
  @Nonnull
  public static List<ShoppingListUpdateAction> buildActions(
      @Nonnull final ShoppingList oldShoppingList,
      @Nonnull final ShoppingListDraft newShoppingList,
      @Nonnull final ShoppingListSyncOptions syncOptions) {

    final List<ShoppingListUpdateAction> updateActions =
        filterEmptyOptionals(
            buildSetSlugUpdateAction(oldShoppingList, newShoppingList),
            buildChangeNameUpdateAction(oldShoppingList, newShoppingList),
            buildSetDescriptionUpdateAction(oldShoppingList, newShoppingList),
            buildSetAnonymousIdUpdateAction(oldShoppingList, newShoppingList),
            buildSetCustomerUpdateAction(oldShoppingList, newShoppingList),
            buildSetStoreUpdateAction(oldShoppingList, newShoppingList),
            buildSetDeleteDaysAfterLastModificationUpdateAction(oldShoppingList, newShoppingList));

    updateActions.addAll(
        buildPrimaryResourceCustomUpdateActions(
            ShoppingListCustomTypeAdapter.of(oldShoppingList),
            newShoppingList::getCustom,
            shoppingListCustomActionBuilder,
            syncOptions));

    updateActions.addAll(
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, syncOptions));

    updateActions.addAll(
        buildTextLineItemsUpdateActions(oldShoppingList, newShoppingList, syncOptions));

    return updateActions;
  }

  private ShoppingListSyncUtils() {}
}
