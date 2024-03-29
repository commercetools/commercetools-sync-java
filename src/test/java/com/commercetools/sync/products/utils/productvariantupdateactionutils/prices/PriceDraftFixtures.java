package com.commercetools.sync.products.utils.productvariantupdateactionutils.prices;

import static com.commercetools.api.models.common.DefaultCurrencyUnits.EUR;
import static com.commercetools.api.models.common.DefaultCurrencyUnits.USD;
import static com.neovisionaries.i18n.CountryCode.*;
import static java.util.Optional.ofNullable;

import com.commercetools.api.models.channel.ChannelResourceIdentifier;
import com.commercetools.api.models.common.CentPrecisionMoneyBuilder;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.common.TypedMoney;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifier;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainer;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.neovisionaries.i18n.CountryCode;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.money.CurrencyUnit;
import javax.money.Monetary;

public final class PriceDraftFixtures {
  public static final PriceDraft DRAFT_111_USD =
      getPriceDraft(BigDecimal.valueOf(111), USD, null, null, null, null, null, null);

  public static final PriceDraft DRAFT_111_USD_CHANNEL1 =
      getPriceDraft(BigDecimal.valueOf(111), USD, null, null, null, null, "channel1", null);

  public static final PriceDraft DRAFT_US_111_USD =
      getPriceDraft(BigDecimal.valueOf(111), USD, US, null, null, null, null, null);

  public static final PriceDraft DRAFT_US_111_USD_CHANNEL1 =
      getPriceDraft(BigDecimal.valueOf(111), USD, US, null, null, null, "channel1", null);

  public static final PriceDraft DRAFT_111_USD_CUST1 =
      getPriceDraft(BigDecimal.valueOf(111), USD, null, "cust1", null, null, null, null);

  public static final PriceDraft DRAFT_111_USD_CHANNEL1_CUST1 =
      getPriceDraft(BigDecimal.valueOf(111), USD, null, "cust1", null, null, "channel1", null);

  public static final PriceDraft DRAFT_US_111_USD_CHANNEL1_CUST1 =
      getPriceDraft(BigDecimal.valueOf(111), USD, US, "cust1", null, null, "channel1", null);

  public static final PriceDraft DRAFT_111_USD_FROM_01 =
      getPriceDraft(BigDecimal.valueOf(111), USD, null, null, byMonth(1), null, null, null);

  public static final PriceDraft DRAFT_US_111_USD_FROM_01 =
      getPriceDraft(BigDecimal.valueOf(111), USD, US, null, byMonth(1), null, null, null);

  public static final PriceDraft DRAFT_111_USD_CHANNEL1_FROM_01 =
      getPriceDraft(BigDecimal.valueOf(111), USD, null, null, byMonth(1), null, "channel1", null);

  public static final PriceDraft DRAFT_US_111_USD_CHANNEL1_FROM_01 =
      getPriceDraft(BigDecimal.valueOf(111), USD, US, null, byMonth(1), null, "channel1", null);

  public static final PriceDraft DRAFT_111_USD_CUST1_FROM_01 =
      getPriceDraft(BigDecimal.valueOf(111), USD, null, "cust1", byMonth(1), null, null, null);

  public static final PriceDraft DRAFT_US_111_USD_CUST1_FROM_01 =
      getPriceDraft(BigDecimal.valueOf(111), USD, US, "cust1", byMonth(1), null, null, null);

  public static final PriceDraft DRAFT_111_USD_CHANNEL1_CUST1_FROM_01 =
      getPriceDraft(
          BigDecimal.valueOf(111), USD, null, "cust1", byMonth(1), null, "channel1", null);

  public static final PriceDraft DRAFT_US_111_USD_CHANNEL1_CUST1_FROM_01 =
      getPriceDraft(BigDecimal.valueOf(111), USD, US, "cust1", byMonth(1), null, "channel1", null);

  public static final PriceDraft DRAFT_111_USD_UNTIL_01 =
      getPriceDraft(BigDecimal.valueOf(111), USD, null, null, null, byMonth(1), null, null);

  public static final PriceDraft DRAFT_US_111_USD_UNTIL_01 =
      getPriceDraft(BigDecimal.valueOf(111), USD, US, null, null, byMonth(1), null, null);

  public static final PriceDraft DRAFT_111_USD_CHANNEL1_UNTIL_01 =
      getPriceDraft(BigDecimal.valueOf(111), USD, null, null, null, byMonth(1), "channel1", null);

