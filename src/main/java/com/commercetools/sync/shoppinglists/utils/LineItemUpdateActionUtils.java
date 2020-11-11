package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;

import javax.annotation.Nonnull;
import java.util.List;

public final class LineItemUpdateActionUtils {

    /**
     * Compares the custom fields and custom types of an {@link LineItem} and an {@link LineItemDraft} and returns a
     * list of {@link UpdateAction}&lt;{@link ShoppingList}&gt; as a result. If both the {@link LineItem} and the
     * {@link LineItemDraft} have identical custom fields and types, then no update action is needed and hence an empty
     * {@link List} is returned.
     *
     * @param oldShoppingList shopping list resource, whose line item should be updated.
     * @param newShoppingList new shopping list draft, which contains the line item to update.
     * @param oldLineItem     the line item which should be updated.
     * @param newLineItem     the line item draft where we get the new custom fields and types.
     * @param syncOptions     responsible for supplying the sync options to the sync utility method. It is used for
     *                        triggering the error callback within the utility, in case of errors.
     * @return A list with the custom field/type update actions or an empty list if the custom fields/types are
     *         identical.
     */
    @Nonnull
    public static List<UpdateAction<ShoppingList>> buildCustomUpdateActions(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList,
        @Nonnull final LineItem oldLineItem,
        @Nonnull final LineItemDraft newLineItem,
        @Nonnull final ShoppingListSyncOptions syncOptions) {

        return CustomUpdateActionUtils.buildCustomUpdateActions(
            oldShoppingList,
            newShoppingList,
            oldLineItem::getCustom,
            newLineItem::getCustom,
            new LineItemCustomActionBuilder(),
            -1,
            t -> oldLineItem.getId(),
            lineItem -> LineItem.resourceTypeId(),
            t -> oldLineItem.getId(),
            syncOptions);
    }

    private LineItemUpdateActionUtils() {
    }
}
