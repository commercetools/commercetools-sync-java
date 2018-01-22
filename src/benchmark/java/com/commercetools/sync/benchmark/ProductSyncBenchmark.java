package com.commercetools.sync.benchmark;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.commercetools.sync.benchmark.BenchmarkUtils.BENCHMARK_RESULTS_FILE_PATH;
import static com.commercetools.sync.benchmark.BenchmarkUtils.CATEGORY_SYNC;
import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.EXECUTION_TIMES;
import static com.commercetools.sync.benchmark.BenchmarkUtils.PRODUCT_SYNC;
import static com.commercetools.sync.benchmark.BenchmarkUtils.UTF8_CHARSET;
import static com.commercetools.sync.benchmark.BenchmarkUtils.VERSION_M8;

public class ProductSyncBenchmark {

    @Test
    @SuppressWarnings("PMD")
    public void product_benchmark() {
        try {
            final String resultsAsString = getFileContent(BENCHMARK_RESULTS_FILE_PATH);
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode actualObject = mapper.readTree(resultsAsString);


            final List<JsonNode> results = iteratorToList(actualObject.get(VERSION_M8)
                                                                      .get(PRODUCT_SYNC)
                                                                      .get(CREATES_ONLY)
                                                                      .get(EXECUTION_TIMES)
                                                                      .elements());

            final double averageResults = getAverageResults(results);
            System.out.println(averageResults);

            final JsonNode newResult = addNewResult(actualObject, VERSION_M8, CATEGORY_SYNC, CREATES_ONLY, 1891);

            //replaceInFile(TRAVIS_TEMP_GIR_DIR_PATH + BENCHMARK_RESULTS_FILE_PATH, "#test", "auto injected from product benchmark");
            writeToFile(newResult.toString(), BENCHMARK_RESULTS_FILE_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JsonNode addNewResult(@Nonnull final JsonNode originalRoot,
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

        final JsonNode newResultsNode = benchmarkNode
            .set(EXECUTION_TIMES, JsonNodeFactory.instance.arrayNode().addAll(results));
        final JsonNode newSyncNode = syncNode.set(benchmark, newResultsNode);
        final JsonNode newVersionNode = versionNode.set(sync, newSyncNode);
        final JsonNode newRoot = rootNode.set(version, newVersionNode);
        return newRoot;
    }

    private static <T> List<T> iteratorToList(@Nonnull final Iterator<T> iterator) {
        final List<T> list = new ArrayList<>();
        iterator.forEachRemaining(list::add);
        return list;
    }

    private static double getAverageResults(@Nonnull final List<JsonNode> results) {
        return results.stream().mapToDouble(JsonNode::asLong).average().orElse(0);
    }


    private static String getFileContent(@Nonnull final String path) throws IOException {
        final byte[] fileBytes = Files.readAllBytes(Paths.get(path));
        return new String(fileBytes, UTF8_CHARSET);
    }

    private static void writeToFile(@Nonnull final String content, @Nonnull final String path) throws IOException {
        Files.write(Paths.get(path), content.getBytes(UTF8_CHARSET));
    }

    private static void replaceInFile(@Nonnull final String path, @Nonnull final String text, @Nonnull final String replacement) throws IOException {
        final String contentAfterReplacement = getFileContent(path).replaceAll(text, replacement);
        writeToFile(contentAfterReplacement, path);
    }
}