  public static final PriceDraft DRAFT_US_111_USD_CHANNEL1_UNTIL_01 =
      getPriceDraft(BigDecimal.valueOf(111), USD, US, null, null, byMonth(1), "channel1", null);

  public static final PriceDraft DRAFT_111_USD_CUST1_UNTIL_01 =
      getPriceDraft(BigDecimal.valueOf(111), USD, null, "cust1", null, byMonth(1), null, null);

  public static final PriceDraft DRAFT_US_111_USD_CUST1_UNTIL_01 =
      getPriceDraft(BigDecimal.valueOf(111), USD, US, "cust1", null, byMonth(1), null, null);

  public static final PriceDraft DRAFT_111_USD_CHANNEL1_CUST1_UNTIL_01 =
      getPriceDraft(
          BigDecimal.valueOf(111), USD, null, "cust1", null, byMonth(1), "channel1", null);

  public static final PriceDraft DRAFT_US_111_USD_CHANNEL1_CUST1_UNTIL_01 =
      getPriceDraft(BigDecimal.valueOf(111), USD, US, "cust1", null, byMonth(1), "channel1", null);

  public static final PriceDraft DRAFT_DE_111_EUR =
      getPriceDraft(BigDecimal.valueOf(111), EUR, DE, null, null, null, null, null);

  public static final PriceDraft DRAFT_111_EUR_01_02 =
      getPriceDraft(BigDecimal.valueOf(111), EUR, null, null, byMonth(1), byMonth(2), null, null);

  public static final PriceDraft DRAFT_111_EUR_CHANNEL1_01_02 =
      getPriceDraft(
          BigDecimal.valueOf(111), EUR, null, null, byMonth(1), byMonth(2), "channel1", null);

  public static final PriceDraft DRAFT_DE_111_EUR_01_02 =
      getPriceDraft(BigDecimal.valueOf(111), EUR, DE, null, byMonth(1), byMonth(2), null, null);

  public static final PriceDraft DRAFT_DE_111_EUR_02_03 =
      getPriceDraft(BigDecimal.valueOf(111), EUR, DE, null, byMonth(2), byMonth(3), null, null);

  public static final PriceDraft DRAFT_DE_111_EUR_03_04 =
      getPriceDraft(BigDecimal.valueOf(111), EUR, DE, null, byMonth(3), byMonth(4), null, null);

  public static final PriceDraft DRAFT_DE_222_EUR_03_04 =
      getPriceDraft(BigDecimal.valueOf(222), EUR, DE, null, byMonth(3), byMonth(4), null, null);

  public static final PriceDraft DRAFT_DE_111_USD =
      getPriceDraft(BigDecimal.valueOf(111), USD, DE, null, null, null, null, null);

  public static final CurrencyUnit GBP = Monetary.getCurrency("GBP");

  public static final PriceDraft DRAFT_UK_111_GBP =
      getPriceDraft(BigDecimal.valueOf(111), GBP, UK, null, null, null, null, null);

  public static final PriceDraft DRAFT_DE_222_EUR_CUST1 =
      getPriceDraft(BigDecimal.valueOf(222), EUR, DE, "cust1", null, null, null, null);

  public static final PriceDraft DRAFT_DE_222_EUR_CUST1_KEY =
      getPriceDraftWithKeys(BigDecimal.valueOf(222), EUR, DE, "cust1", null, null, null, null);

  public static final PriceDraft DRAFT_DE_333_USD_CUST1 =
      getPriceDraft(BigDecimal.valueOf(333), USD, DE, "cust1", null, null, null, null);

  public static final PriceDraft DRAFT_DE_333_USD_CUST1_KEY =
      getPriceDraftWithKeys(BigDecimal.valueOf(333), USD, DE, "cust1", null, null, null, null);

  public static final PriceDraft DRAFT_UK_111_GBP_01_02 =
      getPriceDraft(BigDecimal.valueOf(111), GBP, UK, null, byMonth(1), byMonth(2), null, null);

  public static final PriceDraft DRAFT_UK_111_GBP_02_03 =
      getPriceDraft(BigDecimal.valueOf(111), GBP, UK, null, byMonth(2), byMonth(3), null, null);

  public static final PriceDraft DRAFT_UK_333_GBP_03_05 =
      getPriceDraft(BigDecimal.valueOf(333), GBP, UK, null, byMonth(3), byMonth(5), null, null);

  public static final PriceDraft DRAFT_UK_333_GBP_01_04 =
      getPriceDraft(BigDecimal.valueOf(333), GBP, UK, null, byMonth(1), byMonth(4), null, null);

