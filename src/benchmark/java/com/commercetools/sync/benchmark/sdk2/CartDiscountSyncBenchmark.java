package com.commercetools.sync.benchmark.sdk2;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.benchmark.sdk2.BenchmarkUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.CartDiscountITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountDraftBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountPagedQueryResponse;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.api.models.cart_discount.StackingMode;
import com.commercetools.sync.sdk2.cartdiscounts.CartDiscountSync;
import com.commercetools.sync.sdk2.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.sdk2.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.sdk2.cartdiscounts.helpers.CartDiscountSyncStatistics;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.QuadConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

  private static final int CART_DISCOUNT_BENCHMARKS_CREATE_ACTION_THRESHOLD =
      23_000; // assert on threshold (based on history of benchmarks; highest was 10496 ms)

  private static final int CART_DISCOUNT_BENCHMARKS_UPDATE_ACTION_THRESHOLD =
      23_000; // assert on threshold (based on history of benchmarks; highest was 11263 ms)

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
            List<CartDiscountUpdateAction>>
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
        cartDiscountSync.sync(cartDiscountDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    assertThat(totalTime)
        .withFailMessage(
            format(
                THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                CART_DISCOUNT_BENCHMARKS_CREATE_ACTION_THRESHOLD))
        .isLessThan(CART_DISCOUNT_BENCHMARKS_CREATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (total number of existing cart discounts)
    final Integer totalNumberOfCartDiscounts =
        CTP_TARGET_CLIENT
            .cartDiscounts()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(CartDiscountPagedQueryResponse::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfCartDiscounts).isEqualTo(NUMBER_OF_RESOURCE_UNDER_TEST);

    assertThat(syncStatistics)
        .hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, NUMBER_OF_RESOURCE_UNDER_TEST, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    if (SUBMIT_BENCHMARK_RESULT) {
      saveNewResult(CART_DISCOUNT_SYNC, CREATES_ONLY, totalTime);
    }
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
                .map(
                    builder ->
                        builder.stackingMode(
                            StackingMode.StackingModeEnum.STOP_AFTER_THIS_DISCOUNT))
                .map(CartDiscountDraftBuilder::build)
                .map(draft -> CTP_TARGET_CLIENT.cartDiscounts().create(draft).execute())
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();

    final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    // Update cart discount drafts to target project with different stacking mode (STACKING)
    final CartDiscountSyncStatistics syncStatistics =
        cartDiscountSync.sync(cartDiscountDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    assertThat(totalTime)
        .withFailMessage(
            format(
                THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                CART_DISCOUNT_BENCHMARKS_UPDATE_ACTION_THRESHOLD))
        .isLessThan(CART_DISCOUNT_BENCHMARKS_UPDATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (number of updated cart discount)
    final Integer totalNumberOfUpdatedCartDiscounts =
        CTP_TARGET_CLIENT
            .cartDiscounts()
            .get()
            .withWhere("stackingMode = \"Stacking\"")
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(CartDiscountPagedQueryResponse::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfUpdatedCartDiscounts).isEqualTo(NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert actual state of CTP project (total number of existing cart discounts)
    final Integer totalNumberOfCartDiscounts =
        CTP_TARGET_CLIENT
            .cartDiscounts()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(CartDiscountPagedQueryResponse::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfCartDiscounts).isEqualTo(NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert statistics
    assertThat(syncStatistics)
        .hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, 0, NUMBER_OF_RESOURCE_UNDER_TEST, 0);

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    if (SUBMIT_BENCHMARK_RESULT) {
      saveNewResult(CART_DISCOUNT_SYNC, UPDATES_ONLY, totalTime);
    }
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
                .map(draft -> CTP_TARGET_CLIENT.cartDiscounts().create(draft).execute())
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
        cartDiscountSync.sync(cartDiscountDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    assertThat(totalTime)
        .withFailMessage(
            format(
                THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                CART_DISCOUNT_BENCHMARKS_UPDATE_ACTION_THRESHOLD))
        .isLessThan(CART_DISCOUNT_BENCHMARKS_UPDATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (number of updated cart discount)
    final Integer totalNumberOfUpdatedCartDiscounts =
        CTP_TARGET_CLIENT
            .cartDiscounts()
            .get()
            .withWhere("stackingMode = \"StopAfterThisDiscount\"")
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(CartDiscountPagedQueryResponse::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfUpdatedCartDiscounts).isEqualTo(0);

    // Assert actual state of CTP project (total number of existing cart discounts)
    final Integer totalNumberOfCartDiscounts =
        CTP_TARGET_CLIENT
            .cartDiscounts()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(CartDiscountPagedQueryResponse::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfCartDiscounts).isEqualTo(NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert statistics
    assertThat(syncStatistics)
        .hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, halfNumberOfDrafts, halfNumberOfDrafts, 0);

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    if (SUBMIT_BENCHMARK_RESULT) {
      saveNewResult(CART_DISCOUNT_SYNC, CREATES_AND_UPDATES, totalTime);
    }
  }

  @Nonnull
  private static List<CartDiscountDraft> buildCartDiscountDrafts(final int numberOfCartDiscounts) {
    final List<String> sortOrders = getSortOrders(numberOfCartDiscounts);
    return IntStream.range(0, numberOfCartDiscounts)
        .mapToObj(
            i ->
                CartDiscountDraftBuilder.of()
                    .name(CART_DISCOUNT_NAME_1)
                    .cartPredicate(CART_DISCOUNT_CART_PREDICATE_1)
                    .value(CART_DISCOUNT_VALUE_DRAFT_1)
                    .target(CART_DISCOUNT_TARGET_1)
                    .sortOrder(sortOrders.get(i))
                    .requiresDiscountCode(false)
                    .key(format("key__%d", i))
                    .isActive(false)
                    .description(ofEnglish(format("description__%d", i)))
                    .validFrom(JANUARY_FROM)
                    .validUntil(JANUARY_UNTIL)
                    .build())
        .collect(toList());
  }
}
