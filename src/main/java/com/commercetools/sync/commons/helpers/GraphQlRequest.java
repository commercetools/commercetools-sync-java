package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.commons.models.GraphQlQueryEndpoint;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.json.SphereJsonUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public class GraphQlRequest implements SphereRequest<GraphQlResult> {
    protected final Set<String> keysToSearch;
    protected final GraphQlQueryEndpoint endpoint;
    private long limit = 500;
    private String queryPredicate = null;

    public GraphQlRequest(@Nonnull final Set<String> keysToSearch, @Nonnull final GraphQlQueryEndpoint endpoint) {

        this.keysToSearch = keysToSearch;
        this.endpoint = endpoint;
    }

    @Nonnull
    public GraphQlRequest withPredicate(final String predicate) {

        this.queryPredicate = predicate;
        return this;
    }

    @Nonnull
    public GraphQlRequest withLimit(final long limit) {

        this.limit = limit;
        return this;
    }

    @Nullable
    @Override
    public GraphQlResult deserialize(final HttpResponse httpResponse) {

        final JsonNode rootJsonNode = SphereJsonUtils.parse(httpResponse.getResponseBody());
        if (rootJsonNode.isNull()) {
            return null;
        }
        JsonNode result = rootJsonNode.get("data").get(endpoint.getName());
        return SphereJsonUtils.readObject(result, GraphQlResult.class);
    }

    @Override
    public HttpRequestIntent httpRequestIntent() {

        final String body = format("{\"query\": \"{%s}\"}", buildQueryString());
        return HttpRequestIntent.of(HttpMethod.POST, "/graphql", body);
    }

    /**
     * This method builds a string matching the required format to query a set of ids matching given keys of a
     * resource using the CTP graphql API
     *
     * @return a string representing a graphql query
     */
    @Nonnull
    String buildQueryString() {

        return format(
            "%s(limit: %d, where: \\\"%s\\\", sort: [\\\"id asc\\\"]) { results { id key } }",
            this.endpoint.getName(), this.limit, createWhereQuery(keysToSearch));
    }

    @Nonnull
    private String createWhereQuery(@Nonnull final Set<String> keys) {
        // The where in the graphql query should look like this in the end =>  `where: "key in (\"key1\",
        // \"key2\")"`
        // So we need an escaping backslash before the quote. So to add this:
        // We need 1 backslash (2 in java) to escape the quote in the graphql query.
        // We need 2 backslashes (4 in java) to escape the backslash in the JSON payload string.
        // We need 1 extra backslash to escape the quote in the java string
        // hence: 7 backslashes:
        final String backslashQuote = "\\\\\\\"";
        final String commaSeparatedKeys =
            keys.stream()
                .collect(
                    joining(
                        format("%s, %s", backslashQuote, backslashQuote),
                        backslashQuote,
                        backslashQuote));

        String whereQuery = createWhereQuery(commaSeparatedKeys);
        return this.queryPredicate == null ? whereQuery : format("%s AND %s", whereQuery, queryPredicate);
    }

    @Nonnull
    private static String createWhereQuery(@Nonnull final String commaSeparatedKeys) {

        return format("key in (%s)", commaSeparatedKeys);
    }

}

