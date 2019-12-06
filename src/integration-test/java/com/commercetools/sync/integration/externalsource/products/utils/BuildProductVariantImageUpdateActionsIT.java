package com.commercetools.sync.integration.externalsource.products.utils;

import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Image;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.AddExternalImage;
import io.sphere.sdk.products.commands.updateactions.MoveImageToPosition;
import io.sphere.sdk.products.commands.updateactions.RemoveImage;
import io.sphere.sdk.products.queries.ProductByKeyGet;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.commercetools.sync.commons.utils.CollectionUtils.emptyIfNull;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;

class BuildProductVariantImageUpdateActionsIT {
    private static final String DRAFTS_ROOT = "com/commercetools/sync/integration/externalsource/products/"
        + "utils/imageUpdateActions/";
    private static final String PRODUCT_WITH_IMAGES_1_2_3 = DRAFTS_ROOT + "draftWithMVImages-1-2-3.json";
    private static final String PRODUCT_WITH_IMAGES_3_2_1 = DRAFTS_ROOT + "draftWithMVImages-3-2-1.json";
    private static final String PRODUCT_WITH_IMAGES_7_1_2 = DRAFTS_ROOT + "draftWithMVImages-7-1-2.json";
    private static final String PRODUCT_WITH_NULL_IMAGES = DRAFTS_ROOT + "draftWithMVNullImages.json";
    private static final String PRODUCT_WITH_EMPTY_IMAGES = DRAFTS_ROOT + "draftWithMVEmptyImages.json";

