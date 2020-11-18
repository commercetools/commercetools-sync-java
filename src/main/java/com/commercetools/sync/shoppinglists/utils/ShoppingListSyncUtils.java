package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;

import javax.annotation.Nonnull;
import java.util.List;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildPrimaryResourceCustomUpdateActions;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.shoppinglists.utils.LineItemUpdateActionUtils.buildLineItemsUpdateActions;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetAnonymousIdUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetCustomerUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetDeleteDaysAfterLastModificationUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetDescriptionUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetSlugUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.TextLineItemUpdateActionUtils.buildTextLineItemsUpdateActions;

public final class ShoppingListSyncUtils {

    private static final ShoppingListCustomActionBuilder shoppingListCustomActionBuilder =
        ShoppingListCustomActionBuilder.of();

    /**
     * Compares all the fields of a {@link ShoppingList} and a {@link ShoppingListDraft}. It returns a {@link List} of
     * {@link UpdateAction}&lt;{@link ShoppingList}&gt; as a result. If no update action is needed, for example in
     * case where both the {@link ShoppingListDraft} and the {@link ShoppingList} have the same fields, an empty
     * {@link List} is returned.
     *
     * @param oldShoppingList the shopping list which should be updated.
     * @param newShoppingList the shopping list draft where we get the new data.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied
     *                    by the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link ShoppingListSyncOptions}
     *                    for more info.
     * @return A list of shopping list specific update actions.
     */
    @Nonnull
    public static List<UpdateAction<ShoppingList>> buildActions(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList,
        @Nonnull final ShoppingListSyncOptions syncOptions) {

        final List<UpdateAction<ShoppingList>> updateActions = filterEmptyOptionals(
            buildSetSlugUpdateAction(oldShoppingList, newShoppingList),
            buildChangeNameUpdateAction(oldShoppingList, newShoppingList),
            buildSetDescriptionUpdateAction(oldShoppingList, newShoppingList),
            buildSetAnonymousIdUpdateAction(oldShoppingList, newShoppingList),
            buildSetCustomerUpdateAction(oldShoppingList, newShoppingList),
            buildSetDeleteDaysAfterLastModificationUpdateAction(oldShoppingList, newShoppingList)
        );

        updateActions.addAll(buildPrimaryResourceCustomUpdateActions(oldShoppingList,
            newShoppingList::getCustom,
            shoppingListCustomActionBuilder,
            syncOptions));

        updateActions.addAll(buildLineItemsUpdateActions(oldShoppingList, newShoppingList, syncOptions));

        updateActions.addAll(buildTextLineItemsUpdateActions(oldShoppingList, newShoppingList, syncOptions));

        return updateActions;
    }

    private ShoppingListSyncUtils() {
    }
}
