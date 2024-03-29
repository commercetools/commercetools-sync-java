package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ITUtils.*;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ensureProductType;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductAddAssetActionBuilder;
import com.commercetools.api.models.product.ProductAddVariantActionBuilder;
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
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.Type;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncWithAssetsIT {
  private static final String ASSETS_CUSTOM_TYPE_KEY = "assetsCustomTypeKey";

  private static ProductType productType;
  private static Type assetsCustomType;
  private Product product;
  private ProductSync productSync;
  private List<AssetDraft> assetDraftsToCreateOnExistingProduct;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<ProductUpdateAction> updateActionsFromSync;

  /**
   * Delete all product related test data from the target project. Then creates for the target CTP
   * project a product type and an asset custom type.
   */
  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    assetsCustomType =
        ensureAssetsCustomType(
            ASSETS_CUSTOM_TYPE_KEY, Locale.ENGLISH, "assetsCustomTypeName", CTP_TARGET_CLIENT);

    productType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
  }

  /**
   * Deletes Products and Types from the target CTP project, then it populates target CTP project
   * with product test data.
   */
  @BeforeEach
  void setupTest() {
    clearSyncTestCollections();
    deleteAllProducts(CTP_TARGET_CLIENT);
    productSync = new ProductSync(buildSyncOptions());

    assetDraftsToCreateOnExistingProduct =
        asList(
            createAssetDraft("1", ofEnglish("1"), assetsCustomType.getId()),
            createAssetDraft("2", ofEnglish("2"), assetsCustomType.getId()),
            createAssetDraft("3", ofEnglish("3"), assetsCustomType.getId()));

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("draftName"))
            .slug(ofEnglish("existingSlug"))
            .masterVariant(createVariantDraft("v1", assetDraftsToCreateOnExistingProduct, null))
            .key("existingProduct")
            .build();

    product =
        CTP_TARGET_CLIENT
            .products()
            .create(productDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();
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
  void sync_withNewProductWithAssets_shouldCreateProduct() {
    final List<AssetDraft> assetDrafts =
        singletonList(createAssetDraftWithKey("4", ofEnglish("4"), ASSETS_CUSTOM_TYPE_KEY));

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .name(ofEnglish("draftName"))
            .slug(ofEnglish("slug"))
            .masterVariant(createVariantDraft("masterVariant", assetDrafts, null))
            .key("draftKey")
            .build();

    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 1, 0, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(updateActionsFromSync).isEmpty();

    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(productDraft.getKey())
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(productProjection).isNotNull();
    final List<Asset> createdAssets = productProjection.getMasterVariant().getAssets();
    assertAssetsAreEqual(createdAssets, assetDrafts);
  }

  @Test
  void
      sync_withNewProductWithAssetsWithDuplicateKeys_shouldNotCreateProductAndTriggerErrorCallback() {
    final List<AssetDraft> assetDrafts =
        asList(
            createAssetDraftWithKey("4", ofEnglish("4"), ASSETS_CUSTOM_TYPE_KEY),
            createAssetDraftWithKey("4", ofEnglish("duplicate asset"), ASSETS_CUSTOM_TYPE_KEY));

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .name(ofEnglish("draftName"))
            .slug(ofEnglish("slug"))
            .masterVariant(createVariantDraft("masterVariant", assetDrafts, null))
            .key("draftKey")
            .build();

    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // Assert results of sync
    assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(updateActionsFromSync).isEmpty();
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .contains(
            "The key '4' was used by multiple assets in product variant" + " 'masterVariant'.");
    assertThat(errorCallBackExceptions).hasSize(1);

    assertThat(errorCallBackExceptions.get(0)).hasCauseExactlyInstanceOf(BadRequestException.class);
    assertThat(errorCallBackExceptions.get(0).getMessage())
        .contains("The key '4' was used by multiple assets in product variant 'masterVariant'.");

    // Assert that the product wasn't created.
    final List<ProductProjection> productProjections =
        CTP_TARGET_CLIENT
            .productProjections()
            .get()
            .withWhere("key=:key")
            .withPredicateVar("key", productDraft.getKey())
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductProjectionPagedQueryResponse::getResults)
            .toCompletableFuture()
            .join();

    assertThat(productProjections).hasSize(0);
  }

  @Test
  void sync_withMatchingProductWithAssetChanges_shouldUpdateProduct() {

    final Map<String, Object> customFieldsJsonMap = new HashMap<>();
    customFieldsJsonMap.put(BOOLEAN_CUSTOM_FIELD_NAME, true);

    // new asset drafts with different kind of asset actions (change order, add asset, remove asset,
    // change asset name, set asset custom fields, change
    final List<AssetDraft> assetDrafts =
        asList(
            createAssetDraftWithKey("4", ofEnglish("4"), ASSETS_CUSTOM_TYPE_KEY),
            createAssetDraftWithKey(
                "3",
                ofEnglish("3"),
                ASSETS_CUSTOM_TYPE_KEY,
                FieldContainerBuilder.of().values(customFieldsJsonMap).build()),
            createAssetDraft("2", ofEnglish("new name")));

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .name(ofEnglish("draftName"))
            .slug(ofEnglish("existingSlug"))
            .masterVariant(createVariantDraft("v1", assetDrafts, null))
            .key(product.getKey())
            .build();

    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // Assert results of sync
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(updateActionsFromSync).isNotEmpty();

    final Map<String, String> assetsKeyToIdMap =
        product.getMasterData().getStaged().getMasterVariant().getAssets().stream()
            .collect(toMap(Asset::getKey, Asset::getId));

    assertThat(updateActionsFromSync)
        .containsExactly(
            ProductRemoveAssetActionBuilder.of().variantId(1L).assetKey("1").staged(true).build(),
            ProductChangeAssetNameActionBuilder.of()
                .variantId(1L)
                .assetKey("2")
                .name(ofEnglish("new name"))
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
                .asset(createAssetDraft("4", ofEnglish("4"), assetsCustomType.getId()))
                .staged(true)
                .position(0)
                .build());

    // Assert that assets got updated correctly
    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(productDraft.getKey())
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(productProjection).isNotNull();
    assertAssetsAreEqual(productProjection.getMasterVariant().getAssets(), assetDrafts);
  }

  @Test
  void sync_withMatchingProductWithNewVariantWithAssets_shouldUpdateAddAssetsToNewVariant() {

    final Map<String, Object> customFieldsJsonMap = new HashMap<>();
    customFieldsJsonMap.put(BOOLEAN_CUSTOM_FIELD_NAME, true);

    // new asset drafts with different kind of asset actions (change order, add asset, remove asset,
    // change asset name, set asset custom fields, change
    final List<AssetDraft> assetDrafts =
        asList(
            createAssetDraftWithKey("4", ofEnglish("4"), ASSETS_CUSTOM_TYPE_KEY),
            createAssetDraftWithKey(
                "3",
                ofEnglish("3"),
                ASSETS_CUSTOM_TYPE_KEY,
                FieldContainerBuilder.of().values(customFieldsJsonMap).build()),
            createAssetDraft("2", ofEnglish("new name")));

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .name(ofEnglish("draftName"))
            .slug(ofEnglish("existingSlug"))
            .masterVariant(createVariantDraft("v1", assetDrafts, null))
            .variants(createVariantDraft("v2", assetDrafts, null))
            .key(product.getKey())
            .build();

    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // Assert results of sync
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(updateActionsFromSync).isNotEmpty();

    final Map<String, String> assetsKeyToIdMap =
        product.getMasterData().getStaged().getMasterVariant().getAssets().stream()
            .collect(toMap(Asset::getKey, Asset::getId));

    assertThat(updateActionsFromSync)
        .containsExactly(
            ProductAddVariantActionBuilder.of()
                .sku("v2")
                .staged(true)
                .key("v2")
                .assets(
                    List.of(
                        createAssetDraft("4", ofEnglish("4"), assetsCustomType.getId()),
                        createAssetDraft(
                            "3",
                            ofEnglish("3"),
                            assetsCustomType.getId(),
                            FieldContainerBuilder.of().values(customFieldsJsonMap).build()),
                        createAssetDraft("2", ofEnglish("new name"))))
                .build(),
            ProductRemoveAssetActionBuilder.of().variantId(1L).assetKey("1").staged(true).build(),
            ProductChangeAssetNameActionBuilder.of()
                .variantId(1L)
                .assetKey("2")
                .name(ofEnglish("new name"))
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
                .asset(createAssetDraft("4", ofEnglish("4"), assetsCustomType.getId()))
                .staged(true)
                .position(0)
                .build());

    // Assert that assets got updated correctly
    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(productDraft.getKey())
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(productProjection).isNotNull();
    assertAssetsAreEqual(productProjection.getMasterVariant().getAssets(), assetDrafts);
  }

  @Test
  void
      sync_withMatchingProductWithDuplicateAssets_shouldFailToUpdateProductAndTriggerErrorCallback() {
    final List<AssetDraft> assetDrafts =
        asList(
            createAssetDraftWithKey("4", ofEnglish("4"), ASSETS_CUSTOM_TYPE_KEY),
            createAssetDraftWithKey("4", ofEnglish("4"), ASSETS_CUSTOM_TYPE_KEY),
            createAssetDraftWithKey("2", ofEnglish("2"), ASSETS_CUSTOM_TYPE_KEY));

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .name(ofEnglish("draftName"))
            .slug(ofEnglish("existingSlug"))
            .masterVariant(createVariantDraft("v1", assetDrafts, null))
            .key(product.getKey())
            .build();

    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // Assert results of sync
    assertThat(syncStatistics).hasValues(1, 0, 0, 0, 0);
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .matches(
            "Failed to build update actions for the assets of the product "
                + "variant with the sku 'v1'. Reason: .*DuplicateKeyException: Supplied asset drafts "
                + "have duplicate keys. Asset keys are expected to be unique inside their container \\(a product variant "
                + "or a category\\).");
    assertThat(errorCallBackExceptions).hasSize(1);
    assertThat(errorCallBackExceptions.get(0))
        .isExactlyInstanceOf(BuildUpdateActionException.class);
    assertThat(errorCallBackExceptions.get(0).getMessage())
        .contains(
            "Supplied asset drafts have duplicate "
                + "keys. Asset keys are expected to be unique inside their container (a product variant or a category).");
    assertThat(errorCallBackExceptions.get(0).getCause())
        .isExactlyInstanceOf(DuplicateKeyException.class);
    assertThat(updateActionsFromSync).isEmpty();

    // Assert that existing assets haven't changed.
    final ProductProjection productProjection =
        CTP_TARGET_CLIENT
            .productProjections()
            .withKey(productDraft.getKey())
            .get()
            .withStaged(true)
            .execute()
            .thenApply(
                productProjectionApiHttpResponse -> productProjectionApiHttpResponse.getBody())
            .toCompletableFuture()
            .join();

    assertThat(productProjection).isNotNull();

    assertAssetsAreEqual(
        productProjection.getMasterVariant().getAssets(), assetDraftsToCreateOnExistingProduct);
  }
}
