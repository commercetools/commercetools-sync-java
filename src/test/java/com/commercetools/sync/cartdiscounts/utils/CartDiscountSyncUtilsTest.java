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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CartDiscountSyncUtilsTest {

    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);

    private static final String KEY = "key";
    private static final LocalizedString NAME = LocalizedString.of(Locale.ENGLISH, "name");
    private static final LocalizedString DESC =
            LocalizedString.of(Locale.ENGLISH, "discount- get 10 percent");
    private static final String PREDICATE =
            "lineItemExists(sku = \"0123456789\" or sku = \"0246891213\") = true";
    private static final CartDiscountValue VALUE = CartDiscountValue.ofRelative(1000);
    private static final CartDiscountTarget TARGET = LineItemsTarget.ofAll();
    private static final ZonedDateTime JANUARY_FROM = ZonedDateTime.parse("2019-01-01T00:00:00.000Z");
    private static final ZonedDateTime JANUARY_UNTIL = ZonedDateTime.parse("2019-01-31T00:00:00.000Z");
    private static final String SORT_ORDER = "0.1";
    private static final Boolean IS_ACTIVE = false;
    private static final Boolean IS_REQUIRE_DISC_CODE = false;
    private static final StackingMode STACKING_MODE = StackingMode.STACKING;


    private CartDiscount mockCartDiscount;

    @BeforeEach
    void setup() {
        mockCartDiscount = mock(CartDiscount.class);
        when(mockCartDiscount.getKey()).thenReturn(KEY);
        when(mockCartDiscount.getName()).thenReturn(NAME);
        when(mockCartDiscount.getDescription()).thenReturn(DESC);
        when(mockCartDiscount.getCartPredicate()).thenReturn(PREDICATE);
        when(mockCartDiscount.getValue()).thenReturn(VALUE);
        when(mockCartDiscount.getTarget()).thenReturn(TARGET);
        when(mockCartDiscount.getValidFrom()).thenReturn(JANUARY_FROM);
        when(mockCartDiscount.getValidUntil()).thenReturn(JANUARY_UNTIL);
        when(mockCartDiscount.getSortOrder()).thenReturn(SORT_ORDER);
        when(mockCartDiscount.isActive()).thenReturn(IS_ACTIVE);
        when(mockCartDiscount.isRequiringDiscountCode()).thenReturn(IS_REQUIRE_DISC_CODE);
        when(mockCartDiscount.getStackingMode()).thenReturn(STACKING_MODE);
    }

    @Test
    void buildActions_FromDraftsWithDifferentNameValues_ShouldBuildUpdateActions() {
        final LocalizedString newName =
                LocalizedString.of(Locale.GERMAN, "Neu Name", Locale.ENGLISH, "new name");

        final CartDiscountDraft newCartDiscount = CartDiscountDraftBuilder
                .of(newName,
                        PREDICATE,
                        VALUE,
                        TARGET,
                        SORT_ORDER,
                        IS_ACTIVE)
                .key(KEY)
                .stackingMode(STACKING_MODE)
                .active(IS_REQUIRE_DISC_CODE)
                .description(DESC)
                .validFrom(JANUARY_FROM)
                .validUntil(JANUARY_UNTIL)
                .build();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).build();
        final List<UpdateAction<CartDiscount>> updateActions =
                CartDiscountSyncUtils.buildActions(mockCartDiscount,
                        newCartDiscount, cartDiscountSyncOptions);

        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).containsExactly(ChangeName.of(newName));
    }

    @Test
    void buildActions_FromDraftsWithAllDifferentValues_ShouldBuildAllUpdateActions() {
        final LocalizedString newName =
                LocalizedString.of(Locale.GERMAN, "Neu Name", Locale.ENGLISH, "new name");
        final CartPredicate newCartDiscountPredicate = CartPredicate.of("1 = 1");
        final CartDiscountValue newCartDiscountValue = CartDiscountValue.ofAbsolute(MoneyImpl.of(10, EUR));
        final CartDiscountTarget newCartDiscountTarget = LineItemsTarget.of("quantity > 1");
        final String newSortOrder = "0.3";
        final LocalizedString newDesc =
                LocalizedString.of(Locale.GERMAN, "Neu Beschreibung", Locale.ENGLISH, "new description");
        final ZonedDateTime newValidFrom = ZonedDateTime.parse("2019-11-01T00:00:00.000Z");
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
                CartDiscountSyncUtils.buildActions(mockCartDiscount,
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