  public static final PriceDraft DRAFT_UK_444_GBP_04_06 =
      getPriceDraft(BigDecimal.valueOf(444), GBP, UK, null, byMonth(4), byMonth(6), null, null);

  public static final PriceDraft DRAFT_666_USD_CUST1_01_02 =
      getPriceDraft(
          BigDecimal.valueOf(666), USD, null, "cust1", byMonth(1), byMonth(2), null, null);

  public static final PriceDraft DRAFT_US_666_USD_CUST1_01_02 =
      getPriceDraft(BigDecimal.valueOf(666), USD, US, "cust1", byMonth(1), byMonth(2), null, null);

  public static final PriceDraft DRAFT_US_666_USD_CUST2_01_02 =
      getPriceDraft(BigDecimal.valueOf(666), USD, US, "cust2", byMonth(1), byMonth(2), null, null);

  public static final PriceDraft DRAFT_US_666_USD_CUST2_01_02_KEY =
      getPriceDraftWithKeys(
          BigDecimal.valueOf(666), USD, US, "cust2", byMonth(1), byMonth(2), null, null);

  public static final PriceDraft DRAFT_FR_888_EUR_01_03 =
      getPriceDraft(BigDecimal.valueOf(888), EUR, FR, null, byMonth(1), byMonth(3), null, null);

  public static final PriceDraft DRAFT_FR_777_EUR_01_04 =
      getPriceDraft(BigDecimal.valueOf(777), EUR, FR, null, byMonth(1), byMonth(4), null, null);

  public static final PriceDraft DRAFT_FR_999_EUR_03_06 =
      getPriceDraft(BigDecimal.valueOf(999), EUR, FR, null, byMonth(3), byMonth(6), null, null);

  public static final PriceDraft DRAFT_NE_777_EUR_01_04 =
      getPriceDraft(BigDecimal.valueOf(777), EUR, NE, null, byMonth(1), byMonth(4), null, null);

  public static final PriceDraft DRAFT_NE_777_EUR_05_07 =
      getPriceDraft(BigDecimal.valueOf(777), EUR, NE, null, byMonth(5), byMonth(7), null, null);

  public static final PriceDraft DRAFT_NE_123_EUR_01_04 =
      getPriceDraft(BigDecimal.valueOf(123), EUR, NE, null, byMonth(1), byMonth(4), null, null);

  public static final PriceDraft DRAFT_NE_321_EUR_04_06 =
      getPriceDraft(BigDecimal.valueOf(321), EUR, NE, null, byMonth(4), byMonth(6), null, null);

  public static final PriceDraft DRAFT_NE_777_CUST1_CHANNEL1_EUR_05_07 =
      getPriceDraft(
          BigDecimal.valueOf(777), EUR, NE, "cust1", byMonth(5), byMonth(7), "channel1", null);

  public static final PriceDraft DRAFT_777_CUST1_CHANNEL1_EUR_05_07 =
      getPriceDraft(
          BigDecimal.valueOf(777), EUR, null, "cust1", byMonth(5), byMonth(7), "channel1", null);

  public static final PriceDraft DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX =
      getPriceDraft(
          BigDecimal.valueOf(100),
          EUR,
          DE,
          null,
          byMonth(1),
          byMonth(2),
          "channel1",
          CustomFieldsDraftBuilder.of()
              .type(TypeResourceIdentifierBuilder.of().id("customType1").build())
              .fields(createCustomFieldsContainer("foo", JsonNodeFactory.instance.textNode("X")))
              .build());

  public static final PriceDraft DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX =
      getPriceDraft(
          BigDecimal.valueOf(100),
          EUR,
          DE,
          null,
          byMonth(1),
          byMonth(2),
          "channel2",
          CustomFieldsDraftBuilder.of()
              .type(TypeResourceIdentifierBuilder.of().id("customType1").build())
              .fields(createCustomFieldsContainer("foo", JsonNodeFactory.instance.textNode("X")))
              .build());

  public static final PriceDraft DRAFT_UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDX =
      getPriceDraft(
          BigDecimal.valueOf(22),
          GBP,
          UK,
          null,
          null,
          null,
          null,
          CustomFieldsDraftBuilder.of()
              .type(TypeResourceIdentifierBuilder.of().id("customType1").build())
              .fields(createCustomFieldsContainer("foo", JsonNodeFactory.instance.textNode("X")))
              .build());

