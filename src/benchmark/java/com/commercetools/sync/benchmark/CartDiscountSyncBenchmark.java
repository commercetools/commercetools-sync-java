package com.commercetools.sync.benchmark;

import static com.commercetools.sync.benchmark.BenchmarkUtils.CART_DISCOUNT_SYNC;
import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_AND_UPDATES;
import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST;
import static com.commercetools.sync.benchmark.BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR;
import static com.commercetools.sync.benchmark.BenchmarkUtils.UPDATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.saveNewResult;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_CART_PREDICATE_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_NAME_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_TARGET_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_VALUE_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.JANUARY_FROM;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.JANUARY_UNTIL;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.deleteCartDiscounts;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.getSortOrders;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.cartdiscounts.CartDiscountSync;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.StackingMode;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountCreateCommand;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartDiscountSyncBenchmark {

  private CartDiscountSyncOptions cartDiscountSyncOptions;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  @AfterAll
  static void tearDown() {
    deleteCartDiscounts(CTP_TARGET_CLIENT);
  }

  @BeforeEach
  void setupTest() {
    clearSyncTestCollections();
    deleteCartDiscounts(CTP_TARGET_CLIENT);
    cartDiscountSyncOptions = buildSyncOptions();
  }

  private void clearSyncTestCollections() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
  }

  private CartDiscountSyncOptions buildSyncOptions() {
    final QuadConsumer<
            SyncException,
            Optional<CartDiscountDraft>,
            Optional<CartDiscount>,
            List<UpdateAction<CartDiscount>>>
        errorCallback =
            (exception, newResource, oldResource, updateActions) -> {
              errorCallBackMessages.add(exception.getMessage());
              errorCallBackExceptions.add(exception.getCause());
            };

    final TriConsumer<SyncException, Optional<CartDiscountDraft>, Optional<CartDiscount>>
        warningCallBack =
            (exception, newResource, oldResource) ->
                warningCallBackMessages.add(exception.getMessage());

    return CartDiscountSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(errorCallback)
        .warningCallback(warningCallBack)
        .build();
  }

  @Test
  void sync_NewCartDiscounts_ShouldCreateCartDiscounts() throws IOException {
    // preparation
    final List<CartDiscountDraft> cartDiscountDrafts =
        buildCartDiscountDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);
    final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    final CartDiscountSyncStatistics syncStatistics =
        executeBlocking(cartDiscountSync.sync(cartDiscountDrafts));
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    // assert on threshold (based on history of benchmarks; highest was 10496 ms)
    final int threshold = 23000; // double of the highest benchmark
    assertThat(totalTime)
        .withFailMessage(format(THRESHOLD_EXCEEDED_ERROR, totalTime, threshold))
        .isLessThan(threshold);

    // Assert actual state of CTP project (total number of existing cart discounts)
    final CompletableFuture<Integer> totalNumberOfCartDiscounts =
        CTP_TARGET_CLIENT
            .execute(CartDiscountQuery.of())
            .thenApply(PagedQueryResult::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture();

    executeBlocking(totalNumberOfCartDiscounts);
    assertThat(totalNumberOfCartDiscounts).isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);

    assertThat(syncStatistics)
        .hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, NUMBER_OF_RESOURCE_UNDER_TEST, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    saveNewResult(CART_DISCOUNT_SYNC, CREATES_ONLY, totalTime);
  }

  @Test
  void sync_ExistingCartDiscounts_ShouldUpdateCartDiscounts() throws IOException {
    // preparation
    final List<CartDiscountDraft> cartDiscountDrafts =
        buildCartDiscountDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);

    // Create cart discount drafts to target project
    // with different stacking mode (STOP_AFTER_THIS_DISCOUNT)
    CompletableFuture.allOf(
            cartDiscountDrafts.stream()
                .map(CartDiscountDraftBuilder::of)
                .map(builder -> builder.stackingMode(StackingMode.STOP_AFTER_THIS_DISCOUNT))
                .map(CartDiscountDraftBuilder::build)
                .map(draft -> CTP_TARGET_CLIENT.execute(CartDiscountCreateCommand.of(draft)))
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();

    final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    // Update cart discount drafts to target project with different stacking mode (STACKING)
    final CartDiscountSyncStatistics syncStatistics =
        executeBlocking(cartDiscountSync.sync(cartDiscountDrafts));
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    // assert on threshold (based on history of benchmarks; highest was 11263 ms)
    final int threshold = 23000; // double of the highest benchmark
    assertThat(totalTime)
        .withFailMessage(format(THRESHOLD_EXCEEDED_ERROR, totalTime, threshold))
        .isLessThan(threshold);

    // Assert actual state of CTP project (number of updated cart discount)
    final CompletableFuture<Integer> totalNumberOfUpdatedCartDiscounts =
        CTP_TARGET_CLIENT
            .execute(
                CartDiscountQuery.of()
                    .withPredicates(QueryPredicate.of("stackingMode = \"Stacking\"")))
            .thenApply(PagedQueryResult::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture();

    executeBlocking(totalNumberOfUpdatedCartDiscounts);
    assertThat(totalNumberOfUpdatedCartDiscounts)
        .isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert actual state of CTP project (total number of existing cart discounts)
    final CompletableFuture<Integer> totalNumberOfCartDiscounts =
        CTP_TARGET_CLIENT
            .execute(CartDiscountQuery.of())
            .thenApply(PagedQueryResult::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture();
    executeBlocking(totalNumberOfCartDiscounts);
    assertThat(totalNumberOfCartDiscounts).isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert statistics
    assertThat(syncStatistics)
        .hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, 0, NUMBER_OF_RESOURCE_UNDER_TEST, 0);

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    saveNewResult(CART_DISCOUNT_SYNC, UPDATES_ONLY, totalTime);
  }

  @Test
  void sync_WithSomeExistingCartDiscounts_ShouldSyncCartDiscounts() throws IOException {
    // preparation
    final List<CartDiscountDraft> cartDiscountDrafts =
        buildCartDiscountDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);
    final int halfNumberOfDrafts = cartDiscountDrafts.size() / 2;
    final List<CartDiscountDraft> firstHalf = cartDiscountDrafts.subList(0, halfNumberOfDrafts);

    // Create first half of cart discount drafts to target project
    // with different stacking mode (STOP_AFTER_THIS_DISCOUNT)
    CompletableFuture.allOf(
            firstHalf.stream()
                .map(CartDiscountDraftBuilder::of)
                .map(builder -> builder.stackingMode(StackingMode.STOP_AFTER_THIS_DISCOUNT))
                .map(CartDiscountDraftBuilder::build)
                .map(draft -> CTP_TARGET_CLIENT.execute(CartDiscountCreateCommand.of(draft)))
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();

    final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    // Create half of the cart discount drafts to target project
    // and update half of the cart discount drafts to target project with different stacking mode
    // (STACKING)
    final CartDiscountSyncStatistics syncStatistics =
        executeBlocking(cartDiscountSync.sync(cartDiscountDrafts));
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    // assert on threshold (based on history of benchmarks; highest was 11277 ms)
    final int threshold = 23000; // double of the highest benchmark
    assertThat(totalTime)
        .withFailMessage(format(THRESHOLD_EXCEEDED_ERROR, totalTime, threshold))
        .isLessThan(threshold);

    // Assert actual state of CTP project (number of updated cart discount)
    final CompletableFuture<Integer> totalNumberOfUpdatedCartDiscounts =
        CTP_TARGET_CLIENT
            .execute(
                CartDiscountQuery.of()
                    .withPredicates(QueryPredicate.of("stackingMode = \"StopAfterThisDiscount\"")))
            .thenApply(PagedQueryResult::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture();

    executeBlocking(totalNumberOfUpdatedCartDiscounts);
    assertThat(totalNumberOfUpdatedCartDiscounts).isCompletedWithValue(0);

    // Assert actual state of CTP project (total number of existing cart discounts)
    final CompletableFuture<Integer> totalNumberOfCartDiscounts =
        CTP_TARGET_CLIENT
            .execute(CartDiscountQuery.of())
            .thenApply(PagedQueryResult::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture();
    executeBlocking(totalNumberOfCartDiscounts);
    assertThat(totalNumberOfCartDiscounts).isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert statistics
    assertThat(syncStatistics)
        .hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, halfNumberOfDrafts, halfNumberOfDrafts, 0);

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    saveNewResult(CART_DISCOUNT_SYNC, CREATES_AND_UPDATES, totalTime);
  }

  @Nonnull
  private static List<CartDiscountDraft> buildCartDiscountDrafts(final int numberOfCartDiscounts) {
    final List<String> sortOrders = getSortOrders(numberOfCartDiscounts);
    return IntStream.range(0, numberOfCartDiscounts)
        .mapToObj(
            i ->
                CartDiscountDraftBuilder.of(
                        CART_DISCOUNT_NAME_1,
                        CART_DISCOUNT_CART_PREDICATE_1,
                        CART_DISCOUNT_VALUE_1,
                        CART_DISCOUNT_TARGET_1,
                        sortOrders.get(i),
                        false)
                    .key(format("key__%d", i))
                    .isActive(false)
                    .description(LocalizedString.of(Locale.ENGLISH, format("description__%d", i)))
                    .validFrom(JANUARY_FROM)
                    .validUntil(JANUARY_UNTIL)
                    .build())
        .collect(toList());
  }
}
