package com.commercetools.sync.sdk2.cartdiscounts.utils;

import static com.commercetools.sync.sdk2.cartdiscounts.utils.CartDiscountUpdateActionUtils.*;
import static com.commercetools.sync.sdk2.commons.helpers.DefaultCurrencyUnits.EUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountChangeCartPredicateActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeIsActiveActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeNameActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeRequiresDiscountCodeActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeSortOrderActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeStackingModeActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeTargetActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeValueActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountCustomLineItemsTarget;
import com.commercetools.api.models.cart_discount.CartDiscountCustomLineItemsTargetBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountLineItemsTargetBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountSetDescriptionActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountSetValidFromActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountSetValidFromAndUntilActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountSetValidUntilActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountShippingCostTargetBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountTarget;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.api.models.cart_discount.CartDiscountValue;
import com.commercetools.api.models.cart_discount.CartDiscountValueAbsoluteBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountValueAbsoluteDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueAbsoluteDraftBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountValueDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueGiftLineItem;
import com.commercetools.api.models.cart_discount.CartDiscountValueGiftLineItemBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountValueGiftLineItemDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueGiftLineItemDraftBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountValueRelativeBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountValueRelativeDraftBuilder;
import com.commercetools.api.models.cart_discount.MultiBuyCustomLineItemsTarget;
import com.commercetools.api.models.cart_discount.MultiBuyCustomLineItemsTargetBuilder;
import com.commercetools.api.models.cart_discount.MultiBuyLineItemsTarget;
import com.commercetools.api.models.cart_discount.MultiBuyLineItemsTargetBuilder;
import com.commercetools.api.models.cart_discount.SelectionMode;
import com.commercetools.api.models.cart_discount.StackingMode;
import com.commercetools.api.models.common.CentPrecisionMoneyBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.ProductReferenceBuilder;
import com.commercetools.api.models.product.ProductResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.helpers.DefaultCurrencyUnits;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CartDiscountUpdateActionUtilsTest {

  @Test
  void buildChangeValueUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue())
        .thenReturn(CartDiscountValueRelativeBuilder.of().permyriad(1000L).build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final CartDiscountValueDraft tenEuro =
        CartDiscountValueAbsoluteDraftBuilder.of()
            .money(
                CentPrecisionMoneyBuilder.of()
                    .centAmount(10L)
                    .currencyCode(EUR.getCurrencyCode())
                    .fractionDigits(0)
                    .build())
            .build();
    when(newCartDiscountDraft.getValue()).thenReturn(tenEuro);

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction)
        .contains(CartDiscountChangeValueActionBuilder.of().value(tenEuro).build());
  }

  @Test
  void buildChangeValueUpdateAction_WithDifferentRelativeValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue())
        .thenReturn(CartDiscountValueRelativeBuilder.of().permyriad(1000L).build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final CartDiscountValueDraft twentyPercent =
        CartDiscountValueRelativeDraftBuilder.of().permyriad(2000L).build();
    when(newCartDiscountDraft.getValue()).thenReturn(twentyPercent);

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction)
        .contains(CartDiscountChangeValueActionBuilder.of().value(twentyPercent).build());
  }

  @Test
  void buildChangeValueUpdateAction_WithSameRelativeValues_ShouldNotBuildUpdateAction() {
    final CartDiscountValue tenPercent =
        CartDiscountValueRelativeBuilder.of().permyriad(1000L).build();
    final CartDiscountValueDraft tenPercent2 =
        CartDiscountValueRelativeDraftBuilder.of().permyriad(1000L).build();

    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue()).thenReturn(tenPercent);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValue()).thenReturn(tenPercent2);

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction).isNotPresent();
  }

  @Test
  void buildChangeValueUpdateAction_WithOnlyNewGiftItemProductValue_ShouldBuildUpdateAction() {
    final CartDiscountValue relativeCartDiscountValue =
        CartDiscountValueRelativeBuilder.of().permyriad(1000L).build();

    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue()).thenReturn(relativeCartDiscountValue);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final CartDiscountValueGiftLineItemDraft newGiftLineItemCartDiscountValue =
        CartDiscountValueGiftLineItemDraftBuilder.of()
            .product(ProductResourceIdentifierBuilder.of().id("product-2").build())
            .variantId(1L)
            .build();
    when(newCartDiscountDraft.getValue()).thenReturn(newGiftLineItemCartDiscountValue);

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction)
        .contains(
            CartDiscountChangeValueActionBuilder.of()
                .value(newGiftLineItemCartDiscountValue)
                .build());
  }

  @Test
  void buildChangeValueUpdateAction_WithOnlyOldGiftItemProductValue_ShouldBuildUpdateAction() {
    final CartDiscountValueGiftLineItem giftLineItemCartDiscountValue =
        CartDiscountValueGiftLineItemBuilder.of()
            .product(ProductReferenceBuilder.of().id("product-2").build())
            .variantId(1L)
            .build();
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue()).thenReturn(giftLineItemCartDiscountValue);

    final CartDiscountValueDraft relativeCartDiscountValue =
        CartDiscountValueRelativeDraftBuilder.of().permyriad(1000L).build();
    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValue()).thenReturn(relativeCartDiscountValue);

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction)
        .contains(
            CartDiscountChangeValueActionBuilder.of().value(relativeCartDiscountValue).build());
  }

  @Test
  void buildChangeValueUpdateAction_WithDifferentGiftItemProductValue_ShouldBuildUpdateAction() {
    final CartDiscountValueGiftLineItem oldGiftLineItemCartDiscountValue =
        CartDiscountValueGiftLineItemBuilder.of()
            .product(ProductReferenceBuilder.of().id("product-1").build())
            .variantId(1L)
            .build();

    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue()).thenReturn(oldGiftLineItemCartDiscountValue);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final CartDiscountValueGiftLineItemDraft newGiftLineItemCartDiscountValue =
        CartDiscountValueGiftLineItemDraftBuilder.of()
            .product(ProductResourceIdentifierBuilder.of().id("product-2").build())
            .variantId(1L)
            .build();
    when(newCartDiscountDraft.getValue()).thenReturn(newGiftLineItemCartDiscountValue);

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction)
        .contains(
            CartDiscountChangeValueActionBuilder.of()
                .value(newGiftLineItemCartDiscountValue)
                .build());
  }

  @Test
  void
      buildChangeValueUpdateAction_WithDifferentGiftItemProductVariantValue_ShouldBuildUpdateAction() {
    final CartDiscountValueGiftLineItem oldGiftLineItemCartDiscountValue =
        CartDiscountValueGiftLineItemBuilder.of()
            .product(productReferenceBuilder -> productReferenceBuilder.id("productId"))
            .variantId(1L)
            .build();

    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue()).thenReturn(oldGiftLineItemCartDiscountValue);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final CartDiscountValueGiftLineItemDraft newGiftLineItemCartDiscountValue =
        CartDiscountValueGiftLineItemDraftBuilder.of()
            .product(productReferenceBuilder -> productReferenceBuilder.id("productId"))
            .variantId(2L)
            .build();
    when(newCartDiscountDraft.getValue()).thenReturn(newGiftLineItemCartDiscountValue);

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction)
        .contains(
            CartDiscountChangeValueActionBuilder.of()
                .value(newGiftLineItemCartDiscountValue)
                .build());
  }

  @Test
  void
      buildChangeValueUpdateAction_WithDifferentGiftItemSupplyChannelValue_ShouldBuildUpdateAction() {
    final CartDiscountValueGiftLineItem oldGiftLineItemCartDiscountValue =
        CartDiscountValueGiftLineItemBuilder.of()
            .product(productReferenceBuilder -> productReferenceBuilder.id("productId"))
            .variantId(1L)
            .distributionChannel(
                channelReferenceBuilder -> channelReferenceBuilder.id("supplyChannel-1"))
            .build();

    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue()).thenReturn(oldGiftLineItemCartDiscountValue);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final CartDiscountValueGiftLineItemDraft newGiftLineItemCartDiscountValue =
        CartDiscountValueGiftLineItemDraftBuilder.of()
            .product(productReferenceBuilder -> productReferenceBuilder.id("productId"))
            .variantId(1L)
            .distributionChannel(
                channelResourceIdentifierBuilder ->
                    channelResourceIdentifierBuilder.id("supplyChannel-2"))
            .build();
    when(newCartDiscountDraft.getValue()).thenReturn(newGiftLineItemCartDiscountValue);

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction)
        .contains(
            CartDiscountChangeValueActionBuilder.of()
                .value(newGiftLineItemCartDiscountValue)
                .build());
  }

  @Test
  void
      buildChangeValueUpdateAction_WithDifferentGiftItemDistributionChannelValue_ShouldBuildUpdateAction() {
    final CartDiscountValueGiftLineItem oldGiftLineItemCartDiscountValue =
        CartDiscountValueGiftLineItemBuilder.of()
            .product(productReferenceBuilder -> productReferenceBuilder.id("productId"))
            .variantId(1L)
            .distributionChannel(
                channelReferenceBuilder -> channelReferenceBuilder.id("dist-channel-1"))
            .build();

    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue()).thenReturn(oldGiftLineItemCartDiscountValue);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final CartDiscountValueGiftLineItemDraft newGiftLineItemCartDiscountValue =
        CartDiscountValueGiftLineItemDraftBuilder.of()
            .product(productReferenceBuilder -> productReferenceBuilder.id("productId"))
            .variantId(1L)
            .distributionChannel(
                channelReferenceBuilder -> channelReferenceBuilder.id("dist-channel-2"))
            .build();
    when(newCartDiscountDraft.getValue()).thenReturn(newGiftLineItemCartDiscountValue);

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction)
        .contains(
            CartDiscountChangeValueActionBuilder.of()
                .value(newGiftLineItemCartDiscountValue)
                .build());
  }

  @Test
  void buildChangeValueUpdateAction_WithSameGiftItemValue_ShouldNotBuildUpdateAction() {
    final CartDiscountValueGiftLineItem giftLineItemCartDiscountValue =
        CartDiscountValueGiftLineItemBuilder.of()
            .product(productReferenceBuilder -> productReferenceBuilder.id("productId"))
            .variantId(1L)
            .build();

    final CartDiscountValueGiftLineItemDraft giftLineItemCartDiscountValue2 =
        CartDiscountValueGiftLineItemDraftBuilder.of()
            .product(
                productResourceIdentifierBuilder ->
                    productResourceIdentifierBuilder.id("productId"))
            .variantId(1L)
            .build();

    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue()).thenReturn(giftLineItemCartDiscountValue);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValue()).thenReturn(giftLineItemCartDiscountValue2);

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction).isNotPresent();
  }

  @Test
  void buildChangeValueUpdateAction_WithSameAbsoluteValues_ShouldNotBuildUpdateAction() {
    final CartDiscountValue values =
        CartDiscountValueAbsoluteBuilder.of()
            .money(
                CentPrecisionMoneyBuilder.of()
                    .centAmount(10L)
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .fractionDigits(0)
                    .build(),
                CentPrecisionMoneyBuilder.of()
                    .centAmount(10L)
                    .currencyCode(DefaultCurrencyUnits.USD.getCurrencyCode())
                    .fractionDigits(0)
                    .build())
            .build();
    final CartDiscountValueDraft values2 =
        CartDiscountValueAbsoluteDraftBuilder.of()
            .money(
                CentPrecisionMoneyBuilder.of()
                    .centAmount(10L)
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .fractionDigits(0)
                    .build(),
                CentPrecisionMoneyBuilder.of()
                    .centAmount(10L)
                    .currencyCode(DefaultCurrencyUnits.USD.getCurrencyCode())
                    .fractionDigits(0)
                    .build())
            .build();

    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue()).thenReturn(values);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValue()).thenReturn(values2);

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction).isNotPresent();
  }

  @Test
  void buildChangeValueUpdateAction_WithDifferentAbsoluteValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue())
        .thenReturn(
            CartDiscountValueAbsoluteBuilder.of()
                .money(
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(10L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                        .build())
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final CartDiscountValueDraft fiftyEuro =
        CartDiscountValueAbsoluteDraftBuilder.of()
            .money(
                CentPrecisionMoneyBuilder.of()
                    .centAmount(50L)
                    .fractionDigits(0)
                    .currencyCode(DefaultCurrencyUnits.USD.getCurrencyCode())
                    .build())
            .build();

    when(newCartDiscountDraft.getValue()).thenReturn(fiftyEuro);

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction)
        .contains(CartDiscountChangeValueActionBuilder.of().value(fiftyEuro).build());
  }

  @Test
  void buildChangeValueUpdateAction_WithNewDuplicatesWithMissingAmount_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue())
        .thenReturn(
            CartDiscountValueAbsoluteBuilder.of()
                .money(
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(10L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                        .build(),
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(50L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                        .build(),
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(30L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                        .build())
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final CartDiscountValueDraft newValue =
        CartDiscountValueAbsoluteDraftBuilder.of()
            .money(
                CentPrecisionMoneyBuilder.of()
                    .centAmount(10L)
                    .fractionDigits(0)
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build(),
                CentPrecisionMoneyBuilder.of()
                    .centAmount(50L)
                    .fractionDigits(0)
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build(),
                CentPrecisionMoneyBuilder.of()
                    .centAmount(50L)
                    .fractionDigits(0)
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .build();
    when(newCartDiscountDraft.getValue()).thenReturn(newValue);

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction)
        .contains(CartDiscountChangeValueActionBuilder.of().value(newValue).build());
  }

  @Test
  void
      buildChangeValueUpdateAction_WithNewDuplicatesWithExtraAndMissingAmount_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue())
        .thenReturn(
            CartDiscountValueAbsoluteBuilder.of()
                .money(
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(20L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                        .build(),
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(50L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                        .build(),
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(30L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                        .build())
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final CartDiscountValueAbsoluteDraft newValue =
        CartDiscountValueAbsoluteDraftBuilder.of()
            .money(
                CentPrecisionMoneyBuilder.of()
                    .centAmount(20L)
                    .fractionDigits(0)
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build(),
                CentPrecisionMoneyBuilder.of()
                    .centAmount(50L)
                    .fractionDigits(0)
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build(),
                CentPrecisionMoneyBuilder.of()
                    .centAmount(50L)
                    .fractionDigits(0)
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .build();
    when(newCartDiscountDraft.getValue()).thenReturn(newValue);

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction)
        .contains(CartDiscountChangeValueActionBuilder.of().value(newValue).build());
  }

  @Test
  void
      buildChangeValueUpdateAction_WithSameAbsoluteAmountsWithDifferentOrder_ShouldNotBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue())
        .thenReturn(
            CartDiscountValueAbsoluteBuilder.of()
                .money(
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(10L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                        .build(),
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(10L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.USD.getCurrencyCode())
                        .build())
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final CartDiscountValueAbsoluteDraft sameValuesWithDifferentOrder =
        CartDiscountValueAbsoluteDraftBuilder.of()
            .money(
                CentPrecisionMoneyBuilder.of()
                    .centAmount(10L)
                    .fractionDigits(0)
                    .currencyCode(DefaultCurrencyUnits.USD.getCurrencyCode())
                    .build(),
                CentPrecisionMoneyBuilder.of()
                    .centAmount(10L)
                    .fractionDigits(0)
                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                    .build())
            .build();
    when(newCartDiscountDraft.getValue()).thenReturn(sameValuesWithDifferentOrder);

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction).isNotPresent();
  }

  @Test
  void buildChangeValueUpdateAction_WithFewerAbsoluteAmounts_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue())
        .thenReturn(
            CartDiscountValueAbsoluteBuilder.of()
                .money(
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(10L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                        .build(),
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(10L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.USD.getCurrencyCode())
                        .build())
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValue())
        .thenReturn(
            CartDiscountValueAbsoluteDraftBuilder.of()
                .money(
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(10L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                        .build())
                .build());

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction)
        .contains(
            CartDiscountChangeValueActionBuilder.of()
                .value(
                    CartDiscountValueAbsoluteDraftBuilder.of()
                        .money(
                            CentPrecisionMoneyBuilder.of()
                                .centAmount(10L)
                                .fractionDigits(0)
                                .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                                .build())
                        .build())
                .build());
  }

  @Test
  void buildChangeValueUpdateAction_WithAdditionalAbsoluteAmounts_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue())
        .thenReturn(
            CartDiscountValueAbsoluteBuilder.of()
                .money(
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(10L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                        .build())
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValue())
        .thenReturn(
            CartDiscountValueAbsoluteDraftBuilder.of()
                .money(
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(10L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                        .build(),
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(10L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.USD.getCurrencyCode())
                        .build())
                .build());

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction)
        .contains(
            CartDiscountChangeValueActionBuilder.of()
                .value(
                    CartDiscountValueAbsoluteDraftBuilder.of()
                        .money(
                            CentPrecisionMoneyBuilder.of()
                                .centAmount(10L)
                                .fractionDigits(0)
                                .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                                .build(),
                            CentPrecisionMoneyBuilder.of()
                                .centAmount(10L)
                                .fractionDigits(0)
                                .currencyCode(DefaultCurrencyUnits.USD.getCurrencyCode())
                                .build())
                        .build())
                .build());
  }

  @Test
  void buildChangeValueUpdateAction_WithSomeNullNewAbsoluteAmounts_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValue())
        .thenReturn(
            CartDiscountValueAbsoluteBuilder.of()
                .money(
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(10L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.USD.getCurrencyCode())
                        .build())
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValue())
        .thenReturn(
            CartDiscountValueAbsoluteDraftBuilder.of()
                .money(
                    CentPrecisionMoneyBuilder.of()
                        .centAmount(10L)
                        .fractionDigits(0)
                        .currencyCode(DefaultCurrencyUnits.USD.getCurrencyCode())
                        .build(),
                    null)
                .build());

    final Optional<CartDiscountUpdateAction> changeValueUpdateAction =
        buildChangeValueUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeValueUpdateAction)
        .contains(
            CartDiscountChangeValueActionBuilder.of()
                .value(
                    CartDiscountValueAbsoluteDraftBuilder.of()
                        .money(
                            CentPrecisionMoneyBuilder.of()
                                .centAmount(10L)
                                .fractionDigits(0)
                                .currencyCode(DefaultCurrencyUnits.USD.getCurrencyCode())
                                .build(),
                            null)
                        .build())
                .build());
  }

  @Test
  void buildChangeCartPredicateUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getCartPredicate())
        .thenReturn("totalPrice = \"10.00 EUR\" and (shippingInfo.shippingMethodName = \"FEDEX\")");

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final String newCartPredicate = "1 = 1";
    when(newCartDiscountDraft.getCartPredicate()).thenReturn(newCartPredicate);

    final Optional<CartDiscountUpdateAction> changeCartPredicateUpdateAction =
        buildChangeCartPredicateUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeCartPredicateUpdateAction)
        .contains(
            CartDiscountChangeCartPredicateActionBuilder.of()
                .cartPredicate(newCartPredicate)
                .build());
  }

  @Test
  void buildChangeCartPredicateUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final String cartPredicate =
        "totalPrice = \"10.00 EUR\" and (shippingInfo.shippingMethodName = \"FEDEX\")";
    final String cartPredicate2 =
        "totalPrice = \"10.00 EUR\" and (shippingInfo.shippingMethodName = \"FEDEX\")";

    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getCartPredicate()).thenReturn(cartPredicate);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getCartPredicate()).thenReturn(cartPredicate2);

    final Optional<CartDiscountUpdateAction> changePredicateUpdateAction =
        buildChangeCartPredicateUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changePredicateUpdateAction).isNotPresent();
  }

  @Test
  void buildChangeTargetUpdateAction_WithDifferentLineItemTargetValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(CartDiscountLineItemsTargetBuilder.of().predicate("").build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final CartDiscountTarget cartDiscountTarget =
        CartDiscountLineItemsTargetBuilder.of().predicate("quantity > 0").build();
    when(newCartDiscountDraft.getTarget()).thenReturn(cartDiscountTarget);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction)
        .contains(CartDiscountChangeTargetActionBuilder.of().target(cartDiscountTarget).build());
  }

  @Test
  void buildChangeTargetUpdateAction_WithSameLineItemTargetValues_ShouldNotBuildUpdateAction() {
    final CartDiscountTarget cartDiscountTarget =
        CartDiscountLineItemsTargetBuilder.of().predicate("quantity > 0").build();
    final CartDiscountTarget cartDiscountTarget2 =
        CartDiscountLineItemsTargetBuilder.of().predicate("quantity > 0").build();

    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget()).thenReturn(cartDiscountTarget);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getTarget()).thenReturn(cartDiscountTarget2);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction).isNotPresent();
  }

  @Test
  void buildChangeTargetUpdateAction_WithDifferentCustomLineItemValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(
            CartDiscountCustomLineItemsTargetBuilder.of().predicate("money = \"100 EUR\"").build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final CartDiscountTarget cartDiscountTarget =
        CartDiscountCustomLineItemsTargetBuilder.of().predicate("1 = 1").build();
    when(newCartDiscountDraft.getTarget()).thenReturn(cartDiscountTarget);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction)
        .contains(CartDiscountChangeTargetActionBuilder.of().target(cartDiscountTarget).build());
  }

  @Test
  void buildChangeTargetUpdateAction_WithSameCustomLineItemValues_ShouldNotBuildUpdateAction() {
    final CartDiscountCustomLineItemsTarget customLineItemsTarget =
        CartDiscountCustomLineItemsTargetBuilder.of().predicate("money = \"100 EUR\"").build();
    final CartDiscountCustomLineItemsTarget customLineItemsTarget2 =
        CartDiscountCustomLineItemsTargetBuilder.of().predicate("money = \"100 EUR\"").build();

    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget()).thenReturn(customLineItemsTarget);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getTarget()).thenReturn(customLineItemsTarget2);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction).isNotPresent();
  }

  @Test
  void buildChangeTargetUpdateAction_WithLineItemAndShippingTargetValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(CartDiscountLineItemsTargetBuilder.of().predicate("").build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final CartDiscountTarget cartDiscountTarget =
        CartDiscountShippingCostTargetBuilder.of().build();
    when(newCartDiscountDraft.getTarget()).thenReturn(cartDiscountTarget);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction)
        .contains(CartDiscountChangeTargetActionBuilder.of().target(cartDiscountTarget).build());
  }

  @Test
  void buildChangeTargetUpdateAction_WithBothShippingTargetValues_ShouldNotBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(CartDiscountShippingCostTargetBuilder.of().build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getTarget())
        .thenReturn(CartDiscountShippingCostTargetBuilder.of().build());

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction).isNotPresent();
  }

  @Test
  void
      buildChangeTargetUpdateAction_WithDifferentMultiBuyLineItemsTargetValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(
            MultiBuyLineItemsTargetBuilder.of()
                .predicate("quantity > 0")
                .triggerQuantity(6)
                .discountedQuantity(2)
                .selectionMode(SelectionMode.CHEAPEST)
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final MultiBuyLineItemsTarget newTarget =
        MultiBuyLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .build();
    when(newCartDiscountDraft.getTarget()).thenReturn(newTarget);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction)
        .contains(CartDiscountChangeTargetActionBuilder.of().target(newTarget).build());
  }

  @Test
  void buildChangeTargetUpdateAction_WithDifferentTargetPredicate_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(
            MultiBuyLineItemsTargetBuilder.of()
                .predicate("quantity > 1")
                .triggerQuantity(6)
                .discountedQuantity(3)
                .selectionMode(SelectionMode.MOST_EXPENSIVE)
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final MultiBuyLineItemsTarget newTarget =
        MultiBuyLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .build();
    when(newCartDiscountDraft.getTarget()).thenReturn(newTarget);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction)
        .contains(CartDiscountChangeTargetActionBuilder.of().target(newTarget).build());
  }

  @Test
  void buildChangeTargetUpdateAction_WithDifferentTriggerQuantity_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(
            MultiBuyLineItemsTargetBuilder.of()
                .predicate("quantity > 0")
                .triggerQuantity(5)
                .discountedQuantity(3)
                .selectionMode(SelectionMode.MOST_EXPENSIVE)
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final MultiBuyLineItemsTarget newTarget =
        MultiBuyLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .build();
    when(newCartDiscountDraft.getTarget()).thenReturn(newTarget);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction)
        .contains(CartDiscountChangeTargetActionBuilder.of().target(newTarget).build());
  }

  @Test
  void buildChangeTargetUpdateAction_WithDifferentDiscountedQuantity_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(
            MultiBuyLineItemsTargetBuilder.of()
                .predicate("quantity > 0")
                .triggerQuantity(6)
                .discountedQuantity(2)
                .selectionMode(SelectionMode.MOST_EXPENSIVE)
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final MultiBuyLineItemsTarget newTarget =
        MultiBuyLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .build();
    when(newCartDiscountDraft.getTarget()).thenReturn(newTarget);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction)
        .contains(CartDiscountChangeTargetActionBuilder.of().target(newTarget).build());
  }

  @Test
  void buildChangeTargetUpdateAction_WithDifferentSelectionMode_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(
            MultiBuyLineItemsTargetBuilder.of()
                .predicate("quantity > 0")
                .triggerQuantity(6)
                .discountedQuantity(3)
                .selectionMode(SelectionMode.CHEAPEST)
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final MultiBuyLineItemsTarget newTarget =
        MultiBuyLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .build();
    when(newCartDiscountDraft.getTarget()).thenReturn(newTarget);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction)
        .contains(CartDiscountChangeTargetActionBuilder.of().target(newTarget).build());
  }

  @Test
  void buildChangeTargetUpdateAction_WithDifferentMaxOccurrence_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(
            MultiBuyLineItemsTargetBuilder.of()
                .predicate("quantity > 0")
                .triggerQuantity(6)
                .discountedQuantity(3)
                .selectionMode(SelectionMode.MOST_EXPENSIVE)
                .maxOccurrence(1)
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final MultiBuyLineItemsTarget newTarget =
        MultiBuyLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .maxOccurrence(2)
            .build();
    when(newCartDiscountDraft.getTarget()).thenReturn(newTarget);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction)
        .contains(CartDiscountChangeTargetActionBuilder.of().target(newTarget).build());
  }

  @Test
  void
      buildChangeTargetUpdateAction_WithSameMultiBuyLineItemsTargetValues_ShouldNotBuildUpdateAction() {
    final MultiBuyLineItemsTarget target =
        MultiBuyLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .build();
    final MultiBuyLineItemsTarget target2 =
        MultiBuyLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .build();

    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget()).thenReturn(target);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getTarget()).thenReturn(target2);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction).isNotPresent();
  }

  @Test
  void
      buildChangeTargetUpdateAction_WithDifferentMultiBuyCustomLineItemsTargetValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(
            MultiBuyCustomLineItemsTargetBuilder.of()
                .predicate("quantity > 0")
                .triggerQuantity(6)
                .discountedQuantity(2)
                .selectionMode(SelectionMode.CHEAPEST)
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final MultiBuyCustomLineItemsTarget newTarget =
        MultiBuyCustomLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .build();
    when(newCartDiscountDraft.getTarget()).thenReturn(newTarget);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction)
        .contains(CartDiscountChangeTargetActionBuilder.of().target(newTarget).build());
  }

  @Test
  void buildChangeTargetUpdateAction_WithDifferentCustomTargetPredicate_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(
            MultiBuyCustomLineItemsTargetBuilder.of()
                .predicate("quantity > 1")
                .triggerQuantity(6)
                .discountedQuantity(3)
                .selectionMode(SelectionMode.MOST_EXPENSIVE)
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final MultiBuyCustomLineItemsTarget newTarget =
        MultiBuyCustomLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .build();
    when(newCartDiscountDraft.getTarget()).thenReturn(newTarget);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction)
        .contains(CartDiscountChangeTargetActionBuilder.of().target(newTarget).build());
  }

  @Test
  void buildChangeTargetUpdateAction_WithDifferentCustomTriggerQuantity_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(
            MultiBuyCustomLineItemsTargetBuilder.of()
                .predicate("quantity > 0")
                .triggerQuantity(5)
                .discountedQuantity(3)
                .selectionMode(SelectionMode.MOST_EXPENSIVE)
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final MultiBuyCustomLineItemsTarget newTarget =
        MultiBuyCustomLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .build();

    when(newCartDiscountDraft.getTarget()).thenReturn(newTarget);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction)
        .contains(CartDiscountChangeTargetActionBuilder.of().target(newTarget).build());
  }

  @Test
  void
      buildChangeTargetUpdateAction_WithDifferentCustomDiscountedQuantity_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(
            MultiBuyCustomLineItemsTargetBuilder.of()
                .predicate("quantity > 0")
                .triggerQuantity(6)
                .discountedQuantity(2)
                .selectionMode(SelectionMode.MOST_EXPENSIVE)
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final MultiBuyCustomLineItemsTarget newTarget =
        MultiBuyCustomLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .build();
    when(newCartDiscountDraft.getTarget()).thenReturn(newTarget);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction)
        .contains(CartDiscountChangeTargetActionBuilder.of().target(newTarget).build());
  }

  @Test
  void buildChangeTargetUpdateAction_WithDifferentCustomSelectionMode_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(
            MultiBuyCustomLineItemsTargetBuilder.of()
                .predicate("quantity > 0")
                .triggerQuantity(6)
                .discountedQuantity(3)
                .selectionMode(SelectionMode.CHEAPEST)
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final MultiBuyCustomLineItemsTarget newTarget =
        MultiBuyCustomLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .build();
    when(newCartDiscountDraft.getTarget()).thenReturn(newTarget);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction)
        .contains(CartDiscountChangeTargetActionBuilder.of().target(newTarget).build());
  }

  @Test
  void buildChangeTargetUpdateAction_WithDifferentCustomMaxOccurrence_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget())
        .thenReturn(
            MultiBuyCustomLineItemsTargetBuilder.of()
                .predicate("quantity > 0")
                .triggerQuantity(6)
                .discountedQuantity(3)
                .selectionMode(SelectionMode.MOST_EXPENSIVE)
                .maxOccurrence(1)
                .build());

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final MultiBuyCustomLineItemsTarget newTarget =
        MultiBuyCustomLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .maxOccurrence(2)
            .build();

    when(newCartDiscountDraft.getTarget()).thenReturn(newTarget);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction)
        .contains(CartDiscountChangeTargetActionBuilder.of().target(newTarget).build());
  }

  @Test
  void
      buildChangeTargetUpdateAction_WithSameMultiBuyCustomLineItemsTargetValues_ShouldNotBuildUpdateAction() {
    final MultiBuyCustomLineItemsTarget target =
        MultiBuyCustomLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .build();
    final MultiBuyCustomLineItemsTarget target2 =
        MultiBuyCustomLineItemsTargetBuilder.of()
            .predicate("quantity > 0")
            .triggerQuantity(6)
            .discountedQuantity(3)
            .selectionMode(SelectionMode.MOST_EXPENSIVE)
            .build();

    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget()).thenReturn(target);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getTarget()).thenReturn(target2);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction).isNotPresent();
  }

  @Test
  void buildChangeTargetUpdateAction_WithBothNullTargetValues_ShouldNotBuildUpdateAction() {
    // this just can be happen if the value type is gift line item
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getTarget()).thenReturn(null);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getTarget()).thenReturn(null);

    final Optional<CartDiscountUpdateAction> changeTargetUpdateAction =
        buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeTargetUpdateAction).isNotPresent();
  }

  @Test
  void buildChangeIsActiveUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getIsActive()).thenReturn(false);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getIsActive()).thenReturn(true);

    final Optional<CartDiscountUpdateAction> changeIsActiveUpdateAction =
        buildChangeIsActiveUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeIsActiveUpdateAction)
        .contains(CartDiscountChangeIsActiveActionBuilder.of().isActive(true).build());
  }

  @Test
  void buildChangeIsActiveUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getIsActive()).thenReturn(false);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getIsActive()).thenReturn(false);

    final Optional<CartDiscountUpdateAction> changeIsActiveUpdateAction =
        buildChangeIsActiveUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeIsActiveUpdateAction).isNotPresent();
  }

  @Test
  void buildChangeIsActiveUpdateAction_WithOnlyNullNewIsActiveValues_ShouldNotBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getIsActive()).thenReturn(true);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getIsActive()).thenReturn(null);

    final Optional<CartDiscountUpdateAction> changeIsActiveUpdateAction =
        buildChangeIsActiveUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeIsActiveUpdateAction).isNotPresent();
  }

  @Test
  void
      buildChangeIsActiveUpdateAction_WithOnlyNullNewIsActiveAndFalseOldIsActive_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getIsActive()).thenReturn(false);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getIsActive()).thenReturn(null);

    final Optional<CartDiscountUpdateAction> changeIsActiveUpdateAction =
        buildChangeIsActiveUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeIsActiveUpdateAction)
        .contains(CartDiscountChangeIsActiveActionBuilder.of().isActive(true).build());
  }

  @Test
  void buildChangeNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getName()).thenReturn(LocalizedString.ofEnglish("cart-discount-1"));

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final LocalizedString name = LocalizedString.ofEnglish("newName");
    when(newCartDiscountDraft.getName()).thenReturn(name);

    final Optional<CartDiscountUpdateAction> changeNameUpdateAction =
        buildChangeNameUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeNameUpdateAction)
        .contains(CartDiscountChangeNameActionBuilder.of().name(name).build());
  }

  @Test
  void buildChangeNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final LocalizedString name = LocalizedString.ofEnglish("name");
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getName()).thenReturn(name);

    final LocalizedString name2 = LocalizedString.ofEnglish("name");
    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getName()).thenReturn(name2);

    final Optional<CartDiscountUpdateAction> changeNameUpdateAction =
        buildChangeNameUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeNameUpdateAction).isNotPresent();
  }

  @Test
  void buildSetDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getDescription())
        .thenReturn(LocalizedString.ofEnglish("cart-discount-1-desc"));

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final LocalizedString description = LocalizedString.ofEnglish("new-description");
    when(newCartDiscountDraft.getDescription()).thenReturn(description);

    final Optional<CartDiscountUpdateAction> setDescriptionUpdateAction =
        buildSetDescriptionUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setDescriptionUpdateAction)
        .contains(CartDiscountSetDescriptionActionBuilder.of().description(description).build());
  }

  @Test
  void buildSetDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final LocalizedString description = LocalizedString.ofEnglish("cart-discount-1-desc");
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getDescription()).thenReturn(description);

    final LocalizedString description2 = LocalizedString.ofEnglish("cart-discount-1-desc");
    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getDescription()).thenReturn(description2);

    final Optional<CartDiscountUpdateAction> setDescriptionUpdateAction =
        buildSetDescriptionUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setDescriptionUpdateAction).isNotPresent();
  }

  @Test
  void buildSetDescriptionUpdateAction_WithOnlyNullNewDescription_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getDescription())
        .thenReturn(LocalizedString.ofEnglish("cart-discount-1-desc"));

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getDescription()).thenReturn(null);

    final Optional<CartDiscountUpdateAction> setDescriptionUpdateAction =
        buildSetDescriptionUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setDescriptionUpdateAction)
        .contains(
            CartDiscountSetDescriptionActionBuilder.of()
                .description((LocalizedString) null)
                .build());
  }

  @Test
  void buildSetDescriptionUpdateAction_WithOnlyNullOldDescription_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getDescription()).thenReturn(null);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final LocalizedString newDesc = LocalizedString.ofEnglish("new-desc");
    when(newCartDiscountDraft.getDescription()).thenReturn(newDesc);

    final Optional<CartDiscountUpdateAction> setDescriptionUpdateAction =
        buildSetDescriptionUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setDescriptionUpdateAction)
        .contains(CartDiscountSetDescriptionActionBuilder.of().description(newDesc).build());
  }

  @Test
  void buildSetDescriptionUpdateAction_WithBothNullDescriptionValues_ShouldNotBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getDescription()).thenReturn(null);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getDescription()).thenReturn(null);

    final Optional<CartDiscountUpdateAction> setDescriptionUpdateAction =
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

    final Optional<CartDiscountUpdateAction> changeChangeSortOrderUpdateAction =
        buildChangeSortOrderUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeChangeSortOrderUpdateAction)
        .contains(CartDiscountChangeSortOrderActionBuilder.of().sortOrder(sortOrder).build());
  }

  @Test
  void buildChangeSortOrderUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final String sortOrder = "0.1";
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getSortOrder()).thenReturn(sortOrder);

    final String sortOrder2 = "0.1";
    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getSortOrder()).thenReturn(sortOrder2);

    final Optional<CartDiscountUpdateAction> changeChangeSortOrderUpdateAction =
        buildChangeSortOrderUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeChangeSortOrderUpdateAction).isNotPresent();
  }

  @Test
  void buildChangeRequiresDiscountCodeUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getRequiresDiscountCode()).thenReturn(true);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getRequiresDiscountCode()).thenReturn(false);

    final Optional<CartDiscountUpdateAction> changeRequiresDiscountCodeUpdateAction =
        buildChangeRequiresDiscountCodeUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeRequiresDiscountCodeUpdateAction)
        .contains(
            CartDiscountChangeRequiresDiscountCodeActionBuilder.of()
                .requiresDiscountCode(false)
                .build());
  }

  @Test
  void buildChangeRequiresDiscountCodeUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getRequiresDiscountCode()).thenReturn(true);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getRequiresDiscountCode()).thenReturn(true);

    final Optional<CartDiscountUpdateAction> changeRequiresDiscountCodeUpdateAction =
        buildChangeRequiresDiscountCodeUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeRequiresDiscountCodeUpdateAction).isNotPresent();
  }

  @Test
  void
      buildChangeRequiresDiscountCodeUpdateAction_WithOnlyNullNewRequiresDiscountCode_ShouldNotBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getRequiresDiscountCode()).thenReturn(false);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getRequiresDiscountCode()).thenReturn(null);

    final Optional<CartDiscountUpdateAction> changeRequiresDiscountCodeUpdateAction =
        buildChangeRequiresDiscountCodeUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeRequiresDiscountCodeUpdateAction).isNotPresent();
  }

  @Test
  void
      buildChangeRequiresDiscountCodeUpdateAction_WithOnlyNullNewReqDisCodeAndTrueOldVal_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getRequiresDiscountCode()).thenReturn(true);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getRequiresDiscountCode()).thenReturn(null);

    final Optional<CartDiscountUpdateAction> changeRequiresDiscountCodeUpdateAction =
        buildChangeRequiresDiscountCodeUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeRequiresDiscountCodeUpdateAction)
        .contains(
            CartDiscountChangeRequiresDiscountCodeActionBuilder.of()
                .requiresDiscountCode(false)
                .build());
  }

  @Test
  void buildChangeStackingModeUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getStackingMode()).thenReturn(StackingMode.STOP_AFTER_THIS_DISCOUNT);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getStackingMode()).thenReturn(StackingMode.STACKING);

    final Optional<CartDiscountUpdateAction> changeStackingModeUpdateAction =
        buildChangeStackingModeUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeStackingModeUpdateAction)
        .contains(
            CartDiscountChangeStackingModeActionBuilder.of()
                .stackingMode(StackingMode.STACKING)
                .build());
  }

  @Test
  void buildChangeStackingModeUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getStackingMode()).thenReturn(StackingMode.STOP_AFTER_THIS_DISCOUNT);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);

    when(newCartDiscountDraft.getStackingMode()).thenReturn(StackingMode.STOP_AFTER_THIS_DISCOUNT);

    final Optional<CartDiscountUpdateAction> changeStackingModeUpdateAction =
        buildChangeStackingModeUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeStackingModeUpdateAction).isNotPresent();
  }

  @Test
  void
      buildChangeStackingModeUpdateAction_WithOnlyNullNewStackingMode_ShouldNotBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getStackingMode()).thenReturn(StackingMode.STACKING);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getStackingMode()).thenReturn(null);

    final Optional<CartDiscountUpdateAction> changeStackingModeUpdateAction =
        buildChangeStackingModeUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeStackingModeUpdateAction).isNotPresent();
  }

  @Test
  void
      buildChangeStackingModeUpdateAction_WithOnlyNullNewModeAndStopAfterDiscountOldMode_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getStackingMode()).thenReturn(StackingMode.STOP_AFTER_THIS_DISCOUNT);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getStackingMode()).thenReturn(null);

    final Optional<CartDiscountUpdateAction> changeStackingModeUpdateAction =
        buildChangeStackingModeUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(changeStackingModeUpdateAction)
        .contains(
            CartDiscountChangeStackingModeActionBuilder.of()
                .stackingMode(StackingMode.STACKING)
                .build());
  }

  @Test
  void buildSetValidFromUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidFrom())
        .thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final ZonedDateTime now = ZonedDateTime.now();
    when(newCartDiscountDraft.getValidFrom()).thenReturn(now);

    final Optional<CartDiscountUpdateAction> setValidFromUpdateAction =
        buildSetValidFromUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidFromUpdateAction)
        .contains(CartDiscountSetValidFromActionBuilder.of().validFrom(now).build());
  }

  @Test
  void buildSetValidFromUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final ZonedDateTime validFrom = ZonedDateTime.parse("2019-04-30T22:00:00.000Z");
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidFrom()).thenReturn(validFrom);

    final ZonedDateTime validFrom2 = ZonedDateTime.parse("2019-04-30T22:00:00.000Z");
    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValidFrom()).thenReturn(validFrom2);

    final Optional<CartDiscountUpdateAction> setValidFromUpdateAction =
        buildSetValidFromUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidFromUpdateAction).isNotPresent();
  }

  @Test
  void buildSetValidFromUpdateAction_WithOnlyNullNewSetValidFromDate_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidFrom())
        .thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValidFrom()).thenReturn(null);

    final Optional<CartDiscountUpdateAction> setValidFromUpdateAction =
        buildSetValidFromUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidFromUpdateAction)
        .contains(CartDiscountSetValidFromActionBuilder.of().validFrom(null).build());
  }

  @Test
  void buildSetValidFromUpdateAction_WithOnlyNullOldSetValidFromDate_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidFrom()).thenReturn(null);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValidFrom())
        .thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));

    final Optional<CartDiscountUpdateAction> setValidFromUpdateAction =
        buildSetValidFromUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidFromUpdateAction)
        .contains(
            CartDiscountSetValidFromActionBuilder.of()
                .validFrom(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"))
                .build());
  }

  @Test
  void buildSetValidFromUpdateAction_WithBothNullSetValidFromDates_ShouldNotBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidFrom()).thenReturn(null);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValidFrom()).thenReturn(null);

    final Optional<CartDiscountUpdateAction> setValidFromUpdateAction =
        buildSetValidFromUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidFromUpdateAction).isNotPresent();
  }

  @Test
  void buildSetValidUntilUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidFrom())
        .thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final ZonedDateTime now = ZonedDateTime.now();
    when(newCartDiscountDraft.getValidUntil()).thenReturn(now);

    final Optional<CartDiscountUpdateAction> setValidUntilUpdateAction =
        buildSetValidUntilUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidUntilUpdateAction)
        .contains(CartDiscountSetValidUntilActionBuilder.of().validUntil(now).build());
  }

  @Test
  void buildSetValidUntilUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final ZonedDateTime validUntil = ZonedDateTime.parse("2019-05-30T22:00:00.000Z");
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidUntil()).thenReturn(validUntil);

    final ZonedDateTime validUntil2 = ZonedDateTime.parse("2019-05-30T22:00:00.000Z");
    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValidUntil()).thenReturn(validUntil2);

    final Optional<CartDiscountUpdateAction> setValidUntilUpdateAction =
        buildSetValidUntilUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidUntilUpdateAction).isNotPresent();
  }

  @Test
  void buildSetValidUntilUpdateAction_WithOnlyNullNewSetValidUntilDate_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidUntil())
        .thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValidUntil()).thenReturn(null);

    final Optional<CartDiscountUpdateAction> setValidUntilUpdateAction =
        buildSetValidUntilUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidUntilUpdateAction)
        .contains(CartDiscountSetValidUntilActionBuilder.of().validUntil(null).build());
  }

  @Test
  void buildSetValidUntilUpdateAction_WithOnlyNullOldSetValidUntilDate_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidUntil()).thenReturn(null);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValidUntil())
        .thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

    final Optional<CartDiscountUpdateAction> setValidUntilUpdateAction =
        buildSetValidUntilUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidUntilUpdateAction)
        .contains(
            CartDiscountSetValidUntilActionBuilder.of()
                .validUntil(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"))
                .build());
  }

  @Test
  void buildSetValidUntilUpdateAction_WithBothNullSetValidUntilDates_ShouldNotBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidUntil()).thenReturn(null);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValidUntil()).thenReturn(null);

    final Optional<CartDiscountUpdateAction> setValidUntilUpdateAction =
        buildSetValidUntilUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidUntilUpdateAction).isNotPresent();
  }

  @Test
  void
      buildSetValidDatesUpdateAction_WithDifferentValidFromDate_ShouldBuildSetValidFromUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidFrom())
        .thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));
    when(oldCartDiscount.getValidUntil())
        .thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final ZonedDateTime differentValidFromDate = ZonedDateTime.now();
    when(newCartDiscountDraft.getValidFrom()).thenReturn(differentValidFromDate);
    when(newCartDiscountDraft.getValidUntil())
        .thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

    final Optional<CartDiscountUpdateAction> setValidFromUpdateAction =
        buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidFromUpdateAction)
        .contains(
            CartDiscountSetValidFromActionBuilder.of().validFrom(differentValidFromDate).build());
  }

  @Test
  void
      buildSetValidDatesUpdateAction_WithDifferentValidUntilDate_ShouldBuildSetValidUntilUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidFrom())
        .thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));
    when(oldCartDiscount.getValidUntil())
        .thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValidFrom())
        .thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));
    final ZonedDateTime differentValidUntilDate = ZonedDateTime.now();
    when(newCartDiscountDraft.getValidUntil()).thenReturn(differentValidUntilDate);

    final Optional<CartDiscountUpdateAction> setValidUntilUpdateAction =
        buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidUntilUpdateAction)
        .contains(
            CartDiscountSetValidUntilActionBuilder.of()
                .validUntil(differentValidUntilDate)
                .build());
  }

  @Test
  void
      buildSetValidDatesUpdateAction_WithDifferentDates_ShouldBuildSetValidFromAndUntilUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidFrom())
        .thenReturn(ZonedDateTime.parse("2019-04-30T22:00:00.000Z"));
    when(oldCartDiscount.getValidUntil())
        .thenReturn(ZonedDateTime.parse("2019-05-30T22:00:00.000Z"));

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    final ZonedDateTime differentValidFromDate = ZonedDateTime.now();
    when(newCartDiscountDraft.getValidFrom()).thenReturn(differentValidFromDate);
    final ZonedDateTime differentValidUntilDate = ZonedDateTime.now();
    when(newCartDiscountDraft.getValidUntil()).thenReturn(differentValidUntilDate);

    final Optional<CartDiscountUpdateAction> setValidUntilUpdateAction =
        buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidUntilUpdateAction)
        .contains(
            CartDiscountSetValidFromAndUntilActionBuilder.of()
                .validFrom(differentValidFromDate)
                .validUntil(differentValidUntilDate)
                .build());
  }

  @Test
  void buildSetValidDatesUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final ZonedDateTime validFrom = ZonedDateTime.parse("2019-04-30T22:00:00.000Z");
    final ZonedDateTime validUntil = ZonedDateTime.parse("2019-05-30T22:00:00.000Z");
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidFrom()).thenReturn(validFrom);
    when(oldCartDiscount.getValidUntil()).thenReturn(validUntil);

    final ZonedDateTime validFrom2 = ZonedDateTime.parse("2019-04-30T22:00:00.000Z");
    final ZonedDateTime validUntil2 = ZonedDateTime.parse("2019-05-30T22:00:00.000Z");
    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValidFrom()).thenReturn(validFrom2);
    when(newCartDiscountDraft.getValidUntil()).thenReturn(validUntil2);

    final Optional<CartDiscountUpdateAction> setValidDatesUpdateAction =
        buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidDatesUpdateAction).isNotPresent();
  }

  @Test
  void buildSetValidDatesUpdateAction_WithOnlyNullOldDates_ShouldBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidUntil()).thenReturn(null);
    when(oldCartDiscount.getValidFrom()).thenReturn(null);

    final ZonedDateTime validFrom = ZonedDateTime.parse("2019-04-30T22:00:00.000Z");
    final ZonedDateTime validUntil = ZonedDateTime.parse("2019-05-30T22:00:00.000Z");
    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValidFrom()).thenReturn(validFrom);
    when(newCartDiscountDraft.getValidUntil()).thenReturn(validUntil);

    final Optional<CartDiscountUpdateAction> setValidUntilUpdateAction =
        buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidUntilUpdateAction)
        .contains(
            CartDiscountSetValidFromAndUntilActionBuilder.of()
                .validFrom(validFrom)
                .validUntil(validUntil)
                .build());
  }

  @Test
  void buildSetValidDatesUpdateAction_WithOnlyNullNewDates_ShouldBuildUpdateAction() {
    final ZonedDateTime validFrom = ZonedDateTime.parse("2019-04-30T22:00:00.000Z");
    final ZonedDateTime validUntil = ZonedDateTime.parse("2019-05-30T22:00:00.000Z");
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidUntil()).thenReturn(validFrom);
    when(oldCartDiscount.getValidFrom()).thenReturn(validUntil);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValidUntil()).thenReturn(null);
    when(newCartDiscountDraft.getValidFrom()).thenReturn(null);

    final Optional<CartDiscountUpdateAction> setValidUntilUpdateAction =
        buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidUntilUpdateAction)
        .contains(CartDiscountSetValidFromAndUntilActionBuilder.of().build());
  }

  @Test
  void
      buildSetValidDatesUpdateAction_WithBothNullValidUntilAndFromDates_ShouldNotBuildUpdateAction() {
    final CartDiscount oldCartDiscount = mock(CartDiscount.class);
    when(oldCartDiscount.getValidUntil()).thenReturn(null);
    when(oldCartDiscount.getValidFrom()).thenReturn(null);

    final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
    when(newCartDiscountDraft.getValidUntil()).thenReturn(null);
    when(newCartDiscountDraft.getValidFrom()).thenReturn(null);

    final Optional<CartDiscountUpdateAction> setValidUntilUpdateAction =
        buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscountDraft);

    assertThat(setValidUntilUpdateAction).isNotPresent();
  }
}
