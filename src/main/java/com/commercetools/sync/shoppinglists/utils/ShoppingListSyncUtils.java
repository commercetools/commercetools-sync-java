package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;

import javax.annotation.Nonnull;
import java.util.List;

import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetAnonymousIdUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetCustomerUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetDeleteDaysAfterLastModificationUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetDescriptionUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetSlugUpdateAction;

public class ShoppingListSyncUtils {

    /**
     * Compares all the fields of a {@link Customer} and a {@link CustomerDraft}. It returns a {@link List} of
     * {@link UpdateAction}&lt;{@link Customer}&gt; as a result. If no update action is needed, for example in
     * case where both the {@link CustomerDraft} and the {@link CustomerDraft} have the same fields, an empty
     * {@link List} is returned.
     *
     * @param oldShoppingList the shopping list which should be updated.
     * @param newShoppingList the shopping list draft where we get the new data
     * @return A list of shopping list specific update actions.
     */

    @Nonnull
    public static List<UpdateAction<ShoppingList>> buildActions(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList) {

        final List<UpdateAction<ShoppingList>> updateActions = filterEmptyOptionals(
            buildSetSlugUpdateAction(oldShoppingList, newShoppingList),
            buildChangeNameUpdateAction(oldShoppingList, newShoppingList),
            buildSetDescriptionUpdateAction(oldShoppingList, newShoppingList),
            buildSetCustomerUpdateAction(oldShoppingList, newShoppingList),
            buildSetAnonymousIdUpdateAction(oldShoppingList, newShoppingList),
            buildSetDeleteDaysAfterLastModificationUpdateAction(oldShoppingList, newShoppingList)
        );

        return updateActions;
    }


}
