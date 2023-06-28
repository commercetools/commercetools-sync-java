package com.commercetools.sync.sdk2.cartdiscounts.utils;

import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActionForReferences;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountChangeCartPredicateActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeIsActiveActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeNameActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeRequiresDiscountCodeActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeSortOrderActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeStackingModeActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeTargetActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeValueActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountSetDescriptionActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountSetValidFromActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountSetValidFromAndUntilActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountSetValidUntilActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.api.models.cart_discount.CartDiscountValueAbsolute;
import com.commercetools.api.models.cart_discount.CartDiscountValueAbsoluteDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueFixed;
import com.commercetools.api.models.cart_discount.CartDiscountValueFixedDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueGiftLineItem;
import com.commercetools.api.models.cart_discount.CartDiscountValueGiftLineItemDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueRelative;
import com.commercetools.api.models.cart_discount.CartDiscountValueRelativeDraft;
import com.commercetools.api.models.cart_discount.StackingMode;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

public final class CartDiscountUpdateActionUtils {

  /**
   * Compares the cart discount values of a {@link CartDiscount} and a {@link CartDiscountDraft} and
   * returns an {@link CartDiscountUpdateAction} as a result in an {@link java.util.Optional}. If
   * both the {@link CartDiscount} and the {@link CartDiscountDraft} have the same cart discount
   * value, then no update action is needed and hence an empty {@link java.util.Optional} is
   * returned.
   *
   * <p>Note: Order is not significant when comparing {@link CartDiscountValueAbsolute}s
   *
   * @param oldCartDiscount the cart discount which should be updated.
   * @param newCartDiscount the cart discount draft where we get the new cart discount value.
   * @return A filled optional with the update action or an empty optional if the cart discount
   *     values are identical.
   */
  @Nonnull
  public static Optional<CartDiscountUpdateAction> buildChangeValueUpdateAction(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount) {

    if (oldCartDiscount.getValue() instanceof CartDiscountValueAbsolute
        && newCartDiscount.getValue() instanceof CartDiscountValueAbsoluteDraft) {

      final CartDiscountValueAbsolute oldValue =
          ((CartDiscountValueAbsolute) oldCartDiscount.getValue());

      final CartDiscountValueAbsoluteDraft newValue =
          ((CartDiscountValueAbsoluteDraft) newCartDiscount.getValue());

      return buildChangeAbsoluteValueUpdateAction(oldValue, newValue);
    }

    if (oldCartDiscount.getValue() instanceof CartDiscountValueGiftLineItem
        && newCartDiscount.getValue() instanceof CartDiscountValueGiftLineItemDraft) {

      final CartDiscountValueGiftLineItem oldValue =
          (CartDiscountValueGiftLineItem) oldCartDiscount.getValue();
      final CartDiscountValueGiftLineItemDraft newValue =
          (CartDiscountValueGiftLineItemDraft) newCartDiscount.getValue();

      return buildChangeGiftLineItemValueUpdateAction(oldValue, newValue);
    }

    if (oldCartDiscount.getValue() instanceof CartDiscountValueRelative
        && newCartDiscount.getValue() instanceof CartDiscountValueRelativeDraft) {
      final CartDiscountValueRelative oldValue =
          (CartDiscountValueRelative) oldCartDiscount.getValue();
      final CartDiscountValueRelativeDraft newValue =
          (CartDiscountValueRelativeDraft) newCartDiscount.getValue();
      return buildChangeRelativeValueUpdateAction(oldValue, newValue);
    }

    if (oldCartDiscount.getValue() instanceof CartDiscountValueFixed
        && newCartDiscount.getValue() instanceof CartDiscountValueFixedDraft) {
      final CartDiscountValueFixed oldValue = (CartDiscountValueFixed) oldCartDiscount.getValue();
      final CartDiscountValueFixedDraft newValue =
          (CartDiscountValueFixedDraft) newCartDiscount.getValue();
      return buildChangeFixedValueUpdateAction(oldValue, newValue);
    }

    return buildUpdateAction(
        oldCartDiscount.getValue(),
        newCartDiscount.getValue(),
        () -> CartDiscountChangeValueActionBuilder.of().value(newCartDiscount.getValue()).build());
  }

