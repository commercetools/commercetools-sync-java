package com.commercetools.sync.products.utils.productvariantupdateactionutils.prices;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.commands.updateactions.AddPrice;
import io.sphere.sdk.products.commands.updateactions.ChangePrice;
import io.sphere.sdk.products.commands.updateactions.RemovePrice;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomField;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomType;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantPricesUpdateActions;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_EUR;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_EUR_01_02;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_EUR_02_03;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_EUR_03_04;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_USD;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_222_EUR_03_04;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_222_EUR_CUST1;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_22_USD;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_333_USD_CUST1;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_FR_888_EUR_01_03;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_FR_999_EUR_03_06;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_NE_777_EUR_01_04;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_NE_777_EUR_05_07;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_111_GBP;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_111_GBP_01_02;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDX;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_22_USD_CUSTOMTYPE1_CUSTOMFIELDX;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_333_GBP_01_04;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_444_GBP_04_06;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_666_GBP_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_999_GBP;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_US_111_USD;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_US_666_USD_CUST1_01_02;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_US_666_USD_CUST2_01_02;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.DE_111_EUR;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.DE_111_EUR_01_02;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.DE_111_EUR_02_03;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.DE_111_USD;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.DE_22_USD;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.DE_345_EUR_CUST2;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.DE_567_EUR_CUST2;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.DE_666_EUR;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.FR_777_EUR_01_04;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.NE_123_EUR_01_04;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.NE_321_EUR_04_06;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.UK_111_GBP_01_02;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.UK_111_GBP_02_03;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.UK_1_GBP_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDY;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.UK_22_USD_CUSTOMTYPE2_CUSTOMFIELDX;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.UK_333_GBP_02_05;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.US_111_USD;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.US_555_USD_CUST2_01_02;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildProductVariantPricesUpdateActionsTest {

    private static final ProductVariant oldProductVariant = mock(ProductVariant.class);
    private static final ProductVariantDraft newProductVariant = mock(ProductVariantDraft.class);

    @BeforeClass
    public static void setup() {
        when(oldProductVariant.getId()).thenReturn(101);
    }

    /**
     * Case#1
     */
    @Test
    public void withNullNewPricesAndEmptyExistingPrices_ShouldNotBuildActions() {
        // Preparation
        when(oldProductVariant.getPrices()).thenReturn(emptyList());

        // Test
        final List<UpdateAction<Product>> updateActions =
            buildProductVariantPricesUpdateActions(oldProductVariant, newProductVariant);

        // Assertion
        assertThat(updateActions).isEmpty();
    }

    /**
     * Case#1
     */
    @Test
    public void withEmptyNewPricesAndEmptyExistingPrices_ShouldNotBuildActions() {
        // Preparation
        when(oldProductVariant.getPrices()).thenReturn(emptyList());
        when(newProductVariant.getPrices()).thenReturn(emptyList());

        // Test
        final List<UpdateAction<Product>> updateActions =
            buildProductVariantPricesUpdateActions(oldProductVariant, newProductVariant);

        // Assertion
        assertThat(updateActions).isEmpty();
    }

    /**
     * Case#2
     */
    @Test
    public void withAllMatchingPrices_ShouldBuildNoActions() {
        // Preparation
        final List<PriceDraft> newPrices = asList(
            DRAFT_US_111_USD,
            DRAFT_DE_111_EUR,
            DRAFT_DE_111_EUR_01_02,
            DRAFT_DE_111_USD);
        when(newProductVariant.getPrices()).thenReturn(newPrices);

        final List<Price> oldPrices = asList(
            DE_111_USD,
            DE_111_EUR,
            DE_111_EUR_01_02,
            US_111_USD);
        when(oldProductVariant.getPrices()).thenReturn(oldPrices);

        // Test
        final List<UpdateAction<Product>> updateActions =
            buildProductVariantPricesUpdateActions(oldProductVariant, newProductVariant);

        // Assertion
        assertThat(updateActions).isEmpty();
    }

    /**
     * Case#3
     */
    @Test
    public void withNonEmptyNewPricesButEmptyExistingPrices_ShouldBuildAddPriceActions() {
        // Preparation
        final List<PriceDraft> newPrices = asList(
            DRAFT_US_111_USD,
            DRAFT_DE_111_EUR,
            DRAFT_DE_111_EUR_01_02,
            DRAFT_DE_111_EUR_02_03,
            DRAFT_DE_111_USD,
            DRAFT_UK_111_GBP);
        when(newProductVariant.getPrices()).thenReturn(newPrices);

        when(oldProductVariant.getPrices()).thenReturn(emptyList());

        // Test
        final List<UpdateAction<Product>> updateActions =
            buildProductVariantPricesUpdateActions(oldProductVariant, newProductVariant);

        // Assertion
        assertThat(updateActions).containsExactly(
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_US_111_USD, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_111_EUR, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_111_EUR_01_02, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_111_EUR_02_03, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_111_USD, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_UK_111_GBP, true));
    }


    /**
     * Case#4
     */
    @Test
    public void withSomeNonChangedMatchingPricesAndNewPrices_ShouldBuildAddPriceActions() {
        // Preparation
        final List<PriceDraft> newPrices = asList(
            DRAFT_DE_111_EUR,
            DRAFT_DE_111_EUR_01_02,
            DRAFT_DE_111_EUR_02_03,
            DRAFT_DE_111_USD,
            DRAFT_UK_111_GBP);
        when(newProductVariant.getPrices()).thenReturn(newPrices);

        final List<Price> oldPrices = asList(
            DE_111_EUR,
            DE_111_EUR_01_02
        );
        when(oldProductVariant.getPrices()).thenReturn(oldPrices);

        // Test
        final List<UpdateAction<Product>> updateActions =
            buildProductVariantPricesUpdateActions(oldProductVariant, newProductVariant);

        // Assertion
        assertThat(updateActions).containsExactly(
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_111_EUR_02_03, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_111_USD, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_UK_111_GBP, true));
    }

    /**
     * Case#5: NULL
     */
    @Test
    public void withNullNewPrices_ShouldBuildRemovePricesAction() {
        // Preparation
        when(newProductVariant.getPrices()).thenReturn(null);
        final List<Price> prices = asList(
            US_111_USD,
            DE_111_EUR,
            DE_111_EUR_01_02,
            DE_111_USD);
        when(oldProductVariant.getPrices()).thenReturn(prices);

        // Test
        final List<UpdateAction<Product>> updateActions =
            buildProductVariantPricesUpdateActions(oldProductVariant, newProductVariant);

        // Assertion
        assertThat(updateActions).containsExactly(
            RemovePrice.of(US_111_USD.getId(), true),
            RemovePrice.of(DE_111_USD.getId(), true),
            RemovePrice.of(DE_111_EUR_01_02.getId(), true),
            RemovePrice.of(DE_111_USD.getId(), true));
    }

    /**
     * Case#5: EMPTY
     */
    @Test
    public void withEmptyNewPrices_ShouldBuildRemovePricesAction() {
        // Preparation
        when(newProductVariant.getPrices()).thenReturn(emptyList());
        final List<Price> prices = asList(
            US_111_USD,
            DE_111_EUR,
            DE_111_EUR_01_02,
            DE_111_USD);
        when(oldProductVariant.getPrices()).thenReturn(prices);

        // Test
        final List<UpdateAction<Product>> updateActions =
            buildProductVariantPricesUpdateActions(oldProductVariant, newProductVariant);

        // Assertion
        assertThat(updateActions).containsExactly(
            RemovePrice.of(US_111_USD.getId(), true),
            RemovePrice.of(DE_111_USD.getId(), true),
            RemovePrice.of(DE_111_EUR_01_02.getId(), true),
            RemovePrice.of(DE_111_USD.getId(), true));
    }

    /**
     * Case#6
     */
    @Test
    public void withSomeNonChangedMatchingPricesAndNoNewPrices_ShouldBuildRemovePriceActions() {
        // Preparation
        final List<PriceDraft> newPrices = singletonList(DRAFT_US_111_USD);
        when(newProductVariant.getPrices()).thenReturn(newPrices);
        final List<Price> prices = asList(
            US_111_USD,
            DE_111_EUR,
            DE_111_EUR_01_02,
            DE_111_USD);
        when(oldProductVariant.getPrices()).thenReturn(prices);


        // Test
        final List<UpdateAction<Product>> updateActions =
            buildProductVariantPricesUpdateActions(oldProductVariant, newProductVariant);

        // Assertion
        assertThat(updateActions).containsExactly(
            RemovePrice.of(DE_111_EUR.getId(), true),
            RemovePrice.of(DE_111_EUR_01_02.getId(), true),
            RemovePrice.of(DE_111_USD.getId(), true));
    }

    /**
     * Case#7
     */
    @Test
    public void withNoMatchingPrices_ShouldBuildRemoveAndAddPricesActions() {
        // Preparation
        final List<PriceDraft> newPrices = asList(
            DRAFT_DE_111_EUR,
            DRAFT_DE_111_EUR_01_02,
            DRAFT_UK_111_GBP);
        when(newProductVariant.getPrices()).thenReturn(newPrices);

        final List<Price> oldPrices = asList(
            DE_111_USD,
            DE_111_EUR_02_03,
            US_111_USD);
        when(oldProductVariant.getPrices()).thenReturn(oldPrices);

        // Test
        final List<UpdateAction<Product>> updateActions =
            buildProductVariantPricesUpdateActions(oldProductVariant, newProductVariant);

        // Assertion
        assertThat(updateActions).containsExactly(
            RemovePrice.of(DE_111_USD.getId(), true),
            RemovePrice.of(DE_111_EUR_01_02.getId(), true),
            RemovePrice.of(US_111_USD.getId(), true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_111_EUR, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_111_EUR_01_02, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_UK_111_GBP, true));
    }

    /**
     * Case#8
     */
    @Test
    public void withSomeChangedMatchingPrices_ShouldBuildRemoveAndAddPricesActions() {
        // Preparation
        final List<PriceDraft> newPrices = asList(
            DRAFT_DE_111_EUR,
            DRAFT_DE_222_EUR_CUST1,
            DRAFT_DE_333_USD_CUST1,
            DRAFT_UK_111_GBP_01_02,
            DRAFT_UK_111_GBP);
        when(newProductVariant.getPrices()).thenReturn(newPrices);

        final List<Price> oldPrices = asList(
            DE_111_EUR,
            DE_345_EUR_CUST2,
            DE_567_EUR_CUST2,
            UK_111_GBP_02_03,
            UK_111_GBP_01_02);
        when(oldProductVariant.getPrices()).thenReturn(oldPrices);

        // Test
        final List<UpdateAction<Product>> updateActions =
            buildProductVariantPricesUpdateActions(oldProductVariant, newProductVariant);

        // Assertion
        assertThat(updateActions).containsExactly(
            RemovePrice.of(DE_345_EUR_CUST2.getId(), true),
            RemovePrice.of(DE_567_EUR_CUST2.getId(), true),
            RemovePrice.of(UK_111_GBP_02_03.getId(), true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_222_EUR_CUST1, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_333_USD_CUST1, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_UK_111_GBP, true));
    }

    /**
     * Case#9
     */
    @Test
    public void withPricesWithOverlappingValidityDates_ShouldBuildRemoveAndAddPricesActions() {
        // Preparation
        final List<PriceDraft> newPrices = asList(
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

        final List<Price> oldPrices = asList(
            DE_111_EUR,
            UK_333_GBP_02_05,
            US_555_USD_CUST2_01_02,
            FR_777_EUR_01_04,
            NE_123_EUR_01_04,
            NE_321_EUR_04_06
        );
        when(oldProductVariant.getPrices()).thenReturn(oldPrices);

        // Test
        final List<UpdateAction<Product>> updateActions =
            buildProductVariantPricesUpdateActions(oldProductVariant, newProductVariant);

        // Assertion
        assertThat(updateActions).containsExactly(
            RemovePrice.of(DE_111_EUR.getId(), true),
            RemovePrice.of(UK_333_GBP_02_05.getId(), true),
            RemovePrice.of(US_555_USD_CUST2_01_02.getId(), true),
            RemovePrice.of(FR_777_EUR_01_04.getId(), true),
            RemovePrice.of(NE_321_EUR_04_06.getId(), true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_111_EUR_01_02, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_222_EUR_03_04, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_UK_333_GBP_01_04, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_UK_444_GBP_04_06, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_US_666_USD_CUST1_01_02, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_FR_888_EUR_01_03, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_FR_999_EUR_03_06, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_NE_777_EUR_05_07, true),
            ChangePrice.of(NE_123_EUR_01_04, DRAFT_NE_777_EUR_01_04, true)
        );
    }


    /**
     * Case#10
     */
    @Test
    public void withAllMatchingChangedPrices_ShouldBuildChangePriceActions() {
        // Preparation
        final List<PriceDraft> newPrices = asList(
            DRAFT_DE_111_EUR,
            DRAFT_US_111_USD,
            DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX,
            DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX,
            DRAFT_UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDX,
            DRAFT_UK_22_USD_CUSTOMTYPE1_CUSTOMFIELDX,
            DRAFT_UK_666_GBP_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX);
        when(newProductVariant.getPrices()).thenReturn(newPrices);

        final List<Price> oldPrices = asList(
            DE_666_EUR,
            DE_22_USD,
            DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY,
            DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX,
            UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDY,
            UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDY,
            UK_22_USD_CUSTOMTYPE2_CUSTOMFIELDX,
            UK_1_GBP_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX
        );
        when(oldProductVariant.getPrices()).thenReturn(oldPrices);

        // Test
        final List<UpdateAction<Product>> updateActions =
            buildProductVariantPricesUpdateActions(oldProductVariant, newProductVariant);

        // Assertion
        assertThat(updateActions).containsExactly(
            ChangePrice.of(DE_666_EUR, DRAFT_DE_111_EUR, true),
            ChangePrice.of(DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY,
                DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX, true),
            SetProductPriceCustomField.ofJson("foo",
                DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX.getCustom().getFields().get("foo"),
                DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY.getId(), true),
            ChangePrice.of(DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX,
                DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX, true),
            SetProductPriceCustomType.ofTypeIdAndJson(
                DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX.getCustom().getType().getId(),
                DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX.getCustom().getFields(),
                DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX.getId(), true),
            SetProductPriceCustomField.ofJson("foo",
                DRAFT_UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDX.getCustom().getFields().get("foo"),
                UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDY.getId(), true),
            SetProductPriceCustomType.ofTypeIdAndJson(
                DRAFT_UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDX.getCustom().getType().getId(),
                DRAFT_UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDX.getCustom().getFields(),
                UK_22_USD_CUSTOMTYPE2_CUSTOMFIELDX.getId(), true),
            ChangePrice.of(UK_1_GBP_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX,
                DRAFT_UK_666_GBP_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX, true));
    }

    /**
     * Case#11
     */
    @Test
    public void withMixedCasesOfPriceMatches_ShouldBuildActions() {
        // Preparation
        final List<PriceDraft> newPrices = asList(
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

        final List<Price> oldPrices = asList(
            DE_111_EUR,
            DE_345_EUR_CUST2,
            DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY,
            DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX,
            DE_22_USD,
            UK_111_GBP_01_02,
            UK_111_GBP_02_03,
            UK_333_GBP_02_05,
            FR_777_EUR_01_04,
            NE_123_EUR_01_04,
            NE_321_EUR_04_06
        );
        when(oldProductVariant.getPrices()).thenReturn(oldPrices);

        // Test
        final List<UpdateAction<Product>> updateActions =
            buildProductVariantPricesUpdateActions(oldProductVariant, newProductVariant);

        // Assertion
        assertThat(updateActions).containsExactly(
            RemovePrice.of(DE_345_EUR_CUST2.getId(), true),
            RemovePrice.of(UK_111_GBP_02_03.getId(), true),
            RemovePrice.of(UK_333_GBP_02_05.getId(), true),
            RemovePrice.of(FR_777_EUR_01_04.getId(), true),
            RemovePrice.of(NE_321_EUR_04_06.getId(), true),
            ChangePrice.of(DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY,
                DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX, true),
            SetProductPriceCustomField.ofJson("foo",
                DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX.getCustom().getFields().get("foo"),
                DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY.getId(), true),
            ChangePrice.of(DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX,
                DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX, true),
            SetProductPriceCustomType.ofTypeIdAndJson(
                DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX.getCustom().getType().getId(),
                DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX.getCustom().getFields(),
                DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX.getId(), true),
            ChangePrice.of(NE_123_EUR_01_04, DRAFT_NE_777_EUR_01_04, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_222_EUR_CUST1, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_111_EUR_01_02, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_111_EUR_03_04, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_DE_333_USD_CUST1, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_UK_999_GBP, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_US_666_USD_CUST2_01_02, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_FR_888_EUR_01_03, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_FR_999_EUR_03_06, true),
            AddPrice.ofVariantId(oldProductVariant.getId(), DRAFT_NE_777_EUR_05_07, true)
        );


    }
}
