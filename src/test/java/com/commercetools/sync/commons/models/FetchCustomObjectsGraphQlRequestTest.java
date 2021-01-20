package com.commercetools.sync.commons.models;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.http.StringHttpRequestBody;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FetchCustomObjectsGraphQlRequestTest {

  @Test
  void httpRequestIntent_WithoutPredicate_ShouldReturnCorrectQueryString() {
    // preparation
    final Instant lastModifiedAt = Instant.parse("2021-01-07T00:00:00Z");
    final FetchCustomObjectsGraphQlRequest request =
        new FetchCustomObjectsGraphQlRequest("container", lastModifiedAt);

    // test
    final HttpRequestIntent httpRequestIntent = request.httpRequestIntent();

    // assertions
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{customObjects(container: \\\"container\\\", limit: 500, "
                + "where: \\\"lastModifiedAt < \\\\\\\"2021-01-07T00:00:00Z\\\\\\\"\\\", "
                + "sort: [\\\"id asc\\\"]) { results { id key } }}\"}");
  }

  @Test
  void httpRequestIntent_WithPredicate_ShouldReturnCorrectQueryString() {
    // preparation
    final Instant lastModifiedAt = Instant.parse("2021-01-07T00:00:00Z");
    final GraphQlBaseRequest<ResourceKeyIdGraphQlResult> request =
        new FetchCustomObjectsGraphQlRequest("container", lastModifiedAt)
            .withPredicate("id > \\\\\\\"id\\\\\\\"");

    // test
    final HttpRequestIntent httpRequestIntent = request.httpRequestIntent();

    // assertions
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{customObjects(container: \\\"container\\\", limit: 500, "
                + "where: \\\"lastModifiedAt < \\\\\\\"2021-01-07T00:00:00Z\\\\\\\""
                + " AND id > \\\\\\\"id\\\\\\\"\\\", sort: [\\\"id asc\\\"])"
                + " { results { id key } }}\"}");
  }

  @Test
  void deserialize_WithMultipleResults_ShouldReturnCorrectResult() throws JsonProcessingException {
    // preparation
    String jsonAsString =
        "{\"data\":{\"customObjects\":{\"results\":[{\"id\":\"id-1\",\"key\":\"key-1\"},"
            + "{\"id\":\"id-2\",\"key\":\"key-2\"},{\"id\":\"id-3\",\"key\":\"key-3\"}]}}}";

    final HttpResponse httpResponse = HttpResponse.of(200, jsonAsString);

    final FetchCustomObjectsGraphQlRequest request =
        new FetchCustomObjectsGraphQlRequest("containerName", Instant.now());

    // test
    final ResourceKeyIdGraphQlResult result = request.deserialize(httpResponse);

    // assertions
    assertThat(result).isNotNull();
    assertThat(result.getResults()).hasSize(3);
    assertThat(result.getResults())
        .extracting("key")
        .containsExactlyInAnyOrder("key-1", "key-2", "key-3");
    assertThat(result.getResults())
        .extracting("id")
        .containsExactlyInAnyOrder("id-1", "id-2", "id-3");
  }
}
