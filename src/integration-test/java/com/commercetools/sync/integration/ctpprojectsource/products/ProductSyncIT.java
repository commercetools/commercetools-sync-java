package com.commercetools.sync.integration.ctpprojectsource.products;

import com.commercetools.sync.commons.utils.SyncUtils;
import com.commercetools.sync.integration.commons.utils.SphereClientUtils;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static java.lang.String.format;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ProductSyncIT {
    private static ProductType productType;
    private static List<Category> categories;
    private ProductSync productSync;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Delete all product related test data from target and source projects. Then creates custom types for both
     * CTP projects categories.
     */
    @BeforeClass
    public static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        deleteProductSyncTestData(CTP_SOURCE_CLIENT);

        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_SOURCE_CLIENT);

        createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
        categories = createCategories(CTP_SOURCE_CLIENT, getCategoryDrafts(null, 2));

        createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_SOURCE_CLIENT);
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
        deleteAllProducts(CTP_SOURCE_CLIENT);

        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
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
        productSync = new ProductSync(syncOptions);


    }

    @AfterClass
    public static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        deleteProductSyncTestData(CTP_SOURCE_CLIENT);
    }

    @Test
    public void sync_withChangesOnly_ShouldUpdateCategories() {
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH, productType,
            categories, createRandomCategoryOrderHints(categories));
        CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();

        final List<Product> products = CTP_SOURCE_CLIENT
            .execute(ProductQuery.of().withLimit(SphereClientUtils.QUERY_MAX_LIMIT))
            .toCompletableFuture().join().getResults();

        final List<ProductDraft> productDrafts = products.stream()
                                                         .map(SyncUtils::getDraftBuilderFromStagedProduct)
                                                         .map(ProductDraftBuilder::build).collect(Collectors.toList());


        final ProductSyncStatistics syncStatistics =  productSync.sync(productDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and  %d products"
                + " failed to sync).", 1, 0, 0, 0));

        Assertions.assertThat(errorCallBackMessages).isEmpty();
        Assertions.assertThat(errorCallBackExceptions).isEmpty();
        Assertions.assertThat(warningCallBackMessages).isEmpty();
    }
}
