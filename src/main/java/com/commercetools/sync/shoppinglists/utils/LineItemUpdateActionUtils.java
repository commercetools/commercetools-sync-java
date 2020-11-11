package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.commands.updateactions.AddLineItemWithSKU;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeLineItemQuantity;
import io.sphere.sdk.shoppinglists.commands.updateactions.RemoveLineItem;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static java.util.stream.Collectors.toList;

public final class LineItemUpdateActionUtils {

    /**
     * Compares a list of {@link LineItem}s with a list of {@link LineItemDraft}s.
     * The method takes in functions for building the required update actions (AddLineItem, RemoveLineItem and
     * 1-1 update actions on line items (e.g. changeLineItemQuantity, setLineItemCustomType, etc..).
     *
     * <p>If the list of new {@link LineItemDraft}s is {@code null}, then remove actions are built for every existing
     * line item.
     *
     * @param oldShoppingList shopping list resource, whose line item should be updated.
     * @param newShoppingList new shopping list draft, which contains the line item to update.
     * @param syncOptions     responsible for supplying the sync options to the sync utility method. It is used for
     *                        triggering the error callback within the utility, in case of errors.
     * @return a list of line item update actions on the resource of shopping lists, if the list of line items are not
     * identical. Otherwise, if the line items are identical, an empty list is returned.
     */
    @Nonnull
    public static List<UpdateAction<ShoppingList>> buildLineItemUpdateActions(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList,
        @Nonnull final ShoppingListSyncOptions syncOptions) {

        boolean hasOldLineItems = oldShoppingList.getLineItems() != null && !oldShoppingList.getLineItems().isEmpty();
        boolean hasNewLineItems = newShoppingList.getLineItems() != null && !newShoppingList.getLineItems().isEmpty()
            && newShoppingList.getLineItems().stream().anyMatch(Objects::nonNull);

        if (hasOldLineItems && !hasNewLineItems) {

            return oldShoppingList.getLineItems()
                                  .stream()
                                  .map(RemoveLineItem::of)
                                  .collect(toList());
        }

        if (!hasOldLineItems && hasNewLineItems) {

            return newShoppingList.getLineItems()
                                  .stream()
                                  .filter(Objects::nonNull)
                                  .map(LineItemUpdateActionUtils::createUpdateActionAddLineItemWithSKU)
                                  .collect(toList());
        }


        final List<LineItem> oldLineItems = oldShoppingList.getLineItems();
        final List<LineItemDraft> newlineItems = newShoppingList.getLineItems()
                                                                .stream()
                                                                .filter(Objects::nonNull)
                                                                .collect(toList());

        final List<UpdateAction<ShoppingList>> updateActions = new ArrayList<>();
        final int minSize = Math.min(oldLineItems.size(), newlineItems.size());
        int firstDiff = 0;
        for (int i = 0; i < minSize; i++) { // iterate considering the order.
            // todo: handle null checks.
            // different sku means the order is different.
            if (!oldLineItems.get(i).getVariant().getSku().equals(newlineItems.get(i).getSku())) {
                firstDiff = i;
                break;
            } else {
                updateActions.addAll(buildLineItemActions(
                    oldShoppingList, newShoppingList, oldLineItems.get(i), newlineItems.get(i), syncOptions));
            }
        }

        // for example:
        // old: li-1, li-2
        // new: li-1, li-3, li-2
        // firstDiff: 1
        // maxSize: 3
        // expected: remove from old li-2, add from draft li-3, li-2
        // using the first difference.
        for (int i = firstDiff; i < oldLineItems.size(); i++) {
            updateActions.add(RemoveLineItem.of(oldLineItems.get(i).getId()));
        }

        for (int i = firstDiff; i < newlineItems.size(); i++) {
            updateActions.add(createUpdateActionAddLineItemWithSKU(newlineItems.get(i)));
        }

        return updateActions;
    }

    @Nonnull
    private static UpdateAction<ShoppingList> createUpdateActionAddLineItemWithSKU(
        @Nonnull final LineItemDraft lineItemDraft) {

        return new AddLineItemWithSKU(lineItemDraft.getSku(),
            lineItemDraft.getQuantity(),
            lineItemDraft.getAddedAt(),
            lineItemDraft.getCustom());
    }

    /**
     * Compares all the fields of a {@link LineItem} and a {@link LineItemDraft} and returns a list of
     * {@link UpdateAction}&lt;{@link ShoppingList}&gt; as a result. If both the {@link LineItem} and
     * the {@link LineItemDraft} have identical fields, then no update action is needed and hence an empty {@link List}
     * is returned.
     *
     * @param oldShoppingList shopping list resource, whose line item should be updated.
     * @param newShoppingList new shopping list draft, which contains the line item to update.
     * @param oldLineItem     the line item which should be updated.
     * @param newLineItem     the line item draft where we get the new fields (i.e. quantity, custom fields and types).
     * @param syncOptions     responsible for supplying the sync options to the sync utility method. It is used for
     *                        triggering the error callback within the utility, in case of errors.
     * @return A list with the update actions or an empty list if the line item fields are identical.
     */
    @Nonnull
    public static List<UpdateAction<ShoppingList>> buildLineItemActions(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList,
        @Nonnull final LineItem oldLineItem,
        @Nonnull final LineItemDraft newLineItem,
        @Nonnull final ShoppingListSyncOptions syncOptions) {

        final List<UpdateAction<ShoppingList>> updateActions = filterEmptyOptionals(
            buildChangeLineItemQuantityUpdateAction(oldLineItem, newLineItem)
        );

        updateActions.addAll(
            buildLineItemCustomUpdateActions(oldShoppingList, newShoppingList, oldLineItem, newLineItem, syncOptions));

        return updateActions;
    }

    /**
     * Compares the {@code quantity} values of a {@link LineItem} and a {@link LineItemDraft}
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
     * Compares the custom fields and custom types of a {@link LineItem} and a {@link LineItemDraft} and returns a
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
     * identical.
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
