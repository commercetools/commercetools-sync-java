package com.commercetools.sync.cartdiscounts.utils;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildPrimaryResourceCustomUpdateActions;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;

import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountCustomActionBuilder;
import com.commercetools.sync.cartdiscounts.models.CartDiscountCustomTypeAdapter;
import com.commercetools.sync.cartdiscounts.models.CartDiscountDraftCustomTypeAdapter;
import java.util.List;
import javax.annotation.Nonnull;

public final class CartDiscountSyncUtils {

  private static final CartDiscountCustomActionBuilder cartDiscountCustomActionBuilder =
      new CartDiscountCustomActionBuilder();

  /**
   * Compares all the fields of a {@link CartDiscount} and a {@link CartDiscountDraft}. It returns a
   * {@link java.util.List} of {@link CartDiscountUpdateAction} as a result. If no update action is
   * needed, for example in case where both the {@link CartDiscount} and the {@link
   * CartDiscountDraft} have the same fields, an empty {@link java.util.List} is returned.
   *
   * @param oldCartDiscount the cart discount which should be updated.
   * @param newCartDiscount the cart discount draft where we get the new data.
   * @param syncOptions the sync options wrapper which contains options related to the sync process
   *     supplied by the user. For example, custom callbacks to call in case of warnings or errors
   *     occurring on the build update action process. And other options (See {@link
   *     com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions} for more info.
   * @return A list of cart discount specific update actions.
   */
  @Nonnull
  public static List<CartDiscountUpdateAction> buildActions(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount,
      @Nonnull final CartDiscountSyncOptions syncOptions) {

    final List<CartDiscountUpdateAction> updateActions =
        filterEmptyOptionals(
            CartDiscountUpdateActionUtils.buildChangeValueUpdateAction(
                oldCartDiscount, newCartDiscount),
            CartDiscountUpdateActionUtils.buildChangeCartPredicateUpdateAction(
                oldCartDiscount, newCartDiscount),
            CartDiscountUpdateActionUtils.buildChangeTargetUpdateAction(
                oldCartDiscount, newCartDiscount),
            CartDiscountUpdateActionUtils.buildChangeIsActiveUpdateAction(
                oldCartDiscount, newCartDiscount),
            CartDiscountUpdateActionUtils.buildChangeNameUpdateAction(
                oldCartDiscount, newCartDiscount),
            CartDiscountUpdateActionUtils.buildSetDescriptionUpdateAction(
                oldCartDiscount, newCartDiscount),
            CartDiscountUpdateActionUtils.buildChangeSortOrderUpdateAction(
                oldCartDiscount, newCartDiscount),
            CartDiscountUpdateActionUtils.buildChangeRequiresDiscountCodeUpdateAction(
                oldCartDiscount, newCartDiscount),
            CartDiscountUpdateActionUtils.buildSetValidDatesUpdateAction(
                oldCartDiscount, newCartDiscount),
            CartDiscountUpdateActionUtils.buildChangeStackingModeUpdateAction(
                oldCartDiscount, newCartDiscount));

    final List<CartDiscountUpdateAction> cartDiscountCustomUpdateActions =
        buildPrimaryResourceCustomUpdateActions(
            CartDiscountCustomTypeAdapter.of(oldCartDiscount),
            CartDiscountDraftCustomTypeAdapter.of(newCartDiscount),
            cartDiscountCustomActionBuilder,
            syncOptions);

    updateActions.addAll(cartDiscountCustomUpdateActions);

    return updateActions;
  }

  private CartDiscountSyncUtils() {}
}