  @NotNull
  private static Optional<CartDiscountUpdateAction> buildChangeFixedValueUpdateAction(
      final CartDiscountValueFixed oldValue, final CartDiscountValueFixedDraft newValue) {
    return oldValue.getMoney().equals(newValue.getMoney())
        ? Optional.empty()
        : Optional.of(CartDiscountChangeValueActionBuilder.of().value(newValue).build());
  }

  @NotNull
  private static Optional<CartDiscountUpdateAction> buildChangeRelativeValueUpdateAction(
      final CartDiscountValueRelative oldValue, final CartDiscountValueRelativeDraft newValue) {
    return oldValue.getPermyriad().equals(newValue.getPermyriad())
        ? Optional.empty()
        : Optional.of(CartDiscountChangeValueActionBuilder.of().value(newValue).build());
  }

  @NotNull
  private static Optional<CartDiscountUpdateAction> buildChangeGiftLineItemValueUpdateAction(
      final CartDiscountValueGiftLineItem oldValue,
      final CartDiscountValueGiftLineItemDraft newValue) {
    return Optional.ofNullable(
        buildActionIfDifferentProducts(oldValue, newValue)
            .orElse(
                buildActionIfDifferentProductVariantIds(oldValue, newValue)
                    .orElse(
                        buildActionIfDifferentSupplyChannels(oldValue, newValue)
                            .orElse(
                                buildActionIfDifferentDistributionChannels(oldValue, newValue)
                                    .orElse(null)))));
  }

  @Nonnull
  private static Optional<CartDiscountUpdateAction> buildActionIfDifferentProducts(
      @Nonnull final CartDiscountValueGiftLineItem oldValue,
      @Nonnull final CartDiscountValueGiftLineItemDraft newValue) {
    return buildUpdateActionForReferences(
        oldValue.getProduct(),
        newValue.getProduct(),
        () -> CartDiscountChangeValueActionBuilder.of().value(newValue).build());
  }

  @Nonnull
  private static Optional<CartDiscountUpdateAction> buildActionIfDifferentProductVariantIds(
      @Nonnull final CartDiscountValueGiftLineItem oldValue,
      @Nonnull final CartDiscountValueGiftLineItemDraft newValue) {
    return buildUpdateAction(
        oldValue.getVariantId(),
        newValue.getVariantId(),
        () -> CartDiscountChangeValueActionBuilder.of().value(newValue).build());
  }

  @Nonnull
  private static Optional<CartDiscountUpdateAction> buildActionIfDifferentSupplyChannels(
      @Nonnull final CartDiscountValueGiftLineItem oldValue,
      @Nonnull final CartDiscountValueGiftLineItemDraft newValue) {
    return buildUpdateActionForReferences(
        oldValue.getSupplyChannel(),
        newValue.getSupplyChannel(),
        () -> CartDiscountChangeValueActionBuilder.of().value(newValue).build());
  }

  @Nonnull
  private static Optional<CartDiscountUpdateAction> buildActionIfDifferentDistributionChannels(
      @Nonnull final CartDiscountValueGiftLineItem oldValue,
      @Nonnull final CartDiscountValueGiftLineItemDraft newValue) {
    return buildUpdateActionForReferences(
        oldValue.getDistributionChannel(),
        newValue.getDistributionChannel(),
        () -> CartDiscountChangeValueActionBuilder.of().value(newValue).build());
  }

  @Nonnull
  private static Optional<CartDiscountUpdateAction> buildChangeAbsoluteValueUpdateAction(
      @Nonnull final CartDiscountValueAbsolute oldValue,
      @Nonnull final CartDiscountValueAbsoluteDraft newValue) {

    if (newValue.getMoney() == null) {
      return Optional.of(CartDiscountChangeValueActionBuilder.of().value(newValue).build());
    }

    if (oldValue.getMoney().size() != newValue.getMoney().size()) {
      return Optional.of(CartDiscountChangeValueActionBuilder.of().value(newValue).build());
    }

    final boolean allOldValuesFoundInNewValues =
        oldValue.getMoney().stream()
            .allMatch(
                oldAmount ->
                    newValue.getMoney().stream()
                        .filter(
                            newAmount -> newAmount.getCurrency().equals(oldAmount.getCurrency()))
                        .anyMatch(newAmount -> newAmount.isEqualTo(oldAmount)));

    return allOldValuesFoundInNewValues
        ? empty()
        : Optional.of(CartDiscountChangeValueActionBuilder.of().value(newValue).build());
  }

