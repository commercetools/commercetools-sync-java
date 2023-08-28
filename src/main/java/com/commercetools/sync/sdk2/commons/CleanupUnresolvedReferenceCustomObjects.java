package com.commercetools.sync.sdk2.commons;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.commons.lang3.StringUtils.*;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLRequestBuilder;
import com.commercetools.api.models.graph_ql.GraphQLVariablesMap;
import com.commercetools.api.models.graph_ql.GraphQLVariablesMapBuilder;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.services.impl.UnresolvedReferencesServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.NotFoundException;
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
    final GraphQlQueryAll graphQlQueryAll =
        new GraphQlQueryAll(
            ctpClient,
            buildGraphQlRequest(containerName, deleteDaysAfterLastModification),
            pageSize);

    return graphQlQueryAll
        .run(customObjectsToRemove -> deleteCustomObjects(containerName, customObjectsToRemove))
        .toCompletableFuture();
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
                + "      id,%n"
                + "      key%n"
                + "    }%n"
                + "  }%n"
                + "}",
            GraphQlQueryResource.CUSTOM_OBJECTS.getName(), containerName);

    final GraphQLVariablesMap graphQLVariablesMap =
        GraphQLVariablesMapBuilder.of()
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
   * @param resourceIdKeyMap a page of custom object's keys.
   */
  private void deleteCustomObjects(
      @Nonnull final String containerName, @Nonnull final Map<String, String> resourceIdKeyMap) {

    CompletableFuture.allOf(
            resourceIdKeyMap.values().stream()
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

  private static class GraphQlQueryAll {
    private final ProjectApiRoot client;
    private final GraphQLRequest graphqlRequest;
    private final long pageSize;

    private Consumer<Map<String, String>> pageConsumer;

    public GraphQlQueryAll(
        @Nonnull final ProjectApiRoot client,
        @Nonnull final GraphQLRequest graphqlRequest,
        final long pageSize) {

      this.client = client;
      this.graphqlRequest = graphqlRequest;
      final GraphQLVariablesMap graphQLVariablesMap = this.graphqlRequest.getVariables();
      graphQLVariablesMap.setValue("limit", pageSize);
      graphQLVariablesMap.setValue("sort", "id asc");
      this.graphqlRequest.setVariables(graphQLVariablesMap);
      this.pageSize = pageSize;
    }

    /**
     * Given a {@link Consumer} to a page of results of type {@link String}, this method sets this
     * instance's {@code pageConsumer} to the supplied value, then it makes requests to fetch the
     * entire result space of a graphql query request {@link GraphQLRequest} to CTP, while accepting
     * the consumer on each fetched page.
     *
     * @param pageConsumer the consumer to accept on each fetched page of the result space.
     * @return a future containing void after the consumer accepted all the pages.
     */
    @Nonnull
    CompletionStage<Void> run(@Nonnull final Consumer<Map<String, String>> pageConsumer) {

      this.pageConsumer = pageConsumer;
      final CompletableFuture<ApiHttpResponse<JsonNode>> firstPage =
          client.graphql().post(graphqlRequest).execute(JsonNode.class);
      return queryNextPages(firstPage);
    }

    /**
     * Given a completion stage {@code currentPageStage} containing a current graphql result {@link
     * GraphQLRequest}, this method composes the completion stage by first checking if the result is
     * null or not. If it is not, then it recursively (by calling itself with the next page's
     * completion stage result) composes to the supplied stage, stages of the all next pages'
     * processing. If there is no next page, then the result of the {@code currentPageStage} would
     * be null and this method would just return a completed future containing null result, which in
     * turn signals the last page of processing.
     *
     * @param currentPageStage a future containing a graphql result {@link GraphQLRequest}.
     */
    @Nonnull
    private CompletionStage<Void> queryNextPages(
        @Nonnull final CompletableFuture<ApiHttpResponse<JsonNode>> currentPageStage) {
      return currentPageStage.thenCompose(
          currentPage ->
              currentPage != null && currentPage.getBody() != null
                  ? queryNextPages(processPageAndGetNext(currentPage.getBody()))
                  : completedFuture(null));
    }

    /**
     * Given a graphql query result representing a page {@link GraphQLRequest}, this method checks
     * if there are elements in the result (size > 0), then it consumes the resultant list using
     * this instance's {@code pageConsumer}. Then it attempts to fetch the next page if it exists
     * and returns a completion stage containing the result of the next page. If there is a next
     * page, then a new future of the next page is returned. If there are no more results, the
     * method returns a completed future containing null.
     *
     * @param jsonNode the current page result as JsonNode
     * @return If there is a next page, then a new future of the next page is returned. If there are
     *     no more results, the method returns a completed future containing null.
     */
    @Nonnull
    private CompletableFuture<ApiHttpResponse<JsonNode>> processPageAndGetNext(
        @Nonnull final JsonNode jsonNode) {
      final Map<String, String> idKeyMap = new HashMap<>();
      final Iterator<JsonNode> elements =
          jsonNode
              .get("data")
              .get(GraphQlQueryResource.CUSTOM_OBJECTS.getName())
              .get("results")
              .elements();
      while (elements.hasNext()) {
        final JsonNode entry = elements.next();
        idKeyMap.put(entry.get("id").asText(), entry.get("key").asText());
      }
      if (!idKeyMap.isEmpty()) {
        consumePageElements(idKeyMap);
        return getNextPageStage(idKeyMap);
      }
      return completedFuture(new ApiHttpResponse<>(200, null, null));
    }

    private void consumePageElements(@Nonnull final Map<String, String> idKeyElements) {
      pageConsumer.accept(idKeyElements);
    }

    /**
     * Given a map of page elements, this method checks if this page is the last page or not by
     * checking if the result size is equal to this instance's {@code pageSize}). If it is, then it
     * means there might be still more results. However, if not, then it means for sure there are no
     * more results and this is the last page. If there is a next page, the id of the last element
     * in the list is used and a future is created containing the fetched results which have an id
     * greater than the id of the last element in the list and this future is returned. If there are
     * no more results, the method returns a completed future containing null.
     *
     * @param idKeyMap map of page elements.
     * @return a future containing the fetched results which have an id greater than the id of the
     *     last element in the set.
     */
    @Nonnull
    private CompletableFuture<ApiHttpResponse<JsonNode>> getNextPageStage(
        @Nonnull final Map<String, String> idKeyMap) {
      if (idKeyMap.size() == pageSize) {
        String lastElementId = EMPTY;
        for (String id : idKeyMap.keySet()) {
          lastElementId = id;
        }

        if (isNotBlank(lastElementId)) {
          final String pagingPredicate = format("id > \"%s\"", lastElementId);
          final GraphQLVariablesMap graphQLVariablesMap = graphqlRequest.getVariables();
          final String predicate =
              Optional.ofNullable(graphQLVariablesMap.values().get("where"))
                  .map(
                      where -> {
                        final String queryPredicate = String.valueOf(where);
                        return isBlank(queryPredicate)
                            ? pagingPredicate
                            : format("%s and %s", queryPredicate, pagingPredicate);
                      })
                  .orElseGet(() -> pagingPredicate);
          graphQLVariablesMap.setValue("where", predicate);
          this.graphqlRequest.setVariables(graphQLVariablesMap);
        }

        return client.graphql().post(this.graphqlRequest).execute(JsonNode.class);
      }
      return completedFuture(new ApiHttpResponse<>(200, null, null));
    }
  }
}
