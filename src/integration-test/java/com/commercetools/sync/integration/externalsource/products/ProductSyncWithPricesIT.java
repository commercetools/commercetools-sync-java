package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.utils.CollectionUtils.collectionToMap;
import static com.commercetools.sync.integration.commons.utils.ChannelITUtils.createChannel;
import static com.commercetools.sync.integration.commons.utils.CustomerGroupITUtils.createCustomerGroup;
import static com.commercetools.sync.integration.commons.utils.ITUtils.EMPTY_SET_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.LOCALISED_STRING_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.NON_EMPTY_SEY_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.NULL_NODE_SET_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.NULL_SET_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createVariantDraft;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.createPricesCustomType;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_EUR;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_EUR_01_02;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_EUR_02_03;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_EUR_03_04;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_111_USD;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_222_EUR_CUST1;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_222_EUR_CUST1_KEY;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_22_USD;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_333_USD_CUST1;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_DE_333_USD_CUST1_KEY;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_FR_777_EUR_01_04;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_FR_888_EUR_01_03;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_FR_999_EUR_03_06;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_NE_123_EUR_01_04;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_NE_321_EUR_04_06;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_NE_777_EUR_01_04;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_NE_777_EUR_05_07;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_111_GBP;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_111_GBP_01_02;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_111_GBP_02_03;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_333_GBP_03_05;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_UK_999_GBP;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_US_111_USD;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_US_666_USD_CUST2_01_02;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_US_666_USD_CUST2_01_02_KEY;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.byMonth;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.createCustomFieldsJsonMap;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.getPriceDraft;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.getPriceDraftWithKeys;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static com.neovisionaries.i18n.CountryCode.DE;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static io.sphere.sdk.models.DefaultCurrencyUnits.USD;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.producttypes.ProductType.referenceOfId;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.internals.helpers.PriceCompositeId;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductProjectionType;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.AddPrice;
import io.sphere.sdk.products.commands.updateactions.ChangePrice;
import io.sphere.sdk.products.commands.updateactions.RemovePrice;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomField;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomType;
import io.sphere.sdk.products.queries.ProductProjectionByKeyGet;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncWithPricesIT {
  private static ProductType productType;
  private static Type customType1;
  private static Type customType2;
  private static Channel channel1;
  private static Channel channel2;
  private static CustomerGroup cust1;
  private static CustomerGroup cust2;
  private Product product;
  private ProductSync productSync;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<UpdateAction<Product>> updateActionsFromSync;

  /**
   * Delete all product related test data from the target project. Then creates price custom types,
   * customer groups, channels and a product type for the target CTP project.
   */
  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);

    customType1 =
        createPricesCustomType("customType1", Locale.ENGLISH, "customType1", CTP_TARGET_CLIENT);

    customType2 =
        createPricesCustomType("customType2", Locale.ENGLISH, "customType2", CTP_TARGET_CLIENT);

    cust1 = createCustomerGroup(CTP_TARGET_CLIENT, "cust1", "cust1");
    cust2 = createCustomerGroup(CTP_TARGET_CLIENT, "cust2", "cust2");

    channel1 = createChannel(CTP_TARGET_CLIENT, "channel1", "channel1");
    channel2 = createChannel(CTP_TARGET_CLIENT, "channel2", "channel2");

    productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
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
            List<UpdateAction<Product>>>
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
            List<UpdateAction<Product>>,
            ProductDraft,
            ProductProjection,
            List<UpdateAction<Product>>>
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
        ProductDraftBuilder.of(
                productType.toReference(),
                ofEnglish("draftName"),
                ofEnglish("existingSlug"),
                createVariantDraft("v1", null, null))
            .key("existingProduct")
            .build();

    product =
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)));

    final ProductDraft newProductDraft =
        ProductDraftBuilder.of(
                referenceOfId(productType.getKey()),
                ofEnglish("draftName"),
                ofEnglish("existingSlug"),
                createVariantDraft("v1", null, null))
            .key("existingProduct")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        executeBlocking(productSync.sync(singletonList(newProductDraft)));

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 0, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(updateActionsFromSync).isEmpty();

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionByKeyGet.of(
                    newProductDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture()
            .join();

    assertThat(productProjection).isNotNull();
    assertThat(productProjection.getMasterVariant().getPrices()).isEmpty();
  }

  @Test
  void sync_withAllMatchingPrices_ShouldNotBuildActions() {
    // Preparation
    final List<PriceDraft> oldPrices =
        asList(DRAFT_US_111_USD, DRAFT_DE_111_EUR, DRAFT_DE_111_EUR_01_02, DRAFT_DE_111_USD);

    final ProductDraft existingProductDraft =
        ProductDraftBuilder.of(
                productType.toReference(),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, oldPrices))
            .key("bar")
            .build();

    product =
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)));

    final ProductDraft newProductDraft =
        ProductDraftBuilder.of(
                referenceOfId(productType.getKey()),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, oldPrices))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        executeBlocking(productSync.sync(singletonList(newProductDraft)));

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 0, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(updateActionsFromSync).isEmpty();

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionByKeyGet.of(
                    newProductDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture()
            .join();

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
        ProductDraftBuilder.of(
                productType.toReference(),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, null))
            .key("bar")
            .build();

    product =
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)));

    final ProductDraft newProductDraft =
        ProductDraftBuilder.of(
                referenceOfId(productType.getKey()),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, newPrices))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        executeBlocking(productSync.sync(singletonList(newProductDraft)));

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    final Integer masterVariantId = product.getMasterData().getStaged().getMasterVariant().getId();
    assertThat(updateActionsFromSync)
        .containsExactlyInAnyOrder(
            AddPrice.ofVariantId(masterVariantId, DRAFT_US_111_USD, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_DE_111_EUR, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_DE_111_EUR_01_02, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_DE_111_EUR_02_03, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_DE_111_USD, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_UK_111_GBP, true));

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionByKeyGet.of(
                    newProductDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture()
            .join();

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
        ProductDraftBuilder.of(
                productType.toReference(),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, null))
            .key("bar")
            .build();

    product =
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)));

    final ProductDraft newProductDraft =
        ProductDraftBuilder.of(
                referenceOfId(productType.getKey()),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, newPrices))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        executeBlocking(productSync.sync(singletonList(newProductDraft)));

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

    final Integer masterVariantId = product.getMasterData().getStaged().getMasterVariant().getId();
    assertThat(updateActionsFromSync)
        .containsExactlyInAnyOrder(
            AddPrice.ofVariantId(masterVariantId, DRAFT_US_111_USD, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_US_111_USD, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_DE_111_EUR, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_DE_111_EUR_01_02, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_DE_111_EUR_02_03, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_DE_111_USD, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_UK_111_GBP, true));

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionByKeyGet.of(
                    newProductDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture()
            .join();

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
        ProductDraftBuilder.of(
                productType.toReference(),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, oldPrices))
            .key("bar")
            .build();

    product =
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)));

    final ProductDraft newProductDraft =
        ProductDraftBuilder.of(
                referenceOfId(productType.getKey()),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, null))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        executeBlocking(productSync.sync(singletonList(newProductDraft)));

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final RemovePrice[] expectedActions =
        product.getMasterData().getStaged().getMasterVariant().getPrices().stream()
            .map(Price::getId)
            .map(id -> RemovePrice.of(id, true))
            .toArray(RemovePrice[]::new);
    assertThat(updateActionsFromSync).containsExactlyInAnyOrder(expectedActions);

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionByKeyGet.of(
                    newProductDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture()
            .join();

    assertThat(productProjection).isNotNull();
    assertPricesAreEqual(productProjection.getMasterVariant().getPrices(), emptyList());
  }

  @Test
  void sync_withSomeChangedMatchingPrices_ShouldAddAndRemovePrices() {
    // Preparation
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
            .customerGroup(cust1)
            .build();
    final PriceDraft price2WithCustomerGroupReferenceWithCust1Id =
        PriceDraftBuilder.of(
                getPriceDraft(
                    BigDecimal.valueOf(333), USD, DE, cust1.getId(), null, null, null, null))
            .customerGroup(cust1)
            .build();

    final List<PriceDraft> newPrices =
        asList(
            DRAFT_DE_111_EUR,
            DRAFT_UK_111_GBP,
            price1WithCustomerGroupWithKey,
            price2WithCustomerGroupWithKey);

    final ProductDraft existingProductDraft =
        ProductDraftBuilder.of(
                productType.toReference(),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, oldPrices))
            .key("bar")
            .build();

    product =
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)));

    final ProductDraft newProductDraft =
        ProductDraftBuilder.of(
                referenceOfId(productType.getKey()),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, newPrices))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        executeBlocking(productSync.sync(singletonList(newProductDraft)));

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);

    final Integer masterVariantId = product.getMasterData().getStaged().getMasterVariant().getId();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof RemovePrice)
        .hasSize(2);
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof AddPrice)
        .hasSize(2)
        .containsExactlyInAnyOrder(
            AddPrice.ofVariantId(
                masterVariantId, price1WithCustomerGroupReferenceWithCust1Id, true),
            AddPrice.ofVariantId(
                masterVariantId, price2WithCustomerGroupReferenceWithCust1Id, true));

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionByKeyGet.of(
                    newProductDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture()
            .join();

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
    createExistingProductWithPrices();
    final ProductDraft newProductDraft = createProductDraftWithNewPrices();

    final ObjectNode lTextWithEnDe =
        JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red");

    final PriceDraft de222EurCust1Ofid =
        PriceDraftBuilder.of(DRAFT_DE_222_EUR_CUST1).customerGroup(cust1).build();

    final PriceDraft de333UsdCust1Ofid =
        PriceDraftBuilder.of(DRAFT_DE_333_USD_CUST1).customerGroup(cust1).build();

    final PriceDraft us666Usd0102Cust2OfId =
        PriceDraftBuilder.of(DRAFT_US_666_USD_CUST2_01_02).customerGroup(cust2).build();

    final CustomFieldsDraft customType1WithEnDeOfId =
        CustomFieldsDraft.ofTypeIdAndJson(
            customType1.getId(),
            createCustomFieldsJsonMap(LOCALISED_STRING_CUSTOM_FIELD_NAME, lTextWithEnDe));
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
        executeBlocking(productSync.sync(singletonList(newProductDraft)));

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);

    final Integer masterVariantId = product.getMasterData().getStaged().getMasterVariant().getId();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof RemovePrice)
        .hasSize(5);
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof ChangePrice)
        .hasSize(3);
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof SetProductPriceCustomType)
        .hasSize(1);
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof SetProductPriceCustomField)
        .hasSize(1);

    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof AddPrice)
        .hasSize(9)
        .containsExactlyInAnyOrder(
            AddPrice.ofVariantId(masterVariantId, de222EurCust1Ofid, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_DE_111_EUR_01_02, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_DE_111_EUR_03_04, true),
            AddPrice.ofVariantId(masterVariantId, de333UsdCust1Ofid, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_UK_999_GBP, true),
            AddPrice.ofVariantId(masterVariantId, us666Usd0102Cust2OfId, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_FR_888_EUR_01_03, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_FR_999_EUR_03_06, true),
            AddPrice.ofVariantId(masterVariantId, DRAFT_NE_777_EUR_05_07, true));

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionByKeyGet.of(
                    newProductDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture()
            .join();

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
    final ArrayNode emptySet = JsonNodeFactory.instance.arrayNode();
    final Map<String, JsonNode> customFieldsJsonMap =
        createCustomFieldsJsonMap(EMPTY_SET_CUSTOM_FIELD_NAME, emptySet);
    final ArrayNode nonEmptySet = JsonNodeFactory.instance.arrayNode();
    nonEmptySet.add("foo");
    customFieldsJsonMap.put(NON_EMPTY_SEY_CUSTOM_FIELD_NAME, nonEmptySet);

    final CustomFieldsDraft customType1WithSet =
        CustomFieldsDraft.ofTypeIdAndJson(customType1.getId(), customFieldsJsonMap);

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
        ProductDraftBuilder.of(
                productType.toReference(),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, singletonList(withChannel1CustomType1WithSet)))
            .key("bar")
            .build();

    product =
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)));

    final Map<String, JsonNode> newCustomFieldsJsonMap =
        createCustomFieldsJsonMap(EMPTY_SET_CUSTOM_FIELD_NAME, emptySet);
    newCustomFieldsJsonMap.put(NULL_SET_CUSTOM_FIELD_NAME, emptySet);
    newCustomFieldsJsonMap.put(NON_EMPTY_SEY_CUSTOM_FIELD_NAME, emptySet);
    newCustomFieldsJsonMap.put(NULL_NODE_SET_CUSTOM_FIELD_NAME, emptySet);

    final CustomFieldsDraft customType1WithEmptySet =
        CustomFieldsDraft.ofTypeKeyAndJson(customType1.getKey(), newCustomFieldsJsonMap);
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
        ProductDraftBuilder.of(
                referenceOfId(productType.getKey()),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, singletonList(withChannel1CustomType1WithNullSet)))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        executeBlocking(productSync.sync(singletonList(newProductDraft)));

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof SetProductPriceCustomField)
        .hasSize(3);

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionByKeyGet.of(
                    newProductDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture()
            .join();

    assertThat(productProjection).isNotNull();

    final List<Price> prices = productProjection.getMasterVariant().getPrices();
    for (Price price : prices) {
      assertThat(price.getCustom().getFieldAsStringSet(EMPTY_SET_CUSTOM_FIELD_NAME)).isEmpty();
      assertThat(price.getCustom().getFieldAsStringSet(NULL_SET_CUSTOM_FIELD_NAME)).isEmpty();
      assertThat(price.getCustom().getFieldAsStringSet(NULL_NODE_SET_CUSTOM_FIELD_NAME)).isEmpty();
      assertThat(price.getCustom().getFieldAsStringSet(NON_EMPTY_SEY_CUSTOM_FIELD_NAME)).isEmpty();
    }
  }

  @Test
  void sync_WithNonEmptySetNewCustomFields_ShouldCorrectlyUpdateCustomFields() {
    // preparation
    final ArrayNode emptySet = JsonNodeFactory.instance.arrayNode();
    final Map<String, JsonNode> customFieldsJsonMap =
        createCustomFieldsJsonMap(EMPTY_SET_CUSTOM_FIELD_NAME, emptySet);
    final ArrayNode nonEmptySet = JsonNodeFactory.instance.arrayNode();
    nonEmptySet.add("foo");
    customFieldsJsonMap.put(NON_EMPTY_SEY_CUSTOM_FIELD_NAME, nonEmptySet);

    final CustomFieldsDraft customType1WithSet =
        CustomFieldsDraft.ofTypeIdAndJson(customType1.getId(), customFieldsJsonMap);

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
        ProductDraftBuilder.of(
                productType.toReference(),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, singletonList(withChannel1CustomType1WithSet)))
            .key("bar")
            .build();

    product =
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)));

    final ArrayNode nonEmptyNewSet = JsonNodeFactory.instance.arrayNode();
    nonEmptyNewSet.add("bar");
    final Map<String, JsonNode> newCustomFieldsJsonMap =
        createCustomFieldsJsonMap(EMPTY_SET_CUSTOM_FIELD_NAME, nonEmptyNewSet);
    newCustomFieldsJsonMap.put(NULL_SET_CUSTOM_FIELD_NAME, nonEmptyNewSet);
    newCustomFieldsJsonMap.put(NON_EMPTY_SEY_CUSTOM_FIELD_NAME, nonEmptyNewSet);
    newCustomFieldsJsonMap.put(NULL_NODE_SET_CUSTOM_FIELD_NAME, nonEmptyNewSet);

    final CustomFieldsDraft customType1WithEmptySet =
        CustomFieldsDraft.ofTypeKeyAndJson(customType1.getKey(), newCustomFieldsJsonMap);
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
        ProductDraftBuilder.of(
                referenceOfId(productType.getKey()),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, singletonList(withChannel1CustomType1WithNullSet)))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        executeBlocking(productSync.sync(singletonList(newProductDraft)));

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof SetProductPriceCustomField)
        .hasSize(4);

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionByKeyGet.of(
                    newProductDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture()
            .join();

    assertThat(productProjection).isNotNull();

    final List<Price> prices = productProjection.getMasterVariant().getPrices();
    for (Price price : prices) {
      assertThat(price.getCustom().getFieldAsStringSet(EMPTY_SET_CUSTOM_FIELD_NAME))
          .containsOnly("bar");
      assertThat(price.getCustom().getFieldAsStringSet(NULL_SET_CUSTOM_FIELD_NAME))
          .containsOnly("bar");
      assertThat(price.getCustom().getFieldAsStringSet(NULL_NODE_SET_CUSTOM_FIELD_NAME))
          .containsOnly("bar");
      assertThat(price.getCustom().getFieldAsStringSet(NON_EMPTY_SEY_CUSTOM_FIELD_NAME))
          .containsOnly("bar");
    }
  }

  @Test
  void sync_WithNullJsonNodeNewCustomFields_ShouldCorrectlyUpdateCustomFields() {
    // preparation
    final ArrayNode emptySet = JsonNodeFactory.instance.arrayNode();
    final Map<String, JsonNode> customFieldsJsonMap =
        createCustomFieldsJsonMap(EMPTY_SET_CUSTOM_FIELD_NAME, emptySet);
    final ArrayNode nonEmptySet = JsonNodeFactory.instance.arrayNode();
    nonEmptySet.add("foo");
    customFieldsJsonMap.put(NON_EMPTY_SEY_CUSTOM_FIELD_NAME, nonEmptySet);

    final CustomFieldsDraft customType1WithSet =
        CustomFieldsDraft.ofTypeIdAndJson(customType1.getId(), customFieldsJsonMap);

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
        ProductDraftBuilder.of(
                productType.toReference(),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, singletonList(withChannel1CustomType1WithSet)))
            .key("bar")
            .build();

    product =
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)));

    final NullNode nullJsonNode = JsonNodeFactory.instance.nullNode();
    final Map<String, JsonNode> newCustomFieldsJsonMap =
        createCustomFieldsJsonMap(EMPTY_SET_CUSTOM_FIELD_NAME, nullJsonNode);
    newCustomFieldsJsonMap.put(NULL_SET_CUSTOM_FIELD_NAME, nullJsonNode);
    newCustomFieldsJsonMap.put(NULL_NODE_SET_CUSTOM_FIELD_NAME, nullJsonNode);
    newCustomFieldsJsonMap.put(NON_EMPTY_SEY_CUSTOM_FIELD_NAME, nullJsonNode);

    final CustomFieldsDraft customType1WithEmptySet =
        CustomFieldsDraft.ofTypeKeyAndJson(customType1.getKey(), newCustomFieldsJsonMap);
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
        ProductDraftBuilder.of(
                referenceOfId(productType.getKey()),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, singletonList(withChannel1CustomType1WithNullSet)))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        executeBlocking(productSync.sync(singletonList(newProductDraft)));

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof SetProductPriceCustomField)
        .hasSize(2);

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionByKeyGet.of(
                    newProductDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture()
            .join();

    assertThat(productProjection).isNotNull();

    final List<Price> prices = productProjection.getMasterVariant().getPrices();
    for (Price price : prices) {
      assertThat(price.getCustom().getFieldAsStringSet(EMPTY_SET_CUSTOM_FIELD_NAME)).isNull();
      assertThat(price.getCustom().getFieldAsStringSet(NULL_SET_CUSTOM_FIELD_NAME)).isNull();
      assertThat(price.getCustom().getFieldAsStringSet(NULL_NODE_SET_CUSTOM_FIELD_NAME)).isNull();
      assertThat(price.getCustom().getFieldAsStringSet(NON_EMPTY_SEY_CUSTOM_FIELD_NAME)).isNull();
    }
  }

  @Test
  void sync_WithNullNewCustomFields_ShouldCorrectlyUpdateCustomFields() {
    // preparation
    final ArrayNode emptySet = JsonNodeFactory.instance.arrayNode();
    final Map<String, JsonNode> customFieldsJsonMap =
        createCustomFieldsJsonMap(EMPTY_SET_CUSTOM_FIELD_NAME, emptySet);
    final ArrayNode nonEmptySet = JsonNodeFactory.instance.arrayNode();
    nonEmptySet.add("foo");
    customFieldsJsonMap.put(NON_EMPTY_SEY_CUSTOM_FIELD_NAME, nonEmptySet);

    final CustomFieldsDraft customType1WithSet =
        CustomFieldsDraft.ofTypeIdAndJson(customType1.getId(), customFieldsJsonMap);

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
        ProductDraftBuilder.of(
                productType.toReference(),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, singletonList(withChannel1CustomType1WithSet)))
            .key("bar")
            .build();

    product =
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)));

    final CustomFieldsDraft customType1WithEmptySet =
        CustomFieldsDraft.ofTypeKeyAndJson(customType1.getKey(), new HashMap<>());
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
        ProductDraftBuilder.of(
                referenceOfId(productType.getKey()),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, singletonList(withChannel1CustomType1WithNullSet)))
            .key("bar")
            .build();

    // test
    final ProductSyncStatistics syncStatistics =
        executeBlocking(productSync.sync(singletonList(newProductDraft)));

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(updateActionsFromSync)
        .filteredOn(action -> action instanceof SetProductPriceCustomField)
        .hasSize(2);

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .execute(
                ProductProjectionByKeyGet.of(
                    newProductDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture()
            .join();

    assertThat(productProjection).isNotNull();

    final List<Price> prices = productProjection.getMasterVariant().getPrices();
    for (Price price : prices) {
      assertThat(price.getCustom().getFieldAsStringSet(EMPTY_SET_CUSTOM_FIELD_NAME)).isNull();
      assertThat(price.getCustom().getFieldAsStringSet(NULL_SET_CUSTOM_FIELD_NAME)).isNull();
      assertThat(price.getCustom().getFieldAsStringSet(NULL_NODE_SET_CUSTOM_FIELD_NAME)).isNull();
      assertThat(price.getCustom().getFieldAsStringSet(NON_EMPTY_SEY_CUSTOM_FIELD_NAME)).isNull();
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
        CustomFieldsDraft.ofTypeKeyAndJson(
            "customType1",
            createCustomFieldsJsonMap(LOCALISED_STRING_CUSTOM_FIELD_NAME, lTextWithEnDe));
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
        asList(
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

    return ProductDraftBuilder.of(
            referenceOfId(productType.getKey()),
            ofEnglish("foo"),
            ofEnglish("bar"),
            createVariantDraft("foo", null, newPrices))
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
   */
  private void createExistingProductWithPrices() {

    final ObjectNode lTextWithEnDeIt =
        JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red").put("it", "rosso");

    final ObjectNode lTextWithEnDe =
        JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red");

    final CustomFieldsDraft customType1WithEnDeItOfId =
        CustomFieldsDraft.ofTypeIdAndJson(
            ProductSyncWithPricesIT.customType1.getId(),
            createCustomFieldsJsonMap(LOCALISED_STRING_CUSTOM_FIELD_NAME, lTextWithEnDeIt));

    final CustomFieldsDraft customType2WithEnDeOfId =
        CustomFieldsDraft.ofTypeIdAndJson(
            customType2.getId(),
            createCustomFieldsJsonMap(LOCALISED_STRING_CUSTOM_FIELD_NAME, lTextWithEnDe));

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
            channel2.getId(),
            customType2WithEnDeOfId);

    final PriceDraft de345EurCust2OfId =
        getPriceDraft(BigDecimal.valueOf(345), EUR, DE, cust2.getId(), null, null, null, null);

    final List<PriceDraft> oldPrices =
        asList(
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
        ProductDraftBuilder.of(
                productType.toReference(),
                ofEnglish("foo"),
                ofEnglish("bar"),
                createVariantDraft("foo", null, oldPrices))
            .key("bar")
            .build();

    product =
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)));
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
