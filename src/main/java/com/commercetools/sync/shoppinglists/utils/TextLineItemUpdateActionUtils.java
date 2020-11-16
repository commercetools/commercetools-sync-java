package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.TextLineItem;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeTextLineItemQuantity;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetTextLineItemDescription;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;

public final class TextLineItemUpdateActionUtils {

    /**
     * Compares the {@link LocalizedString} descriptions of {@link TextLineItem} and a {@link TextLineItemDraft} and
     * returns an {@link Optional} of update action, which would contain the {@code "setTextLineItemDescription"}
     * {@link UpdateAction}. If both the {@link TextLineItem} and the {@link TextLineItemDraft} have the same
     * {@code description} values, then no update action is needed and hence an empty optional will be returned.
     *
     * @param oldTextLineItem the text line item which should be updated.
     * @param newTextLineItem the text line item draft where we get the new description.
     * @return A filled optional with the update action or an empty optional if the descriptions are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ShoppingList>> buildSetTextLineItemDescriptionUpdateAction(
        @Nonnull final TextLineItem oldTextLineItem,
        @Nonnull final TextLineItemDraft newTextLineItem) {

        return buildUpdateAction(oldTextLineItem.getDescription(), newTextLineItem.getDescription(), () ->
            SetTextLineItemDescription.of(oldTextLineItem).withDescription(newTextLineItem.getDescription()));
    }

    /**
     * Compares the {@code quantity} values of a {@link TextLineItem} and a {@link TextLineItemDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeTextLineItemQuantity"}
     * {@link UpdateAction}. If both {@link TextLineItem} and {@link TextLineItemDraft} have the same
     * {@code quantity} values, then no update action is needed and empty optional will be returned.
     *
     * <p>Note: If {@code quantity} from the {@code newTextLineItem} is {@code null}, the new {@code quantity}
     * will be set to default value {@code 1L}.  If {@code quantity} from the {@code newTextLineItem} is {@code 0},
     * then it means removing the text line item.
     *
     * @param oldTextLineItem the text line item which should be updated.
     * @param newTextLineItem the text line item draft where we get the new quantity.
     * @return A filled optional with the update action or an empty optional if the quantities are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ShoppingList>> buildChangeTextLineItemQuantityUpdateAction(
        @Nonnull final TextLineItem oldTextLineItem,
        @Nonnull final TextLineItemDraft newTextLineItem) {

        final Long newTextLineItemQuantity = newTextLineItem.getQuantity() == null
            ? NumberUtils.LONG_ONE : newTextLineItem.getQuantity();

        return buildUpdateAction(oldTextLineItem.getQuantity(), newTextLineItemQuantity,
            () -> ChangeTextLineItemQuantity.of(oldTextLineItem.getId(), newTextLineItemQuantity));
    }

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
