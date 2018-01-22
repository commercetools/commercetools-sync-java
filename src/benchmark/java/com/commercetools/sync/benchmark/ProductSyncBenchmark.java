package com.commercetools.sync.benchmark;


import com.commercetools.sync.commons.utils.SyncSolutionInfo;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.PRODUCT_SYNC;
import static com.commercetools.sync.benchmark.BenchmarkUtils.THRESHOLD;
import static com.commercetools.sync.benchmark.BenchmarkUtils.calculateDiff;
import static com.commercetools.sync.benchmark.BenchmarkUtils.saveNewResult;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductSyncBenchmark {
    private static ProductType productType;
    private ProductSyncOptions syncOptions;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    @BeforeClass
    public static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_TARGET_CLIENT);
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    }

    @Before
    public void setupTest() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);
        syncOptions = buildSyncOptions();
    }

    private void clearSyncTestCollections() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
    }

    private ProductSyncOptions buildSyncOptions() {
        final BiConsumer<String, Throwable> errorCallBack = (errorMessage, exception) -> {
            errorCallBackMessages.add(errorMessage);
            errorCallBackExceptions.add(exception);
        };
        final Consumer<String> warningCallBack = warningMessage -> warningCallBackMessages.add(warningMessage);

        return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                        .errorCallback(errorCallBack)
                                        .warningCallback(warningCallBack)
                                        .build();
    }

    @AfterClass
    public static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
    }

    @Test
    public void sync_NewProducts_ShouldCreateProducts() throws IOException {
        final int numberOfProducts = 100;
        final List<ProductDraft> productDrafts = buildProductDrafts(numberOfProducts);

        // Sync drafts
        final ProductSync productSync = new ProductSync(syncOptions);

        final long beforeSyncTime = System.currentTimeMillis();
        final ProductSyncStatistics syncStatistics = productSync.sync(productDrafts).toCompletableFuture().join();
        final long totalTime = System.currentTimeMillis() - beforeSyncTime;

        assertThat(syncStatistics).hasValues(numberOfProducts, numberOfProducts, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();


        final double diff = calculateDiff(SyncSolutionInfo.LIB_VERSION, PRODUCT_SYNC, CREATES_ONLY, totalTime);
        assertThat(diff).isLessThanOrEqualTo(THRESHOLD)
                        .withFailMessage(format("Diff of benchmark '%e' is longer than expected"
                            + " threshold of '%e'.", diff, THRESHOLD));

        saveNewResult(SyncSolutionInfo.LIB_VERSION, PRODUCT_SYNC, CREATES_ONLY, totalTime);
    }

    @Nonnull
    private List<ProductDraft> buildProductDrafts(final int numberOfProducts) {
        final List<ProductDraft> productDrafts = new ArrayList<>();
        final Reference<ProductType> draftsProductType = ProductType
            .referenceOfId(ProductSyncBenchmark.productType.getKey());
        for (int i = 0; i < numberOfProducts; i++) {
            final ProductVariantDraft masterVariantDraft = ProductVariantDraftBuilder.of()
                                                                                     .key("masterVariantKey_" + i)
                                                                                     .build();
            final ProductDraft productDraft = ProductDraftBuilder
                .of(draftsProductType, ofEnglish("name_" + i), ofEnglish("slug_" + i), masterVariantDraft)
                .key("productKey_" + i)
                .build();
            productDrafts.add(productDraft);
        }
        return productDrafts;
    }
}
