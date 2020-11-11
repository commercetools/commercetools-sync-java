package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.commons.models.GraphQlQueryEndpoint;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.http.StringHttpRequestBody;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GraphQlRequestTest {

    @Test
    void newGraphQlRequest_WithNullKeys_ShouldThrowNullPointerException() {
        //test & assertion
        assertThatThrownBy(
            () -> new GraphQlRequest(null, GraphQlQueryEndpoint.CATEGORIES))
            .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    void httpRequestIntent_WithEmptyKeys_ShouldReturnQueryStringWithEmptyKeysClause() {
        //preparation
        final GraphQlRequest categoryGraphQlRequest =
            new GraphQlRequest(Collections.emptySet(), GraphQlQueryEndpoint.CATEGORIES);

        //test
        final HttpRequestIntent httpRequestIntent = categoryGraphQlRequest.httpRequestIntent();

        //assertions
        assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
        assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
        final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
        assertThat(requestBody).isNotNull();
        assertThat(requestBody.getString())
            .isEqualTo("{\"query\": \"{categories(limit: 500, where: \\\"key"
                + " in (\\\\\\\"\\\\\\\")\\\", sort: [\\\"id asc\\\"]) { results { id key } }}\"}");
    }

    @Test
    void httpRequestIntent_WithKeys_ShouldReturnCorrectQueryString() {
        //preparation
        final Set<String> keysToSearch = new HashSet<>();
        keysToSearch.add("key1");
        keysToSearch.add("key2");
        final GraphQlRequest categoryGraphQlRequest = new GraphQlRequest(keysToSearch, GraphQlQueryEndpoint.CATEGORIES);

        //test
        final HttpRequestIntent httpRequestIntent = categoryGraphQlRequest.httpRequestIntent();

        //assertions
        assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
        assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
        final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
        assertThat(requestBody).isNotNull();
        assertThat(requestBody.getString())
            .isEqualTo("{\"query\": \"{categories(limit: 500, where: \\\"key"
                + " in (\\\\\\\"key1\\\\\\\", \\\\\\\"key2\\\\\\\")\\\", sort: [\\\"id asc\\\"]) { results { id key } "
                + "}}\"}");
    }

    @Test
    void httpRequestIntent_WithKeyAndExplicitLimit_ShouldReturnCorrectQueryString() {
        //preparation
        final GraphQlRequest categoryGraphQlRequest =
            new GraphQlRequest(singleton("key1"), GraphQlQueryEndpoint.CATEGORIES).withLimit(10);

        //test
        final HttpRequestIntent httpRequestIntent = categoryGraphQlRequest.httpRequestIntent();

        //assertions
        assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
        assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
        final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
        assertThat(requestBody).isNotNull();
        assertThat(requestBody.getString())
            .isEqualTo("{\"query\": \"{categories(limit: 10, where: \\\"key"
                + " in (\\\\\\\"key1\\\\\\\")\\\", sort: [\\\"id asc\\\"]) { results { id key } "
                + "}}\"}");
    }

    @Test
    void httpRequestIntent_WithKeyAndPredicate_ShouldReturnCorrectQueryString() {
        //preparation
        final GraphQlRequest categoryGraphQlRequest =
            new GraphQlRequest(singleton("key1"), GraphQlQueryEndpoint.CATEGORIES).withPredicate("id > \\\\\\\"id"
                + "\\\\\\\"");

        //test
        final HttpRequestIntent httpRequestIntent = categoryGraphQlRequest.httpRequestIntent();

        //assertions
        assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
        assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
        final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
        assertThat(requestBody).isNotNull();
        assertThat(requestBody.getString())
            .isEqualTo("{\"query\": \"{categories(limit: 500, where: \\\"key"
                + " in (\\\\\\\"key1\\\\\\\") AND id > \\\\\\\"id\\\\\\\"\\\", sort: [\\\"id asc\\\"])"
                + " { results { id key } }}\"}");
    }

    @Test
    void deserialize_WithEmptyResult_ShouldReturnNull() {
        //preparation
        final HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getResponseBody()).thenReturn("null".getBytes());
        final GraphQlRequest categoryGraphQlRequest =
            new GraphQlRequest(singleton("key-1"), GraphQlQueryEndpoint.CATEGORIES);

        //test
        final GraphQlResult result = categoryGraphQlRequest.deserialize(httpResponse);

        //assertions
        assertThat(result).isNull();
    }

    @Test
    void deserialize_WithEmptyResult_ShouldDeserializeCorrectly() {
        //preparation
        String jsonAsString = "{\"data\":{\"categories\":{\"results\":[]}}}";
        final HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getResponseBody()).thenReturn(jsonAsString.getBytes());
        final GraphQlRequest categoriesGraphQlRequest =
            new GraphQlRequest(singleton("key-1"), GraphQlQueryEndpoint.CATEGORIES);

        //test
        final GraphQlResult result = categoriesGraphQlRequest.deserialize(httpResponse);

        //assertions
        assertThat(result).isNotNull();
        assertThat(result.getResults()).isEmpty();
    }

    @Test
    void deserialize_WithSingleResult_ShouldReturnSingletonMap() {
        //preparation
        HttpResponse httpResponse = mock(HttpResponse.class);
        String jsonAsString = "{\"data\":{\"categories\":{\"results\":[{\"id\":\"id-1\",\"key\":\"key-1\"}]}}}";
        when(httpResponse.getResponseBody()).thenReturn(jsonAsString.getBytes());
        final GraphQlRequest categoryGraphQlRequest =
            new GraphQlRequest(singleton("key-1"), GraphQlQueryEndpoint.CATEGORIES);

        //test
        final GraphQlResult result = categoryGraphQlRequest.deserialize(httpResponse);

        //assertions
        assertThat(result).isNotNull();
        assertThat(result.getResults()).hasSize(1);
        assertThat(result.getResults())
            .extracting("key")
            .containsExactly("key-1");
        assertThat(result.getResults())
            .extracting("id")
            .containsExactly("id-1");
    }

    @Test
    void deserialize_WithMultipleResults_ShouldReturnCorrectResult() {
        //preparation
        HttpResponse httpResponse = mock(HttpResponse.class);
        String jsonAsString = "{\"data\":{\"categories\":{\"results\":[{\"id\":\"id-1\",\"key\":\"key-1\"},"
            + "{\"id\":\"id-2\",\"key\":\"key-2\"},{\"id\":\"id-3\",\"key\":\"key-3\"}]}}}";
        when(httpResponse.getResponseBody()).thenReturn(jsonAsString.getBytes());
        final GraphQlRequest categoryGraphQlRequest =
            new GraphQlRequest(singleton("key-1"), GraphQlQueryEndpoint.CATEGORIES);

        //test
        final GraphQlResult result = categoryGraphQlRequest.deserialize(httpResponse);

        //assertions
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
