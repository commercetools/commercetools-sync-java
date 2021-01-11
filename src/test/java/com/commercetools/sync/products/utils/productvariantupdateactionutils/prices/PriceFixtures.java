package com.commercetools.sync.products.utils.productvariantupdateactionutils.prices;

import static com.commercetools.sync.commons.MockUtils.getMockCustomFields;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.GBP;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.byMonth;
import static com.neovisionaries.i18n.CountryCode.DE;
import static com.neovisionaries.i18n.CountryCode.FR;
import static com.neovisionaries.i18n.CountryCode.NE;
import static com.neovisionaries.i18n.CountryCode.UK;
import static com.neovisionaries.i18n.CountryCode.US;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static io.sphere.sdk.models.DefaultCurrencyUnits.USD;
import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceBuilder;
import io.sphere.sdk.types.CustomFields;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.money.CurrencyUnit;

public final class PriceFixtures {
  public static final Price USD_111 =
      getPrice(BigDecimal.valueOf(111), USD, null, null, null, null, null, null);

  public static final Price US_111_USD =
      getPrice(BigDecimal.valueOf(111), USD, US, null, null, null, null, null);

  public static final Price DE_111_EUR =
      getPrice(BigDecimal.valueOf(111), EUR, DE, null, null, null, null, null);

  public static final Price DE_111_EUR_01_02 =
      getPrice(BigDecimal.valueOf(111), EUR, DE, null, byMonth(1), byMonth(2), null, null);

  public static final Price DE_111_EUR_02_03 =
      getPrice(BigDecimal.valueOf(111), EUR, DE, null, byMonth(2), byMonth(3), null, null);

  public static final Price DE_111_USD =
      getPrice(BigDecimal.valueOf(111), USD, DE, null, null, null, null, null);

  public static final Price EUR_345_CHANNEL1 =
      getPrice(BigDecimal.valueOf(345), EUR, null, null, null, null, "channel1", null);

  public static final Price EUR_345_CHANNEL1_CUST1 =
      getPrice(BigDecimal.valueOf(345), EUR, null, "cust1", null, null, "channel1", null);

  public static final Price DE_345_EUR_CHANNEL1_CUST1 =
      getPrice(BigDecimal.valueOf(345), EUR, DE, "cust1", null, null, "channel1", null);

  public static final Price DE_345_EUR_CHANNEL1 =
      getPrice(BigDecimal.valueOf(345), EUR, DE, null, null, null, "channel1", null);

  public static final Price EUR_345_CUST2 =
      getPrice(BigDecimal.valueOf(345), EUR, null, "cust2", null, null, null, null);
  public static final Price DE_345_EUR_CUST2 =
      getPrice(BigDecimal.valueOf(345), EUR, DE, "cust2", null, null, null, null);
  public static final Price DE_567_EUR_CUST3 =
      getPrice(BigDecimal.valueOf(567), EUR, DE, "cust3", null, null, null, null);

  public static final Price USD_111_FROM_01 =
      getPrice(BigDecimal.valueOf(111), USD, null, null, byMonth(1), null, null, null);

  public static final Price US_111_USD_FROM_01 =
      getPrice(BigDecimal.valueOf(111), USD, US, null, byMonth(1), null, null, null);

  public static final Price USD_111_CHANNEL1_FROM_01 =
      getPrice(BigDecimal.valueOf(111), USD, null, null, byMonth(1), null, "channel1", null);

  public static final Price US_111_USD_CHANNEL1_FROM_01 =
      getPrice(BigDecimal.valueOf(111), USD, US, null, byMonth(1), null, "channel1", null);

  public static final Price USD_111_CUST1_FROM_01 =
      getPrice(BigDecimal.valueOf(111), USD, null, "cust1", byMonth(1), null, null, null);

  public static final Price US_111_USD_CUST1_FROM_01 =
      getPrice(BigDecimal.valueOf(111), USD, US, "cust1", byMonth(1), null, null, null);

  public static final Price USD_111_CHANNEL1_CUST1_FROM_01 =
      getPrice(BigDecimal.valueOf(111), USD, null, "cust1", byMonth(1), null, "channel1", null);

  public static final Price US_111_USD_CHANNEL1_CUST1_FROM_01 =
      getPrice(BigDecimal.valueOf(111), USD, US, "cust1", byMonth(1), null, "channel1", null);

  public static final Price USD_111_UNTIL_01 =
      getPrice(BigDecimal.valueOf(111), USD, null, null, null, byMonth(1), null, null);

  public static final Price US_111_USD_UNTIL_01 =
      getPrice(BigDecimal.valueOf(111), USD, US, null, null, byMonth(1), null, null);

  public static final Price USD_111_CHANNEL1_UNTIL_01 =
      getPrice(BigDecimal.valueOf(111), USD, null, null, null, byMonth(1), "channel1", null);

  public static final Price US_111_USD_CHANNEL1_UNTIL_01 =
      getPrice(BigDecimal.valueOf(111), USD, US, null, null, byMonth(1), "channel1", null);

  public static final Price USD_111_CUST1_UNTIL_01 =
      getPrice(BigDecimal.valueOf(111), USD, null, "cust1", null, byMonth(1), null, null);

  public static final Price US_111_USD_CUST1_UNTIL_01 =
      getPrice(BigDecimal.valueOf(111), USD, US, "cust1", null, byMonth(1), null, null);

  public static final Price USD_111_CHANNEL1_CUST1_UNTIL_01 =
      getPrice(BigDecimal.valueOf(111), USD, null, "cust1", null, byMonth(1), "channel1", null);

