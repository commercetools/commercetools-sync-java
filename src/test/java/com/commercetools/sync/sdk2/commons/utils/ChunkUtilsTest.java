package com.commercetools.sync.sdk2.commons.utils;

import com.commercetools.api.client.ApiRoot;
import com.commercetools.api.client.ByProjectKeyCategoriesGet;
import com.commercetools.api.client.ByProjectKeyGraphqlPost;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.PagedQueryResourceRequest;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryPagedQueryResponse;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.models.graph_ql.GraphQLResponseBuilder;
import com.commercetools.sync.commons.models.ResourceKeyId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sphere.sdk.client.SphereClient;
import io.vrap.rmf.base.client.ApiHttpClientImpl;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChunkUtilsTest {

  @Test
  void chunk_WithEmptyList_ShouldNotChunkItems() {
    final List<List<Object>> chunk = ChunkUtils.chunk(emptyList(), 5);

    assertThat(chunk).isEmpty();
  }

  @Test
  void chunk_WithList_ShouldChunkItemsIntoMultipleLists() {
    final List<List<String>> chunks =
        ChunkUtils.chunk(asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), 3);

    assertThat(chunks).hasSize(4);
    assertThat(chunks)
        .isEqualTo(
            asList(
                asList("1", "2", "3"),
                asList("4", "5", "6"),
                asList("7", "8", "9"),
                singletonList("10")));
  }

  @Test
  void executeChunks_withEmptyRequestList_ShouldReturnEmptyList() {
    List<ByProjectKeyCategoriesGet> queries = new ArrayList<>();
    final List<ApiHttpResponse<CategoryPagedQueryResponse>> results =
        ChunkUtils.executeChunks(queries).join();

    assertThat(results).isEmpty();
  }

  @Test
  void executeChunks_withQueryBuilderRequests_ShouldReturnResults() {
    @SuppressWarnings("unchecked")
    String jsonStringCategories =
        "{\"results\":[{\"id\":\"catId1\", \"key\":\"catKey1\"},"
            + "{\"id\":\"catId2\", \"key\":\"catKey2\"},"
            + "{\"id\":\"catId3\", \"key\":\"catKey3\"}]}";

    final List<ApiHttpResponse<CategoryPagedQueryResponse>> results =
        ChunkUtils.executeChunks(
                asList(getCategoryQueryGetWithWhereKeyInList(asList("1", "2", "3"), jsonStringCategories),
                    getCategoryQueryGetWithWhereKeyInList(asList("4", "5", "6"), jsonStringCategories)))
            .join();

    assertThat(results).hasSize(2);
    List<Category> categories = results.stream().map(ApiHttpResponse::getBody).map(CategoryPagedQueryResponse::getResults).flatMap(Collection::stream).collect(Collectors.toList());
    assertThat(categories).hasSize(6);
  }

  @Test
  void executeChunks_withGraphqlRequests_ShouldReturnResults() {

    String jsonStringKeyToId =
        "{\"data\": {\"categories\": {\"results\":[{\"id\":\"coId1\", \"key\":\"coKey1\"},"
            + "{\"id\":\"coId2\", \"key\":\"coKey2\"}]}}}";

    ProjectApiRoot client = ApiRootBuilder.of(request -> {
      if (request.getUri() != null && request.getUri().toString().contains("graphql")) {
        return completedFuture(new ApiHttpResponse<>(200, null, jsonStringKeyToId.getBytes(StandardCharsets.UTF_8)));
      }
      return completedFuture(new ApiHttpResponse<>(404, null, "".getBytes(StandardCharsets.UTF_8)));
    }).withApiBaseUrl("baseUrl").build("testClient");

    final List<ApiHttpResponse<GraphQLResponse>> results =
        ChunkUtils.executeChunks(client, asList(mock(GraphQLRequest.class), mock(GraphQLRequest.class), mock(GraphQLRequest.class))).join();

    assertThat(results).hasSize(3);
  }

  private ByProjectKeyCategoriesGet getCategoryQueryGetWithWhereKeyInList(final List<String> keylist, final String response) {
    return ApiRootBuilder.of(request -> completedFuture(new ApiHttpResponse<>(200, null, response.getBytes(StandardCharsets.UTF_8)))
    )
            .withApiBaseUrl("baseURl")
            .build()
            .withProjectKey("projectKey")
            .categories()
            .get()
            .withWhere("key in :keys")
            .withPredicateVar("keys", keylist);
  }
}