  /**
   * Compares the cartPredicates of a {@link CartDiscount} and a {@link CartDiscountDraft} and
   * returns an {@link CartDiscountUpdateAction} as a result in an {@link java.util.Optional}. If
   * both the {@link CartDiscount} and the {@link CartDiscountDraft} have the same cartPredicate,
   * then no update action is needed and hence an empty {@link java.util.Optional} is returned.
   *
   * @param oldCartDiscount the cart discount which should be updated.
   * @param newCartDiscount the cart discount draft where we get the new cartPredicate.
   * @return A filled optional with the update action or an empty optional if the cartPredicates are
   *     identical.
   */
  @Nonnull
  public static Optional<CartDiscountUpdateAction> buildChangeCartPredicateUpdateAction(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount) {
    return buildUpdateAction(
        oldCartDiscount.getCartPredicate(),
        newCartDiscount.getCartPredicate(),
        () ->
            CartDiscountChangeCartPredicateActionBuilder.of()
                .cartPredicate(newCartDiscount.getCartPredicate())
                .build());
  }

  /**
   * Compares the cart discount target values of a {@link CartDiscount} and a {@link
   * CartDiscountDraft} and returns an {@link CartDiscountUpdateAction} as a result in an {@link
   * java.util.Optional}. If both the {@link CartDiscount} and the {@link CartDiscountDraft} have
   * the same cart discount target, then no update action is needed and hence an empty {@link
   * java.util.Optional} is returned.
   *
   * @param oldCartDiscount the cart discount which should be updated.
   * @param newCartDiscount the cart discount draft where we get the new cart discount target.
   * @return A filled optional with the update action or an empty optional if the cart discount
   *     targets are identical.
   */
  @Nonnull
  public static Optional<CartDiscountUpdateAction> buildChangeTargetUpdateAction(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount) {
    return buildUpdateAction(
        oldCartDiscount.getTarget(),
        newCartDiscount.getTarget(),
        () ->
            CartDiscountChangeTargetActionBuilder.of().target(newCartDiscount.getTarget()).build());
  }

