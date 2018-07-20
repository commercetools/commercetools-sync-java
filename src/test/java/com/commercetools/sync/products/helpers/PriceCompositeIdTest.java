package com.commercetools.sync.products.helpers;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.products.PriceDraft;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_111_USD;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_EUR_01_02;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_222_EUR_CUST1;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_NE_777_CUST1_CHANNEL1_EUR_05_07;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_US_111_USD;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_US_666_USD_CUST1_01_02;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.byMonth;
import static com.neovisionaries.i18n.CountryCode.DE;
import static com.neovisionaries.i18n.CountryCode.NE;
import static com.neovisionaries.i18n.CountryCode.US;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PriceCompositeIdTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("ofProductDraftTestCases")
    void ofPriceDraft_ShouldCreateCompositeId(@Nonnull final String testCaseName,
                                              @Nonnull final PriceDraft priceDraft,
                                              @Nonnull final String currencyCode,
                                              @Nullable final CountryCode countryCode,
                                              @Nullable final String customerGroupId,
                                              @Nullable final String channelId,
                                              @Nullable final ZonedDateTime validFrom,
                                              @Nullable final ZonedDateTime validUntil) {

        final PriceCompositeId priceCompositeId = PriceCompositeId.of(priceDraft);

        assertEquals(priceCompositeId.getCurrencyCode(), currencyCode);
        assertEquals(priceCompositeId.getCountryCode(), countryCode);
        assertEquals(priceCompositeId.getCustomerGroupId(), customerGroupId);
        assertEquals(priceCompositeId.getChannelId(), channelId);
        assertEquals(priceCompositeId.getValidFrom(), validFrom);
        assertEquals(priceCompositeId.getValidUntil(), validUntil);
    }

    private static Stream<Arguments> ofProductDraftTestCases() {
        final String CASE_1 = "with currency only";
        final String CASE_2 = "with currency and countryCode";
        final String CASE_3 = "with currency, countryCode, validFrom and validUntil";
        final String CASE_4 = "with currency, countryCode and CustomerGroup";
        final String CASE_5 = "with currency, countryCode, CustomerGroup, validFrom and validUntil";
        final String CASE_6 = "with currency, countryCode, channel, validFrom and validUntil";
        final String CASE_7 = "with currency, countryCode, CustomerGroup, channel, validFrom and validUntil";

        return Stream.of(
            Arguments.of(CASE_1, DRAFT_111_USD, "USD", null, null, null, null, null),
            Arguments.of(CASE_2, DRAFT_US_111_USD, "USD", US, null, null, null, null),
            Arguments.of(CASE_3, DRAFT_DE_111_EUR_01_02, "EUR", DE, null, null, byMonth(1), byMonth(2)),
            Arguments.of(CASE_4, DRAFT_DE_222_EUR_CUST1, "EUR", DE, "cust1", null, null, null),
            Arguments.of(CASE_5, DRAFT_US_666_USD_CUST1_01_02, "USD", US, "cust1", null, byMonth(1), byMonth(2)),
            Arguments.of(CASE_6, DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX, "EUR", DE, null, "channel1",
                byMonth(1), byMonth(2)),
            Arguments.of(CASE_7, DRAFT_NE_777_CUST1_CHANNEL1_EUR_05_07, "EUR", NE, "cust1", "channel1",
                byMonth(5), byMonth(7))
        );
    }
}
