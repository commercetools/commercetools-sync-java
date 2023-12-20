package com.commercetools.sync.benchmark;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.InventoryITUtils.deleteInventoryEntries;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.inventory.InventoryEntryDraftBuilder;
import com.commercetools.api.models.inventory.InventoryPagedQueryResponse;
import com.commercetools.sync.inventories.InventorySync;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventorySyncBenchmark {

  private static final int INVENTORY_BENCHMARKS_CREATE_ACTION_THRESHOLD =
      18_000; // (based on history of benchmarks; highest was ~9 seconds)
  private static final int INVENTORY_BENCHMARKS_UPDATE_ACTION_THRESHOLD =
      18_000; // (based on history of benchmarks; highest was ~9 seconds)
  private static final int INVENTORY_BENCHMARKS_CREATE_AND_UPDATE_ACTION_THRESHOLD =
      18_000; // (based on history of benchmarks; highest was ~9 seconds)

  @BeforeEach
  void setup() {
    deleteInventoryEntries(CTP_TARGET_CLIENT);
  }

  @AfterAll
  static void tearDown() {
    deleteInventoryEntries(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_NewInventories_ShouldCreateInventories() throws IOException {
    // preparation
    final List<InventoryEntryDraft> inventoryEntryDrafts =
        buildInventoryDrafts(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);
    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

    // benchmark
    final long beforeSync = System.currentTimeMillis();
    final InventorySyncStatistics inventorySyncStatistics =
        inventorySync.sync(inventoryEntryDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSync;

    // assert on threshold
    assertThat(totalTime)
        .withFailMessage(
            String.format(
                BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                INVENTORY_BENCHMARKS_CREATE_ACTION_THRESHOLD))
        .isLessThan(INVENTORY_BENCHMARKS_CREATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (total number of existing inventories)
    final Long totalNumberOfInventories =
        CTP_TARGET_CLIENT
            .inventory()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(InventoryPagedQueryResponse::getTotal)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfInventories).isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert on sync statistics
    assertThat(inventorySyncStatistics)
        .hasValues(
            BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST,
            BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST,
            0,
            0);
    if (BenchmarkUtils.SUBMIT_BENCHMARK_RESULT) {
      BenchmarkUtils.saveNewResult(
          BenchmarkUtils.INVENTORY_SYNC, BenchmarkUtils.CREATES_ONLY, totalTime);
    }
  }

  @Test
  void sync_ExistingInventories_ShouldUpdateInventories() throws IOException {
    // preparation
    final List<InventoryEntryDraft> inventoryEntryDrafts =
        buildInventoryDrafts(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);
    // Create drafts to target project with different quantity
    CompletableFuture.allOf(
            inventoryEntryDrafts.stream()
                .map(InventoryEntryDraftBuilder::of)
                .map(builder -> builder.quantityOnStock(0L))
                .map(InventoryEntryDraftBuilder::build)
                .map(draft -> CTP_TARGET_CLIENT.inventory().create(draft).execute())
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();

    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

    // benchmark
    final long beforeSync = System.currentTimeMillis();
    final InventorySyncStatistics inventorySyncStatistics =
        inventorySync.sync(inventoryEntryDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSync;

    // assert on threshold
    assertThat(totalTime)
        .withFailMessage(
            String.format(
                BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                INVENTORY_BENCHMARKS_UPDATE_ACTION_THRESHOLD))
        .isLessThan(INVENTORY_BENCHMARKS_UPDATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (total number of existing inventories)
    final Long totalNumberOfInventories =
        CTP_TARGET_CLIENT
            .inventory()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(InventoryPagedQueryResponse::getTotal)
            .toCompletableFuture()
            .join();
    assertThat(totalNumberOfInventories).isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert on sync statistics
    assertThat(inventorySyncStatistics)
        .hasValues(
            BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST,
            0,
            BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST,
            0);
    if (BenchmarkUtils.SUBMIT_BENCHMARK_RESULT) {
      BenchmarkUtils.saveNewResult(
          BenchmarkUtils.INVENTORY_SYNC, BenchmarkUtils.UPDATES_ONLY, totalTime);
    }
  }

  @Test
  void sync_WithSomeExistingInventories_ShouldSyncInventories() throws IOException {
    // preparation
    final List<InventoryEntryDraft> inventoryEntryDrafts =
        buildInventoryDrafts(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);
    final int halfNumberOfDrafts = inventoryEntryDrafts.size() / 2;
    final List<InventoryEntryDraft> firstHalf = inventoryEntryDrafts.subList(0, halfNumberOfDrafts);

    // Create first half of drafts to target project with different quantity
    CompletableFuture.allOf(
            firstHalf.stream()
                .map(InventoryEntryDraftBuilder::of)
                .map(builder -> builder.quantityOnStock(0L))
                .map(InventoryEntryDraftBuilder::build)
                .map(draft -> CTP_TARGET_CLIENT.inventory().post(draft).execute())
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();

    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

    // benchmark
    final long beforeSync = System.currentTimeMillis();
    final InventorySyncStatistics inventorySyncStatistics =
        inventorySync.sync(inventoryEntryDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSync;

    // assert on threshold
    assertThat(totalTime)
        .withFailMessage(
            String.format(
                BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                INVENTORY_BENCHMARKS_CREATE_AND_UPDATE_ACTION_THRESHOLD))
        .isLessThan(INVENTORY_BENCHMARKS_CREATE_AND_UPDATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (total number of existing inventories)
    final Long totalNumberOfInventories =
        CTP_TARGET_CLIENT
            .inventory()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(InventoryPagedQueryResponse::getTotal)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfInventories).isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert on sync statistics
    assertThat(inventorySyncStatistics)
        .hasValues(
            BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST,
            halfNumberOfDrafts,
            halfNumberOfDrafts,
            0);
    if (BenchmarkUtils.SUBMIT_BENCHMARK_RESULT) {
      BenchmarkUtils.saveNewResult(
          BenchmarkUtils.INVENTORY_SYNC, BenchmarkUtils.CREATES_AND_UPDATES, totalTime);
    }
  }

  @Nonnull
  private List<InventoryEntryDraft> buildInventoryDrafts(final int numberOfDrafts) {
    final ZonedDateTime expectedDelivery =
        ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));

    final List<InventoryEntryDraft> resourceDrafts = new ArrayList<>();
    for (int i = 0; i < numberOfDrafts; i++) {
      resourceDrafts.add(
          InventoryEntryDraftBuilder.of()
              .sku("sku_" + i)
              .quantityOnStock(1L)
              .expectedDelivery(expectedDelivery)
              .restockableInDays(1L)
              .build());
    }
    return resourceDrafts;
  }
}
