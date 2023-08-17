package com.commercetools.sync.sdk2.commons;

import static com.commercetools.sync.sdk2.commons.ExceptionUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.*;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.sync.sdk2.commons.utils.TestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.NotFoundException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class CleanupUnresolvedReferenceCustomObjectsTest {
  private ProjectApiRoot mockClient = mock(ProjectApiRoot.class);
  private final ByProjectKeyCustomObjectsByContainerByKeyDelete
      byProjectKeyCustomObjectsByContainerByKeyDelete =
          mock(ByProjectKeyCustomObjectsByContainerByKeyDelete.class);
  private final ByProjectKeyGraphqlPost byProjectKeyGraphQlPost =
      mock(ByProjectKeyGraphqlPost.class);
  private static final int deleteDaysAfterLastModification = 30;

  @BeforeEach
  void setup() {
    final ProjectApiRoot client = mock(ProjectApiRoot.class);
    when(client.graphql()).thenReturn(mock());
    when(client.graphql().post(any(GraphQLRequest.class))).thenReturn(byProjectKeyGraphQlPost);

    final ByProjectKeyCustomObjectsByContainerByKeyRequestBuilder
        byProjectKeyCustomObjectsByContainerByKeyRequestBuilder =
            mock(ByProjectKeyCustomObjectsByContainerByKeyRequestBuilder.class);
    when(client.customObjects()).thenReturn(mock());
    when(client.customObjects().withContainerAndKey(anyString(), anyString()))
        .thenReturn(byProjectKeyCustomObjectsByContainerByKeyRequestBuilder);
    when(byProjectKeyCustomObjectsByContainerByKeyRequestBuilder.delete())
        .thenReturn(byProjectKeyCustomObjectsByContainerByKeyDelete);
    mockClient = client;
  }

  @AfterEach
  void cleanup() {
    reset(mockClient, byProjectKeyCustomObjectsByContainerByKeyDelete, byProjectKeyGraphQlPost);
  }

  @Test
  void cleanup_withDeleteDaysAfterLastModification_ShouldDeleteAndReturnCleanupStatistics()
      throws JsonProcessingException {
    String jsonStringCustomObjects =
        "{ \"data\": {\"customObjects\": {\"results\":[{\"id\":\"coId1\", \"key\":\"coKey1\"}, {"
            + "\"id\":\"coId1\", \"key\":\"coKey2\"}]}}}";
    final ApiHttpResponse<JsonNode> fetchedCustomObjectsApiHttpResponse =
        TestUtils.mockJsonNodeResponse(jsonStringCustomObjects);
    when(byProjectKeyGraphQlPost.execute(eq(JsonNode.class)))
        .thenReturn(CompletableFuture.completedFuture(fetchedCustomObjectsApiHttpResponse));

    final Throwable badRequestException = createBadGatewayException();
    final ApiHttpResponse<CustomObject> customObjectApiHttpResponse = mock(ApiHttpResponse.class);
    when(customObjectApiHttpResponse.getBody()).thenReturn(mock(CustomObject.class));
    when(byProjectKeyCustomObjectsByContainerByKeyDelete.execute())
        .thenReturn(CompletableFutureUtils.failed(badRequestException))
        .thenReturn(CompletableFuture.completedFuture(customObjectApiHttpResponse));

    final CleanupUnresolvedReferenceCustomObjects.Statistics statistics =
        CleanupUnresolvedReferenceCustomObjects.of(mockClient)
            .cleanup(deleteDaysAfterLastModification)
            .join();

    assertThat(statistics.getTotalDeleted()).isEqualTo(2);
    assertThat(statistics.getTotalFailed()).isEqualTo(1);
    assertThat(statistics.getReportMessage())
        .isEqualTo("Summary: 2 custom objects were deleted in total (1 failed to delete).");
  }

  @Test
  void cleanup_withNotFound404Exception_ShouldNotIncrementFailedCounter()
      throws JsonProcessingException {
    String jsonStringCustomObjects =
        "{ \"data\": {\"customObjects\": {\"results\":[{\"id\":\"coId1\", \"key\":\"coKey1\"}]}}}";
    final ApiHttpResponse<JsonNode> fetchedCustomObjectsApiHttpResponse =
        TestUtils.mockJsonNodeResponse(jsonStringCustomObjects);
    when(byProjectKeyGraphQlPost.execute(eq(JsonNode.class)))
        .thenReturn(CompletableFuture.completedFuture(fetchedCustomObjectsApiHttpResponse));

    final String json = getErrorResponseJsonString(404);
    final NotFoundException notFoundException =
        new NotFoundException(
            404,
            "",
            null,
            "",
            new ApiHttpResponse<>(404, null, json.getBytes(StandardCharsets.UTF_8)));
    final ApiHttpResponse<CustomObject> customObjectApiHttpResponse = mock(ApiHttpResponse.class);
    when(customObjectApiHttpResponse.getBody()).thenReturn(mock(CustomObject.class));
    when(byProjectKeyCustomObjectsByContainerByKeyDelete.execute())
        .thenReturn(CompletableFuture.completedFuture(customObjectApiHttpResponse))
        .thenReturn(CompletableFutureUtils.failed(notFoundException));

    final CleanupUnresolvedReferenceCustomObjects.Statistics statistics =
        CleanupUnresolvedReferenceCustomObjects.of(mockClient)
            .cleanup(deleteDaysAfterLastModification)
            .join();

    assertThat(statistics.getTotalDeleted()).isEqualTo(1);
    assertThat(statistics.getTotalFailed()).isEqualTo(0);
    assertThat(statistics.getReportMessage())
        .isEqualTo("Summary: 1 custom objects were deleted in total (0 failed to delete).");
  }

  @Test
  void cleanup_withBadRequest400Exception_ShouldIncrementFailedCounterAndTriggerErrorCallback()
      throws JsonProcessingException {
    String jsonStringCustomObjects =
        "{ \"data\": {\"customObjects\": {\"results\":[{\"id\":\"coId1\", \"key\":\"coKey1\"}]}}}";
    final ApiHttpResponse<JsonNode> fetchedCustomObjectsApiHttpResponse =
        TestUtils.mockJsonNodeResponse(jsonStringCustomObjects);
    when(byProjectKeyGraphQlPost.execute(eq(JsonNode.class)))
        .thenReturn(CompletableFuture.completedFuture(fetchedCustomObjectsApiHttpResponse));

    final ApiHttpResponse<CustomObject> customObjectApiHttpResponse = mock(ApiHttpResponse.class);
    when(customObjectApiHttpResponse.getBody()).thenReturn(mock(CustomObject.class));
    final Throwable badRequestException = createBadRequestException();
    when(byProjectKeyCustomObjectsByContainerByKeyDelete.execute())
        .thenReturn(CompletableFuture.completedFuture(customObjectApiHttpResponse))
        .thenReturn(CompletableFutureUtils.failed(badRequestException));

    final List<Throwable> exceptions = new ArrayList<>();
    final CleanupUnresolvedReferenceCustomObjects.Statistics statistics =
        CleanupUnresolvedReferenceCustomObjects.of(mockClient)
            .errorCallback(exceptions::add)
            .cleanup(deleteDaysAfterLastModification)
            .join();

    assertThat(statistics.getTotalDeleted()).isEqualTo(1);
    assertThat(statistics.getTotalFailed()).isEqualTo(2);
    assertThat(exceptions).contains(badRequestException);
    assertThat(statistics.getReportMessage())
        .isEqualTo("Summary: 1 custom objects were deleted in total (2 failed to delete).");
  }
}
