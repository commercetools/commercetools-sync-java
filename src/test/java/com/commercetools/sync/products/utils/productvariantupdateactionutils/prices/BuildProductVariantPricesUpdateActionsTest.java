package com.commercetools.sync.products.utils.productvariantupdateactionutils.prices;

import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantPricesUpdateActions;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.*;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.*;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.product.ProductAddPriceActionBuilder;
import com.commercetools.api.models.product.ProductChangePriceActionBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductRemovePriceActionBuilder;
import com.commercetools.api.models.product.ProductSetProductPriceCustomFieldActionBuilder;
import com.commercetools.api.models.product.ProductSetProductPriceCustomTypeActionBuilder;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BuildProductVariantPricesUpdateActionsTest {

  private final ProductProjection oldProduct = mock(ProductProjection.class);
  private final ProductDraft newProductDraft = mock(ProductDraft.class);
  private final ProductVariant oldProductVariant = mock(ProductVariant.class);
  private final ProductVariantDraft newProductVariant = mock(ProductVariantDraft.class);
  private List<String> errorMessages;
  private final ProductSyncOptions syncOptions =
      ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
          .errorCallback(
              (exception, oldResource, newResource, updateActions) ->
                  errorMessages.add(exception.getMessage()))
          .build();

  @BeforeEach
  void setupMethod() {
    errorMessages = new ArrayList<>();
  }

  @Test
  void withNullNewPricesAndEmptyExistingPrices_ShouldNotBuildActions() {
    // Preparation
    when(newProductVariant.getPrices()).thenReturn(null);
    when(oldProductVariant.getPrices()).thenReturn(emptyList());

    // Test
    final List<ProductUpdateAction> updateActions =
        buildProductVariantPricesUpdateActions(
            oldProduct, newProductDraft, oldProductVariant, newProductVariant, syncOptions);

    // Assertion
    assertThat(updateActions).isEmpty();
  }

  @Test
  void withSomeNullNewPricesAndExistingPrices_ShouldBuildActionsAndTriggerErrorCallback() {
    // Preparation
    final List<PriceDraft> newPrices =
        asList(DRAFT_US_111_USD, null, DRAFT_DE_111_EUR_01_02, DRAFT_DE_111_USD);
    when(newProductVariant.getPrices()).thenReturn(newPrices);
    final List<Price> oldPrices = asList(US_111_USD, DE_111_EUR, DE_111_EUR_01_02, DE_111_USD);
    when(oldProductVariant.getPrices()).thenReturn(oldPrices);

    // Test
    final List<ProductUpdateAction> updateActions =
        buildProductVariantPricesUpdateActions(
            oldProduct, newProductDraft, oldProductVariant, newProductVariant, syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            ProductRemovePriceActionBuilder.of().priceId(DE_111_EUR.getId()).staged(true).build());
    assertThat(errorMessages)
        .containsExactly(
            format(
                "Failed to build prices update actions for one price on the "
                    + "variant with id '%d' and key '%s'. Reason: %s",
                oldProductVariant.getId(), newProductVariant.getKey(), "New price is null."));
  }

  @Test
  void withEmptyNewPricesAndEmptyExistingPrices_ShouldNotBuildActions() {
    // Preparation
    when(oldProductVariant.getPrices()).thenReturn(emptyList());
    when(newProductVariant.getPrices()).thenReturn(emptyList());

    // Test
    final List<ProductUpdateAction> updateActions =
        buildProductVariantPricesUpdateActions(
            oldProduct, newProductDraft, oldProductVariant, newProductVariant, syncOptions);

    // Assertion
    assertThat(updateActions).isEmpty();
  }

  @Test
  void withAllMatchingPrices_ShouldNotBuildActions() {
    // Preparation
    final List<PriceDraft> newPrices =
        asList(DRAFT_US_111_USD, DRAFT_DE_111_EUR, DRAFT_DE_111_EUR_01_02, DRAFT_DE_111_USD);
    when(newProductVariant.getPrices()).thenReturn(newPrices);

    final List<Price> oldPrices = asList(US_111_USD, DE_111_EUR, DE_111_EUR_01_02, DE_111_USD);
    when(oldProductVariant.getPrices()).thenReturn(oldPrices);

    // Test
    final List<ProductUpdateAction> updateActions =
        buildProductVariantPricesUpdateActions(
            oldProduct, newProductDraft, oldProductVariant, newProductVariant, syncOptions);

    // Assertion
    assertThat(updateActions).isEmpty();
  }

  @Test
  void withNonEmptyNewPricesButEmptyExistingPrices_ShouldBuildAddPriceActions() {
    // Preparation
    final List<PriceDraft> newPrices =
        asList(
            DRAFT_US_111_USD,
            DRAFT_DE_111_EUR,
            DRAFT_DE_111_EUR_01_02,
            DRAFT_DE_111_EUR_02_03,
            DRAFT_DE_111_USD,
            DRAFT_UK_111_GBP);
    when(newProductVariant.getPrices()).thenReturn(newPrices);
    when(oldProductVariant.getPrices()).thenReturn(emptyList());

    // Test
    final List<ProductUpdateAction> updateActions =
        buildProductVariantPricesUpdateActions(
            oldProduct, newProductDraft, oldProductVariant, newProductVariant, syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_US_111_USD)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_111_EUR)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_111_EUR_01_02)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_111_EUR_02_03)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_111_USD)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_UK_111_GBP)
                .staged(true)
                .build());
  }

  @Test
  void withSomeNonChangedMatchingPricesAndNewPrices_ShouldBuildAddPriceActions() {
    // Preparation
    final List<PriceDraft> newPrices =
        asList(
            DRAFT_DE_111_EUR,
            DRAFT_DE_111_EUR_01_02,
            DRAFT_DE_111_EUR_02_03,
            DRAFT_DE_111_USD,
            DRAFT_UK_111_GBP);
    when(newProductVariant.getPrices()).thenReturn(newPrices);

    final List<Price> oldPrices = asList(DE_111_EUR, DE_111_EUR_01_02);
    when(oldProductVariant.getPrices()).thenReturn(oldPrices);

    // Test
    final List<ProductUpdateAction> updateActions =
        buildProductVariantPricesUpdateActions(
            oldProduct, newProductDraft, oldProductVariant, newProductVariant, syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_111_EUR_02_03)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_111_USD)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_UK_111_GBP)
                .staged(true)
                .build());
  }

  @Test
  void withNullNewPrices_ShouldBuildRemovePricesAction() {
    // Preparation
    when(newProductVariant.getPrices()).thenReturn(null);
    final List<Price> prices = asList(US_111_USD, DE_111_EUR, DE_111_EUR_01_02, DE_111_USD);
    when(oldProductVariant.getPrices()).thenReturn(prices);

    // Test
    final List<ProductUpdateAction> updateActions =
        buildProductVariantPricesUpdateActions(
            oldProduct, newProductDraft, oldProductVariant, newProductVariant, syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductRemovePriceActionBuilder.of().priceId(US_111_USD.getId()).staged(true).build(),
            ProductRemovePriceActionBuilder.of().priceId(DE_111_EUR.getId()).staged(true).build(),
            ProductRemovePriceActionBuilder.of()
                .priceId(DE_111_EUR_01_02.getId())
                .staged(true)
                .build(),
            ProductRemovePriceActionBuilder.of().priceId(DE_111_USD.getId()).staged(true).build());
  }

  @Test
  void withEmptyNewPrices_ShouldBuildRemovePricesAction() {
    // Preparation
    when(newProductVariant.getPrices()).thenReturn(emptyList());
    final List<Price> prices = asList(US_111_USD, DE_111_EUR, DE_111_EUR_01_02, DE_111_USD);
    when(oldProductVariant.getPrices()).thenReturn(prices);

    // Test
    final List<ProductUpdateAction> updateActions =
        buildProductVariantPricesUpdateActions(
            oldProduct, newProductDraft, oldProductVariant, newProductVariant, syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductRemovePriceActionBuilder.of().priceId(US_111_USD.getId()).staged(true).build(),
            ProductRemovePriceActionBuilder.of().priceId(DE_111_EUR.getId()).staged(true).build(),
            ProductRemovePriceActionBuilder.of()
                .priceId(DE_111_EUR_01_02.getId())
                .staged(true)
                .build(),
            ProductRemovePriceActionBuilder.of().priceId(DE_111_USD.getId()).staged(true).build());
  }

  @Test
  void withSomeNonChangedMatchingPricesAndNoNewPrices_ShouldBuildRemovePriceActions() {
    // Preparation
    final List<PriceDraft> newPrices = List.of(DRAFT_US_111_USD);
    when(newProductVariant.getPrices()).thenReturn(newPrices);
    final List<Price> prices = asList(US_111_USD, DE_111_EUR, DE_111_EUR_01_02, DE_111_USD);
    when(oldProductVariant.getPrices()).thenReturn(prices);

    // Test
    final List<ProductUpdateAction> updateActions =
        buildProductVariantPricesUpdateActions(
            oldProduct, newProductDraft, oldProductVariant, newProductVariant, syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductRemovePriceActionBuilder.of().priceId(DE_111_EUR.getId()).staged(true).build(),
            ProductRemovePriceActionBuilder.of()
                .priceId(DE_111_EUR_01_02.getId())
                .staged(true)
                .build(),
            ProductRemovePriceActionBuilder.of().priceId(DE_111_USD.getId()).staged(true).build());
  }

  @Test
  void withNoMatchingPrices_ShouldBuildRemoveAndAddPricesActions() {
    // Preparation
    final List<PriceDraft> newPrices =
        asList(DRAFT_DE_111_EUR, DRAFT_DE_111_EUR_01_02, DRAFT_UK_111_GBP);
    when(newProductVariant.getPrices()).thenReturn(newPrices);

    final List<Price> oldPrices = asList(DE_111_USD, DE_111_EUR_02_03, US_111_USD);
    when(oldProductVariant.getPrices()).thenReturn(oldPrices);

    // Test
    final List<ProductUpdateAction> updateActions =
        buildProductVariantPricesUpdateActions(
            oldProduct, newProductDraft, oldProductVariant, newProductVariant, syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductRemovePriceActionBuilder.of().priceId(DE_111_USD.getId()).staged(true).build(),
            ProductRemovePriceActionBuilder.of()
                .priceId(DE_111_EUR_02_03.getId())
                .staged(true)
                .build(),
            ProductRemovePriceActionBuilder.of().priceId(US_111_USD.getId()).staged(true).build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_111_EUR)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_111_EUR_01_02)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_UK_111_GBP)
                .staged(true)
                .build());
  }

  @Test
  void withSomeChangedMatchingPrices_ShouldBuildRemoveAndAddPricesActions() {
    // Preparation
    final List<PriceDraft> newPrices =
        asList(
            DRAFT_DE_111_EUR,
            DRAFT_DE_222_EUR_CUST1,
            DRAFT_DE_333_USD_CUST1,
            DRAFT_UK_111_GBP_01_02,
            DRAFT_UK_111_GBP);
    when(newProductVariant.getPrices()).thenReturn(newPrices);

    final List<Price> oldPrices =
        asList(DE_111_EUR, DE_345_EUR_CUST2, DE_567_EUR_CUST3, UK_111_GBP_02_03, UK_111_GBP_01_02);
    when(oldProductVariant.getPrices()).thenReturn(oldPrices);

    // Test
    final List<ProductUpdateAction> updateActions =
        buildProductVariantPricesUpdateActions(
            oldProduct, newProductDraft, oldProductVariant, newProductVariant, syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductRemovePriceActionBuilder.of()
                .priceId(DE_345_EUR_CUST2.getId())
                .staged(true)
                .build(),
            ProductRemovePriceActionBuilder.of()
                .priceId(DE_567_EUR_CUST3.getId())
                .staged(true)
                .build(),
            ProductRemovePriceActionBuilder.of()
                .priceId(UK_111_GBP_02_03.getId())
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_222_EUR_CUST1)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_333_USD_CUST1)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_UK_111_GBP)
                .staged(true)
                .build());
  }

  @Test
  void withPricesWithOverlappingValidityDates_ShouldBuildRemoveAndAddPricesActions() {
    // Preparation
    final List<PriceDraft> newPrices =
        asList(
            DRAFT_DE_111_EUR_01_02,
            DRAFT_DE_222_EUR_03_04,
            DRAFT_UK_333_GBP_01_04,
            DRAFT_UK_444_GBP_04_06,
            DRAFT_US_666_USD_CUST1_01_02,
            DRAFT_FR_888_EUR_01_03,
            DRAFT_FR_999_EUR_03_06,
            DRAFT_NE_777_EUR_01_04,
            DRAFT_NE_777_EUR_05_07);
    when(newProductVariant.getPrices()).thenReturn(newPrices);

    final List<Price> oldPrices =
        asList(
            DE_111_EUR,
            UK_333_GBP_03_05,
            US_555_USD_CUST2_01_02,
            FR_777_EUR_01_04,
            NE_123_EUR_01_04,
            NE_321_EUR_04_06);
    when(oldProductVariant.getPrices()).thenReturn(oldPrices);

    // Test
    final List<ProductUpdateAction> updateActions =
        buildProductVariantPricesUpdateActions(
            oldProduct, newProductDraft, oldProductVariant, newProductVariant, syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductRemovePriceActionBuilder.of().priceId(DE_111_EUR.getId()).staged(true).build(),
            ProductRemovePriceActionBuilder.of()
                .priceId(UK_333_GBP_03_05.getId())
                .staged(true)
                .build(),
            ProductRemovePriceActionBuilder.of()
                .priceId(US_555_USD_CUST2_01_02.getId())
                .staged(true)
                .build(),
            ProductRemovePriceActionBuilder.of()
                .priceId(FR_777_EUR_01_04.getId())
                .staged(true)
                .build(),
            ProductRemovePriceActionBuilder.of()
                .priceId(NE_321_EUR_04_06.getId())
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_111_EUR_01_02)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_222_EUR_03_04)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_UK_333_GBP_01_04)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_UK_444_GBP_04_06)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_US_666_USD_CUST1_01_02)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_FR_888_EUR_01_03)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_FR_999_EUR_03_06)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_NE_777_EUR_05_07)
                .staged(true)
                .build(),
            ProductChangePriceActionBuilder.of()
                .priceId(NE_123_EUR_01_04.getId())
                .price(DRAFT_NE_777_EUR_01_04)
                .staged(true)
                .build());
  }

  @Test
  void withAllMatchingChangedPrices_ShouldBuildChangePriceActions() {
    // Preparation
    final List<PriceDraft> newPrices =
        asList(
            DRAFT_DE_111_EUR,
            DRAFT_DE_111_USD,
            DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX,
            DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX,
            DRAFT_UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDX,
            DRAFT_UK_22_USD_CUSTOMTYPE1_CUSTOMFIELDX,
            DRAFT_UK_666_GBP_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX);
    when(newProductVariant.getPrices()).thenReturn(newPrices);

    final List<Price> oldPrices =
        asList(
            DE_666_EUR,
            DE_111_USD,
            DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY,
            DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX,
            UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDY,
            UK_22_USD_CUSTOMTYPE2_CUSTOMFIELDX,
            UK_1_GBP_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX);
    when(oldProductVariant.getPrices()).thenReturn(oldPrices);

    // Test
    final List<ProductUpdateAction> updateActions =
        buildProductVariantPricesUpdateActions(
            oldProduct, newProductDraft, oldProductVariant, newProductVariant, syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductChangePriceActionBuilder.of()
                .priceId(DE_666_EUR.getId())
                .price(DRAFT_DE_111_EUR)
                .staged(true)
                .build(),
            ProductChangePriceActionBuilder.of()
                .priceId(DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY.getId())
                .price(DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX)
                .staged(true)
                .build(),
            ProductSetProductPriceCustomFieldActionBuilder.of()
                .name("foo")
                .value(
                    DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX
                        .getCustom()
                        .getFields()
                        .values()
                        .get("foo"))
                .priceId(DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY.getId())
                .staged(true)
                .build(),
            ProductChangePriceActionBuilder.of()
                .priceId(DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX.getId())
                .price(DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX)
                .staged(true)
                .build(),
            ProductSetProductPriceCustomTypeActionBuilder.of()
                .type(
                    DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX.getCustom().getType())
                .fields(
                    DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX
                        .getCustom()
                        .getFields())
                .priceId(DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX.getId())
                .staged(true)
                .build(),
            ProductSetProductPriceCustomFieldActionBuilder.of()
                .name("foo")
                .value(
                    DRAFT_UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDX
                        .getCustom()
                        .getFields()
                        .values()
                        .get("foo"))
                .priceId(UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDY.getId())
                .staged(true)
                .build(),
            ProductSetProductPriceCustomTypeActionBuilder.of()
                .type(DRAFT_UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDX.getCustom().getType())
                .fields(DRAFT_UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDX.getCustom().getFields())
                .priceId(UK_22_USD_CUSTOMTYPE2_CUSTOMFIELDX.getId())
                .staged(true)
                .build(),
            ProductChangePriceActionBuilder.of()
                .priceId(UK_1_GBP_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX.getId())
                .price(DRAFT_UK_666_GBP_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX)
                .staged(true)
                .build());
  }

  @Test
  void withDifferentPriceActions_ShouldBuildActionsInCorrectOrder() {
    // Preparation
    final List<PriceDraft> newPrices = asList(DRAFT_DE_111_EUR_01_02, DRAFT_NE_777_EUR_01_04);
    when(newProductVariant.getPrices()).thenReturn(newPrices);

    final List<Price> oldPrices = asList(DE_111_EUR, NE_123_EUR_01_04);
    when(oldProductVariant.getPrices()).thenReturn(oldPrices);

    // Test
    final List<ProductUpdateAction> updateActions =
        buildProductVariantPricesUpdateActions(
            oldProduct, newProductDraft, oldProductVariant, newProductVariant, syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactly(
            ProductRemovePriceActionBuilder.of().priceId(DE_111_EUR.getId()).staged(true).build(),
            ProductChangePriceActionBuilder.of()
                .priceId(NE_123_EUR_01_04.getId())
                .price(DRAFT_NE_777_EUR_01_04)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_111_EUR_01_02)
                .staged(true)
                .build());
  }

  @Test
  void withMixedCasesOfPriceMatches_ShouldBuildActions() {
    // Preparation
    final List<PriceDraft> newPrices =
        asList(
            DRAFT_DE_111_EUR,
            DRAFT_DE_222_EUR_CUST1,
            DRAFT_DE_111_EUR_01_02,
            DRAFT_DE_111_EUR_03_04,
            DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX,
            DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX,
            DRAFT_DE_333_USD_CUST1,
            DRAFT_DE_22_USD,
            DRAFT_UK_111_GBP_01_02,
            DRAFT_UK_999_GBP,
            DRAFT_US_666_USD_CUST2_01_02,
            DRAFT_FR_888_EUR_01_03,
            DRAFT_FR_999_EUR_03_06,
            DRAFT_NE_777_EUR_01_04,
            DRAFT_NE_777_EUR_05_07);
    when(newProductVariant.getPrices()).thenReturn(newPrices);

    final List<Price> oldPrices =
        asList(
            DE_111_EUR,
            DE_345_EUR_CUST2,
            DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY,
            DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX,
            DE_22_USD,
            UK_111_GBP_01_02,
            UK_111_GBP_02_03,
            UK_333_GBP_03_05,
            FR_777_EUR_01_04,
            NE_123_EUR_01_04,
            NE_321_EUR_04_06);
    when(oldProductVariant.getPrices()).thenReturn(oldPrices);

    // Test
    final List<ProductUpdateAction> updateActions =
        buildProductVariantPricesUpdateActions(
            oldProduct, newProductDraft, oldProductVariant, newProductVariant, syncOptions);

    // Assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductRemovePriceActionBuilder.of()
                .priceId(DE_345_EUR_CUST2.getId())
                .staged(true)
                .build(),
            ProductRemovePriceActionBuilder.of()
                .priceId(UK_111_GBP_02_03.getId())
                .staged(true)
                .build(),
            ProductRemovePriceActionBuilder.of()
                .priceId(UK_333_GBP_03_05.getId())
                .staged(true)
                .build(),
            ProductRemovePriceActionBuilder.of()
                .priceId(FR_777_EUR_01_04.getId())
                .staged(true)
                .build(),
            ProductRemovePriceActionBuilder.of()
                .priceId(NE_321_EUR_04_06.getId())
                .staged(true)
                .build(),
            ProductChangePriceActionBuilder.of()
                .priceId(DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY.getId())
                .price(DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX)
                .staged(true)
                .build(),
            ProductSetProductPriceCustomFieldActionBuilder.of()
                .name("foo")
                .value(
                    DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX
                        .getCustom()
                        .getFields()
                        .values()
                        .get("foo"))
                .priceId(DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY.getId())
                .staged(true)
                .build(),
            ProductChangePriceActionBuilder.of()
                .priceId(DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX.getId())
                .price(DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX)
                .staged(true)
                .build(),
            ProductSetProductPriceCustomTypeActionBuilder.of()
                .type(
                    DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX.getCustom().getType())
                .fields(
                    DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX
                        .getCustom()
                        .getFields())
                .priceId(DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX.getId())
                .staged(true)
                .build(),
            ProductChangePriceActionBuilder.of()
                .priceId(NE_123_EUR_01_04.getId())
                .price(DRAFT_NE_777_EUR_01_04)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_222_EUR_CUST1)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_111_EUR_01_02)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_111_EUR_03_04)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_DE_333_USD_CUST1)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_UK_999_GBP)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_US_666_USD_CUST2_01_02)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_FR_888_EUR_01_03)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_FR_999_EUR_03_06)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(oldProductVariant.getId())
                .price(DRAFT_NE_777_EUR_05_07)
                .staged(true)
                .build());
  }
}
