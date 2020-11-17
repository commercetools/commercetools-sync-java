package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.commands.updateactions.AddLineItemWithSku;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeLineItemQuantity;
import io.sphere.sdk.shoppinglists.commands.updateactions.RemoveLineItem;
import org.apache.commons.lang3.StringUtils;
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
     *         identical. Otherwise, if the line items are identical, an empty list is returned.
     */
    @Nonnull
    public static List<UpdateAction<ShoppingList>> buildLineItemsUpdateActions(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList,
        @Nonnull final ShoppingListSyncOptions syncOptions) {

        final boolean hasOldLineItems = oldShoppingList.getLineItems() != null
            && !oldShoppingList.getLineItems().isEmpty();
        final boolean hasNewLineItems = newShoppingList.getLineItems() != null
            && !newShoppingList.getLineItems().isEmpty()
            && newShoppingList.getLineItems().stream().anyMatch(Objects::nonNull);

        if (hasOldLineItems && !hasNewLineItems) {

            return oldShoppingList.getLineItems()
                                  .stream()
                                  .map(RemoveLineItem::of)
                                  .collect(toList());

        } else if (!hasOldLineItems) {

            if (!hasNewLineItems) {
                return emptyList();
            }

            return newShoppingList.getLineItems()
                                  .stream()
                                  .filter(Objects::nonNull)
                                  .map(AddLineItemWithSku::of)
                                  .collect(toList());
        }

        final List<LineItem> oldLineItems = oldShoppingList.getLineItems();
        final List<LineItemDraft> newlineItems = newShoppingList.getLineItems()
                                                                .stream()
                                                                .filter(Objects::nonNull)
                                                                .collect(toList());

        return buildUpdateActions(oldShoppingList, newShoppingList, oldLineItems, newlineItems, syncOptions);
    }


    /**
     * The decisions in the calculating update actions are documented on the
     * `docs/adr/0002-shopping-lists-lineitem-and-textlineitem-update-actions.md`
     */
    @Nonnull
    private static List<UpdateAction<ShoppingList>> buildUpdateActions(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList,
        @Nonnull final List<LineItem> oldLineItems,
        @Nonnull final List<LineItemDraft> newlineItems,
        @Nonnull final ShoppingListSyncOptions syncOptions) {

        final List<UpdateAction<ShoppingList>> updateActions = new ArrayList<>();

        final int minSize = Math.min(oldLineItems.size(), newlineItems.size());
        int indexOfFirstDifference = minSize;
        for (int i = 0; i < minSize; i++) {

            final LineItem oldLineItem = oldLineItems.get(i);
            final LineItemDraft newLineItem = newlineItems.get(i);

            if (oldLineItem.getVariant() == null || StringUtils.isBlank(oldLineItem.getVariant().getSku())) {

                throw new IllegalArgumentException(
                    format("LineItem at position '%d' of the ShoppingList with key '%s' has no SKU set. "
                        + "Please make sure all line items have SKUs", i, oldShoppingList.getKey()));

            } else if (StringUtils.isBlank(newLineItem.getSku())) {

                throw new IllegalArgumentException(
                    format("LineItemDraft at position '%d' of the ShoppingListDraft with key '%s' has no SKU set. "
                        + "Please make sure all line items have SKUs", i, newShoppingList.getKey()));

            }

            if (oldLineItem.getVariant().getSku().equals(newLineItem.getSku())
                && hasIdenticalAddedAtValues(oldLineItem, newLineItem)) {

                updateActions.addAll(buildLineItemUpdateActions(
                    oldShoppingList, newShoppingList, oldLineItem, newLineItem, syncOptions));
            } else {
                // different sku or addedAt means the order is different.
                // To be able to ensure the order, we need to remove and add this line item back
                // with the up to date values.
                indexOfFirstDifference = i;
                break;
            }
        }

        // for example:
        // old: li-1, li-2
        // new: li-1, li-3, li-2
        // indexOfFirstDifference: 1 (li-2 vs li-3)
        // expected: remove from old li-2, add from draft li-3, li-2 starting from the index.
        for (int i = indexOfFirstDifference; i < oldLineItems.size(); i++) {
            updateActions.add(RemoveLineItem.of(oldLineItems.get(i).getId()));
        }

        for (int i = indexOfFirstDifference; i < newlineItems.size(); i++) {
            updateActions.add(AddLineItemWithSku.of(newlineItems.get(i)));
        }

        return updateActions;
    }

    private static boolean hasIdenticalAddedAtValues(
        @Nonnull final LineItem oldLineItem,
        @Nonnull final LineItemDraft newLineItem) {

        if (newLineItem.getAddedAt() == null) {
            return true; // omit, if not set in draft.
        }

        return oldLineItem.getAddedAt().equals(newLineItem.getAddedAt());
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
    public static List<UpdateAction<ShoppingList>> buildLineItemUpdateActions(
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
     * <p>Note: If {@code quantity} from the {@code newLineItem} is {@code null}, the new {@code quantity}
     * will be set to default value {@code 1L}.  If {@code quantity} from the {@code newLineItem} is {@code 0}, then it
     * means removing the line item.
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
            null, // not used by util.
            t -> oldLineItem.getId(),
            lineItem -> LineItem.resourceTypeId(),
            t -> oldLineItem.getId(),
            syncOptions);
    }

    private LineItemUpdateActionUtils() {
    }
}
