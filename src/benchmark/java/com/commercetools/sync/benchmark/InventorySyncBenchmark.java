package com.commercetools.sync.benchmark;

import com.commercetools.sync.commons.utils.SyncSolutionInfo;
import com.commercetools.sync.inventories.InventorySync;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.INVENTORY_SYNC;
import static com.commercetools.sync.benchmark.BenchmarkUtils.THRESHOLD;
import static com.commercetools.sync.benchmark.BenchmarkUtils.calculateDiff;
import static com.commercetools.sync.benchmark.BenchmarkUtils.saveNewResult;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.deleteInventoryEntries;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.deleteSupplyChannels;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;


public class InventorySyncBenchmark {
    @Before
    public void setup() {
        deleteInventoryEntries(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
        deleteSupplyChannels(CTP_TARGET_CLIENT);
    }

    @AfterClass
    public static void tearDown() {
        deleteInventoryEntries(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
        deleteSupplyChannels(CTP_TARGET_CLIENT);
    }


    @Test
    public void sync_NewInventories_ShouldCreateInventories() throws IOException {
        final int numberOfInventories = 10000;
        final List<InventoryEntryDraft> inventoryEntryDrafts = buildInventoryDrafts(numberOfInventories);


        final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                                     .build();
        final InventorySync inventorySync = new InventorySync(inventorySyncOptions);


        final long beforeSync = System.currentTimeMillis();
        final InventorySyncStatistics inventorySyncStatistics =
            executeBlocking(inventorySync.sync(inventoryEntryDrafts));
        final long totalTime = System.currentTimeMillis() - beforeSync;

        assertThat(inventorySyncStatistics).hasValues(numberOfInventories, numberOfInventories, 0, 0);

        final double diff = calculateDiff(SyncSolutionInfo.LIB_VERSION, INVENTORY_SYNC, CREATES_ONLY, totalTime);
        assertThat(diff).isLessThanOrEqualTo(THRESHOLD)
                        .withFailMessage(format("Diff of benchmark '%e' is longer than expected"
                            + " threshold of '%e'.", diff, THRESHOLD));
        saveNewResult(SyncSolutionInfo.LIB_VERSION, INVENTORY_SYNC, CREATES_ONLY, totalTime);
    }

    @Nonnull
    private List<InventoryEntryDraft> buildInventoryDrafts(final int numberOfDrafts) {
        final ZonedDateTime expectedDelivery = ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));

        final List<InventoryEntryDraft> resourceDrafts = new ArrayList<>();
        for (int i = 0; i < numberOfDrafts; i++) {
            resourceDrafts.add(
                InventoryEntryDraftBuilder.of("sku_" + i, 1L, expectedDelivery, 1, null)
                                          .build());
        }
        return resourceDrafts;
    }
}
