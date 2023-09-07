package com.commercetools.sync.benchmark;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.*;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ensureProductType;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductPagedQueryResponse;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionPagedQueryResponse;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifier;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncBenchmark {
  private static ProductType productType;
  private ProductSyncOptions syncOptions;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private static final int PRODUCT_BENCHMARKS_CREATE_ACTION_THRESHOLD =
      30_000; // 150% of the maximum value (20K)
  private static final int PRODUCT_BENCHMARKS_UPDATE_ACTION_THRESHOLD =
      33_000; // 150% of the maximum value (22K)

  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    ensureCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY, ENGLISH, OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_TARGET_CLIENT);
    productType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
  }

  @BeforeEach
  void setupTest() {
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
    final QuadConsumer<
            SyncException,
            Optional<ProductDraft>,
            Optional<ProductProjection>,
            List<ProductUpdateAction>>
        errorCallback =
            (exception, newResource, oldResource, updateActions) -> {
              errorCallBackMessages.add(exception.getMessage());
              errorCallBackExceptions.add(exception.getCause());
            };
    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
        warningCallback =
            (exception, newResource, oldResource) ->
                warningCallBackMessages.add(exception.getMessage());

    return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(errorCallback)
        .warningCallback(warningCallback)
        .build();
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_NewProducts_ShouldCreateProducts() throws IOException {
    final List<ProductDraft> productDrafts =
        buildProductDrafts(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    // Sync drafts
    final ProductSync productSync = new ProductSync(syncOptions);

    final long beforeSyncTime = System.currentTimeMillis();
    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    assertThat(totalTime)
        .withFailMessage(
            String.format(
                BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                PRODUCT_BENCHMARKS_CREATE_ACTION_THRESHOLD))
        .isLessThan(PRODUCT_BENCHMARKS_CREATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (total number of existing products)
    final Long totalNumberOfProducts =
        CTP_TARGET_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductProjectionPagedQueryResponse::getTotal)
            .join();
    assertThat(totalNumberOfProducts).isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    assertThat(syncStatistics)
        .hasValues(
            BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST,
            BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST,
            0,
            0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    if (BenchmarkUtils.SUBMIT_BENCHMARK_RESULT) {
      BenchmarkUtils.saveNewResult(
          BenchmarkUtils.PRODUCT_SYNC_SDK_V2, BenchmarkUtils.CREATES_ONLY, totalTime);
    }
  }

  @Test
  void sync_ExistingProducts_ShouldUpdateProducts() throws IOException {
    final List<ProductDraft> productDrafts =
        buildProductDrafts(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);
    // Create drafts to target project with different descriptions
    CompletableFuture.allOf(
            productDrafts.stream()
                .map(ProductDraftBuilder::of)
                .map(builder -> builder.description(ofEnglish("oldDescription")))
                .map(builder -> builder.productType(productType.toResourceIdentifier()))
                .map(ProductDraftBuilder::build)
                .map(draft -> CTP_TARGET_CLIENT.products().create(draft).execute())
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();

    // Sync new drafts
    final ProductSync productSync = new ProductSync(syncOptions);

    final long beforeSyncTime = System.currentTimeMillis();
    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    assertThat(totalTime)
        .withFailMessage(
            String.format(
                BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                PRODUCT_BENCHMARKS_UPDATE_ACTION_THRESHOLD))
        .isLessThan(PRODUCT_BENCHMARKS_UPDATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (number of updated products)
    final Long totalNumberOfUpdatedProducts =
        CTP_TARGET_CLIENT
            .products()
            .get()
            .withWhere("masterData(staged(description(en=:description)))")
            .withPredicateVar("description", "newDescription")
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductPagedQueryResponse::getTotal)
            .join();

    assertThat(totalNumberOfUpdatedProducts)
        .isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert actual state of CTP project (total number of existing products)
    final Long totalNumberOfProducts =
        CTP_TARGET_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductProjectionPagedQueryResponse::getTotal)
            .join();
    assertThat(totalNumberOfProducts).isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert statistics
    assertThat(syncStatistics)
        .hasValues(
            BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST,
            0,
            BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST,
            0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    if (BenchmarkUtils.SUBMIT_BENCHMARK_RESULT) {
      BenchmarkUtils.saveNewResult(
          BenchmarkUtils.PRODUCT_SYNC_SDK_V2, BenchmarkUtils.UPDATES_ONLY, totalTime);
    }
  }

  @Test
  void sync_WithSomeExistingProducts_ShouldSyncProducts() throws IOException {
    final List<ProductDraft> productDrafts =
        buildProductDrafts(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);
    final int halfNumberOfDrafts = productDrafts.size() / 2;
    final List<ProductDraft> firstHalf = productDrafts.subList(0, halfNumberOfDrafts);

    // Create first half of drafts to target project with different description
    CompletableFuture.allOf(
            firstHalf.stream()
                .map(ProductDraftBuilder::of)
                .map(builder -> builder.description(ofEnglish("oldDescription")))
                .map(builder -> builder.productType(productType.toResourceIdentifier()))
                .map(ProductDraftBuilder::build)
                .map(draft -> CTP_TARGET_CLIENT.products().create(draft).execute())
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();

    // Sync new drafts
    final ProductSync productSync = new ProductSync(syncOptions);

    final long beforeSyncTime = System.currentTimeMillis();
    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    assertThat(totalTime)
        .withFailMessage(
            String.format(
                BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                PRODUCT_BENCHMARKS_UPDATE_ACTION_THRESHOLD))
        .isLessThan(PRODUCT_BENCHMARKS_UPDATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (number of updated products)
    final Long totalNumberOfUpdatedProducts =
        CTP_TARGET_CLIENT
            .products()
            .get()
            .withWhere("masterData(staged(description(en=:description)))")
            .withPredicateVar("description", "oldDescription")
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductPagedQueryResponse::getTotal)
            .join();

    assertThat(totalNumberOfUpdatedProducts).isEqualTo(0L);

    // Assert actual state of CTP project (total number of existing products)
    final Long totalNumberOfProducts =
        CTP_TARGET_CLIENT
            .products()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductPagedQueryResponse::getTotal)
            .join();
    assertThat(totalNumberOfProducts).isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert statistics
    assertThat(syncStatistics)
        .hasValues(
            BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST,
            halfNumberOfDrafts,
            halfNumberOfDrafts,
            0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    if (BenchmarkUtils.SUBMIT_BENCHMARK_RESULT) {
      BenchmarkUtils.saveNewResult(
          BenchmarkUtils.PRODUCT_SYNC_SDK_V2, BenchmarkUtils.CREATES_AND_UPDATES, totalTime);
    }
  }

  @Nonnull
  private List<ProductDraft> buildProductDrafts(final int numberOfProducts) {
    final List<ProductDraft> productDrafts = new ArrayList<>();
    final ProductTypeResourceIdentifier draftsProductType =
        ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build();
    for (int i = 0; i < numberOfProducts; i++) {
      final ProductVariantDraft masterVariantDraft =
          ProductVariantDraftBuilder.of().key("masterVariantKey_" + i).sku("sku_" + i).build();
      final ProductDraft productDraft =
          ProductDraftBuilder.of()
              .productType(draftsProductType)
              .name(ofEnglish("name_" + i))
              .slug(ofEnglish("slug_" + i))
              .masterVariant(masterVariantDraft)
              .description(ofEnglish("newDescription"))
              .key("productKey_" + i)
              .build();
      productDrafts.add(productDraft);
    }
    return productDrafts;
  }
}
