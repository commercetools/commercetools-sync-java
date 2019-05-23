package com.commercetools.sync.cartdiscounts.utils;

import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountTarget;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.CustomLineItemsTarget;
import io.sphere.sdk.cartdiscounts.LineItemsTarget;
import io.sphere.sdk.cartdiscounts.ShippingCostTarget;
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
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Optional;

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
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountUpdateActionUtils.buildSetValidFromUpdateAction;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountUpdateActionUtils.buildSetValidUntilUpdateAction;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static io.sphere.sdk.models.DefaultCurrencyUnits.USD;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CartDiscountUpdateActionUtilsTest {

    private static final CartDiscount CART_DISCOUNT_WITH_RELATIVE_VALUE =
        readObjectFromResource("cart-discount-with-relative-value.json", CartDiscount.class);
    private static final CartDiscount CART_DISCOUNT_WITH_ABSOLUTE_VALUE =
        readObjectFromResource("cart-discount-with-absolute-value.json", CartDiscount.class);
    private static final CartDiscount CART_DISCOUNT_WITH_GIFT_ITEM_VALUE =
        readObjectFromResource("cart-discount-with-gift-item-value.json", CartDiscount.class);
    private static final CartDiscount CART_DISCOUNT_WITH_CUSTOM_LINE_ITEM_TARGET =
        readObjectFromResource("cart-discount-with-custom-line-item-target.json", CartDiscount.class);

    @Test
    public void buildChangeValueUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountValue tenEuro = CartDiscountValue.ofAbsolute(MoneyImpl.of(10, EUR));
        when(newCartDiscountDraft.getValue()).thenReturn(tenEuro);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
            buildChangeValueUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(tenEuro));
    }

    @Test
    public void buildChangeValueUpdateAction_WithDifferentRelativeValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountValue twentyPercent = CartDiscountValue.ofRelative(2000);
        when(newCartDiscountDraft.getValue()).thenReturn(twentyPercent);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
            buildChangeValueUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(twentyPercent));
    }

    @Test
    public void buildChangeValueUpdateAction_WithNewGiftItemValue_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValue()).thenReturn(CART_DISCOUNT_WITH_GIFT_ITEM_VALUE.getValue());

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
            buildChangeValueUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(CART_DISCOUNT_WITH_GIFT_ITEM_VALUE.getValue()));
    }

    @Test
    public void buildChangeValueUpdateAction_WithSameAbsoluteValues_ShouldNotBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountValue values =
            CartDiscountValue.ofAbsolute(asList(MoneyImpl.of(10, EUR), MoneyImpl.of(10, USD)));
        when(newCartDiscountDraft.getValue()).thenReturn(values);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
            buildChangeValueUpdateAction(CART_DISCOUNT_WITH_ABSOLUTE_VALUE, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeValueUpdateAction_WithSameAbsoluteValuesWithDifferentOrder_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountValue sameValuesWithDifferentOrder =
            CartDiscountValue.ofAbsolute(asList(MoneyImpl.of(10, USD), MoneyImpl.of(10, EUR)));
        when(newCartDiscountDraft.getValue()).thenReturn(sameValuesWithDifferentOrder);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
            buildChangeValueUpdateAction(CART_DISCOUNT_WITH_ABSOLUTE_VALUE, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(sameValuesWithDifferentOrder));
    }

    @Test
    public void buildChangeValueUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountValue tenPercent = CartDiscountValue.ofRelative(1000);
        when(newCartDiscountDraft.getValue()).thenReturn(tenPercent);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
            buildChangeValueUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeValueUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValue()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
            buildChangeValueUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(null));
    }

    @Test
    public void buildChangeCartPredicateUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getCartPredicate()).thenReturn("1 = 1");

        final Optional<UpdateAction<CartDiscount>> changeCartPredicateUpdateAction =
            buildChangeCartPredicateUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeCartPredicateUpdateAction).contains(ChangeCartPredicate.of("1 = 1"));
    }

    @Test
    public void buildChangeCartPredicateUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getCartPredicate()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeCartPredicateUpdateAction =
            buildChangeCartPredicateUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeCartPredicateUpdateAction).contains(ChangeCartPredicate.of((String) null));
    }

    @Test
    public void buildChangeCartPredicateUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getCartPredicate())
            .thenReturn("totalPrice = \"10.00 EUR\" and (shippingInfo.shippingMethodName = \"FEDEX\")");

        final Optional<UpdateAction<CartDiscount>> changePredicateUpdateAction =
            buildChangeCartPredicateUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changePredicateUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeTargetUpdateAction_WithDifferentLineItemTargetValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountTarget cartDiscountTarget = LineItemsTarget.of("quantity > 0");
        when(newCartDiscountDraft.getTarget()).thenReturn(cartDiscountTarget);

        final Optional<UpdateAction<CartDiscount>> changeTargetUpdateAction =
            buildChangeTargetUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeTargetUpdateAction).contains(ChangeTarget.of(cartDiscountTarget));
    }

    @Test
    public void buildChangeTargetUpdateAction_WithSameLineItemTargetValues_ShouldNotBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountTarget cartDiscountTarget = LineItemsTarget.of("1 = 1");
        when(newCartDiscountDraft.getTarget()).thenReturn(cartDiscountTarget);

        final Optional<UpdateAction<CartDiscount>> changeTargetUpdateAction =
            buildChangeTargetUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeTargetUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeTargetUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getTarget()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeTargetUpdateAction =
            buildChangeTargetUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeTargetUpdateAction).contains(ChangeTarget.of(null));
    }

    @Test
    public void buildChangeTargetUpdateAction_WithDifferentCustomLineItemValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountTarget cartDiscountTarget = CustomLineItemsTarget.of("1 = 1");
        when(newCartDiscountDraft.getTarget()).thenReturn(cartDiscountTarget);

        final Optional<UpdateAction<CartDiscount>> changeTargetUpdateAction =
            buildChangeTargetUpdateAction(CART_DISCOUNT_WITH_CUSTOM_LINE_ITEM_TARGET, newCartDiscountDraft);

        assertThat(changeTargetUpdateAction).contains(ChangeTarget.of(cartDiscountTarget));
    }

    @Test
    public void buildChangeTargetUpdateAction_WithLineItemAndShippingTargetValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountTarget cartDiscountTarget = ShippingCostTarget.of();
        when(newCartDiscountDraft.getTarget()).thenReturn(cartDiscountTarget);

        final Optional<UpdateAction<CartDiscount>> changeTargetUpdateAction =
            buildChangeTargetUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeTargetUpdateAction).contains(ChangeTarget.of(cartDiscountTarget));
    }

    @Test
    public void buildChangeIsActiveUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isActive()).thenReturn(true);

        final Optional<UpdateAction<CartDiscount>> changeIsActiveUpdateAction =
            buildChangeIsActiveUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeIsActiveUpdateAction).contains(ChangeIsActive.of(true));
    }

    @Test
    public void buildChangeIsActiveUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isActive()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeIsActiveUpdateAction =
            buildChangeIsActiveUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeIsActiveUpdateAction).contains(ChangeIsActive.of(true));
    }

    @Test
    public void buildChangeIsActiveUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isActive()).thenReturn(false);

        final Optional<UpdateAction<CartDiscount>> changeIsActiveUpdateAction =
            buildChangeIsActiveUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeIsActiveUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getName()).thenReturn(LocalizedString.of(Locale.ENGLISH, "newName"));

        final Optional<UpdateAction<CartDiscount>> changeNameUpdateAction =
            buildChangeNameUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeNameUpdateAction).contains(ChangeName.of(LocalizedString.of(Locale.ENGLISH, "newName")));
    }

    @Test
    public void buildChangeNameUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getName()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeNameUpdateAction =
            buildChangeNameUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeNameUpdateAction).contains(ChangeName.of(null));
    }

    @Test
    public void buildChangeNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getName()).thenReturn(LocalizedString.of(Locale.ENGLISH, "cart-discount-1"));

        final Optional<UpdateAction<CartDiscount>> changeNameUpdateAction =
            buildChangeNameUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeNameUpdateAction).isNotPresent();
    }


    @Test
    public void buildSetDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getDescription())
            .thenReturn(LocalizedString.of(Locale.ENGLISH, "new-description"));

        final Optional<UpdateAction<CartDiscount>> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(setDescriptionUpdateAction)
            .contains(SetDescription.of(LocalizedString.of(Locale.ENGLISH, "new-description")));
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getDescription()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(setDescriptionUpdateAction).contains(SetDescription.of(null));
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getDescription())
            .thenReturn(LocalizedString.of(Locale.ENGLISH, "cart-discount-1-desc"));

        final Optional<UpdateAction<CartDiscount>> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(setDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeSortOrderUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getSortOrder()).thenReturn("0.3");

        final Optional<UpdateAction<CartDiscount>> changeChangeSortOrderUpdateAction =
            buildChangeSortOrderUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeChangeSortOrderUpdateAction).contains(ChangeSortOrder.of("0.3"));
    }

    @Test
    public void buildChangeSortOrderUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getSortOrder()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeChangeSortOrderUpdateAction =
            buildChangeSortOrderUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeChangeSortOrderUpdateAction).contains(ChangeSortOrder.of(null));
    }

    @Test
    public void buildChangeSortOrderUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getSortOrder()).thenReturn("0.1");

        final Optional<UpdateAction<CartDiscount>> changeChangeSortOrderUpdateAction =
            buildChangeSortOrderUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeChangeSortOrderUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeRequiresDiscountCodeUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isRequiresDiscountCode()).thenReturn(false);

        final Optional<UpdateAction<CartDiscount>> changeRequiresDiscountCodeUpdateAction =
            buildChangeRequiresDiscountCodeUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeRequiresDiscountCodeUpdateAction).contains(ChangeRequiresDiscountCode.of(false));
    }

    @Test
    public void buildChangeRequiresDiscountCodeUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isRequiresDiscountCode()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeRequiresDiscountCodeUpdateAction =
            buildChangeRequiresDiscountCodeUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeRequiresDiscountCodeUpdateAction).contains(ChangeRequiresDiscountCode.of(false));
    }

    @Test
    public void buildChangeRequiresDiscountCodeUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isRequiresDiscountCode()).thenReturn(true);

        final Optional<UpdateAction<CartDiscount>> changeRequiresDiscountCodeUpdateAction =
            buildChangeRequiresDiscountCodeUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeRequiresDiscountCodeUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeStackingModeUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getStackingMode()).thenReturn(StackingMode.STACKING);

        final Optional<UpdateAction<CartDiscount>> changeStackingModeUpdateAction =
            buildChangeStackingModeUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeStackingModeUpdateAction)
            .contains(ChangeStackingMode.of(StackingMode.STACKING));
    }

    @Test
    public void buildChangeStackingModeUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getStackingMode()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeStackingModeUpdateAction =
            buildChangeStackingModeUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeStackingModeUpdateAction).contains(ChangeStackingMode.of(StackingMode.STACKING));
    }

    @Test
    public void buildChangeStackingModeUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getStackingMode()).thenReturn(StackingMode.STOP_AFTER_THIS_DISCOUNT);

        final Optional<UpdateAction<CartDiscount>> changeStackingModeUpdateAction =
            buildChangeStackingModeUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(changeStackingModeUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetValidFromUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final ZonedDateTime now = ZonedDateTime.now();
        when(newCartDiscountDraft.getValidFrom()).thenReturn(now);

        final Optional<UpdateAction<CartDiscount>> setValidFromUpdateAction =
            buildSetValidFromUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(setValidFromUpdateAction).contains(SetValidFrom.of(now));
    }

    @Test
    public void buildSetValidFromUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidFrom()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> setValidFromUpdateAction =
            buildSetValidFromUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(setValidFromUpdateAction).contains(SetValidFrom.of(null));
    }

    @Test
    public void buildSetValidFromUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidFrom()).thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));

        final Optional<UpdateAction<CartDiscount>> setValidFromUpdateAction =
            buildSetValidFromUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(setValidFromUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetValidUntilUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final ZonedDateTime now = ZonedDateTime.now();
        when(newCartDiscountDraft.getValidUntil()).thenReturn(now);

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
            buildSetValidUntilUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(setValidUntilUpdateAction).contains(SetValidUntil.of(now));
    }

    @Test
    public void buildSetValidUntilUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidUntil()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
            buildSetValidUntilUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(setValidUntilUpdateAction).contains(SetValidUntil.of(null));
    }

    @Test
    public void buildSetValidUntilUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidUntil()).thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
            buildSetValidUntilUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(setValidUntilUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetValidDatesUpdateAction_WithDifferentValidFromDate_ShouldBuildSetValidFromUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final ZonedDateTime differentValidFromDate = ZonedDateTime.now();
        when(newCartDiscountDraft.getValidFrom()).thenReturn(differentValidFromDate);
        when(newCartDiscountDraft.getValidUntil()).thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

        final Optional<UpdateAction<CartDiscount>> setValidFromUpdateAction =
            buildSetValidDatesUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(setValidFromUpdateAction).contains(SetValidFrom.of(differentValidFromDate));
    }

    @Test
    public void buildSetValidDatesUpdateAction_WithDifferentValidUntilDate_ShouldBuildSetUntilFromUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidFrom()).thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));
        final ZonedDateTime differentValidUntilDate = ZonedDateTime.now();
        when(newCartDiscountDraft.getValidUntil()).thenReturn(differentValidUntilDate);

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
            buildSetValidDatesUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(setValidUntilUpdateAction).contains(SetValidUntil.of(differentValidUntilDate));
    }

    @Test
    public void buildSetValidDatesUpdateAction_WithDifferentDates_ShouldBuildSetValidFromAndUntilUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final ZonedDateTime differentValidFromDate = ZonedDateTime.now();
        when(newCartDiscountDraft.getValidFrom()).thenReturn(differentValidFromDate);
        final ZonedDateTime differentValidUntilDate = ZonedDateTime.now();
        when(newCartDiscountDraft.getValidUntil()).thenReturn(differentValidUntilDate);

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
            buildSetValidDatesUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(setValidUntilUpdateAction)
            .contains(SetValidFromAndUntil.of(differentValidFromDate, differentValidUntilDate));
    }

    @Test
    public void buildSetValidDatesUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidFrom()).thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));
        when(newCartDiscountDraft.getValidUntil()).thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

        final Optional<UpdateAction<CartDiscount>> setValidDatesUpdateAction =
            buildSetValidDatesUpdateAction(CART_DISCOUNT_WITH_RELATIVE_VALUE, newCartDiscountDraft);

        assertThat(setValidDatesUpdateAction).isNotPresent();
    }
}
