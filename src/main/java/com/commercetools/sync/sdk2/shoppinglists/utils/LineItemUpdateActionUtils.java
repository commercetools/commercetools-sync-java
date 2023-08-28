package com.commercetools.sync.sdk2.shoppinglists.utils;

import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.sdk2.commons.utils.OptionalUtils.filterEmptyOptionals;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListAddLineItemActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListChangeLineItemQuantityActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItem;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraft;
import com.commercetools.api.models.shopping_list.ShoppingListRemoveLineItemActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.sdk2.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.sdk2.shoppinglists.models.ShoppingListLineItemCustomTypeAdapter;
import com.commercetools.sync.sdk2.shoppinglists.models.ShoppingListLineItemDraftCustomTypeAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public final class LineItemUpdateActionUtils {

  /**
   * Compares a list of {@link ShoppingListLineItem}s with a list of {@link
   * ShoppingListLineItemDraft}s. The method takes in functions for building the required update
   * actions (AddLineItem, RemoveLineItem and 1-1 update actions on line items (e.g.
   * changeLineItemQuantity, setLineItemCustomType, etc..).
   *
   * <p>If the list of new {@link ShoppingListLineItemDraft}s is {@code null}, then remove actions
   * are built for every existing line item.
   *
   * @param oldShoppingList shopping list resource, whose line item should be updated.
   * @param newShoppingList new shopping list draft, which contains the line item to update.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return a list of line item update actions on the resource of shopping lists, if the list of
   *     line items are not identical. Otherwise, if the line items are identical, an empty list is
   *     returned.
   */
  @Nonnull
  public static List<ShoppingListUpdateAction> buildLineItemsUpdateActions(
      @Nonnull final ShoppingList oldShoppingList,
      @Nonnull final ShoppingListDraft newShoppingList,
      @Nonnull final ShoppingListSyncOptions syncOptions) {

    final boolean hasOldLineItems =
        oldShoppingList.getLineItems() != null && !oldShoppingList.getLineItems().isEmpty();
    final boolean hasNewLineItems =
        newShoppingList.getLineItems() != null
            && !newShoppingList.getLineItems().isEmpty()
            && newShoppingList.getLineItems().stream().anyMatch(Objects::nonNull);

    if (hasOldLineItems && !hasNewLineItems) {

      return oldShoppingList.getLineItems().stream()
          .map(
              shoppingListLineItem ->
                  ShoppingListRemoveLineItemActionBuilder.of()
                      .lineItemId(shoppingListLineItem.getId())
                      .build())
          .collect(toList());

    } else if (!hasOldLineItems) {

      if (!hasNewLineItems) {
        return emptyList();
      }

      return newShoppingList.getLineItems().stream()
          .filter(Objects::nonNull)
          .filter(LineItemUpdateActionUtils::hasQuantity)
          .map(
              shoppingListLineItemDraft ->
                  ShoppingListAddLineItemActionBuilder.of()
                      .addedAt(shoppingListLineItemDraft.getAddedAt())
                      .custom(shoppingListLineItemDraft.getCustom())
                      .sku(shoppingListLineItemDraft.getSku())
                      .productId(shoppingListLineItemDraft.getProductId())
                      .quantity(shoppingListLineItemDraft.getQuantity())
                      .variantId(shoppingListLineItemDraft.getVariantId())
                      .build())
          .collect(toList());
    }

    final List<ShoppingListLineItem> oldLineItems = oldShoppingList.getLineItems();
    final List<ShoppingListLineItemDraft> newlineItems =
        newShoppingList.getLineItems().stream().filter(Objects::nonNull).collect(toList());

    return buildUpdateActions(
        oldShoppingList, newShoppingList, oldLineItems, newlineItems, syncOptions);
  }

  private static boolean hasQuantity(@Nonnull final ShoppingListLineItemDraft lineItemDraft) {
    /*

     with this check, it's avoided bad request case like below:

     "code": "InvalidField",
     "message": "The value '0' is not valid for field 'quantity'. Quantity 0 is not allowed.",

    */
    return lineItemDraft.getQuantity() != null && lineItemDraft.getQuantity() > 0;
  }

  /**
   * The decisions in the calculating update actions are documented on the
   * `docs/adr/0002-shopping-lists-lineitem-and-textlineitem-update-actions.md`
   */
  @Nonnull
  private static List<ShoppingListUpdateAction> buildUpdateActions(
      @Nonnull final ShoppingList oldShoppingList,
      @Nonnull final ShoppingListDraft newShoppingList,
      @Nonnull final List<ShoppingListLineItem> oldLineItems,
      @Nonnull final List<ShoppingListLineItemDraft> newlineItems,
      @Nonnull final ShoppingListSyncOptions syncOptions) {

    final List<ShoppingListUpdateAction> updateActions = new ArrayList<>();

    final int minSize = Math.min(oldLineItems.size(), newlineItems.size());
    int indexOfFirstDifference = minSize;
    for (int i = 0; i < minSize; i++) {

      final ShoppingListLineItem oldLineItem = oldLineItems.get(i);
      final ShoppingListLineItemDraft newLineItem = newlineItems.get(i);

      if (oldLineItem.getVariant() == null
          || StringUtils.isBlank(oldLineItem.getVariant().getSku())) {

        syncOptions.applyErrorCallback(
            new SyncException(
                format(
                    "LineItem at position '%d' of the ShoppingList with key '%s' has no SKU set. "
                        + "Please make sure all line items have SKUs.",
                    i, oldShoppingList.getKey())),
            oldShoppingList,
            newShoppingList,
            updateActions);

        return emptyList();

      } else if (StringUtils.isBlank(newLineItem.getSku())) {

        syncOptions.applyErrorCallback(
            new SyncException(
                format(
                    "LineItemDraft at position '%d' of the ShoppingListDraft with key '%s' has no SKU set. "
                        + "Please make sure all line item drafts have SKUs.",
                    i, newShoppingList.getKey())),
            oldShoppingList,
            newShoppingList,
            updateActions);

        return emptyList();
      }

      if (oldLineItem.getVariant().getSku().equals(newLineItem.getSku())) {

        updateActions.addAll(
            buildLineItemUpdateActions(
                oldShoppingList, newShoppingList, oldLineItem, newLineItem, syncOptions));
      } else {
        // different sku means the order is different.
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
      updateActions.add(
          ShoppingListRemoveLineItemActionBuilder.of()
              .lineItemId(oldLineItems.get(i).getId())
              .build());
    }

    for (int i = indexOfFirstDifference; i < newlineItems.size(); i++) {
      final ShoppingListLineItemDraft lineItemDraft = newlineItems.get(i);
      if (hasQuantity(lineItemDraft)) {
        updateActions.add(
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(lineItemDraft.getAddedAt())
                .custom(lineItemDraft.getCustom())
                .sku(lineItemDraft.getSku())
                .productId(lineItemDraft.getProductId())
                .quantity(lineItemDraft.getQuantity())
                .variantId(lineItemDraft.getVariantId())
                .build());
      }
    }

    return updateActions;
  }

  /**
   * Compares all the fields of a {@link ShoppingListLineItem} and a {@link
   * ShoppingListLineItemDraft} and returns a list of {@link ShoppingListUpdateAction} as a result.
   * If both the {@link ShoppingListLineItem} and the {@link ShoppingListLineItemDraft} have
   * identical fields, then no update action is needed and hence an empty {@link java.util.List} is
   * returned.
   *
   * @param oldShoppingList shopping list resource, whose line item should be updated.
   * @param newShoppingList new shopping list draft, which contains the line item to update.
   * @param oldLineItem the line item which should be updated.
   * @param newLineItem the line item draft where we get the new fields (i.e. quantity, custom
   *     fields and types).
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return A list with the update actions or an empty list if the line item fields are identical.
   */
  @Nonnull
  public static List<ShoppingListUpdateAction> buildLineItemUpdateActions(
      @Nonnull final ShoppingList oldShoppingList,
      @Nonnull final ShoppingListDraft newShoppingList,
      @Nonnull final ShoppingListLineItem oldLineItem,
      @Nonnull final ShoppingListLineItemDraft newLineItem,
      @Nonnull final ShoppingListSyncOptions syncOptions) {

    final List<ShoppingListUpdateAction> updateActions =
        filterEmptyOptionals(buildChangeLineItemQuantityUpdateAction(oldLineItem, newLineItem));

    updateActions.addAll(
        buildLineItemCustomUpdateActions(
            oldShoppingList, newShoppingList, oldLineItem, newLineItem, syncOptions));

    return updateActions;
  }

  /**
   * Compares the {@code quantity} values of a {@link ShoppingListLineItem} and a {@link
   * ShoppingListLineItemDraft} and returns an {@link java.util.Optional} of update action, which
   * would contain the {@code "changeLineItemQuantity"} {@link ShoppingListUpdateAction}. If both
   * {@link ShoppingListLineItem} and {@link ShoppingListLineItemDraft} have the same {@code
   * quantity} values, then no update action is needed and empty optional will be returned.
   *
   * <p>Note: If {@code quantity} from the {@code newLineItem} is {@code null}, the new {@code
   * quantity} will be set to default value {@code 1L}. If {@code quantity} from the {@code
   * newLineItem} is {@code 0}, then it means removing the line item.
   *
   * @param oldLineItem the line item which should be updated.
   * @param newLineItem the line item draft where we get the new quantity.
   * @return A filled optional with the update action or an empty optional if the quantities are
   *     identical.
   */
  @Nonnull
  public static Optional<ShoppingListUpdateAction> buildChangeLineItemQuantityUpdateAction(
      @Nonnull final ShoppingListLineItem oldLineItem,
      @Nonnull final ShoppingListLineItemDraft newLineItem) {

    final Long newLineItemQuantity =
        newLineItem.getQuantity() == null ? NumberUtils.LONG_ONE : newLineItem.getQuantity();

    return buildUpdateAction(
        oldLineItem.getQuantity(),
        newLineItemQuantity,
        () ->
            ShoppingListChangeLineItemQuantityActionBuilder.of()
                .lineItemId(oldLineItem.getId())
                .quantity(newLineItemQuantity)
                .build());
  }

  /**
   * Compares the custom fields and custom types of a {@link ShoppingListLineItem} and a {@link
   * ShoppingListLineItemDraft} and returns a list of {@link ShoppingListUpdateAction} as a result.
   * If both the {@link ShoppingListLineItem} and the {@link ShoppingListLineItemDraft} have
   * identical custom fields and types, then no update action is needed and hence an empty {@link
   * java.util.List} is returned.
   *
   * @param oldShoppingList shopping list resource, whose line item should be updated.
   * @param newShoppingList new shopping list draft, which contains the line item to update.
   * @param oldLineItem the line item which should be updated.
   * @param newLineItem the line item draft where we get the new custom fields and types.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the error callback within the utility, in case of errors.
   * @return A list with the custom field/type update actions or an empty list if the custom
   *     fields/types are identical.
   */
  @Nonnull
  public static List<ShoppingListUpdateAction> buildLineItemCustomUpdateActions(
      @Nonnull final ShoppingList oldShoppingList,
      @Nonnull final ShoppingListDraft newShoppingList,
      @Nonnull final ShoppingListLineItem oldLineItem,
      @Nonnull final ShoppingListLineItemDraft newLineItem,
      @Nonnull final ShoppingListSyncOptions syncOptions) {

    return CustomUpdateActionUtils.buildCustomUpdateActions(
        newShoppingList,
        ShoppingListLineItemCustomTypeAdapter.of(oldLineItem),
        ShoppingListLineItemDraftCustomTypeAdapter.of(newLineItem),
        new LineItemCustomActionBuilder(),
        null, // not used by util.
        t -> oldLineItem.getId(),
        lineItem -> ResourceTypeId.LINE_ITEM.getJsonName(),
        t -> oldLineItem.getId(),
        syncOptions);
  }

  private LineItemUpdateActionUtils() {}
}
