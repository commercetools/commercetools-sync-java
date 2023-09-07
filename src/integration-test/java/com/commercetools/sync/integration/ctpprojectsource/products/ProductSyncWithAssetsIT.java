package com.commercetools.sync.integration.ctpprojectsource.products;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ITUtils.*;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ensureProductType;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.product.ProductAddAssetActionBuilder;
import com.commercetools.api.models.product.ProductChangeAssetNameActionBuilder;
import com.commercetools.api.models.product.ProductChangeAssetOrderActionBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionPagedQueryResponse;
import com.commercetools.api.models.product.ProductRemoveAssetActionBuilder;
import com.commercetools.api.models.product.ProductSetAssetCustomFieldActionBuilder;
import com.commercetools.api.models.product.ProductSetAssetCustomTypeActionBuilder;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.Type;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.products.utils.ProductTransformUtils;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncWithAssetsIT {
  private static final String ASSETS_CUSTOM_TYPE_KEY = "assetsCustomTypeKey";
  private static ProductType sourceProductType;
  private static ProductType targetProductType;

  private static Type targetAssetCustomType;
  private static Type sourceAssetCustomType;

  private ProductSync productSync;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<ProductUpdateAction> updateActions;
  private List<Throwable> errorCallBackExceptions;
  private ReferenceIdToKeyCache referenceIdToKeyCache;

  /**
   * Delete all product related test data from target and source projects. Then creates for both CTP
   * projects product types and asset custom types.
   */
  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    deleteProductSyncTestData(CTP_SOURCE_CLIENT);

    targetProductType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    sourceProductType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_SOURCE_CLIENT);

    targetAssetCustomType =
        ensureAssetsCustomType(
            ASSETS_CUSTOM_TYPE_KEY, Locale.ENGLISH, "assetsCustomTypeName", CTP_TARGET_CLIENT);
    sourceAssetCustomType =
        ensureAssetsCustomType(
            ASSETS_CUSTOM_TYPE_KEY, Locale.ENGLISH, "assetsCustomTypeName", CTP_SOURCE_CLIENT);
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
    productSync = new ProductSync(buildSyncOptions());
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
            (exception, oldResource, newResource) ->
                warningCallBackMessages.add(exception.getMessage()))
        .beforeUpdateCallback(
            (updateActions1, newProductDraft, oldProduct) -> beforeUpdateCallback(updateActions1))
        .build();
  }

  private void errorCallback(
      @Nonnull final String errorMessage, @Nullable final Throwable exception) {
    errorCallBackMessages.add(errorMessage);
    errorCallBackExceptions.add(exception);
  }

  private List<ProductUpdateAction> beforeUpdateCallback(
      @Nonnull final List<ProductUpdateAction> updateActions) {
    this.updateActions.addAll(updateActions);
    return updateActions;
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    deleteProductSyncTestData(CTP_SOURCE_CLIENT);
  }

  @Test
  void sync_withNewProductWithAssets_shouldCreateProduct() {
    final List<AssetDraft> assetDraftsToCreateOnExistingProduct =
        asList(
            createAssetDraft("1", ofEnglish("1"), sourceAssetCustomType.getId()),
            createAssetDraft("2", ofEnglish("2"), sourceAssetCustomType.getId()),
            createAssetDraft("3", ofEnglish("3"), sourceAssetCustomType.getId()));

    final ProductDraft draftToCreateOnTargetProject =
        ProductDraftBuilder.of()
            .productType(targetProductType.toResourceIdentifier())
            .name(ofEnglish("draftName"))
            .slug(ofEnglish("existingProductInTarget"))
            .masterVariant(ProductVariantDraftBuilder.of().key("k1").sku("sku1").build())
            .key("existingProductInTarget")
            .build();
    CTP_TARGET_CLIENT
        .products()
        .create(draftToCreateOnTargetProject)
        .execute()
        .toCompletableFuture()
        .join();

    final ProductDraft draftToCreateOnSourceProject =
        ProductDraftBuilder.of()
            .productType(sourceProductType.toResourceIdentifier())
            .name(ofEnglish("draftName"))
            .slug(ofEnglish("existingProductInSource"))
            .masterVariant(
                createVariantDraft("masterVariant", assetDraftsToCreateOnExistingProduct, null))
            .key("existingProductInSource")
            .build();
    CTP_SOURCE_CLIENT
        .products()
        .create(draftToCreateOnSourceProject)
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
            .thenApply(ProductProjectionPagedQueryResponse::getResults)
            .toCompletableFuture()
            .join();

    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    // Assert results of sync
    assertThat(syncStatistics).hasValues(1, 1, 0, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    // Assert that the product was created with the assets.
    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey("existingProductInSource")
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(productProjection).isNotNull();
    final List<Asset> createdAssets = productProjection.getMasterVariant().getAssets();
    assertAssetsAreEqual(createdAssets, assetDraftsToCreateOnExistingProduct);
  }

  @Test
  void sync_withMatchingProductWithAssetChanges_shouldUpdateProduct() {
    final Map<String, Object> customFieldsJsonMap = new HashMap<>();
    customFieldsJsonMap.put(BOOLEAN_CUSTOM_FIELD_NAME, true);

    final List<AssetDraft> assetDraftsToCreateOnExistingProductOnTargetProject =
        asList(
            createAssetDraft("1", ofEnglish("1"), targetAssetCustomType.getId()),
            createAssetDraft("2", ofEnglish("2"), targetAssetCustomType.getId()),
            createAssetDraft("3", ofEnglish("3"), targetAssetCustomType.getId()));

    final List<AssetDraft> assetDraftsToCreateOnExistingProductOnSourceProject =
        asList(
            createAssetDraft("4", ofEnglish("4"), sourceAssetCustomType.getId()),
            createAssetDraft(
                "3",
                ofEnglish("3"),
                sourceAssetCustomType.getId(),
                FieldContainerBuilder.of().values(customFieldsJsonMap).build()),
            createAssetDraft("2", ofEnglish("newName")));

    final String productKey = "same-product";
    final ProductDraft draftToCreateOnTargetProject =
        ProductDraftBuilder.of()
            .productType(targetProductType.toResourceIdentifier())
            .name(ofEnglish("draftName"))
            .slug(ofEnglish("existingProductInTarget"))
            .masterVariant(
                createVariantDraft(
                    "masterVariant", assetDraftsToCreateOnExistingProductOnTargetProject, null))
            .key(productKey)
            .build();
    CTP_TARGET_CLIENT.products().create(draftToCreateOnTargetProject).executeBlocking();

    final ProductDraft draftToCreateOnSourceProject =
        ProductDraftBuilder.of()
            .productType(sourceProductType.toResourceIdentifier())
            .name(draftToCreateOnTargetProject.getName())
            .slug(draftToCreateOnTargetProject.getSlug())
            .masterVariant(
                createVariantDraft(
                    "masterVariant", assetDraftsToCreateOnExistingProductOnSourceProject, null))
            .key(productKey)
            .build();
    CTP_SOURCE_CLIENT.products().create(draftToCreateOnSourceProject).executeBlocking();

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductProjectionPagedQueryResponse::getResults)
            .join();

    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    // Assert results of sync
    assertThat(syncStatistics).hasValues(1, 0, 1, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    // Assert that assets got updated correctly
    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(productKey)
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    assertThat(productProjection).isNotNull();
    final List<Asset> createdAssets = productProjection.getMasterVariant().getAssets();
    assertAssetsAreEqual(createdAssets, assetDraftsToCreateOnExistingProductOnSourceProject);

    // Assert update actions
    final Map<String, String> assetsKeyToIdMap =
        createdAssets.stream().collect(toMap(Asset::getKey, Asset::getId));

    assertThat(updateActions)
        .containsExactly(
            ProductRemoveAssetActionBuilder.of().variantId(1L).assetKey("1").staged(true).build(),
            ProductChangeAssetNameActionBuilder.of()
                .variantId(1L)
                .assetKey("2")
                .name(ofEnglish("newName"))
                .staged(true)
                .build(),
            ProductSetAssetCustomTypeActionBuilder.of()
                .variantId(1L)
                .assetKey("2")
                .staged(true)
                .build(),
            ProductSetAssetCustomFieldActionBuilder.of()
                .variantId(1L)
                .assetKey("3")
                .name(BOOLEAN_CUSTOM_FIELD_NAME)
                .value(customFieldsJsonMap.get(BOOLEAN_CUSTOM_FIELD_NAME))
                .staged(true)
                .build(),
            ProductSetAssetCustomFieldActionBuilder.of()
                .variantId(1L)
                .assetKey("3")
                .name(LOCALISED_STRING_CUSTOM_FIELD_NAME)
                .staged(true)
                .build(),
            ProductChangeAssetOrderActionBuilder.of()
                .variantId(1L)
                .assetOrder(List.of(assetsKeyToIdMap.get("3"), assetsKeyToIdMap.get("2")))
                .staged(true)
                .build(),
            ProductAddAssetActionBuilder.of()
                .variantId(1L)
                .asset(createAssetDraft("4", ofEnglish("4"), targetAssetCustomType.getId()))
                .staged(true)
                .position(0)
                .build());
  }
}
