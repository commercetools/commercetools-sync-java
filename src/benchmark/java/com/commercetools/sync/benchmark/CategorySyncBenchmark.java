package com.commercetools.sync.benchmark;

import static com.commercetools.sync.benchmark.BenchmarkUtils.CATEGORY_SYNC;
import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_AND_UPDATES;
import static com.commercetools.sync.benchmark.BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST;
import static com.commercetools.sync.benchmark.BenchmarkUtils.UPDATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.saveNewResult;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.category.CategoryPagedQueryResponse;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategorySyncBenchmark {

  private static final int CATEGORY_BENCHMARKS_CREATE_ACTION_THRESHOLD = 23_000;
  private static final int CATEGORY_BENCHMARKS_UPDATE_ACTION_THRESHOLD = 23_000;
  private static final int CATEGORY_BENCHMARKS_CREATE_AND_UPDATE_ACTION_THRESHOLD = 23_000;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private CategorySyncOptions categorySyncOptions;

  @BeforeAll
  static void setup() {
    deleteAllCategories();
  }

  @BeforeEach
  void setupTest() {
    deleteAllCategories();
    clearSyncTestCollections();
    categorySyncOptions = buildSyncOptions();
  }

  private void clearSyncTestCollections() {
    errorCallBackMessages = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
  }

  private CategorySyncOptions buildSyncOptions() {
    final QuadConsumer<
            SyncException, Optional<CategoryDraft>, Optional<Category>, List<CategoryUpdateAction>>
        errorCallback =
            (exception, newResource, oldResource, updateActions) -> {
              errorCallBackMessages.add(exception.getMessage());
              errorCallBackExceptions.add(exception.getCause());
            };

    final TriConsumer<SyncException, Optional<CategoryDraft>, Optional<Category>> warningCallBack =
        (exception, newResource, oldResource) ->
            warningCallBackMessages.add(exception.getMessage());

    return CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(errorCallback)
        .warningCallback(warningCallBack)
        .build();
  }

  @Test
  void sync_NewCategories_ShouldCreateCategories() throws IOException {
    // preparation
    final List<CategoryDraft> categoryDrafts =
        getCategoryDraftsWithMetaDescription(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    final CategorySync categorySync = new CategorySync(categorySyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    final CategorySyncStatistics syncStatistics =
        categorySync.sync(categoryDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    // asserts
    assertThat(totalTime)
        .withFailMessage(
            String.format(
                BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                CATEGORY_BENCHMARKS_CREATE_ACTION_THRESHOLD))
        .isLessThan(CATEGORY_BENCHMARKS_CREATE_ACTION_THRESHOLD);

    AssertionsForStatistics.assertThat(syncStatistics)
        .hasValues(
            BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST,
            BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST,
            0,
            0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    // Assert actual state of CTP project (total number of existing categories)
    final Long totalNumberOfCreatedCategories =
        CTP_TARGET_CLIENT
            .categories()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(CategoryPagedQueryResponse::getTotal)
            .join();
    assertThat(totalNumberOfCreatedCategories)
        .isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    if (BenchmarkUtils.SUBMIT_BENCHMARK_RESULT) {
      BenchmarkUtils.saveNewResult(CATEGORY_SYNC, BenchmarkUtils.CREATES_ONLY, totalTime);
    }
  }

  @Test
  void sync_ExistingCategories_ShouldUpdateCategories() throws IOException {
    final List<CategoryDraft> categoryDrafts =
        getCategoryDraftsWithMetaDescription(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    // Create drafts to target project with different meta descriptions
    CompletableFuture.allOf(
            categoryDrafts.stream()
                .map(CategoryDraftBuilder::of)
                .map(
                    builder ->
                        builder.metaDescription(LocalizedString.ofEnglish("oldMetaDescription")))
                .map(CategoryDraftBuilder::build)
                .map(draft -> CTP_TARGET_CLIENT.categories().create(draft).execute())
                .map(CompletableFuture::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();

    final CategorySync categorySync = new CategorySync(categorySyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    final CategorySyncStatistics syncStatistics =
        categorySync.sync(categoryDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    // asserts
    assertThat(totalTime)
        .withFailMessage(
            String.format(
                BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                CATEGORY_BENCHMARKS_UPDATE_ACTION_THRESHOLD))
        .isLessThan(CATEGORY_BENCHMARKS_UPDATE_ACTION_THRESHOLD);
    AssertionsForStatistics.assertThat(syncStatistics)
        .hasValues(
            BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST,
            0,
            BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST,
            0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    // Assert actual state of CTP project (number of updated categories)
    final Long numberOfUpdatedCategories =
        CTP_TARGET_CLIENT
            .categories()
            .get()
            .withWhere("metaDescription(en=:desc)")
            .withPredicateVar("desc", "newMetaDescription")
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(CategoryPagedQueryResponse::getTotal)
            .join();
    assertThat(numberOfUpdatedCategories).isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert actual state of CTP project (total number of existing categories)
    final Long totalNumberOfCategories =
        CTP_TARGET_CLIENT
            .categories()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(CategoryPagedQueryResponse::getTotal)
            .join();
    assertThat(totalNumberOfCategories).isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    if (BenchmarkUtils.SUBMIT_BENCHMARK_RESULT) {
      saveNewResult(CATEGORY_SYNC, UPDATES_ONLY, totalTime);
    }
  }

  @Test
  void sync_WithSomeExistingCategories_ShouldSyncCategories() throws IOException {
    // preparation
    final List<CategoryDraft> categoryDrafts =
        getCategoryDraftsWithMetaDescription(NUMBER_OF_RESOURCE_UNDER_TEST);
    final int halfNumberOfDrafts = categoryDrafts.size() / 2;
    final List<CategoryDraft> firstHalfOfDrafts = categoryDrafts.subList(0, halfNumberOfDrafts);

    // Create drafts to target project with different meta descriptions
    CompletableFuture.allOf(
            firstHalfOfDrafts.stream()
                .map(CategoryDraftBuilder::of)
                .map(
                    builder ->
                        builder.metaDescription(LocalizedString.ofEnglish("oldMetaDescription")))
                .map(CategoryDraftBuilder::build)
                .map(draft -> CTP_TARGET_CLIENT.categories().create(draft).execute())
                .map(CompletableFuture::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();

    final CategorySync categorySync = new CategorySync(categorySyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    final CategorySyncStatistics syncStatistics =
        categorySync.sync(categoryDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    // asserts
    assertThat(totalTime)
        .withFailMessage(
            String.format(
                BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                CATEGORY_BENCHMARKS_CREATE_AND_UPDATE_ACTION_THRESHOLD))
        .isLessThan(CATEGORY_BENCHMARKS_CREATE_AND_UPDATE_ACTION_THRESHOLD);

    AssertionsForStatistics.assertThat(syncStatistics)
        .hasValues(
            BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST,
            halfNumberOfDrafts,
            halfNumberOfDrafts,
            0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    // Assert actual state of CTP project (number of updated categories)
    final Long numberOfUpdatedCategories =
        CTP_TARGET_CLIENT
            .categories()
            .get()
            .withWhere("metaDescription(en=:desc)")
            .withPredicateVar("desc", "newMetaDescription")
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(CategoryPagedQueryResponse::getTotal)
            .join();
    assertThat(numberOfUpdatedCategories).isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert actual state of CTP project (total number of existing categories)
    final Long totalNumberOfCategories =
        CTP_TARGET_CLIENT
            .categories()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(CategoryPagedQueryResponse::getTotal)
            .join();
    assertThat(totalNumberOfCategories).isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    if (BenchmarkUtils.SUBMIT_BENCHMARK_RESULT) {
      saveNewResult(CATEGORY_SYNC, CREATES_AND_UPDATES, totalTime);
    }
  }

  private List<CategoryDraft> getCategoryDraftsWithMetaDescription(final int numberOfCategories) {
    final List<CategoryDraft> categoryDraftsWithDescription = new ArrayList<>();
    final List<CategoryDraft> categoryDrafts = getCategoryDrafts(null, numberOfCategories, false);
    for (CategoryDraft categoryDraft : categoryDrafts) {
      final CategoryDraftBuilder categoryDraftBuilder =
          CategoryDraftBuilder.of(categoryDraft)
              .metaDescription(LocalizedString.ofEnglish("newMetaDescription"));
      categoryDraftsWithDescription.add(categoryDraftBuilder.build());
    }
    return categoryDraftsWithDescription;
  }

  private static void deleteAllCategories() {
    QueryUtils.queryAll(
            CTP_TARGET_CLIENT.categories().get(),
            fetchedCategories -> {
              CompletableFuture.allOf(
                      fetchedCategories.stream()
                          .map(category -> deleteCategory(category))
                          .map(CompletionStage::toCompletableFuture)
                          .toArray(CompletableFuture[]::new))
                  .join();
            })
        .toCompletableFuture()
        .join();
  }

  private static CompletionStage<Category> deleteCategory(final Category category) {
    return CTP_TARGET_CLIENT
        .categories()
        .delete(category)
        .execute()
        .thenApply(ApiHttpResponse::getBody);
  }
}
