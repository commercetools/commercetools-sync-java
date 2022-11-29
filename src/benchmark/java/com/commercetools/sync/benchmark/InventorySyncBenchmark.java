package com.commercetools.sync.benchmark;

import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_AND_UPDATES;
import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.INVENTORY_SYNC;
import static com.commercetools.sync.benchmark.BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST;
import static com.commercetools.sync.benchmark.BenchmarkUtils.SUBMIT_BENCHMARK_RESULT;
import static com.commercetools.sync.benchmark.BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR;
import static com.commercetools.sync.benchmark.BenchmarkUtils.UPDATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.saveNewResult;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ChannelITUtils.deleteChannels;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.deleteInventoryEntries;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.inventories.InventorySync;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class InventorySyncBenchmark {

  private static final int INVENTORY_BENCHMARKS_CREATE_ACTION_THRESHOLD =
      18_000; // (based on history of benchmarks; highest was ~9 seconds)

  @BeforeEach
  void setup() {
    deleteInventoryEntries(CTP_TARGET_CLIENT);
    deleteTypes(CTP_TARGET_CLIENT);
    deleteChannels(CTP_TARGET_CLIENT);
  }

  @AfterAll
  static void tearDown() {
    deleteInventoryEntries(CTP_TARGET_CLIENT);
    deleteTypes(CTP_TARGET_CLIENT);
    deleteChannels(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_NewInventories_ShouldCreateInventories() throws IOException {
    // preparation
    final List<InventoryEntryDraft> inventoryEntryDrafts =
        buildInventoryDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);
    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

    // benchmark
    final long beforeSync = System.currentTimeMillis();
    final InventorySyncStatistics inventorySyncStatistics =
        executeBlocking(inventorySync.sync(inventoryEntryDrafts));
    final long totalTime = System.currentTimeMillis() - beforeSync;

    // assert on threshold
    assertThat(totalTime)
        .withFailMessage(
            format(
                THRESHOLD_EXCEEDED_ERROR, totalTime, INVENTORY_BENCHMARKS_CREATE_ACTION_THRESHOLD))
        .isLessThan(INVENTORY_BENCHMARKS_CREATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (total number of existing inventories)
    final CompletableFuture<Integer> totalNumberOfInventories =
        CTP_TARGET_CLIENT
            .execute(InventoryEntryQuery.of())
            .thenApply(PagedQueryResult::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture();

    executeBlocking(totalNumberOfInventories);
    assertThat(totalNumberOfInventories).isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert on sync statistics
    assertThat(inventorySyncStatistics)
        .hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, NUMBER_OF_RESOURCE_UNDER_TEST, 0, 0);
    if (SUBMIT_BENCHMARK_RESULT) {
      saveNewResult(INVENTORY_SYNC, CREATES_ONLY, totalTime);
    }
  }

  @Disabled
  @Test
  void sync_ExistingInventories_ShouldUpdateInventories() throws IOException {
    // TODO: SHOULD BE IMPLEMENTED.
    saveNewResult(INVENTORY_SYNC, UPDATES_ONLY, 50000);
  }

  @Disabled
  @Test
  void sync_WithSomeExistingInventories_ShouldSyncInventories() throws IOException {
    // TODO: SHOULD BE IMPLEMENTED.
    saveNewResult(INVENTORY_SYNC, CREATES_AND_UPDATES, 30000);
  }

  @Nonnull
  private List<InventoryEntryDraft> buildInventoryDrafts(final int numberOfDrafts) {
    final ZonedDateTime expectedDelivery =
        ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));

    final List<InventoryEntryDraft> resourceDrafts = new ArrayList<>();
    for (int i = 0; i < numberOfDrafts; i++) {
      resourceDrafts.add(
          InventoryEntryDraftBuilder.of("sku_" + i, 1L, expectedDelivery, 1, null).build());
    }
    return resourceDrafts;
  }
}
