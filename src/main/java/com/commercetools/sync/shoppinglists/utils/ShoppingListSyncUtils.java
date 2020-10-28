package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
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

    @Nonnull
    public static List<UpdateAction<ShoppingList>> buildActions(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList,
        @Nonnull final ShoppingListSyncOptions syncOptions) {

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
