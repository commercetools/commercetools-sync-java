package com.commercetools.sync.benchmark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class BenchmarkUtils {
    public static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;
    public static final String BENCHMARK_RESULTS_FILE_NAME = "benchmarks.json";
    public static final String BENCHMARK_RESULTS_FILE_DIR = Optional.of(System.getenv("TRAVIS_BUILD_DIR"))
                                                                    .map(path -> path + "/tmp_git_dir/")
                                                                    .orElse("");
    public static final String BENCHMARK_RESULTS_FILE_PATH = BENCHMARK_RESULTS_FILE_DIR + BENCHMARK_RESULTS_FILE_NAME;
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


    static void saveNewResult(@Nonnull final String version,
                              @Nonnull final String sync,
                              @Nonnull final String benchmark,
                              final double newResult) throws IOException {
            // Add new result, calculate average and persist new JSON.
            final JsonNode rootNode = new ObjectMapper().readTree(getFileContent(BENCHMARK_RESULTS_FILE_PATH));
            final JsonNode withNewResult = addNewResult(rootNode, version, sync, benchmark, newResult);

            final double averageResults = getBenchmarkAverage(withNewResult, version, sync, benchmark);
            final JsonNode withNewAverage = addAverage(withNewResult, version, sync, benchmark, averageResults);
            writeToFile(withNewAverage.toString(), BENCHMARK_RESULTS_FILE_PATH);
    }


    static JsonNode addNewResult(@Nonnull final JsonNode originalRoot,
                                 @Nonnull final String version,
                                 @Nonnull final String sync,
                                 @Nonnull final String benchmark,
                                 final double newResult) {

        final ObjectNode rootNode = (ObjectNode) originalRoot;
        final ObjectNode versionNode = (ObjectNode) rootNode.get(version);
        final ObjectNode syncNode = (ObjectNode) versionNode.get(sync);
        final ObjectNode benchmarkNode = (ObjectNode) syncNode.get(benchmark);

        final List<JsonNode> results = iteratorToList(benchmarkNode.get(EXECUTION_TIMES).elements());

        // Add newResult
        results.add(JsonNodeFactory.instance.numberNode(newResult));

        final JsonNode newBenchmarkNode = benchmarkNode
            .set(EXECUTION_TIMES, JsonNodeFactory.instance.arrayNode().addAll(results));
        final JsonNode newSyncNode = syncNode.set(benchmark, newBenchmarkNode);
        final JsonNode newVersionNode = versionNode.set(sync, newSyncNode);
        final JsonNode newRoot = rootNode.set(version, newVersionNode);
        return newRoot;
    }

    static JsonNode addAverage(@Nonnull final JsonNode originalRoot,
                               @Nonnull final String version,
                               @Nonnull final String sync,
                               @Nonnull final String benchmark,
                               final double average) {
        final ObjectNode rootNode = (ObjectNode) originalRoot;
        final ObjectNode versionNode = (ObjectNode) rootNode.get(version);
        final ObjectNode syncNode = (ObjectNode) versionNode.get(sync);
        final ObjectNode benchmarkNode = (ObjectNode) syncNode.get(benchmark);

        final JsonNode newBenchmarkNode = benchmarkNode.set(AVERAGE, JsonNodeFactory.instance.numberNode(average));
        final JsonNode newSyncNode = syncNode.set(benchmark, newBenchmarkNode);
        final JsonNode newVersionNode = versionNode.set(sync, newSyncNode);
        final JsonNode newRoot = rootNode.set(version, newVersionNode);
        return newRoot;
    }

    static <T> List<T> iteratorToList(@Nonnull final Iterator<T> iterator) {
        final List<T> list = new ArrayList<>();
        iterator.forEachRemaining(list::add);
        return list;
    }

    static double getBenchmarkAverage(@Nonnull final JsonNode originalRoot,
                                      @Nonnull final String version,
                                      @Nonnull final String sync,
                                      @Nonnull final String benchmark) {
        final List<JsonNode> results = iteratorToList(originalRoot.get(version)
                                                                  .get(sync)
                                                                  .get(benchmark)
                                                                  .get(EXECUTION_TIMES)
                                                                  .elements());
        return getAverageResults(results);
    }

    static double getAverageResults(@Nonnull final List<JsonNode> results) {
        return results.stream().mapToDouble(JsonNode::asLong).average().orElse(0);
    }


    static String getFileContent(@Nonnull final String path) throws IOException {
        final byte[] fileBytes = Files.readAllBytes(Paths.get(path));
        return new String(fileBytes, UTF8_CHARSET);
    }

    static void writeToFile(@Nonnull final String content, @Nonnull final String path) throws IOException {
        Files.write(Paths.get(path), content.getBytes(UTF8_CHARSET));
    }

    private static void replaceInFile(@Nonnull final String path, @Nonnull final String text, @Nonnull final String replacement) throws IOException {
        final String contentAfterReplacement = getFileContent(path).replaceAll(text, replacement);
        writeToFile(contentAfterReplacement, path);
    }
}
