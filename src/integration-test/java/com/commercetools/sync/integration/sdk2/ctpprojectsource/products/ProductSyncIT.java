package com.commercetools.sync.integration.sdk2.ctpprojectsource.products;

import static com.commercetools.sync.integration.sdk2.commons.utils.CategoryITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.InventoryITUtils.SUPPLY_CHANNEL_KEY_1;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductTypeITUtils.ensureProductType;
import static com.commercetools.sync.integration.sdk2.commons.utils.StateITUtils.ensureState;
import static com.commercetools.sync.integration.sdk2.commons.utils.TaxCategoryITUtils.ensureTaxCategory;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.sdk2.products.ActionGroup.ATTRIBUTES;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static java.util.Collections.*;
import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelDraft;
import com.commercetools.api.models.channel.ChannelDraftBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.common.LocalizedStringBuilder;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.CategoryOrderHints;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductPublishAction;
import com.commercetools.api.models.product.ProductPublishActionBuilder;
import com.commercetools.api.models.product.ProductReference;
import com.commercetools.api.models.product.ProductSetAttributeActionBuilder;
import com.commercetools.api.models.product.ProductSetAttributeInAllVariantsActionBuilder;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifier;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.Type;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.products.ProductSync;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.SyncFilter;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.sdk2.products.utils.ProductTransformUtils;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncIT {
  private static ProductType sourceProductType;
  private static ProductType targetProductType;

  private static TaxCategory sourceTaxCategory;
  private static TaxCategory targetTaxCategory;

  private static State sourceProductState;
  private static State targetProductState;

  private static Channel sourcePriceChannel;
  private static Channel targetPriceChannel;

  private static Type sourcePriceCustomType;
  private static Type targetPriceCustomType;

  private static List<CategoryReference> sourceCategoryReferencesWithIds;

  private static List<CategoryResourceIdentifier> sourceCategoryResourceIdentifiers;
  private static List<CategoryReference> targetCategoryReferencesWithIds;

  private static List<CategoryResourceIdentifier> targetCategoryResourceIdentifiers;
  private ProductSync productSync;
  private ReferenceIdToKeyCache referenceIdToKeyCache;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<ProductUpdateAction> updateActions;
  private List<Throwable> errorCallBackExceptions;

  /**
   * Delete all product related test data from target and source projects. Then creates for both CTP
   * projects price channels, product types, tax categories, categories, custom types for categories
   * and product states.
   */
  @BeforeAll
  static void setup() {
    final ChannelDraft channelDraft1 = ChannelDraftBuilder.of().key(SUPPLY_CHANNEL_KEY_1).build();
    targetPriceChannel = ensureChannel(channelDraft1, CTP_TARGET_CLIENT);
    sourcePriceChannel = ensureChannel(channelDraft1, CTP_SOURCE_CLIENT);

    targetPriceCustomType =
        ensurePricesCustomType(
            "pricesCustomTypeKey", ENGLISH, "pricesCustomTypeName", CTP_TARGET_CLIENT);
    sourcePriceCustomType =
        ensurePricesCustomType(
            "pricesCustomTypeKey", ENGLISH, "pricesCustomTypeName", CTP_SOURCE_CLIENT);

    ensureCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY,
        Locale.ENGLISH,
        OLD_CATEGORY_CUSTOM_TYPE_NAME,
        CTP_TARGET_CLIENT);
    ensureCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY,
        Locale.ENGLISH,
        OLD_CATEGORY_CUSTOM_TYPE_NAME,
        CTP_SOURCE_CLIENT);

    final List<Category> targetCategories =
        ensureCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
    targetCategoryReferencesWithIds = getReferencesWithIds(targetCategories);
    targetCategoryResourceIdentifiers = getResourceIdentifiersWithIds(targetCategories);
    final List<Category> sourceCategories =
        ensureCategories(CTP_SOURCE_CLIENT, getCategoryDrafts(null, 2));
    sourceCategoryReferencesWithIds = getReferencesWithIds(sourceCategories);
    sourceCategoryResourceIdentifiers = getResourceIdentifiersWithIds(sourceCategories);

    targetProductType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    sourceProductType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_SOURCE_CLIENT);

    targetTaxCategory = ensureTaxCategory(CTP_TARGET_CLIENT);
    sourceTaxCategory = ensureTaxCategory(CTP_SOURCE_CLIENT);

    targetProductState = ensureState(CTP_TARGET_CLIENT, StateTypeEnum.PRODUCT_STATE);
    sourceProductState = ensureState(CTP_SOURCE_CLIENT, StateTypeEnum.PRODUCT_STATE);
  }

  /**
   * Deletes Products from the source and target CTP projects, clears the callback collections then
   * it instantiates a new {@link com.commercetools.sync.products.ProductSync} instance.
   */
  @BeforeEach
  void setupTest() {
    clearSyncTestCollections();
    deleteAllProducts(CTP_TARGET_CLIENT);
    deleteAllProducts(CTP_SOURCE_CLIENT);
    final ProductSyncOptions syncOptions = buildSyncOptions();
    productSync = new ProductSync(syncOptions);
    referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
  }

  private void clearSyncTestCollections() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
    updateActions = new ArrayList<>();
  }

  private ProductSyncOptions buildSyncOptions() {
    return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(
            (exception, oldResource, newResource, updateActions) ->
                errorCallback(exception.getMessage(), exception.getCause()))
        .warningCallback(
            (exception, oldResource, newResources) ->
                warningCallBackMessages.add(exception.getMessage()))
        .beforeUpdateCallback(this::beforeUpdateCallback)
        .build();
  }

  private void errorCallback(
      @Nonnull final String errorMessage, @Nullable final Throwable exception) {
    errorCallBackMessages.add(errorMessage);
    errorCallBackExceptions.add(exception);
  }

  private List<ProductUpdateAction> beforeUpdateCallback(
      @Nonnull final List<ProductUpdateAction> updateActions,
      @Nonnull final ProductDraft newProductDraft,
      @Nonnull final ProductProjection oldProduct) {
    this.updateActions.addAll(updateActions);
    return updateActions;
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    deleteProductSyncTestData(CTP_SOURCE_CLIENT);
  }

  @Test
  void sync_withDoubleQuotationCharacterInProductKey_ShouldSyncProducts() {
    // preparation
    final String sampleProductKey = "sample-\"product-type";
    final ProductDraft newProductDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, sourceProductType.toResourceIdentifier())
            .taxCategory(sourceTaxCategory.toResourceIdentifier())
            .state(sourceProductState.toResourceIdentifier())
            .categories(sourceCategoryResourceIdentifiers)
            .categoryOrderHints(createRandomCategoryOrderHints(sourceCategoryReferencesWithIds))
            .publish(true)
            .key(sampleProductKey)
            .build();

    CTP_SOURCE_CLIENT.products().create(newProductDraft).execute().toCompletableFuture().join();

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(response -> response.getBody().getResults())
            .toCompletableFuture()
            .join();

    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    // assertions
    assertThat(syncStatistics).hasValues(1, 1, 0, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final List<ProductProjection> targetProducts =
        CTP_TARGET_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(response -> response.getBody().getResults())
            .toCompletableFuture()
            .join();

    assertThat(targetProducts.get(0).getKey()).isEqualTo(sampleProductKey);
  }

  @Test
  void sync_withChangesOnly_ShouldUpdateProducts() {
    final ProductDraft existingProductDraft =
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            targetProductType.toReference(),
            targetTaxCategory.toReference(),
            targetProductState.toReference(),
            targetCategoryReferencesWithIds,
            createRandomCategoryOrderHintsFromResourceIdentifiers(
                targetCategoryResourceIdentifiers));
    CTP_TARGET_CLIENT
        .products()
        .create(existingProductDraft)
        .execute()
        .toCompletableFuture()
        .join();

    final ProductDraft newProductDraft =
        createProductDraft(
            PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            sourceProductType.toReference(),
            sourceTaxCategory.toReference(),
            sourceProductState.toReference(),
            sourceCategoryReferencesWithIds,
            createRandomCategoryOrderHints(sourceCategoryReferencesWithIds));
    CTP_SOURCE_CLIENT.products().create(newProductDraft).execute().toCompletableFuture().join();

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join()
            .getResults();

    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withChangesOnlyAndUnPublish_ShouldUpdateProducts() {
    final ProductDraft existingProductDraft =
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            targetProductType.toReference(),
            targetTaxCategory.toReference(),
            targetProductState.toReference(),
            targetCategoryReferencesWithIds,
            createRandomCategoryOrderHints(targetCategoryReferencesWithIds));
    CTP_TARGET_CLIENT
        .products()
        .create(existingProductDraft)
        .execute()
        .toCompletableFuture()
        .join();

    final ProductDraft newProductDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, sourceProductType.toResourceIdentifier())
            .taxCategory(sourceTaxCategory.toResourceIdentifier())
            .state(sourceProductState.toResourceIdentifier())
            .categories(sourceCategoryResourceIdentifiers)
            .categoryOrderHints(createRandomCategoryOrderHints(sourceCategoryReferencesWithIds))
            .publish(false)
            .build();

    CTP_SOURCE_CLIENT.products().create(newProductDraft).execute().toCompletableFuture().join();

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join()
            .getResults();

    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withUpdatedDraftAndPublishFlagSetToTrue_ShouldPublishProductEvenItWasPublishedBefore() {
    final ProductDraft publishedProductDraft =
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            targetProductType.toReference(),
            targetTaxCategory.toReference(),
            targetProductState.toReference(),
            targetCategoryReferencesWithIds,
            createRandomCategoryOrderHints(targetCategoryReferencesWithIds));
    CTP_TARGET_CLIENT
        .products()
        .create(publishedProductDraft)
        .execute()
        .toCompletableFuture()
        .join();

    // new product draft has a publish flag set to true and the existing product is published
    // already
    final ProductDraft newProductDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, sourceProductType.toResourceIdentifier())
            .taxCategory(sourceTaxCategory.toResourceIdentifier())
            .state(sourceProductState.toResourceIdentifier())
            .categories(sourceCategoryResourceIdentifiers)
            .categoryOrderHints(createRandomCategoryOrderHints(sourceCategoryReferencesWithIds))
            .publish(true)
            .build();

    CTP_SOURCE_CLIENT.products().create(newProductDraft).execute().toCompletableFuture().join();

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join()
            .getResults();

    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    // last action is publish
    assertThat(updateActions.get(updateActions.size() - 1))
        .isInstanceOf(ProductPublishAction.class);
  }

  @Test
  void sync_withPriceReferences_ShouldUpdateProducts() {
    final ProductDraft existingProductDraft =
        createProductDraft(
            PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH,
            targetProductType.toReference(),
            targetTaxCategory.toReference(),
            targetProductState.toReference(),
            targetCategoryReferencesWithIds,
            createRandomCategoryOrderHints(targetCategoryReferencesWithIds));

    final ProductDraft existingDraftWithPriceReferences =
        getDraftWithPriceReferences(
            existingProductDraft,
            targetPriceChannel.toResourceIdentifier(),
            CustomFieldsDraftBuilder.of()
                .type(builder -> builder.key(targetPriceCustomType.getKey()))
                .fields(FieldContainerBuilder.of().build())
                .build());

    CTP_TARGET_CLIENT
        .products()
        .create(existingDraftWithPriceReferences)
        .execute()
        .toCompletableFuture()
        .join();

    final ProductDraft newProductDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH,
                sourceProductType.toResourceIdentifier())
            .taxCategory(sourceTaxCategory.toResourceIdentifier())
            .state(sourceProductState.toResourceIdentifier())
            .categories(sourceCategoryResourceIdentifiers)
            .categoryOrderHints(createRandomCategoryOrderHints(sourceCategoryReferencesWithIds))
            .publish(false)
            .build();

    final ProductDraft newDraftWithPriceReferences =
        getDraftWithPriceReferences(
            newProductDraft,
            sourcePriceChannel.toResourceIdentifier(),
            CustomFieldsDraftBuilder.of()
                .type(builder -> builder.key(sourcePriceCustomType.getKey()))
                .fields(FieldContainerBuilder.of().build())
                .build());

    CTP_SOURCE_CLIENT
        .products()
        .create(newDraftWithPriceReferences)
        .execute()
        .toCompletableFuture()
        .join();

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join()
            .getResults();

    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withProductTypeReference_ShouldUpdateProducts() {
    // Preparation
    // Create custom options with whitelisting and action filter callback..
    final ProductSyncOptions customSyncOptions =
        ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errorCallback(exception.getMessage(), exception.getCause()))
            .warningCallback(
                (exception, oldResource, newResources) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .beforeUpdateCallback(this::beforeUpdateCallback)
            .syncFilter(SyncFilter.ofWhiteList(ATTRIBUTES))
            .build();
    final ProductSync customSync = new ProductSync(customSyncOptions);

    // Create 3 existing products in target project with keys (productKey1, productKey2 and
    // productKey3)
    final ProductDraft existingProductDraft =
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            targetProductType.toReference(),
            targetTaxCategory.toReference(),
            targetProductState.toReference(),
            targetCategoryReferencesWithIds,
            createRandomCategoryOrderHints(targetCategoryReferencesWithIds));
    CTP_TARGET_CLIENT
        .products()
        .create(existingProductDraft)
        .execute()
        .toCompletableFuture()
        .join();

    final ProductDraft existingProductDraft2 =
        createProductDraft(
            PRODUCT_KEY_2_RESOURCE_PATH,
            targetProductType.toReference(),
            targetTaxCategory.toReference(),
            targetProductState.toReference(),
            targetCategoryReferencesWithIds,
            createRandomCategoryOrderHints(targetCategoryReferencesWithIds));
    CTP_TARGET_CLIENT
        .products()
        .create(existingProductDraft2)
        .execute()
        .toCompletableFuture()
        .join();

    final ProductDraft existingProductDraft3 =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH, targetProductType.toResourceIdentifier())
            .slug(LocalizedStringBuilder.of().addValue("en", "newSlug3").build())
            .key("productKey3")
            .masterVariant(ProductVariantDraftBuilder.of().key("v3").sku("s3").build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categories(Collections.emptyList())
            .categoryOrderHints((CategoryOrderHints) null)
            .build();
    CTP_TARGET_CLIENT
        .products()
        .create(existingProductDraft3)
        .execute()
        .toCompletableFuture()
        .join();

    // Create 2 existing products in source project with keys (productKey2 and productKey3)
    final ProductDraft newProductDraft2 =
        createProductDraft(
            PRODUCT_KEY_2_RESOURCE_PATH,
            sourceProductType.toReference(),
            sourceTaxCategory.toReference(),
            sourceProductState.toReference(),
            sourceCategoryReferencesWithIds,
            createRandomCategoryOrderHints(sourceCategoryReferencesWithIds));
    final Product product2 =
        CTP_SOURCE_CLIENT
            .products()
            .create(newProductDraft2)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();
    final ProductDraft newProductDraft3 =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH, sourceProductType.toResourceIdentifier())
            .slug(LocalizedStringBuilder.of().addValue("en", "newSlug3").build())
            .key("productKey3")
            .masterVariant(ProductVariantDraftBuilder.of().key("v3").sku("s3").build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categories(Collections.emptyList())
            .categoryOrderHints((CategoryOrderHints) null)
            .build();
    final Product product3 =
        CTP_SOURCE_CLIENT
            .products()
            .create(newProductDraft3)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    // Create existing product with productKey1 in source project that has references to products
    // with keys
    // (productKey2 and productKey3).

    final ProductReference productReferenceValue1 = getProductReferenceWithId(product2.getId());
    final ProductReference productReferenceValue2 = getProductReferenceWithId(product3.getId());

    final Attribute productRefAttr =
        AttributeBuilder.of().name("product-reference").value(productReferenceValue1).build();
    final Attribute productSetRefAttr =
        getReferenceSetAttributeDraft(
            "product-reference-set", productReferenceValue1, productReferenceValue2);
    final List<Attribute> attributeDrafts = existingProductDraft.getMasterVariant().getAttributes();
    attributeDrafts.addAll(Arrays.asList(productRefAttr, productSetRefAttr));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of().key("v1").sku("s1").attributes(attributeDrafts).build();

    final ProductDraft newProductDraftWithProductReference =
        createProductDraftBuilder(
                PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, sourceProductType.toResourceIdentifier())
            .masterVariant(masterVariant)
            .taxCategory(sourceTaxCategory.toResourceIdentifier())
            .state(sourceProductState.toResourceIdentifier())
            .categories(Collections.emptyList())
            .categoryOrderHints((CategoryOrderHints) null)
            .build();
    CTP_SOURCE_CLIENT
        .products()
        .create(newProductDraftWithProductReference)
        .execute()
        .toCompletableFuture()
        .join();

    // Test
    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join()
            .getResults();
    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();
    final ProductSyncStatistics syncStatistics =
        customSync.sync(productDrafts).toCompletableFuture().join();

    // Assertion
    assertThat(syncStatistics).hasValues(3, 0, 1, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final Product targetProduct2 =
        CTP_TARGET_CLIENT
            .products()
            .withKey("productKey2")
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    final Product targetProduct3 =
        CTP_TARGET_CLIENT
            .products()
            .withKey("productKey3")
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    final ProductReference targetProductReferenceValue2 =
        getProductReferenceWithId(targetProduct2.getId());
    final ProductReference targetProductReferenceValue3 =
        getProductReferenceWithId(targetProduct3.getId());

    final Attribute targetProductRefAttr =
        AttributeBuilder.of().name("product-reference").value(targetProductReferenceValue2).build();
    final Attribute targetProductSetRefAttr =
        getReferenceSetAttributeDraft(
            "product-reference-set", targetProductReferenceValue2, targetProductReferenceValue3);

    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name(targetProductRefAttr.getName())
                .value(targetProductRefAttr.getValue())
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name(targetProductSetRefAttr.getName())
                .value(targetProductSetRefAttr.getValue())
                .staged(true)
                .build(),
            ProductPublishActionBuilder.of().build());
  }

  @Test
  void sync_withChangedAttributes_ShouldUpdateProducts() {
    // Preparation
    // Create custom options with whitelisting and action filter callback..
    final ProductSyncOptions customSyncOptions =
        ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errorCallback(exception.getMessage(), exception.getCause()))
            .warningCallback(
                (exception, oldResource, newResources) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .beforeUpdateCallback(this::beforeUpdateCallback)
            .syncFilter(SyncFilter.ofWhiteList(ATTRIBUTES))
            .build();
    final ProductSync customSync = new ProductSync(customSyncOptions);

    // Create existing products in target project with keys (productKey1)
    final ProductDraft existingProductDraft =
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            targetProductType.toReference(),
            targetTaxCategory.toReference(),
            targetProductState.toReference(),
            targetCategoryReferencesWithIds,
            createRandomCategoryOrderHints(targetCategoryReferencesWithIds));
    CTP_TARGET_CLIENT
        .products()
        .create(existingProductDraft)
        .execute()
        .toCompletableFuture()
        .join();

    // Create existing product with productKey1 in source project with changed attributes
    final ProductDraft newProductDraftWithProductReference =
        createProductDraftBuilder(
                PRODUCT_KEY_1_CHANGED_ATTRIBUTES_RESOURCE_PATH,
                sourceProductType.toResourceIdentifier())
            .taxCategory(sourceTaxCategory.toResourceIdentifier())
            .state(sourceProductState.toResourceIdentifier())
            .categories(Collections.emptyList())
            .categoryOrderHints((CategoryOrderHints) null)
            .build();
    CTP_SOURCE_CLIENT
        .products()
        .create(newProductDraftWithProductReference)
        .execute()
        .toCompletableFuture()
        .join();

    // Test
    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join()
            .getResults();
    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();
    final ProductSyncStatistics syncStatistics =
        customSync.sync(productDrafts).toCompletableFuture().join();

    // Assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final Attribute priceInfoAttrDraft =
        AttributeBuilder.of().name("priceInfo").value("100/kg").build();
    final Attribute angebotAttrDraft =
        AttributeBuilder.of().name("angebot").value("big discount").build();

    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("priceInfo")
                .value("100/kg")
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of().name("size").staged(true).build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("rinderrasse")
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("herkunft")
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("teilstueck")
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("fuetterung")
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of().name("reifung").staged(true).build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("haltbarkeit")
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("verpackung")
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("anlieferung")
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("zubereitung")
                .staged(true)
                .build(),
            ProductSetAttributeActionBuilder.of()
                .variantId(1L)
                .name("localisedText")
                .staged(true)
                .build(),
            ProductSetAttributeActionBuilder.of()
                .variantId(1L)
                .name("angebot")
                .value("big discount")
                .staged(true)
                .build(),
            ProductPublishActionBuilder.of().build());
  }

  @Test
  void sync_withEmptySetAttribute_ShouldCreateProductWithAnEmptySetAttribute() {
    // Preparation
    // Create custom options with whitelisting and action filter callback..
    final ProductSyncOptions customSyncOptions =
        ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, newResource, oldResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                ((exception, newResource, oldResource) ->
                    warningCallBackMessages.add(exception.getMessage())))
            .beforeUpdateCallback(this::beforeUpdateCallback)
            .syncFilter(SyncFilter.ofWhiteList(ATTRIBUTES))
            .build();
    final ProductSync customSync = new ProductSync(customSyncOptions);

    // Create a product that will be referenced by another product in the target project
    final ProductDraft productDraftToBeReferenced =
        ProductDraftBuilder.of()
            .productType(targetProductType.toResourceIdentifier())
            .name(LocalizedString.ofEnglish("root"))
            .slug(LocalizedString.ofEnglish("root"))
            .build();

    final Product productToBeReferenced =
        CTP_TARGET_CLIENT
            .products()
            .create(productDraftToBeReferenced)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    // Create a product "bar" that references a product on the target project
    final ProductReference productReferenceValue1 =
        getProductReferenceWithId(productToBeReferenced.getId());

    final Attribute productSetRefAttr =
        getReferenceSetAttributeDraft("product-reference-set", productReferenceValue1);

    final ProductVariantDraft variantWithProductReferences =
        ProductVariantDraftBuilder.of()
            .key("bar")
            .sku("bar")
            .attributes(singletonList(productSetRefAttr))
            .build();

    final ProductDraft existingProductDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH, targetProductType.toResourceIdentifier())
            .masterVariant(variantWithProductReferences)
            .taxCategory(targetTaxCategory.toResourceIdentifier())
            .state(targetProductState.toResourceIdentifier())
            .build();

    CTP_TARGET_CLIENT
        .products()
        .create(existingProductDraft)
        .execute()
        .toCompletableFuture()
        .join();

    // Create a product "bar" that has an empty references set on the source project (this is
    // expected to update)
    final ProductVariantDraft variantBarWithEmptyReferenceSet =
        ProductVariantDraftBuilder.of()
            .key("bar")
            .sku("bar")
            .attributes(singletonList(getReferenceSetAttributeDraft(productSetRefAttr.getName())))
            .build();

    final ProductDraft newProductDraftWithProductReference =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH, sourceProductType.toResourceIdentifier())
            .masterVariant(variantBarWithEmptyReferenceSet)
            .taxCategory(sourceTaxCategory.toResourceIdentifier())
            .state(sourceProductState.toResourceIdentifier())
            .build();
    CTP_SOURCE_CLIENT
        .products()
        .create(newProductDraftWithProductReference)
        .execute()
        .toCompletableFuture()
        .join();

    // Create a product "foo" that has an empty references set on the source project (this is
    // expected to create)
    final ProductVariantDraft variantFooWithEmptyReferenceSet =
        ProductVariantDraftBuilder.of()
            .key("foo")
            .sku("foo")
            .attributes(singletonList(getReferenceSetAttributeDraft(productSetRefAttr.getName())))
            .build();

    final ProductDraft sourceProductDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, sourceProductType.toResourceIdentifier())
            .taxCategory(sourceTaxCategory.toResourceIdentifier())
            .state(sourceProductState.toResourceIdentifier())
            .categories(sourceCategoryResourceIdentifiers)
            .categoryOrderHints(createRandomCategoryOrderHints(sourceCategoryReferencesWithIds))
            .masterVariant(variantFooWithEmptyReferenceSet)
            .build();
    CTP_SOURCE_CLIENT.products().create(sourceProductDraft).execute().toCompletableFuture().join();

    // Test
    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(response -> response.getBody().getResults())
            .toCompletableFuture()
            .join();
    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();
    final ProductSyncStatistics syncStatistics =
        customSync.sync(productDrafts).toCompletableFuture().join();

    // Assertion
    assertThat(syncStatistics).hasValues(2, 1, 1, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(updateActions)
        .containsExactly(
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name(productSetRefAttr.getName())
                .value(Collections.emptyList())
                .staged(true)
                .build(),
            ProductPublishActionBuilder.of().build());

    final Product targetProduct =
        CTP_TARGET_CLIENT
            .products()
            .withKey(sourceProductDraft.getKey())
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    final Attribute targetAttribute =
        targetProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .getAttribute(productSetRefAttr.getName());
    assertThat(targetAttribute).isNotNull();
    assertThat(targetAttribute.getValue()).isEqualTo(Collections.emptyList());
  }
}
