package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.commands.updateactions.AddTextLineItemWithAddedAt;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.TextLineItem;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeTextLineItemName;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeTextLineItemQuantity;
import io.sphere.sdk.shoppinglists.commands.updateactions.RemoveTextLineItem;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetTextLineItemDescription;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public final class TextLineItemUpdateActionUtils {

    /**
     * Compares a list of {@link TextLineItem}s with a list of {@link TextLineItemDraft}s.
     * The method takes in functions for building the required update actions (AddTextLineItem, RemoveTextLineItem and
     * 1-1 update actions on text line items (e.g. changeTextLineItemQuantity, setTextLineItemCustomType, etc..).
     *
     * <p>If the list of new {@link TextLineItemDraft}s is {@code null}, then remove actions are built for every
     * existing text line item.
     *
     * @param oldShoppingList shopping list resource, whose text line items should be updated.
     * @param newShoppingList new shopping list draft, which contains the text line items to update.
     * @param syncOptions     responsible for supplying the sync options to the sync utility method. It is used for
     *                        triggering the error callback within the utility, in case of errors.
     * @return a list of text line item update actions on the resource of shopping lists, if the list of text line items
     *         are not identical. Otherwise, if the text line items are identical, an empty list is returned.
     */
    @Nonnull
    public static List<UpdateAction<ShoppingList>> buildTextLineItemsUpdateActions(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList,
        @Nonnull final ShoppingListSyncOptions syncOptions) {

        final boolean hasOldTextLineItems = oldShoppingList.getTextLineItems() != null
            && !oldShoppingList.getTextLineItems().isEmpty();
        final boolean hasNewTextLineItems = newShoppingList.getTextLineItems() != null
            && !newShoppingList.getTextLineItems().isEmpty()
            && newShoppingList.getTextLineItems().stream().anyMatch(Objects::nonNull);

        if (hasOldTextLineItems && !hasNewTextLineItems) {

            return oldShoppingList.getTextLineItems()
                                  .stream()
                                  .map(RemoveTextLineItem::of)
                                  .collect(toList());

        } else if (!hasOldTextLineItems) {

            if (!hasNewTextLineItems) {
                return emptyList();
            }

            return newShoppingList.getTextLineItems()
                                  .stream()
                                  .filter(Objects::nonNull)
                                  .filter(TextLineItemUpdateActionUtils::hasQuantity)
                                  .map(AddTextLineItemWithAddedAt::of)
                                  .collect(toList());
        }

        final List<TextLineItem> oldTextLineItems = oldShoppingList.getTextLineItems();
        final List<TextLineItemDraft> newTextLineItems = newShoppingList.getTextLineItems()
                                                                        .stream()
                                                                        .filter(Objects::nonNull)
                                                                        .collect(toList());

        return buildUpdateActions(oldShoppingList, newShoppingList, oldTextLineItems, newTextLineItems, syncOptions);
    }

    private static boolean hasQuantity(@Nonnull final TextLineItemDraft textLineItemDraft) {
        /*

         with this check, it's avoided bad request case like below:

         "code": "InvalidField",
         "message": "The value '0' is not valid for field 'quantity'. Quantity 0 is not allowed.",

        */
        return textLineItemDraft.getQuantity() != null && textLineItemDraft.getQuantity() > 0;
    }

    /**
     * The decisions in the calculating update actions are documented on the
     * `docs/adr/0002-shopping-lists-lineitem-and-textlineitem-update-actions.md`
     */
    @Nonnull
    private static List<UpdateAction<ShoppingList>> buildUpdateActions(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList,
        @Nonnull final List<TextLineItem> oldTextLineItems,
        @Nonnull final List<TextLineItemDraft> newTextLineItems,
        @Nonnull final ShoppingListSyncOptions syncOptions) {

        final List<UpdateAction<ShoppingList>> updateActions = new ArrayList<>();

        final int minSize = Math.min(oldTextLineItems.size(), newTextLineItems.size());
        for (int i = 0; i < minSize; i++) {

            final TextLineItem oldTextLineItem = oldTextLineItems.get(i);
            final TextLineItemDraft newTextLineItem = newTextLineItems.get(i);

            if (newTextLineItem.getName() == null || newTextLineItem.getName().getLocales().isEmpty()) {
                /*
                checking the name of the oldTextLineItem is not needed, because it's required.
                with this check below, it's avoided bad request case like:

                "detailedErrorMessage": "actions -> name: Missing required value"
                */
                syncOptions.applyErrorCallback(new SyncException(
                        format("TextLineItemDraft at position '%d' of the ShoppingListDraft with key '%s' has no name "
                            + "set. Please make sure all text line items have names.", i, newShoppingList.getKey())),
                    oldShoppingList, newShoppingList, updateActions);

                return emptyList();
            }

            updateActions.addAll(buildTextLineItemUpdateActions(
                oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, syncOptions));
        }

        for (int i = minSize; i < oldTextLineItems.size(); i++) {
            updateActions.add(RemoveTextLineItem.of(oldTextLineItems.get(i).getId()));
        }

        for (int i = minSize; i < newTextLineItems.size(); i++) {
            if (hasQuantity(newTextLineItems.get(i))) {
                updateActions.add(AddTextLineItemWithAddedAt.of(newTextLineItems.get(i)));
            }
        }

        return updateActions;
    }

    /**
     * Compares all the fields of a {@link TextLineItem} and a {@link TextLineItemDraft} and returns a list of
     * {@link UpdateAction}&lt;{@link ShoppingList}&gt; as a result. If both the {@link TextLineItem} and
     * the {@link TextLineItemDraft} have identical fields, then no update action is needed and hence an empty
     * {@link List} is returned.
     *
     * @param oldShoppingList shopping list resource, whose line item should be updated.
     * @param newShoppingList new shopping list draft, which contains the line item to update.
     * @param oldTextLineItem     the text line item which should be updated.
     * @param newTextLineItem     the text line item draft where we get the new fields (i.e. quantity, custom fields).
     * @param syncOptions     responsible for supplying the sync options to the sync utility method. It is used for
     *                        triggering the error callback within the utility, in case of errors.
     * @return A list with the update actions or an empty list if the text line item fields are identical.
     */
    @Nonnull
    public static List<UpdateAction<ShoppingList>> buildTextLineItemUpdateActions(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList,
        @Nonnull final TextLineItem oldTextLineItem,
        @Nonnull final TextLineItemDraft newTextLineItem,
        @Nonnull final ShoppingListSyncOptions syncOptions) {

        final List<UpdateAction<ShoppingList>> updateActions = filterEmptyOptionals(
            buildChangeTextLineItemNameUpdateAction(oldTextLineItem, newTextLineItem),
            buildSetTextLineItemDescriptionUpdateAction(oldTextLineItem, newTextLineItem),
            buildChangeTextLineItemQuantityUpdateAction(oldTextLineItem, newTextLineItem)
        );

        updateActions.addAll(buildTextLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldTextLineItem, newTextLineItem, syncOptions));

        return updateActions;
    }

    /**
     * Compares the {@link LocalizedString} names of {@link TextLineItem} and a {@link TextLineItemDraft} and
     * returns an {@link Optional} of update action, which would contain the {@code "changeTextLineItemName"}
     * {@link UpdateAction}. If both the {@link TextLineItem} and the {@link TextLineItemDraft} have the same
     * {@code description} values, then no update action is needed and hence an empty optional will be returned.
     *
     * @param oldTextLineItem the text line item which should be updated.
     * @param newTextLineItem the text line item draft where we get the new name.
     * @return A filled optional with the update action or an empty optional if the names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ShoppingList>> buildChangeTextLineItemNameUpdateAction(
        @Nonnull final TextLineItem oldTextLineItem,
        @Nonnull final TextLineItemDraft newTextLineItem) {

        return buildUpdateAction(oldTextLineItem.getName(), newTextLineItem.getName(), () ->
            ChangeTextLineItemName.of(oldTextLineItem.getId(), newTextLineItem.getName()));
    }

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
            textLineItem -> TextLineItem.resourceTypeId(),
            t -> oldTextLineItem.getId(),
            syncOptions);
    }

    private TextLineItemUpdateActionUtils() {
    }
}