  public static final PriceDraft DRAFT_UK_22_USD_CUSTOMTYPE1_CUSTOMFIELDX =
      getPriceDraft(
          BigDecimal.valueOf(22),
          USD,
          UK,
          null,
          null,
          null,
          null,
          CustomFieldsDraftBuilder.of()
              .type(TypeResourceIdentifierBuilder.of().id("customType1").build())
              .fields(createCustomFieldsContainer("foo", JsonNodeFactory.instance.textNode("X")))
              .build());

  public static final PriceDraft DRAFT_UK_666_GBP_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX =
      getPriceDraft(
          BigDecimal.valueOf(666),
          GBP,
          UK,
          null,
          null,
          null,
          "channel1",
          CustomFieldsDraftBuilder.of()
              .type(TypeResourceIdentifierBuilder.of().id("customType1").build())
              .fields(createCustomFieldsContainer("foo", JsonNodeFactory.instance.textNode("X")))
              .build());

  public static final PriceDraft DRAFT_DE_22_USD =
      getPriceDraft(BigDecimal.valueOf(22), USD, DE, null, null, null, null, null);

  public static final PriceDraft DRAFT_UK_999_GBP =
      getPriceDraft(BigDecimal.valueOf(999), GBP, UK, null, null, null, null, null);

  @Nonnull
  public static ZonedDateTime byMonth(final int month) {
    return ZonedDateTime.of(2018, month, 1, 0, 0, 0, 0, ZoneId.of("Z"));
  }

  @Nonnull
  public static FieldContainer createCustomFieldsContainer(
      @Nonnull final String fieldName, @Nonnull final JsonNode fieldValue) {
    return FieldContainerBuilder.of().addValue(fieldName, fieldValue).build();
  }

  @Nonnull
  public static PriceDraft getPriceDraft(
      @Nonnull final BigDecimal value,
      @Nonnull final CurrencyUnit currencyUnits,
      @Nullable final CountryCode countryCode,
      @Nullable final String customerGroupId,
      @Nullable final ZonedDateTime validFrom,
      @Nullable final ZonedDateTime validUntil,
      @Nullable final String channelId,
      @Nullable final CustomFieldsDraft customFieldsDraft) {

    final TypedMoney typedMoney =
        CentPrecisionMoneyBuilder.of()
            .centAmount(value.multiply(BigDecimal.valueOf(100)).longValue())
            .currencyCode(currencyUnits.getCurrencyCode())
            .fractionDigits(2)
            .build();
    return PriceDraftBuilder.of()
        .value(typedMoney)
        .country(Optional.ofNullable(countryCode).map(CountryCode::getAlpha2).orElse(null))
        .customerGroup(
            ofNullable(customerGroupId)
                .map(id -> CustomerGroupResourceIdentifier.builder().id(id).build())
                .orElse(null))
        .validFrom(validFrom)
        .validUntil(validUntil)
        .channel(
            ofNullable(channelId)
                .map(id -> ChannelResourceIdentifier.builder().id(id).build())
                .orElse(null))
        .custom(customFieldsDraft)
        .build();
  }

  @Nonnull
  public static PriceDraft getPriceDraftWithKeys(
      @Nonnull final BigDecimal value,
      @Nonnull final CurrencyUnit currencyUnits,
      @Nullable final CountryCode countryCode,
      @Nullable final String customerGroupKey,
      @Nullable final ZonedDateTime validFrom,
      @Nullable final ZonedDateTime validUntil,
      @Nullable final String channelKey,
      @Nullable final CustomFieldsDraft customFieldsDraft) {

    final TypedMoney typedMoney =
        CentPrecisionMoneyBuilder.of()
            .centAmount(value.multiply(BigDecimal.valueOf(100)).longValue())
            .currencyCode(currencyUnits.getCurrencyCode())
            .fractionDigits(2)
            .build();
    return PriceDraftBuilder.of()
        .value(typedMoney)
        .country(Optional.ofNullable(countryCode).map(CountryCode::getAlpha2).orElse(null))
        .customerGroup(
            ofNullable(customerGroupKey)
                .map(key -> CustomerGroupResourceIdentifier.builder().key(key).build())
                .orElse(null))
        .validFrom(validFrom)
        .validUntil(validUntil)
        .channel(
            ofNullable(channelKey)
                .map(key -> ChannelResourceIdentifier.builder().key(key).build())
                .orElse(null))
        .custom(customFieldsDraft)
        .build();
  }

  private PriceDraftFixtures() {}
}
