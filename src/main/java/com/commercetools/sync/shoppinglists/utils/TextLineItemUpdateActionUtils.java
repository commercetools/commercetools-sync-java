package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.TextLineItem;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;

import javax.annotation.Nonnull;
import java.util.List;

public final class TextLineItemUpdateActionUtils {

    /**
     * Compares the custom fields and custom types of a {@link TextLineItem} and a {@link TextLineItemDraft} and returns
     * a list of {@link UpdateAction}&lt;{@link ShoppingList}&gt; as a result. If both the {@link TextLineItem} and the
     * {@link TextLineItemDraft} have identical custom fields and types, then no update action is needed and hence an
     * empty {@link List} is returned.
     *
     * @param oldShoppingList shopping list resource, whose text line item should be updated.
     * @param newShoppingList new shopping list draft, which contains the text line item to update.
     * @param oldTextLineItem the text line item which should be updated.
     * @param newTextLineItem the text line item draft where we get the new custom fields and types.
     * @param syncOptions     responsible for supplying the sync options to the sync utility method. It is used for
     *                        triggering the error callback within the utility, in case of errors.
     * @return A list with the custom field/type update actions or an empty list if the custom fields/types are
     *         identical.
     */
    @Nonnull
    public static List<UpdateAction<ShoppingList>> buildTextLineItemCustomUpdateActions(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList,
        @Nonnull final TextLineItem oldTextLineItem,
        @Nonnull final TextLineItemDraft newTextLineItem,
        @Nonnull final ShoppingListSyncOptions syncOptions) {

        return CustomUpdateActionUtils.buildCustomUpdateActions(
            oldShoppingList,
            newShoppingList,
            oldTextLineItem::getCustom,
            newTextLineItem::getCustom,
            new TextLineItemCustomActionBuilder(),
            null, // not used by util.
            t -> oldTextLineItem.getId(),
            lineItem -> TextLineItem.resourceTypeId(),
            t -> oldTextLineItem.getId(),
            syncOptions);
    }

    private TextLineItemUpdateActionUtils() {
    }
}
