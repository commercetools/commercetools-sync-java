package com.commercetools.sync.cartdiscounts.utils;

import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountTarget;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.CustomLineItemsTarget;
import io.sphere.sdk.cartdiscounts.GiftLineItemCartDiscountValue;
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
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.jupiter.api.Test;

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
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static io.sphere.sdk.models.DefaultCurrencyUnits.USD;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CartDiscountUpdateActionUtilsTest {

    @Test
    void buildChangeValueUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValue()).thenReturn(CartDiscountValue.ofRelative(1000));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountValue tenEuro = CartDiscountValue.ofAbsolute(MoneyImpl.of(10, EUR));
        when(newCartDiscountDraft.getValue()).thenReturn(tenEuro);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
                buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(tenEuro));
    }

    @Test
    void buildChangeValueUpdateAction_WithDifferentRelativeValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValue()).thenReturn(CartDiscountValue.ofRelative(1000));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountValue twentyPercent = CartDiscountValue.ofRelative(2000);
        when(newCartDiscountDraft.getValue()).thenReturn(twentyPercent);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
                buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(twentyPercent));
    }

    @Test
    void buildChangeValueUpdateAction_WithSameRelativeValues_ShouldNotBuildUpdateAction() {
        final CartDiscountValue tenPercent = CartDiscountValue.ofRelative(1000);

        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValue()).thenReturn(tenPercent);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValue()).thenReturn(tenPercent);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
                buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeValueUpdateAction_WithNewGiftItemValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValue()).thenReturn(CartDiscountValue.ofRelative(1000));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final GiftLineItemCartDiscountValue giftLineItemCartDiscountValue =
                GiftLineItemCartDiscountValue.of(Reference.of(Product.referenceTypeId(), "productId"),
                        1, null, null);
        when(newCartDiscountDraft.getValue()).thenReturn(giftLineItemCartDiscountValue);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
                buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(giftLineItemCartDiscountValue));
    }

    @Test
    void buildChangeValueUpdateAction_WithDifferentGiftItemProductValue_ShouldBuildUpdateAction() {
        final GiftLineItemCartDiscountValue oldGiftLineItemCartDiscountValue =
                GiftLineItemCartDiscountValue.of(Reference.of(Product.referenceTypeId(), "product-1"),
                        1, null, null);

        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValue()).thenReturn(oldGiftLineItemCartDiscountValue);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final GiftLineItemCartDiscountValue newGiftLineItemCartDiscountValue =
                GiftLineItemCartDiscountValue.of(Reference.of(Product.referenceTypeId(), "product-2"),
                        1, null, null);
        when(newCartDiscountDraft.getValue()).thenReturn(newGiftLineItemCartDiscountValue);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
                buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(newGiftLineItemCartDiscountValue));
    }

    @Test
    void buildChangeValueUpdateAction_WithDifferentGiftItemProductVariantValue_ShouldBuildUpdateAction() {
        final GiftLineItemCartDiscountValue oldGiftLineItemCartDiscountValue =
                GiftLineItemCartDiscountValue.of(Reference.of(Product.referenceTypeId(), "productId"),
                        1, null, null);

        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValue()).thenReturn(oldGiftLineItemCartDiscountValue);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final GiftLineItemCartDiscountValue newGiftLineItemCartDiscountValue =
                GiftLineItemCartDiscountValue.of(Reference.of(Product.referenceTypeId(), "productId"),
                        2, null, null);
        when(newCartDiscountDraft.getValue()).thenReturn(newGiftLineItemCartDiscountValue);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
                buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(newGiftLineItemCartDiscountValue));
    }

    @Test
    void buildChangeValueUpdateAction_WithDifferentGiftItemSupplyChannelValue_ShouldBuildUpdateAction() {
        final GiftLineItemCartDiscountValue oldGiftLineItemCartDiscountValue =
                GiftLineItemCartDiscountValue.of(Reference.of(Product.referenceTypeId(), "productId"),
                        1, Reference.of(Channel.referenceTypeId(), "supplyChannel-1"), null);

        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValue()).thenReturn(oldGiftLineItemCartDiscountValue);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final GiftLineItemCartDiscountValue newGiftLineItemCartDiscountValue =
                GiftLineItemCartDiscountValue.of(Reference.of(Product.referenceTypeId(), "productId"),
                        1, Reference.of(Channel.referenceTypeId(), "supplyChannel-2"), null);
        when(newCartDiscountDraft.getValue()).thenReturn(newGiftLineItemCartDiscountValue);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
                buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(newGiftLineItemCartDiscountValue));
    }

    @Test
    void buildChangeValueUpdateAction_WithDifferentGiftItemDistributionChannelValue_ShouldBuildUpdateAction() {
        final GiftLineItemCartDiscountValue oldGiftLineItemCartDiscountValue =
                GiftLineItemCartDiscountValue.of(Reference.of(Product.referenceTypeId(), "productId"),
                        1, null, Reference.of(Channel.referenceTypeId(), "dist-channel-1"));

        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValue()).thenReturn(oldGiftLineItemCartDiscountValue);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final GiftLineItemCartDiscountValue newGiftLineItemCartDiscountValue =
                GiftLineItemCartDiscountValue.of(Reference.of(Product.referenceTypeId(), "productId"),
                        1, null, Reference.of(Channel.referenceTypeId(), "dist-channel-2"));
        when(newCartDiscountDraft.getValue()).thenReturn(newGiftLineItemCartDiscountValue);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
                buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(newGiftLineItemCartDiscountValue));
    }

    @Test
    void buildChangeValueUpdateAction_WithSameGiftItemValue_ShouldNotBuildUpdateAction() {
        final GiftLineItemCartDiscountValue giftLineItemCartDiscountValue =
                GiftLineItemCartDiscountValue.of(Reference.of(Product.referenceTypeId(), "productId"),
                        1, null, null);

        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValue()).thenReturn(giftLineItemCartDiscountValue);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValue()).thenReturn(giftLineItemCartDiscountValue);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
                buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeValueUpdateAction_WithSameAbsoluteValues_ShouldNotBuildUpdateAction() {
        final CartDiscountValue values =
                CartDiscountValue.ofAbsolute(asList(MoneyImpl.of(10, EUR), MoneyImpl.of(10, USD)));

        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValue()).thenReturn(values);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValue()).thenReturn(values);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
                buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeValueUpdateAction_WithDifferentAbsoluteValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValue()).thenReturn(CartDiscountValue.ofAbsolute(MoneyImpl.of(10, EUR)));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountValue fiftyEuro = CartDiscountValue.ofAbsolute(MoneyImpl.of(50, EUR));
        when(newCartDiscountDraft.getValue()).thenReturn(fiftyEuro);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
                buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(fiftyEuro));
    }

    @Test
    void buildChangeValueUpdateAction_WithSameAbsoluteValuesWithDifferentOrder_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValue()).thenReturn(
                CartDiscountValue.ofAbsolute(asList(MoneyImpl.of(10, EUR), MoneyImpl.of(10, USD))));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountValue sameValuesWithDifferentOrder =
                CartDiscountValue.ofAbsolute(asList(MoneyImpl.of(10, USD), MoneyImpl.of(10, EUR)));
        when(newCartDiscountDraft.getValue()).thenReturn(sameValuesWithDifferentOrder);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
                buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(sameValuesWithDifferentOrder));
    }

    @Test
    void buildChangeValueUpdateAction_WithJustNewValueHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValue()).thenReturn(CartDiscountValue.ofRelative(1000));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValue()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
                buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(null));
    }

    @Test
    void buildChangeValueUpdateAction_WithJustOldValueHaveNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValue()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountValue value = CartDiscountValue.ofAbsolute(MoneyImpl.of(10, EUR));
        when(newCartDiscountDraft.getValue()).thenReturn(value);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
                buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).contains(ChangeValue.of(value));
    }

    @Test
    void buildChangeValueUpdateAction_WithBothValueHaveNullValues_ShouldNotBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValue()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValue()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeValueUpdateAction =
                buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeValueUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeCartPredicateUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getCartPredicate())
                .thenReturn("totalPrice = \"10.00 EUR\" and (shippingInfo.shippingMethodName = \"FEDEX\")");

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final String newCartPredicate = "1 = 1";
        when(newCartDiscountDraft.getCartPredicate()).thenReturn(newCartPredicate);

        final Optional<UpdateAction<CartDiscount>> changeCartPredicateUpdateAction =
                buildChangeCartPredicateUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeCartPredicateUpdateAction).contains(ChangeCartPredicate.of(newCartPredicate));
    }

    @Test
    void buildChangeCartPredicateUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final String cartPredicate = "totalPrice = \"10.00 EUR\" and (shippingInfo.shippingMethodName = \"FEDEX\")";

        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getCartPredicate()).thenReturn(cartPredicate);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getCartPredicate()).thenReturn(cartPredicate);

        final Optional<UpdateAction<CartDiscount>> changePredicateUpdateAction =
                buildChangeCartPredicateUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changePredicateUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeCartPredicateUpdateAction_WithJustNewCartPredicateHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getCartPredicate())
                .thenReturn("totalPrice = \"10.00 EUR\" and (shippingInfo.shippingMethodName = \"FEDEX\")");

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getCartPredicate()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeCartPredicateUpdateAction =
                buildChangeCartPredicateUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeCartPredicateUpdateAction).contains(ChangeCartPredicate.of((String) null));
    }

    @Test
    void buildChangeCartPredicateUpdateAction_WithJustOldCartPredicateHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getCartPredicate()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final String newCartPredicate = "1 = 1";
        when(newCartDiscountDraft.getCartPredicate()).thenReturn(newCartPredicate);

        final Optional<UpdateAction<CartDiscount>> changeCartPredicateUpdateAction =
                buildChangeCartPredicateUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeCartPredicateUpdateAction).contains(ChangeCartPredicate.of(newCartPredicate));
    }

    @Test
    void buildChangeCartPredicateUpdateAction_WithBothCartPredicateHaveNullValues_ShouldNotBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getCartPredicate()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getCartPredicate()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeCartPredicateUpdateAction =
                buildChangeCartPredicateUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeCartPredicateUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeTargetUpdateAction_WithDifferentLineItemTargetValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getTarget()).thenReturn(LineItemsTarget.ofAll());

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountTarget cartDiscountTarget = LineItemsTarget.of("quantity > 0");
        when(newCartDiscountDraft.getTarget()).thenReturn(cartDiscountTarget);

        final Optional<UpdateAction<CartDiscount>> changeTargetUpdateAction =
                buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeTargetUpdateAction).contains(ChangeTarget.of(cartDiscountTarget));
    }

    @Test
    void buildChangeTargetUpdateAction_WithSameLineItemTargetValues_ShouldNotBuildUpdateAction() {
        final CartDiscountTarget cartDiscountTarget = LineItemsTarget.of("quantity > 0");

        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getTarget()).thenReturn(cartDiscountTarget);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getTarget()).thenReturn(cartDiscountTarget);

        final Optional<UpdateAction<CartDiscount>> changeTargetUpdateAction =
                buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeTargetUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeTargetUpdateAction_WithDifferentCustomLineItemValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getTarget()).thenReturn(CustomLineItemsTarget.of("money = \"100 EUR\""));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountTarget cartDiscountTarget = CustomLineItemsTarget.of("1 = 1");
        when(newCartDiscountDraft.getTarget()).thenReturn(cartDiscountTarget);

        final Optional<UpdateAction<CartDiscount>> changeTargetUpdateAction =
                buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeTargetUpdateAction).contains(ChangeTarget.of(cartDiscountTarget));
    }

    @Test
    void buildChangeTargetUpdateAction_WithLineItemAndShippingTargetValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getTarget()).thenReturn(LineItemsTarget.ofAll());

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final CartDiscountTarget cartDiscountTarget = ShippingCostTarget.of();
        when(newCartDiscountDraft.getTarget()).thenReturn(cartDiscountTarget);

        final Optional<UpdateAction<CartDiscount>> changeTargetUpdateAction =
                buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeTargetUpdateAction).contains(ChangeTarget.of(cartDiscountTarget));
    }

    @Test
    void buildChangeTargetUpdateAction_WithJustNewTargetHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getTarget()).thenReturn(LineItemsTarget.ofAll());

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getTarget()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeTargetUpdateAction =
                buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeTargetUpdateAction).contains(ChangeTarget.of(null));
    }

    @Test
    void buildChangeTargetUpdateAction_WithJustOldTargetHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getTarget()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final LineItemsTarget newTarget = LineItemsTarget.ofAll();
        when(newCartDiscountDraft.getTarget()).thenReturn(newTarget);

        final Optional<UpdateAction<CartDiscount>> changeTargetUpdateAction =
                buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeTargetUpdateAction).contains(ChangeTarget.of(newTarget));
    }

    @Test
    void buildChangeTargetUpdateAction_WithBothTargetHaveNullValues_ShouldNotBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getTarget()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getTarget()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeTargetUpdateAction =
                buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeTargetUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeIsActiveUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.isActive()).thenReturn(false);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isActive()).thenReturn(true);

        final Optional<UpdateAction<CartDiscount>> changeIsActiveUpdateAction =
                buildChangeIsActiveUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeIsActiveUpdateAction).contains(ChangeIsActive.of(true));
    }

    @Test
    void buildChangeIsActiveUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.isActive()).thenReturn(false);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isActive()).thenReturn(false);

        final Optional<UpdateAction<CartDiscount>> changeIsActiveUpdateAction =
                buildChangeIsActiveUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeIsActiveUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeIsActiveUpdateAction_WithJustNewIsActiveHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.isActive()).thenReturn(false);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isActive()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeIsActiveUpdateAction =
                buildChangeIsActiveUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeIsActiveUpdateAction).contains(ChangeIsActive.of(true));
    }

    @Test
    void buildChangeIsActiveUpdateAction_WithJustOldIsActiveHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.isActive()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isActive()).thenReturn(false);

        final Optional<UpdateAction<CartDiscount>> changeIsActiveUpdateAction =
                buildChangeIsActiveUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeIsActiveUpdateAction).contains(ChangeIsActive.of(false));
    }

    @Test
    void buildChangeIsActiveUpdateAction_WithBothIsActiveHaveNullValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.isActive()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isActive()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeIsActiveUpdateAction =
                buildChangeIsActiveUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeIsActiveUpdateAction).contains(ChangeIsActive.of(true));
    }

    @Test
    void buildChangeNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getName()).thenReturn(LocalizedString.of(Locale.ENGLISH, "cart-discount-1"));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final LocalizedString name = LocalizedString.of(Locale.ENGLISH, "newName");
        when(newCartDiscountDraft.getName()).thenReturn(name);

        final Optional<UpdateAction<CartDiscount>> changeNameUpdateAction =
                buildChangeNameUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeNameUpdateAction).contains(ChangeName.of(name));
    }

    @Test
    void buildChangeNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final LocalizedString name = LocalizedString.of(Locale.ENGLISH, "name");
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getName()).thenReturn(name);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getName()).thenReturn(name);

        final Optional<UpdateAction<CartDiscount>> changeNameUpdateAction =
                buildChangeNameUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeNameUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeNameUpdateAction_WithJustNewNameHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getName()).thenReturn(LocalizedString.of(Locale.ENGLISH, "name"));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getName()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeNameUpdateAction =
                buildChangeNameUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeNameUpdateAction).contains(ChangeName.of(null));
    }

    @Test
    void buildChangeNameUpdateAction_WithJustOldNameHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.isActive()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final LocalizedString newName = LocalizedString.of(Locale.ENGLISH, "name");
        when(newCartDiscountDraft.getName()).thenReturn(newName);

        final Optional<UpdateAction<CartDiscount>> changeNameUpdateAction =
                buildChangeNameUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeNameUpdateAction).contains(ChangeName.of(newName));
    }

    @Test
    void buildChangeNameUpdateAction_WithBothNameHaveNullValues_ShouldNotBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getName()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getName()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeNameUpdateAction =
                buildChangeNameUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeNameUpdateAction).isNotPresent();
    }

    @Test
    void buildSetDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getDescription())
                .thenReturn(LocalizedString.of(Locale.ENGLISH, "cart-discount-1-desc"));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final LocalizedString description = LocalizedString.of(Locale.ENGLISH, "new-description");
        when(newCartDiscountDraft.getDescription()).thenReturn(description);

        final Optional<UpdateAction<CartDiscount>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setDescriptionUpdateAction).contains(SetDescription.of(description));
    }

    @Test
    void buildSetDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final LocalizedString description = LocalizedString.of(Locale.ENGLISH, "cart-discount-1-desc");

        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getDescription()).thenReturn(description);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getDescription()).thenReturn(description);

        final Optional<UpdateAction<CartDiscount>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setDescriptionUpdateAction).isNotPresent();
    }

    @Test
    void buildSetDescriptionUpdateAction_WithJustNewDescriptionHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getDescription())
                .thenReturn(LocalizedString.of(Locale.ENGLISH, "cart-discount-1-desc"));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getDescription()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setDescriptionUpdateAction).contains(SetDescription.of(null));
    }

    @Test
    void buildSetDescriptionUpdateAction_WithJustOldDescriptionHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getDescription()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final LocalizedString newDesc = LocalizedString.of(Locale.ENGLISH, "new-desc");
        when(newCartDiscountDraft.getDescription()).thenReturn(newDesc);

        final Optional<UpdateAction<CartDiscount>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setDescriptionUpdateAction).contains(SetDescription.of(newDesc));
    }

    @Test
    void buildSetDescriptionUpdateAction_WithBothDescriptionHaveNullValues_ShouldNotBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getDescription()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getDescription()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setDescriptionUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeSortOrderUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getSortOrder()).thenReturn("0.1");

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final String sortOrder = "0.3";
        when(newCartDiscountDraft.getSortOrder()).thenReturn(sortOrder);

        final Optional<UpdateAction<CartDiscount>> changeChangeSortOrderUpdateAction =
                buildChangeSortOrderUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeChangeSortOrderUpdateAction).contains(ChangeSortOrder.of(sortOrder));
    }

    @Test
    void buildChangeSortOrderUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final String sortOrder = "0.1";

        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getSortOrder()).thenReturn(sortOrder);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getSortOrder()).thenReturn(sortOrder);

        final Optional<UpdateAction<CartDiscount>> changeChangeSortOrderUpdateAction =
                buildChangeSortOrderUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeChangeSortOrderUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeSortOrderUpdateAction_WithJustNewSortOrderHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getSortOrder()).thenReturn("0.1");

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getSortOrder()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeChangeSortOrderUpdateAction =
                buildChangeSortOrderUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeChangeSortOrderUpdateAction).contains(ChangeSortOrder.of(null));
    }

    @Test
    void buildChangeSortOrderUpdateAction_WithJustOldSortOrderHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getSortOrder()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final String newSortOrder = "0.3";
        when(newCartDiscountDraft.getSortOrder()).thenReturn(newSortOrder);

        final Optional<UpdateAction<CartDiscount>> changeChangeSortOrderUpdateAction =
                buildChangeSortOrderUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeChangeSortOrderUpdateAction).contains(ChangeSortOrder.of(newSortOrder));
    }

    @Test
    void buildChangeSortOrderUpdateAction_WithBothSortOrderHaveNullValues_ShouldNotBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getSortOrder()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getSortOrder()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeChangeSortOrderUpdateAction =
                buildChangeSortOrderUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeChangeSortOrderUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeRequiresDiscountCodeUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.isRequiringDiscountCode()).thenReturn(true);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isRequiresDiscountCode()).thenReturn(false);

        final Optional<UpdateAction<CartDiscount>> changeRequiresDiscountCodeUpdateAction =
                buildChangeRequiresDiscountCodeUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeRequiresDiscountCodeUpdateAction).contains(ChangeRequiresDiscountCode.of(false));
    }

    @Test
    void buildChangeRequiresDiscountCodeUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.isRequiringDiscountCode()).thenReturn(true);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isRequiresDiscountCode()).thenReturn(true);

        final Optional<UpdateAction<CartDiscount>> changeRequiresDiscountCodeUpdateAction =
                buildChangeRequiresDiscountCodeUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeRequiresDiscountCodeUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeRequiresDiscountCodeUpdateAction_WithJustNewRequiresDisCodeHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.isRequiringDiscountCode()).thenReturn(true);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isRequiresDiscountCode()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeRequiresDiscountCodeUpdateAction =
                buildChangeRequiresDiscountCodeUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeRequiresDiscountCodeUpdateAction).contains(ChangeRequiresDiscountCode.of(false));
    }

    @Test
    void buildChangeRequiresDiscountCodeUpdateAction_WithJustOldRequiresDisCodeHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.isRequiringDiscountCode()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isRequiresDiscountCode()).thenReturn(true);

        final Optional<UpdateAction<CartDiscount>> changeRequiresDiscountCodeUpdateAction =
                buildChangeRequiresDiscountCodeUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeRequiresDiscountCodeUpdateAction).contains(ChangeRequiresDiscountCode.of(true));
    }

    @Test
    void buildChangeRequiresDiscountCodeUpdateAction_WithBothReqDisCodeHaveNullValues_ShouldNotBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.isRequiringDiscountCode()).thenReturn(true);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.isRequiresDiscountCode()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeRequiresDiscountCodeUpdateAction =
                buildChangeRequiresDiscountCodeUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeRequiresDiscountCodeUpdateAction).contains(ChangeRequiresDiscountCode.of(false));
    }

    @Test
    void buildChangeStackingModeUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getStackingMode()).thenReturn(StackingMode.STOP_AFTER_THIS_DISCOUNT);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getStackingMode()).thenReturn(StackingMode.STACKING);

        final Optional<UpdateAction<CartDiscount>> changeStackingModeUpdateAction =
                buildChangeStackingModeUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeStackingModeUpdateAction)
                .contains(ChangeStackingMode.of(StackingMode.STACKING));
    }

    @Test
    void buildChangeStackingModeUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getStackingMode()).thenReturn(StackingMode.STOP_AFTER_THIS_DISCOUNT);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getStackingMode()).thenReturn(StackingMode.STOP_AFTER_THIS_DISCOUNT);

        final Optional<UpdateAction<CartDiscount>> changeStackingModeUpdateAction =
                buildChangeStackingModeUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeStackingModeUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeStackingModeUpdateAction_WithJustNewStackingModeHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getStackingMode()).thenReturn(StackingMode.STOP_AFTER_THIS_DISCOUNT);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getStackingMode()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeStackingModeUpdateAction =
                buildChangeStackingModeUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeStackingModeUpdateAction).contains(ChangeStackingMode.of(StackingMode.STACKING));
    }

    @Test
    void buildChangeStackingModeUpdateAction_WithJustOldRequiresDisCodeHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getStackingMode()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getStackingMode()).thenReturn(StackingMode.STOP_AFTER_THIS_DISCOUNT);

        final Optional<UpdateAction<CartDiscount>> changeStackingModeUpdateAction =
                buildChangeStackingModeUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeStackingModeUpdateAction)
                .contains(ChangeStackingMode.of(StackingMode.STOP_AFTER_THIS_DISCOUNT));
    }

    @Test
    void buildChangeStackingModeUpdateAction_WithBothReqDisCodeHaveNullValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getStackingMode()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getStackingMode()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> changeStackingModeUpdateAction =
                buildChangeStackingModeUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(changeStackingModeUpdateAction).contains(ChangeStackingMode.of(StackingMode.STACKING));
    }

    @Test
    void buildSetValidFromUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidFrom()).thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final ZonedDateTime now = ZonedDateTime.now();
        when(newCartDiscountDraft.getValidFrom()).thenReturn(now);

        final Optional<UpdateAction<CartDiscount>> setValidFromUpdateAction =
                buildSetValidFromUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidFromUpdateAction).contains(SetValidFrom.of(now));
    }

    @Test
    void buildSetValidFromUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final ZonedDateTime validFrom = ZonedDateTime.parse("2019-04-30T22:00:00.000Z");

        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidFrom()).thenReturn(validFrom);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidFrom()).thenReturn(validFrom);

        final Optional<UpdateAction<CartDiscount>> setValidFromUpdateAction =
                buildSetValidFromUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidFromUpdateAction).isNotPresent();
    }

    @Test
    void buildSetValidFromUpdateAction_WithJustNewSetValidFromDateHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidFrom()).thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidFrom()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> setValidFromUpdateAction =
                buildSetValidFromUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidFromUpdateAction).contains(SetValidFrom.of(null));
    }

    @Test
    void buildSetValidFromUpdateAction_WithJustOldSetValidFromDateHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidFrom()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidFrom()).thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));

        final Optional<UpdateAction<CartDiscount>> setValidFromUpdateAction =
                buildSetValidFromUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidFromUpdateAction).contains(SetValidFrom.of(ZonedDateTime.parse("2019-04-30T22:00:00.000Z")));
    }

    @Test
    void buildSetValidFromUpdateAction_WithBothSetValidFromDateHaveNullValues_ShouldNotBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidFrom()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidFrom()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> setValidFromUpdateAction =
                buildSetValidFromUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidFromUpdateAction).isNotPresent();
    }

    @Test
    void buildSetValidUntilUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidFrom()).thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final ZonedDateTime now = ZonedDateTime.now();
        when(newCartDiscountDraft.getValidUntil()).thenReturn(now);

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
                buildSetValidUntilUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidUntilUpdateAction).contains(SetValidUntil.of(now));
    }

    @Test
    void buildSetValidUntilUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final ZonedDateTime validUntil = ZonedDateTime.parse("2019-05-30T22:00:00.000Z");

        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidUntil()).thenReturn(validUntil);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidUntil()).thenReturn(validUntil);

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
                buildSetValidUntilUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidUntilUpdateAction).isNotPresent();
    }

    @Test
    void buildSetValidUntilUpdateAction_WithJustNewSetValidUntilDateHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidUntil()).thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidUntil()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
                buildSetValidUntilUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidUntilUpdateAction).contains(SetValidUntil.of(null));
    }

    @Test
    void buildSetValidUntilUpdateAction_WithJustOldSetValidUntilDateHasNullValue_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidUntil()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidUntil()).thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
                buildSetValidUntilUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidUntilUpdateAction)
                .contains(SetValidUntil.of(ZonedDateTime.parse("2019-05-30T22:00:00.000Z")));
    }

    @Test
    void buildSetValidUntilUpdateAction_WithBothSetValidUntilDateHaveNullValues_ShouldNotBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidUntil()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidUntil()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
                buildSetValidUntilUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidUntilUpdateAction).isNotPresent();
    }

    @Test
    void buildSetValidDatesUpdateAction_WithDifferentValidFromDate_ShouldBuildSetValidFromUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidFrom()).thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));
        when(oldCartDiscount.getValidUntil()).thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final ZonedDateTime differentValidFromDate = ZonedDateTime.now();
        when(newCartDiscountDraft.getValidFrom()).thenReturn(differentValidFromDate);
        when(newCartDiscountDraft.getValidUntil()).thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

        final Optional<UpdateAction<CartDiscount>> setValidFromUpdateAction =
                buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidFromUpdateAction).contains(SetValidFrom.of(differentValidFromDate));
    }

    @Test
    void buildSetValidDatesUpdateAction_WithDifferentValidUntilDate_ShouldBuildSetValidUntilUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidFrom()).thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));
        when(oldCartDiscount.getValidUntil()).thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidFrom()).thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));
        final ZonedDateTime differentValidUntilDate = ZonedDateTime.now();
        when(newCartDiscountDraft.getValidUntil()).thenReturn(differentValidUntilDate);

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
                buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidUntilUpdateAction).contains(SetValidUntil.of(differentValidUntilDate));
    }

    @Test
    void buildSetValidDatesUpdateAction_WithDifferentDates_ShouldBuildSetValidFromAndUntilUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidFrom()).thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));
        when(oldCartDiscount.getValidUntil()).thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        final ZonedDateTime differentValidFromDate = ZonedDateTime.now();
        when(newCartDiscountDraft.getValidFrom()).thenReturn(differentValidFromDate);
        final ZonedDateTime differentValidUntilDate = ZonedDateTime.now();
        when(newCartDiscountDraft.getValidUntil()).thenReturn(differentValidUntilDate);

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
                buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidUntilUpdateAction)
                .contains(SetValidFromAndUntil.of(differentValidFromDate, differentValidUntilDate));
    }

    @Test
    void buildSetValidDatesUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final ZonedDateTime validFrom = ZonedDateTime.parse("2019-04-30T22:00:00.000Z");
        final ZonedDateTime validUntil = ZonedDateTime.parse("2019-05-30T22:00:00.000Z");

        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidFrom()).thenReturn(validFrom);
        when(oldCartDiscount.getValidUntil()).thenReturn(validUntil);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidFrom()).thenReturn(validFrom);
        when(newCartDiscountDraft.getValidUntil()).thenReturn(validUntil);

        final Optional<UpdateAction<CartDiscount>> setValidDatesUpdateAction =
                buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidDatesUpdateAction).isNotPresent();
    }

    @Test
    void buildSetValidDatesUpdateAction_WithJustOldDatesHaveNullValues_ShouldBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidUntil()).thenReturn(null);
        when(oldCartDiscount.getValidFrom()).thenReturn(null);

        final ZonedDateTime validFrom = ZonedDateTime.parse("2019-04-30T22:00:00.000Z");
        final ZonedDateTime validUntil = ZonedDateTime.parse("2019-05-30T22:00:00.000Z");
        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidFrom()).thenReturn(validFrom);
        when(newCartDiscountDraft.getValidUntil()).thenReturn(validUntil);

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
                buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidUntilUpdateAction).contains(SetValidFromAndUntil.of(validFrom, validUntil));
    }

    @Test
    void buildSetValidDatesUpdateAction_WithJustNewDatesHaveNullValues_ShouldBuildUpdateAction() {
        final ZonedDateTime validFrom = ZonedDateTime.parse("2019-04-30T22:00:00.000Z");
        final ZonedDateTime validUntil = ZonedDateTime.parse("2019-05-30T22:00:00.000Z");
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidUntil()).thenReturn(validFrom);
        when(oldCartDiscount.getValidFrom()).thenReturn(validUntil);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidUntil()).thenReturn(null);
        when(newCartDiscountDraft.getValidFrom()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
                buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidUntilUpdateAction).contains(SetValidFromAndUntil.of(null, null));
    }

    @Test
    void buildSetValidDatesUpdateAction_WithBothSetValidUntilAndFromDateHaveNullValues_ShouldNotBuildUpdateAction() {
        final CartDiscount oldCartDiscount = mock(CartDiscount.class);
        when(oldCartDiscount.getValidUntil()).thenReturn(null);
        when(oldCartDiscount.getValidFrom()).thenReturn(null);

        final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
        when(newCartDiscountDraft.getValidUntil()).thenReturn(null);
        when(newCartDiscountDraft.getValidFrom()).thenReturn(null);

        final Optional<UpdateAction<CartDiscount>> setValidUntilUpdateAction =
                buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscountDraft);

        assertThat(setValidUntilUpdateAction).isNotPresent();
    }
}
