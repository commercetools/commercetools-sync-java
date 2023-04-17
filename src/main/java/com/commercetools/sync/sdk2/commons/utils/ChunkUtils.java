package com.commercetools.sync.sdk2.commons.utils;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.collectionOfFuturesToFutureOfCollection;
import static java.util.stream.Collectors.toList;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.PagedQueryResourceRequest;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class ChunkUtils {

  /**
   * Executes the given {@link List} of {@link PagedQueryResourceRequest}s, and collects results in
   * a list.
   *
   * @param requests A list of {@link PagedQueryResourceRequest} implementation to allow {@link
   *     ProjectApiRoot} to execute queries on CTP.
   * @param <ResourceT> the type of the underlying model.
   * @param <QueryT> the type of the request model.
   * @return a list of lists where each list represents the results of passed {@link
   *     PagedQueryResourceRequest}.
   */
  public static <QueryT extends PagedQueryResourceRequest<QueryT, ResourceT>, ResourceT>
      CompletableFuture<List<ApiHttpResponse<ResourceT>>> executeChunks(
          @Nonnull final List<QueryT> requests) {

    final List<CompletableFuture<ApiHttpResponse<ResourceT>>> futures =
        requests.stream().map(request -> request.execute()).collect(toList());

    return collectionOfFuturesToFutureOfCollection(futures, toList());
  }

  /**
   * Executes the given {@link List} of {@link GraphQLRequest}s, and collects results in a list.
   *
   * @param requests A list of {@link GraphQLRequest} implementation to allow {@link ProjectApiRoot}
   *     to execute queries on CTP.
   * @return a list of lists where each list represents the results of passed {@link
   *     GraphQLRequest}.
   */
  public static CompletableFuture<List<ApiHttpResponse<GraphQLResponse>>> executeChunks(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final List<GraphQLRequest> requests) {

    final List<CompletableFuture<ApiHttpResponse<GraphQLResponse>>> futures =
        requests.stream()
            .map(request -> ctpClient.graphql().post(request).execute())
            .collect(toList());

    return collectionOfFuturesToFutureOfCollection(futures, toList());
  }

  /**
   * Given a collection of items and a {@code chunkSize}, this method chunks the elements into
   * chunks with the {@code chunkSize} represented by a {@link List} of elements.
   *
   * @param elements the list of elements
   * @param chunkSize the size of each chunk.
   * @param <T> the type of the underlying model.
   * @return a list of lists where each list represents a chunk of elements.
   */
  public static <T> List<List<T>> chunk(
      @Nonnull final Collection<T> elements, final int chunkSize) {
    final AtomicInteger index = new AtomicInteger(0);

    return new ArrayList<>(
        elements.stream()
            .collect(Collectors.groupingBy(x -> index.getAndIncrement() / chunkSize))
            .values());
  }
}