    private static ProductType productType;
    private ProductSync productSync;
    private List<String> errorCallBackMessages;
    private List<UpdateAction<Product>> updateActionsBuiltBySync;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Delete all product related test data from target and source projects. Then creates product types in both
     * CTP projects.
     */
    @BeforeAll
    static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    }

    @AfterAll
    static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
    }

    /**
     * Create old product from the draft.
     */
    @BeforeEach
    void setUp() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);

        final BiConsumer<String, Throwable> errorCallBack = (errorMessage, exception) -> {
            errorCallBackMessages.add(errorMessage);
            errorCallBackExceptions.add(exception);
        };

        final Consumer<String> warningCallBack = warningMessage -> warningCallBackMessages.add(warningMessage);

        final TriFunction<List<UpdateAction<Product>>, ProductDraft, Product, List<UpdateAction<Product>>>
            updateActionsCollector = (updateActions, newDraft, oldProduct) -> {
                updateActionsBuiltBySync.addAll(updateActions);
                return updateActions;
            };


        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                               .errorCallback(errorCallBack)
                                                                               .warningCallback(warningCallBack)
                                                                               .beforeUpdateCallback(
                                                                                   updateActionsCollector)
                                                                               .build();
        productSync = new ProductSync(productSyncOptions);
    }

    private void clearSyncTestCollections() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
        updateActionsBuiltBySync = new ArrayList<>();
    }

    @Test
    void sync_WithNullImageLists_ShouldNotBuildImageUpdateActionAndSyncCorrectly() {
        assertProductSyncShouldNotBuildImageUpdateActions(PRODUCT_WITH_NULL_IMAGES, PRODUCT_WITH_NULL_IMAGES);

    }

    private void assertProductSyncShouldNotBuildImageUpdateActions(@Nonnull final String oldProductDraftJsonResource,
                                                                   @Nonnull final String newProductDraftJsonResource) {
        // Prepare existing product before sync.
        final ProductDraft oldProductDraft =
            ProductDraftBuilder.of(readObjectFromResource(oldProductDraftJsonResource, ProductDraft.class))
                               .productType(productType.toReference())
                               .build();
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(oldProductDraft)));

        // Prepare new product to feed into sync.
        final ProductDraft newProduct =
            ProductDraftBuilder.of(readObjectFromResource(newProductDraftJsonResource, ProductDraft.class))
                               .productType(ProductType.referenceOfId(productType.getKey()))
                               .build();

        // Sync
        executeBlocking(productSync.sync(Collections.singletonList(newProduct)));

        // Assert Update Actions are built correctly.
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        updateActionsBuiltBySync.forEach(updateAction -> {
            assertThat(updateAction).isNotExactlyInstanceOf(MoveImageToPosition.class);
            assertThat(updateAction).isNotExactlyInstanceOf(AddExternalImage.class);
            assertThat(updateAction).isNotExactlyInstanceOf(RemoveImage.class);
        });

        // Assert product state after sync.
        final Product productAfterSync = executeBlocking(
            CTP_TARGET_CLIENT.execute(ProductByKeyGet.of(oldProductDraft.getKey())));
        assertImagesAreSyncedCorrectly(newProduct, productAfterSync);
    }

    @Test
    void sync_WithEmptyImageLists_ShouldNotBuildImageUpdateActionAndSyncCorrectly() {
        assertProductSyncShouldNotBuildImageUpdateActions(PRODUCT_WITH_EMPTY_IMAGES, PRODUCT_WITH_EMPTY_IMAGES);
    }

    @Test
    void sync_WithNullNewImageList_ShouldBuildRemoveImageUpdateActionsAndSyncCorrectly() {
        // Prepare existing product before sync.
        final ProductDraft oldProductDraft =
            ProductDraftBuilder.of(readObjectFromResource(PRODUCT_WITH_IMAGES_1_2_3, ProductDraft.class))
                               .productType(productType.toReference())
                               .build();
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(oldProductDraft)));

        // Prepare new product to feed into sync.
        final ProductDraft newProduct =
            ProductDraftBuilder.of(readObjectFromResource(PRODUCT_WITH_NULL_IMAGES, ProductDraft.class))
                               .productType(ProductType.referenceOfId(productType.getKey()))
                               .build();

        // Sync
        executeBlocking(productSync.sync(Collections.singletonList(newProduct)));

        // Assert Update Actions are built correctly.
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(updateActionsBuiltBySync.stream()
                                           .filter(updateAction -> updateAction instanceof RemoveImage)
                                           .count()).isEqualTo(3);

        // Assert product state after sync.
        final Product productAfterSync = executeBlocking(
            CTP_TARGET_CLIENT.execute(ProductByKeyGet.of(oldProductDraft.getKey())));
        assertImagesAreSyncedCorrectly(newProduct, productAfterSync);
    }

    @Test
    void sync_WithEmptyNewImageList_ShouldBuildRemoveImageUpdateActionsAndSyncCorrectly() {
        assertProductSyncShouldBuildRemoveImageUpdateActions(PRODUCT_WITH_IMAGES_1_2_3,
            PRODUCT_WITH_EMPTY_IMAGES, RemoveImage.class);

    }

    private void assertProductSyncShouldBuildRemoveImageUpdateActions(
        @Nonnull final String oldProductDraftJsonResource,
        @Nonnull final String newProductDraftJsonResource,
        @Nonnull final Class<?> updateActionType) {
        // Prepare existing product before sync.
        final ProductDraft oldProductDraft =
            ProductDraftBuilder.of(readObjectFromResource(oldProductDraftJsonResource, ProductDraft.class))
                               .productType(productType.toReference())
                               .build();
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(oldProductDraft)));

        // Prepare new product to feed into sync.
        final ProductDraft newProduct =
            ProductDraftBuilder.of(readObjectFromResource(newProductDraftJsonResource, ProductDraft.class))
                               .productType(ProductType.referenceOfId(productType.getKey()))
                               .build();

        // Sync
        executeBlocking(productSync.sync(Collections.singletonList(newProduct)));

        // Assert Update Actions are built correctly.
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(updateActionsBuiltBySync.stream()
                                           .filter(updateActionType::isInstance)
                                           .count()).isEqualTo(3);

        // Assert product state after sync.
        final Product productAfterSync = executeBlocking(
            CTP_TARGET_CLIENT.execute(ProductByKeyGet.of(oldProductDraft.getKey())));
        assertImagesAreSyncedCorrectly(newProduct, productAfterSync);
    }

    @Test
    void sync_WithNullOldImageList_ShouldBuildAddExternalImageUpdateActionsAndSyncCorrectly() {
        assertProductSyncShouldBuildRemoveImageUpdateActions(PRODUCT_WITH_NULL_IMAGES,
            PRODUCT_WITH_IMAGES_1_2_3, AddExternalImage.class);
    }

    @Test
    void sync_WithEmpyOldImageList_ShouldBuildAddExternalImageUpdateActionsAndSyncCorrectly() {
        assertProductSyncShouldBuildRemoveImageUpdateActions(PRODUCT_WITH_EMPTY_IMAGES,
            PRODUCT_WITH_IMAGES_1_2_3, AddExternalImage.class);
    }

    @Test
    void sync_WithIdenticalNonNullImageLists_ShouldNotBuildImageUpdateActions() {
        assertProductSyncShouldNotBuildImageUpdateActions(PRODUCT_WITH_IMAGES_1_2_3, PRODUCT_WITH_IMAGES_1_2_3);
    }

    @Test
    void sync_WithDifferentImageLists_ShouldBuildImageUpdateActionsAndSyncCorrectly() {
        // Prepare existing product before sync.
        final ProductDraft oldProductDraft =
            ProductDraftBuilder.of(readObjectFromResource(PRODUCT_WITH_IMAGES_1_2_3, ProductDraft.class))
                               .productType(productType.toReference())
                               .build();
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(oldProductDraft)));

        // Prepare new product to feed into sync.
        final ProductDraft newProduct =
            ProductDraftBuilder.of(readObjectFromResource(PRODUCT_WITH_IMAGES_7_1_2, ProductDraft.class))
                               .productType(ProductType.referenceOfId(productType.getKey()))
                               .build();

        // Sync
        executeBlocking(productSync.sync(Collections.singletonList(newProduct)));

        // Assert Update Actions are built correctly
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(updateActionsBuiltBySync
            .stream().anyMatch(updateAction -> updateAction instanceof MoveImageToPosition))
            .isTrue();
        assertThat(updateActionsBuiltBySync
            .stream().anyMatch(updateAction -> updateAction instanceof AddExternalImage))
            .isTrue();
        assertThat(updateActionsBuiltBySync
            .stream().anyMatch(updateAction -> updateAction instanceof RemoveImage))
            .isTrue();

        // Assert product state after sync.
        final Product productAfterSync = executeBlocking(
            CTP_TARGET_CLIENT.execute(ProductByKeyGet.of(oldProductDraft.getKey())));
        assertImagesAreSyncedCorrectly(newProduct, productAfterSync);
    }

    @Test
    void sync_WithDifferentOrderedImageLists_ShouldBuildImageUpdateActionsAndSyncCorrectly() {
        // Prepare existing product before sync.
        final ProductDraft oldProductDraft =
            ProductDraftBuilder.of(readObjectFromResource(PRODUCT_WITH_IMAGES_1_2_3, ProductDraft.class))
                               .productType(productType.toReference())
                               .build();
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(oldProductDraft)));

        // Prepare new product to feed into sync.
        final ProductDraft newProduct =
            ProductDraftBuilder.of(readObjectFromResource(PRODUCT_WITH_IMAGES_3_2_1, ProductDraft.class))
                               .productType(ProductType.referenceOfId(productType.getKey()))
                               .build();

        // Sync
        executeBlocking(productSync.sync(Collections.singletonList(newProduct)));

        // Assert Update Actions are built correctly
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(updateActionsBuiltBySync
            .stream().filter(updateAction -> updateAction instanceof MoveImageToPosition).count())
            .isEqualTo(2);
        assertThat(updateActionsBuiltBySync
            .stream().noneMatch(updateAction -> updateAction instanceof AddExternalImage))
            .isTrue();
        assertThat(updateActionsBuiltBySync
            .stream().noneMatch(updateAction -> updateAction instanceof RemoveImage))
            .isTrue();

        // Assert product state after sync.
        final Product productAfterSync = executeBlocking(
            CTP_TARGET_CLIENT.execute(ProductByKeyGet.of(oldProductDraft.getKey())));
        assertImagesAreSyncedCorrectly(newProduct, productAfterSync);
    }

    private static void assertImagesAreSyncedCorrectly(@Nonnull final ProductDraft newProduct,
                                                       @Nonnull final Product productAfterSync) {
        final List<Image> oldImagesAfterSync =
            productAfterSync.getMasterData().getStaged().getMasterVariant().getImages();
        List<Image> newImages = emptyIfNull(newProduct.getMasterVariant().getImages());
        assertThat(newImages).isEqualTo(oldImagesAfterSync);
    }
}