  /**
   * Compares the {@link Boolean} isActive values of a {@link CartDiscount} and a {@link
   * CartDiscountDraft} and returns an {@link CartDiscountUpdateAction} as a result in an {@link
   * java.util.Optional}. If both the {@link CartDiscount} and the {@link CartDiscountDraft} have
   * the same 'isActive' value, then no update action is needed and hence an empty {@link
   * java.util.Optional} is returned.
   *
   * <p>Note: A {@code null} {@code isActive} value in the {@link CartDiscount} is treated as a
   * {@code true} value which is the default value of CTP.
   *
   * @param oldCartDiscount the cart discount which should be updated.
   * @param newCartDiscount the cart discount draft where we get the new isActive.
   * @return A filled optional with the update action or an empty optional if the isActive values
   *     are identical.
   */
  @Nonnull
  public static Optional<CartDiscountUpdateAction> buildChangeIsActiveUpdateAction(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount) {
    final Boolean isActive = ofNullable(newCartDiscount.getIsActive()).orElse(true);

    return buildUpdateAction(
        oldCartDiscount.getIsActive(),
        isActive,
        () -> CartDiscountChangeIsActiveActionBuilder.of().isActive(isActive).build());
  }

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} names of a {@link
   * CartDiscount} and a {@link CartDiscountDraft} and returns an {@link CartDiscountUpdateAction}
   * as a result in an {@link java.util.Optional}. If both the {@link CartDiscount} and the {@link
   * CartDiscountDraft} have the same name, then no update action is needed and hence an empty
   * {@link java.util.Optional} is returned.
   *
   * @param oldCartDiscount the cart discount which should be updated.
   * @param newCartDiscount the cart discount draft where we get the new name.
   * @return A filled optional with the update action or an empty optional if the names are
   *     identical.
   */
  @Nonnull
  public static Optional<CartDiscountUpdateAction> buildChangeNameUpdateAction(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount) {
    return buildUpdateAction(
        oldCartDiscount.getName(),
        newCartDiscount.getName(),
        () -> CartDiscountChangeNameActionBuilder.of().name(newCartDiscount.getName()).build());
  }

  /**
   * Compares the {@link com.commercetools.api.models.common.LocalizedString} descriptions of a
   * {@link CartDiscount} and a {@link CartDiscountDraft} and returns an {@link
   * CartDiscountUpdateAction} as a result in an {@link java.util.Optional}. If both the {@link
   * CartDiscount} and the {@link CartDiscountDraft} have the same description, then no update
   * action is needed and hence an empty {@link java.util.Optional} is returned.
   *
   * @param oldCartDiscount the cart discount which should be updated.
   * @param newCartDiscount the cart discount draft where we get the new description.
   * @return A filled optional with the update action or an empty optional if the descriptions are
   *     identical.
   */
  @Nonnull
  public static Optional<CartDiscountUpdateAction> buildSetDescriptionUpdateAction(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount) {
    return buildUpdateAction(
        oldCartDiscount.getDescription(),
        newCartDiscount.getDescription(),
        () ->
            CartDiscountSetDescriptionActionBuilder.of()
                .description(newCartDiscount.getDescription())
                .build());
  }

  /**
   * Compares the sortOrder values of a {@link CartDiscount} and a {@link CartDiscountDraft} and
   * returns an {@link CartDiscountUpdateAction} as a result in an {@link java.util.Optional}. If
   * both the {@link CartDiscount} and the {@link CartDiscountDraft} have the same sortOrder, then
   * no update action is needed and hence an empty {@link java.util.Optional} is returned.
   *
   * @param oldCartDiscount the cart discount which should be updated.
   * @param newCartDiscount the cart discount draft where we get the new sortOrder.
   * @return A filled optional with the update action or an empty optional if the sortOrders are
   *     identical.
   */
  @Nonnull
  public static Optional<CartDiscountUpdateAction> buildChangeSortOrderUpdateAction(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount) {
    return buildUpdateAction(
        oldCartDiscount.getSortOrder(),
        newCartDiscount.getSortOrder(),
        () ->
            CartDiscountChangeSortOrderActionBuilder.of()
                .sortOrder(newCartDiscount.getSortOrder())
                .build());
  }

  /**
   * Compares the {@link Boolean} requiresDiscountCode values of a {@link CartDiscount} and a {@link
   * CartDiscountDraft} and returns an {@link CartDiscountUpdateAction} as a result in an {@link
   * java.util.Optional}. If both the {@link CartDiscount} and the {@link CartDiscountDraft} have
   * the same requiresDiscountCode value, then no update action is needed and hence an empty {@link
   * java.util.Optional} is returned.
   *
   * <p>Note: A {@code null} {@code requiresDiscountCode} value in the {@link CartDiscount} is
   * treated as a {@code false} value which is the default value of CTP.
   *
   * @param oldCartDiscount the cart discount which should be updated.
   * @param newCartDiscount the cart discount draft where we get the new 'requiresDiscountCode'.
   * @return A filled optional with the update action or an empty optional if the
   *     'requiresDiscountCode' values are identical.
   */
  @Nonnull
  public static Optional<CartDiscountUpdateAction> buildChangeRequiresDiscountCodeUpdateAction(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount) {
    final Boolean requiresDiscountCode =
        ofNullable(newCartDiscount.getRequiresDiscountCode()).orElse(false);

    return buildUpdateAction(
        oldCartDiscount.getRequiresDiscountCode(),
        requiresDiscountCode,
        () ->
            CartDiscountChangeRequiresDiscountCodeActionBuilder.of()
                .requiresDiscountCode(requiresDiscountCode)
                .build());
  }

  /**
   * Compares the {@link java.time.ZonedDateTime} validFrom values of a {@link CartDiscount} and a
   * {@link CartDiscountDraft} and returns an {@link CartDiscountUpdateAction} as a result in an
   * {@link java.util.Optional}. If both the {@link CartDiscount} and the {@link CartDiscountDraft}
   * have the same validFrom, then no update action is needed and hence an empty {@link
   * java.util.Optional} is returned.
   *
   * @param oldCartDiscount the cart discount which should be updated.
   * @param newCartDiscount the cart discount draft where we get the new validFrom.
   * @return A filled optional with the update action or an empty optional if the validFrom values
   *     are identical.
   */
  @Nonnull
  public static Optional<CartDiscountUpdateAction> buildSetValidFromUpdateAction(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount) {

    return buildUpdateAction(
        oldCartDiscount.getValidFrom(),
        newCartDiscount.getValidFrom(),
        () ->
            CartDiscountSetValidFromActionBuilder.of()
                .validFrom(newCartDiscount.getValidFrom())
                .build());
  }

  /**
   * Compares the {@link java.time.ZonedDateTime} validUntil values of a {@link CartDiscount} and a
   * {@link CartDiscountDraft} and returns an {@link CartDiscountUpdateAction} as a result in an
   * {@link java.util.Optional}. If both the {@link CartDiscount} and the {@link CartDiscountDraft}
   * have the same validUntil, then no update action is needed and hence an empty {@link
   * java.util.Optional} is returned.
   *
   * @param oldCartDiscount the cart discount which should be updated.
   * @param newCartDiscount the cart discount draft where we get the new validUntil.
   * @return A filled optional with the update action or an empty optional if the validUntil values
   *     are identical.
   */
  @Nonnull
  public static Optional<CartDiscountUpdateAction> buildSetValidUntilUpdateAction(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount) {

    return buildUpdateAction(
        oldCartDiscount.getValidUntil(),
        newCartDiscount.getValidUntil(),
        () ->
            CartDiscountSetValidUntilActionBuilder.of()
                .validUntil(newCartDiscount.getValidUntil())
                .build());
  }

  /**
   * Compares the {@link java.time.ZonedDateTime} validFrom and {@link java.time.ZonedDateTime}
   * validUntil values of a {@link CartDiscount} and a {@link CartDiscountDraft} and returns an
   * {@link CartDiscountUpdateAction} as a result in an {@link java.util.Optional}. - If both the
   * {@link CartDiscount} and the {@link CartDiscountDraft} have different validFrom and same
   * validUntil values, then 'setValidFrom' update action returned. - If both the {@link
   * CartDiscount} and the {@link CartDiscountDraft} have the same validFrom and different
   * validUntil values, then 'setValidUntil' update action returned. - If both the {@link
   * CartDiscount} and the {@link CartDiscountDraft} have different validFrom and different
   * validUntil values, then 'setValidFromAndUntil' update action returned. - If both the {@link
   * CartDiscount} and the {@link CartDiscountDraft} have same validFrom and validUntil values, then
   * no update action is needed and hence an empty {@link java.util.Optional} is returned.
   *
   * @param oldCartDiscount the cart discount which should be updated.
   * @param newCartDiscount the cart discount draft where we get the new validFrom and validUntil.
   * @return A filled optional with the update action or an empty optional if the validFrom and
   *     validUntil values are identical.
   */
  @Nonnull
  public static Optional<CartDiscountUpdateAction> buildSetValidDatesUpdateAction(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount) {

    final Optional<CartDiscountUpdateAction> setValidFromUpdateAction =
        buildSetValidFromUpdateAction(oldCartDiscount, newCartDiscount);

    final Optional<CartDiscountUpdateAction> setValidUntilUpdateAction =
        buildSetValidUntilUpdateAction(oldCartDiscount, newCartDiscount);

    if (setValidFromUpdateAction.isPresent() && setValidUntilUpdateAction.isPresent()) {
      return Optional.of(
          CartDiscountSetValidFromAndUntilActionBuilder.of()
              .validFrom(newCartDiscount.getValidFrom())
              .validUntil(newCartDiscount.getValidUntil())
              .build());
    }

    return setValidFromUpdateAction.isPresent()
        ? setValidFromUpdateAction
        : setValidUntilUpdateAction;
  }

  /**
   * Compares the {@link StackingMode} stacking modes of a {@link CartDiscount} and a {@link
   * CartDiscountDraft} and returns an {@link CartDiscountUpdateAction} as a result in an {@link
   * java.util.Optional}. If both the {@link CartDiscount} and the {@link CartDiscountDraft} have
   * the same stacking mode, then no update action is needed and hence an empty {@link
   * java.util.Optional} is returned.
   *
   * <p>Note: A {@code null} {@code stackingMode} value in the {@link CartDiscount} is treated as a
   * {@link StackingMode#STACKING} value which is the default value of CTP.
   *
   * @param oldCartDiscount the cart discount which should be updated.
   * @param newCartDiscount the cart discount draft where we get the new stacking mode.
   * @return A filled optional with the update action or an empty optional if the stacking modes are
   *     identical.
   */
  @Nonnull
  public static Optional<CartDiscountUpdateAction> buildChangeStackingModeUpdateAction(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount) {
    final StackingMode stackingMode =
        ofNullable(newCartDiscount.getStackingMode()).orElse(StackingMode.STACKING);

    return buildUpdateAction(
        oldCartDiscount.getStackingMode(),
        stackingMode,
        () -> CartDiscountChangeStackingModeActionBuilder.of().stackingMode(stackingMode).build());
  }

  private CartDiscountUpdateActionUtils() {}
}
