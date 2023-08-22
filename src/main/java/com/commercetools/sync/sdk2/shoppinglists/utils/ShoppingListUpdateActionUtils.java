package com.commercetools.sync.sdk2.shoppinglists.utils;

import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActionForReferences;

import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListChangeNameActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListSetAnonymousIdActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetCustomerActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetDeleteDaysAfterLastModificationActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetDescriptionActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetSlugActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import java.util.Optional;
import javax.annotation.Nonnull;

public final class ShoppingListUpdateActionUtils {

  private ShoppingListUpdateActionUtils() {}

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} slugs of a {@link
   * ShoppingList} and a {@link ShoppingListDraft} and returns an {@link ShoppingListUpdateAction}
   * as a result in an {@link java.util.Optional}. If both the {@link ShoppingList} and the {@link
   * ShoppingListDraft} have the same slug, then no update action is needed and hence an empty
   * {@link java.util.Optional} is returned.
   *
   * @param oldShoppingList the shopping list which should be updated.
   * @param newShoppingList the shopping list draft where we get the new slug.
   * @return A filled optional with the update action or an empty optional if the slugs are
   *     identical.
   */
  @Nonnull
  public static Optional<ShoppingListUpdateAction> buildSetSlugUpdateAction(
      @Nonnull final ShoppingList oldShoppingList,
      @Nonnull final ShoppingListDraft newShoppingList) {

    return buildUpdateAction(
        oldShoppingList.getSlug(),
        newShoppingList.getSlug(),
        () -> ShoppingListSetSlugActionBuilder.of().slug(newShoppingList.getSlug()).build());
  }

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} names of a {@link
   * ShoppingList} and a {@link ShoppingListDraft} and returns an {@link ShoppingListUpdateAction}
   * as a result in an {@link java.util.Optional}. If both the {@link ShoppingList} and the {@link
   * ShoppingListDraft} have the same name, then no update action is needed and hence an empty
   * {@link java.util.Optional} is returned.
   *
   * @param oldShoppingList the shopping list which should be updated.
   * @param newShoppingList the shopping list draft where we get the new name.
   * @return A filled optional with the update action or an empty optional if the names are
   *     identical.
   */
  @Nonnull
  public static Optional<ShoppingListUpdateAction> buildChangeNameUpdateAction(
      @Nonnull final ShoppingList oldShoppingList,
      @Nonnull final ShoppingListDraft newShoppingList) {

    return buildUpdateAction(
        oldShoppingList.getName(),
        newShoppingList.getName(),
        () -> ShoppingListChangeNameActionBuilder.of().name(newShoppingList.getName()).build());
  }

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} descriptions of {@link
   * ShoppingList} and a {@link ShoppingListDraft} and returns an {@link ShoppingListUpdateAction}
   * as a result in an {@link java.util.Optional}. If both the {@link ShoppingList} and the {@link
   * ShoppingListDraft} have the same description, then no update action is needed and hence an
   * empty {@link java.util.Optional} is returned.
   *
   * @param oldShoppingList the shopping list which should be updated.
   * @param newShoppingList the shopping list draft where we get the new description.
   * @return A filled optional with the update action or an empty optional if the descriptions are
   *     identical.
   */
  @Nonnull
  public static Optional<ShoppingListUpdateAction> buildSetDescriptionUpdateAction(
      @Nonnull final ShoppingList oldShoppingList,
      @Nonnull final ShoppingListDraft newShoppingList) {

    return buildUpdateAction(
        oldShoppingList.getDescription(),
        newShoppingList.getDescription(),
        () ->
            ShoppingListSetDescriptionActionBuilder.of()
                .description(newShoppingList.getDescription())
                .build());
  }

  /**
   * Compares the customer references of a {@link ShoppingList} and a {@link ShoppingListDraft} and
   * returns an {@link ShoppingListUpdateAction} as a result in an {@link java.util.Optional}. If
   * both the {@link ShoppingList} and the {@link ShoppingListDraft} have the same customer, then no
   * update action is needed and hence an empty {@link java.util.Optional} is returned.
   *
   * @param oldShoppingList the shopping list which should be updated.
   * @param newShoppingList the shopping list draft which holds the new customer.
   * @return A filled optional with the update action or an empty optional if the customers are
   *     identical.
   */
  @Nonnull
  public static Optional<ShoppingListUpdateAction> buildSetCustomerUpdateAction(
      @Nonnull final ShoppingList oldShoppingList,
      @Nonnull final ShoppingListDraft newShoppingList) {

    return buildUpdateActionForReferences(
        oldShoppingList.getCustomer(),
        newShoppingList.getCustomer(),
        () ->
            ShoppingListSetCustomerActionBuilder.of()
                .customer(newShoppingList.getCustomer())
                .build());
  }

  /**
   * Compares the anonymousIds of {@link ShoppingList} and a {@link ShoppingListDraft} and returns
   * an {@link ShoppingListUpdateAction} as a result in an {@link java.util.Optional}. If both the
   * {@link ShoppingList} and the {@link ShoppingListDraft} have the same anonymousId, then no
   * update action is needed and hence an empty {@link java.util.Optional} is returned.
   *
   * @param oldShoppingList the shopping list which should be updated.
   * @param newShoppingList the shopping list draft which holds the new anonymousId.
   * @return A filled optional with the update action or an empty optional if the anonymousIds are
   *     identical.
   */
  @Nonnull
  public static Optional<ShoppingListUpdateAction> buildSetAnonymousIdUpdateAction(
      @Nonnull final ShoppingList oldShoppingList,
      @Nonnull final ShoppingListDraft newShoppingList) {

    return buildUpdateAction(
        oldShoppingList.getAnonymousId(),
        newShoppingList.getAnonymousId(),
        () ->
            ShoppingListSetAnonymousIdActionBuilder.of()
                .anonymousId(newShoppingList.getAnonymousId())
                .build());
  }

  /**
   * Compares the deleteDaysAfterLastModification of {@link ShoppingList} and a {@link
   * ShoppingListDraft} and returns an {@link ShoppingListUpdateAction} as a result in an {@link
   * java.util.Optional}. If both the {@link ShoppingList} and the {@link ShoppingListDraft} have
   * the same deleteDaysAfterLastModification, then no update action is needed and hence an empty
   * {@link java.util.Optional} is returned.
   *
   * @param oldShoppingList the shopping list which should be updated.
   * @param newShoppingList the shopping list draft which holds the new
   *     deleteDaysAfterLastModification.
   * @return A filled optional with the update action or an empty optional if the
   *     deleteDaysAfterLastModifications are identical.
   */
  @Nonnull
  public static Optional<ShoppingListUpdateAction>
      buildSetDeleteDaysAfterLastModificationUpdateAction(
          @Nonnull final ShoppingList oldShoppingList,
          @Nonnull final ShoppingListDraft newShoppingList) {

    return buildUpdateAction(
        oldShoppingList.getDeleteDaysAfterLastModification(),
        newShoppingList.getDeleteDaysAfterLastModification(),
        () ->
            ShoppingListSetDeleteDaysAfterLastModificationActionBuilder.of()
                .deleteDaysAfterLastModification(
                    newShoppingList.getDeleteDaysAfterLastModification())
                .build());
  }
}
