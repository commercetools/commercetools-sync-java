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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

public class BenchmarkUtils {
    private static final String BENCHMARK_RESULTS_FILE_NAME = "benchmarks.json";
    private static final String BENCHMARK_RESULTS_FILE_DIR = ofNullable(System.getenv("CI_BUILD_DIR"))
        .map(path -> path + "/tmp_git_dir/benchmarks/").orElse("");
    private static final String BENCHMARK_RESULTS_FILE_PATH = BENCHMARK_RESULTS_FILE_DIR + BENCHMARK_RESULTS_FILE_NAME;
    private static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;
    private static final String EXECUTION_TIMES = "executionTimes";
    private static final String AVERAGE = "average";
    private static final String DIFF = "diff";

    static final String PRODUCT_SYNC = "productSync";
    static final String INVENTORY_SYNC = "inventorySync";
    static final String CATEGORY_SYNC = "categorySync";
    static final String CREATES_ONLY = "createsOnly";
    static final String UPDATES_ONLY = "updatesOnly";
    static final String CREATES_AND_UPDATES = "mix";
    static final int THRESHOLD = 120000; //120 seconds in milliseconds
    static final int NUMBER_OF_RESOURCE_UNDER_TEST = 1;


    static void saveNewResult(@Nonnull final String version,
                              @Nonnull final String sync,
                              @Nonnull final String benchmark,
                              final double newResult) throws IOException {

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

        // If version doesn't exist yet, create a new JSON object for the new version.
        if (versionNode == null) {
            rootNode = createVersionNode(rootNode, version);
            versionNode = (ObjectNode) rootNode.get(version);
        }

        final ObjectNode syncNode = (ObjectNode) versionNode.get(sync);
        final ObjectNode benchmarkNode = (ObjectNode) syncNode.get(benchmark);

        // Get current list of execution times for the specified benchmark of the specified sync module
        // of the specified version.
        final List<JsonNode> results = toList(benchmarkNode.get(EXECUTION_TIMES).elements());

        // Add new result.
        results.add(JsonNodeFactory.instance.numberNode(newResult));
        benchmarkNode.set(EXECUTION_TIMES, JsonNodeFactory.instance.arrayNode().addAll(results));

        // Compute new average and add to JSON Object
        final double averageResult = calculateAvg(results);
        benchmarkNode.set(AVERAGE, JsonNodeFactory.instance.numberNode(averageResult));

        // Compute new diff from the last version.
        final double diff = calculateDiff(rootNode, version, sync, benchmark, averageResult);
        benchmarkNode.set(DIFF, JsonNodeFactory.instance.numberNode(diff));

        final JsonNode newSyncNode = syncNode.set(benchmark, benchmarkNode);
        final JsonNode newVersionNode = versionNode.set(sync, newSyncNode);
        final JsonNode newRoot = rootNode.set(version, newVersionNode);
        return newRoot;
    }

    @Nonnull
    private static <T> List<T> toList(@Nonnull final Iterator<T> iterator) {
        return toStream(iterator).collect(Collectors.toList());
    }

    @Nonnull
    private static <T> Stream<T> toStream(@Nonnull final Iterator<T> iterator) {
        return stream(spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
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
        final JsonNode rootNode = new ObjectMapper().readTree(getFileContent(BENCHMARK_RESULTS_FILE_PATH));
        return calculateDiff(rootNode, version, sync, benchmark, average);
    }

    private static double calculateDiff(@Nonnull final JsonNode originalRoot,
                                @Nonnull final String version,
                                @Nonnull final String sync,
                                @Nonnull final String benchmark,
                                final double average) {
        return getLatestVersionName(originalRoot, version)
            .map(latestVersionName -> originalRoot.get(latestVersionName)
                                                  .get(sync)
                                                  .get(benchmark)
                                                  .get(AVERAGE))
            .map(latestAverageNode -> average - latestAverageNode.asDouble())
            // if there is no latest version - the current average is the diff.
            .orElse(average);
    }

    private static Optional<String> getLatestVersionName(@Nonnull final JsonNode originalRoot,
                                                         @Nonnull final String currentVersionName) {
        return toStream(originalRoot.fieldNames()).reduce((firstVersion, secondVersion) ->
            !currentVersionName.equals(secondVersion) ? secondVersion : firstVersion);
    }


    private static String getFileContent(@Nonnull final String path) throws IOException {
        final byte[] fileBytes = Files.readAllBytes(Paths.get(path));
        return new String(fileBytes, UTF8_CHARSET);
    }

    private static void writeToFile(@Nonnull final String content, @Nonnull final String path) throws IOException {
        Files.write(Paths.get(path), content.getBytes(UTF8_CHARSET));
    }
}
