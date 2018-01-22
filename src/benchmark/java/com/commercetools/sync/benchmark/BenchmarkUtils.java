package com.commercetools.sync.benchmark;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BenchmarkUtils {
    public static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;
    public static final String BENCHMARK_RESULTS_FILE_PATH = "benchmarks.json";
    public static final String TRAVIS_TEMP_GIR_DIR_PATH = System.getenv("TRAVIS_BUILD_DIR") + "/git_temp_dir/";
    public static final String VERSION_M8 = "v1.0.0-M8";
    public static final String PRODUCT_SYNC = "productSync";
    public static final String INVENTORY_SYNC = "inventorySync";
    public static final String CATEGORY_SYNC = "categorySync";
    public static final String CREATES_ONLY = "createsOnly";
    public static final String UPDATES_ONLY = "updatesOnly";
    public static final String CREATES_AND_UPDATES = "mix";
    public static final String EXECUTION_TIMES = "executionTimes";
    public static final String AVERAGE = "average";
    public static final String DIFF = "diff";
}
