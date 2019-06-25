package com.commercetools.sync.cartdiscounts.utils;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscountTarget;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.CartPredicate;
import io.sphere.sdk.cartdiscounts.LineItemsTarget;
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
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetValidFromAndUntil;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CartDiscountSyncUtilsTest {

    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private static final CartDiscount CART_DISCOUNT_WITH_SHIPPING_TARGET =
        readObjectFromResource("cart-discount-with-shipping-target.json", CartDiscount.class);

    @Test
    void buildActions_FromDraftsWithDifferentNameValues_ShouldBuildUpdateActions() {
        final LocalizedString newName =
            LocalizedString.of(Locale.GERMAN, "Neu Name", Locale.ENGLISH, "new name");

        final CartDiscountDraft newCartDiscount = CartDiscountDraftBuilder
            .of(newName,
                CART_DISCOUNT_WITH_SHIPPING_TARGET.getCartPredicate(),
                CART_DISCOUNT_WITH_SHIPPING_TARGET.getValue(),
                CART_DISCOUNT_WITH_SHIPPING_TARGET.getTarget(),
                CART_DISCOUNT_WITH_SHIPPING_TARGET.getSortOrder(),
                CART_DISCOUNT_WITH_SHIPPING_TARGET.isActive())
            .active(CART_DISCOUNT_WITH_SHIPPING_TARGET.isRequiringDiscountCode())
            .description(CART_DISCOUNT_WITH_SHIPPING_TARGET.getDescription())
            .validFrom(CART_DISCOUNT_WITH_SHIPPING_TARGET.getValidFrom())
            .validUntil(CART_DISCOUNT_WITH_SHIPPING_TARGET.getValidUntil())
            .build();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).build();
        final List<UpdateAction<CartDiscount>> updateActions =
            CartDiscountSyncUtils.buildActions(CART_DISCOUNT_WITH_SHIPPING_TARGET,
                newCartDiscount, cartDiscountSyncOptions);

        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).containsExactly(ChangeName.of(newName));
    }

    @Test
    void buildActions_FromDraftsWithAllDifferentValues_ShouldBuildAllUpdateActions() {
        final LocalizedString newName =
            LocalizedString.of(Locale.GERMAN, "Neu Name", Locale.ENGLISH, "new name");
        final CartPredicate newCartDiscountPredicate = CartPredicate.of("1 = 1");
        final CartDiscountValue newCartDiscountValue =  CartDiscountValue.ofAbsolute(MoneyImpl.of(10, EUR));
        final CartDiscountTarget newCartDiscountTarget = LineItemsTarget.of("quantity > 1");
        final String newSortOrder = "0.3";
        final LocalizedString newDesc =
            LocalizedString.of(Locale.GERMAN, "Neu Beschreibung", Locale.ENGLISH, "new description");
        final ZonedDateTime newValidFrom = ZonedDateTime.parse("2019-01-01T00:00:00.000Z");
        final ZonedDateTime newValidUntil = ZonedDateTime.parse("2019-11-15T00:00:00.000Z");
        final boolean newIsActive = true;
        final boolean newRequireDiscountCode = true;
        final StackingMode newStackingMode = StackingMode.STOP_AFTER_THIS_DISCOUNT;

        final CartDiscountDraft newCartDiscount = CartDiscountDraftBuilder
            .of(newName,
                newCartDiscountPredicate,
                newCartDiscountValue,
                newCartDiscountTarget,
                newSortOrder,
                newRequireDiscountCode)
            .active(newIsActive)
            .description(newDesc)
            .validFrom(newValidFrom)
            .validUntil(newValidUntil)
            .stackingMode(newStackingMode)
            .build();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).build();
        final List<UpdateAction<CartDiscount>> updateActions =
            CartDiscountSyncUtils.buildActions(CART_DISCOUNT_WITH_SHIPPING_TARGET,
                newCartDiscount, cartDiscountSyncOptions);

        assertThat(updateActions).containsExactly(
            ChangeValue.of(newCartDiscountValue),
            ChangeCartPredicate.of(newCartDiscountPredicate),
            ChangeTarget.of(newCartDiscountTarget),
            ChangeIsActive.of(newIsActive),
            ChangeName.of(newName),
            SetDescription.of(newDesc),
            ChangeSortOrder.of(newSortOrder),
            ChangeRequiresDiscountCode.of(newRequireDiscountCode),
            SetValidFromAndUntil.of(newValidFrom, newValidUntil),
            ChangeStackingMode.of(newStackingMode)
        );
    }

}
