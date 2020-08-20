package com.commercetools.sync.benchmark;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_AND_UPDATES;
import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST;
import static com.commercetools.sync.benchmark.BenchmarkUtils.PRODUCT_TYPE_SYNC;
import static com.commercetools.sync.benchmark.BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR;
import static com.commercetools.sync.benchmark.BenchmarkUtils.UPDATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.saveNewResult;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.deleteProductTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ProductTypeSyncBenchmark {

    private ProductTypeSyncOptions productTypeSyncOptions;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    @AfterAll
    static void tearDown() {
        deleteProductTypes(CTP_TARGET_CLIENT);
    }

    @BeforeEach
    void setupTest() {
        clearSyncTestCollections();
        deleteProductTypes(CTP_TARGET_CLIENT);
        productTypeSyncOptions = buildSyncOptions();
    }

    @Nonnull
    private ProductTypeSyncOptions buildSyncOptions() {
        final QuadConsumer<SyncException, Optional<ProductTypeDraft>, Optional<ProductType>,
                List<UpdateAction<ProductType>>> errorCallBack =
                    (exception, newResource, oldResource, updateActions) -> {
                        errorCallBackMessages.add(exception.getMessage());
                        errorCallBackExceptions.add(exception.getCause());
                    };
        final TriConsumer<SyncException, Optional<ProductTypeDraft>, Optional<ProductType>> warningCallBack =
            (exception, newResource, oldResource) -> warningCallBackMessages.add(exception.getMessage());
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
        final List<ProductTypeDraft> productTypeDrafts = buildProductTypeDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);
        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // benchmark
        final long beforeSyncTime = System.currentTimeMillis();
        final ProductTypeSyncStatistics syncStatistics = executeBlocking(productTypeSync.sync(productTypeDrafts));
        final long totalTime = System.currentTimeMillis() - beforeSyncTime;

        // assert on threshold (based on history of benchmarks; highest was ~12 seconds)
        final int threshold = 24000; // double of the highest benchmark
        assertThat(totalTime).withFailMessage(format(THRESHOLD_EXCEEDED_ERROR, totalTime, threshold))
                             .isLessThan(threshold);

        // Assert actual state of CTP project (total number of existing product types)
        final CompletableFuture<Integer> totalNumberOfProductTypes =
                CTP_TARGET_CLIENT.execute(ProductTypeQuery.of())
                                 .thenApply(PagedQueryResult::getTotal)
                                 .thenApply(Long::intValue)
                                 .toCompletableFuture();

        executeBlocking(totalNumberOfProductTypes);
        assertThat(totalNumberOfProductTypes).isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);


        assertThat(syncStatistics).hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, NUMBER_OF_RESOURCE_UNDER_TEST, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();

        saveNewResult(PRODUCT_TYPE_SYNC, CREATES_ONLY, totalTime);
    }

    @Test
    void sync_ExistingProductTypes_ShouldUpdateProductTypes() throws IOException {
        // preparation
        final List<ProductTypeDraft> productTypeDrafts = buildProductTypeDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);
        // Create drafts to target project with different attribute definition name
        CompletableFuture.allOf(
                productTypeDrafts.stream()
                                 .map(ProductTypeDraftBuilder::of)
                                 .map(ProductTypeSyncBenchmark::applyAttributeDefinitionNameChange)
                                 .map(ProductTypeDraftBuilder::build)
                                 .map(draft -> CTP_TARGET_CLIENT.execute(ProductTypeCreateCommand.of(draft)))
                                 .map(CompletionStage::toCompletableFuture)
                                 .toArray(CompletableFuture[]::new))
                         .join();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // benchmark
        final long beforeSyncTime = System.currentTimeMillis();
        final ProductTypeSyncStatistics syncStatistics = executeBlocking(productTypeSync.sync(productTypeDrafts));
        final long totalTime = System.currentTimeMillis() - beforeSyncTime;

        // assert on threshold (based on history of benchmarks; highest was ~13 seconds)
        final int threshold = 26000; // double of the highest benchmark
        assertThat(totalTime).withFailMessage(format(THRESHOLD_EXCEEDED_ERROR, totalTime, threshold))
                             .isLessThan(threshold);

        // Assert actual state of CTP project (number of updated product types)
        final CompletableFuture<Integer> totalNumberOfUpdatedProductTypes =
                CTP_TARGET_CLIENT.execute(ProductTypeQuery.of()
                                                          .withPredicates(p -> p.attributes().name().is("attr_name_1")))
                                 .thenApply(PagedQueryResult::getTotal)
                                 .thenApply(Long::intValue)
                                 .toCompletableFuture();

        executeBlocking(totalNumberOfUpdatedProductTypes);
        assertThat(totalNumberOfUpdatedProductTypes).isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);

        // Assert actual state of CTP project (total number of existing product types)
        final CompletableFuture<Integer> totalNumberOfProductTypes =
                CTP_TARGET_CLIENT.execute(ProductTypeQuery.of())
                                 .thenApply(PagedQueryResult::getTotal)
                                 .thenApply(Long::intValue)
                                 .toCompletableFuture();

        executeBlocking(totalNumberOfProductTypes);
        assertThat(totalNumberOfProductTypes).isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);

        // Assert statistics
        assertThat(syncStatistics).hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, 0, NUMBER_OF_RESOURCE_UNDER_TEST, 0);

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();

        saveNewResult(PRODUCT_TYPE_SYNC, UPDATES_ONLY, totalTime);
    }

    @Test
    void sync_WithSomeExistingProductTypes_ShouldSyncProductTypes() throws IOException {
        // preparation
        final List<ProductTypeDraft> productTypeDrafts = buildProductTypeDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);
        final int halfNumberOfDrafts = productTypeDrafts.size() / 2;
        final List<ProductTypeDraft> firstHalf = productTypeDrafts.subList(0, halfNumberOfDrafts);

        // Create first half of drafts to target project with different attribute definition name
        CompletableFuture.allOf(firstHalf.stream()
                                         .map(ProductTypeDraftBuilder::of)
                                         .map(ProductTypeSyncBenchmark::applyAttributeDefinitionNameChange)
                                         .map(ProductTypeDraftBuilder::build)
                                         .map(draft -> CTP_TARGET_CLIENT.execute(ProductTypeCreateCommand.of(draft)))
                                         .map(CompletionStage::toCompletableFuture)
                                         .toArray(CompletableFuture[]::new))
                         .join();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // benchmark
        final long beforeSyncTime = System.currentTimeMillis();
        final ProductTypeSyncStatistics syncStatistics = executeBlocking(productTypeSync.sync(productTypeDrafts));
        final long totalTime = System.currentTimeMillis() - beforeSyncTime;

        // assert on threshold (based on history of benchmarks; highest was ~13 seconds)
        final int threshold = 26000; // double of the highest benchmark
        assertThat(totalTime).withFailMessage(format(THRESHOLD_EXCEEDED_ERROR, totalTime, threshold))
                             .isLessThan(threshold);

        // Assert actual state of CTP project (number of updated product types)
        final CompletableFuture<Integer> totalNumberOfProductTypesWithOldName =
                CTP_TARGET_CLIENT.execute(ProductTypeQuery.of()
                                                          .withPredicates(
                                                              p -> p.attributes().name().is("attr_name_1_old")))
                                 .thenApply(PagedQueryResult::getTotal)
                                 .thenApply(Long::intValue)
                                 .toCompletableFuture();

        executeBlocking(totalNumberOfProductTypesWithOldName);
        assertThat(totalNumberOfProductTypesWithOldName).isCompletedWithValue(0);

        // Assert actual state of CTP project (total number of existing product types)
        final CompletableFuture<Integer> totalNumberOfProductTypes =
                CTP_TARGET_CLIENT.execute(ProductTypeQuery.of())
                                 .thenApply(PagedQueryResult::getTotal)
                                 .thenApply(Long::intValue)
                                 .toCompletableFuture();
        executeBlocking(totalNumberOfProductTypes);
        assertThat(totalNumberOfProductTypes).isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);

        // Assert statistics
        assertThat(syncStatistics).hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, halfNumberOfDrafts, halfNumberOfDrafts, 0);

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();

        saveNewResult(PRODUCT_TYPE_SYNC, CREATES_AND_UPDATES, totalTime);
    }


    @Nonnull
    private static List<ProductTypeDraft> buildProductTypeDrafts(final int numberOfTypes) {
        return IntStream
                .range(0, numberOfTypes)
                .mapToObj(i -> ProductTypeDraftBuilder.of(
                        format("key__%d", i),
                        format("name__%d", i),
                        format("description__%d", i),
                        singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
                ).build())
                .collect(Collectors.toList());
    }

    @Nonnull
    private static ProductTypeDraftBuilder applyAttributeDefinitionNameChange(
            @Nonnull final ProductTypeDraftBuilder builder) {

        final List<AttributeDefinitionDraft> list =
                builder.getAttributes()
                       .stream()
                       .map(attributeDefinitionDraft -> AttributeDefinitionDraftBuilder
                               .of(attributeDefinitionDraft)
                               .name(attributeDefinitionDraft.getName() + "_old")
                               .build())
                       .collect(Collectors.toList());

        return builder.attributes(list);
    }

}
