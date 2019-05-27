package com.commercetools.sync.internals.helpers;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.*;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.*;
import static com.neovisionaries.i18n.CountryCode.DE;
import static com.neovisionaries.i18n.CountryCode.NE;
import static com.neovisionaries.i18n.CountryCode.US;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceCompositeIdTest {
    private static final String CASE_1 = "with currency only";
    private static final String CASE_2 = "with currency and countryCode";
    private static final String CASE_3 = "with currency and channel";
    private static final String CASE_4 = "with currency, countryCode and channel";
    private static final String CASE_5 = "with currency and customerGroup";
    private static final String CASE_6 = "with currency, countryCode and customerGroup";
    private static final String CASE_7 = "with currency, channel and customerGroup";
    private static final String CASE_8 = "with currency, countryCode, channel and customerGroup";

    private static final String CASE_9 = "with currency and validFrom";
    private static final String CASE_10 = "with currency, countryCode and validFrom";
    private static final String CASE_11 = "with currency, channel and validFrom";
    private static final String CASE_12 = "with currency, countryCode, channel and validFrom";
    private static final String CASE_13 = "with currency, customerGroup and validFrom";
    private static final String CASE_14 = "with currency, countryCode, customerGroup and validFrom";
    private static final String CASE_15 = "with currency, channel, customerGroup and validFrom";
    private static final String CASE_16 = "with currency, countryCode, channel, customerGroup and validFrom";

    private static final String CASE_17 = "with currency and validUntil";
    private static final String CASE_18 = "with currency, countryCode and validUntil";
    private static final String CASE_19 = "with currency, channel and validUntil";
    private static final String CASE_20 = "with currency, countryCode, channel and validUntil";
    private static final String CASE_21 = "with currency, customerGroup and validUntil";
    private static final String CASE_22 = "with currency, countryCode, customerGroup and validUntil";
    private static final String CASE_23 = "with currency, channel, customerGroup and validUntil";
    private static final String CASE_24 = "with currency, countryCode, channel, customerGroup and validUntil";

    private static final String CASE_25 = "with currency, validFrom and validUntil";
    private static final String CASE_26 = "with currency, countryCode, validFrom and validUntil";
    private static final String CASE_27 = "with currency, channel, validFrom and validUntil";
    private static final String CASE_28 = "with currency, countryCode, channel, validFrom and validUntil";
    private static final String CASE_29 = "with currency, customerGroup, validFrom and validUntil";
    private static final String CASE_30 = "with currency, countryCode, customerGroup, validFrom and validUntil";
    private static final String CASE_31 = "with currency, channel, customerGroup, validFrom and validUntil";
    private static final String CASE_32 =
        "with currency, countryCode, channel, customerGroup, validFrom and validUntil";

    @ParameterizedTest(name = "[#ofPriceDraft]: {0}")
    @MethodSource("ofPriceDraftTestCases")
    void ofPriceDraft_ShouldCreateCompositeId(@Nonnull final String testCaseName,
                                              @Nonnull final PriceDraft priceDraft,
                                              @Nonnull final String currencyCode,
                                              @Nullable final CountryCode countryCode,
                                              @Nullable final String customerGroupId,
                                              @Nullable final String channelId,
                                              @Nullable final ZonedDateTime validFrom,
                                              @Nullable final ZonedDateTime validUntil) {

        final PriceCompositeId priceCompositeId = PriceCompositeId.of(priceDraft);

        assertCompositeId(currencyCode, countryCode, customerGroupId, channelId, validFrom, validUntil,
            priceCompositeId);
    }

    private static Stream<Arguments> ofPriceDraftTestCases() {
        return Stream.of(
            Arguments.of(CASE_1, DRAFT_111_USD, "USD", null, null, null, null, null),
            Arguments.of(CASE_2, DRAFT_US_111_USD, "USD", US, null, null, null, null),
            Arguments.of(CASE_3, DRAFT_111_USD_CHANNEL1, "USD", null, null, "channel1", null, null),
            Arguments.of(CASE_4, DRAFT_US_111_USD_CHANNEL1, "USD", US, null, "channel1", null, null),
            Arguments.of(CASE_5, DRAFT_111_USD_CUST1, "USD", null, "cust1", null, null, null),
            Arguments.of(CASE_6, DRAFT_DE_333_USD_CUST1, "USD", DE, "cust1", null, null, null),
            Arguments.of(CASE_7, DRAFT_111_USD_CHANNEL1_CUST1, "USD", null, "cust1", "channel1", null, null),
            Arguments.of(CASE_8, DRAFT_US_111_USD_CHANNEL1_CUST1, "USD", US, "cust1", "channel1", null, null),

            Arguments.of(CASE_9, DRAFT_111_USD_FROM_01, "USD", null, null, null, byMonth(1), null),
            Arguments.of(CASE_10, DRAFT_US_111_USD_FROM_01, "USD", US, null, null, byMonth(1), null),
            Arguments.of(CASE_11, DRAFT_111_USD_CHANNEL1_FROM_01, "USD", null, null, "channel1", byMonth(1), null),
            Arguments.of(CASE_12, DRAFT_US_111_USD_CHANNEL1_FROM_01, "USD", US, null, "channel1", byMonth(1), null),
            Arguments.of(CASE_13, DRAFT_111_USD_CUST1_FROM_01, "USD", null, "cust1", null, byMonth(1), null),
            Arguments.of(CASE_14, DRAFT_US_111_USD_CUST1_FROM_01, "USD", US, "cust1", null, byMonth(1), null),
            Arguments
                .of(CASE_15, DRAFT_111_USD_CHANNEL1_CUST1_FROM_01, "USD", null, "cust1", "channel1", byMonth(1), null),
            Arguments
                .of(CASE_16, DRAFT_US_111_USD_CHANNEL1_CUST1_FROM_01, "USD", US, "cust1", "channel1", byMonth(1), null),

            Arguments.of(CASE_17, DRAFT_111_USD_UNTIL_01, "USD", null, null, null, null, byMonth(1)),
            Arguments.of(CASE_18, DRAFT_US_111_USD_UNTIL_01, "USD", US, null, null, null, byMonth(1)),
            Arguments.of(CASE_19, DRAFT_111_USD_CHANNEL1_UNTIL_01, "USD", null, null, "channel1", null, byMonth(1)),
            Arguments.of(CASE_20, DRAFT_US_111_USD_CHANNEL1_UNTIL_01, "USD", US, null, "channel1", null, byMonth(1)),
            Arguments.of(CASE_21, DRAFT_111_USD_CUST1_UNTIL_01, "USD", null, "cust1", null, null, byMonth(1)),
            Arguments.of(CASE_22, DRAFT_US_111_USD_CUST1_UNTIL_01, "USD", US, "cust1", null, null, byMonth(1)),
            Arguments
                .of(CASE_23, DRAFT_111_USD_CHANNEL1_CUST1_UNTIL_01, "USD", null, "cust1", "channel1", null, byMonth(1)),
            Arguments.of(CASE_24, DRAFT_US_111_USD_CHANNEL1_CUST1_UNTIL_01, "USD", US, "cust1", "channel1", null,
                byMonth(1)),

            Arguments.of(CASE_25, DRAFT_111_EUR_01_02, "EUR", null, null, null, byMonth(1), byMonth(2)),
            Arguments.of(CASE_26, DRAFT_DE_111_EUR_01_02, "EUR", DE, null, null, byMonth(1), byMonth(2)),
            Arguments.of(CASE_27, DRAFT_111_EUR_CHANNEL1_01_02, "EUR", null, null, "channel1", byMonth(1), byMonth(2)),
            Arguments.of(CASE_28, DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX, "EUR", DE, null, "channel1",
                byMonth(1), byMonth(2)),
            Arguments.of(CASE_29, DRAFT_666_USD_CUST1_01_02, "USD", null, "cust1", null, byMonth(1), byMonth(2)),
            Arguments.of(CASE_30, DRAFT_US_666_USD_CUST1_01_02, "USD", US, "cust1", null, byMonth(1), byMonth(2)),
            Arguments.of(CASE_31, DRAFT_777_CUST1_CHANNEL1_EUR_05_07, "EUR", null, "cust1", "channel1", byMonth(5),
                byMonth(7)),
            Arguments.of(CASE_32, DRAFT_NE_777_CUST1_CHANNEL1_EUR_05_07, "EUR", NE, "cust1", "channel1",
                byMonth(5), byMonth(7))
        );
    }

    @ParameterizedTest(name = "[#ofPrice]: {0}")
    @MethodSource("ofPriceTestCases")
    void ofPrice_ShouldCreateCompositeId(@Nonnull final String testCaseName,
                                         @Nonnull final Price price,
                                         @Nonnull final String currencyCode,
                                         @Nullable final CountryCode countryCode,
                                         @Nullable final String customerGroupId,
                                         @Nullable final String channelId,
                                         @Nullable final ZonedDateTime validFrom,
                                         @Nullable final ZonedDateTime validUntil) {

        final PriceCompositeId priceCompositeId = PriceCompositeId.of(price);

        assertCompositeId(currencyCode, countryCode, customerGroupId, channelId, validFrom, validUntil,
            priceCompositeId);
    }


    private static Stream<Arguments> ofPriceTestCases() {
        return Stream.of(
            Arguments.of(CASE_1, USD_111, "USD", null, null, null, null, null),
            Arguments.of(CASE_2, US_111_USD, "USD", US, null, null, null, null),
            Arguments.of(CASE_3, EUR_345_CHANNEL1, "EUR", null, null, "channel1", null, null),
            Arguments.of(CASE_4, DE_345_EUR_CHANNEL1, "EUR", DE, null, "channel1", null, null),
            Arguments.of(CASE_5, EUR_345_CUST2, "EUR", null, "cust2", null, null, null),
            Arguments.of(CASE_6, DE_345_EUR_CUST2, "EUR", DE, "cust2", null, null, null),
            Arguments.of(CASE_7, EUR_345_CHANNEL1_CUST1, "EUR", null, "cust1", "channel1", null, null),
            Arguments.of(CASE_8, DE_345_EUR_CHANNEL1_CUST1, "EUR", DE, "cust1", "channel1", null, null),

            Arguments.of(CASE_9, USD_111_FROM_01, "USD", null, null, null, byMonth(1), null),
            Arguments.of(CASE_10, US_111_USD_FROM_01, "USD", US, null, null, byMonth(1), null),
            Arguments.of(CASE_11, USD_111_CHANNEL1_FROM_01, "USD", null, null, "channel1", byMonth(1), null),
            Arguments.of(CASE_12, US_111_USD_CHANNEL1_FROM_01, "USD", US, null, "channel1", byMonth(1), null),
            Arguments.of(CASE_13, USD_111_CUST1_FROM_01, "USD", null, "cust1", null, byMonth(1), null),
            Arguments.of(CASE_14, US_111_USD_CUST1_FROM_01, "USD", US, "cust1", null, byMonth(1), null),
            Arguments.of(CASE_15, USD_111_CHANNEL1_CUST1_FROM_01, "USD", null, "cust1", "channel1", byMonth(1), null),
            Arguments.of(CASE_16, US_111_USD_CHANNEL1_CUST1_FROM_01, "USD", US, "cust1", "channel1", byMonth(1), null),

            Arguments.of(CASE_17, USD_111_UNTIL_01, "USD", null, null, null, null, byMonth(1)),
            Arguments.of(CASE_18, US_111_USD_UNTIL_01, "USD", US, null, null, null, byMonth(1)),
            Arguments.of(CASE_19, USD_111_CHANNEL1_UNTIL_01, "USD", null, null, "channel1", null, byMonth(1)),
            Arguments.of(CASE_20, US_111_USD_CHANNEL1_UNTIL_01, "USD", US, null, "channel1", null, byMonth(1)),
            Arguments.of(CASE_21, USD_111_CUST1_UNTIL_01, "USD", null, "cust1", null, null, byMonth(1)),
            Arguments.of(CASE_22, US_111_USD_CUST1_UNTIL_01, "USD", US, "cust1", null, null, byMonth(1)),
            Arguments.of(CASE_23, USD_111_CHANNEL1_CUST1_UNTIL_01, "USD", null, "cust1", "channel1", null, byMonth(1)),
            Arguments.of(CASE_24, US_111_USD_CHANNEL1_CUST1_UNTIL_01, "USD", US, "cust1", "channel1", null, byMonth(1)),

            Arguments.of(CASE_25, USD_111_01_02, "USD", null, null, null, byMonth(1), byMonth(2)),
            Arguments.of(CASE_26, US_111_USD_01_02, "USD", US, null, null, byMonth(1), byMonth(2)),
            Arguments.of(CASE_27, USD_111_CHANNEL1_01_02, "USD", null, null, "channel1", byMonth(1), byMonth(2)),
            Arguments.of(CASE_28, DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX, "EUR", DE, null, "channel2",
                byMonth(1), byMonth(2)),
            Arguments.of(CASE_29, USD_111_CUST1_01_02, "USD", null, "cust1", null, byMonth(1), byMonth(2)),
            Arguments.of(CASE_30, US_111_USD_CUST1_01_02, "USD", US, "cust1", null, byMonth(1), byMonth(2)),
            Arguments.of(CASE_31, USD_555_CUST2_CHANNEL1_01_02, "USD", null, "cust2", "channel1", byMonth(1),
                byMonth(2)),
            Arguments.of(CASE_32, US_555_USD_CUST2_CHANNEL1_01_02, "USD", US, "cust2", "channel1", byMonth(1),
                byMonth(2))
        );
    }


    private static void assertCompositeId(@Nonnull final String currencyCode,
                                          @Nullable final CountryCode countryCode,
                                          @Nullable final String customerGroupId,
                                          @Nullable final String channelId,
                                          @Nullable final ZonedDateTime validFrom,
                                          @Nullable final ZonedDateTime validUntil,
                                          @Nonnull final PriceCompositeId priceCompositeId) {

        assertEquals(currencyCode, priceCompositeId.getCurrencyCode());
        assertEquals(countryCode, priceCompositeId.getCountryCode());
        assertEquals(customerGroupId, priceCompositeId.getCustomerGroupId());
        assertEquals(channelId, priceCompositeId.getChannelId());
        assertEquals(validFrom, priceCompositeId.getValidFrom());
        assertEquals(validUntil, priceCompositeId.getValidUntil());
    }

    @ParameterizedTest(name = "[#equals]: {0}")
    @MethodSource("equalsTestCases")
    @SuppressWarnings("PMD")
    void equals(@Nonnull final String testCaseName,
                @Nonnull final PriceCompositeId thisPriceCompositeId,
                @Nullable final Object otherObject,
                final boolean expectedResult) {

        final boolean result = thisPriceCompositeId.equals(otherObject);

        assertEquals(expectedResult, result);
    }

    private static Stream<Arguments> equalsTestCases() {
        final PriceCompositeId priceCompositeId = PriceCompositeId.of(USD_111);
        return Stream.of(
            Arguments.of("Null priceCompositeId", priceCompositeId, null, false),
            Arguments.of("Not instance of priceCompositeId", priceCompositeId, "foo", false),
            Arguments.of("Exact same instances", priceCompositeId, priceCompositeId, true),
            Arguments.of("Equal but different instances", priceCompositeId, PriceCompositeId.of(USD_111), true),
            Arguments.of("Not equal countryCode", priceCompositeId, PriceCompositeId.of(DE_111_EUR), false),
            Arguments.of("Not equal currencyCode", priceCompositeId, PriceCompositeId.of(EUR_345_CHANNEL1), false),
            Arguments.of("Not equal channelId", priceCompositeId, PriceCompositeId.of(USD_111_CHANNEL1_01_02), false),
            Arguments.of("Not equal customerGroupId", priceCompositeId, PriceCompositeId.of(USD_111_CUST1_01_02),
                false),
            Arguments.of("Not equal validFrom", priceCompositeId, PriceCompositeId.of(USD_111_FROM_01), false),
            Arguments.of("Not equal validUntil", priceCompositeId, PriceCompositeId.of(USD_111_UNTIL_01), false)
        );
    }
}
