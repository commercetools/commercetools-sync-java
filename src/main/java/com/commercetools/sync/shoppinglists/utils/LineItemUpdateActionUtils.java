package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeLineItemQuantity;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;

public final class LineItemUpdateActionUtils {

    /**
     * Compares the {@code quantity} values of an {@link LineItem} and an {@link LineItemDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeLineItemQuantity"}
     * {@link UpdateAction}. If both {@link LineItem} and {@link LineItemDraft} have the same
     * {@code quantity} values, then no update action is needed and empty optional will be returned.
     *
     * <p>Note: If {@code quantity} from the {@code newLineItem} is {@code null} or {@code 0}, the new {@code quantity}
     * will be set to default value {@code 1L}.
     *
     * @param oldLineItem the line item which should be updated.
     * @param newLineItem the line item draft where we get the new quantity.
     * @return A filled optional with the update action or an empty optional if the quantities are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ShoppingList>> buildChangeLineItemQuantityUpdateAction(
        @Nonnull final LineItem oldLineItem,
        @Nonnull final LineItemDraft newLineItem) {

        final Long newLineItemQuantity = newLineItem.getQuantity() == null
            || newLineItem.getQuantity().equals(NumberUtils.LONG_ZERO)
            ? NumberUtils.LONG_ONE : newLineItem.getQuantity();

        return buildUpdateAction(oldLineItem.getQuantity(), newLineItemQuantity,
            () -> ChangeLineItemQuantity.of(oldLineItem.getId(), newLineItemQuantity));
    }

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
    public static List<UpdateAction<ShoppingList>> buildLineItemCustomUpdateActions(
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
