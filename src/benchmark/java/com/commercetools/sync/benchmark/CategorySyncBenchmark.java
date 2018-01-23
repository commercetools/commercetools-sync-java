package com.commercetools.sync.benchmark;

import com.commercetools.sync.commons.utils.SyncSolutionInfo;
import org.junit.Test;

import java.io.IOException;

import static com.commercetools.sync.benchmark.BenchmarkUtils.CATEGORY_SYNC;
import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_AND_UPDATES;
import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.UPDATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.saveNewResult;

public class CategorySyncBenchmark {

    @Test
    public void sync_NewCategories_ShouldCreateCategories() throws IOException {
        // TODO: SHOULD BE IMPLEMENTED.
        saveNewResult(SyncSolutionInfo.LIB_VERSION, CATEGORY_SYNC, CREATES_ONLY, 20000);
    }

    @Test
    public void sync_ExistingCategories_ShouldUpdateCategories() throws IOException {
        // TODO: SHOULD BE IMPLEMENTED.
        saveNewResult(SyncSolutionInfo.LIB_VERSION, CATEGORY_SYNC, UPDATES_ONLY, 10000);
    }

    @Test
    public void sync_WithSomeExistingCategories_ShouldSyncCategories() throws IOException {
        // TODO: SHOULD BE IMPLEMENTED.
        saveNewResult(SyncSolutionInfo.LIB_VERSION, CATEGORY_SYNC, CREATES_AND_UPDATES, 30000);
    }

}