  public static final Price US_111_USD_CHANNEL1_CUST1_UNTIL_01 =
      getPrice(BigDecimal.valueOf(111), USD, US, "cust1", null, byMonth(1), "channel1", null);

  public static final Price USD_111_01_02 =
      getPrice(BigDecimal.valueOf(111), USD, null, null, byMonth(1), byMonth(2), null, null);

  public static final Price US_111_USD_01_02 =
      getPrice(BigDecimal.valueOf(111), USD, US, null, byMonth(1), byMonth(2), null, null);

  public static final Price USD_111_CUST1_01_02 =
      getPrice(BigDecimal.valueOf(111), USD, null, "cust1", byMonth(1), byMonth(2), null, null);

  public static final Price US_111_USD_CUST1_01_02 =
      getPrice(BigDecimal.valueOf(111), USD, US, "cust1", byMonth(1), byMonth(2), null, null);

  public static final Price USD_111_CHANNEL1_01_02 =
      getPrice(BigDecimal.valueOf(111), USD, null, null, byMonth(1), byMonth(2), "channel1", null);

  public static final Price UK_111_GBP_01_02 =
      getPrice(BigDecimal.valueOf(111), GBP, UK, null, byMonth(1), byMonth(2), null, null);

  public static final Price UK_111_GBP_02_03 =
      getPrice(BigDecimal.valueOf(111), GBP, UK, null, byMonth(2), byMonth(3), null, null);

  public static final Price UK_333_GBP_03_05 =
      getPrice(BigDecimal.valueOf(333), GBP, UK, null, byMonth(3), byMonth(5), null, null);

  public static final Price US_555_USD_CUST2_01_02 =
      getPrice(BigDecimal.valueOf(555), USD, US, "cust2", byMonth(1), byMonth(2), null, null);

  public static final Price USD_555_CUST2_CHANNEL1_01_02 =
      getPrice(
          BigDecimal.valueOf(555), USD, null, "cust2", byMonth(1), byMonth(2), "channel1", null);

  public static final Price US_555_USD_CUST2_CHANNEL1_01_02 =
      getPrice(BigDecimal.valueOf(555), USD, US, "cust2", byMonth(1), byMonth(2), "channel1", null);

  public static final Price FR_777_EUR_01_04 =
      getPrice(BigDecimal.valueOf(777), EUR, FR, null, byMonth(1), byMonth(4), null, null);

  public static final Price NE_123_EUR_01_04 =
      getPrice(BigDecimal.valueOf(123), EUR, NE, null, byMonth(1), byMonth(4), null, null);

  public static final Price NE_321_EUR_04_06 =
      getPrice(BigDecimal.valueOf(321), EUR, NE, null, byMonth(4), byMonth(6), null, null);

  public static final Price DE_666_EUR =
      getPrice(BigDecimal.valueOf(666), EUR, DE, null, null, null, null, null);

  public static final Price DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY =
      getPrice(
          BigDecimal.valueOf(222),
          EUR,
          DE,
          null,
          byMonth(1),
          byMonth(2),
          "channel1",
          getMockCustomFields("customType1", "foo", JsonNodeFactory.instance.textNode("Y")));

  public static final Price DE_222_EUR_01_02_CHANNEL2_CUSTOMTYPE2_CUSTOMFIELDX =
      getPrice(
          BigDecimal.valueOf(222),
          EUR,
          DE,
          null,
          byMonth(1),
          byMonth(2),
          "channel2",
          getMockCustomFields("customType2", "foo", JsonNodeFactory.instance.textNode("X")));

  public static final Price UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDY =
      getPrice(
          BigDecimal.valueOf(22),
          GBP,
          UK,
          null,
          null,
          null,
          null,
          getMockCustomFields("customType1", "foo", JsonNodeFactory.instance.textNode("Y")));

  public static final Price UK_22_USD_CUSTOMTYPE2_CUSTOMFIELDX =
      getPrice(
          BigDecimal.valueOf(22),
          USD,
          UK,
          null,
          null,
          null,
          null,
          getMockCustomFields("customType2", "foo", JsonNodeFactory.instance.textNode("X")));

  public static final Price UK_1_GBP_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX =
      getPrice(
          BigDecimal.valueOf(1),
          GBP,
          UK,
          null,
          null,
          null,
          "channel1",
          getMockCustomFields("customType1", "foo", JsonNodeFactory.instance.textNode("X")));

  public static final Price DE_22_USD =
      getPrice(BigDecimal.valueOf(22), USD, DE, null, null, null, null, null);

  @Nonnull
  private static Price getPrice(
      @Nonnull final BigDecimal value,
      @Nonnull final CurrencyUnit currencyUnits,
      @Nullable final CountryCode countryCode,
      @Nullable final String customerGroupId,
      @Nullable final ZonedDateTime validFrom,
      @Nullable final ZonedDateTime validUntil,
      @Nullable final String channelId,
      @Nullable final CustomFields customFields) {

    return PriceBuilder.of(Price.of(value, currencyUnits))
        .id(UUID.randomUUID().toString())
        .customerGroup(ofNullable(customerGroupId).map(CustomerGroup::referenceOfId).orElse(null))
        .country(countryCode)
        .validFrom(validFrom)
        .validUntil(validUntil)
        .channel(ofNullable(channelId).map(Channel::referenceOfId).orElse(null))
        .custom(customFields)
        .build();
  }

  private PriceFixtures() {}
}
