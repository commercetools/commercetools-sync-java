package com.commercetools.sync.products.utils.productvariantupdateactionutils.prices;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceBuilder;
import io.sphere.sdk.types.CustomFields;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.money.CurrencyUnit;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockCustomFields;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.GBP;
import static com.neovisionaries.i18n.CountryCode.DE;
import static com.neovisionaries.i18n.CountryCode.FR;
import static com.neovisionaries.i18n.CountryCode.NE;
import static com.neovisionaries.i18n.CountryCode.UK;
import static com.neovisionaries.i18n.CountryCode.US;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static io.sphere.sdk.models.DefaultCurrencyUnits.USD;

final class PriceFixtures {

    static final Price US_111_USD = getPrice(BigDecimal.valueOf(111), USD, US,
        null, null, null, null, null);

    static final Price DE_111_EUR = getPrice(BigDecimal.valueOf(111), EUR, DE, null, null, null, null, null);

    static final Price DE_111_EUR_01_02 = getPrice(BigDecimal.valueOf(111), EUR,
        DE, null,
        ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()), null, null);

    static final Price DE_111_EUR_02_03 = getPrice(BigDecimal.valueOf(111), EUR,
        DE, null,
        ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 3, 1, 0, 0, 0, 0, ZoneId.systemDefault()), null, null);

    static final Price DE_111_USD = getPrice(BigDecimal.valueOf(111), USD, DE,
        null, null, null, null, null);

    static final Price DE_345_EUR_CUST2 = getPrice(BigDecimal.valueOf(345), EUR, DE, "cust2",
        null, null, null, null);
    static final Price DE_567_EUR_CUST3 = getPrice(BigDecimal.valueOf(567), EUR, DE, "cust3",
        null, null, null, null);

    static final Price UK_111_GBP_01_02 = getPrice(BigDecimal.valueOf(111), GBP, UK,
        null, ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()), null, null);

    static final Price UK_111_GBP_02_03 = getPrice(BigDecimal.valueOf(111), GBP, UK,
        null, ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 3, 1, 0, 0, 0, 0, ZoneId.systemDefault()), null, null);


    static final Price UK_333_GBP_02_05 = getPrice(BigDecimal.valueOf(333), GBP, UK,
        null, ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 5, 1, 0, 0, 0, 0, ZoneId.systemDefault()), null, null);

    static final Price US_555_USD_CUST2_01_02 = getPrice(BigDecimal.valueOf(555), USD, US,
        "cust2", ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()), null, null);

    static final Price FR_777_EUR_01_04 = getPrice(BigDecimal.valueOf(777), EUR, FR,
        null, ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 4, 1, 0, 0, 0, 0, ZoneId.systemDefault()), null, null);

    static final Price NE_123_EUR_01_04 = getPrice(BigDecimal.valueOf(123), EUR, NE,
        null, ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 4, 1, 0, 0, 0, 0, ZoneId.systemDefault()), null, null);

    static final Price NE_321_EUR_04_06 = getPrice(BigDecimal.valueOf(321), EUR, NE,
        null, ZonedDateTime.of(2018, 4, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 6, 1, 0, 0, 0, 0, ZoneId.systemDefault()), null, null);

    static final Price DE_666_EUR = getPrice(BigDecimal.valueOf(666), EUR, DE, null, null, null, null, null);

    static final Price DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY = getPrice(BigDecimal.valueOf(222),
        EUR,
        DE, null,
        ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        "channel1", getMockCustomFields("customType1", "foo", JsonNodeFactory.instance.textNode("Y")));

    static final Price DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX = getPrice(BigDecimal.valueOf(222),
        EUR,
        DE, null,
        ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        "channel2", getMockCustomFields("customType2", "foo", JsonNodeFactory.instance.textNode("X")));

    static final Price UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDY = getPrice(BigDecimal.valueOf(22), GBP,
        UK, null, null, null, null,
        getMockCustomFields("customType1", "foo", JsonNodeFactory.instance.textNode("Y")));

    static final Price UK_22_USD_CUSTOMTYPE2_CUSTOMFIELDX = getPrice(BigDecimal.valueOf(22), USD,
        UK, null, null, null, null,
        getMockCustomFields("customType2", "foo", JsonNodeFactory.instance.textNode("X")));

    static final Price UK_1_GBP_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX = getPrice(BigDecimal.valueOf(1), GBP,
        UK, null, null, null, "channel1",
        getMockCustomFields("customType1", "foo", JsonNodeFactory.instance.textNode("X")));

    static final Price DE_22_USD = getPrice(BigDecimal.valueOf(22), USD, DE,
        null, null, null, null, null);


    @Nonnull
    private static Price getPrice(@Nonnull final BigDecimal value,
                                  @Nonnull final CurrencyUnit currencyUnits,
                                  @Nonnull final CountryCode countryCode,
                                  @Nullable final String customerGroupId,
                                  @Nullable final ZonedDateTime validFrom,
                                  @Nullable final ZonedDateTime validUntil,
                                  @Nullable final String channelId,
                                  @Nullable final CustomFields customFields) {

        return PriceBuilder.of(Price.of(value, currencyUnits))
                           .id(UUID.randomUUID().toString())
                           .customerGroup(CustomerGroup.referenceOfId(customerGroupId))
                           .country(countryCode)
                           .validFrom(validFrom)
                           .validUntil(validUntil)
                           .channel(Channel.referenceOfId(channelId))
                           .custom(customFields)
                           .build();
    }


    private PriceFixtures() {
    }
}
