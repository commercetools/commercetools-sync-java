package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.api.models.common.DefaultCurrencyUnits.EUR;
import static com.commercetools.api.models.common.DefaultCurrencyUnits.USD;
import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.commons.utils.CustomerGroupITUtils.*;
import static com.commercetools.sync.integration.commons.utils.ITUtils.EMPTY_SET_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.LOCALISED_STRING_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.NON_EMPTY_SEY_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.NULL_NODE_SET_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.NULL_SET_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createCustomFieldsJsonMap;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createVariantDraft;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.*;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.*;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.sdk2.commons.utils.CollectionUtils.collectionToMap;
import static com.commercetools.sync.sdk2.commons.utils.CustomValueConverter.convertCustomValueObjDataToJsonNode;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_EUR;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_EUR_01_02;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_EUR_02_03;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_EUR_03_04;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_USD;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_222_EUR_CUST1;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_222_EUR_CUST1_KEY;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_22_USD;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_333_USD_CUST1;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_333_USD_CUST1_KEY;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_FR_777_EUR_01_04;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_FR_888_EUR_01_03;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_FR_999_EUR_03_06;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_NE_123_EUR_01_04;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_NE_321_EUR_04_06;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_NE_777_EUR_01_04;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_NE_777_EUR_05_07;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_111_GBP;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_111_GBP_01_02;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_111_GBP_02_03;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_333_GBP_03_05;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_999_GBP;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_US_111_USD;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_US_666_USD_CUST2_01_02;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_US_666_USD_CUST2_01_02_KEY;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.byMonth;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.getPriceDraft;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.getPriceDraftWithKeys;
import static com.neovisionaries.i18n.CountryCode.DE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelDraftBuilder;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.api.models.product.*;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.type.*;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.QuadConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriFunction;
import com.commercetools.sync.sdk2.products.ProductSync;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.PriceCompositeId;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.*;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncWithPricesIT {
  private static ProductType productType;
  private static Type customType1;
  private static Channel channel1;
  private ProductSync productSync;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<ProductUpdateAction> updateActionsFromSync;

  /**
   * Delete all product related test data from the target project. Then creates price custom types,
   * customer groups, channels and a product type for the target CTP project.
   */
  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);

    customType1 =
        ensurePricesCustomType("customType1", Locale.ENGLISH, "customType1", CTP_TARGET_CLIENT);

    channel1 = ensureChannel(ChannelDraftBuilder.of().key("channel1").build(), CTP_TARGET_CLIENT);

    productType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
  }

  /** Deletes Products from the target CTP project. */
  @BeforeEach
  void setupTest() {
    clearSyncTestCollections();
    deleteAllProducts(CTP_TARGET_CLIENT);
    productSync = new ProductSync(buildSyncOptions());
  }

  private void clearSyncTestCollections() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
    updateActionsFromSync = new ArrayList<>();
  }

  private ProductSyncOptions buildSyncOptions() {
    final QuadConsumer<
            SyncException,
            Optional<ProductDraft>,
            Optional<ProductProjection>,
            List<ProductUpdateAction>>
        errorCallBack =
            (exception, newResource, oldResource, updateActions) -> {
              errorCallBackMessages.add(exception.getMessage());
              errorCallBackExceptions.add(exception.getCause());
            };
    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
        warningCallBack =
            (exception, newResource, oldResource) ->
                warningCallBackMessages.add(exception.getMessage());

    final TriFunction<
            List<ProductUpdateAction>, ProductDraft, ProductProjection, List<ProductUpdateAction>>
        actionsCallBack =
            (updateActions, newDraft, oldProduct) -> {
              updateActionsFromSync.addAll(updateActions);
              return updateActions;
            };

    return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(errorCallBack)
        .warningCallback(warningCallBack)
        .beforeUpdateCallback(actionsCallBack)
        .build();
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_withNullNewPricesAndEmptyExistingPrices_ShouldNotBuildUpdateActions() {
    // Preparation
    final ProductDraft existingProductDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("draftName"))
            .slug(ofEnglish("existingSlug"))
            .masterVariant(createVariantDraft("v1", null, null))
            .key("existingProduct")
            .build();

    CTP_TARGET_CLIENT.products().create(existingProductDraft).executeBlocking().getBody();

    final ProductDraft newProductDraft =
        ProductDraftBuilder.of()
            .productType(
                productTypeResourceIdentifierBuilder ->
                    productTypeResourceIdentifierBuilder.id(productType.getKey()))
            .name(ofEnglish("draftName"))
            .slug(ofEnglish("existingSlug"))
            .masterVariant(createVariantDraft("v1", null, null))
            .key("existingProduct")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(newProductDraft)).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 0, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(updateActionsFromSync).isEmpty();

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(newProductDraft.getKey())
            .get()
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(productProjection).isNotNull();
    assertThat(productProjection.getMasterVariant().getPrices()).isEmpty();
  }

  @Test
  void sync_withAllMatchingPrices_ShouldNotBuildActions() {
    // Preparation
    final List<PriceDraft> oldPrices =
        asList(DRAFT_US_111_USD, DRAFT_DE_111_EUR, DRAFT_DE_111_EUR_01_02, DRAFT_DE_111_USD);

    final ProductDraft existingProductDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(createVariantDraft("foo", null, oldPrices))
            .key("bar")
            .build();

    CTP_TARGET_CLIENT.products().create(existingProductDraft).executeBlocking().getBody();

    final ProductDraft newProductDraft =
        ProductDraftBuilder.of()
            .productType(
                productTypeResourceIdentifierBuilder ->
                    productTypeResourceIdentifierBuilder.id(productType.getKey()))
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(createVariantDraft("foo", null, oldPrices))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(newProductDraft)).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 0, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(updateActionsFromSync).isEmpty();

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(newProductDraft.getKey())
            .get()
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(productProjection).isNotNull();
    assertPricesAreEqual(productProjection.getMasterVariant().getPrices(), oldPrices);
  }

  @Test
  void withNonEmptyNewPricesButEmptyExistingPrices_ShouldAddAllNewPrices() {
    // Preparation
    final List<PriceDraft> newPrices =
        asList(
            DRAFT_US_111_USD,
            DRAFT_DE_111_EUR,
            DRAFT_DE_111_EUR_01_02,
            DRAFT_DE_111_EUR_02_03,
            DRAFT_DE_111_USD,
            DRAFT_UK_111_GBP);

    final ProductDraft existingProductDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(createVariantDraft("foo", null, null))
            .key("bar")
            .build();

    final Product product =
        CTP_TARGET_CLIENT.products().create(existingProductDraft).executeBlocking().getBody();

    final ProductDraft newProductDraft =
        ProductDraftBuilder.of()
            .productType(
                productTypeResourceIdentifierBuilder ->
                    productTypeResourceIdentifierBuilder.id(productType.getKey()))
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(createVariantDraft("foo", null, newPrices))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(newProductDraft)).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    final Long masterVariantId = product.getMasterData().getStaged().getMasterVariant().getId();
    assertThat(updateActionsFromSync)
        .containsExactlyInAnyOrder(
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_US_111_USD)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_DE_111_EUR)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_DE_111_EUR_01_02)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_DE_111_EUR_02_03)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_DE_111_USD)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_UK_111_GBP)
                .staged(true)
                .build());

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(newProductDraft.getKey())
            .get()
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(productProjection).isNotNull();
    assertPricesAreEqual(productProjection.getMasterVariant().getPrices(), newPrices);
  }

  @Test
  void withNonEmptyNewWithDuplicatesPricesButEmptyExistingPrices_ShouldNotSyncPrices() {
    // Preparation
    final List<PriceDraft> newPrices =
        asList(
            DRAFT_US_111_USD,
            DRAFT_US_111_USD,
            DRAFT_DE_111_EUR,
            DRAFT_DE_111_EUR_01_02,
            DRAFT_DE_111_EUR_02_03,
            DRAFT_DE_111_USD,
            DRAFT_UK_111_GBP);

    final ProductDraft existingProductDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(createVariantDraft("foo", null, null))
            .key("bar")
            .build();

    final Product product =
        CTP_TARGET_CLIENT.products().create(existingProductDraft).executeBlocking().getBody();

    final ProductDraft newProductDraft =
        ProductDraftBuilder.of()
            .productType(
                productTypeResourceIdentifierBuilder ->
                    productTypeResourceIdentifierBuilder.id(productType.getKey()))
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(createVariantDraft("foo", null, newPrices))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(newProductDraft)).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);

    assertThat(errorCallBackExceptions)
        .hasSize(1)
        .extracting(Throwable::getMessage)
        .extracting(String::toLowerCase)
        .singleElement(as(STRING))
        .contains("duplicate price scope");
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .extracting(String::toLowerCase)
        .singleElement(as(STRING))
        .contains("duplicate price scope");
    assertThat(warningCallBackMessages).isEmpty();

    final Long masterVariantId = product.getMasterData().getStaged().getMasterVariant().getId();
    assertThat(updateActionsFromSync)
        .containsExactlyInAnyOrder(
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_US_111_USD)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_US_111_USD)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_DE_111_EUR)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_DE_111_EUR_01_02)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_DE_111_EUR_02_03)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_DE_111_USD)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_UK_111_GBP)
                .staged(true)
                .build());

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(newProductDraft.getKey())
            .get()
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(productProjection).isNotNull();
    assertPricesAreEqual(productProjection.getMasterVariant().getPrices(), emptyList());
  }

  @Test
  void sync_withNullNewPrices_ShouldRemoveAllPrices() {
    // Preparation
    final List<PriceDraft> oldPrices =
        asList(
            DRAFT_US_111_USD,
            DRAFT_DE_111_EUR,
            DRAFT_DE_111_EUR_01_02,
            DRAFT_DE_111_EUR_02_03,
            DRAFT_DE_111_USD,
            DRAFT_UK_111_GBP);

    final ProductDraft existingProductDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(createVariantDraft("foo", null, oldPrices))
            .key("bar")
            .build();

    final Product product =
        CTP_TARGET_CLIENT.products().create(existingProductDraft).executeBlocking().getBody();

    final ProductDraft newProductDraft =
        ProductDraftBuilder.of()
            .productType(
                productTypeResourceIdentifierBuilder ->
                    productTypeResourceIdentifierBuilder.id(productType.getKey()))
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(createVariantDraft("foo", null, null))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(newProductDraft)).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final ProductUpdateAction[] expectedActions =
        product.getMasterData().getStaged().getMasterVariant().getPrices().stream()
            .map(Price::getId)
            .map(id -> ProductRemovePriceActionBuilder.of().priceId(id).staged(true).build())
            .toArray(ProductUpdateAction[]::new);
    assertThat(updateActionsFromSync).containsExactlyInAnyOrder(expectedActions);

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(newProductDraft.getKey())
            .get()
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(productProjection).isNotNull();
    assertPricesAreEqual(productProjection.getMasterVariant().getPrices(), emptyList());
  }

  @Test
  void sync_withSomeChangedMatchingPrices_ShouldAddAndRemovePrices() {
    // Preparation
    final CustomerGroup cust1 = ensureCustomerGroup(CTP_TARGET_CLIENT, "cust1", "cust1");

    final List<PriceDraft> oldPrices =
        asList(DRAFT_DE_111_EUR, DRAFT_UK_111_GBP, DRAFT_DE_111_EUR_03_04, DRAFT_DE_111_EUR_01_02);

    final PriceDraft price1WithCustomerGroupWithKey =
        getPriceDraftWithKeys(BigDecimal.valueOf(222), EUR, DE, "cust1", null, null, null, null);
    final PriceDraft price2WithCustomerGroupWithKey =
        getPriceDraftWithKeys(BigDecimal.valueOf(333), USD, DE, "cust1", null, null, null, null);

    final PriceDraft price1WithCustomerGroupReferenceWithCust1Id =
        PriceDraftBuilder.of(
                getPriceDraft(
                    BigDecimal.valueOf(222), EUR, DE, cust1.getId(), null, null, null, null))
            .customerGroup(cust1.toResourceIdentifier())
            .build();
    final PriceDraft price2WithCustomerGroupReferenceWithCust1Id =
        PriceDraftBuilder.of(
                getPriceDraft(
                    BigDecimal.valueOf(333), USD, DE, cust1.getId(), null, null, null, null))
            .customerGroup(cust1.toResourceIdentifier())
            .build();

    final List<PriceDraft> newPrices =
        asList(
            DRAFT_DE_111_EUR,
            DRAFT_UK_111_GBP,
            price1WithCustomerGroupWithKey,
            price2WithCustomerGroupWithKey);

    final ProductDraft existingProductDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(createVariantDraft("foo", null, oldPrices))
            .key("bar")
            .build();

    final Product product =
        CTP_TARGET_CLIENT.products().create(existingProductDraft).executeBlocking().getBody();

    final ProductDraft newProductDraft =
        ProductDraftBuilder.of()
            .productType(
                productTypeResourceIdentifierBuilder ->
                    productTypeResourceIdentifierBuilder.id(productType.getKey()))
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(createVariantDraft("foo", null, newPrices))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(newProductDraft)).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);

    final Long masterVariantId = product.getMasterData().getStaged().getMasterVariant().getId();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof ProductRemovePriceAction)
        .hasSize(2);
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof ProductAddPriceAction)
        .hasSize(2)
        .containsExactlyInAnyOrder(
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(price1WithCustomerGroupReferenceWithCust1Id)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(price2WithCustomerGroupReferenceWithCust1Id)
                .staged(true)
                .build());

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(newProductDraft.getKey())
            .get()
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(productProjection).isNotNull();

    final List<PriceDraft> newPricesWithIds =
        asList(
            DRAFT_DE_111_EUR,
            DRAFT_UK_111_GBP,
            price1WithCustomerGroupReferenceWithCust1Id,
            price2WithCustomerGroupReferenceWithCust1Id);
    assertPricesAreEqual(productProjection.getMasterVariant().getPrices(), newPricesWithIds);
  }

  @Test
  void withMixedCasesOfPriceMatches_ShouldBuildActions() {
    // Preparation
    final CustomerGroup cust1 = ensureCustomerGroup(CTP_TARGET_CLIENT, "cust1", "cust1");
    final CustomerGroup cust2 = ensureCustomerGroup(CTP_TARGET_CLIENT, "cust2", "cust2");
    final Channel channel2 =
        ensureChannel(ChannelDraftBuilder.of().key("channel2").build(), CTP_TARGET_CLIENT);

    final Product product = createExistingProductWithPrices(cust2.getId(), channel2.getId());
    final ProductDraft newProductDraft = createProductDraftWithNewPrices();

    final ObjectNode lTextWithEnDe =
        JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red");

    final PriceDraft de222EurCust1Ofid =
        PriceDraftBuilder.of(DRAFT_DE_222_EUR_CUST1)
            .customerGroup(cust1.toResourceIdentifier())
            .build();

    final PriceDraft de333UsdCust1Ofid =
        PriceDraftBuilder.of(DRAFT_DE_333_USD_CUST1)
            .customerGroup(cust1.toResourceIdentifier())
            .build();

    final PriceDraft us666Usd0102Cust2OfId =
        PriceDraftBuilder.of(DRAFT_US_666_USD_CUST2_01_02)
            .customerGroup(cust2.toResourceIdentifier())
            .build();

    final CustomFieldsDraft customType1WithEnDeOfId =
        CustomFieldsDraftBuilder.of()
            .type(customType1.toResourceIdentifier())
            .fields(createCustomFieldsJsonMap(LOCALISED_STRING_CUSTOM_FIELD_NAME, lTextWithEnDe))
            .build();
    final PriceDraft withChannel1CustomType1WithEnDeOfId =
        getPriceDraft(
            BigDecimal.valueOf(100),
            EUR,
            DE,
            null,
            byMonth(1),
            byMonth(2),
            channel1.getId(),
            customType1WithEnDeOfId);
    final PriceDraft withChannel2CustomType1WithEnDeOfId =
        getPriceDraft(
            BigDecimal.valueOf(100),
            EUR,
            DE,
            null,
            byMonth(1),
            byMonth(2),
            channel2.getId(),
            customType1WithEnDeOfId);

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(newProductDraft)).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);

    final Long masterVariantId = product.getMasterData().getStaged().getMasterVariant().getId();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof ProductRemovePriceAction)
        .hasSize(5);
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof ProductChangePriceAction)
        .hasSize(3);
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof ProductSetProductPriceCustomTypeAction)
        .hasSize(1);
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof ProductSetProductPriceCustomFieldAction)
        .hasSize(1);

    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof ProductAddPriceAction)
        .hasSize(9)
        .containsExactlyInAnyOrder(
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(de222EurCust1Ofid)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_DE_111_EUR_01_02)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_DE_111_EUR_03_04)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(de333UsdCust1Ofid)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_UK_999_GBP)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(us666Usd0102Cust2OfId)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_FR_888_EUR_01_03)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_FR_999_EUR_03_06)
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(masterVariantId)
                .price(DRAFT_NE_777_EUR_05_07)
                .staged(true)
                .build());

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(newProductDraft.getKey())
            .get()
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(productProjection).isNotNull();

    final List<PriceDraft> newPricesWithReferenceIds =
        asList(
            DRAFT_DE_111_EUR,
            de222EurCust1Ofid,
            DRAFT_DE_111_EUR_01_02,
            DRAFT_DE_111_EUR_03_04,
            withChannel1CustomType1WithEnDeOfId,
            withChannel2CustomType1WithEnDeOfId,
            de333UsdCust1Ofid,
            DRAFT_DE_22_USD,
            DRAFT_UK_111_GBP_01_02,
            DRAFT_UK_999_GBP,
            us666Usd0102Cust2OfId,
            DRAFT_FR_888_EUR_01_03,
            DRAFT_FR_999_EUR_03_06,
            DRAFT_NE_777_EUR_01_04,
            DRAFT_NE_777_EUR_05_07);
    assertPricesAreEqual(
        productProjection.getMasterVariant().getPrices(), newPricesWithReferenceIds);
  }

  @Test
  void sync_WithEmptySetNewCustomFields_ShouldCorrectlyUpdateCustomFields() {
    // preparation
    final FieldContainer fieldContainer =
        createCustomFieldsJsonMap(
            EMPTY_SET_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.arrayNode());
    fieldContainer.setValue(
        NON_EMPTY_SEY_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.arrayNode().add("foo"));

    final CustomFieldsDraft customType1WithSet =
        CustomFieldsDraftBuilder.of()
            .type(customType1.toResourceIdentifier())
            .fields(fieldContainer)
            .build();

    final PriceDraft withChannel1CustomType1WithSet =
        getPriceDraft(
            BigDecimal.valueOf(100),
            EUR,
            DE,
            null,
            byMonth(1),
            byMonth(2),
            channel1.getId(),
            customType1WithSet);
    final ProductDraft existingProductDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(createVariantDraft("foo", null, List.of(withChannel1CustomType1WithSet)))
            .key("bar")
            .build();

    CTP_TARGET_CLIENT.products().create(existingProductDraft).executeBlocking();

    final FieldContainer newFieldContainer =
        createCustomFieldsJsonMap(
            EMPTY_SET_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.arrayNode());
    newFieldContainer.setValue(NULL_SET_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.arrayNode());
    newFieldContainer.setValue(
        NON_EMPTY_SEY_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.arrayNode());
    newFieldContainer.setValue(
        NULL_NODE_SET_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.arrayNode());

    final CustomFieldsDraft customType1WithEmptySet =
        CustomFieldsDraftBuilder.of()
            .type(
                typeResourceIdentifierBuilder ->
                    typeResourceIdentifierBuilder.key(customType1.getKey()))
            .fields(newFieldContainer)
            .build();
    final PriceDraft withChannel1CustomType1WithNullSet =
        getPriceDraftWithKeys(
            BigDecimal.valueOf(100),
            EUR,
            DE,
            null,
            byMonth(1),
            byMonth(2),
            channel1.getKey(),
            customType1WithEmptySet);
    final ProductDraft newProductDraft =
        ProductDraftBuilder.of()
            .productType(
                productTypeResourceIdentifierBuilder ->
                    productTypeResourceIdentifierBuilder.id(productType.getKey()))
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(
                createVariantDraft("foo", null, List.of(withChannel1CustomType1WithNullSet)))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(newProductDraft)).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof ProductSetProductPriceCustomFieldAction)
        .hasSize(3);

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(newProductDraft.getKey())
            .get()
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(productProjection).isNotNull();

    final List<Price> prices = productProjection.getMasterVariant().getPrices();
    for (Price price : prices) {
      assertThat(
              convertCustomValueObjDataToJsonNode(
                  price.getCustom().getFields().values().get(EMPTY_SET_CUSTOM_FIELD_NAME)))
          .isEmpty();
      assertThat(
              convertCustomValueObjDataToJsonNode(
                  price.getCustom().getFields().values().get(NULL_SET_CUSTOM_FIELD_NAME)))
          .isEmpty();
      assertThat(
              convertCustomValueObjDataToJsonNode(
                  price.getCustom().getFields().values().get(NULL_NODE_SET_CUSTOM_FIELD_NAME)))
          .isEmpty();
      assertThat(
              convertCustomValueObjDataToJsonNode(
                  price.getCustom().getFields().values().get(NON_EMPTY_SEY_CUSTOM_FIELD_NAME)))
          .isEmpty();
    }
  }

  @Test
  void sync_WithNonEmptySetNewCustomFields_ShouldCorrectlyUpdateCustomFields() {
    // preparation
    final FieldContainer fieldContainer =
        createCustomFieldsJsonMap(
            EMPTY_SET_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.arrayNode());
    fieldContainer.setValue(
        NON_EMPTY_SEY_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.arrayNode().add("foo"));

    final CustomFieldsDraft customType1WithSet =
        CustomFieldsDraftBuilder.of()
            .type(
                typeResourceIdentifierBuilder ->
                    typeResourceIdentifierBuilder.id(customType1.getId()))
            .fields(fieldContainer)
            .build();

    final PriceDraft withChannel1CustomType1WithSet =
        getPriceDraft(
            BigDecimal.valueOf(100),
            EUR,
            DE,
            null,
            byMonth(1),
            byMonth(2),
            channel1.getId(),
            customType1WithSet);
    final ProductDraft existingProductDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(createVariantDraft("foo", null, List.of(withChannel1CustomType1WithSet)))
            .key("bar")
            .build();

    CTP_TARGET_CLIENT.products().create(existingProductDraft).executeBlocking();

    final ArrayNode nonEmptyNewSet = JsonNodeFactory.instance.arrayNode().add("bar");
    final FieldContainer newFieldContainer =
        createCustomFieldsJsonMap(EMPTY_SET_CUSTOM_FIELD_NAME, nonEmptyNewSet);
    newFieldContainer.setValue(NULL_SET_CUSTOM_FIELD_NAME, nonEmptyNewSet);
    newFieldContainer.setValue(NON_EMPTY_SEY_CUSTOM_FIELD_NAME, nonEmptyNewSet);
    newFieldContainer.setValue(NULL_NODE_SET_CUSTOM_FIELD_NAME, nonEmptyNewSet);

    final CustomFieldsDraft customType1WithEmptySet =
        CustomFieldsDraftBuilder.of()
            .type(
                typeResourceIdentifierBuilder ->
                    typeResourceIdentifierBuilder.key(customType1.getKey()))
            .fields(newFieldContainer)
            .build();
    final PriceDraft withChannel1CustomType1WithNullSet =
        getPriceDraftWithKeys(
            BigDecimal.valueOf(100),
            EUR,
            DE,
            null,
            byMonth(1),
            byMonth(2),
            channel1.getKey(),
            customType1WithEmptySet);
    final ProductDraft newProductDraft =
        ProductDraftBuilder.of()
            .productType(
                productTypeResourceIdentifierBuilder ->
                    productTypeResourceIdentifierBuilder.id(productType.getKey()))
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(
                createVariantDraft("foo", null, List.of(withChannel1CustomType1WithNullSet)))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(newProductDraft)).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof ProductSetProductPriceCustomFieldAction)
        .hasSize(4);

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(newProductDraft.getKey())
            .get()
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(productProjection).isNotNull();

    final List<Price> prices = productProjection.getMasterVariant().getPrices();
    for (Price price : prices) {
      final Map<String, Object> fieldValues = price.getCustom().getFields().values();
      assertThat((List) fieldValues.get(EMPTY_SET_CUSTOM_FIELD_NAME)).containsOnly("bar");
      assertThat((List) fieldValues.get(NULL_SET_CUSTOM_FIELD_NAME)).containsOnly("bar");
      assertThat((List) fieldValues.get(NULL_NODE_SET_CUSTOM_FIELD_NAME)).containsOnly("bar");
      assertThat((List) fieldValues.get(NON_EMPTY_SEY_CUSTOM_FIELD_NAME)).containsOnly("bar");
    }
  }

  @Test
  void sync_WithNullNewCustomFields_ShouldCorrectlyUpdateCustomFields() {
    // preparation
    final FieldContainer fieldContainer =
        createCustomFieldsJsonMap(
            EMPTY_SET_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.arrayNode());
    fieldContainer.setValue(
        NON_EMPTY_SEY_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.arrayNode().add("foo"));

    final CustomFieldsDraft customType1WithSet =
        CustomFieldsDraftBuilder.of()
            .type(
                typeResourceIdentifierBuilder ->
                    typeResourceIdentifierBuilder.id(customType1.getId()))
            .fields(fieldContainer)
            .build();

    final PriceDraft withChannel1CustomType1WithSet =
        getPriceDraft(
            BigDecimal.valueOf(100),
            EUR,
            DE,
            null,
            byMonth(1),
            byMonth(2),
            channel1.getId(),
            customType1WithSet);
    final ProductDraft existingProductDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(createVariantDraft("foo", null, List.of(withChannel1CustomType1WithSet)))
            .key("bar")
            .build();

    CTP_TARGET_CLIENT.products().create(existingProductDraft).executeBlocking();

    final CustomFieldsDraft customType1WithEmptySet =
        CustomFieldsDraftBuilder.of()
            .type(
                typeResourceIdentifierBuilder ->
                    typeResourceIdentifierBuilder.key(customType1.getKey()))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(new HashMap<>()))
            .build();
    final PriceDraft withChannel1CustomType1WithNullSet =
        getPriceDraftWithKeys(
            BigDecimal.valueOf(100),
            EUR,
            DE,
            null,
            byMonth(1),
            byMonth(2),
            channel1.getKey(),
            customType1WithEmptySet);

    final ProductDraft newProductDraft =
        ProductDraftBuilder.of()
            .productType(
                productTypeResourceIdentifierBuilder ->
                    productTypeResourceIdentifierBuilder.id(productType.getKey()))
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(
                createVariantDraft("foo", null, List.of(withChannel1CustomType1WithNullSet)))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(newProductDraft)).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof ProductSetProductPriceCustomFieldAction)
        .hasSize(2);

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(newProductDraft.getKey())
            .get()
            .withStaged(true)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(productProjection).isNotNull();

    final List<Price> prices = productProjection.getMasterVariant().getPrices();
    for (Price price : prices) {
      final Map<String, Object> customFieldValues = price.getCustom().getFields().values();
      assertThat(customFieldValues.get(EMPTY_SET_CUSTOM_FIELD_NAME)).isNull();
      assertThat(customFieldValues.get(NULL_SET_CUSTOM_FIELD_NAME)).isNull();
      assertThat(customFieldValues.get(NULL_NODE_SET_CUSTOM_FIELD_NAME)).isNull();
      assertThat(customFieldValues.get(NON_EMPTY_SEY_CUSTOM_FIELD_NAME)).isNull();
    }
  }

  /**
   * Creates a productDraft with a master variant containing the following priceDrafts:
   *
   * <ul>
   *   <li>DE_111_EUR
   *   <li>DE_222_EUR_CUST1
   *   <li>DE_111_EUR_01_02
   *   <li>DE_111_EUR_03_04
   *   <li>DE_100_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELD{en, de}
   *   <li>DE_100_EUR_01_02_CHANNEL2_CUSTOMTYPE1_CUSTOMFIELD{en, de}
   *   <li>DE_333_USD_CUST1
   *   <li>DE_22_USD
   *   <li>UK_111_GBP_01_02
   *   <li>UK_999_GBP
   *   <li>US_666_USD_CUST2_01_02,
   *   <li>FR_888_EUR_01_03
   *   <li>FR_999_EUR_03_06
   *   <li>NE_777_EUR_01_04
   *   <li>NE_777_EUR_05_07
   * </ul>
   */
  private ProductDraft createProductDraftWithNewPrices() {
    final ObjectNode lTextWithEnDe =
        JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red");

    final CustomFieldsDraft customType1WithEnDeOfKey =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key("customType1"))
            .fields(createCustomFieldsJsonMap(LOCALISED_STRING_CUSTOM_FIELD_NAME, lTextWithEnDe))
            .build();
    final PriceDraft withChannel1CustomType1WithEnDeOfKey =
        getPriceDraftWithKeys(
            BigDecimal.valueOf(100),
            EUR,
            DE,
            null,
            byMonth(1),
            byMonth(2),
            "channel1",
            customType1WithEnDeOfKey);
    final PriceDraft withChannel2CustomType1WithEnDeOfKey =
        getPriceDraftWithKeys(
            BigDecimal.valueOf(100),
            EUR,
            DE,
            null,
            byMonth(1),
            byMonth(2),
            "channel2",
            customType1WithEnDeOfKey);

    final List<PriceDraft> newPrices =
        List.of(
            DRAFT_DE_111_EUR,
            DRAFT_DE_222_EUR_CUST1_KEY,
            DRAFT_DE_111_EUR_01_02,
            DRAFT_DE_111_EUR_03_04,
            withChannel1CustomType1WithEnDeOfKey,
            withChannel2CustomType1WithEnDeOfKey,
            DRAFT_DE_333_USD_CUST1_KEY,
            DRAFT_DE_22_USD,
            DRAFT_UK_111_GBP_01_02,
            DRAFT_UK_999_GBP,
            DRAFT_US_666_USD_CUST2_01_02_KEY,
            DRAFT_FR_888_EUR_01_03,
            DRAFT_FR_999_EUR_03_06,
            DRAFT_NE_777_EUR_01_04,
            DRAFT_NE_777_EUR_05_07);

    return ProductDraftBuilder.of()
        .productType(
            productTypeResourceIdentifierBuilder ->
                productTypeResourceIdentifierBuilder.id(productType.getKey()))
        .name(ofEnglish("foo"))
        .slug(ofEnglish("bar"))
        .masterVariant(createVariantDraft("foo", null, newPrices))
        .key("bar")
        .build();
  }

  /**
   * Creates a product with a master variant containing the following prices:
   *
   * <ul>
   *   <li>DE_111_EUR
   *   <li>DE_345_EUR_CUST2
   *   <li>DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE1_CUSTOMFIELD{en, de, it}
   *   <li>DE_222_EUR_01_02_CHANNEL1_CUSTOMTYPE2_CUSTOMFIELD{en, de}
   *   <li>DE_22_USD
   *   <li>UK_111_GBP_01_02
   *   <li>UK_111_GBP_02_03
   *   <li>UK_333_GBP_03_05
   *   <li>FR_777_EUR_01_04
   *   <li>NE_123_EUR_01_04
   *   <li>NE_321_EUR_04_06
   * </ul>
   *
   * @return
   * @param existingCustomerGroupId
   */
  private Product createExistingProductWithPrices(
      @Nonnull final String existingCustomerGroupId, @Nonnull final String existingChannelId) {
    final Type customType2 =
        ensurePricesCustomType("customType2", Locale.ENGLISH, "customType2", CTP_TARGET_CLIENT);

    final ObjectNode lTextWithEnDeIt =
        JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red").put("it", "rosso");

    final ObjectNode lTextWithEnDe =
        JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red");

    final CustomFieldsDraft customType1WithEnDeItOfId =
        CustomFieldsDraftBuilder.of()
            .type(customType1.toResourceIdentifier())
            .fields(createCustomFieldsJsonMap(LOCALISED_STRING_CUSTOM_FIELD_NAME, lTextWithEnDeIt))
            .build();

    final CustomFieldsDraft customType2WithEnDeOfId =
        CustomFieldsDraftBuilder.of()
            .type(customType2.toResourceIdentifier())
            .fields(createCustomFieldsJsonMap(LOCALISED_STRING_CUSTOM_FIELD_NAME, lTextWithEnDe))
            .build();

    final PriceDraft de222Eur0102Channel1Ct1DeEnItOfId =
        getPriceDraft(
            BigDecimal.valueOf(222),
            EUR,
            DE,
            null,
            byMonth(1),
            byMonth(2),
            channel1.getId(),
            customType1WithEnDeItOfId);

    final PriceDraft de222Eur0102Channel2Ct2DeEnOfId =
        getPriceDraft(
            BigDecimal.valueOf(222),
            EUR,
            DE,
            null,
            byMonth(1),
            byMonth(2),
            existingChannelId,
            customType2WithEnDeOfId);

    final PriceDraft de345EurCust2OfId =
        getPriceDraft(
            BigDecimal.valueOf(345), EUR, DE, existingCustomerGroupId, null, null, null, null);

    final List<PriceDraft> oldPrices =
        List.of(
            DRAFT_DE_111_EUR,
            de345EurCust2OfId,
            de222Eur0102Channel1Ct1DeEnItOfId,
            de222Eur0102Channel2Ct2DeEnOfId,
            DRAFT_DE_22_USD,
            DRAFT_UK_111_GBP_01_02,
            DRAFT_UK_111_GBP_02_03,
            DRAFT_UK_333_GBP_03_05,
            DRAFT_FR_777_EUR_01_04,
            DRAFT_NE_123_EUR_01_04,
            DRAFT_NE_321_EUR_04_06);

    final ProductDraft existingProductDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .masterVariant(createVariantDraft("foo", null, oldPrices))
            .key("bar")
            .build();

    return CTP_TARGET_CLIENT.products().create(existingProductDraft).executeBlocking().getBody();
  }

  /**
   * Asserts that a list of {@link Asset} and a list of {@link AssetDraft} have the same ordering of
   * prices (prices are matched by key). It asserts that the matching assets have the same name,
   * description, custom fields, tags, and asset sources.
   *
   * @param prices the list of prices to compare to the list of price drafts.
   * @param priceDrafts the list of price drafts to compare to the list of prices.
   */
  static void assertPricesAreEqual(
      @Nonnull final List<Price> prices, @Nonnull final List<PriceDraft> priceDrafts) {

    final Map<PriceCompositeId, Price> priceMap = collectionToMap(prices, PriceCompositeId::of);
    final PriceCompositeId[] priceCompositeIds =
        priceDrafts.stream().map(PriceCompositeId::of).toArray(PriceCompositeId[]::new);

    assertThat(priceMap).containsOnlyKeys(priceCompositeIds);
  }
}
