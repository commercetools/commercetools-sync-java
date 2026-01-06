package com.commercetools.sync.benchmark;

import static com.commercetools.sync.benchmark.BenchmarkUtils.*;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.deleteProductTypes;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypePagedQueryResponse;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductTypeSyncBenchmark {

  private ProductTypeSyncOptions productTypeSyncOptions;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private static final int PRODUCT_TYPE_BENCHMARKS_CREATE_ACTION_THRESHOLD =
      24_000; // 150% of the maximum value (16K)
  private static final int PRODUCT_TYPE_BENCHMARKS_UPDATE_ACTION_THRESHOLD =
      24_000; // 150% of the maximum value (16K)

  @AfterAll
  static void tearDown() {
    deleteAllProducts(CTP_TARGET_CLIENT);
    deleteProductTypes(CTP_TARGET_CLIENT);
  }

  @BeforeEach
  void setupTest() {
    clearSyncTestCollections();
    // Delete products first because they reference product types
    deleteAllProducts(CTP_TARGET_CLIENT);
    deleteProductTypes(CTP_TARGET_CLIENT);
    productTypeSyncOptions = buildSyncOptions();
  }

  @Nonnull
  private ProductTypeSyncOptions buildSyncOptions() {
    final QuadConsumer<
            SyncException,
            Optional<ProductTypeDraft>,
            Optional<ProductType>,
            List<ProductTypeUpdateAction>>
        errorCallBack =
            (exception, newResource, oldResource, updateActions) -> {
              errorCallBackMessages.add(exception.getMessage());
              errorCallBackExceptions.add(exception.getCause());
            };
    final TriConsumer<SyncException, Optional<ProductTypeDraft>, Optional<ProductType>>
        warningCallBack =
            (exception, newResource, oldResource) ->
                warningCallBackMessages.add(exception.getMessage());
    return ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(errorCallBack)
        .warningCallback(warningCallBack)
        .build();
  }

  private void clearSyncTestCollections() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
  }

  @Test
  void sync_NewProductTypes_ShouldCreateProductTypes() throws IOException {
    // preparation
    final List<ProductTypeDraft> productTypeDrafts =
        buildProductTypeDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);
    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    final ProductTypeSyncStatistics syncStatistics =
        productTypeSync.sync(productTypeDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    // assert on threshold (based on history of benchmarks; highest was ~12 seconds)
    assertThat(totalTime)
        .withFailMessage(
            format(
                THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                PRODUCT_TYPE_BENCHMARKS_CREATE_ACTION_THRESHOLD))
        .isLessThan(PRODUCT_TYPE_BENCHMARKS_CREATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (total number of existing product types)
    final Integer totalNumberOfProductTypes =
        CTP_TARGET_CLIENT
            .productTypes()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductTypePagedQueryResponse::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfProductTypes).isEqualTo(NUMBER_OF_RESOURCE_UNDER_TEST);

    assertThat(syncStatistics)
        .hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, NUMBER_OF_RESOURCE_UNDER_TEST, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    if (SUBMIT_BENCHMARK_RESULT) {
      saveNewResult(PRODUCT_TYPE_SYNC, CREATES_ONLY, totalTime);
    }
  }

  @Test
  void sync_ExistingProductTypes_ShouldUpdateProductTypes() throws IOException {
    // preparation
    final List<ProductTypeDraft> productTypeDrafts =
        buildProductTypeDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);
    // Create drafts to target project with different attribute definition name
    CompletableFuture.allOf(
            productTypeDrafts.stream()
                .map(ProductTypeDraftBuilder::of)
                .map(ProductTypeSyncBenchmark::applyAttributeDefinitionNameChange)
                .map(ProductTypeDraftBuilder::build)
                .map(draft -> CTP_TARGET_CLIENT.productTypes().create(draft).execute())
                .toArray(CompletableFuture[]::new))
        .join();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    final ProductTypeSyncStatistics syncStatistics =
        productTypeSync.sync(productTypeDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    // assert on threshold (based on history of benchmarks; highest was ~13 seconds)
    assertThat(totalTime)
        .withFailMessage(
            format(
                THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                PRODUCT_TYPE_BENCHMARKS_UPDATE_ACTION_THRESHOLD))
        .isLessThan(PRODUCT_TYPE_BENCHMARKS_UPDATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (number of updated product types)
    final Long totalNumberOfUpdatedProductTypes =
        CTP_TARGET_CLIENT
            .productTypes()
            .get()
            .withWhere("attributes(name=:name)")
            .withPredicateVar("name", "attr_name_1")
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductTypePagedQueryResponse::getTotal)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfUpdatedProductTypes).isEqualTo(NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert actual state of CTP project (total number of existing product types)
    final Long totalNumberOfProductTypes =
        CTP_TARGET_CLIENT
            .productTypes()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductTypePagedQueryResponse::getTotal)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfProductTypes).isEqualTo(NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert statistics
    assertThat(syncStatistics)
        .hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, 0, NUMBER_OF_RESOURCE_UNDER_TEST, 0);

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    if (SUBMIT_BENCHMARK_RESULT) {
      saveNewResult(PRODUCT_TYPE_SYNC, UPDATES_ONLY, totalTime);
    }
  }

  @Test
  void sync_WithSomeExistingProductTypes_ShouldSyncProductTypes() throws IOException {
    // preparation
    final List<ProductTypeDraft> productTypeDrafts =
        buildProductTypeDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);
    final int halfNumberOfDrafts = productTypeDrafts.size() / 2;
    final List<ProductTypeDraft> firstHalf = productTypeDrafts.subList(0, halfNumberOfDrafts);

    // Create first half of drafts to target project with different attribute definition name
    CompletableFuture.allOf(
            firstHalf.stream()
                .map(ProductTypeDraftBuilder::of)
                .map(ProductTypeSyncBenchmark::applyAttributeDefinitionNameChange)
                .map(ProductTypeDraftBuilder::build)
                .map(draft -> CTP_TARGET_CLIENT.productTypes().create(draft).execute())
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    final ProductTypeSyncStatistics syncStatistics =
        productTypeSync.sync(productTypeDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    // assert on threshold (based on history of benchmarks; highest was ~13 seconds)
    assertThat(totalTime)
        .withFailMessage(
            format(
                THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                PRODUCT_TYPE_BENCHMARKS_UPDATE_ACTION_THRESHOLD))
        .isLessThan(PRODUCT_TYPE_BENCHMARKS_UPDATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (number of updated product types)
    final Long totalNumberOfProductTypesWithOldName =
        CTP_TARGET_CLIENT
            .productTypes()
            .get()
            .withWhere("attributes(name=:name)")
            .withPredicateVar("name", "attr_name_1_old")
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductTypePagedQueryResponse::getTotal)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfProductTypesWithOldName).isEqualTo(0);

    // Assert actual state of CTP project (total number of existing product types)
    final Long totalNumberOfProductTypes =
        CTP_TARGET_CLIENT
            .productTypes()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductTypePagedQueryResponse::getTotal)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfProductTypes).isEqualTo(NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert statistics
    assertThat(syncStatistics)
        .hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, halfNumberOfDrafts, halfNumberOfDrafts, 0);

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    if (SUBMIT_BENCHMARK_RESULT) {
      saveNewResult(PRODUCT_TYPE_SYNC, CREATES_AND_UPDATES, totalTime);
    }
  }

  @Nonnull
  private static List<ProductTypeDraft> buildProductTypeDrafts(final int numberOfTypes) {
    return IntStream.range(0, numberOfTypes)
        .mapToObj(
            i ->
                ProductTypeDraftBuilder.of()
                    .key(format("key__%d", i))
                    .name(format("name__%d", i))
                    .description(format("description__%d", i))
                    .attributes(singletonList(ATTRIBUTE_DEFINITION_DRAFT_1))
                    .build())
        .collect(Collectors.toList());
  }

  @Nonnull
  private static ProductTypeDraftBuilder applyAttributeDefinitionNameChange(
      @Nonnull final ProductTypeDraftBuilder builder) {

    final List<AttributeDefinitionDraft> list =
        builder.getAttributes().stream()
            .map(
                attributeDefinitionDraft ->
                    AttributeDefinitionDraftBuilder.of(attributeDefinitionDraft)
                        .name(attributeDefinitionDraft.getName() + "_old")
                        .build())
            .collect(Collectors.toList());

    return builder.attributes(list);
  }
}
