package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductProjectionType;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.AddAsset;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetName;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.products.commands.updateactions.RemoveAsset;
import io.sphere.sdk.products.commands.updateactions.SetPrices;
import io.sphere.sdk.products.queries.ProductProjectionByKeyGet;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.types.Type;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ITUtils.BOOLEAN_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.assertAssetsAreEqual;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createAssetDraft;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createAssetsCustomType;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createVariantDraft;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.producttypes.ProductType.referenceOfId;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductSyncWithAssetsIT {
    private static final String ASSETS_CUSTOM_TYPE_KEY = "assetsCustomTypeKey";

    private static ProductType productType;
    private static Type assetsCustomType;
    private ProductSyncOptions syncOptions;
    private Product product;
    private List<AssetDraft> assetDraftsToCreateOnExistingProduct;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;
    private List<UpdateAction<Product>> updateActionsFromSync;

    /**
     * Delete all product related test data from the target project. Then creates for the target CTP project a product
     * type and an asset custom type.
     */
    @BeforeClass
    public static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        assetsCustomType = createAssetsCustomType(ASSETS_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            "assetsCustomTypeName", CTP_TARGET_CLIENT);

        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    }

    /**
     * Deletes Products and Types from the target CTP project, then it populates target CTP project with product test
     * data.
     */
    @Before
    public void setupTest() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);
        syncOptions = buildSyncOptions();


        assetDraftsToCreateOnExistingProduct = asList(
            createAssetDraft("1", ofEnglish("1"), assetsCustomType.getId()),
            createAssetDraft("2", ofEnglish("2"), assetsCustomType.getId()),
            createAssetDraft("3", ofEnglish("3"), assetsCustomType.getId()));

        final ProductDraft productDraft = ProductDraftBuilder
            .of(productType.toReference(), ofEnglish("draftName"), ofEnglish("existingSlug"),
                createVariantDraft("v1", assetDraftsToCreateOnExistingProduct))
            .key("existingProduct")
            .build();

        product = executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)));
    }

    private void clearSyncTestCollections() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
        updateActionsFromSync = new ArrayList<>();
    }

    private ProductSyncOptions buildSyncOptions() {
        final BiConsumer<String, Throwable> errorCallBack = (errorMessage, exception) -> {
            errorCallBackMessages.add(errorMessage);
            errorCallBackExceptions.add(exception);
        };
        final Consumer<String> warningCallBack = warningMessage -> warningCallBackMessages.add(warningMessage);

        final TriFunction<List<UpdateAction<Product>>, ProductDraft, Product, List<UpdateAction<Product>>>
            actionsCallBack = (updateActions, newDraft, oldProduct) -> {
            updateActionsFromSync.addAll(updateActions);
            return updateActions;
        };

        return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                        .errorCallback(errorCallBack)
                                        .warningCallback(warningCallBack)
                                        .beforeUpdateCallback(actionsCallBack)
                                        .build();
    }

    @AfterClass
    public static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
    }

    @Test
    public void sync_withNewProductWithAssets_shouldCreateProduct() {
        final List<AssetDraft> assetDrafts =
            singletonList(createAssetDraft("4", ofEnglish("4"), ASSETS_CUSTOM_TYPE_KEY));

        final ProductDraft productDraft = ProductDraftBuilder
            .of(referenceOfId(productType.getKey()), ofEnglish("draftName"), ofEnglish("slug"),
                createVariantDraft("masterVariant", assetDrafts))
            .key("draftKey")
            .build();

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            executeBlocking(productSync.sync(singletonList(productDraft)));

        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(updateActionsFromSync).isEmpty();

        final ProductProjection productProjection = CTP_TARGET_CLIENT
            .execute(ProductProjectionByKeyGet.of(productDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture().join();

        assertThat(productProjection).isNotNull();
        final List<Asset> createdAssets = productProjection.getMasterVariant().getAssets();
        assertAssetsAreEqual(createdAssets, assetDrafts);
    }

    @Test
    public void sync_withNewProductWithAssetsWithDuplicateKeys_shouldNotCreateProductAndTriggerErrorCallback() {
        final List<AssetDraft> assetDrafts = asList(
            createAssetDraft("4", ofEnglish("4"), ASSETS_CUSTOM_TYPE_KEY),
            createAssetDraft("4", ofEnglish("duplicate asset"), ASSETS_CUSTOM_TYPE_KEY));

        final ProductDraft productDraft = ProductDraftBuilder
            .of(referenceOfId(productType.getKey()), ofEnglish("draftName"), ofEnglish("slug"),
                createVariantDraft("masterVariant", assetDrafts))
            .key("draftKey")
            .build();

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            executeBlocking(productSync.sync(singletonList(productDraft)));

        // Assert results of sync
        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(updateActionsFromSync).isEmpty();
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).contains("The key '4' was used by multiple assets in product variant"
            + " 'masterVariant'.");
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ErrorResponseException.class);
        assertThat(errorCallBackExceptions.get(0).getMessage()).contains("The key '4' was used by multiple assets in"
            + " product variant 'masterVariant'.");


        // Assert that the product wasn't created.
        final ProductProjection productProjection = CTP_TARGET_CLIENT
            .execute(ProductProjectionByKeyGet.of(productDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture().join();

        assertThat(productProjection).isNull();
    }

    @Test
    public void sync_withMatchingProductWithAssetChanges_shouldUpdateProduct() {

        final Map<String, JsonNode> customFieldsJsonMap = new HashMap<>();
        customFieldsJsonMap.put(BOOLEAN_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.booleanNode(true));

        // new asset drafts with different kind of asset actions (change order, add asset, remove asset, change asset
        // name, set asset custom fields, change
        final List<AssetDraft> assetDrafts = asList(
            createAssetDraft("4", ofEnglish("4"), ASSETS_CUSTOM_TYPE_KEY),
            createAssetDraft("3", ofEnglish("3"), ASSETS_CUSTOM_TYPE_KEY, customFieldsJsonMap),
            createAssetDraft("2", ofEnglish("new name")));


        final ProductDraft productDraft = ProductDraftBuilder
            .of(referenceOfId(productType.getKey()), ofEnglish("draftName"), ofEnglish("existingSlug"),
                createVariantDraft("v1", assetDrafts))
            .key(product.getKey())
            .build();

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            executeBlocking(productSync.sync(singletonList(productDraft)));

        // Assert results of sync
        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(updateActionsFromSync).isNotEmpty();

        final Map<String, String> assetsKeyToIdMap = product.getMasterData()
                                                            .getStaged()
                                                            .getMasterVariant()
                                                            .getAssets()
                                                            .stream().collect(toMap(Asset::getKey, Asset::getId));

        assertThat(updateActionsFromSync).containsExactly(
            SetPrices.of(1, emptyList()),
            RemoveAsset.ofVariantIdWithKey(1, "1", true),
            ChangeAssetName.ofAssetKeyAndVariantId(1, "3", ofEnglish("newName"), true),
            ChangeAssetOrder.ofVariantId(1, asList(assetsKeyToIdMap.get("3"), assetsKeyToIdMap.get("2")), true),
            AddAsset.ofVariantId(1, createAssetDraft("4", ofEnglish("4"), assetsCustomType.getId()))
                    .withStaged(true).withPosition(0)
        );

        // Assert that assets got updated correctly
        final ProductProjection productProjection = CTP_TARGET_CLIENT
            .execute(ProductProjectionByKeyGet.of(productDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture().join();

        assertThat(productProjection).isNotNull();
        assertAssetsAreEqual(productProjection.getMasterVariant().getAssets(), assetDrafts);
    }

    @Test
    public void sync_withMatchingProductWithDuplicateAssets_shouldFailToUpdateProductAndTriggerErrorCallback() {
        final List<AssetDraft> assetDrafts = asList(
            createAssetDraft("4", ofEnglish("4"), ASSETS_CUSTOM_TYPE_KEY),
            createAssetDraft("4", ofEnglish("4"), ASSETS_CUSTOM_TYPE_KEY),
            createAssetDraft("2", ofEnglish("2"), ASSETS_CUSTOM_TYPE_KEY));


        final ProductDraft productDraft = ProductDraftBuilder
            .of(referenceOfId(productType.getKey()), ofEnglish("draftName"), ofEnglish("existingSlug"),
                createVariantDraft("v1", assetDrafts))
            .key(product.getKey())
            .build();

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            executeBlocking(productSync.sync(singletonList(productDraft)));

        // Assert results of sync
        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).matches("Failed to build update actions for the assets of the product "
            + "variant with the sku 'v1'. Reason: .*BuildUpdateActionException: Supplied asset drafts "
            + "have duplicate keys. Asset keys are expected to be unique inside their container \\(a product variant "
            + "or a category\\).");
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(BuildUpdateActionException.class);
        assertThat(errorCallBackExceptions.get(0).getMessage()).contains("Supplied asset drafts have duplicate "
            + "keys. Asset keys are expected to be unique inside their container (a product variant or a category).");
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(IllegalStateException.class);
        assertThat(updateActionsFromSync).containsExactly(SetPrices.of(1, emptyList()));


        // Assert that existing assets haven't changed.
        final ProductProjection productProjection = CTP_TARGET_CLIENT
            .execute(ProductProjectionByKeyGet.of(productDraft.getKey(), ProductProjectionType.STAGED))
            .toCompletableFuture().join();

        assertThat(productProjection).isNotNull();

        assertAssetsAreEqual(productProjection.getMasterVariant().getAssets(), assetDraftsToCreateOnExistingProduct);
    }
}
