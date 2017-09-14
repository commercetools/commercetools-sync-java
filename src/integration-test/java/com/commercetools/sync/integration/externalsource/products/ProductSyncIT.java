package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.PRODUCT_KEY_1_CHANGED_RESOURCE_PATH;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.PRODUCT_KEY_2_RESOURCE_PATH;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ProductSyncIT {
    private static ProductType productType;
    private static List<Category> categories;
    private ProductSyncOptions syncOptions;
    private Product product;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Delete all product related test data from target project. Then creates custom types for target CTP project
     * categories.
     */
    @BeforeClass
    public static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_TARGET_CLIENT);
        categories = createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    }

    /**
     * Deletes Products and Types from target CTP projects, then it populates target CTP project with product test
     * data.
     */
    @Before
    public void setupTest() {

        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
        deleteAllProducts(CTP_TARGET_CLIENT);

        syncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                               .publish(true)
                                               .revertStagedChanges(true)
                                               .updateStaged(false)
                                               .setErrorCallBack(
                                                   (errorMessage, exception) -> {
                                                       errorCallBackMessages
                                                           .add(errorMessage);
                                                       errorCallBackExceptions
                                                           .add(exception);
                                                   })
                                               .setWarningCallBack(warningMessage ->
                                                   warningCallBackMessages
                                                       .add(warningMessage))
                                               .build();

        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH, productType,
            categories, createRandomCategoryOrderHints(categories));
        product = CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft))
                                   .toCompletableFuture().join();
    }

    @AfterClass
    public static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
    }

    @Test
    public void sync_withNewProduct_shouldCreateProduct() {
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType).build();


        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = productSync.sync(singletonList(productDraft))
                                                                .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 1, 0, 0));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withNewProductWithExistingSlug_shouldNotCreateProduct() {
        @SuppressWarnings("ConstantConditions")
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType)
            .slug(product.getMasterData().getCurrent().getSlug())
            .build();


        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = productSync.sync(singletonList(productDraft))
                                                                .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 0, 1));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ErrorResponseException.class);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).contains(format("A duplicate value '\\\"%s\\\"' exists for field"
            + " 'slug.en' on", product.getMasterData().getCurrent().getSlug().get(Locale.ENGLISH)));
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withEqualProduct_shouldNotUpdateProduct() {
        @SuppressWarnings("ConstantConditions")
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH,
            productType, categories, product.getMasterData().getCurrent().getCategoryOrderHints());

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = productSync.sync(singletonList(productDraft))
                                                                .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 0, 0));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withChangedProduct_shouldUpdateProduct() {
        @SuppressWarnings("ConstantConditions")
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            productType, categories, product.getMasterData().getCurrent().getCategoryOrderHints());

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = productSync.sync(singletonList(productDraft))
                                                                .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 1, 0));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test// TODO handle all retry cases
    public void sync_withChangedProductButConcurrentModificationException_shouldRetryAndUpdateProduct() {
        // Mock sphere client to return ConcurrentModification on the first update request.
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(ProductUpdateCommand.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new ConcurrentModificationException()))
            .thenCallRealMethod();

        final ProductSyncOptions spyOptions = ProductSyncOptionsBuilder.of(spyClient)
                                                                .setErrorCallBack(
                                                                    (errorMessage, exception) -> {
                                                                        errorCallBackMessages
                                                                            .add(errorMessage);
                                                                        errorCallBackExceptions
                                                                            .add(exception);
                                                                    })
                                                                .setWarningCallBack(warningMessage ->
                                                                    warningCallBackMessages
                                                                        .add(warningMessage))
                                                                .build();

        final ProductSync spyProductSync = new ProductSync(spyOptions);

        @SuppressWarnings("ConstantConditions")
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            productType, categories, product.getMasterData().getCurrent().getCategoryOrderHints());

        final ProductSyncStatistics syncStatistics = spyProductSync.sync(singletonList(productDraft))
                                                                .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 1, 0));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withMultipleBatchSyncing_ShouldSync() {
        //_-----_-----_-----_-----_-----_PREPARE EXISTING PRODUCTS (productKey1, productKey2, productKey3)------
        //_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----
        @SuppressWarnings("ConstantConditions")

        final ProductDraft key2Draft = createProductDraft(PRODUCT_KEY_2_RESOURCE_PATH,
            productType, categories, product.getMasterData().getCurrent().getCategoryOrderHints());

        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(key2Draft))
                         .toCompletableFuture()
                         .join();

        final ProductDraft key3Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey3")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug3"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(key3Draft))
                         .toCompletableFuture()
                         .join();

        //_-----_-----_-----_-----_-----_PREPARE BATCHES FROM EXTERNAL SOURCE-----_-----_-----_-----_-----_-----
        //_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            productType, categories, product.getMasterData().getCurrent().getCategoryOrderHints());

        final List<ProductDraft> batch1 = new ArrayList<>();
        batch1.add(productDraft);

        final ProductDraft key4Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey4")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug4"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final List<ProductDraft> batch2 = new ArrayList<>();
        batch2.add(key4Draft);

        final ProductDraft key3DraftNewSlug = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey3")
            .slug(LocalizedString.of(Locale.ENGLISH, "newSlug"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final List<ProductDraft> batch3 = new ArrayList<>();
        batch3.add(key3DraftNewSlug);


        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = productSync.sync(batch1)
                                                                .thenCompose(result -> productSync.sync(batch2))
                                                                .thenCompose(result -> productSync.sync(batch3))
                                                                .toCompletableFuture()
                                                                .join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 3, 1, 2, 0));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }


    @Test
    public void sync_withSingleBatchSyncing_ShouldSync() {
        @SuppressWarnings("ConstantConditions")
        //PREPARE BATCHES FROM EXTERNAL SOURCE
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            productType, categories, product.getMasterData().getCurrent().getCategoryOrderHints());

        final ProductDraft key3Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey3")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug3"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft key4Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey4")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug4"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft key5Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey5")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug5"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft key6Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey6")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug6"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final List<ProductDraft> batch = new ArrayList<>();
        batch.add(productDraft);
        batch.add(key3Draft);
        batch.add(key4Draft);
        batch.add(key5Draft);
        batch.add(key6Draft);

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = productSync.sync(batch)
                                                                .toCompletableFuture()
                                                                .join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 5, 4, 1, 0));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withSameSlugInSingleBatch_ShouldNotSyncIt() {
        @SuppressWarnings("ConstantConditions")
        //PREPARE BATCHES FROM EXTERNAL SOURCE
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            productType, categories, product.getMasterData().getCurrent().getCategoryOrderHints());

        final ProductDraft key3Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey3")
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft key4Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey4")
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft key5Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey5")
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft key6Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey6")
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final List<ProductDraft> batch = new ArrayList<>();
        batch.add(productDraft);
        batch.add(key3Draft);
        batch.add(key4Draft);
        batch.add(key5Draft);
        batch.add(key6Draft);

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = productSync.sync(batch)
                                                                .toCompletableFuture()
                                                                .join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 5, 1, 1, 3));
        assertThat(errorCallBackExceptions).hasSize(3);
        errorCallBackExceptions
            .forEach(exception -> assertThat(exception).isExactlyInstanceOf(ErrorResponseException.class));
        assertThat(errorCallBackMessages).hasSize(3);
        errorCallBackMessages.forEach(errorMessage -> assertThat(errorMessage)
                .contains(format("A duplicate value '\\\"%s\\\"' exists for field 'slug.en' on",
                        key3Draft.getSlug().get(Locale.ENGLISH))));
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withADraftsWithBlankKeysInBatch_ShouldNotSyncItAndTriggerErrorCallBack() {
        @SuppressWarnings("ConstantConditions")
        //PREPARE BATCHES FROM EXTERNAL SOURCE
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            productType, categories, product.getMasterData().getCurrent().getCategoryOrderHints());

        // Draft with null key
        final ProductDraft key3Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key(null)
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        // Draft with empty key
        final ProductDraft key4Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("")
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final List<ProductDraft> batch = new ArrayList<>();
        batch.add(productDraft);
        batch.add(key3Draft);
        batch.add(key4Draft);

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = productSync.sync(batch)
                                                                .toCompletableFuture()
                                                                .join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 3, 0, 1, 2));
        assertThat(errorCallBackExceptions).hasSize(2);
        assertThat(errorCallBackMessages).hasSize(2);
        assertThat(errorCallBackMessages.get(0))
            .isEqualToIgnoringCase(format("ProductDraft with name: %s doesn't have a key.", key3Draft.getName()));
        assertThat(errorCallBackMessages.get(1))
            .isEqualToIgnoringCase(format("ProductDraft with name: %s doesn't have a key.", key4Draft.getName()));
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withANullDraftInBatch_ShouldNotSyncItAndTriggerErrorCallBack() {
        @SuppressWarnings("ConstantConditions")
        //PREPARE BATCHES FROM EXTERNAL SOURCE
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            productType, categories, product.getMasterData().getCurrent().getCategoryOrderHints());

        final List<ProductDraft> batch = new ArrayList<>();
        batch.add(productDraft);
        batch.add(null);

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = productSync.sync(batch)
                                                                .toCompletableFuture()
                                                                .join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 2, 0, 1, 1));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualToIgnoringCase("ProductDraft is null.");
        assertThat(warningCallBackMessages).isEmpty();
    }


    @Test
    public void sync_withSameDraftsWithChangesInBatch_ShouldRetryUpdateBecauseOfConcurrentModificationExceptions() {
        @SuppressWarnings("ConstantConditions")
        //PREPARE BATCHES FROM EXTERNAL SOURCE
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            productType, categories, product.getMasterData().getCurrent().getCategoryOrderHints());

        // Draft with same key
        final ProductDraft draftWithSameKey = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key(productDraft.getKey())
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final List<ProductDraft> batch = new ArrayList<>();
        batch.add(productDraft);
        batch.add(draftWithSameKey);

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = productSync.sync(batch)
                                                                .toCompletableFuture()
                                                                .join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 2, 0, 2, 0));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }
}
