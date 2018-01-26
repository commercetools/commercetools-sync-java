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
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_AND_UPDATES;
import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST;
import static com.commercetools.sync.benchmark.BenchmarkUtils.PRODUCT_SYNC;
import static com.commercetools.sync.benchmark.BenchmarkUtils.THRESHOLD;
import static com.commercetools.sync.benchmark.BenchmarkUtils.UPDATES_ONLY;
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
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.products.ProductProjectionType.STAGED;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
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
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, ENGLISH,
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
        final List<ProductDraft> productDrafts = buildProductDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);

        // Sync drafts
        final ProductSync productSync = new ProductSync(syncOptions);

        final long beforeSyncTime = System.currentTimeMillis();
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(productDrafts));
        final long totalTime = System.currentTimeMillis() - beforeSyncTime;

        final double diff = calculateDiff(SyncSolutionInfo.LIB_VERSION, PRODUCT_SYNC, CREATES_ONLY, totalTime);
        assertThat(diff)
            .withFailMessage(format("Diff of benchmark '%e' is longer than expected threshold of '%e'.",
                diff, THRESHOLD))
            .isLessThanOrEqualTo(THRESHOLD);

        // Assert actual state of CTP project (number of updated products)
        assertThat(CTP_TARGET_CLIENT.execute(ProductProjectionQuery.ofStaged()
                                                                   .withPredicates(QueryPredicate.of("version = \"2\"")))
                                    .thenApply(PagedQueryResult::getTotal)
                                    .thenApply(Long::intValue)
                                    .toCompletableFuture())
            .withFailMessage("Wrong total number of existing products with version \"2\" on CTP project")
            .isCompletedWithValue(0);

        // Assert actual state of CTP project (total number of existing products)
        assertThat(CTP_TARGET_CLIENT.execute(ProductProjectionQuery.ofStaged())
                                    .thenApply(PagedQueryResult::getTotal)
                                    .thenApply(Long::intValue)
                                    .toCompletableFuture())
            .withFailMessage("Wrong total number of existing products on CTP project")
            .isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);


        assertThat(syncStatistics).hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, NUMBER_OF_RESOURCE_UNDER_TEST, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();

        saveNewResult(SyncSolutionInfo.LIB_VERSION, PRODUCT_SYNC, CREATES_ONLY, totalTime);
    }

    @Test
    public void sync_ExistingProducts_ShouldUpdateProducts() throws IOException {
        final List<ProductDraft> productDrafts = buildProductDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);
        // Create drafts to target project with different slugs
        CompletableFuture.allOf(productDrafts.stream()
                                             .map(ProductDraftBuilder::of)
                                             .map(builder -> builder.slug(
                                                 ofEnglish(builder.getSlug().get(ENGLISH) + "_old")))
                                             .map(builder -> builder.productType(productType.toReference()))
                                             .map(ProductDraftBuilder::build)
                                             .map(draft -> CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(draft)))
                                             .map(CompletionStage::toCompletableFuture)
                                             .toArray(CompletableFuture[]::new))
                         .join();

        // Sync new drafts
        final ProductSync productSync = new ProductSync(syncOptions);

        final long beforeSyncTime = System.currentTimeMillis();
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(productDrafts));
        final long totalTime = System.currentTimeMillis() - beforeSyncTime;


        // Calculate time taken for benchmark and assert it lies within threshold
        final double diff = calculateDiff(SyncSolutionInfo.LIB_VERSION, PRODUCT_SYNC, CREATES_ONLY, totalTime);
        assertThat(diff)
            .withFailMessage(format("Diff of benchmark '%e' is longer than expected threshold of '%e'.", diff,
                THRESHOLD))
            .isLessThanOrEqualTo(THRESHOLD);

        // Assert actual state of CTP project (number of updated products)
        assertThat(CTP_TARGET_CLIENT.execute(ProductProjectionQuery.ofStaged()
                                                        .withPredicates(QueryPredicate.of("version = \"2\"")))
                         .thenApply(PagedQueryResult::getTotal)
                         .thenApply(Long::intValue)
                         .toCompletableFuture())
            .isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);

        // Assert actual state of CTP project (total number of existing products)
        assertThat(CTP_TARGET_CLIENT.execute(ProductProjectionQuery.ofStaged())
                                    .thenApply(PagedQueryResult::getTotal)
                                    .thenApply(Long::intValue)
                                    .toCompletableFuture())
            .isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);

        // Assert statistics
        assertThat(syncStatistics).hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, 0, NUMBER_OF_RESOURCE_UNDER_TEST, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();

        saveNewResult(SyncSolutionInfo.LIB_VERSION, PRODUCT_SYNC, UPDATES_ONLY, totalTime);
    }

    @Test
    public void sync_WithSomeExistingProducts_ShouldSyncProducts() throws IOException {
        final List<ProductDraft> productDrafts = buildProductDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);
        final int halfNumberOfDrafts = productDrafts.size() / 2;
        final List<ProductDraft> firstHalf = productDrafts.subList(0, halfNumberOfDrafts);

        // Create first half of drafts to target project with different slugs
        CompletableFuture.allOf(firstHalf.stream()
                                             .map(ProductDraftBuilder::of)
                                             .map(builder -> builder.slug(
                                                 ofEnglish(builder.getSlug().get(ENGLISH) + "_old")))
                                             .map(builder -> builder.productType(productType.toReference()))
                                             .map(ProductDraftBuilder::build)
                                             .map(draft -> CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(draft)))
                                             .map(CompletionStage::toCompletableFuture)
                                             .toArray(CompletableFuture[]::new))
                         .join();

        // Sync new drafts
        final ProductSync productSync = new ProductSync(syncOptions);

        final long beforeSyncTime = System.currentTimeMillis();
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(productDrafts));
        final long totalTime = System.currentTimeMillis() - beforeSyncTime;


        // Calculate time taken for benchmark and assert it lies within threshold
        final double diff = calculateDiff(SyncSolutionInfo.LIB_VERSION, PRODUCT_SYNC, CREATES_ONLY, totalTime);
        assertThat(diff)
            .withFailMessage(format("Diff of benchmark '%e' is longer than expected threshold of '%e'.", diff,
                THRESHOLD))
            .isLessThanOrEqualTo(THRESHOLD);

        // Assert actual state of CTP project (number of updated products)
        assertThat(CTP_TARGET_CLIENT.execute(ProductProjectionQuery.ofStaged()
                                                                   .withPredicates(QueryPredicate.of("version = \"2\"")))
                                    .thenApply(PagedQueryResult::getTotal)
                                    .thenApply(Long::intValue)
                                    .toCompletableFuture())
            .isCompletedWithValue(halfNumberOfDrafts);

        // Assert actual state of CTP project (total number of existing products)
        assertThat(CTP_TARGET_CLIENT.execute(ProductProjectionQuery.of(STAGED))
                                    .thenApply(PagedQueryResult::getTotal)
                                    .thenApply(Long::intValue)
                                    .toCompletableFuture())
            .isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);



        // Assert statistics
        assertThat(syncStatistics).hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, halfNumberOfDrafts, halfNumberOfDrafts, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();

        saveNewResult(SyncSolutionInfo.LIB_VERSION, PRODUCT_SYNC, CREATES_AND_UPDATES, totalTime);
    }

    @Nonnull
    private List<ProductDraft> buildProductDrafts(final int numberOfProducts) {
        final List<ProductDraft> productDrafts = new ArrayList<>();
        final Reference<ProductType> draftsProductType = ProductType.referenceOfId(productType.getKey());
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
