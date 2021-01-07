package com.commercetools.sync.commons.models;

import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.http.StringHttpRequestBody;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FetchCustomObjectsGraphQlRequestTest {

    @Test
    void httpRequestIntent_WithoutPredicate_ShouldReturnCorrectQueryString() {
        //preparation
        final Instant lastModifiedAt = Instant.parse("2021-01-07T00:00:00Z");
        final FetchCustomObjectsGraphQlRequest request =
            new FetchCustomObjectsGraphQlRequest("container", lastModifiedAt);

        //test
        final HttpRequestIntent httpRequestIntent = request.httpRequestIntent();

        //assertions
        assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
        assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
        final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
        assertThat(requestBody).isNotNull();
        assertThat(requestBody.getString())
            .isEqualTo("{\"query\": \"{customObjects(container: \\\"container\\\", limit: 500, "
                + "where: \\\"lastModifiedAt < \\\\\\\"2021-01-07T00:00:00Z\\\\\\\"\\\", "
                + "sort: [\\\"id asc\\\"]) { results { id key } }}\"}");
    }

    @Test
    void httpRequestIntent_WithPredicate_ShouldReturnCorrectQueryString() {
        //preparation
        final Instant lastModifiedAt = Instant.parse("2021-01-07T00:00:00Z");
        final GraphQlBaseRequest<ResourceKeyIdGraphQlResult> request =
            new FetchCustomObjectsGraphQlRequest("container", lastModifiedAt)
                .withPredicate("id > \\\\\\\"id\\\\\\\"");

        //test
        final HttpRequestIntent httpRequestIntent = request.httpRequestIntent();

        //assertions
        assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
        assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
        final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
        assertThat(requestBody).isNotNull();
        assertThat(requestBody.getString())
            .isEqualTo("{\"query\": \"{customObjects(container: \\\"container\\\", limit: 500, "
                + "where: \\\"lastModifiedAt < \\\\\\\"2021-01-07T00:00:00Z\\\\\\\"\\\""
                + " AND id > \\\\\\\"id\\\\\\\"\\\", sort: [\\\"id asc\\\"])"
                + " { results { id key } }}\"}");
    }
}
