package com.commercetools.sync.sdk2.commons;

import static java.lang.String.format;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLRequestBuilder;
import com.commercetools.api.models.graph_ql.GraphQLVariablesMap;
import com.commercetools.api.models.graph_ql.GraphQLVariablesMapBuilder;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.services.impl.UnresolvedReferencesServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.NotFoundException;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CleanupUnresolvedReferenceCustomObjects {

  private final ProjectApiRoot ctpClient;
  private final Statistics statistics;
  private int pageSize = 500;
  private Consumer<Throwable> errorCallback;

  private CleanupUnresolvedReferenceCustomObjects(@Nonnull final ProjectApiRoot ctpClient) {
    this.ctpClient = ctpClient;
    this.statistics = new Statistics();
  }

  /**
   * Creates new instance of {@link CleanupUnresolvedReferenceCustomObjects} which has the
   * functionality to run cleanup helpers.
   *
   * @param ctpClient the client object.
   * @return new instance of {@link CleanupUnresolvedReferenceCustomObjects}
   */
  public static CleanupUnresolvedReferenceCustomObjects of(
      @Nonnull final ProjectApiRoot ctpClient) {
    return new CleanupUnresolvedReferenceCustomObjects(ctpClient);
  }

  /**
   * Sets the {@code errorCallback} function of the cleanup. This callback will be called whenever
   * an event occurs that leads to an error alert from the cleanup process.
   *
   * @param errorCallback the new value to set to the error callback.
   * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
   */
  public CleanupUnresolvedReferenceCustomObjects errorCallback(
      @Nonnull final Consumer<Throwable> errorCallback) {
    this.errorCallback = errorCallback;
    return this;
  }

  /**
   * Given an {@code exception}, this method calls the {@code errorCallback} function. If {@code
   * errorCallback} is null, this method does nothing.
   *
   * @param exception {@link Throwable} instance to supply as first param to the {@code
   *     errorCallback} function.
   */
  private void applyErrorCallback(@Nonnull final Throwable exception) {
    if (this.errorCallback != null) {
      this.errorCallback.accept(exception);
    }
  }

  /**
   * Configures the pageSize (limit), the maximum number of results to return from the grapqhl
   * query, which can be set using the limit query parameter. The default page size is 500.
   *
   * <p>NOTE: Changing this value might negatively impact the performance of the cleanup and must be
   * tested properly.
   *
   * @param pageSize int that indicates batch size of resources to process.
   * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
   */
  public CleanupUnresolvedReferenceCustomObjects pageSize(final int pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  /**
   * Deletes the unresolved reference custom objects persisted by commercetools-sync-java library to
   * handle reference resolution. The custom objects will be deleted if it hasn't been modified for
   * the specified amount of days as given {@code deleteDaysAfterLastModification}.
   *
   * <p>Note: Keeping the unresolved references forever can negatively influence the performance of
   * your project, so deleting unused data ensures the best performance for your project.
   *
   * @param deleteDaysAfterLastModification Days to query. The custom objects will be deleted if it
   *     hasn't been modified for the specified amount of days.
   * @return an instance of {@link CompletableFuture}&lt;{@link Statistics} &gt; which contains the
   *     processing time, the total number of custom objects that were deleted and failed to delete,
   *     and a proper summary message of the statistics.
   */
  public CompletableFuture<Statistics> cleanup(final int deleteDaysAfterLastModification) {

    return CompletableFuture.allOf(
            cleanupUnresolvedProductReferences(deleteDaysAfterLastModification),
            cleanupUnresolvedParentCategoryReferences(deleteDaysAfterLastModification),
            cleanupUnresolvedStateReferences(deleteDaysAfterLastModification))
        .thenApply(ignoredResult -> statistics);
  }

  private CompletableFuture<Void> cleanup(
      @Nonnull final String containerName, final int deleteDaysAfterLastModification) {
    final List<String> customObjectsToDelete = new ArrayList<>();

    return ctpClient
        .graphql()
        .post(buildGraphQlRequest(containerName, deleteDaysAfterLastModification))
        .execute()
        .thenAccept(
            graphQlResults -> {
              if (graphQlResults.getBody() != null && graphQlResults.getBody().getData() != null) {
                final Object data = graphQlResults.getBody().getData();
                final ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
                final JsonNode jsonNode = objectMapper.convertValue(data, JsonNode.class);
                final Iterator<JsonNode> elements =
                    jsonNode
                        .get(GraphQlQueryResource.CUSTOM_OBJECTS.getName())
                        .get("results")
                        .elements();
                while (elements.hasNext()) {
                  JsonNode keyEntry = elements.next();
                  customObjectsToDelete.add(keyEntry.get("key").asText());
                }
                deleteCustomObjects(containerName, customObjectsToDelete);
              }
            });
  }

  private CompletableFuture<Void> cleanupUnresolvedProductReferences(
      final int deleteDaysAfterLastModification) {
    return cleanup(
        UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
        deleteDaysAfterLastModification);
  }

  private CompletableFuture<Void> cleanupUnresolvedParentCategoryReferences(
      final int deleteDaysAfterLastModification) {
    return cleanup(
        UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_CATEGORY_CONTAINER_KEY,
        deleteDaysAfterLastModification);
  }

  private CompletableFuture<Void> cleanupUnresolvedStateReferences(
      final int deleteDaysAfterLastModification) {
    return cleanup(
        UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_TRANSITION_CONTAINER_KEY,
        deleteDaysAfterLastModification);
  }

  /**
   * Prepares a graphql request to fetch the unresolved reference custom objects based on the given
   * {@code containerName} and {@code deleteDaysAfterLastModification}.
   *
   * @param containerName container name (i.e
   *     "commercetools-sync-java.UnresolvedReferencesService.productDrafts")
   * @param deleteDaysAfterLastModification Days to query lastModifiedAt. The custom objects will be
   *     deleted if it hasn't been modified for the specified amount of days.
   */
  private GraphQLRequest buildGraphQlRequest(
      @Nonnull final String containerName, final int deleteDaysAfterLastModification) {

    final Instant lastModifiedAt =
        Instant.now().minus(deleteDaysAfterLastModification, ChronoUnit.DAYS);
    final String query =
        format(
            "query fetchKeys($where: String, $limit: Int) {%n"
                + "  %s(container: \"%s\", limit: $limit, where: $where) {%n"
                + "    results {%n"
                + "      key%n"
                + "    }%n"
                + "  }%n"
                + "}",
            GraphQlQueryResource.CUSTOM_OBJECTS.getName(), containerName);

    final GraphQLVariablesMap graphQLVariablesMap =
        GraphQLVariablesMapBuilder.of()
            .addValue("limit", 250)
            .addValue("where", format("lastModifiedAt < \"%s\"", lastModifiedAt))
            .build();
    return GraphQLRequestBuilder.of().query(query).variables(graphQLVariablesMap).build();
  }

  /**
   * Deletes all custom objects in the given {@link List} representing a page of custom object's
   * keys.
   *
   * <p>Note: The deletion is done concurrently but it's blocked by page consumer to avoid race
   * conditions like fetching and removing same custom objects in same time.
   *
   * @param resourceKeys a page of custom object's keys.
   */
  private void deleteCustomObjects(
      @Nonnull final String containerName, @Nonnull final List<String> resourceKeys) {

    CompletableFuture.allOf(
            resourceKeys.stream()
                .map(resourceKey -> executeDeletion(containerName, resourceKey))
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();
  }

  private CompletionStage<Optional<CustomObject>> executeDeletion(
      @Nonnull final String containerName, @Nonnull final String resourceKey) {

    return ctpClient
        .customObjects()
        .withContainerAndKey(containerName, resourceKey)
        .delete()
        .execute()
        .handle(this::handleDeleteCallback);
  }

  private Optional<CustomObject> handleDeleteCallback(
      @Nonnull final ApiHttpResponse<CustomObject> resource, @Nullable final Throwable throwable) {

    if (throwable == null) {
      statistics.totalDeleted.incrementAndGet();
      return Optional.of(resource.getBody());
    } else if (throwable instanceof NotFoundException) {
      return Optional.empty();
    }

    applyErrorCallback(throwable);
    statistics.totalFailed.incrementAndGet();
    return Optional.empty();
  }

  public static class Statistics {
    final AtomicInteger totalDeleted;
    final AtomicInteger totalFailed;

    private Statistics() {
      this.totalDeleted = new AtomicInteger();
      this.totalFailed = new AtomicInteger();
    }

    public int getTotalDeleted() {
      return totalDeleted.get();
    }

    public int getTotalFailed() {
      return totalFailed.get();
    }

    public String getReportMessage() {
      return format(
          "Summary: %s custom objects were deleted in total (%s failed to delete).",
          getTotalDeleted(), getTotalFailed());
    }
  }
}
