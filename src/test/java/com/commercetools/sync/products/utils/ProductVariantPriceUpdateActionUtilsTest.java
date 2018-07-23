package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceBuilder;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.products.PriceTier;
import io.sphere.sdk.products.PriceTierBuilder;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.ChangePrice;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomField;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomType;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.commercetools.sync.products.utils.ProductVariantPriceUpdateActionUtils.buildActions;
import static com.commercetools.sync.products.utils.ProductVariantPriceUpdateActionUtils.buildChangePriceUpdateAction;
import static com.commercetools.sync.products.utils.ProductVariantPriceUpdateActionUtils.buildCustomUpdateActions;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceFixtures.DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductVariantPriceUpdateActionUtilsTest {
    private static final ProductSyncOptions SYNC_OPTIONS = ProductSyncOptionsBuilder
        .of(mock(SphereClient.class)).build();

    private static final MonetaryAmount EUR_10 = MoneyImpl.of(BigDecimal.TEN, EUR);
    private static final MonetaryAmount EUR_20 = MoneyImpl.of(BigDecimal.valueOf(20), EUR);
    private static final PriceTier TIER_1_EUR_10 = PriceTierBuilder.of(1, EUR_10).build();
    private static final PriceTier TIER_2_EUR_10 = PriceTierBuilder.of(2, EUR_10).build();
    private static final PriceTier TIER_1_EUR_20 = PriceTierBuilder.of(1, EUR_20).build();

    private static final Price PRICE_EUR_10_TIER_1_EUR_10 = PriceBuilder.of(EUR_10)
                                                                        .id(UUID.randomUUID().toString())
                                                                        .tiers(singletonList(TIER_1_EUR_10))
                                                                        .build();

    private static final PriceDraft DRAFT_EUR_10_TIER_1_EUR_10 = PriceDraftBuilder.of(EUR_10)
                                                                                  .tiers(singletonList(TIER_1_EUR_10))
                                                                                  .build();

    private static final PriceDraft DRAFT_EUR_20_TIER_1_EUR_10 = PriceDraftBuilder.of(EUR_20)
                                                                                  .tiers(singletonList(TIER_1_EUR_10))
                                                                                  .build();

    private static final PriceDraft DRAFT_EUR_10_TIER_1_EUR_20 = PriceDraftBuilder.of(EUR_10)
                                                                                  .tiers(singletonList(TIER_1_EUR_20))
                                                                                  .build();

    private static final PriceDraft DRAFT_EUR_10_TIER_2_EUR_10 = PriceDraftBuilder.of(EUR_10)
                                                                                  .tiers(singletonList(TIER_2_EUR_10))
                                                                                  .build();

    private static final PriceDraft DRAFT_EUR_10_MULTIPLE_TIERS = PriceDraftBuilder.of(EUR_10)
                                                                                   .tiers(asList(TIER_2_EUR_10,
                                                                                       TIER_1_EUR_10))
                                                                                   .build();

    private static final Price PRICE_EUR_10_NULL_TIERS = PriceBuilder.of(EUR_10)
                                                                     .id(UUID.randomUUID().toString())
                                                                     .tiers(null)
                                                                     .build();

    private static final PriceDraft DRAFT_EUR_10_NULL_TIERS = PriceDraftBuilder.of(EUR_10)
                                                                               .tiers(null)
                                                                               .build();

    private static final Price PRICE_EUR_10_EMPTY_TIERS = PriceBuilder.of(EUR_10)
                                                                      .id(UUID.randomUUID().toString())
                                                                      .tiers(emptyList())
                                                                      .build();

    private static final PriceDraft DRAFT_EUR_10_EMPTY_TIERS = PriceDraftBuilder.of(EUR_10)
                                                                                .tiers(emptyList())
                                                                                .build();


    @ParameterizedTest(name = "[#buildActions]: {0}")
    @MethodSource("buildActionsTestCases")
    void buildActionsTest(@Nonnull final String testCaseName,
                          @Nonnull final Price oldPrice,
                          @Nonnull final PriceDraft newPrice,
                          @Nonnull final List<UpdateAction<Product>> expectedResult) {

        final List<UpdateAction<Product>> result = buildActions(0, oldPrice, newPrice, SYNC_OPTIONS);

        assertEquals(expectedResult, result);
    }

    private static Stream<Arguments> buildActionsTestCases() {
        final String case1 = "identical values and null tiers";
        final String case2 = "identical values and empty tiers";
        final String case3 = "identical values and identical tiers";
        final String case4 = "different values and identical tiers";
        final String case5 = "identical values and different tiers [different in value]";
        final String case6 = "identical values and different tiers [different in minimumQuantity]";
        final String case7 = "identical values and different tiers [different in number of tiers]";
        final String case8 = "different values and different custom fields";

        return Stream.of(
            Arguments.of(case1, PRICE_EUR_10_NULL_TIERS, DRAFT_EUR_10_NULL_TIERS, emptyList()),
            Arguments.of(case2, PRICE_EUR_10_EMPTY_TIERS, DRAFT_EUR_10_EMPTY_TIERS, emptyList()),
            Arguments.of(case3, PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_10_TIER_1_EUR_10, emptyList()),
            Arguments.of(case4, PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_20_TIER_1_EUR_10,
                singletonList(ChangePrice.of(PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_20_TIER_1_EUR_10, true))),
            Arguments.of(case5, PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_10_TIER_1_EUR_20,
                singletonList(ChangePrice.of(PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_10_TIER_1_EUR_20, true))),
            Arguments.of(case6, PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_10_TIER_2_EUR_10,
                singletonList(ChangePrice.of(PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_10_TIER_2_EUR_10, true))),
            Arguments.of(case7, PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_10_MULTIPLE_TIERS,
                singletonList(ChangePrice.of(PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_10_MULTIPLE_TIERS, true))),
            Arguments.of(case8, DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY,
                DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX,
                asList(ChangePrice.of(DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY,
                    DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX, true),
                    SetProductPriceCustomField.ofJson("foo",
                        DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX.getCustom().getFields().get("foo"),
                        DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY.getId(), true)))
        );
    }

    @ParameterizedTest(name = "[#buildChangePrice]: {0}")
    @MethodSource("buildChangePriceTestCases")
    void buildChangePriceUpdateActionTest(@Nonnull final String testCaseName,
                                          @Nonnull final Price oldPrice,
                                          @Nonnull final PriceDraft newPrice,
                                          @Nullable final UpdateAction<Product> expectedResult) {

        final ChangePrice result = buildChangePriceUpdateAction(oldPrice, newPrice).orElse(null);

        assertEquals(expectedResult, result);
    }

    private static Stream<Arguments> buildChangePriceTestCases() {
        final String case1 = "identical values and null tiers";
        final String case2 = "identical values and empty tiers";
        final String case3 = "identical values and identical tiers";
        final String case4 = "different values and identical tiers";
        final String case5 = "identical values and different tiers [different in value]";
        final String case6 = "identical values and different tiers [different in minimumQuantity]";
        final String case7 = "identical values and different tiers [different in number of tiers]";


        return Stream.of(
            Arguments.of(case1, PRICE_EUR_10_NULL_TIERS, DRAFT_EUR_10_NULL_TIERS, null),
            Arguments.of(case2, PRICE_EUR_10_EMPTY_TIERS, DRAFT_EUR_10_EMPTY_TIERS, null),
            Arguments.of(case3, PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_10_TIER_1_EUR_10, null),
            Arguments.of(case4, PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_20_TIER_1_EUR_10,
                ChangePrice.of(PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_20_TIER_1_EUR_10, true)),
            Arguments.of(case5, PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_10_TIER_1_EUR_20,
                ChangePrice.of(PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_10_TIER_1_EUR_20, true)),
            Arguments.of(case6, PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_10_TIER_2_EUR_10,
                ChangePrice.of(PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_10_TIER_2_EUR_10, true)),
            Arguments.of(case7, PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_10_MULTIPLE_TIERS,
                ChangePrice.of(PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_10_MULTIPLE_TIERS, true))
        );
    }



    @Test
    void buildCustomUpdateActions_WithSameStagedValues_ShouldNotBuildUpdateAction() {
        final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
        oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFieldsMap.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));


        final CustomFields oldCustomFields = mock(CustomFields.class);
        when(oldCustomFields.getType()).thenReturn(Type.referenceOfId("1"));
        when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

        final CustomFieldsDraft newCustomFieldsDraft =
            CustomFieldsDraft.ofTypeIdAndJson("1", oldCustomFieldsMap);

        final Price oldPrice = PriceBuilder.of(EUR_10)
                                        .id(UUID.randomUUID().toString())
                                        .tiers(singletonList(TIER_1_EUR_10))
                                        .custom(oldCustomFields)
                                        .build();

        final PriceDraft newPrice = PriceDraftBuilder.of(EUR_10)
                                                     .tiers(singletonList(TIER_1_EUR_10))
                                                     .custom(newCustomFieldsDraft)
                                                     .build();

        final List<UpdateAction<Product>> updateActions =
            buildCustomUpdateActions(1, oldPrice, newPrice, SYNC_OPTIONS);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildCustomUpdateActions_WithDifferentStagedValues_ShouldBuildUpdateAction() {
        final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
        oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFieldsMap.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
        newCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        newCustomFieldsMap.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));


        final CustomFields oldCustomFields = mock(CustomFields.class);
        when(oldCustomFields.getType()).thenReturn(Type.referenceOfId("1"));
        when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

        final CustomFieldsDraft newCustomFieldsDraft =
            CustomFieldsDraft.ofTypeIdAndJson("1", newCustomFieldsMap);

        final Price oldPrice = PriceBuilder.of(EUR_10)
                                           .id(UUID.randomUUID().toString())
                                           .tiers(singletonList(TIER_1_EUR_10))
                                           .custom(oldCustomFields)
                                           .build();

        final PriceDraft newPrice = PriceDraftBuilder.of(EUR_10)
                                                     .tiers(singletonList(TIER_1_EUR_10))
                                                     .custom(newCustomFieldsDraft)
                                                     .build();

        final List<UpdateAction<Product>> updateActions =
            buildCustomUpdateActions(1, oldPrice, newPrice, SYNC_OPTIONS);

        assertThat(updateActions).hasSize(2);
    }

    @Test
    void buildCustomUpdateActions_WithNullOldStagedValues_ShouldBuildUpdateAction() {
        final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
        newCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        newCustomFieldsMap.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));


        final CustomFieldsDraft newCustomFieldsDraft =
            CustomFieldsDraft.ofTypeIdAndJson("1", newCustomFieldsMap);

        final Price oldPrice = PriceBuilder.of(EUR_10)
                                           .id(UUID.randomUUID().toString())
                                           .tiers(singletonList(TIER_1_EUR_10))
                                           .custom(null)
                                           .build();

        final PriceDraft newPrice = PriceDraftBuilder.of(EUR_10)
                                                     .tiers(singletonList(TIER_1_EUR_10))
                                                     .custom(newCustomFieldsDraft)
                                                     .build();

        final List<UpdateAction<Product>> updateActions =
            buildCustomUpdateActions(1, oldPrice, newPrice, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            SetProductPriceCustomType.ofTypeIdAndJson("1", newCustomFieldsMap, oldPrice.getId(), true)
        );
    }

    @Test
    void buildCustomUpdateActions_WithBadCustomFieldData_ShouldNotBuildUpdateActionAndTriggerErrorCallback() {
        final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
        oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFieldsMap.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
        newCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        newCustomFieldsMap.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));


        final CustomFields oldCustomFields = mock(CustomFields.class);
        when(oldCustomFields.getType()).thenReturn(Type.referenceOfId(""));
        when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

        final CustomFieldsDraft newCustomFieldsDraft =
            CustomFieldsDraft.ofTypeIdAndJson("", newCustomFieldsMap);

        final Price oldPrice = PriceBuilder.of(EUR_10)
                                           .id(UUID.randomUUID().toString())
                                           .tiers(singletonList(TIER_1_EUR_10))
                                           .custom(oldCustomFields)
                                           .build();

        final PriceDraft newPrice = PriceDraftBuilder.of(EUR_10)
                                                     .tiers(singletonList(TIER_1_EUR_10))
                                                     .custom(newCustomFieldsDraft)
                                                     .build();


        final List<String> errors = new ArrayList<>();

        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                        .errorCallback((errorMessage, throwable) ->
                                                                            errors.add(errorMessage))
                                                                        .build();

        final List<UpdateAction<Product>> updateActions =
            buildCustomUpdateActions(1, oldPrice, newPrice, syncOptions);


        assertThat(updateActions).isEmpty();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0))
            .isEqualTo(format("Failed to build custom fields update actions on the product-price with id '%s'."
                + " Reason: Custom type ids are not set for both the old and new product-price.", oldPrice.getId()));
    }
}
