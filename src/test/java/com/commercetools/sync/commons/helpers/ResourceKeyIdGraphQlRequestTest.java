package com.commercetools.sync.commons.helpers;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercetools.sync.commons.models.GraphQlBaseRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.http.StringHttpRequestBody;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResourceKeyIdGraphQlRequestTest {

  @Test
  void newGraphQlRequest_WithNullKeys_ShouldThrowNullPointerException() {
    // test & assertion
    assertThatThrownBy(
            () -> new ResourceKeyIdGraphQlRequest(null, GraphQlQueryResources.CATEGORIES))
        .isExactlyInstanceOf(NullPointerException.class);
  }

  @Test
  void httpRequestIntent_WithEmptyKeys_ShouldReturnQueryStringWithEmptyKeysClause() {
    // preparation
    final ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest =
        new ResourceKeyIdGraphQlRequest(Collections.emptySet(), GraphQlQueryResources.CATEGORIES);

    // test
    final HttpRequestIntent httpRequestIntent = resourceKeyIdGraphQlRequest.httpRequestIntent();

    // assertions
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{categories(limit: 500, where: \\\"key"
                + " in (\\\\\\\"\\\\\\\")\\\", sort: [\\\"id asc\\\"]) { results { id key } }}\"}");
  }

  @Test
  void httpRequestIntent_WithKeys_ShouldReturnCorrectQueryString() {
    // preparation
    final Set<String> keysToSearch = new HashSet<>();
    keysToSearch.add("key1");
    keysToSearch.add("key2");
    final ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest =
        new ResourceKeyIdGraphQlRequest(keysToSearch, GraphQlQueryResources.CATEGORIES);

    // test
    final HttpRequestIntent httpRequestIntent = resourceKeyIdGraphQlRequest.httpRequestIntent();

    // assertions
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{categories(limit: 500, where: \\\"key"
                + " in (\\\\\\\"key1\\\\\\\", \\\\\\\"key2\\\\\\\")\\\", sort: [\\\"id asc\\\"]) { results { id key } "
                + "}}\"}");
  }

  @Test
  void httpRequestIntent_WithSpecialCharacterInKeys_ShouldReturnCorrectQueryString() {
    // preparation
    final Set<String> keysToSearch = new HashSet<>();
    keysToSearch.add("key1");
    keysToSearch.add("key\"2");
    final ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest =
        new ResourceKeyIdGraphQlRequest(keysToSearch, GraphQlQueryResources.CATEGORIES);

    // test
    final HttpRequestIntent httpRequestIntent = resourceKeyIdGraphQlRequest.httpRequestIntent();

    // assertions
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{categories(limit: 500, where: \\\"key"
                + " in (\\\\\\\"key1\\\\\\\", \\\\\\\"key\\\"2\\\\\\\")\\\", sort: [\\\"id asc\\\"]) { results { id key } "
                + "}}\"}");
  }

  @Test
  void httpRequestIntent_WithSomeEmptyAndNullKeys_ShouldReturnCorrectQueryString() {
    // preparation
    final Set<String> keysToSearch = new HashSet<>();
    keysToSearch.add("key1");
    keysToSearch.add("");
    keysToSearch.add("key2");
    keysToSearch.add(null);
    final ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest =
        new ResourceKeyIdGraphQlRequest(keysToSearch, GraphQlQueryResources.CATEGORIES);

    // test
    final HttpRequestIntent httpRequestIntent = resourceKeyIdGraphQlRequest.httpRequestIntent();

    // assertions
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{categories(limit: 500, where: \\\"key"
                + " in (\\\\\\\"key1\\\\\\\", \\\\\\\"key2\\\\\\\")\\\", sort: [\\\"id asc\\\"]) { results { id key } "
                + "}}\"}");
  }

  @Test
  void httpRequestIntent_WithKeyAndExplicitLimit_ShouldReturnCorrectQueryString() {
    // preparation
    final GraphQlBaseRequest<ResourceKeyIdGraphQlResult> resourceKeyIdGraphQlRequest =
        new ResourceKeyIdGraphQlRequest(singleton("key1"), GraphQlQueryResources.CATEGORIES)
            .withLimit(10);

    // test
    final HttpRequestIntent httpRequestIntent = resourceKeyIdGraphQlRequest.httpRequestIntent();

    // assertions
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{categories(limit: 10, where: \\\"key"
                + " in (\\\\\\\"key1\\\\\\\")\\\", sort: [\\\"id asc\\\"]) { results { id key } "
                + "}}\"}");
  }

  @Test
  void httpRequestIntent_WithKeyAndPredicate_ShouldReturnCorrectQueryString() {
    // preparation
    final GraphQlBaseRequest<ResourceKeyIdGraphQlResult> resourceKeyIdGraphQlRequest =
        new ResourceKeyIdGraphQlRequest(singleton("key1"), GraphQlQueryResources.CATEGORIES)
            .withPredicate("id > \\\\\\\"id" + "\\\\\\\"");

    // test
    final HttpRequestIntent httpRequestIntent = resourceKeyIdGraphQlRequest.httpRequestIntent();

    // assertions
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{categories(limit: 500, where: \\\"key"
                + " in (\\\\\\\"key1\\\\\\\") AND id > \\\\\\\"id\\\\\\\"\\\", sort: [\\\"id asc\\\"])"
                + " { results { id key } }}\"}");
  }

  @Test
  void deserialize_WithEmptyResult_ShouldReturnNull() throws JsonProcessingException {
    // preparation
    final HttpResponse httpResponse = HttpResponse.of(200, "null");
    final ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest =
        new ResourceKeyIdGraphQlRequest(singleton("key-1"), GraphQlQueryResources.CATEGORIES);

    // test
    final ResourceKeyIdGraphQlResult result = resourceKeyIdGraphQlRequest.deserialize(httpResponse);

    // assertions
    assertThat(result).isNull();
  }

  @Test
  void deserialize_WithEmptyResult_ShouldDeserializeCorrectly() throws JsonProcessingException {
    // preparation
    String jsonAsString = "{\"data\":{\"categories\":{\"results\":[]}}}";

    final HttpResponse httpResponse = HttpResponse.of(200, jsonAsString);

    final ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest =
        new ResourceKeyIdGraphQlRequest(singleton("key-1"), GraphQlQueryResources.CATEGORIES);

    // test
    final ResourceKeyIdGraphQlResult result = resourceKeyIdGraphQlRequest.deserialize(httpResponse);

    // assertions
    assertThat(result).isNotNull();
    assertThat(result.getResults()).isEmpty();
  }

  @Test
  void deserialize_WithSingleResult_ShouldReturnSingletonMap() throws JsonProcessingException {
    // preparation
    String jsonAsString =
        "{\"data\":{\"categories\":{\"results\":[{\"id\":\"id-1\",\"key\":\"key-1\"}]}}}";

    final HttpResponse httpResponse = HttpResponse.of(200, jsonAsString);

    final ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest =
        new ResourceKeyIdGraphQlRequest(singleton("key-1"), GraphQlQueryResources.CATEGORIES);

    // test
    final ResourceKeyIdGraphQlResult result = resourceKeyIdGraphQlRequest.deserialize(httpResponse);

    // assertions
    assertThat(result).isNotNull();
    assertThat(result.getResults()).hasSize(1);
    assertThat(result.getResults()).extracting("key").containsExactly("key-1");
    assertThat(result.getResults()).extracting("id").containsExactly("id-1");
  }

  @Test
  void deserialize_WithMultipleResults_ShouldReturnCorrectResult() throws JsonProcessingException {
    // preparation
    String jsonAsString =
        "{\"data\":{\"categories\":{\"results\":[{\"id\":\"id-1\",\"key\":\"key-1\"},"
            + "{\"id\":\"id-2\",\"key\":\"key-2\"},{\"id\":\"id-3\",\"key\":\"key-3\"}]}}}";

    final HttpResponse httpResponse = HttpResponse.of(200, jsonAsString);

    final ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest =
        new ResourceKeyIdGraphQlRequest(singleton("key-1"), GraphQlQueryResources.CATEGORIES);

    // test
    final ResourceKeyIdGraphQlResult result = resourceKeyIdGraphQlRequest.deserialize(httpResponse);

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
