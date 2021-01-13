package com.commercetools.sync.cartdiscounts.utils;

import static com.commercetools.sync.cartdiscounts.utils.CartDiscountUpdateActionUtils.buildChangeCartPredicateUpdateAction;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountUpdateActionUtils.buildChangeIsActiveUpdateAction;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountUpdateActionUtils.buildChangeRequiresDiscountCodeUpdateAction;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountUpdateActionUtils.buildChangeSortOrderUpdateAction;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountUpdateActionUtils.buildChangeStackingModeUpdateAction;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountUpdateActionUtils.buildChangeTargetUpdateAction;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountUpdateActionUtils.buildChangeValueUpdateAction;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountUpdateActionUtils.buildSetDescriptionUpdateAction;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountUpdateActionUtils.buildSetValidDatesUpdateAction;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildPrimaryResourceCustomUpdateActions;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountCustomActionBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.commands.UpdateAction;
import java.util.List;
import javax.annotation.Nonnull;

public final class CartDiscountSyncUtils {

  private static final CartDiscountCustomActionBuilder cartDiscountCustomActionBuilder =
      new CartDiscountCustomActionBuilder();

  /**
   * Compares all the fields of a {@link CartDiscount} and a {@link CartDiscountDraft}. It returns a
   * {@link List} of {@link UpdateAction}&lt;{@link CartDiscount}&gt; as a result. If no update
   * action is needed, for example in case where both the {@link CartDiscount} and the {@link
   * CartDiscountDraft} have the same fields, an empty {@link List} is returned.
   *
   * @param oldCartDiscount the cart discount which should be updated.
   * @param newCartDiscount the cart discount draft where we get the new data.
   * @param syncOptions the sync options wrapper which contains options related to the sync process
   *     supplied by the user. For example, custom callbacks to call in case of warnings or errors
   *     occurring on the build update action process. And other options (See {@link
   *     CartDiscountSyncOptions} for more info.
   * @return A list of cart discount specific update actions.
   */
  @Nonnull
  public static List<UpdateAction<CartDiscount>> buildActions(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount,
      @Nonnull final CartDiscountSyncOptions syncOptions) {

    final List<UpdateAction<CartDiscount>> updateActions =
        filterEmptyOptionals(
            buildChangeValueUpdateAction(oldCartDiscount, newCartDiscount),
            buildChangeCartPredicateUpdateAction(oldCartDiscount, newCartDiscount),
            buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscount),
            buildChangeIsActiveUpdateAction(oldCartDiscount, newCartDiscount),
            buildChangeNameUpdateAction(oldCartDiscount, newCartDiscount),
            buildSetDescriptionUpdateAction(oldCartDiscount, newCartDiscount),
            buildChangeSortOrderUpdateAction(oldCartDiscount, newCartDiscount),
            buildChangeRequiresDiscountCodeUpdateAction(oldCartDiscount, newCartDiscount),
            buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscount),
            buildChangeStackingModeUpdateAction(oldCartDiscount, newCartDiscount));

    final List<UpdateAction<CartDiscount>> cartDiscountCustomUpdateActions =
        buildPrimaryResourceCustomUpdateActions(
            oldCartDiscount, newCartDiscount, cartDiscountCustomActionBuilder, syncOptions);

    updateActions.addAll(cartDiscountCustomUpdateActions);

    return updateActions;
  }

  private CartDiscountSyncUtils() {}
}
