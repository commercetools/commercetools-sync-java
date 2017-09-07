package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.buildProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductSyncIT {
    private static final String PRODUCT_KEY_1_CHANGED_RESOURCE_PATH = "product-key-1-changed.json";
    private static final String PRODUCT_KEY_2_RESOURCE_PATH = "product-key-2.json";
    private static final String PRODUCT_TYPE_RESOURCE_PATH = "product-type.json";

    private static ProductType productType;
    private static List<Category> categories;
    private ProductSyncOptions syncOptions;
    private Product product;

    /**
     * Delete all categories and types from target project. Then create custom types for target CTP project categories.
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
     * Initializes environment for integration test of product synchronization against CT platform.
     *
     * <p>It first removes up all related resources. Then creates required product type, categories, products and
     * associates products to categories.
     */
    @Before
    public void setUp() {
        deleteAllProducts(CTP_TARGET_CLIENT);

        syncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                        .publish(true)
                                        .revertStagedChanges(true)
                                        .updateStaged(false)
                                        .build();

        final ProductDraft productDraft = buildProductDraft(PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH, productType,
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
    }

    @Test
    public void sync_withEqualProduct_shouldNotUpdateProduct() {
        @SuppressWarnings("ConstantConditions")
        final ProductDraft productDraft = buildProductDraft(PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH,
            productType, categories, product.getMasterData().getCurrent().getCategoryOrderHints());

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = productSync.sync(singletonList(productDraft))
                                                                .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 0, 0));
    }

    @Test
    public void sync_withChangedProduct_shouldUpdateProduct() {
        @SuppressWarnings("ConstantConditions")
        final ProductDraft productDraft = buildProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            productType, categories, product.getMasterData().getCurrent().getCategoryOrderHints());

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = productSync.sync(singletonList(productDraft))
                                                                .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 1, 0));
    }
}
