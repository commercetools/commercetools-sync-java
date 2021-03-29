package com.commercetools.sync.benchmark;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.annotation.Nonnull;

final class BenchmarkUtils {
  private static final String BENCHMARK_RESULTS_FILE_NAME = "benchmarks.json";
  private static final String BENCHMARK_RESULTS_FILE_DIR =
      ofNullable(System.getProperty("user.dir"))
          .map(path -> path + "/tmp_git_dir/benchmarks/")
          .orElse("");
  private static final String BENCHMARK_RESULTS_FILE_PATH =
      BENCHMARK_RESULTS_FILE_DIR + BENCHMARK_RESULTS_FILE_NAME;
  private static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;
  private static final String EXECUTION_TIME = "executionTime";
  private static final String BRANCH_NAME =
      ofNullable(System.getenv("GITHUB_ACTION_COMMIT"))
          .map(commitMessage -> commitMessage.substring(0, 7)) // Use smaller commit sha
          .orElse("dev-local");

  static final String PRODUCT_SYNC = "productSync";
  static final String INVENTORY_SYNC = "inventorySync";
  static final String CATEGORY_SYNC = "categorySync";
  static final String TYPE_SYNC = "typeSync";
  static final String PRODUCT_TYPE_SYNC = "productTypeSync";
  static final String CART_DISCOUNT_SYNC = "cartDiscountSync";
  static final String CREATES_ONLY = "createsOnly";
  static final String UPDATES_ONLY = "updatesOnly";
  static final String CREATES_AND_UPDATES = "mix";
  static final int NUMBER_OF_RESOURCE_UNDER_TEST = 1000;
  static final String THRESHOLD_EXCEEDED_ERROR =
      "Total execution time of benchmark '%d' took longer than allowed" + " threshold of '%d'.";

  static void saveNewResult(
      @Nonnull final String sync, @Nonnull final String benchmark, final double newResult)
      throws IOException {

    final JsonNode rootNode = new ObjectMapper().readTree(getFileContent());
    final JsonNode withNewResult = addNewResult(rootNode, sync, benchmark, newResult);
    writeToFile(withNewResult.toString());
  }

  @Nonnull
  private static String getFileContent() throws IOException {

    final byte[] fileBytes = Files.readAllBytes(Paths.get(BENCHMARK_RESULTS_FILE_PATH));
    return new String(fileBytes, UTF8_CHARSET);
  }

  @Nonnull
  private static JsonNode addNewResult(
      @Nonnull final JsonNode originalRoot,
      @Nonnull final String sync,
      @Nonnull final String benchmark,
      final double newResult) {

    ObjectNode rootNode = (ObjectNode) originalRoot;
    ObjectNode branchNode = (ObjectNode) rootNode.get(BRANCH_NAME);

    // If version doesn't exist yet, create a new JSON object for the new version.
    if (branchNode == null) {
      branchNode = createVersionNode();
      rootNode.set(BRANCH_NAME, branchNode);
    }

    final ObjectNode syncNode = (ObjectNode) branchNode.get(sync);
    final ObjectNode benchmarkNode = (ObjectNode) syncNode.get(benchmark);

    // Add new result.
    benchmarkNode.set(EXECUTION_TIME, JsonNodeFactory.instance.numberNode(newResult));
    return rootNode;
  }

  @Nonnull
  private static ObjectNode createVersionNode() {

    final ObjectNode newVersionNode = JsonNodeFactory.instance.objectNode();
    newVersionNode.set(PRODUCT_SYNC, createSyncNode());
    newVersionNode.set(INVENTORY_SYNC, createSyncNode());
    newVersionNode.set(CATEGORY_SYNC, createSyncNode());
    newVersionNode.set(PRODUCT_TYPE_SYNC, createSyncNode());
    newVersionNode.set(TYPE_SYNC, createSyncNode());
    newVersionNode.set(CART_DISCOUNT_SYNC, createSyncNode());
    return newVersionNode;
  }

  @Nonnull
  private static ObjectNode createSyncNode() {

    final ObjectNode newSyncNode = JsonNodeFactory.instance.objectNode();
    newSyncNode.set(CREATES_ONLY, createBenchmarkNode());
    newSyncNode.set(UPDATES_ONLY, createBenchmarkNode());
    newSyncNode.set(CREATES_AND_UPDATES, createBenchmarkNode());

    return newSyncNode;
  }

  @Nonnull
  private static ObjectNode createBenchmarkNode() {

    final ObjectNode newBenchmarkNode = JsonNodeFactory.instance.objectNode();
    newBenchmarkNode.set(EXECUTION_TIME, JsonNodeFactory.instance.numberNode(0));

    return newBenchmarkNode;
  }

  private static void writeToFile(@Nonnull final String content) throws IOException {
    Files.write(Paths.get(BENCHMARK_RESULTS_FILE_PATH), content.getBytes(UTF8_CHARSET));
  }

  private BenchmarkUtils() {}
}
