package com.commercetools.sync.products.utils.productvariantupdateactionutils.prices;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.MoneyImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.money.CurrencyUnit;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.neovisionaries.i18n.CountryCode.DE;
import static com.neovisionaries.i18n.CountryCode.FR;
import static com.neovisionaries.i18n.CountryCode.UK;
import static com.neovisionaries.i18n.CountryCode.US;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static io.sphere.sdk.models.DefaultCurrencyUnits.USD;

final class PriceDraftFixtures {
    static final PriceDraft DRAFT_US_111_USD = getPriceDraft(BigDecimal.valueOf(111), USD, US,
        null, null, null, null, null);

    static final PriceDraft DRAFT_DE_111_EUR = getPriceDraft(BigDecimal.valueOf(111), EUR, DE,
        null, null, null, null, null);

    static final PriceDraft DRAFT_DE_111_EUR_01_02 = getPriceDraft(BigDecimal.valueOf(111), EUR, DE, null,
        ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        null, null);

    static final PriceDraft DRAFT_DE_111_EUR_02_03 = getPriceDraft(BigDecimal.valueOf(111), EUR, DE, null,
        ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 3, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        null, null);

    static final PriceDraft DRAFT_DE_111_EUR_03_04 = getPriceDraft(BigDecimal.valueOf(111), EUR, DE, null,
        ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 3, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        null, null);

    static final PriceDraft DRAFT_DE_222_EUR_03_04 = getPriceDraft(BigDecimal.valueOf(222), EUR, DE, null,
        ZonedDateTime.of(2018, 3, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 4, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        null, null);

    static final PriceDraft DRAFT_DE_111_USD = getPriceDraft(BigDecimal.valueOf(111), USD, DE,
        null, null, null, null, null);

    static final CurrencyUnit GBP = MoneyImpl.createCurrencyByCode("GBP");

    static final PriceDraft DRAFT_UK_111_GBP = getPriceDraft(BigDecimal.valueOf(111), GBP,
        UK, null, null, null, null, null);

    static final PriceDraft DRAFT_DE_222_EUR_CUST1 = getPriceDraft(BigDecimal.valueOf(222), EUR,
        DE, "cust1", null, null, null, null);

    static final PriceDraft DRAFT_DE_333_USD_CUST1 = getPriceDraft(BigDecimal.valueOf(333), USD,
        DE, "cust1", null, null, null, null);

    static final PriceDraft DRAFT_UK_111_GBP_01_02 = getPriceDraft(BigDecimal.valueOf(111), GBP, UK, null,
        ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        null, null);

    static final PriceDraft DRAFT_UK_333_GBP_01_04 = getPriceDraft(BigDecimal.valueOf(333), GBP, UK, null,
        ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 4, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        null, null);

    static final PriceDraft DRAFT_UK_444_GBP_04_06 = getPriceDraft(BigDecimal.valueOf(444), GBP, UK, null,
        ZonedDateTime.of(2018, 4, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 6, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        null, null);

    static final PriceDraft DRAFT_US_666_USD_CUST1_01_02 = getPriceDraft(BigDecimal.valueOf(666), USD, US,
        "cust1",
        ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        null, null);

    static final PriceDraft DRAFT_US_666_USD_CUST2_01_02 = getPriceDraft(BigDecimal.valueOf(666), USD, US,
        "cust2",
        ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        null, null);

    static final PriceDraft DRAFT_FR_888_EUR_01_03 = getPriceDraft(BigDecimal.valueOf(888), EUR, FR, null,
        ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 3, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        null, null);

    static final PriceDraft DRAFT_FR_999_EUR_03_06 = getPriceDraft(BigDecimal.valueOf(999), EUR, FR, null,
        ZonedDateTime.of(2018, 3, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 6, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        null, null);

    static final PriceDraft DRAFT_NE_777_EUR_01_04 = getPriceDraft(BigDecimal.valueOf(777), EUR, FR, null,
        ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 4, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        null, null);

    static final PriceDraft DRAFT_NE_777_EUR_05_07 = getPriceDraft(BigDecimal.valueOf(777), EUR, FR, null,
        ZonedDateTime.of(2018, 5, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        ZonedDateTime.of(2018, 7, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        null, null);

    static final PriceDraft DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX =
        getPriceDraft(BigDecimal.valueOf(111), EUR, DE, null,
            ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
            ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
            "channel1", CustomFieldsDraft.ofTypeIdAndJson("customType1",
                createCustomFieldsJsonMap("X", JsonNodeFactory.instance.textNode("X"))));

    static final PriceDraft DRAFT_DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELDX =
        getPriceDraft(BigDecimal.valueOf(111), EUR, DE, null,
            ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
            ZonedDateTime.of(2018, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
            "channel2", CustomFieldsDraft.ofTypeIdAndJson("customType1",
                createCustomFieldsJsonMap("X", JsonNodeFactory.instance.textNode("X"))));

    static final PriceDraft DRAFT_UK_22_GBP_CUSTOMTYPE1_CUSTOMFIELDX =
        getPriceDraft(BigDecimal.valueOf(22), GBP, UK, null, null, null, null,
            CustomFieldsDraft.ofTypeIdAndJson("customType1",
                createCustomFieldsJsonMap("foo", JsonNodeFactory.instance.textNode("X"))));

    static final PriceDraft DRAFT_UK_22_USD_CUSTOMTYPE1_CUSTOMFIELDX =
        getPriceDraft(BigDecimal.valueOf(22), GBP, UK, null, null, null, null,
            CustomFieldsDraft.ofTypeIdAndJson("customType1",
                createCustomFieldsJsonMap("foo", JsonNodeFactory.instance.textNode("X"))));

    static final PriceDraft DRAFT_UK_666_GBP_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX =
        getPriceDraft(BigDecimal.valueOf(666), GBP, UK, null, null, null, "channel1",
            CustomFieldsDraft.ofTypeIdAndJson("customType1",
                createCustomFieldsJsonMap("foo", JsonNodeFactory.instance.textNode("X"))));

    static final PriceDraft DRAFT_DE_22_USD = getPriceDraft(BigDecimal.valueOf(22), USD, DE,
        null, null, null, null, null);

    static final PriceDraft DRAFT_UK_999_GBP = getPriceDraft(BigDecimal.valueOf(999), GBP,
        UK, null, null, null, null, null);


    @Nonnull
    private static Map<String, JsonNode> createCustomFieldsJsonMap(@Nonnull final String fieldName,
                                                                   @Nonnull final JsonNode fieldValue) {
        final Map<String, JsonNode> customFieldsJsons = new HashMap<>();
        customFieldsJsons.put(fieldName, fieldValue);
        return customFieldsJsons;
    }


    @Nonnull
    private static PriceDraft getPriceDraft(@Nonnull final BigDecimal value,
                                            @Nonnull final CurrencyUnit currencyUnits,
                                            @Nonnull final CountryCode countryCode,
                                            @Nullable final String customerGroupId,
                                            @Nullable final ZonedDateTime validFrom,
                                            @Nullable final ZonedDateTime validUntil,
                                            @Nullable final String channelId,
                                            @Nullable final CustomFieldsDraft customFieldsDraft) {

        return PriceDraftBuilder.of(Price.of(value, currencyUnits))
                                .country(countryCode)
                                .customerGroup(CustomerGroup.referenceOfId(customerGroupId))
                                .validFrom(validFrom)
                                .validUntil(validUntil)
                                .channel(Channel.referenceOfId(channelId))
                                .custom(customFieldsDraft)
                                .build();
    }

    private PriceDraftFixtures() {
    }
}
