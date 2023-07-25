package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.products.utils.ProductVariantPriceUpdateActionUtils.buildActions;
import static com.commercetools.sync.sdk2.products.utils.ProductVariantPriceUpdateActionUtils.buildChangePriceUpdateAction;
import static com.commercetools.sync.sdk2.products.utils.ProductVariantPriceUpdateActionUtils.buildCustomUpdateActions;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceFixtures.DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.CentPrecisionMoneyBuilder;
import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.common.PriceBuilder;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.common.PriceTier;
import com.commercetools.api.models.common.PriceTierBuilder;
import com.commercetools.api.models.common.PriceTierDraft;
import com.commercetools.api.models.common.PriceTierDraftBuilder;
import com.commercetools.api.models.common.TypedMoney;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductChangePriceAction;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductSetProductPriceCustomFieldAction;
import com.commercetools.api.models.product.ProductSetProductPriceCustomTypeAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainer;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProductVariantPriceUpdateActionUtilsTest {
  private static final ProductSyncOptions SYNC_OPTIONS =
      ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
  final Product mainProduct = mock(Product.class);
  final ProductDraft mainProductDraft = mock(ProductDraft.class);

  private static final TypedMoney EUR_10 =
      CentPrecisionMoneyBuilder.of()
          .centAmount(BigDecimal.valueOf(1000).longValue())
          .currencyCode("EUR")
          .fractionDigits(2)
          .build();
  private static final TypedMoney EUR_20 =
      CentPrecisionMoneyBuilder.of()
          .centAmount(BigDecimal.valueOf(2000).longValue())
          .currencyCode("EUR")
          .fractionDigits(2)
          .build();
  private static final PriceTier TIER_1_EUR_10 =
      PriceTierBuilder.of().value(EUR_10).minimumQuantity(1L).build();
  private static final PriceTierDraft TIER_1_EUR_10_DRAFT =
      PriceTierDraftBuilder.of().value(EUR_10).minimumQuantity(1L).build();
  private static final PriceTier TIER_2_EUR_10 =
      PriceTierBuilder.of().value(EUR_10).minimumQuantity(2L).build();
  private static final PriceTierDraft TIER_2_EUR_10_DRAFT =
      PriceTierDraftBuilder.of().value(EUR_10).minimumQuantity(2L).build();
  private static final PriceTier TIER_1_EUR_20 =
      PriceTierBuilder.of().value(EUR_20).minimumQuantity(1L).build();
  private static final PriceTierDraft TIER_1_EUR_20_DRAFT =
      PriceTierDraftBuilder.of().value(EUR_20).minimumQuantity(1L).build();

  private static final Price PRICE_EUR_10_TIER_1_EUR_10 =
      PriceBuilder.of()
          .value(EUR_10)
          .id(UUID.randomUUID().toString())
          .tiers(singletonList(TIER_1_EUR_10))
          .build();

  private static final PriceDraft DRAFT_EUR_10_TIER_1_EUR_10 =
      PriceDraftBuilder.of().value(EUR_10).tiers(singletonList(TIER_1_EUR_10_DRAFT)).build();

  private static final PriceDraft DRAFT_EUR_20_TIER_1_EUR_10 =
      PriceDraftBuilder.of().value(EUR_20).tiers(singletonList(TIER_1_EUR_10_DRAFT)).build();

  private static final PriceDraft DRAFT_EUR_10_TIER_1_EUR_20 =
      PriceDraftBuilder.of().value(EUR_10).tiers(singletonList(TIER_1_EUR_20_DRAFT)).build();

  private static final PriceDraft DRAFT_EUR_10_TIER_2_EUR_10 =
      PriceDraftBuilder.of().value(EUR_10).tiers(singletonList(TIER_2_EUR_10_DRAFT)).build();

  private static final PriceDraft DRAFT_EUR_10_MULTIPLE_TIERS =
      PriceDraftBuilder.of()
          .value(EUR_10)
          .tiers(asList(TIER_2_EUR_10_DRAFT, TIER_1_EUR_10_DRAFT))
          .build();

  private static final PriceDraft DRAFT_NULL_VALUE = PriceDraft.of();

  private static final Price PRICE_EUR_10_NULL_TIERS =
      PriceBuilder.of()
          .value(EUR_10)
          .id(UUID.randomUUID().toString())
          .tiers((PriceTier) null)
          .build();

  private static final PriceDraft DRAFT_EUR_10_NULL_TIERS =
      PriceDraftBuilder.of().value(EUR_10).tiers((PriceTierDraft) null).build();

  private static final Price PRICE_EUR_10_EMPTY_TIERS =
      PriceBuilder.of().value(EUR_10).id(UUID.randomUUID().toString()).tiers(emptyList()).build();

  private static final PriceDraft DRAFT_EUR_10_EMPTY_TIERS =
      PriceDraftBuilder.of().value(EUR_10).tiers(emptyList()).build();

  @ParameterizedTest(name = "[#buildActions]: {0}")
  @MethodSource("buildActionsTestCases")
  void buildActionsTest(
      @Nonnull final String testCaseName,
      @Nonnull final Price oldPrice,
      @Nonnull final PriceDraft newPrice,
      @Nonnull final List<ProductUpdateAction> expectedResult,
      @Nonnull final List<String> expectedWarnings) {
    // preparation
    final List<String> warnings = new ArrayList<>();
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .warningCallback(
                (exception, oldResource, newResource) -> warnings.add(exception.getMessage()))
            .build();

    // test
    final List<ProductUpdateAction> result =
        buildActions(mainProductDraft, 0L, oldPrice, newPrice, syncOptions);

    // assertion
    assertEquals(expectedResult, result);
    assertEquals(expectedWarnings, warnings);
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
    final String case9 = "different values (with a null new value)";

    return Stream.of(
        Arguments.of(
            case1, PRICE_EUR_10_NULL_TIERS, DRAFT_EUR_10_NULL_TIERS, emptyList(), emptyList()),
        Arguments.of(
            case2, PRICE_EUR_10_EMPTY_TIERS, DRAFT_EUR_10_EMPTY_TIERS, emptyList(), emptyList()),
        Arguments.of(
            case3,
            PRICE_EUR_10_TIER_1_EUR_10,
            DRAFT_EUR_10_TIER_1_EUR_10,
            emptyList(),
            emptyList()),
        Arguments.of(
            case4,
            PRICE_EUR_10_TIER_1_EUR_10,
            DRAFT_EUR_20_TIER_1_EUR_10,
            singletonList(
                ProductChangePriceAction.builder()
                    .priceId(PRICE_EUR_10_TIER_1_EUR_10.getId())
                    .price(DRAFT_EUR_20_TIER_1_EUR_10)
                    .staged(true)
                    .build()),
            emptyList()),
        Arguments.of(
            case5,
            PRICE_EUR_10_TIER_1_EUR_10,
            DRAFT_EUR_10_TIER_1_EUR_20,
            singletonList(
                ProductChangePriceAction.builder()
                    .price(DRAFT_EUR_10_TIER_1_EUR_20)
                    .priceId(PRICE_EUR_10_TIER_1_EUR_10.getId())
                    .staged(true)
                    .build()),
            emptyList()),
        Arguments.of(
            case6,
            PRICE_EUR_10_TIER_1_EUR_10,
            DRAFT_EUR_10_TIER_2_EUR_10,
            singletonList(
                ProductChangePriceAction.builder()
                    .price(DRAFT_EUR_10_TIER_2_EUR_10)
                    .priceId(PRICE_EUR_10_TIER_1_EUR_10.getId())
                    .staged(true)
                    .build()),
            emptyList()),
        Arguments.of(
            case7,
            PRICE_EUR_10_TIER_1_EUR_10,
            DRAFT_EUR_10_MULTIPLE_TIERS,
            singletonList(
                ProductChangePriceAction.builder()
                    .price(DRAFT_EUR_10_MULTIPLE_TIERS)
                    .priceId(PRICE_EUR_10_TIER_1_EUR_10.getId())
                    .staged(true)
                    .build()),
            emptyList()),
        Arguments.of(
            case8,
            DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY,
            DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX,
            asList(
                ProductChangePriceAction.builder()
                    .price(DRAFT_DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDX)
                    .priceId(DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY.getId())
                    .staged(true)
                    .build(),
                ProductSetProductPriceCustomFieldAction.builder()
                    .name("foo")
                    .priceId(DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELDY.getId())
                    .value(JsonNodeFactory.instance.textNode("X"))
                    .staged(true)
                    .build()),
            emptyList()),
        Arguments.of(
            case9,
            PRICE_EUR_10_TIER_1_EUR_10,
            DRAFT_NULL_VALUE,
            emptyList(),
            singletonList(
                format(
                    "Cannot unset 'value' field of price with id '%s'.",
                    PRICE_EUR_10_TIER_1_EUR_10.getId()))));
  }

  @ParameterizedTest(name = "[#buildChangePrice]: {0}")
  @MethodSource("buildChangePriceTestCases")
  void buildChangePriceUpdateActionTest(
      @Nonnull final String testCaseName,
      @Nonnull final Price oldPrice,
      @Nonnull final PriceDraft newPrice,
      @Nullable final ProductUpdateAction expectedResult,
      @Nonnull final List<String> expectedWarnings) {
    // preparation
    final List<String> warnings = new ArrayList<>();
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .warningCallback(
                (exception, oldResource, newResource) -> warnings.add(exception.getMessage()))
            .build();

    // test
    final ProductUpdateAction result =
        buildChangePriceUpdateAction(oldPrice, newPrice, syncOptions).orElse(null);

    // assertion
    assertEquals(expectedResult, result);
    assertEquals(expectedWarnings, warnings);
  }

  private static Stream<Arguments> buildChangePriceTestCases() {
    final String case1 = "identical values and null tiers";
    final String case2 = "identical values and empty tiers";
    final String case3 = "identical values and identical tiers";
    final String case4 = "different values and identical tiers";
    final String case5 = "identical values and different tiers [different in value]";
    final String case6 = "identical values and different tiers [different in minimumQuantity]";
    final String case7 = "identical values and different tiers [different in number of tiers]";
    final String case8 = "different values (with a null new value)";

    return Stream.of(
        Arguments.of(case1, PRICE_EUR_10_NULL_TIERS, DRAFT_EUR_10_NULL_TIERS, null, emptyList()),
        Arguments.of(case2, PRICE_EUR_10_EMPTY_TIERS, DRAFT_EUR_10_EMPTY_TIERS, null, emptyList()),
        Arguments.of(
            case3, PRICE_EUR_10_TIER_1_EUR_10, DRAFT_EUR_10_TIER_1_EUR_10, null, emptyList()),
        Arguments.of(
            case4,
            PRICE_EUR_10_TIER_1_EUR_10,
            DRAFT_EUR_20_TIER_1_EUR_10,
            ProductChangePriceAction.builder()
                .priceId(PRICE_EUR_10_TIER_1_EUR_10.getId())
                .price(DRAFT_EUR_20_TIER_1_EUR_10)
                .staged(true)
                .build(),
            emptyList()),
        Arguments.of(
            case5,
            PRICE_EUR_10_TIER_1_EUR_10,
            DRAFT_EUR_10_TIER_1_EUR_20,
            ProductChangePriceAction.builder()
                .priceId(PRICE_EUR_10_TIER_1_EUR_10.getId())
                .price(DRAFT_EUR_10_TIER_1_EUR_20)
                .staged(true)
                .build(),
            emptyList()),
        Arguments.of(
            case6,
            PRICE_EUR_10_TIER_1_EUR_10,
            DRAFT_EUR_10_TIER_2_EUR_10,
            ProductChangePriceAction.builder()
                .priceId(PRICE_EUR_10_TIER_1_EUR_10.getId())
                .price(DRAFT_EUR_10_TIER_2_EUR_10)
                .staged(true)
                .build(),
            emptyList()),
        Arguments.of(
            case7,
            PRICE_EUR_10_TIER_1_EUR_10,
            DRAFT_EUR_10_MULTIPLE_TIERS,
            ProductChangePriceAction.builder()
                .priceId(PRICE_EUR_10_TIER_1_EUR_10.getId())
                .price(DRAFT_EUR_10_MULTIPLE_TIERS)
                .staged(true)
                .build(),
            emptyList()),
        Arguments.of(
            case8,
            PRICE_EUR_10_TIER_1_EUR_10,
            DRAFT_NULL_VALUE,
            null,
            singletonList(
                format(
                    "Cannot unset 'value' field of price with id '%s'.",
                    PRICE_EUR_10_TIER_1_EUR_10.getId()))));
  }

  @Test
  void buildCustomUpdateActions_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
    oldCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));
    final FieldContainer oldCustomFieldContainer =
        FieldContainerBuilder.of().values(oldCustomFieldsMap).build();
    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReference.builder().id("1").build());
    when(oldCustomFields.getFields()).thenReturn(oldCustomFieldContainer);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(TypeResourceIdentifierBuilder.of().id("1").build())
            .fields(oldCustomFieldContainer)
            .build();

    final Price oldPrice =
        PriceBuilder.of()
            .value(EUR_10)
            .id(UUID.randomUUID().toString())
            .tiers(singletonList(TIER_1_EUR_10))
            .custom(oldCustomFields)
            .build();

    final PriceDraft newPrice =
        PriceDraftBuilder.of()
            .value(EUR_10)
            .tiers(singletonList(TIER_1_EUR_10_DRAFT))
            .custom(newCustomFieldsDraft)
            .build();

    final List<ProductUpdateAction> updateActions =
        buildCustomUpdateActions(mainProductDraft, 1L, oldPrice, newPrice, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildCustomUpdateActions_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
    oldCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
    newCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));

    final FieldContainer oldCustomFieldContainer =
        FieldContainerBuilder.of().values(oldCustomFieldsMap).build();
    final FieldContainer newCustomFieldContainer =
        FieldContainerBuilder.of().values(newCustomFieldsMap).build();

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReference.builder().id("1").build());
    when(oldCustomFields.getFields()).thenReturn(oldCustomFieldContainer);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraft.builder()
            .type(TypeResourceIdentifierBuilder.of().id("1").build())
            .fields(newCustomFieldContainer)
            .build();

    final Price oldPrice =
        PriceBuilder.of()
            .value(EUR_10)
            .id(UUID.randomUUID().toString())
            .tiers(singletonList(TIER_1_EUR_10))
            .custom(oldCustomFields)
            .build();

    final PriceDraft newPrice =
        PriceDraftBuilder.of()
            .value(EUR_10)
            .tiers(singletonList(TIER_1_EUR_10_DRAFT))
            .custom(newCustomFieldsDraft)
            .build();

    final List<ProductUpdateAction> updateActions =
        buildCustomUpdateActions(mainProductDraft, 1L, oldPrice, newPrice, SYNC_OPTIONS);

    assertThat(updateActions).hasSize(2);
  }

  @Test
  void buildCustomUpdateActions_WithNullOldStagedValues_ShouldBuildUpdateAction() {
    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
    newCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));

    final FieldContainer newCustomFieldContainer =
        FieldContainerBuilder.of().values(newCustomFieldsMap).build();

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(TypeResourceIdentifierBuilder.of().id("1").build())
            .fields(newCustomFieldContainer)
            .build();

    final Price oldPrice =
        PriceBuilder.of()
            .value(EUR_10)
            .id(UUID.randomUUID().toString())
            .tiers(singletonList(TIER_1_EUR_10))
            .custom((CustomFields) null)
            .build();

    final PriceDraft newPrice =
        PriceDraftBuilder.of()
            .value(EUR_10)
            .tiers(singletonList(TIER_1_EUR_10_DRAFT))
            .custom(newCustomFieldsDraft)
            .build();

    final List<ProductUpdateAction> updateActions =
        buildCustomUpdateActions(mainProductDraft, 1L, oldPrice, newPrice, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductSetProductPriceCustomTypeAction.builder()
                .type(TypeResourceIdentifierBuilder.of().id("1").build())
                .fields(newCustomFieldContainer)
                .priceId(oldPrice.getId())
                .staged(true)
                .build());
  }

  @Test
  void
      buildCustomUpdateActions_WithBadCustomFieldData_ShouldNotBuildUpdateActionAndTriggerErrorCallback() {
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
    oldCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
    newCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));

    final FieldContainer oldCustomFieldContainer =
        FieldContainerBuilder.of().values(oldCustomFieldsMap).build();
    final FieldContainer newCustomFieldContainer =
        FieldContainerBuilder.of().values(newCustomFieldsMap).build();

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id("").build());
    when(oldCustomFields.getFields()).thenReturn(oldCustomFieldContainer);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(TypeResourceIdentifierBuilder.of().id("").build())
            .fields(newCustomFieldContainer)
            .build();

    final Price oldPrice =
        PriceBuilder.of()
            .value(EUR_10)
            .id(UUID.randomUUID().toString())
            .tiers(singletonList(TIER_1_EUR_10))
            .custom(oldCustomFields)
            .build();

    final PriceDraft newPrice =
        PriceDraftBuilder.of()
            .value(EUR_10)
            .tiers(singletonList(TIER_1_EUR_10_DRAFT))
            .custom(newCustomFieldsDraft)
            .build();

    final List<String> errors = new ArrayList<>();

    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errors.add(exception.getMessage()))
            .build();

    final List<ProductUpdateAction> updateActions =
        buildCustomUpdateActions(mainProductDraft, 1L, oldPrice, newPrice, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(errors).hasSize(1);
    assertThat(errors.get(0))
        .isEqualTo(
            format(
                "Failed to build custom fields update actions on the product-price with id '%s'."
                    + " Reason: Custom type ids are not set for both the old and new product-price.",
                oldPrice.getId()));
  }
}
