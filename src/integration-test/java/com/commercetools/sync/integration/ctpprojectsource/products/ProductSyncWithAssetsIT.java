package com.commercetools.sync.integration.ctpprojectsource.products;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductProjectionType;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.AddAsset;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetName;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.products.commands.updateactions.RemoveAsset;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomField;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomType;
import io.sphere.sdk.products.queries.ProductProjectionByKeyGet;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.types.Type;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ITUtils.BOOLEAN_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.LOCALISED_STRING_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.assertAssetsAreEqual;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createAssetDraft;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createAssetsCustomType;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createVariantDraft;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.buildProductQuery;
import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.replaceProductsReferenceIdsWithKeys;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductSyncWithAssetsIT {
    private static final String ASSETS_CUSTOM_TYPE_KEY = "assetsCustomTypeKey";
    private static ProductType sourceProductType;
    private static ProductType targetProductType;

    private static Type targetAssetCustomType;
    private static Type sourceAssetCustomType;

    private ProductSync productSync;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<UpdateAction<Product>> updateActions;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Delete all product related test data from target and source projects. Then creates for both CTP projects product
     * types and asset custom types.
     */
    @BeforeClass
    public static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        deleteProductSyncTestData(CTP_SOURCE_CLIENT);

        targetProductType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
        sourceProductType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_SOURCE_CLIENT);


        targetAssetCustomType = createAssetsCustomType(ASSETS_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            "assetsCustomTypeName", CTP_TARGET_CLIENT);
        sourceAssetCustomType = createAssetsCustomType(ASSETS_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            "assetsCustomTypeName", CTP_SOURCE_CLIENT);
    }

    /**
     * Deletes Products from the source and target CTP projects, clears the callback collections then it instantiates a
     * new {@link ProductSync} instance.
     */
    @Before
    public void setupTest() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);
        deleteAllProducts(CTP_SOURCE_CLIENT);
        productSync = new ProductSync(buildSyncOptions());
    }

    private void clearSyncTestCollections() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
        updateActions = new ArrayList<>();
    }

    private ProductSyncOptions buildSyncOptions() {
        return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                        .errorCallback(this::errorCallback)
                                        .warningCallback(warningCallBackMessages::add)
                                        .beforeUpdateCallback(this::beforeUpdateCallback)
                                        .build();
    }

    private void errorCallback(@Nonnull final String errorMessage, @Nullable final Throwable exception) {
        errorCallBackMessages.add(errorMessage);
        errorCallBackExceptions.add(exception);
    }

    private List<UpdateAction<Product>> beforeUpdateCallback(@Nonnull final List<UpdateAction<Product>> updateActions,
                                                             @Nonnull final ProductDraft newProductDraft,
                                                             @Nonnull final Product oldProduct) {
        this.updateActions.addAll(updateActions);
        return updateActions;
    }

    @AfterClass
    public static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        deleteProductSyncTestData(CTP_SOURCE_CLIENT);
    }

    @Test
    public void sync_withNewProductWithAssets_shouldCreateProduct() {
        final List<AssetDraft> assetDraftsToCreateOnExistingProduct = asList(
            createAssetDraft("1", ofEnglish("1"), sourceAssetCustomType.getId()),
            createAssetDraft("2", ofEnglish("2"), sourceAssetCustomType.getId()),
            createAssetDraft("3", ofEnglish("3"), sourceAssetCustomType.getId()));

        final ProductDraft draftToCreateOnTargetProject = ProductDraftBuilder
            .of(targetProductType.toReference(), ofEnglish("draftName"), ofEnglish("existingProductInTarget"),
                ProductVariantDraftBuilder.of().key("k1").sku("sku1").build())
            .key("existingProductInTarget")
            .build();
        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(draftToCreateOnTargetProject)).toCompletableFuture().join();

        final ProductDraft draftToCreateOnSourceProject = ProductDraftBuilder
            .of(sourceProductType.toReference(), ofEnglish("draftName"), ofEnglish("existingProductInSource"),
                createVariantDraft("masterVariant", assetDraftsToCreateOnExistingProduct))
            .key("existingProductInSource")
            .build();
        CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(draftToCreateOnSourceProject)).toCompletableFuture().join();

        final List<Product> products = CTP_SOURCE_CLIENT.execute(buildProductQuery())
                                                        .toCompletableFuture().join().getResults();

        final List<ProductDraft> productDrafts = replaceProductsReferenceIdsWithKeys(products);

        final ProductSyncStatistics syncStatistics =  productSync.sync(productDrafts).toCompletableFuture().join();

        // Assert results of sync
        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();

        // Assert that the product was created with the assets.
        final ProductProjection productProjection = CTP_TARGET_CLIENT
            .execute(ProductProjectionByKeyGet.of("existingProductInSource", ProductProjectionType.STAGED))
            .toCompletableFuture().join();

        assertThat(productProjection).isNotNull();
        final List<Asset> createdAssets = productProjection.getMasterVariant().getAssets();
        assertAssetsAreEqual(createdAssets, assetDraftsToCreateOnExistingProduct);
    }

    @Test
    public void sync_withMatchingProductWithAssetChanges_shouldUpdateProduct() {
        final Map<String, JsonNode> customFieldsJsonMap = new HashMap<>();
        customFieldsJsonMap.put(BOOLEAN_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.booleanNode(true));

        final List<AssetDraft> assetDraftsToCreateOnExistingProductOnTargetProject = asList(
            createAssetDraft("1", ofEnglish("1"), targetAssetCustomType.getId()),
            createAssetDraft("2", ofEnglish("2"), targetAssetCustomType.getId()),
            createAssetDraft("3", ofEnglish("3"), targetAssetCustomType.getId()));

        final List<AssetDraft> assetDraftsToCreateOnExistingProductOnSourceProject = asList(
            createAssetDraft("4", ofEnglish("4"), sourceAssetCustomType.getId()),
            createAssetDraft("3", ofEnglish("3"), sourceAssetCustomType.getId(), customFieldsJsonMap),
            createAssetDraft("2", ofEnglish("newName")));

        final String productKey = "same-product";
        final ProductDraft draftToCreateOnTargetProject = ProductDraftBuilder
            .of(targetProductType.toReference(), ofEnglish("draftName"), ofEnglish("existingProductInTarget"),
                createVariantDraft("masterVariant", assetDraftsToCreateOnExistingProductOnTargetProject))
            .key(productKey)
            .build();
        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(draftToCreateOnTargetProject)).toCompletableFuture().join();

        final ProductDraft draftToCreateOnSourceProject = ProductDraftBuilder
            .of(sourceProductType.toReference(), draftToCreateOnTargetProject.getName(),
                draftToCreateOnTargetProject.getSlug(), createVariantDraft("masterVariant",
                    assetDraftsToCreateOnExistingProductOnSourceProject))
            .key(productKey)
            .build();
        CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(draftToCreateOnSourceProject)).toCompletableFuture().join();

        final List<Product> products = CTP_SOURCE_CLIENT.execute(buildProductQuery())
                                                        .toCompletableFuture().join().getResults();

        final List<ProductDraft> productDrafts = replaceProductsReferenceIdsWithKeys(products);

        final ProductSyncStatistics syncStatistics =  productSync.sync(productDrafts).toCompletableFuture().join();

        // Assert results of sync
        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();

        // Assert that assets got updated correctly
        final ProductProjection productProjection = CTP_TARGET_CLIENT
            .execute(ProductProjectionByKeyGet.of(productKey, ProductProjectionType.STAGED))
            .toCompletableFuture().join();

        assertThat(productProjection).isNotNull();
        final List<Asset> createdAssets = productProjection.getMasterVariant().getAssets();
        assertAssetsAreEqual(createdAssets, assetDraftsToCreateOnExistingProductOnSourceProject);

        // Assert update actions
        final Map<String, String> assetsKeyToIdMap = createdAssets.stream().collect(toMap(Asset::getKey, Asset::getId));

        assertThat(updateActions).containsExactly(
            RemoveAsset.ofVariantIdWithKey(1, "1", true),
            ChangeAssetName.ofAssetKeyAndVariantId(1, "2", ofEnglish("newName"), true),
            SetAssetCustomType.ofVariantIdAndAssetKey(1, "2", null, true),
            SetAssetCustomField.ofVariantIdAndAssetKey(1, "3", BOOLEAN_CUSTOM_FIELD_NAME,
                customFieldsJsonMap.get(BOOLEAN_CUSTOM_FIELD_NAME), true),
            SetAssetCustomField.ofVariantIdAndAssetKey(1, "3", LOCALISED_STRING_CUSTOM_FIELD_NAME,
                null, true),
            ChangeAssetOrder.ofVariantId(1, asList(assetsKeyToIdMap.get("3"), assetsKeyToIdMap.get("2")), true),
            AddAsset.ofVariantId(1, createAssetDraft("4", ofEnglish("4"),
                targetAssetCustomType.getId()))
                    .withStaged(true).withPosition(0)
        );
    }
}
