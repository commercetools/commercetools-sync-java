package com.commercetools.sync.benchmark;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.io.IOException;

import static com.commercetools.sync.benchmark.BenchmarkUtils.CATEGORY_SYNC;
import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_AND_UPDATES;
import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.UPDATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.saveNewResult;
@Disabled
class CategorySyncBenchmark {

    @Test
    void sync_NewCategories_ShouldCreateCategories() throws IOException {
        // TODO: SHOULD BE IMPLEMENTED.
        saveNewResult(CATEGORY_SYNC, CREATES_ONLY, 20000);
    }

    @Test
    void sync_ExistingCategories_ShouldUpdateCategories() throws IOException {
        // TODO: SHOULD BE IMPLEMENTED.
        saveNewResult(CATEGORY_SYNC, UPDATES_ONLY, 10000);
    }

    @Test
    void sync_WithSomeExistingCategories_ShouldSyncCategories() throws IOException {
        // TODO: SHOULD BE IMPLEMENTED.
        saveNewResult(CATEGORY_SYNC, CREATES_AND_UPDATES, 30000);
    }

}
