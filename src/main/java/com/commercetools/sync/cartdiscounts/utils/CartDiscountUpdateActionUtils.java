package com.commercetools.sync.cartdiscounts.utils;

import io.sphere.sdk.cartdiscounts.AbsoluteCartDiscountValue;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.GiftLineItemCartDiscountValue;
import io.sphere.sdk.cartdiscounts.StackingMode;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeCartPredicate;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeIsActive;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeName;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeRequiresDiscountCode;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeSortOrder;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeStackingMode;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeTarget;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeValue;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetDescription;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetValidFrom;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetValidFromAndUntil;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetValidUntil;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActionForReferences;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public final class CartDiscountUpdateActionUtils {

    /**
     * Compares the cart discount values of a {@link CartDiscount} and a {@link CartDiscountDraft}
     * and returns an {@link UpdateAction}&lt;{@link CartDiscount}&gt; as a result in an {@link Optional}.
     * If both the {@link CartDiscount} and the {@link CartDiscountDraft} have the same cart discount value,
     * then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * <p>Note: Order is not significant when comparing {@link AbsoluteCartDiscountValue}s
     *
     * @param oldCartDiscount the cart discount which should be updated.
     * @param newCartDiscount the cart discount draft where we get the new cart discount value.
     * @return A filled optional with the update action or an empty optional if the cart discount values are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<CartDiscount>> buildChangeValueUpdateAction(
            @Nonnull final CartDiscount oldCartDiscount,
            @Nonnull final CartDiscountDraft newCartDiscount) {

        if (oldCartDiscount.getValue() instanceof AbsoluteCartDiscountValue
                && newCartDiscount.getValue() instanceof AbsoluteCartDiscountValue) {

            final AbsoluteCartDiscountValue oldValue =
                ((AbsoluteCartDiscountValue) oldCartDiscount.getValue());

            final AbsoluteCartDiscountValue newValue =
                ((AbsoluteCartDiscountValue) newCartDiscount.getValue());

            return buildChangeAbsoluteValueUpdateAction(oldValue, newValue);
        }

        if (oldCartDiscount.getValue() instanceof GiftLineItemCartDiscountValue
                && newCartDiscount.getValue() instanceof GiftLineItemCartDiscountValue) {

            final GiftLineItemCartDiscountValue oldValue = (GiftLineItemCartDiscountValue) oldCartDiscount.getValue();
            final GiftLineItemCartDiscountValue newValue = (GiftLineItemCartDiscountValue) newCartDiscount.getValue();

            return Optional.ofNullable(
                buildActionIfDifferentProducts(oldValue, newValue)
                    .orElse(buildActionIfDifferentProductVariantIds(oldValue, newValue)
                        .orElse(buildActionIfDifferentSupplyChannels(oldValue, newValue)
                            .orElse(buildActionIfDifferentDistributionChannels(oldValue, newValue)
                                .orElse(null)))));
        }

        return buildUpdateAction(oldCartDiscount.getValue(), newCartDiscount.getValue(),
            () -> ChangeValue.of(newCartDiscount.getValue()));
    }

    @Nonnull
    private static Optional<ChangeValue> buildActionIfDifferentProducts(
            @Nonnull final GiftLineItemCartDiscountValue oldValue,
            @Nonnull final GiftLineItemCartDiscountValue newValue) {
        return buildUpdateActionForReferences(oldValue.getProduct(), newValue.getProduct(),
            () -> ChangeValue.of(newValue));
    }

    @Nonnull
    private static Optional<ChangeValue> buildActionIfDifferentProductVariantIds(
            @Nonnull final GiftLineItemCartDiscountValue oldValue,
            @Nonnull final GiftLineItemCartDiscountValue newValue) {
        return buildUpdateAction(oldValue.getVariantId(), newValue.getVariantId(),
            () -> ChangeValue.of(newValue));
    }

    @Nonnull
    private static Optional<ChangeValue> buildActionIfDifferentSupplyChannels(
            @Nonnull final GiftLineItemCartDiscountValue oldValue,
            @Nonnull final GiftLineItemCartDiscountValue newValue) {
        return buildUpdateActionForReferences(oldValue.getSupplyChannel(), newValue.getSupplyChannel(),
            () -> ChangeValue.of(newValue));
    }

    @Nonnull
    private static Optional<ChangeValue> buildActionIfDifferentDistributionChannels(
            @Nonnull final GiftLineItemCartDiscountValue oldValue,
            @Nonnull final GiftLineItemCartDiscountValue newValue) {
        return buildUpdateActionForReferences(oldValue.getDistributionChannel(), newValue.getDistributionChannel(),
            () -> ChangeValue.of(newValue));
    }

    @Nonnull
    private static Optional<UpdateAction<CartDiscount>> buildChangeAbsoluteValueUpdateAction(
            @Nonnull final AbsoluteCartDiscountValue oldValue,
            @Nonnull final AbsoluteCartDiscountValue newValue) {

        if (newValue.getMoney() == null) {
            return Optional.of(ChangeValue.of(newValue));
        }

        if (oldValue.getMoney().size() != newValue.getMoney().size()) {
            return Optional.of(ChangeValue.of(newValue));
        }

        final boolean allOldValuesFoundInNewValues = oldValue.getMoney().stream().allMatch(oldAmount -> 
                newValue.getMoney().stream()
                        .filter(newAmount -> newAmount.getCurrency().equals(oldAmount.getCurrency()))
                        .anyMatch(newAmount -> newAmount.isEqualTo(oldAmount)));

        return allOldValuesFoundInNewValues ? empty() : Optional.of(ChangeValue.of(newValue));
    }

    /**
     * Compares the cartPredicates of a {@link CartDiscount} and a {@link CartDiscountDraft}
     * and returns an {@link UpdateAction}&lt;{@link CartDiscount}&gt; as a result in an {@link Optional}.
     * If both the {@link CartDiscount} and the {@link CartDiscountDraft} have the same cartPredicate,
     * then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldCartDiscount the cart discount which should be updated.
     * @param newCartDiscount the cart discount draft where we get the new cartPredicate.
     * @return A filled optional with the update action or an empty optional if the cartPredicates are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<CartDiscount>> buildChangeCartPredicateUpdateAction(
        @Nonnull final CartDiscount oldCartDiscount,
        @Nonnull final CartDiscountDraft newCartDiscount) {
        return buildUpdateAction(oldCartDiscount.getCartPredicate(), newCartDiscount.getCartPredicate(),
            () -> ChangeCartPredicate.of(newCartDiscount.getCartPredicate()));
    }

    /**
     * Compares the cart discount target values of a {@link CartDiscount} and a {@link CartDiscountDraft}
     * and returns an {@link UpdateAction}&lt;{@link CartDiscount}&gt; as a result in an {@link Optional}.
     * If both the {@link CartDiscount} and the {@link CartDiscountDraft} have the same cart discount target,
     * then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldCartDiscount the cart discount which should be updated.
     * @param newCartDiscount the cart discount draft where we get the new cart discount target.
     * @return A filled optional with the update action or an empty optional if the cart discount targets are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<CartDiscount>> buildChangeTargetUpdateAction(
        @Nonnull final CartDiscount oldCartDiscount,
        @Nonnull final CartDiscountDraft newCartDiscount) {
        return buildUpdateAction(oldCartDiscount.getTarget(), newCartDiscount.getTarget(),
            () -> ChangeTarget.of(newCartDiscount.getTarget()));
    }

    /**
     * Compares the {@link Boolean} isActive values of a {@link CartDiscount} and a {@link CartDiscountDraft}
     * and returns an {@link UpdateAction}&lt;{@link CartDiscount}&gt; as a result in an {@link Optional}.
     * If both the {@link CartDiscount} and the {@link CartDiscountDraft} have the same 'isActive' value,
     * then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * <p>Note: A {@code null} {@code isActive} value in the {@link CartDiscount} is treated as a
     * {@code true} value which is the default value of CTP.
     *
     * @param oldCartDiscount the cart discount which should be updated.
     * @param newCartDiscount the cart discount draft where we get the new isActive.
     * @return A filled optional with the update action or an empty optional if the isActive values are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<CartDiscount>> buildChangeIsActiveUpdateAction(
        @Nonnull final CartDiscount oldCartDiscount,
        @Nonnull final CartDiscountDraft newCartDiscount) {
        final Boolean isActive = ofNullable(newCartDiscount.isActive()).orElse(true);

        return buildUpdateAction(oldCartDiscount.isActive(), isActive, () -> ChangeIsActive.of(isActive));
    }

    /**
     * Compares the {@link LocalizedString} names of a {@link CartDiscount} and a {@link CartDiscountDraft}
     * and returns an {@link UpdateAction}&lt;{@link CartDiscount}&gt; as a result in an {@link Optional}.
     * If both the {@link CartDiscount} and the {@link CartDiscountDraft} have the same name,
     * then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldCartDiscount the cart discount which should be updated.
     * @param newCartDiscount the cart discount draft where we get the new name.
     * @return A filled optional with the update action or an empty optional if the names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<CartDiscount>> buildChangeNameUpdateAction(
        @Nonnull final CartDiscount oldCartDiscount,
        @Nonnull final CartDiscountDraft newCartDiscount) {
        return buildUpdateAction(oldCartDiscount.getName(), newCartDiscount.getName(),
            () -> ChangeName.of(newCartDiscount.getName()));
    }

    /**
     * Compares the {@link LocalizedString} descriptions of a {@link CartDiscount} and a {@link CartDiscountDraft} and
     * returns an {@link UpdateAction}&lt;{@link CartDiscount}&gt; as a result in an {@link Optional}. If both the
     * {@link CartDiscount} and the {@link CartDiscountDraft} have the same description,
     * then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldCartDiscount the cart discount which should be updated.
     * @param newCartDiscount the cart discount draft where we get the new description.
     * @return A filled optional with the update action or an empty optional if the descriptions are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<CartDiscount>> buildSetDescriptionUpdateAction(
        @Nonnull final CartDiscount oldCartDiscount,
        @Nonnull final CartDiscountDraft newCartDiscount) {
        return buildUpdateAction(oldCartDiscount.getDescription(), newCartDiscount.getDescription(),
            () -> SetDescription.of(newCartDiscount.getDescription()));
    }

    /**
     * Compares the sortOrder values of a {@link CartDiscount} and a {@link CartDiscountDraft}
     * and returns an {@link UpdateAction}&lt;{@link CartDiscount}&gt; as a result in an {@link Optional}.
     * If both the {@link CartDiscount} and the {@link CartDiscountDraft} have the same sortOrder,
     * then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldCartDiscount the cart discount which should be updated.
     * @param newCartDiscount the cart discount draft where we get the new sortOrder.
     * @return A filled optional with the update action or an empty optional if the sortOrders are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<CartDiscount>> buildChangeSortOrderUpdateAction(
        @Nonnull final CartDiscount oldCartDiscount,
        @Nonnull final CartDiscountDraft newCartDiscount) {
        return buildUpdateAction(oldCartDiscount.getSortOrder(), newCartDiscount.getSortOrder(),
            () -> ChangeSortOrder.of(newCartDiscount.getSortOrder()));
    }

    /**
     * Compares the {@link Boolean} requiresDiscountCode values of a {@link CartDiscount}
     * and a {@link CartDiscountDraft} and returns an {@link UpdateAction}&lt;{@link CartDiscount}&gt; as a result
     * in an {@link Optional}. If both the {@link CartDiscount} and the {@link CartDiscountDraft} have the same
     * requiresDiscountCode value, then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * <p>Note: A {@code null} {@code requiresDiscountCode} value in the {@link CartDiscount} is treated as a
     * {@code false} value which is the default value of CTP.
     *
     * @param oldCartDiscount the cart discount which should be updated.
     * @param newCartDiscount the cart discount draft where we get the new 'requiresDiscountCode'.
     * @return A filled optional with the update action or an empty optional if the 'requiresDiscountCode' values
     *         are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<CartDiscount>> buildChangeRequiresDiscountCodeUpdateAction(
        @Nonnull final CartDiscount oldCartDiscount,
        @Nonnull final CartDiscountDraft newCartDiscount) {
        final Boolean requiresDiscountCode = ofNullable(newCartDiscount.isRequiresDiscountCode()).orElse(false);

        return buildUpdateAction(oldCartDiscount.isRequiringDiscountCode(), requiresDiscountCode,
            () -> ChangeRequiresDiscountCode.of(requiresDiscountCode));
    }

    /**
     * Compares the {@link ZonedDateTime} validFrom values of a {@link CartDiscount} and a {@link CartDiscountDraft}
     * and returns an {@link UpdateAction}&lt;{@link CartDiscount}&gt; as a result in an {@link Optional}.
     * If both the {@link CartDiscount} and the {@link CartDiscountDraft} have the same validFrom,
     * then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldCartDiscount the cart discount which should be updated.
     * @param newCartDiscount the cart discount draft where we get the new validFrom.
     * @return A filled optional with the update action or an empty optional if the validFrom values are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<CartDiscount>> buildSetValidFromUpdateAction(
        @Nonnull final CartDiscount oldCartDiscount,
        @Nonnull final CartDiscountDraft newCartDiscount) {

        return buildUpdateAction(oldCartDiscount.getValidFrom(), newCartDiscount.getValidFrom(),
            () -> SetValidFrom.of(newCartDiscount.getValidFrom()));
    }

    /**
     * Compares the {@link ZonedDateTime} validUntil values of a {@link CartDiscount} and a {@link CartDiscountDraft}
     * and returns an {@link UpdateAction}&lt;{@link CartDiscount}&gt; as a result in an {@link Optional}.
     * If both the {@link CartDiscount} and the {@link CartDiscountDraft} have the same validUntil,
     * then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldCartDiscount the cart discount which should be updated.
     * @param newCartDiscount the cart discount draft where we get the new validUntil.
     * @return A filled optional with the update action or an empty optional if the validUntil values are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<CartDiscount>> buildSetValidUntilUpdateAction(
        @Nonnull final CartDiscount oldCartDiscount,
        @Nonnull final CartDiscountDraft newCartDiscount) {

        return buildUpdateAction(oldCartDiscount.getValidUntil(), newCartDiscount.getValidUntil(),
            () -> SetValidUntil.of(newCartDiscount.getValidUntil()));
    }

    /**
     * Compares the {@link ZonedDateTime} validFrom and {@link ZonedDateTime} validUntil values
     * of a {@link CartDiscount} and a {@link CartDiscountDraft}
     * and returns an {@link UpdateAction}&lt;{@link CartDiscount}&gt; as a result in an {@link Optional}.
     * - If both the {@link CartDiscount} and the {@link CartDiscountDraft} have different validFrom
     * and same validUntil values, then 'setValidFrom' update action returned.
     * - If both the {@link CartDiscount} and the {@link CartDiscountDraft} have the same validFrom
     * and different validUntil values, then 'setValidUntil' update action returned.
     * - If both the {@link CartDiscount} and the {@link CartDiscountDraft} have different validFrom
     * and different validUntil values, then 'setValidFromAndUntil' update action returned.
     * - If both the {@link CartDiscount} and the {@link CartDiscountDraft} have same validFrom and validUntil values,
     * then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldCartDiscount the cart discount which should be updated.
     * @param newCartDiscount the cart discount draft where we get the new validFrom and validUntil.
     * @return A filled optional with the update action or an empty optional if the validFrom and validUntil
     *         values are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<CartDiscount>> buildSetValidDatesUpdateAction(
        @Nonnull final CartDiscount oldCartDiscount,
        @Nonnull final CartDiscountDraft newCartDiscount) {

        final Optional<UpdateAction<CartDiscount>> setValidFromUpdateAction =
            buildSetValidFromUpdateAction(oldCartDiscount, newCartDiscount);

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
            buildSetValidUntilUpdateAction(oldCartDiscount, newCartDiscount);

        if (setValidFromUpdateAction.isPresent() && setValidUntilUpdateAction.isPresent()) {
            return Optional.of(
                SetValidFromAndUntil.of(newCartDiscount.getValidFrom(), newCartDiscount.getValidUntil()));
        }

        return setValidFromUpdateAction.isPresent() ? setValidFromUpdateAction : setValidUntilUpdateAction;
    }

    /**
     * Compares the {@link StackingMode} stacking modes of a {@link CartDiscount} and a {@link CartDiscountDraft}
     * and returns an {@link UpdateAction}&lt;{@link CartDiscount}&gt; as a result in an {@link Optional}.
     * If both the {@link CartDiscount} and the {@link CartDiscountDraft} have the same stacking mode,
     * then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * <p>Note: A {@code null} {@code stackingMode} value in the {@link CartDiscount} is treated as a
     * {@link StackingMode#STACKING} value which is the default value of CTP.
     *
     * @param oldCartDiscount the cart discount which should be updated.
     * @param newCartDiscount the cart discount draft where we get the new stacking mode.
     * @return A filled optional with the update action or an empty optional if the stacking modes are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<CartDiscount>> buildChangeStackingModeUpdateAction(
        @Nonnull final CartDiscount oldCartDiscount,
        @Nonnull final CartDiscountDraft newCartDiscount) {
        final StackingMode stackingMode = ofNullable(newCartDiscount.getStackingMode()).orElse(StackingMode.STACKING);

        return buildUpdateAction(oldCartDiscount.getStackingMode(), stackingMode,
            () -> ChangeStackingMode.of(stackingMode));
    }

    private CartDiscountUpdateActionUtils() {
    }
}
