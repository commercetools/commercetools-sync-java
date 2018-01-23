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

import static java.util.Optional.ofNullable;

public class BenchmarkUtils {
    public static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;
    public static final String BENCHMARK_RESULTS_FILE_NAME = "benchmarks.json";
    public static final String BENCHMARK_RESULTS_FILE_DIR = ofNullable(System.getenv("TRAVIS_BUILD_DIR"))
        .map(path -> path + "/tmp_git_dir/benchmarks/").orElse("");
    public static final String BENCHMARK_RESULTS_FILE_PATH = BENCHMARK_RESULTS_FILE_DIR + BENCHMARK_RESULTS_FILE_NAME;
    public static final String PRODUCT_SYNC = "productSync";
    public static final String INVENTORY_SYNC = "inventorySync";
    public static final String CATEGORY_SYNC = "categorySync";
    public static final String CREATES_ONLY = "createsOnly";
    public static final String UPDATES_ONLY = "updatesOnly";
    public static final String CREATES_AND_UPDATES = "mix";
    public static final String EXECUTION_TIMES = "executionTimes";
    public static final String AVERAGE = "average";
    public static final String DIFF = "diff";
    public static double THRESHOLD = 120000; //120 seconds in milliseconds
    public static int NUMBER_OF_RESOURCE_UNDER_TEST = 10000;


    static void saveNewResult(@Nonnull final String version,
                              @Nonnull final String sync,
                              @Nonnull final String benchmark,
                              final double newResult) throws IOException {
        // Add new result, calculate average and persist new JSON.
        final JsonNode rootNode = new ObjectMapper().readTree(getFileContent(BENCHMARK_RESULTS_FILE_PATH));
        final JsonNode withNewResult = addNewResult(rootNode, version, sync, benchmark, newResult);
        writeToFile(withNewResult.toString(), BENCHMARK_RESULTS_FILE_PATH);
    }

    private static JsonNode addNewResult(@Nonnull final JsonNode originalRoot,
                                         @Nonnull final String version,
                                         @Nonnull final String sync,
                                         @Nonnull final String benchmark,
                                         final double newResult) {
        ObjectNode rootNode = (ObjectNode) originalRoot;
        ObjectNode versionNode = (ObjectNode) rootNode.get(version);

        if (versionNode == null) {
            rootNode = createVersionNode(rootNode, version);
            versionNode = (ObjectNode) rootNode.get(version);
        }

        final ObjectNode syncNode = (ObjectNode) versionNode.get(sync);
        final ObjectNode benchmarkNode = (ObjectNode) syncNode.get(benchmark);

        final List<JsonNode> results = iteratorToList(benchmarkNode.get(EXECUTION_TIMES).elements());

        // Add newResult
        results.add(JsonNodeFactory.instance.numberNode(newResult));
        benchmarkNode.set(EXECUTION_TIMES, JsonNodeFactory.instance.arrayNode().addAll(results));

        // Add new average
        final double averageResult = calculateAvg(results);
        benchmarkNode.set(AVERAGE, JsonNodeFactory.instance.numberNode(averageResult));

        // Add new diff
        final double diff = calculateDiff(rootNode, version, sync, benchmark, averageResult);
        benchmarkNode.set(DIFF, JsonNodeFactory.instance.numberNode(diff));

        final JsonNode newSyncNode = syncNode.set(benchmark, benchmarkNode);
        final JsonNode newVersionNode = versionNode.set(sync, newSyncNode);
        final JsonNode newRoot = rootNode.set(version, newVersionNode);
        return newRoot;
    }

    private static <T> List<T> iteratorToList(@Nonnull final Iterator<T> iterator) {
        final List<T> list = new ArrayList<>();
        iterator.forEachRemaining(list::add);
        return list;
    }

    private static double calculateAvg(@Nonnull final List<JsonNode> results) {
        return results.stream().mapToDouble(JsonNode::asLong).average().orElse(0);
    }

    private static ObjectNode createVersionNode(@Nonnull final ObjectNode rootNode, @Nonnull final String version) {
        final ObjectNode newVersionNode = createSyncNode(
            createSyncNode(createSyncNode(JsonNodeFactory.instance.objectNode(),
                PRODUCT_SYNC), INVENTORY_SYNC), CATEGORY_SYNC);
        return (ObjectNode) rootNode.set(version, newVersionNode);
    }

    private static ObjectNode createSyncNode(@Nonnull final ObjectNode versionNode,
                                             @Nonnull final String sync) {
        final ObjectNode newSyncNode = createBenchmarkNode(
            createBenchmarkNode(
                createBenchmarkNode(JsonNodeFactory.instance.objectNode(), CREATES_ONLY), UPDATES_ONLY),
            CREATES_AND_UPDATES);
        return (ObjectNode) versionNode.set(sync, newSyncNode);
    }

    private static ObjectNode createBenchmarkNode(@Nonnull final ObjectNode syncNode,
                                                  @Nonnull final String benchmark) {
        final ObjectNode newBenchmarkNode = JsonNodeFactory.instance.objectNode();
        newBenchmarkNode.set(EXECUTION_TIMES, JsonNodeFactory.instance.arrayNode());
        newBenchmarkNode.set(AVERAGE, JsonNodeFactory.instance.numberNode(0));
        newBenchmarkNode.set(DIFF, JsonNodeFactory.instance.numberNode(0));

        syncNode.set(benchmark, newBenchmarkNode);
        return syncNode;
    }

    static double calculateDiff(@Nonnull final String version,
                                @Nonnull final String sync,
                                @Nonnull final String benchmark,
                                final double average) throws IOException {
        // Add new result, calculate average and persist new JSON.
        final JsonNode rootNode = new ObjectMapper().readTree(getFileContent(BENCHMARK_RESULTS_FILE_PATH));
        return calculateDiff(rootNode, version, sync, benchmark, average);
    }

    private static double calculateDiff(@Nonnull final JsonNode originalRoot,
                                @Nonnull final String version,
                                @Nonnull final String sync,
                                @Nonnull final String benchmark,
                                final double average) {
        return getLatestVersionName(originalRoot, version).map(latestVersionName ->
            originalRoot.get(latestVersionName).get(sync).get(benchmark).get(AVERAGE))
                                                 .map(latestAverageNode -> average - latestAverageNode.asDouble())
                                                 .orElse(average);
    }

    private static Optional<String> getLatestVersionName(@Nonnull final JsonNode originalRoot,
                                                         @Nonnull final String currentVersionName) {
        String latestVersion = null;
        final Iterator<String> versionIterator = originalRoot.fieldNames();
        while (versionIterator.hasNext()) {
            final String version = versionIterator.next();
            if (!currentVersionName.equals(version)) {
                latestVersion = version;
            }
        }
        return ofNullable(latestVersion);
    }


    private static String getFileContent(@Nonnull final String path) throws IOException {
        final byte[] fileBytes = Files.readAllBytes(Paths.get(path));
        return new String(fileBytes, UTF8_CHARSET);
    }

    private static void writeToFile(@Nonnull final String content, @Nonnull final String path) throws IOException {
        Files.write(Paths.get(path), content.getBytes(UTF8_CHARSET));
    }
}
